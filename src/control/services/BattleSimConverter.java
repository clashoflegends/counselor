package control.services;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.RosterDerivation;
import business.combat.RunGate;
import business.combat.ScenarioRoster;
import business.facade.ExercitoFacade;
import java.util.List;
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
    /** T-801 flips this. Until then Run is disabled and says so. */
    private static final boolean ENGINE_EXISTS = false;

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
     * Whether Run may be enabled at all: false until T-801 builds the resolution chain.
     *
     * One constant, one call site, so the day the engine lands is a one-line change rather than a
     * hunt through the model for everything that assumed there was none.
     */
    public static boolean isRunnable(CombatScenario scenario) {
        return scenario != null && scenario.getRunGate(ENGINE_EXISTS).isRunnable();
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
        // No nation, no numbers: BattleSimFacade.getPlatoonDefense dereferences getNacao()
        // unguarded for the ;PDB; capital-distance bonus, and an army whose owner is unknown is a
        // real state in this package rather than a bad fixture.
        if (army == null || army.getNacao() == null) {
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
        if (army == null || army.getNacao() == null) {
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
        final String strength = getArmyStrengthShort(army);
        if (participation == null || !participation.isInAnyLayer()) {
            return strength.isEmpty() ? army.getNome()
                    : String.format("%s  -  %s", army.getNome(), strength);
        }
        final StringBuilder badge = new StringBuilder();
        for (CombatLayer layer : participation.getLayers()) {
            badge.append(layer.getBadge());
        }
        return String.format("%s  [%s]  %s", army.getNome(), badge, strength);
    }
}
