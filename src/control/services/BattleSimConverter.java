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
     * Why Run is disabled, for the status bar rather than a tooltip.
     *
     * A tooltip on a disabled button is unreliable across platforms, and this is the one message the
     * player most needs when nothing happens.
     *
     * ALWAYS returns a reason, because Run is always disabled: there is no engine. Phase 5 (T-501)
     * is what gives it an enabled state, and that is where this gains an "everything is ready"
     * answer. Said explicitly because the javadoc previously claimed a null-means-enabled contract
     * the method has never had, and T-501 would have been written against it.
     */
    public static String getRunDisabledReason(CombatScenario scenario) {
        if (scenario != null && !scenario.hasCombat()) {
            return labels.getString("BATTLESIM.RUN.DISABLED.NOCOMBAT");
        }
        return labels.getString("BATTLESIM.RUN.DISABLED.NOENGINE");
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
        return String.format("%s: %,d   %s: %,d   %s",
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
        if (participation == null || !participation.isInAnyLayer()) {
            return army.getNome();
        }
        final StringBuilder badge = new StringBuilder();
        for (CombatLayer layer : participation.getLayers()) {
            badge.append(layer.getBadge());
        }
        return String.format("%s  [%s]", army.getNome(), badge);
    }
}
