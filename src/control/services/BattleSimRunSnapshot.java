package control.services;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import model.Nacao;
import model.Pelotao;
import persistenceCommons.BundleManager;

/** Frozen values only: pins must outlive their producing window and every subsequent edit. */
public record BattleSimRunSnapshot(UUID runId, UUID familyId, String game, int turn, String name,
        String time, Map<String, Input> inputs, List<Army> armies, Map<CombatLayer, Integer> rounds,
        String city, String report) {
    public record Input(String label, String value) { }
    /** After/lost count represented units only; unreported units never become zero-loss claims. */
    public record Units(long before, Long after, Long lost, long unreported) { }
    public record Army(UUID id, String name, String nation, Units soldiers, Units ships,
            Map<CombatLayer, String> outcomes) {
        public Army { outcomes = Map.copyOf(outcomes); }
    }
    public BattleSimRunSnapshot {
        inputs = Collections.unmodifiableMap(new LinkedHashMap<>(inputs));
        armies = List.copyOf(armies);
        rounds = Map.copyOf(rounds);
    }

    public static BattleSimRunSnapshot capture(BattleSimVariants variant, CombatResult result,
            String report, BundleManager labels) {
        if (result == null) throw new IllegalArgumentException("A completed run is required");
        variant.synchronize();
        final CombatScenario scenario = variant.scenario();
        final Map<String, Input> inputs = new LinkedHashMap<>();
        input(inputs, "hex", labels.getString("BATTLESIM.BATTLEFIELD"),
                scenario.getLocal() == null ? "" : scenario.getLocal().getCoordenadas());
        input(inputs, "terrain", labels.getString("TERRENO"), scenario.getTerreno());
        final var city = scenario.getCidade();
        field(inputs, "city", "BATTLESIM.CITY.PARTICIPATES", scenario.isCityParticipates(), labels);
        if (city != null) {
            field(inputs, "city", "NOME", city.getNome(), labels);
            field(inputs, "city", "NACAO", nation(city.getNacao()), labels);
            field(inputs, "city", "TAMANHO", city.getTamanho(), labels);
            field(inputs, "city", "FORTIFICACOES", city.getFortificacao(), labels);
            field(inputs, "city", "LEALDADE", city.getLealdade(), labels);
        }
        final List<Army> armies = new ArrayList<>();
        for (ArmySim army : scenario.getArmies()) {
            final UUID id = variant.armyId(army);
            final String prefix = "army/" + id;
            final String name = scenario.getDisplayName(army) + " [" + id.toString().substring(0, 8) + "]";
            field(inputs, prefix, name, "NOME", army.getNome(), labels);
            field(inputs, prefix, name, "NACAO", nation(army.getNacao()), labels);
            field(inputs, prefix, name, "COMANDANTE", army.getComandantePericia(), labels);
            field(inputs, prefix, name, "MORAL", army.getMoral() + (scenario.isMoraleUnknown(army) ? " (?)" : ""), labels);
            field(inputs, prefix, name, "TATICA", business.converter.TitleFactory.getTaticaNome(army.getTatica()), labels);
            field(inputs, prefix, name, "BATTLESIM.COMBAT.LEVEL", BattleSimConverter.getCombatLevelName(army.getCombatLevel()), labels);
            field(inputs, prefix, name, "BATTLESIM.COMBAT.TARGET", nation(army.getTargetNacao()), labels);
            field(inputs, prefix, name, "BATTLESIM.ATTACK.BONUS", army.getAttackBonus(), labels);
            field(inputs, prefix, name, "BATTLESIM.DEFENSE.BONUS", army.getArmyDefenseBonus(), labels);
            field(inputs, prefix, name, "BATTLESIM.VARIANT.MAGIC", army.getCombateAtaqueOnetime(), labels);
            field(inputs, prefix, name, "BATTLESIM.VARIANT.ASSUMPTIONS", labels.getString("BATTLESIM.PROVENANCE." + scenario.getProvenance(army)), labels);
            for (var entry : army.getPelotoes().entrySet()) {
                final Pelotao platoon = entry.getValue();
                final String row = prefix + "/platoon/" + entry.getKey();
                final String title = name + " / " + entry.getKey();
                field(inputs, row, title, "NOME", platoon.getTipoTropa(), labels);
                field(inputs, row, title, "QTD", platoon.getQtd(), labels);
                field(inputs, row, title, "TREINO", platoon.getTreino(), labels);
                field(inputs, row, title, "ARMA", platoon.getModAtaque(), labels);
                field(inputs, row, title, "ARMADURA", platoon.getModDefesa(), labels);
                field(inputs, row, title, "BATTLESIM.VARIANT.ASSUMPTIONS", labels.getString("BATTLESIM.PROVENANCE." + scenario.getProvenance(platoon)), labels);
            }
            final Map<CombatLayer, String> outcomes = new EnumMap<>(CombatLayer.class);
            for (CombatLayer layer : CombatLayer.values()) {
                final CombatResult.Outcome outcome = result.getOutcome(army, layer);
                outcomes.put(layer, outcome == null ? labels.getString("BATTLESIM.VARIANT.UNREPORTED")
                        : labels.getString("BATTLESIM.VARIANT.OUTCOME." + outcome));
            }
            armies.add(new Army(id, scenario.getDisplayName(army), nation(army.getNacao()), units(army, result, false),
                    units(army, result, true), outcomes));
        }
        final RelationshipMatrix matrix = scenario.getRelationships();
        for (Nacao from : matrix.getNacoes()) for (Nacao to : matrix.getNacoes()) {
            if (from == to) continue;
            input(inputs, "diplomacy/" + from.getCodigo() + "/" + to.getCodigo(),
                    nation(from) + " > " + nation(to), matrix.getValor(from, to) + "; " + matrix.getOrigin(from, to));
        }
        final Map<CombatLayer, Integer> rounds = new EnumMap<>(CombatLayer.class);
        for (CombatLayer layer : CombatLayer.values()) rounds.put(layer, result.getRounds(layer));
        final List<String> cityLines = BattleSimConverter.getCityLines(scenario, result);
        final String cityResult = result.getCityResult() == null ? labels.getString("BATTLESIM.VARIANT.UNREPORTED")
                : cityLines.isEmpty() ? labels.getString("BATTLESIM.LAYER.NOBATTLE") : String.join(" ", cityLines);
        return new BattleSimRunSnapshot(UUID.randomUUID(), variant.familyId(), variant.gameCode(), variant.turn(), variant.name(),
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                inputs, armies, rounds, cityResult, report);
    }

    private static Units units(ArmySim army, CombatResult result, boolean ships) {
        long before = 0, after = 0, lost = 0, unreported = 0;
        boolean represented = false;
        for (Pelotao platoon : army.getPelotoes().values()) {
            if ((platoon.getTipoTropa() != null && platoon.getTipoTropa().isBarcos()) != ships) continue;
            before += platoon.getQtd();
            if (result.has(platoon)) {
                represented = true;
                after += result.getAfter(platoon);
                lost += result.getLost(platoon);
            } else unreported += platoon.getQtd();
        }
        return new Units(before, represented ? after : null, represented ? lost : null, unreported);
    }

    private static String nation(Nacao nation) {
        return nation == null ? "--" : nation.getNome() + " [" + nation.getCodigo() + "]";
    }
    private static void input(Map<String, Input> rows, String key, String label, Object value) {
        final String shown = value instanceof baseLib.BaseModel model
                ? model.getNome() + " [" + model.getCodigo() + "]" : String.valueOf(value);
        rows.put(key, new Input(label, shown));
    }
    private static void field(Map<String, Input> rows, String prefix, String key, Object value, BundleManager labels) {
        field(rows, prefix, labels.getString("BATTLESIM.CITY.TITLE"), key, value, labels);
    }
    private static void field(Map<String, Input> rows, String prefix, String name, String key, Object value, BundleManager labels) {
        input(rows, prefix + "/" + key, name + " / " + labels.getString(key), value);
    }
}
