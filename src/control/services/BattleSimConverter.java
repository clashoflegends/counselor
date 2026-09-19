package control.services;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.RosterDerivation;
import business.combat.ScenarioRoster;
import java.util.List;
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
     * The army editor's "Fights in: sea, land" line, or why it takes no part.
     *
     * An army that fights nowhere is the interesting case, and the one a player opens the simulator
     * to ask about: a fleet that will not defend his city, a garrison that will not sortie. So the
     * empty answer carries the REASON rather than an empty list.
     */
    public static String getFightsIn(LayerParticipation participation) {
        if (participation == null) {
            return "";
        }
        final List<CombatLayer> layers = participation.getLayers();
        if (layers.isEmpty()) {
            return labels.getString("BATTLESIM.FIGHTS.NONE") + " (" + getWhyNot(participation) + ")";
        }
        final StringBuilder ret = new StringBuilder();
        for (CombatLayer layer : layers) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(getLayerName(layer));
        }
        return String.format(labels.getString("BATTLESIM.FIGHTS.IN"), ret.toString());
    }

    /**
     * Why an army fights nowhere, taken from the layer that has the most to say.
     *
     * The land layer is asked first because it is the one a player is usually surprised about. Each
     * reason maps to exactly one test in {@code LayerParticipation}, so this never has to guess.
     */
    private static String getWhyNot(LayerParticipation participation) {
        for (CombatLayer layer : new CombatLayer[]{CombatLayer.ARMY, CombatLayer.NAVY, CombatLayer.CITY}) {
            final LayerParticipation.Reason reason = participation.getReason(layer);
            if (reason != null && reason != LayerParticipation.Reason.FIGHTS) {
                return labels.getString("BATTLESIM.REASON." + reason.name());
            }
        }
        return "";
    }

    public static String getCombatLevelName(CombatLevel level) {
        return labels.getString("BATTLESIM.LEVEL." + level.name());
    }

    public static String getProvenanceName(CombatScenario.Provenance provenance) {
        return labels.getString("BATTLESIM.PROVENANCE." + provenance.name());
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
        switch (derivation.getBasis()) {
            case GAME_TYPE:
                ret.append(labels.getString("BATTLESIM.STATUS.GAMETYPE"));
                break;
            case PARTLY_ASSUMED:
                ret.append(String.format(labels.getString("BATTLESIM.STATUS.ASSUMED"),
                        derivation.getAssumedPairs()));
                break;
            default:
                ret.append(labels.getString("BATTLESIM.STATUS.ALLREAD"));
                break;
        }
        final int edited = scenario == null ? 0 : scenario.getEditedCount();
        if (edited > 0) {
            ret.append(String.format(labels.getString("BATTLESIM.STATUS.EDITED"), edited));
        }
        return ret.toString();
    }

    /**
     * Why Run is disabled, for the status bar rather than a tooltip.
     *
     * A tooltip on a disabled button is unreliable across platforms, and this is the one message the
     * player most needs when nothing happens. Returns null when Run should be enabled - which, until
     * the engine exists, it never is.
     */
    public static String getRunDisabledReason(CombatScenario scenario) {
        if (scenario != null && !scenario.hasCombat()) {
            return labels.getString("BATTLESIM.RUN.DISABLED.NOCOMBAT");
        }
        return labels.getString("BATTLESIM.RUN.DISABLED.NOENGINE");
    }

    /** The roster leaf: the army's name and its N A C badge. */
    public static String getArmyTitle(ArmySim army, LayerParticipation participation) {
        final String badge = participation == null ? "..." : participation.getBadge();
        return String.format("%s  %s", army.getNome(), badge);
    }
}
