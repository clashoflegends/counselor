package gui.services;

import business.MovimentoExercito;
import java.util.List;
import model.Local;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavalLandingSessionTest {
    @Test void manualAutomaticAndReopenedRoutesStopAtLandingAndCanBeEdited() {
        var f = new ArmyRouteFinderTest.Fixture();
        f.locals.clear();
        Local start = f.hex("0204", f.water);
        Local landing = f.hex("0304", f.forest);
        landing.setLanding(" 663 ");
        Local beyond = f.hex("0404", f.water);
        f.ship.getMovimentoTerreno().put(f.forest, 5);
        var context = new ArmyMovementSession.Context(List.of(f.ship), true, false, 14,
                MovimentoExercito.BY_WATER, true);
        var session = new ArmyMovementSession(f.locals, start, context);
        assertTrue(session.append("e"));
        assertEquals(5, session.cost());
        assertTrue(session.append("h"));
        assertTrue(session.append("e"));
        assertSame(landing, session.destination());
        assertEquals("e;h;e;nr", session.getDirecoesIdTipo());
        session.setContext(new ArmyMovementSession.Context(List.of(f.ship), false, true, 14,
                MovimentoExercito.BY_WATER, true));
        assertEquals("e;h;e;ev", session.getDirecoesIdTipo());
        assertEquals(1, session.steps().size());
        session.setContext(context);
        assertFalse(session.routeTo(beyond));
        assertTrue(session.routeTo(landing));
        session.removeLast();
        assertSame(start, session.destination());
        assertTrue(session.append("e"));
        session.clear();
        assertEquals(0, session.cost());
        // Loading a saved route uses the same append path as either order editor and MovSim.
        for (String token : List.of("e", "h", "e")) session.append(token);
        assertEquals("e;h;e;nr", session.getDirecoesIdTipo());
        assertSame(landing, session.destination());
        var departure = new ArmyMovementSession(f.locals, landing, context);
        assertTrue(departure.append("e"));
        assertEquals(1, departure.cost());
        assertSame(beyond, departure.destination());
        landing.setRio("36");
        session.clear();
        assertTrue(session.append("e"));
        assertTrue(session.append("e"));
        assertSame(beyond, session.destination());
        assertTrue(session.routeTo(beyond));
        assertEquals(6, session.cost());
    }

    @Test void routeKeepsRiverArrivalEvenWhenCheaperLandingArrivalAlreadyReachesTheHex() {
        var f = new ArmyRouteFinderTest.Fixture();
        f.locals.clear();
        Local start = f.hex("0204", f.water);
        f.hex("0303", f.water);
        Local junction = f.hex("0304", f.forest);
        junction.setLanding("6");
        junction.setRio("23");
        Local end = f.hex("0404", f.water);
        var movement = f.movement(f.ship);
        movement.setMoveType(MovimentoExercito.BY_WATER);
        var route = ArmyRouteFinder.find(f.locals, start, end, movement).orElseThrow();
        assertEquals(List.of("ne", "se", "e"), route.directions());
        assertEquals(3, route.cost());
        f.ship.getMovimentoTerreno().put(f.forest, 5);
        movement.setLimiteMovimento(4);
        assertTrue(ArmyRouteFinder.find(f.locals, start, junction, movement).isEmpty());
    }
}
