package gui.services;

import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Habilidade;
import model.Local;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmyRouteFinderTest {

    @Test
    void longerRoadRouteBeatsTheShorterExpensiveRoute() {
        Fixture f = new Fixture();
        var route = ArmyRouteFinder.find(f.locals, f.start, f.target, f.movement(f.infantry)).orElseThrow();
        assertEquals(List.of("ne", "e", "se"), route.directions());
        assertEquals(6, route.cost());
        assertEquals(12, f.replay(f.movement(f.infantry), f.start, List.of("e", "e")));
        assertEquals(route.cost(), f.replay(f.movement(f.infantry), f.start, route.directions()));
    }

    @Test
    void everyTroopTypeParticipatesInTheCost() {
        Fixture f = new Fixture();
        var cavalry = ArmyRouteFinder.find(f.locals, f.start, f.target, f.movement(f.cavalry)).orElseThrow();
        assertEquals(List.of("e", "e"), cavalry.directions(), "equal-cost routes prefer fewer steps");
        assertEquals(3, cavalry.cost());
        var mixed = ArmyRouteFinder.find(f.locals, f.start, f.target,
                f.movement(f.cavalry, f.infantry)).orElseThrow();
        assertEquals(List.of("ne", "e", "se"), mixed.directions());
        assertEquals(6, mixed.cost());
    }

    @Test
    void foodRoundingIsPerStepAndEvasiveAndFixedModesUseTheExistingEngine() {
        Fixture f = new Fixture();
        MovimentoExercito movement = f.movement(f.infantry);
        movement.setComida(false);
        var hungry = ArmyRouteFinder.find(f.locals, f.start, f.target, movement).orElseThrow();
        assertEquals(9, hungry.cost(), "ceil(2 * 4/3) for each of three road steps");
        assertEquals(hungry.cost(), f.replay(movement, f.start, hungry.directions()));
        movement.setComida(true);
        movement.setEvasivo(true);
        assertEquals(12, ArmyRouteFinder.find(f.locals, f.start, f.target, movement).orElseThrow().cost());
        movement.setMoveType(MovimentoExercito.BY_FIXED);
        var fixed = ArmyRouteFinder.find(f.locals, f.start, f.target, movement).orElseThrow();
        assertEquals(List.of("e", "e"), fixed.directions());
        assertEquals(2, fixed.cost());
    }

    @Test
    void acceptsTheExactMovementBudgetAndRejectsOverBudget() {
        Fixture f = new Fixture();
        MovimentoExercito movement = f.movement(f.infantry);
        movement.setLimiteMovimento(6);
        assertEquals(6, ArmyRouteFinder.find(f.locals, f.start, f.target, movement).orElseThrow().cost());
        movement.setLimiteMovimento(5);
        assertTrue(ArmyRouteFinder.find(f.locals, f.start, f.target, movement).isEmpty());
    }

    @Test
    void explicitPlanningBudgetAllows24PointsButNot25WithoutChangingTheOrderAllowance() {
        Fixture f = new Fixture();
        f.locals.clear();
        for (int col = 1; col <= 26; col++) {
            f.hex(String.format("%02d04", col), f.plain);
        }
        MovimentoExercito movement = f.movement(f.infantry);
        movement.setMoveType(MovimentoExercito.BY_FIXED);
        movement.setLimiteMovimento(12);
        Local origin = f.locals.get("0104");
        assertTrue(ArmyRouteFinder.find(f.locals, origin, f.locals.get("2504"), movement).isEmpty());
        var route = ArmyRouteFinder.find(f.locals, origin, f.locals.get("2504"), movement, 24)
                .orElseThrow();
        assertEquals(24, route.cost());
        assertEquals(24, route.directions().size());
        assertTrue(ArmyRouteFinder.find(f.locals, origin, f.locals.get("2604"), movement, 24).isEmpty());
        assertEquals(12, movement.getLimiteMovimento());
    }

    @Test
    void impassableRiverEdgesAndWaterAreNotUsedAsLandShortcuts() {
        Fixture f = new Fixture();
        f.locals.remove("0303");
        f.locals.remove("0403");
        f.start.setRio("3");
        assertTrue(ArmyRouteFinder.find(f.locals, f.start, f.target, f.movement(f.infantry)).isEmpty());
        f.start.setPonte("3");
        assertEquals(13, ArmyRouteFinder.find(f.locals, f.start, f.target,
                f.movement(f.infantry)).orElseThrow().cost());
        f.start.setRio("");
        f.start.setPonte("");
        f.locals.get("0304").setTerreno(f.water);
        assertTrue(ArmyRouteFinder.find(f.locals, f.start, f.target, f.movement(f.infantry)).isEmpty());
    }

    @Test
    void riverJunctionKeepsAMoreExpensiveArrivalThatCanContinue() {
        Fixture f = new Fixture();
        f.locals.clear();
        Local start = f.hex("0305", f.water);
        Local detour = f.hex("0204", f.water);
        Local junction = f.hex("0304", f.shore);
        junction.setRio("123");
        Local target = f.hex("0404", f.plain);
        target.setRio("6");
        MovimentoExercito movement = f.movement(f.ship);
        movement.setMoveType(MovimentoExercito.BY_WATER);
        assertEquals(1, f.replay(movement, start, List.of("ne")));
        assertTrue(f.replay(movement, start, List.of("ne", "e")) >= 9999,
                "the cheapest arrival at the junction cannot navigate to the target");
        var route = ArmyRouteFinder.find(f.locals, start, target, movement).orElseThrow();
        assertEquals(List.of("nw", "e", "e"), route.directions());
        assertEquals(3, route.cost());
        assertEquals(route.cost(), f.replay(movement, start, route.directions()));
        assertSame(detour, f.locals.get("0204"));
    }

    @Test
    void handlesOriginOffMapAndNullTargetsWithoutInventingSteps() {
        Fixture f = new Fixture();
        var movement = f.movement(f.infantry);
        assertEquals(List.of(), ArmyRouteFinder.find(f.locals, f.start, f.start, movement)
                .orElseThrow().directions());
        assertEquals(0, ArmyRouteFinder.find(f.locals, f.start, f.start, movement).orElseThrow().cost());
        assertTrue(ArmyRouteFinder.find(f.locals, f.start, null, movement).isEmpty());
        Local absent = new Local();
        absent.setCodigo("9999");
        assertTrue(ArmyRouteFinder.find(f.locals, f.start, absent, movement).isEmpty());
        assertTrue(ArmyRouteFinder.find(f.locals, absent, f.start, movement).isEmpty());
    }

    @Test
    void leavesSettingsAndMapUntouchedAndReturnsAnImmutableDeterministicRoute() {
        Fixture f = new Fixture();
        MovimentoExercito movement = f.movement(f.infantry);
        movement.setOrigem(f.target);
        movement.setDestino(f.start);
        movement.setDirecao(5);
        movement.setDirecaoAnterior(2);
        var route = ArmyRouteFinder.find(f.locals, f.start, f.target, movement).orElseThrow();
        assertEquals(route, ArmyRouteFinder.find(f.locals, f.start, f.target, movement).orElseThrow());
        assertSame(f.target, movement.getOrigem());
        assertSame(f.start, movement.getDestino());
        assertEquals(5, movement.getDirecao());
        assertEquals(2, movement.getDirecaoAnterior());
        assertTrue(movement.isComida());
        assertFalse(movement.isEvasivo());
        assertEquals(14, movement.getLimiteMovimento());
        assertEquals(5, f.locals.size());
        assertSame(f.forest, f.locals.get("0304").getTerreno());
        assertEquals(6, f.replay(movement, f.start, route.directions()));
        assertThrows(UnsupportedOperationException.class, () -> route.directions().add("h"));
    }

    static class Fixture {
        final SortedMap<String, Local> locals = new TreeMap<>();
        final Terreno plain = terrain("P", false);
        final Terreno forest = terrain("F", false);
        final Terreno water = terrain("E", true);
        final Terreno shore = terrain("L", false);
        final TipoTropa infantry = troop("INF", ";TTR;", 3, 9);
        final TipoTropa cavalry = troop("CAV", ";TTF;", 2, 1);
        final TipoTropa ship = troop("SHIP", ";TTN;", 1, 1);
        final Local start = hex("0204", plain);
        final Local target = hex("0404", plain);

        Fixture() {
            hex("0304", forest);
            hex("0303", plain);
            hex("0403", plain);
            start.setEstrada("2");
            locals.get("0303").setEstrada("35");
            locals.get("0403").setEstrada("46");
            target.setEstrada("1");
        }

        Local hex(String code, Terreno terrain) {
            Local local = new Local();
            local.setCodigo(code);
            local.setCoordenadas(code);
            local.setTerreno(terrain);
            local.setEstrada("");
            local.setRio("");
            local.setRiacho("");
            local.setVau("");
            local.setPonte("");
            locals.put(code, local);
            return local;
        }

        MovimentoExercito movement(TipoTropa... troops) {
            MovimentoExercito movement = new MovimentoExercito();
            movement.addTropasAll(List.of(troops));
            movement.setComida(true);
            movement.setLimiteMovimento(14);
            return movement;
        }

        private Terreno terrain(String code, boolean water) {
            Terreno terrain = new Terreno();
            terrain.setCodigo(code);
            terrain.setNome(code);
            terrain.setAgua(water);
            return terrain;
        }

        private TipoTropa troop(String code, String ability, int plainCost, int forestCost) {
            TipoTropa troop = new TipoTropa();
            troop.setCodigo(code);
            troop.setNome(code);
            Habilidade habilidade = new Habilidade();
            habilidade.setCodigo(ability);
            troop.addHabilidade(habilidade);
            troop.getMovimentoTerreno().put(plain, plainCost);
            troop.getMovimentoTerreno().put(forest, forestCost);
            troop.getMovimentoTerreno().put(water, 1);
            troop.getMovimentoTerreno().put(shore, 1);
            return troop;
        }

        int replay(MovimentoExercito settings, Local start, List<String> directions) {
            try {
                MovimentoExercito movement = settings.clone();
                Local local = start;
                int previous = 0;
                int cost = 0;
                LocalFacade facade = new LocalFacade();
                for (String direction : directions) {
                    int number = ConverterFactory.direcaoToInt(direction);
                    Local next = locals.get(facade.getIdentificacaoVizinho(local, number));
                    movement.setOrigem(local);
                    movement.setDestino(next);
                    movement.setDirecao(number);
                    movement.setDirecaoAnterior(previous);
                    cost += movement.getCustoMovimento();
                    previous = ConverterFactory.getDirecao(number + 3);
                    local = next;
                }
                return cost;
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError(ex);
            }
        }
    }
}
