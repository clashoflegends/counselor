package e2e;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import business.combat.RunGate;
import control.services.BattleSimReadiness;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleSimReadinessTest {
    private static Nacao nation(String code) {
        final Nacao ret = new Nacao();
        ret.setCodigo(code);
        ret.setNome(code);
        return ret;
    }

    private static CombatScenario scenario() {
        final Terreno terrain = new Terreno();
        terrain.setCodigo("P");
        final Local hex = new Local();
        hex.setCodigo("1428");
        hex.setCoordenadas("1428");
        hex.setTerreno(terrain);
        return new CombatScenario(null, hex);
    }

    private static ArmySim army(CombatScenario scenario, Nacao nation, String typeCode, int quantity,
            CombatScenario.Provenance provenance) {
        final ArmySim ret = new ArmySim("army", scenario.getTerreno(), nation);
        ret.setCombatLevel(CombatLevel.ATTACK_CITY);
        if (typeCode != null) {
            platoon(ret, typeCode, quantity);
        }
        scenario.addArmy(ret, provenance);
        return ret;
    }

    private static void platoon(ArmySim army, String code, int quantity) {
        final TipoTropa type = new TipoTropa();
        type.setCodigo(code);
        final Pelotao platoon = new Pelotao();
        platoon.setTipoTropa(type);
        platoon.setQtd(quantity);
        army.getPelotoes().put(platoon.getCodigo(), platoon);
    }

    @Test
    void emptyAndNullScenariosKeepTheExistingGateAndReadOnlyLists() {
        for (CombatScenario scenario : new CombatScenario[]{null, scenario()}) {
            final BattleSimReadiness facts = BattleSimReadiness.of(scenario);
            assertEquals(RunGate.NO_ARMIES, facts.gate());
            for (CombatLayer layer : CombatLayer.values()) {
                assertTrue(facts.participants(layer).isEmpty());
                assertThrows(UnsupportedOperationException.class, () -> facts.participants(layer).clear());
            }
        }
    }

    @Test
    void runnableCanCoexistWithUnknownMoraleAndAnAssumedReverseDirection() {
        final CombatScenario scenario = scenario();
        final Nacao first = nation("a"), second = nation("b");
        final ArmySim mine = army(scenario, first, "inf", 100, CombatScenario.Provenance.EXACT);
        final ArmySim enemy = army(scenario, second, "inf", 100, CombatScenario.Provenance.ESTIMATED);
        enemy.setMoral(0);
        mine.setMoral(50);
        scenario.setRelacionamento(first, second, RelationshipMatrix.SWORN_ENEMY);
        final BattleSimReadiness facts = BattleSimReadiness.of(scenario);

        assertEquals(RunGate.READY, facts.gate());
        assertEquals(scenario.getRunGate(), facts.gate());
        assertEquals(java.util.List.of(enemy), facts.unknownMorale());
        assertTrue(facts.composition().isEmpty(), "estimated identified troops are not unknown composition");
        assertEquals(1, facts.assumedDirections().size());
        assertEquals(2, facts.participants(CombatLayer.ARMY).size());
        assertTrue(facts.participants(CombatLayer.NAVY).isEmpty());
        assertEquals(0, enemy.getMoral(), "readiness must not fill values");
        assertEquals(RelationshipMatrix.Origin.ASSUMED, scenario.getRelationships().getOrigin(second, first));
    }

    @Test
    void emptyManualAndPositivePlaceholderArmiesEachCountOnce() {
        final CombatScenario scenario = scenario();
        final Nacao owner = nation("a");
        final ArmySim empty = army(scenario, owner, null, 0, CombatScenario.Provenance.MANUAL);
        final ArmySim unknown = army(scenario, owner, "none", 20, CombatScenario.Provenance.MANUAL);
        platoon(unknown, "ship", 10);
        final ArmySim identified = army(scenario, owner, "inf", 20, CombatScenario.Provenance.ESTIMATED);
        platoon(identified, "none", 0);
        identified.setMoral(50);
        final BattleSimReadiness facts = BattleSimReadiness.of(scenario);

        assertEquals(2, facts.composition().size());
        assertSame(empty, facts.composition().get(0).army());
        assertTrue(facts.composition().get(0).empty());
        assertSame(unknown, facts.composition().get(1).army());
        assertFalse(facts.composition().get(1).empty());
        assertTrue(facts.unknownMorale().isEmpty(), "manual zero morale makes no additional certainty claim");
    }

    @Test
    void nationPairCountDoesNotMultiplyWithTheNumberOfArmies() {
        final CombatScenario scenario = scenario();
        final Nacao first = nation("a"), second = nation("b");
        for (int i = 0; i < 2; i++) {
            army(scenario, first, "inf", 100, CombatScenario.Provenance.EXACT);
            army(scenario, second, "inf", 100, CombatScenario.Provenance.EXACT);
        }
        assertEquals(1, BattleSimReadiness.of(scenario).assumedDirections().size());
        assertEquals(4, scenario.getAssumedCount(), "existing status uses army pairs, a different unit");
    }

    @Test
    void cityOwnerIsIncludedEvenWhenItHasNoArmyAndTheAttackerDirectionIsAssumed() {
        final CombatScenario scenario = scenario();
        final Nacao attacker = nation("a"), defender = nation("b");
        final ArmySim army = army(scenario, attacker, "inf", 100, CombatScenario.Provenance.EXACT);
        final Cidade city = new Cidade();
        city.setCodigo("city");
        city.setTamanho(2);
        scenario.setCidade(city);
        scenario.setCityOwner(defender);
        scenario.setCityParticipates(true);
        scenario.setRelacionamento(defender, attacker, RelationshipMatrix.SWORN_ENEMY);

        final BattleSimReadiness facts = BattleSimReadiness.of(scenario);
        assertEquals(1, facts.assumedDirections().size());
        assertEquals(RelationshipMatrix.Origin.ASSUMED, scenario.getRelationships().getOrigin(attacker, defender));
        assertTrue(facts.participants(CombatLayer.CITY).isEmpty(),
                "a known hostile defender direction must not imply the attacker will assault");
        assertEquals(RunGate.NO_HOSTILE_PAIR, facts.gate());
    }

    @Test
    void noHostilityAndNoEngagementRetainGatePrecedence() {
        final CombatScenario scenario = scenario();
        final Nacao first = nation("a"), second = nation("b");
        army(scenario, first, null, 0, CombatScenario.Provenance.EXACT);
        army(scenario, second, null, 0, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(first, second, RelationshipMatrix.NEUTRAL);
        scenario.setRelacionamento(second, first, RelationshipMatrix.NEUTRAL);
        assertEquals(RunGate.NO_HOSTILE_PAIR, BattleSimReadiness.of(scenario).gate());
        scenario.setRelacionamento(first, second, RelationshipMatrix.SWORN_ENEMY);
        assertEquals(RunGate.NO_ENGAGEMENT, BattleSimReadiness.of(scenario).gate());
        assertEquals(2, BattleSimReadiness.of(scenario).composition().size());
    }
}
