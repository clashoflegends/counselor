/*
 * ThreatReach.java
 */
package control.services;

import business.ArmyPath;
import business.MovimentoExercito;
import business.facade.ExercitoFacade;
import business.facade.JogadorFacade;
import business.combat.HostilityDeriver;
import business.combat.RelationshipMatrix;
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
import model.Partida;
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
public final class ThreatReach {

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
            for (List<Threat> at : byHex.values()) {
                //soonest first, once, so a row can lead with the worst news without the renderer
                //sorting a shared list on every paint
                Collections.sort(at);
            }
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
            //read-only, and sorted once when the picture was built. This is called from a table cell
            //renderer - once per cell per repaint - and it used to sort the STORED list in place and
            //hand back a live reference to it every time.
            return Collections.unmodifiableList(ret);
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
     * <b>Lazy, but in practice it always runs.</b> Nothing is computed until something asks - and the
     * Cities tab always asks, because the Threat column is unconditional and {@code TabCidadesGui}
     * builds its model in its constructor. So every player pays for it on every EGF open whether or
     * not he has ever switched the map layer on, and that layer's own setting does not gate it. That
     * is a deliberate trade and the number behind it is 71ms on a 13-player game with 3,071 hexes and
     * 9 hostile armies, which is lost in the noise of opening a turn file - but it is a measurement
     * on ONE file, and a game with ten times the hostile armies has not been measured.
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
            SortedMap<String, Local> locais, Partida partida, int armyMoveMaxPoints) {
        Coverage ret = current;
        if (ret == null) {
            ret = compute(armies, observer, locais, partida, armyMoveMaxPoints);
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
            SortedMap<String, Local> locais, Partida partida, int armyMoveMaxPoints) {
        final Map<Local, List<Threat>> byHex = new HashMap<>();
        final Cenario cenario = partida == null ? null : partida.getCenario();
        if (armies == null || locais == null || cenario == null) {
            return new Coverage(byHex);
        }
        final int budget = budgetPerTurn(armyMoveMaxPoints);
        final RelationshipMatrix relations = relationsFor(armies, observer, partida);
        for (Exercito army : armies) {
            // One unreadable army must never cost the whole picture. A half-built threat map that
            // silently omits the army next door is worse than one that logs and carries on.
            try {
                if (!isHostile(army, observer, relations)) {
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
     * The relationship table for every nation with an army on the board, or null when it cannot be
     * built.
     *
     * {@code HostilityDeriver} is the shared answer to "will these two fight" and it is the one that
     * knows about TEAM FLAGS. That matters here: {@code Jogador.isJogadorAliado} decides an alliance
     * from {@code nacaoAlvo.getRelacionamento(mine)} - the OTHER nation view of me - and the EGF
     * carries a foreign nation row only when it is positive and aimed at the player. In a locked-team
     * game those rows may say nothing at all, while {@code Nacao.getTeamFlag} ships for every nation
     * and settles the whole table by construction ({@code NacaoControl.doCarregaRelacionamentosFresh}:
     * same team ALLY, different team SWORN_ENEMY). Without it every TEAMMATE army parked near the
     * player cities would come back red.
     */
    private static RelationshipMatrix relationsFor(Collection<Exercito> armies, Jogador observer,
            Partida partida) {
        try {
            final List<Nacao> nacoes = new ArrayList<>();
            for (Exercito army : armies) {
                if (army != null && army.getNacao() != null && !nacoes.contains(army.getNacao())) {
                    nacoes.add(army.getNacao());
                }
            }
            for (Nacao mine : observer.getNacoes().values()) {
                if (!nacoes.contains(mine)) {
                    nacoes.add(mine);
                }
            }
            return new HostilityDeriver().deriveNations(partida, nacoes, observer);
        } catch (RuntimeException ex) {
            //a threat picture with a cruder hostility test beats no threat picture
            log.warn("Relationship matrix unavailable, falling back to the ally test: " + ex);
            return null;
        }
    }

    /**
     * Whose armies count.
     *
     * <b>Everything that is not a declared friend.</b> NOT {@code RelationshipMatrix.isHostile},
     * which asks whether two nations will FIGHT and so needs a negative relationship: at NEUTRAL it
     * answers no, and a neutral nation can march on a city next turn as easily as a sworn enemy can.
     * The question here is not "are we at war" but "can that reach me", so the bar is FRIEND or
     * better and everything below it counts.
     * <p>
     * Read off the player OWN row ({@code mine -> theirs}) and not the OR of both directions. His own
     * rows are the complete ones; the reverse direction arrives only when a foreign nation has
     * declared something positive, so reading it could only ever clear a threat the client cannot
     * actually prove is cleared.
     * <p>
     * The matrix rather than {@code Jogador.isJogadorAliado} because of TEAM FLAGS. That method
     * decides an alliance from {@code nacaoAlvo.getRelacionamento(mine)}, the other nation view of
     * me, which in a locked-team game may be empty - while {@code HostilityDeriver} folds in
     * {@code getTeamFlag}, which ships for every nation in every EGF. Without it every TEAMMATE army
     * parked near the player cities would come back red. The fallback, for when the matrix cannot be
     * built at all, is the cruder test the Armies tab filter uses.
     * <p>
     * An army with NO nation counts either way, and that is the one place this deliberately parts
     * company with that filter: it wraps the whole test in {@code catch (NullPointerException)} and
     * drops such an army silently. An unscouted army can arrive exactly that way, and dropping the
     * ones we know least about is backwards for a threat display.
     * <p>
     * A NEUTRAL nation counts as hostile. That is a choice, not an oversight: neutral is where an NPC
     * horde and a nation that simply has not declared yet both sit, and neither is a reason to leave a
     * city unmarked.
     */
    public static boolean isHostile(Exercito army, Jogador observer) {
        return isHostile(army, observer, null);
    }

    static boolean isHostile(Exercito army, Jogador observer, RelationshipMatrix relations) {
        if (army == null || observer == null) {
            return false;
        }
        final Nacao nacao = army.getNacao();
        if (nacao == null) {
            return true;
        }
        if (observer.isNacao(nacao)) {
            return false;
        }
        if (relations != null) {
            // EVERY own nation must be on friendly terms before this is cleared. An OR over them
            // meant a player holding two nations, one with a treaty with X and one at war with X,
            // lost X from the whole picture - including from the cities of the nation at war with
            // it. Four or more nations per player is an ordinary case here.
            for (Nacao mine : observer.getNacoes().values()) {
                if (relations.getValor(mine, nacao) < RelationshipMatrix.FRIEND) {
                    return true;
                }
            }
            return !observer.getNacoes().isEmpty();
        }
        // Fallback, for when the matrix cannot be built at all. Per own nation for the same reason
        // the matrix path is: Jogador.isJogadorAliado ORs over the player's nations, so a treaty
        // signed by one of them cleared the threat for all of them. Same DIRECTION it uses though -
        // theirs towards mine - because that is the one the EGF exports, and only when positive.
        for (Nacao mine : observer.getNacoes().values()) {
            if (nacao.getRelacionamento(mine) < RelationshipMatrix.FRIEND) {
                return true;
            }
        }
        return !observer.getNacoes().isEmpty();
    }

    /**
     * Flood this one army's reach into the picture.
     *
     * <b>One flood per troop type, at TWICE the budget.</b> The cost of every hex comes back in the
     * {@code ArmyPath}, so a single run answers both turns: at or under one budget is turn 1, over it
     * is turn 2. Running twice would do the same work twice.
     * <p>
     * <b>Turn 2 for a LAND force is an upper bound and the UI says so.</b> Movement points do not
     * carry over between turns, so the exact two-turn reach is a second flood seeded from every hex
     * turn 1 ends on. {@code calculateArmyRangeHexes} is single-source and belongs to the Judge
     * ({@code Ordem851MoveNpcArmy} calls it), so a land force takes the doubled budget instead,
     * which can only over-state.
     * <p>
     * <b>For a fleet the doubled budget does not over-state, it MISSES</b>, and
     * {@link #addMarchAfterLanding} is why that needed its own pass rather than a bigger number.
     */
    private static void addArmy(Map<Local, List<Threat>> byHex, Exercito army,
            SortedMap<String, Local> locais, Cenario cenario, int budget) {
        final Local origin = exercitoFacade.getLocal(army);
        if (origin == null) {
            return;
        }
        // The hex it is standing on is reachable by definition, and a city under an army's nose is
        // the one the player most needs flagged.
        //
        // Outside the run loop, though runsFor now always returns at least one run, so this is belt
        // and braces rather than the fix: the bug was an army with BOTH size bands at zero producing
        // no runs at all, so the loop body never executed and it vanished from the picture
        // completely - including from the hex it was sitting on. What actually closes that is the
        // land fallback in runsFor, and anArmyWithNoSizeBandStillMarksWhereItStands is what fails if
        // either half is removed.
        record(byHex, origin, new Threat(army, 1, null));
        for (Run run : runsFor(army, cenario, !army.getPelotoes().isEmpty())) {
            // TWO floods, one per turn, rather than one at the doubled budget classified by cost.
            // A LANDING costs whatever points are left under the limit (ExercitoFacade:704-707), so
            // its total always EQUALS the limit, whatever the limit is - at a doubled budget every
            // landing would come out over one turn worth of points, and a fleet one hex off the
            // player port would be drawn as two turns away. Under-warning a coastal city is the one
            // direction this must never go.
            final Map<Local, ArmyPath> soon = flood(army, run, origin, locais, budget);
            final Map<Local, ArmyPath> later = flood(army, run, origin, locais, budget * MAX_TURNS);
            for (Local hex : later.keySet()) {
                if (hex == null || hex == origin) {
                    continue;
                }
                record(byHex, hex, new Threat(army, soon.containsKey(hex) ? 1 : MAX_TURNS,
                        run.assumedTroop));
            }
            if (run.byWater) {
                addMarchAfterLanding(byHex, army, cenario, locais, budget, soon.keySet());
            }
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
        final Local origin = exercitoFacade.getLocal(army);
        for (Run run : runsFor(army, cenario, !army.getPelotoes().isEmpty())) {
            for (Map.Entry<Local, ArmyPath> hit
                    : flood(army, run, origin, locais, limit).entrySet()) {
                final ArmyPath seen = ret.get(hit.getKey());
                if (seen == null || hit.getValue().getCost() < seen.getCost()) {
                    //the union over troop types: whichever of them gets here fastest
                    ret.put(hit.getKey(), hit.getValue());
                }
            }
        }
        return ret;
    }

    /** One flood, built from the same assumptions for every caller. */
    private static Map<Local, ArmyPath> flood(Exercito army, Run run, Local origin,
            SortedMap<String, Local> locais, int limit) {
        final MovimentoExercito movEx = new MovimentoExercito();
        movEx.addTropasAll(run.troops);
        movEx.setOrigem(origin);
        movEx.setPorAgua(run.byWater);
        // SUPPLY. An unscouted army exports comida 0, i.e. starving, which would cost it a third of
        // its speed through the x4/3 unfed penalty - a placeholder quietly UNDER-stating reach. It
        // does not, and the reason is worth knowing rather than guarding against twice:
        // ExercitoFacade.getComidaConsumo sums upkeep over the PLATOONS, and an unscouted army has
        // none, so consumption is 0, 0 >= 0 holds and isComida already answers fed. That is an
        // accident of the formula, not a decision, so ThreatReachTest pins it - if consumption is
        // ever estimated from the size band instead, this silently gets slower and that test says so.
        movEx.setComida(exercitoFacade.isComida(army));
        // EVASIVE movement doubles every hex, and the flag is NEVER believed - not even on an army
        // whose platoons we can count.
        //
        // It is not a property of the army, it is a record of the order it issued LAST turn:
        // ExercitoControl sets "NORMAL" when an army is raised, OrdemBaseMovParent overwrites it
        // from the trailing ev/nr parameter of whatever move was ordered, and the server stores it
        // in exercito.tp_movimentacao for the client to read back. An enemy that crept evasively
        // last turn only has to type nr to move at full speed next turn, so charging it double was
        // an UNDER-warning on exactly the armies the player has counted and trusts the column about.
        // A scouted stack five plains hexes from a capital came out beyond both bands.
        //
        // False is also what printTagArmyRange hardcoded before it shared this flood, so the Armies
        // tab keeps the range it has always drawn.
        movEx.setEvasivo(false);
        // MOUNTAINS. getCustoMovimentacaoTerra refuses mountain-to-mountain without a road unless
        // the MOVEMENT allows it or every troop carries ;TTM;, and that movement flag defaults to
        // TRUE - so leaving it unset was allowing it for everyone by accident. Both Judge call sites
        // set it from the army; this sets it so the worst case is a decision instead, because a
        // client cannot see the scenario rule or a mountain-walking item and drawing a range as
        // impassable when it is not would under-warn.
        //
        // No test can fail on this line alone - it writes the value the field already held - so it
        // is intent, not a guard. mountainCrossingIsAllowedOnPurpose pins the BEHAVIOUR, which is
        // what would break if the default ever changed underneath.
        movEx.setMoveMountain(true);
        movEx.setLimiteMovimento(limit);
        return exercitoFacade.calculateArmyRangeHexes(locais, movEx);
    }

    /**
     * March inland from wherever the fleet can put troops ashore this turn.
     *
     * <b>Without this the turn-2 band for a fleet is "sail twice as far, then land", never "land,
     * then march"</b> - and the second is the invasion. Two rules in the shared flood conspire:
     * {@code ExercitoFacade} spends EVERY remaining point on the step ashore, so a landing hex
     * always totals exactly the limit and nothing can follow it; and the matching land flood returns
     * empty immediately, because a land army is not allowed to start on water and the fleet's origin
     * IS a water hex. So a hostile fleet two hexes off the coast marked the port and left a city
     * three plains hexes inland - well inside one turn's march - unmarked altogether. That is the
     * case a coastal player opens this column for.
     * <p>
     * One land flood per landing hex, at ONE turn's budget, recorded at turn 2: the Judge has no
     * restriction here either, turn 1 is Move Navy and turn 2 is a plain move or a Forced March. The
     * troops are the army's own where we can count them, and its owner's assumed LAND roster where
     * we cannot - {@code MilestoneCalculos} gives every army a land band, so a fleet is assumed to
     * be carrying something.
     * <p>
     * Capped, and the cap is an admitted under-warning: a fleet with a very long turn-1 reach can
     * touch more shore than is worth flooding from, and a bounded, logged truncation beats an
     * unbounded cost on the EGF-open path. The landing hexes are sorted so the cap is deterministic
     * rather than whatever order the hash map happened to produce.
     */
    private static void addMarchAfterLanding(Map<Local, List<Threat>> byHex, Exercito army,
            Cenario cenario, SortedMap<String, Local> locais, int budget, Collection<Local> reached) {
        final List<Local> shore = new ArrayList<>();
        for (Local hex : reached) {
            if (hex != null && hex.getTerreno() != null && !hex.getTerreno().isAgua()) {
                shore.add(hex);
            }
        }
        if (shore.isEmpty()) {
            return;
        }
        Collections.sort(shore);
        if (shore.size() > MAX_LANDINGS) {
            log.warn(String.format("%s can land on %d hexes; marching inland from the first %d only.",
                    army.getNome(), shore.size(), MAX_LANDINGS));
        }
        final List<Run> ashore = landRunsFor(army, cenario);
        final int limit = Math.min(shore.size(), MAX_LANDINGS);
        for (int ii = 0; ii < limit; ii++) {
            for (Run run : ashore) {
                for (Local hex : flood(army, run, shore.get(ii), locais, budget).keySet()) {
                    if (hex != null) {
                        record(byHex, hex, new Threat(army, MAX_TURNS, run.assumedTroop));
                    }
                }
            }
        }
    }

    /** Bounds the inland march: see {@link #addMarchAfterLanding}. */
    private static final int MAX_LANDINGS = 24;

    /** What the fleet puts ashore: its own land troops, or the owner's assumed land roster. */
    private static List<Run> landRunsFor(Exercito army, Cenario cenario) {
        final List<Run> ret = new ArrayList<>(2);
        final List<TipoTropa> own = exercitoFacade.getTipoTropasTerra(army);
        if (!own.isEmpty()) {
            ret.add(new Run(own, false, null));
            return ret;
        }
        addAssumedRuns(ret, assumedRoster(army, cenario), cenario, false);
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
        if (army.getTamanhoEsquadra() > 0) {
            addAssumedRuns(ret, roster, cenario, true);
        }
        if (army.getTamanhoExercito() > 0 || ret.isEmpty()) {
            //Both bands at zero means "not even estimated", not "nothing there" - 11 rows across
            //277,953 in ex_exercito. An army that is on the map is an army, so it gets the land
            //assumption rather than vanishing from the picture entirely.
            addAssumedRuns(ret, roster, cenario, false);
        }
        return ret;
    }

    private static void addAssumedRuns(List<Run> out, Collection<TipoTropa> roster, Cenario cenario,
            boolean byWater) {
        List<TipoTropa> eligible = ofMedium(roster, byWater);
        if (eligible.isEmpty()) {
            // The race roster is only half of what a nation can field: SHIPS are granted by nation
            // POWERS, not by race. ;PCC;, ;PTC;, ;PKR;, ;XDG;, ;XRR; and ;XSL; all hand out hulls,
            // and the standard seven-troop roster (;cp;cl;ip;il;ar;me;wm;) carries no ;TTN; at all -
            // so for most nations the race list has no ship in it and this filter came back empty.
            // There was no second fallback, so the sea Run was never created and an unscouted FLEET
            // drew no naval threat whatever: every coastal city read clean. Falling back per medium
            // rather than only when the whole roster is empty is what closes that.
            eligible = ofMedium(cenario.getTipoTropas().values(), byWater);
        }
        for (TipoTropa troop : nonDominated(eligible, cenario)) {
            out.add(new Run(Collections.singletonList(troop), byWater, troop.getNome()));
        }
    }

    private static List<TipoTropa> ofMedium(Collection<TipoTropa> troops, boolean byWater) {
        final List<TipoTropa> ret = new ArrayList<>();
        for (TipoTropa troop : troops) {
            if (troop != null && troop.isBarcos() == byWater) {
                ret.add(troop);
            }
        }
        return ret;
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
     * mountains. A type chosen by one scalar is slower than some other type on some terrain, and the
     * reach computed from it would UNDER-state on exactly those paths. The union over the
     * non-dominated types is the real worst case over compositions this owner could field.
     *
     * Cheap: rosters run 4 to 7 types over 5 to 10 terrains, so this is a few hundred lookups, and
     * after tie-breaking the standard roster collapses to two floods.
     */
    static List<TipoTropa> nonDominated(List<TipoTropa> troops, Cenario cenario) {
        if (troops.size() <= 1) {
            return troops;
        }
        final List<Terreno> terrains = new ArrayList<>(cenario.getTerrenos().values());
        final List<int[]> vectors = new ArrayList<>(troops.size());
        for (TipoTropa troop : troops) {
            vectors.add(costVector(troop, terrains));
        }
        final List<TipoTropa> ret = new ArrayList<>(troops.size());
        for (int ii = 0; ii < troops.size(); ii++) {
            boolean beaten = false;
            for (int jj = 0; jj < troops.size() && !beaten; jj++) {
                if (ii == jj || !dominates(vectors.get(jj), vectors.get(ii))) {
                    continue;
                }
                // TIES. Two troops with IDENTICAL vectors each dominate the other, so taken
                // literally both are beaten and the whole list came back empty - which then fell
                // through to the catch-all below and flooded EVERY type. That is not an edge case:
                // the standard roster ;cp;cl;ip;il;ar;me;wm; is two identical classes (cp=cl, and
                // ip=il=ar=me=wm), so seven floods ran where two would do. Keep the first member of
                // each class and drop the rest.
                if (dominates(vectors.get(ii), vectors.get(jj)) && jj > ii) {
                    continue;
                }
                beaten = true;
            }
            if (!beaten) {
                ret.add(troops.get(ii));
            }
        }
        //unreachable now that ties are broken by index - the first member of a maximal class always
        //survives - but a dominance bug must never silence the whole threat, so it is kept
        return ret.isEmpty() ? troops : ret;
    }

    /**
     * This troop's cost on every terrain, off road and on.
     *
     * <b>It calls the same function the flood calls, with no guard of its own.</b> An earlier
     * version substituted a large sentinel for a terrain the troop has no entry for, reasoning that
     * missing should read as impassable. That was backwards twice over:
     * {@code getCustoMovimentoBase} swallows the resulting NPE and keeps its initial 1, so in the
     * REAL flood a missing entry is the CHEAPEST thing on the map; and in {@code dominates} a large
     * number in a troop's own slot makes that troop easier to beat, not harder, so the sentinel
     * dropped exactly the troop that would have gone furthest. The vector has to be the flood's cost
     * function or the filter is answering a different question from the one that matters.
     */
    private static int[] costVector(TipoTropa troop, List<Terreno> terrains) {
        final int[] ret = new int[terrains.size() * 2];
        int ii = 0;
        for (Terreno terreno : terrains) {
            for (boolean road : new boolean[]{false, true}) {
                ret[ii++] = exercitoFacade.getCustoMovimentoBase(
                        Collections.singletonList(troop), terreno, road, troop.isBarcos());
            }
        }
        return ret;
    }

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
