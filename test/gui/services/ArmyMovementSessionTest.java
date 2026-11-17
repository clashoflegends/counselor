package gui.services;

import business.MovimentoExercito;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmyMovementSessionTest {

    @Test
    void modifierChangesRecalculateChosenStepsWithoutRemovingHoldsOrDetours() {
        var f = new ArmyRouteFinderTest.Fixture();
        var session = session(f, true, false, 12, MovimentoExercito.BY_LAND);
        for (String direction : List.of("ne", "h", "e", "se")) {
            assertTrue(session.append(direction));
        }
        assertEquals("ne;h;e;se;nr", session.getDirecoesIdTipo());
        int arrival = session.steps().get(0).movement().getDirecao();
        assertEquals((arrival + 2) % 6 + 1, session.steps().get(2).movement().getDirecaoAnterior());
        List<Integer> originalCosts = session.steps().stream().map(ArmyMovementSession.Step::cost).toList();

        session.setContext(context(f, false, true, 12, MovimentoExercito.BY_LAND));
        assertEquals("ne;h;e;se;ev", session.getDirecaoDisplay());
        assertEquals(f.target, session.destination());
        assertEquals(4, session.steps().size());
        assertTrue(session.cost() > originalCosts.stream().mapToInt(Integer::intValue).sum());
        assertTrue(session.steps().stream().allMatch(step -> step.movement().getLimiteMovimento() == 12));
        session.removeLast();
        assertEquals("ne;h;e;ev", session.getDirecoesIdTipo());
        session.clear();
        assertEquals("ev", session.getDirecoesIdTipo());
        assertSame(f.start, session.destination());
        assertEquals(0, session.cost());
    }

    @Test
    void automaticReplacementIsAtomicAndUsesTheSameCostsAsManualReplay() {
        var f = new ArmyRouteFinderTest.Fixture();
        var session = session(f, true, false, 8, MovimentoExercito.BY_LAND);
        assertTrue(session.append("h"));
        var before = session.steps();
        assertFalse(session.routeTo(null));
        assertEquals(before, session.steps());
        assertTrue(session.routeTo(f.target));
        assertEquals("ne;e;se;nr", session.getDirecoesIdTipo());
        assertEquals(6, session.cost());
        assertEquals(f.replay(f.movement(f.infantry), f.start,
                session.steps().stream().map(ArmyMovementSession.Step::direction).toList()), session.cost());
        assertTrue(session.steps().stream().allMatch(step -> step.movement().getLimiteMovimento() == 8));
    }

    @Test
    void manualInputCanExceedPlanningCapWithoutChangingTheOrderAllowance() {
        var f = new ArmyRouteFinderTest.Fixture();
        for (int allowance : new int[]{8, 12, 14, 17}) {
            var session = session(f, true, false, allowance, MovimentoExercito.BY_FIXED);
            for (int i = 0; i < 25; i++) {
                assertTrue(session.append("h"));
            }
            assertEquals(25, session.cost());
            assertEquals(allowance, session.context().allowance());
            assertTrue(session.steps().stream()
                    .allMatch(step -> step.movement().getLimiteMovimento() == allowance));
        }
    }

    @Test
    void disabledContextPreventsInputAndReplacementWithoutDestroyingAnEnteredRoute() {
        var f = new ArmyRouteFinderTest.Fixture();
        var session = session(f, true, false, 14, MovimentoExercito.BY_WATER);
        assertTrue(session.append("h"));
        var c = session.context();
        session.setContext(new ArmyMovementSession.Context(c.troops(), c.food(), c.evasive(),
                c.allowance(), c.moveType(), false));
        String before = session.getDirecoesIdTipo();
        assertFalse(session.append("e"));
        assertFalse(session.routeTo(f.target));
        assertEquals(before, session.getDirecoesIdTipo());
    }

    @Test
    void legacyHoldAliasesKeepTheLastNonHoldArrivalDirection() {
        var f = new ArmyRouteFinderTest.Fixture();
        var session = session(f, true, false, 12, MovimentoExercito.BY_LAND);
        for (String direction : List.of("NE", "H", "C", "L", "SE")) {
            assertTrue(session.append(direction));
        }
        assertEquals("NE;H;C;L;SE;nr", session.getDirecoesIdTipo());
        assertEquals(5, session.steps().get(3).movement().getDirecaoAnterior());
        assertEquals(f.target, session.destination());
    }

    @Test
    void infeasibleManualStepsRemainEnteredWhenCostsChange() {
        var f = new ArmyRouteFinderTest.Fixture();
        f.locals.get("0304").setTerreno(f.water);
        var session = session(f, true, false, 12, MovimentoExercito.BY_LAND);
        assertTrue(session.append("e"));
        assertTrue(session.steps().get(0).cost() >= 9999);
        session.setContext(context(f, false, true, 12, MovimentoExercito.BY_LAND));
        assertEquals("e;ev", session.getDirecoesIdTipo());
        assertTrue(session.steps().get(0).cost() >= 9999);
        assertTrue(session.routeTo(f.target));
        assertEquals("ne;e;se;ev", session.getDirecoesIdTipo());
        assertTrue(session.steps().stream().allMatch(step -> step.cost() < 9999));
    }

    @Test
    void calculationContextCopiesItsTroopListAndReturnedStepsCannotBeAddedTo() {
        var f = new ArmyRouteFinderTest.Fixture();
        var troops = new ArrayList<>(List.of(f.infantry));
        var context = new ArmyMovementSession.Context(troops, true, false, 12,
                MovimentoExercito.BY_LAND, true);
        troops.clear();
        assertEquals(List.of(f.infantry), context.troops());
        var session = new ArmyMovementSession(f.locals, f.start, context);
        assertTrue(session.append("ne"));
        assertThrows(UnsupportedOperationException.class, () -> session.steps().clear());
    }

    private static ArmyMovementSession session(ArmyRouteFinderTest.Fixture f,
            boolean food, boolean evasive, int allowance, int moveType) {
        return new ArmyMovementSession(f.locals, f.start, context(f, food, evasive, allowance, moveType));
    }

    private static ArmyMovementSession.Context context(ArmyRouteFinderTest.Fixture f,
            boolean food, boolean evasive, int allowance, int moveType) {
        return new ArmyMovementSession.Context(List.of(f.infantry), food, evasive, allowance, moveType, true);
    }
}
