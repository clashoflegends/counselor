package control.services;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CasualtyMode;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.RosterDerivation;
import business.combat.RunGate;
import business.combat.ScenarioRoster;
import java.util.ArrayList;
import business.facade.ExercitoFacade;
import java.util.List;
import model.Cenario;
import model.Pelotao;
import msgs.BaseMsgs;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * Everything BattleSim puts on screen as words.
 *
 * The model side answers in enums and counts on purpose - {@code ScenarioRoster} says
 * FIGHTING_AGAINST_ME, {@code RosterDerivation} says PARTLY_ASSUMED and a number - so that the rules
 * can be tested without a bundle and without a window. This is where those become a sentence, and it
 * is the only place in the BattleSim that touches {@code labels.properties}.
 *
 * Every string comes from the bundle. A hardcoded English word here would be invisible until a
 * Portuguese player opened the window.
 */
public class BattleSimConverter {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    /**
     * The land layer resolves now (T-801), so Run can be enabled. Still ONE constant: the sea and
     * city layers are not here yet, and when they arrive this stays exactly where it is.
     */
    private static final boolean ENGINE_EXISTS = true;

    private BattleSimConverter() {
    }

    /** The roster node's title, as in "Fighting against me". */
    public static String getGroupName(ScenarioRoster.Group group) {
        switch (group) {
            case MINE:
                return labels.getString("BATTLESIM.GROUP.MINE");
            case FIGHTING_WITH_ME:
                return labels.getString("BATTLESIM.GROUP.WITH");
            case FIGHTING_AGAINST_ME:
                return labels.getString("BATTLESIM.GROUP.AGAINST");
            default:
                return labels.getString("BATTLESIM.GROUP.OUT");
        }
    }

    /** "Fighting against me (2,300)". The running troop total is the wireframe's right column. */
    public static String getGroupTitle(ScenarioRoster roster, ScenarioRoster.Group group) {
        return String.format("%s (%,d)", getGroupName(group), roster.getQtTropas(group));
    }

    public static String getLayerName(CombatLayer layer) {
        switch (layer) {
            case NAVY:
                return labels.getString("BATTLESIM.LAYER.NAVY");
            case ARMY:
                return labels.getString("BATTLESIM.LAYER.ARMY");
            default:
                return labels.getString("BATTLESIM.LAYER.CITY");
        }
    }

    /**
     * The army editor's participation block: a summary line, then ALL THREE layers, each answered.
     *
     * <h3>Three answers, never one</h3>
     *
     * John, 2026-09-19: "The participation is layer by layer. N A C. There are enough edge cases
     * that there is always a situation that someone will participate of 0 to 3 of them."
     *
     * This used to print one reason for the whole army - the first non-FIGHTS answer, asked of the
     * land layer first - and that was a true sentence standing in for three different ones. Live
     * example from hex 1141: four Greyjoy armies sitting on a Tully city read "Takes no part in
     * this battle (no enemy present)". Perfectly true of the LAND layer, and it buried the answer
     * the player actually needed - the city layer said "not ordered to assault", which he fixes by
     * changing Combat level to Attack city. The one thing the line exists to tell him was the one
     * thing it hid.
     *
     * So every layer states its own case, in the N A C order the Judge resolves them, including the
     * ones it fights in. An army in no layer is still the case a player opens the simulator to ask
     * about - a fleet that will not defend his city, a garrison that will not sortie - and now he
     * sees which of the three tests stopped it and can act on that one.
     */
    public static String getFightsIn(LayerParticipation participation) {
        if (participation == null) {
            return "";
        }
        // HTML, because the answer is four short lines rather than one long one.
        final List<CombatLayer> layers = participation.getLayers();
        final StringBuilder ret = new StringBuilder("<html>");
        if (layers.isEmpty()) {
            ret.append(labels.getString("BATTLESIM.FIGHTS.NONE"));
        } else {
            final StringBuilder named = new StringBuilder();
            for (CombatLayer layer : layers) {
                if (named.length() > 0) {
                    named.append(", ");
                }
                named.append(getLayerName(layer));
            }
            ret.append(String.format(labels.getString("BATTLESIM.FIGHTS.IN"), named.toString()));
        }
        // then ALL THREE layers, in the N A C order the Judge resolves them, each with its own
        // answer. Never one sentence for the whole army: see the method javadoc.
        for (CombatLayer layer : new CombatLayer[]{CombatLayer.NAVY, CombatLayer.ARMY,
            CombatLayer.CITY}) {
            ret.append("<br>&nbsp;&nbsp;").append(getLayerName(layer)).append(": ")
                    .append(getReasonName(participation.getReason(layer)));
        }
        return ret.append("</html>").toString();
    }

    /** One layer's answer: "takes part", or the single test that stopped it. */
    private static String getReasonName(LayerParticipation.Reason reason) {
        if (reason == null || reason == LayerParticipation.Reason.FIGHTS) {
            return labels.getString("BATTLESIM.FIGHTS.YES");
        }
        return labels.getString("BATTLESIM.REASON." + reason.name());
    }

    public static String getCombatLevelName(CombatLevel level) {
        return labels.getString("BATTLESIM.LEVEL." + level.name());
    }

    public static String getProvenanceName(CombatScenario.Provenance provenance) {
        return labels.getString("BATTLESIM.PROVENANCE." + provenance.name());
    }

    /**
     * One of the seven relationship steps, named: "At war enemy", "Ally", "Lord"...
     *
     * From {@code BaseMsgs.nacaoRelacionamento}, the array the rest of the game already reads with
     * the same {@code + 2} offset, so the diplomacy panel invents no vocabulary of its own and a
     * translator has nothing new to translate. Clamped rather than trusted: the array is seven long
     * and the value arrives from an EGF, and an out-of-range index here would take the panel down
     * instead of showing a row.
     */
    public static String getRelationshipName(int valor) {
        final int index = Math.max(0, Math.min(BaseMsgs.nacaoRelacionamento.length - 1, valor + 2));
        return BaseMsgs.nacaoRelacionamento[index];
    }

    /**
     * The status bar: how this scenario decided who fights whom, and how much of it was guessed.
     *
     * R-15 lives here. A guessed peace looks exactly like a known one once it reaches a number, so
     * the count of unresolved pairs is stated rather than left to a tooltip - and the player's own
     * edits are stated too, because a scenario he has adjusted is no longer the one his EGF
     * describes and he should not read the result as if it were.
     */
    public static String getDerivationText(CombatScenario scenario) {
        final RosterDerivation derivation = RosterDerivation.of(scenario);
        final StringBuilder ret = new StringBuilder();
        // Saying "read from your EGF" says nothing: the EGF is the ONLY source of information the
        // Counselor has, so every number on this screen came from it. The status bar is for what
        // the player could NOT be told - pairs nothing could resolve, and his own overrides. When
        // there is neither, it stays quiet rather than reassuring him about the obvious.
        if (derivation.getAssumedPairs() > 0) {
            ret.append(String.format(labels.getString("BATTLESIM.STATUS.ASSUMED"),
                    derivation.getAssumedPairs()));
        }
        final int edited = scenario == null ? 0 : scenario.getEditedCount();
        if (edited > 0) {
            if (ret.length() > 0) {
                ret.append("   ");
            }
            ret.append(String.format(labels.getString("BATTLESIM.STATUS.EDITED"), edited));
        }
        return ret.toString();
    }

    /**
     * Why Run is disabled, for the status bar rather than a tooltip. R-40.
     *
     * A tooltip on a disabled button is unreliable across platforms, and this is the one message the
     * player most needs when nothing happens - which is the complaint the whole rebuild started
     * from.
     *
     * The decision is {@link RunGate}'s, in PbmCommons, where it can be tested without a bundle.
     * This only names it, and the label keys are the enum constants, so a new state cannot be added
     * without its sentence. {@link RunGate#READY} maps to an EMPTY string: when Run works, the
     * status bar has nothing to explain and says nothing.
     */
    public static String getRunDisabledReason(CombatScenario scenario) {
        return labels.getString(scenario == null
                ? "BATTLESIM.RUN.DISABLED.NO_ARMIES"
                : "BATTLESIM.RUN." + (scenario.getRunGate(ENGINE_EXISTS) == RunGate.READY
                        ? "READY" : "DISABLED." + scenario.getRunGate(ENGINE_EXISTS).name()));
    }

    /**
     * Whether Run may be enabled at all.
     *
     * One constant, one call site. T-801 landed the land layer and flipped it; the sea and city
     * layers do not get a second flag, because a scenario that engages on one layer is runnable and
     * the result says which layers it resolved.
     */
    public static boolean isRunnable(CombatScenario scenario) {
        return scenario != null && scenario.getRunGate(ENGINE_EXISTS).isRunnable();
    }

    /**
     * Every army and platoon as tab-separated text, for the clipboard. T-430.
     *
     * Ported from the old window with its two defects fixed.
     *
     * <b>Land and naval were SWAPPED against their headers.</b>
     * {@code getAtaqueExercito(army, true)} sat under "Land attack", and {@code true} means NAVAL:
     * {@code BattleSimFacade.getArmyAttack} tests {@code naval == tipoTropa.isBarcos()}. The old
     * army TABLE had it right, so only anyone who pasted the export was misled - quietly, since
     * both columns are plausible numbers.
     *
     * <b>The headers were hardcoded English</b> in a feature where every other string comes from
     * the bundle. They come from the bundle now, reusing the keys the army table already uses.
     */
    public static String getClipboardText(CombatScenario scenario) {
        final StringBuilder ret = new StringBuilder();
        appendRow(ret, labels.getString("COMANDANTE"), labels.getString("NACAO"),
                labels.getString("MORAL"), labels.getString("TROPA.ATAQUE.TERRA"),
                labels.getString("TROPA.DEFESA.TERRA"), labels.getString("TROPA.ATAQUE.NAVAL"),
                labels.getString("TROPA.DEFESA.NAVAL"));
        final ExercitoFacade facade = new ExercitoFacade();
        for (ArmySim army : scenario.getArmies()) {
            appendRow(ret, army.getNome(),
                    army.getNacao() == null ? "" : String.valueOf(army.getNacao().getNome()),
                    String.valueOf(army.getMoral()),
                    // false is LAND, true is NAVAL. The old export had these the other way round.
                    String.valueOf(facade.getAtaqueExercito(army, false)),
                    String.valueOf(facade.getDefesaExercito(army, false)),
                    String.valueOf(facade.getAtaqueExercito(army, true)),
                    String.valueOf(facade.getDefesaExercito(army, true)));
            for (Pelotao pelotao : new control.BattleSimControler.PlatoonTableModel(
                    scenario, army).getPlatoons()) {
                appendRow(ret, "", pelotao.getTipoTropa() == null ? ""
                        : String.valueOf(pelotao.getTipoTropa().getNome()),
                        String.valueOf(pelotao.getQtd()), String.valueOf(pelotao.getTreino()),
                        String.valueOf(pelotao.getModAtaque()),
                        String.valueOf(pelotao.getModDefesa()), "");
            }
        }
        return ret.toString();
    }

    private static void appendRow(StringBuilder to, String... cells) {
        for (int ii = 0; ii < cells.length; ii++) {
            if (ii > 0) {
                to.append('\t');
            }
            to.append(cells[ii]);
        }
        to.append('\n');
    }

    /**
     * What the platoon table's ROW ORDER actually means for this army. T-437.
     *
     * The table is sorted, and a sorted list of rows looks like a sequence whether or not one
     * exists - so when it does not, this says so rather than letting the player read a ranking into
     * it. John, 2026-09-20: "standard tactics splits the damage across all platoons equally, no
     * specific sequence like the other tactics. So not to mislead players, it hid the sequence."
     *
     * The old window hid the whole troop list instead, which was honest about the order and
     * explained nothing. Saying it in words keeps the list, which the player still needs.
     *
     * Covers all three no-sequence cases, not just the obvious one - see {@link CasualtyMode}. An
     * army with ships gets the extra line, because naval casualties rank even when its land
     * casualties do not.
     */
    public static String getCasualtyModeText(ArmySim army, Cenario cenario) {
        if (army == null) {
            return "";
        }
        final CasualtyMode land = CasualtyMode.of(army, cenario, CombatLayer.ARMY);
        final StringBuilder ret = new StringBuilder("<html>");
        if (land.isSequenced()) {
            ret.append(labels.getString("BATTLESIM.CASUALTY.RANKED"));
        } else if (army.getTatica() == CasualtyMode.TATICA_STANDARD) {
            ret.append(labels.getString("BATTLESIM.CASUALTY.STANDARD"));
        } else {
            ret.append(labels.getString("BATTLESIM.CASUALTY.NOTACTICS"));
        }
        if (!land.isSequenced() && hasShips(army)) {
            ret.append("<br>").append(labels.getString("BATTLESIM.CASUALTY.SHIPSRANKED"));
        }
        return ret.append("</html>").toString();
    }

    private static boolean hasShips(ArmySim army) {
        for (model.Pelotao pelotao : army.getPelotoes().values()) {
            if (pelotao.getTipoTropa() != null && pelotao.getTipoTropa().isBarcos()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The four numbers that answer "who is stronger": land and sea, attack and defence.
     *
     * The single biggest thing the three-pane rebuild was missing. The old window's army table
     * carried these as four columns and they are the reason a player opens the tool at all - it
     * could say who fights whom and in what order they die, and could not say who would win.
     *
     * Computed live off {@code ExercitoFacade}, which already takes {@code IExercito}, so the
     * simulated army goes straight in and the numbers are the Judge's own - no second
     * implementation to drift. <b>The boolean is NAVAL, not land</b>:
     * {@code BattleSimFacade.getArmyAttack} tests {@code naval == tipoTropa.isBarcos()}. The old
     * clipboard export had it backwards and printed the fleet's strength under "Land attack".
     */
    public static String getArmyStrength(ArmySim army) {
        if (army == null) {
            return "";
        }
        final ExercitoFacade facade = new ExercitoFacade();
        return String.format("<html>%s: %,d &nbsp; %s: %,d<br>%s: %,d &nbsp; %s: %,d</html>",
                labels.getString("TROPA.ATAQUE.TERRA"), facade.getAtaqueExercito(army, false),
                labels.getString("TROPA.DEFESA.TERRA"), facade.getDefesaExercito(army, false),
                labels.getString("TROPA.ATAQUE.NAVAL"), facade.getAtaqueExercito(army, true),
                labels.getString("TROPA.DEFESA.NAVAL"), facade.getDefesaExercito(army, true));
    }

    /**
     * A compact strength for the roster, so armies can be COMPARED without clicking each one.
     *
     * The old window's scan surface was a table; this one is a tree, and a detail pane only ever
     * shows the selected army. Without something on the node itself the player has to click every
     * army in turn and remember the numbers, which is exactly the job the old table did for him.
     *
     * Only the layer the army actually has troops in, because a land host's naval attack is zero
     * and printing "0/0" beside every army would be noise dressed as data.
     */
    public static String getArmyStrengthShort(ArmySim army) {
        if (army == null) {
            return "";
        }
        final ExercitoFacade facade = new ExercitoFacade();
        final StringBuilder ret = new StringBuilder();
        final int landAttack = facade.getAtaqueExercito(army, false);
        final int landDefense = facade.getDefesaExercito(army, false);
        if (landAttack > 0 || landDefense > 0) {
            ret.append(String.format("%,d/%,d", landAttack, landDefense));
        }
        final int seaAttack = facade.getAtaqueExercito(army, true);
        final int seaDefense = facade.getDefesaExercito(army, true);
        if (seaAttack > 0 || seaDefense > 0) {
            if (ret.length() > 0) {
                ret.append("  ");
            }
            ret.append(String.format("%s %,d/%,d",
                    labels.getString("BATTLESIM.LAYER.NAVY"), seaAttack, seaDefense));
        }
        return ret.toString();
    }

    /**
     * What the run did, for the status bar: rounds fought, plus everything it could not do.
     *
     * The caveats are not a footnote. A result that omits what it skipped looks complete, and a
     * player reading casualties off a land-only resolution while a fleet sits on the same hex is
     * being misled by omission. So the layer limit is stated on EVERY result, not only when
     * something goes wrong, and the resolver's own notes follow it.
     */
    public static String getRunResultText(CombatResult result) {
        if (result == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder("<html>");
        // Zero rounds is NOT a battle resolved in zero rounds, and saying "Resolved in 0 round(s)"
        // would be a completed simulation of a fight that never happened. It has two causes and
        // both are real: the hex engages only on the sea or city layer (the run gate says READY for
        // engagement on ANY layer, by design), or the armies standing on land are not hostile to
        // each other. Either way the honest sentence is the same one.
        ret.append(result.getRounds() == 0
                ? labels.getString("BATTLESIM.RESULT.NOLANDBATTLE")
                : String.format(labels.getString("BATTLESIM.RESULT.DONE"), result.getRounds()));
        for (String note : result.getNotes()) {
            ret.append("<br>").append(labels.getString(note));
        }
        return ret.append("</html>").toString();
    }

    /**
     * "Reported size: Vast army" - the server's own word for how big this army is.
     *
     * Shown for EVERY army, not only the ones the player cannot see into, and the javadoc used to
     * claim otherwise. The band is server-derived in both cases: for an unscouted enemy it is the
     * only strength figure there is, and for his own army it is the same sentence his army list
     * already shows him. Suppressing it on the armies he can count would also take it away exactly
     * when he starts typing a composition into an unscouted one - which is the moment he most wants
     * to check his guess against what he was told.
     *
     * It is a LABEL and stays one: the band is what he was told, and an editable version would
     * invite him to treat a reversed guess as data. What he types instead is the platoon list,
     * which is his own estimate and is marked as such.
     */
    public static String getSizeBandText(ArmySim army) {
        if (army == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder();
        if (!army.getSizeBand().isEmpty()) {
            ret.append(String.format(labels.getString("BATTLESIM.SIZE.REPORTED"),
                    army.getSizeBand()));
        }
        if (army.getPelotoes().isEmpty()) {
            // an empty platoon table looks like a bug unless something says what to do about it
            if (ret.length() > 0) {
                ret.append("   ");
            }
            ret.append(labels.getString("BATTLESIM.PLATOON.NONE"));
        }
        return ret.toString();
    }

    /**
     * The city line: what the attackers face, and whether there is a round 0.
     *
     * Two numbers, not one, because the city layer is TWO rounds. Round 0 is siege engines against
     * the FORTIFICATION and is fought only when an attacker carries them; round 1 is the single
     * army-versus-city exchange against the city's DEFENSE. Round 0 comes first and can reduce the
     * fortification, so collapsing them into one figure would hide the order that decides the
     * result.
     */
    public static String getCityText(CombatScenario scenario) {
        if (scenario == null || scenario.getCidadeAtiva() == null) {
            return labels.getString("BATTLESIM.CITY.NONE");
        }
        // Three facts on three lines, not one run-on sentence. Spaced onto one line they ran
        // past the edge of the Ground panel and the siege clause - the half that says whether there
        // is a round 0 at all - was the half that got cut off.
        return String.format("<html>%s: %,d<br>%s: %,d<br>%s</html>",
                labels.getString("BATTLESIM.CITY.DEFENSE"), scenario.getCityDefense(),
                labels.getString("BATTLESIM.CITY.FORTIFICATION.DEFENSE"),
                scenario.getCityFortificationDefense(),
                labels.getString(scenario.isSiegeExpected()
                        ? "BATTLESIM.CITY.SIEGE" : "BATTLESIM.CITY.NOSIEGE"));
    }

    /**
     * The roster leaf: the army's name, and the layers it fights in.
     *
     * Only the layers it IS in, with nothing at all when it fights nowhere. The positional badge
     * {@code N A C} / {@code N \u00b7 \u00b7} is right for a fixed-width column but wrong here: a
     * JTree's proportional font does not align the slots into columns anyway, so the placeholders
     * carry no information, and an army in no layer rendered as three dots beside its name, which
     * every reader takes for a truncated name. The empty case is explained properly in the army
     * editor's "Fights in:" line, which is where a player is looking when he asks.
     */
    public static String getArmyTitle(ArmySim army, LayerParticipation participation) {
        return getArmyTitle(army, participation, null);
    }

    /**
     * @param outcome how the last run ended for this army, or null before one.
     *
     * The mark goes at the FRONT, where the eye lands first and where it lines up down the column.
     * It is the one thing in the roster that answers the question the player actually opened the
     * window to ask, and reading it should not mean parsing the rest of the row.
     */
    public static String getArmyTitle(ArmySim army, LayerParticipation participation,
            CombatResult result) {
        final String strength = getArmyStrengthShort(army);
        if (participation == null || !participation.isInAnyLayer()) {
            return strength.isEmpty() ? army.getNome()
                    : String.format("%s  -  %s", army.getNome(), strength);
        }
        return String.format("%s  [%s]  %s", army.getNome(),
                getLayerMarks(army, participation, result), strength);
    }

    /**
     * THREE SLOTS, sea then land then city, and each one says what happened in its own layer.
     *
     * The slot never moves, which is the whole value of it: the middle mark is always the land
     * battle whether or not there was a fight at sea, so a column of armies can be read down rather
     * than parsed one row at a time. A battle is three fights and they can end differently - a fleet
     * can win at sea and the troops it lands still be destroyed ashore - so one mark per army would
     * have to pick one of those and hide the rest.
     *
     * Four states per slot, and they are four different statements:
     * <ul>
     *   <li>a dot - this army is not in that layer at all;</li>
     *   <li>the layer's letter - it is in it, but no verdict: either no run yet, or a layer the
     *       engine does not resolve yet, which today is the sea and the city;</li>
     *   <li>an outcome glyph - it fought and this is how it ended;</li>
     *   <li>the watching glyph - it was in the layer and never met an enemy there.</li>
     * </ul>
     */
    public static String getLayerMarks(ArmySim army, LayerParticipation participation,
            CombatResult result) {
        final StringBuilder ret = new StringBuilder();
        for (CombatLayer layer : CombatLayer.values()) {
            if (ret.length() > 0) {
                ret.append(' ');
            }
            ret.append(markFor(army, participation, result, layer));
        }
        return ret.toString();
    }

    private static String markFor(ArmySim army, LayerParticipation participation,
            CombatResult result, CombatLayer layer) {
        if (participation == null || !participation.isIn(layer)) {
            return labels.getString("BATTLESIM.OUTCOME.NOT_IN_LAYER");
        }
        final CombatResult.Outcome outcome = result == null ? null : result.getOutcome(army, layer);
        return outcome == null ? layer.getBadge()
                : labels.getString("BATTLESIM.OUTCOME." + outcome.name());
    }

    /**
     * Before, after and lost for one army, or NULL when it took no part.
     *
     * Null rather than zeroes, because those are different statements: an army that fought and lost
     * nobody has three real numbers, and an army that was never in the battle has none. The table
     * shows "--" for the second.
     */
    public static int[] getArmyTotals(ArmySim army, CombatResult result) {
        int before = 0;
        int after = 0;
        boolean any = false;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (!result.has(pelotao)) {
                continue;
            }
            any = true;
            before += pelotao.getQtd();
            after += result.getAfter(pelotao);
        }
        return any ? new int[]{before, after, before - after} : null;
    }

    /**
     * The verdict, in at most a few lines: who is left holding the hex and what it cost them.
     *
     * Built from the OUTCOMES rather than from the casualty numbers, because the two can disagree in
     * a way that matters: an army that fought and lost nobody and an army that never engaged both
     * end at full strength, and only the resolver knows which is which.
     */
    public static List<String> getVerdictLines(CombatScenario scenario, CombatResult result) {
        final List<String> ret = new ArrayList<>();
        if (result.getRounds() == 0) {
            ret.add(labels.getString("BATTLESIM.VERDICT.NOBATTLE"));
            return ret;
        }
        final List<String> standing = new ArrayList<>();
        final List<String> destroyed = new ArrayList<>();
        boolean undecided = false;
        for (ArmySim army : scenario.getArmies()) {
            final CombatResult.Outcome outcome = result.getOutcome(army, CombatLayer.ARMY);
            if (outcome == CombatResult.Outcome.WON) {
                standing.add(army.getNome());
            } else if (outcome == CombatResult.Outcome.LOST) {
                destroyed.add(army.getNome());
            } else if (outcome == CombatResult.Outcome.UNDECIDED) {
                undecided = true;
            }
        }
        if (undecided) {
            ret.add(labels.getString("BATTLESIM.VERDICT.STALEMATE"));
        } else if (!standing.isEmpty()) {
            ret.add(String.format(labels.getString("BATTLESIM.VERDICT.HOLDS"),
                    join(standing)));
        }
        if (!destroyed.isEmpty()) {
            ret.add(String.format(labels.getString("BATTLESIM.VERDICT.DESTROYED"),
                    join(destroyed)));
        }
        for (ArmySim army : scenario.getArmies()) {
            final int[] totals = getArmyTotals(army, result);
            if (totals == null || totals[2] <= 0) {
                continue;
            }
            ret.add(String.format(labels.getString("BATTLESIM.VERDICT.COST"), army.getNome(),
                    totals[2], totals[0], Math.round(100f * totals[2] / totals[0])));
        }
        return ret;
    }

    private static String join(List<String> names) {
        final StringBuilder ret = new StringBuilder();
        for (String name : names) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(name);
        }
        return ret.toString();
    }

    /**
     * The three marks in words, for the roster tooltip.
     *
     * An emoji is a guess unless something says what it means. Null before a run and for an army in
     * no layer at all, so the hover stays silent rather than explaining a row of dots.
     */
    public static String getLayerHint(ArmySim army, LayerParticipation participation,
            CombatResult result) {
        if (participation == null || !participation.isInAnyLayer() || result == null) {
            return null;
        }
        final StringBuilder ret = new StringBuilder("<html>");
        for (CombatLayer layer : CombatLayer.values()) {
            if (!participation.isIn(layer)) {
                continue;
            }
            final CombatResult.Outcome outcome = result.getOutcome(army, layer);
            ret.append(String.format("%s: %s<br>", labels.getString("BATTLESIM.LAYER."
                    + layer.name()), outcome == null
                            ? labels.getString("BATTLESIM.OUTCOME.NOT_RESOLVED.HINT")
                            : labels.getString("BATTLESIM.OUTCOME." + outcome.name() + ".HINT")));
        }
        return ret.append("</html>").toString();
    }
}
