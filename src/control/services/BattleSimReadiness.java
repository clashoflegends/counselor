package control.services;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.RelationshipMatrix;
import business.combat.RunGate;
import business.combat.ScenarioDefaults;
import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import model.Nacao;

/** Read-only facts for the readiness strip; runnable never means that every input is known. */
public final class BattleSimReadiness {
    public record CompositionIssue(ArmySim army, boolean empty) {
    }

    /** One nation pair is listed once, even when both of its directional cells are assumed. */
    public record AssumedDirection(Nacao first, Nacao second) {
    }

    private final RunGate gate;
    private final Map<CombatLayer, List<ArmySim>> participants;
    private final List<ArmySim> unknownMorale;
    private final List<CompositionIssue> composition;
    private final List<AssumedDirection> assumedDirections;

    private BattleSimReadiness(RunGate gate, Map<CombatLayer, List<ArmySim>> participants,
            List<ArmySim> unknownMorale, List<CompositionIssue> composition,
            List<AssumedDirection> assumedDirections) {
        this.gate = gate;
        final Map<CombatLayer, List<ArmySim>> copied = new EnumMap<>(CombatLayer.class);
        participants.forEach((layer, list) -> copied.put(layer, List.copyOf(list)));
        this.participants = Map.copyOf(copied);
        this.unknownMorale = List.copyOf(unknownMorale);
        this.composition = List.copyOf(composition);
        this.assumedDirections = List.copyOf(assumedDirections);
    }

    public static BattleSimReadiness of(CombatScenario scenario) {
        final Map<CombatLayer, List<ArmySim>> participants = new EnumMap<>(CombatLayer.class);
        for (CombatLayer layer : CombatLayer.values()) {
            participants.put(layer, new ArrayList<>());
        }
        final List<ArmySim> morale = new ArrayList<>();
        final List<CompositionIssue> composition = new ArrayList<>();
        final List<AssumedDirection> assumed = new ArrayList<>();
        if (scenario == null) {
            return new BattleSimReadiness(RunGate.NO_ARMIES, participants, morale, composition, assumed);
        }
        final Map<ArmySim, LayerParticipation> participation = scenario.getParticipation();
        final ExercitoFacade armies = new ExercitoFacade();
        for (ArmySim army : scenario.getArmies()) {
            final LayerParticipation one = participation.get(army);
            for (CombatLayer layer : CombatLayer.values()) {
                if (one != null && one.getReason(layer) == LayerParticipation.Reason.FIGHTS) {
                    participants.get(layer).add(army);
                }
            }
            if (scenario.isMoraleUnknown(army)) {
                morale.add(army);
            }
            final boolean empty = armies.getQtTropasTotal(army) <= 0;
            final boolean unidentified = army.getPelotoes().values().stream()
                    .anyMatch(platoon -> platoon.getQtd() > 0 && ScenarioDefaults.isPlaceholder(platoon));
            if (empty || unidentified) {
                composition.add(new CompositionIssue(army, empty));
            }
        }
        final RelationshipMatrix relationships = scenario.getRelationships();
        final List<Nacao> nations = scenario.getNacoes();
        for (int i = 0; i < nations.size(); i++) {
            for (int j = i + 1; j < nations.size(); j++) {
                final Nacao first = nations.get(i), second = nations.get(j);
                if (relationships.getOrigin(first, second) == RelationshipMatrix.Origin.ASSUMED
                        || relationships.getOrigin(second, first) == RelationshipMatrix.Origin.ASSUMED) {
                    assumed.add(new AssumedDirection(first, second));
                }
            }
        }
        return new BattleSimReadiness(scenario.getRunGate(), participants, morale, composition, assumed);
    }

    public RunGate gate() {
        return gate;
    }

    /** Eligibility before running; later layers still depend on what survives earlier layers. */
    public List<ArmySim> participants(CombatLayer layer) {
        return participants.get(layer);
    }

    public List<ArmySim> unknownMorale() {
        return unknownMorale;
    }

    public List<CompositionIssue> composition() {
        return composition;
    }

    public List<AssumedDirection> assumedDirections() {
        return assumedDirections;
    }
}
