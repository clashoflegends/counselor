package e2e;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import control.services.BattleSimRunSnapshot;
import control.services.BattleSimVariants;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import persistenceCommons.SettingsManager;
import static org.junit.jupiter.api.Assertions.*;

class BattleSimVariantsTest {
    static CombatScenario scenario() {
        final Terreno terrain = new Terreno();
        terrain.setCodigo("P");
        terrain.setNome("Plains");
        final Local local = new Local();
        local.setCodigo("1428"); local.setCoordenadas("1428"); local.setTerreno(terrain);
        final Partida game = new Partida(); game.setCodigo("88"); game.setTurno(20);
        final CombatScenario ret = new CombatScenario(game, local);
        final Nacao nation = new Nacao(); nation.setCodigo("1"); nation.setNome("Nation");
        final ArmySim army = new ArmySim("Duplicate", terrain, nation);
        platoon(army, "inf", 100);
        ret.addArmy(army, CombatScenario.Provenance.EXACT);
        return ret;
    }
    private static Pelotao platoon(ArmySim army, String code, int count) {
        final TipoTropa type = new TipoTropa(); type.setCodigo(code); type.setNome(code);
        final Pelotao ret = new Pelotao(); ret.setTipoTropa(type); ret.setQtd(count);
        army.getPelotoes().put(code, ret);
        return ret;
    }
    private static BattleSimRunSnapshot capture(BattleSimVariants variant, CombatResult result) {
        return BattleSimRunSnapshot.capture(variant, result, "Frozen report with caveats", SettingsManager.getInstance().getBundleManager());
    }

    @Test void forksKeepSimulationIdentityButClonesAndReimportsDoNot() {
        try (var original = new BattleSimVariants(scenario(), "A")) {
            final ArmySim army = original.scenario().getArmies().get(0);
            final var id = original.armyId(army);
            try (var fork = original.fork("B")) {
                assertEquals(id, fork.armyId(fork.scenario().getArmies().get(0)));
                assertNotSame(army, fork.scenario().getArmies().get(0));
                final ArmySim clone = new ArmySim(army);
                fork.scenario().addArmy(clone, CombatScenario.Provenance.MANUAL);
                assertNotEquals(id, fork.armyId(clone));
            }
            original.scenario().remArmy(army);
            original.synchronize();
            original.scenario().addArmy(army, CombatScenario.Provenance.EXACT);
            assertNotEquals(id, original.armyId(army));
        }
    }

    @Test void frozenResultsAndChangedInputsSurviveSubsequentEditsAndOriginClosure() {
        final var original = new BattleSimVariants(scenario(), "Original");
        try (var fork = original.fork("Alternative")) {
            final ArmySim army = original.scenario().getArmies().get(0);
            final Pelotao platoon = army.getPelotoes().get("inf");
            final CombatResult result = new CombatResult(); result.put(platoon, 100, 70);
            result.setOutcome(army, CombatLayer.ARMY, CombatResult.Outcome.UNDECIDED);
            final var snapshot = capture(original, result);
            original.family().pin(snapshot, -1);
            final var frozenInputs = snapshot.inputs().toString();
            platoon.setQtd(999); army.setMoral(77); result.put(platoon, 999, 1);
            original.rename("Changed");
            original.close(); original.close();
            assertEquals(1, fork.family().pins().size());
            assertEquals("Original", snapshot.name());
            assertEquals(frozenInputs, snapshot.inputs().toString());
            assertEquals(70L, snapshot.armies().get(0).soldiers().after());
            assertEquals(30L, snapshot.armies().get(0).soldiers().lost());
            assertFalse(snapshot.armies().get(0).outcomes().get(CombatLayer.ARMY).isBlank());
            assertThrows(UnsupportedOperationException.class, () -> snapshot.inputs().clear());
        }
        assertTrue(original.family().pins().isEmpty());
    }

    @Test void absentPlatoonsRemainUnreportedAndShipsAreSeparate() {
        try (var variant = new BattleSimVariants(scenario(), "A")) {
            final ArmySim army = variant.scenario().getArmies().get(0);
            final Pelotao unknown = platoon(army, "reserve", 25);
            final Pelotao ship = platoon(army, "ship", 12);
            final model.Habilidade naval = new model.Habilidade(); naval.setCodigo(";TTN;");
            ship.getTipoTropa().addHabilidade(naval);
            final CombatResult result = new CombatResult();
            result.put(army.getPelotoes().get("inf"), 100, 70);
            result.put(ship, 12, 10);
            final var snapshot = capture(variant, result);
            final var frozen = snapshot.armies().get(0);
            assertEquals(125, frozen.soldiers().before());
            assertEquals(25, frozen.soldiers().unreported());
            assertEquals(70L, frozen.soldiers().after());
            assertEquals(10L, frozen.ships().after());
            final var absent = capture(variant, new CombatResult()).armies().get(0);
            assertNull(absent.soldiers().lost());
            assertNull(absent.ships().after());
            assertFalse(result.has(unknown));
        }
    }

    @Test void capacityRequiresExplicitReplacementAndFamiliesCannotMix() {
        try (var variant = new BattleSimVariants(scenario(), "A");
             var other = new BattleSimVariants(scenario(), "A")) {
            final int[] updates = {0};
            variant.family().listen(() -> updates[0]++);
            for (int i = 0; i < BattleSimVariants.MAX_PINS; i++) variant.family().pin(capture(variant, new CombatResult()), -1);
            final var first = variant.family().pins().get(0);
            assertThrows(IllegalStateException.class, () -> variant.family().pin(capture(variant, new CombatResult()), -1));
            assertThrows(IllegalArgumentException.class, () -> variant.family().pin(capture(other, new CombatResult()), 0));
            variant.family().pin(capture(variant, new CombatResult()), 2);
            assertSame(first, variant.family().pins().get(0));
            variant.family().remove(2);
            assertEquals(8, updates[0]);
        }
    }

    @Test void gameAndTurnIdentityAreFrozenEvenWhenNamesAndCodesMatch() {
        try (var variant = new BattleSimVariants(scenario(), "A")) {
            final Partida game = variant.scenario().getPartida();
            assertTrue(variant.matches(game));
            final Partida other = new Partida(); other.setCodigo(game.getCodigo()); other.setTurno(game.getTurno());
            assertFalse(variant.matches(other));
            game.setTurno(game.getTurno() + 1);
            assertFalse(variant.matches(game));
            assertEquals(20, capture(variant, new CombatResult()).turn());
        }
    }

    @Test void cityLossDoesNotClaimDestructionAndCityWinDoesNotClaimSurvival() {
        checkCity(false);
        checkCity(true);
    }

    private static void checkCity(boolean noSurvivor) {
        final CombatScenario seed = scenario();
        final var local = seed.getLocal();
        final model.Cidade city = new model.Cidade();
        city.setCodigo("city"); city.setNome("City"); city.setTamanho(3);
        city.setFortificacao(1); city.setLealdade(50);
        final Nacao owner = new Nacao(); owner.setCodigo("owner"); owner.setNome("Owner");
        city.setNacao(owner); local.setCidade(city); city.setLocal(local);
        final CombatScenario scenario = new CombatScenario(seed.getPartida(), local);
        final ArmySim army = seed.getArmies().get(0);
        army.setLocal(scenario.getLocal()); army.setMoral(100);
        army.setCombatLevel(business.combat.CombatLevel.ATTACK_CITY);
        final Pelotao platoon = army.getPelotoes().get("inf");
        final java.util.SortedMap<Terreno, Integer> attack = new java.util.TreeMap<>();
        final java.util.SortedMap<Terreno, Integer> defense = new java.util.TreeMap<>();
        final java.util.SortedMap<Terreno, Integer> movement = new java.util.TreeMap<>();
        attack.put(scenario.getTerreno(), noSurvivor ? 1_000_000 : 1);
        defense.put(scenario.getTerreno(), noSurvivor ? 1 : 1_000_000);
        movement.put(scenario.getTerreno(), 5);
        platoon.getTipoTropa().setAtaqueTerreno(attack);
        platoon.getTipoTropa().setDefesaTerreno(defense);
        platoon.getTipoTropa().setMovimentoTerreno(movement);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(army.getNacao(), owner, business.combat.RelationshipMatrix.SWORN_ENEMY);
        final CombatResult result = new business.combat.CombatChain().resolve(scenario, null);
        assertEquals(noSurvivor ? business.combat.CityCombatResolver.CityOutcome.CAPTURED_NO_SURVIVOR
                : business.combat.CityCombatResolver.CityOutcome.REPELLED, result.getCityResult().getOutcome());
        final var labels = SettingsManager.getInstance().getBundleManager();
        try (var variant = new BattleSimVariants(scenario, "City")) {
            final var snapshot = capture(variant, result);
            final var units = snapshot.armies().get(0).soldiers();
            if (noSurvivor) assertEquals(0L, units.after());
            else assertTrue(units.after() > 0);
            assertEquals(labels.getString("BATTLESIM.VARIANT.OUTCOME." + (noSurvivor ? "WON" : "LOST")),
                    snapshot.armies().get(0).outcomes().get(CombatLayer.CITY));
            assertFalse(snapshot.city().isBlank());
        }
    }
}
