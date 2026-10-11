/*
 * ThreatReach.java
 */
package control.services;

import business.ArmyPath;
import business.MovimentoExercito;
import business.facade.ExercitoFacade;
import business.facade.JogadorFacade;
import business.facade.LocalFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import model.Cenario;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The inverse of "where can this army go": <b>which hexes can a hostile force reach, from how far,
 * and in how many turns</b>. One standing picture instead of one army at a time off a list click.
 *
 * <b>It only ever marks a threat, never safety.</b> An enemy army the player cannot see is not in
 * the EGF at all, so the absence of a mark is the absence of information, not a promise - the same
 * one-directional contract {@code CitySizeCap} keeps. Every assumption below therefore leans towards
 * over-stating reach, because for a warning the costly error is the one that stays silent.
 *
 * <b>The fog problem, and why it is not solved by doing nothing.</b> An unscouted enemy army ships
 * with an EMPTY troop map (verified in the live EGF: {@code <tropas class="tree-map"/>}, alongside an
 * empty {@code habilidades}, {@code comida} 0 and {@code moral} 0 - all placeholders). Handing that
 * empty list to the shared cost model is not neutral, it is catastrophic:
 * {@code MovimentoExercito.getCustoMovimentacaoTerra} opens with {@code if (getTropas().isEmpty())
 * return 1;} <b>before</b> the water, unbridged-river and mountain-to-mountain checks further down
 * the same method. Every hex then costs 1 and nothing is impassable, so the army's reach comes back
 * as a perfect circle of the full point budget that walks across oceans. Against real terrain costs
 * - 3 for plains, 12 for mountains, against a budget near 14 - the true reach is about a quarter of
 * that. This is live today in {@code MapaControler.printTagArmyRange}, which the Armies tab calls on
 * selection and whose filter combo offers All and Enemies.
 * <p>
 * The short circuit is NOT fixed where it lives: {@code MovimentoExercito} is used by seven Judge
 * classes and {@code calculateArmyRangeHexes} is called by {@code Ordem851MoveNpcArmy}. It is fixed
 * by never handing the cost model an empty list - see {@link #assumedRoster}.
 *
 * @author jmoura
 */
public final class ThreatReach implements Serializable {

    private static final Log log = LogFactory.getLog(ThreatReach.class);

    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();
    private static final LocalFacade localFacade = new LocalFacade();

    /** How far ahead the picture looks. Turn 1 is exact; turn 2 is an upper bound - see {@link #addArmy}. */
    public static final int MAX_TURNS = 2;

    /**
     * The Judge's own per-turn movement budget, taken at its WORST case.
     *
     * {@code OrdemBaseMovParent.getMovPoints} gives a plain move exactly
     * {@code getArmyMoveMaxPoints()}, but both {@code Ordem860ForcedMarch} and
     * {@code Ordem830MovNavy} override it with {@code getMovimentoExercito() + 2}, and those two are
     * not edge cases - 11,676 and 10,787 orders issued across games 850 to 907. The floor of 14 is
     * {@code Ordem851MoveNpcArmy}, which hardcodes its budget regardless of scenario; the client
     * cannot reliably tell an NPC army apart ({@code NacaoFacade.isNpc} is a name match on
     * "Barbarians" and takes a Personagem, not an Exercito), so one worst case covers both.
     */
    public static int budgetPerTurn(int armyMoveMaxPoints) {
        return Math.max(armyMoveMaxPoints + 2, 14);
    }

    /** One hostile force that reaches a hex, and what had to be assumed to say so. */
    public static final class Threat implements Comparable<Threat>, Serializable {

        private final Exercito army;
        private final int turns;
        private final String assumedTroop;

        Threat(Exercito army, int turns, String assumedTroop) {
            this.army = army;
            this.turns = turns;
            this.assumedTroop = assumedTroop;
        }

        public Exercito getArmy() {
            return army;
        }

        /** 1 or 2. */
        public int getTurns() {
            return turns;
        }

        /**
         * The troop type this reach was computed from when the army's real composition is invisible,
         * or null when it was counted. Never a claim about what is actually there: it is the fastest
         * thing this owner could field, and the UI must say so.
         */
        public String getAssumedTroop() {
            return assumedTroop;
        }

        public boolean isAssumed() {
            return assumedTroop != null;
        }

        public String getArmyName() {
            return army == null ? "" : army.getNome();
        }

        public String getNationName() {
            try {
                return army.getNacao().getNome();
            } catch (NullPointerException ex) {
                return "";
            }
        }

        @Override
        public int compareTo(Threat other) {
            return Integer.compare(this.turns, other.turns);
        }
    }

    /** Every hex a hostile force can reach, with the soonest turn and who gets there. */
    public static final class Coverage implements Serializable {

        private final Map<Local, List<Threat>> byHex;

        Coverage(Map<Local, List<Threat>> byHex) {
            this.byHex = byHex;
        }

        public boolean isEmpty() {
            return byHex.isEmpty();
        }

        /** Every hex reachable within {@code turns}, for one overlay band. */
        public Set<Local> getHexesWithin(int turns) {
            final Set<Local> ret = new HashSet<>();
            for (Map.Entry<Local, List<Threat>> entry : byHex.entrySet()) {
                if (soonest(entry.getValue()) <= turns) {
                    ret.add(entry.getKey());
                }
            }
            return ret;
        }

        /** The forces that reach this hex, soonest first, or an empty list. */
        public List<Threat> getThreats(Local hex) {
            final List<Threat> ret = byHex.get(hex);
            if (ret == null) {
                return Collections.emptyList();
            }
            Collections.sort(ret);
            return ret;
        }

        /** The soonest turn anything reaches this hex, or 0 for nothing. */
        public int getSoonest(Local hex) {
            final List<Threat> at = byHex.get(hex);
            return at == null ? 0 : soonest(at);
        }

        private static int soonest(List<Threat> at) {
            int ret = Integer.MAX_VALUE;
            for (Threat t : at) {
                ret = Math.min(ret, t.getTurns());
            }
            return ret;
        }
    }

    /**
     * The picture for the turn file currently open, or null before anything has asked for one.
     *
     * <b>Computed lazily and kept, not recomputed per redraw</b>, which is the one place this parts
     * company with {@code ScoutFootprint} and {@code MoveConvergence}. Those two answer questions
     * about the PLAYER'S OWN orders and have to be refreshed every time he saves one. This answers a
     * question about where the enemy is standing, which the EGF fixes for the whole planning session
     * - and it is the expensive one, a flood per hostile army over the whole map.
     * <p>
     * Lazy rather than computed at load, because opening an EGF is already the slowest thing the
     * client does and nobody should pay for a map layer they may never switch on.
     * <p>
     * <b>Cleared when a new game is opened</b>, in {@code MapaControler.initialize}. A kept Threat
     * holds an Exercito and the map keys on Local, and either one reaches the whole previous World
     * graph - the same leak the other two snapshots are cleared for.
     */
    private static volatile Coverage current = null;

    public static void setCurrent(Coverage coverage) {
        current = coverage;
    }

    /** The cached picture, computing it on the first ask. Never null. */
    public static Coverage getOrCompute(Collection<Exercito> armies, Jogador observer,
            SortedMap<String, Local> locais, Cenario cenario, int armyMoveMaxPoints) {
        Coverage ret = current;
        if (ret == null) {
            ret = compute(armies, observer, locais, cenario, armyMoveMaxPoints);
            current = ret;
        }
        return ret;
    }

    private ThreatReach() {
    }

    /**
     * The whole picture, over every hostile army in the world.
     *
     * Pure: everything it needs is a parameter, so it is testable without a loaded game, and it never
     * touches the model.
     */
    public static Coverage compute(Collection<Exercito> armies, Jogador observer,
            SortedMap<String, Local> locais, Cenario cenario, int armyMoveMaxPoints) {
        final Map<Local, List<Threat>> byHex = new HashMap<>();
        if (armies == null || locais == null || cenario == null) {
            return new Coverage(byHex);
        }
        final int budget = budgetPerTurn(armyMoveMaxPoints);
        for (Exercito army : armies) {
            // One unreadable army must never cost the whole picture. A half-built threat map that
            // silently omits the army next door is worse than one that logs and carries on.
            try {
                if (!isHostile(army, observer)) {
                    continue;
                }
                addArmy(byHex, army, locais, cenario, budget);
            } catch (RuntimeException ex) {
                log.warn(String.format("Threat reach skipped for %s: %s",
                        army == null ? "?" : army.getNome(), ex));
            }
        }
        return new Coverage(byHex);
    }

    /**
     * Whose armies count.
     *
     * Direction matters and is always {@code mine.isInimigo(theirs)} - the reverse is unanswerable on
     * the client, because the EGF carries a foreign nation's relationship only when it is positive
     * and aimed at the player.
     * <p>
     * An army with NO nation counts too, which is the one place this deliberately parts company with
     * the Armies tab's own "enemies" filter: that filter wraps the whole test in
     * {@code catch (NullPointerException)} and drops such an army silently. An unscouted army can
     * arrive exactly that way, and dropping the ones we know least about is backwards for a threat
     * display.
     */
    public static boolean isHostile(Exercito army, Jogador observer) {
        if (army == null || observer == null) {
            return false;
        }
        final Nacao nacao = army.getNacao();
        if (nacao == null) {
            return true;
        }
        if (observer.isNacao(nacao) || jogadorFacade.isAlly(nacao, observer)) {
            return false;
        }
        return true;
    }

    /**
     * Flood this one army's reach into the picture.
     *
     * <b>One flood per troop type, at TWICE the budget.</b> The cost of every hex comes back in the
     * {@code ArmyPath}, so a single run answers both turns: at or under one budget is turn 1, over it
     * is turn 2. Running twice would do the same work twice.
     * <p>
     * <b>Turn 2 is an upper bound and the UI says so.</b> Movement points do not carry over between
     * turns, so the exact two-turn reach is a second flood seeded from every hex turn 1 ends on.
     * {@code calculateArmyRangeHexes} is single-source and belongs to the Judge
     * ({@code Ordem851MoveNpcArmy} calls it), so this takes the doubled budget instead. That can only
     * over-state, which is the direction this whole class leans.
     */
    private static void addArmy(Map<Local, List<Threat>> byHex, Exercito army,
            SortedMap<String, Local> locais, Cenario cenario, int budget) {
        final Local origin = exercitoFacade.getLocal(army);
        if (origin == null) {
            return;
        }
        for (Map.Entry<Run, HashMap<Local, ArmyPath>> run
                : floodsFor(army, locais, cenario, budget * MAX_TURNS).entrySet()) {
            final String assumed = run.getKey().assumedTroop;
            for (Map.Entry<Local, ArmyPath> hit : run.getValue().entrySet()) {
                if (hit.getKey() == null || hit.getValue() == null || hit.getKey() == origin) {
                    continue;
                }
                final int turns = hit.getValue().getCost() <= budget ? 1 : MAX_TURNS;
                record(byHex, hit.getKey(), new Threat(army, turns, assumed));
            }
            // the hex it is standing on is reachable by definition, and a city under an army's nose
            // is the one the player most needs flagged
            record(byHex, origin, new Threat(army, 1, assumed));
        }
    }

    /**
     * Every hex this one army can reach within {@code limit} points, cheapest path per hex.
     *
     * <b>Public because {@code MapaControler.printTagArmyRange} needs it.</b> That feature draws the
     * reach of whatever army is selected in the Armies tab, and the tab's filter combo offers All
     * first and Enemies further down - so it can be pointed at an army with no visible composition,
     * and before this existed it drew the cost-1 circle described in this class's javadoc. Routing it
     * through here is what fixes that, and keeps the two from ever answering differently.
     * <p>
     * The BUDGET stays the caller's business. This answers "how far, for this many points"; the
     * threat picture asks with {@link #budgetPerTurn}'s worst case, while the Armies tab asks with
     * the plain-move allowance, which is the honest answer for the player's own army.
     */
    public static Map<Local, ArmyPath> rangeOf(Exercito army, SortedMap<String, Local> locais,
            Cenario cenario, int limit) {
        final Map<Local, ArmyPath> ret = new HashMap<>();
        if (army == null || locais == null || cenario == null
                || exercitoFacade.getLocal(army) == null) {
            return ret;
        }
        for (HashMap<Local, ArmyPath> reach : floodsFor(army, locais, cenario, limit).values()) {
            for (Map.Entry<Local, ArmyPath> hit : reach.entrySet()) {
                final ArmyPath seen = ret.get(hit.getKey());
                if (seen == null || hit.getValue().getCost() < seen.getCost()) {
                    //the union over troop types: whichever of them gets here fastest
                    ret.put(hit.getKey(), hit.getValue());
                }
            }
        }
        return ret;
    }

    /** One flood per {@link Run}, each built from the same assumptions. */
    private static Map<Run, HashMap<Local, ArmyPath>> floodsFor(Exercito army,
            SortedMap<String, Local> locais, Cenario cenario, int limit) {
        final Map<Run, HashMap<Local, ArmyPath>> ret = new HashMap<>();
        final Local origin = exercitoFacade.getLocal(army);
        final boolean visible = !army.getPelotoes().isEmpty();
        for (Run run : runsFor(army, cenario, visible)) {
            final MovimentoExercito movEx = new MovimentoExercito();
            movEx.addTropasAll(run.troops);
            movEx.setOrigem(origin);
            movEx.setPorAgua(run.byWater);
            // SUPPLY. An unscouted army exports comida 0, i.e. starving, which would cost it a
            // third of its speed through the x4/3 unfed penalty - a placeholder quietly
            // UNDER-stating reach. It does not, and the reason is worth knowing rather than
            // guarding against twice: ExercitoFacade.getComidaConsumo sums upkeep over the
            // PLATOONS, and an unscouted army has none, so consumption is 0, 0 >= 0 holds and
            // isComida already answers fed. That is an accident of the formula, not a decision,
            // so ThreatReachTest pins it - if consumption is ever estimated from the size band
            // instead, this silently gets slower and that test is what says so.
            movEx.setComida(exercitoFacade.isComida(army));
            // EVASIVE movement doubles every hex. The flag is real on an army we can see and a
            // placeholder on one we cannot, and unlike supply nothing else makes it safe: if the
            // server ever exported a true here, taking it at face value would halve the reach of
            // the armies we know least about.
            movEx.setEvasivo(visible && army.isMovimentacaoEvasiva());
            movEx.setLimiteMovimento(limit);
            ret.put(run, exercitoFacade.calculateArmyRangeHexes(locais, movEx));
        }
        return ret;
    }

    private static void record(Map<Local, List<Threat>> byHex, Local hex, Threat threat) {
        List<Threat> at = byHex.get(hex);
        if (at == null) {
            at = new ArrayList<>(2);
            byHex.put(hex, at);
        }
        at.add(threat);
    }

    /** One flood to run: a troop list, a medium, and the name to blame if it was assumed. */
    private static final class Run {

        private final List<TipoTropa> troops;
        private final boolean byWater;
        private final String assumedTroop;

        Run(List<TipoTropa> troops, boolean byWater, String assumedTroop) {
            this.troops = troops;
            this.byWater = byWater;
            this.assumedTroop = assumedTroop;
        }
    }

    /**
     * The floods this army needs.
     *
     * A visible army is run exactly as it stands, land troops and ships separately, which is what
     * {@code printTagArmyRange} already does. An invisible one is run once per NON-DOMINATED troop
     * its owner could field - see {@link #assumedRoster} and {@link #nonDominated}.
     * <p>
     * Land or sea comes from {@code tamanhoExercito} and {@code tamanhoEsquadra}, which the EGF
     * exports even unscouted, and NOT from {@code ExercitoFacade.isEsquadra}: that reads {@code ;TTN;}
     * out of the army's habilidades map, which arrives empty on anything the player has not scouted.
     * Both can be set, and then both floods run.
     */
    private static List<Run> runsFor(Exercito army, Cenario cenario, boolean visible) {
        final List<Run> ret = new ArrayList<>(2);
        if (visible) {
            final List<TipoTropa> land = exercitoFacade.getTipoTropasTerra(army);
            if (!land.isEmpty()) {
                ret.add(new Run(land, false, null));
            }
            final List<TipoTropa> sea = exercitoFacade.getTipoTropasAgua(army);
            if (!sea.isEmpty()) {
                ret.add(new Run(sea, true, null));
            }
            return ret;
        }
        final Collection<TipoTropa> roster = assumedRoster(army, cenario);
        if (army.getTamanhoExercito() > 0) {
            addAssumedRuns(ret, roster, cenario, false);
        }
        if (army.getTamanhoEsquadra() > 0) {
            addAssumedRuns(ret, roster, cenario, true);
        }
        if (ret.isEmpty()) {
            //nothing is known and nothing can be assumed: say nothing rather than guess a circle
            log.warn(String.format("No roster to assume for %s; no threat computed.", army.getNome()));
        }
        return ret;
    }

    private static void addAssumedRuns(List<Run> out, Collection<TipoTropa> roster, Cenario cenario,
            boolean byWater) {
        final List<TipoTropa> eligible = new ArrayList<>();
        for (TipoTropa troop : roster) {
            if (troop != null && troop.isBarcos() == byWater) {
                eligible.add(troop);
            }
        }
        for (TipoTropa troop : nonDominated(eligible, cenario)) {
            out.add(new Run(Collections.singletonList(troop), byWater, troop.getNome()));
        }
    }

    /**
     * What an army we cannot see into could be made of: its owner's race roster, falling back to the
     * whole scenario catalogue.
     *
     * Exactly the precedent {@code WhatIfSearch} already sets for the same question about combat
     * STRENGTH, so the two halves of "what might that enemy be" cannot drift apart.
     */
    static Collection<TipoTropa> assumedRoster(Exercito army, Cenario cenario) {
        try {
            if (army.getNacao() != null && army.getNacao().getRaca() != null
                    && !army.getNacao().getRaca().getTropas().isEmpty()) {
                return army.getNacao().getRaca().getTropas().keySet();
            }
        } catch (RuntimeException ex) {
            log.warn("Race roster unreadable, falling back to the catalogue: " + ex);
        }
        return cenario.getTipoTropas().values();
    }

    /**
     * The troop types worth flooding: those no other type beats everywhere.
     *
     * <b>Not "the fastest troop".</b> Speed is terrain-dependent and rides habilidades -
     * {@code ;TTF;} and {@code ;TTR;} halve road cost, {@code ;T2M;} and {@code ;TM2;} halve
     * mountains, and {@code isMoveMountain} decides whether mountain-to-mountain is possible at all.
     * A type chosen by one scalar is slower than some other type on some terrain, and the reach
     * computed from it would UNDER-state on exactly those paths. The union over the non-dominated
     * types is the real worst case over compositions this owner could actually field.
     * <p>
     * Cheap: rosters run 4 to 7 types over 5 to 10 terrains, so this is a few hundred lookups and
     * leaves 1 to 3 types to flood.
     */
    static List<TipoTropa> nonDominated(List<TipoTropa> troops, Cenario cenario) {
        if (troops.size() <= 1) {
            return troops;
        }
        final List<Terreno> terrains = new ArrayList<>(cenario.getTerrenos().values());
        final Map<TipoTropa, int[]> vectors = new HashMap<>();
        for (TipoTropa troop : troops) {
            vectors.put(troop, costVector(troop, terrains));
        }
        final List<TipoTropa> ret = new ArrayList<>(troops.size());
        for (TipoTropa troop : troops) {
            boolean beaten = false;
            for (TipoTropa other : troops) {
                if (other != troop && dominates(vectors.get(other), vectors.get(troop))) {
                    beaten = true;
                    break;
                }
            }
            if (!beaten) {
                ret.add(troop);
            }
        }
        return ret.isEmpty() ? troops : ret;
    }

    /**
     * This troop's cost on every terrain, off road and on, plus its mountain-crossing flag.
     *
     * The terrain lookup is checked for a MISSING entry rather than left to
     * {@code getCustoMovimentoBase}, whose loop is wrapped in {@code catch (NullPointerException)}
     * and would return its initial 1 - a troop with no entry for a terrain would then look like the
     * fastest thing on the map and dominate everything that can really walk there. Missing reads as
     * impassable here, which is the safe direction for a DOMINANCE test: it keeps the troop in play
     * rather than letting it silently remove the real contenders.
     */
    private static int[] costVector(TipoTropa troop, List<Terreno> terrains) {
        final int[] ret = new int[terrains.size() * 2 + 1];
        int ii = 0;
        for (Terreno terreno : terrains) {
            for (boolean road : new boolean[]{false, true}) {
                final Integer base = troop.getMovimentoTerreno().get(terreno);
                ret[ii++] = base == null || base == 0 ? IMPASSABLE
                        : exercitoFacade.getCustoMovimentoBase(
                                Collections.singletonList(troop), terreno, road, troop.isBarcos());
            }
        }
        //a troop that can cross mountain to mountain is never beaten by one that cannot
        ret[ii] = troop.isMoveMountain() ? 0 : 1;
        return ret;
    }

    private static final int IMPASSABLE = 9999;

    /** True when {@code a} is at least as cheap as {@code b} everywhere. */
    private static boolean dominates(int[] a, int[] b) {
        if (a == null || b == null) {
            return false;
        }
        for (int ii = 0; ii < a.length; ii++) {
            if (a[ii] > b[ii]) {
                return false;
            }
        }
        return true;
    }
}
