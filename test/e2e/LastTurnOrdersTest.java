package e2e;

import baseLib.BaseModel;
import control.facade.WorldFacadeCounselor;
import control.services.LastTurnOrders;
import java.util.List;
import model.Nacao;
import model.Ordem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Do it again" repeats last turn's orders, which arrive inside the turn file as the actor's executed
 * actions. The dangerous part is not the copying but the SHARING: a saved order is held by identity in
 * the actor's slots, in the cost forecast set and in the submission, so handing the same instance (or
 * the same parameter list) to two slots would silently under-count the forecast and alias into the
 * saved orders file. These pin the copy, and the filter that keeps a no-longer-available order out.
 */
class LastTurnOrdersTest {

    private static final int ORDEM_A = 1555;
    private static final int ORDEM_B = 1942;

    private static WorldFacadeCounselor wfc;
    private static BaseModel pc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
        pc = TestWorld.freshPersonagem(wfc);
    }

    @BeforeEach
    void clearExecuted() {
        pc.getAcaoExecutadas().clear();
        pc.remAcoes();
    }

    private static java.util.Collection<PersonagemOrdem> executed() {
        return pc.getAcaoExecutadas().values();
    }

    @Test
    void anActorWithNothingLastTurnHasNothingToRepeat() {
        assertFalse(LastTurnOrders.hasAny(executed()));
        assertTrue(LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_A)).isEmpty());
    }

    @Test
    void repeatsAnOrderThatIsStillAvailable() {
        pc.addAcaoExecutada(TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));

        List<PersonagemOrdem> repeatable = LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_A, ORDEM_B));

        assertEquals(1, repeatable.size());
        assertEquals(ORDEM_A, repeatable.get(0).getOrdem().getNumero());
        assertEquals(List.of("Kingsguard"), repeatable.get(0).getParametrosId());
    }

    @Test
    void skipsAnOrderTheActorCanNoLongerChoose() {
        pc.addAcaoExecutada(TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        pc.addAcaoExecutada(TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_B), "0101"));

        List<PersonagemOrdem> repeatable = LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_B));

        assertEquals(1, repeatable.size(), "an unavailable order must not be repeated");
        assertEquals(ORDEM_B, repeatable.get(0).getOrdem().getNumero());
        assertEquals(1, LastTurnOrders.countUnavailable(executed(), List.of(ORDEM_B)));
    }

    /** The forecast set dedups by reference: two slots must never receive the same order object. */
    @Test
    void everyRepeatIsAFreshObjectSoTheForecastCountsItOnce() {
        PersonagemOrdem lastTurn = TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard");
        pc.addAcaoExecutada(lastTurn);

        PersonagemOrdem first = LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_A)).get(0);
        PersonagemOrdem second = LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_A)).get(0);

        assertNotSame(lastTurn, first, "repeat handed out the live executed order");
        assertNotSame(first, second, "two repeats shared one order object");

        final Nacao nacao = pc.getNacao();
        wfc.getMapPersonagemOrdens(nacao).clear();
        try {
            int cost = wfc.getOrderCost(first, nacao);
            assertTrue(wfc.addNacaoPersonagemOrdens(nacao, first));
            assertTrue(wfc.addNacaoPersonagemOrdens(nacao, second), "the forecast treated them as one order");
            assertEquals(cost * 2, wfc.getNacaoOrderCost(nacao));
        } finally {
            wfc.getMapPersonagemOrdens(nacao).clear();
        }
    }

    @Test
    void parameterListsAreCopiedNotAliased() {
        PersonagemOrdem lastTurn = TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard");
        pc.addAcaoExecutada(lastTurn);

        PersonagemOrdem repeat = LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_A)).get(0);
        repeat.getParametrosId().set(0, "Queensguard");

        assertEquals(List.of("Kingsguard"), lastTurn.getParametrosId(),
                "editing a repeated order changed last turn's record");
    }

    @Test
    void keepsTheScenarioOrderItselfSharedSoTheSavedFileCarriesTheRightCode() {
        Ordem ordem = TestWorld.ordem(wfc, ORDEM_A);
        pc.addAcaoExecutada(TestWorld.buildOrder(pc, ordem, "Kingsguard"));

        assertSame(ordem, LastTurnOrders.listRepeatable(executed(), pc.getNome(), List.of(ORDEM_A)).get(0).getOrdem());
    }
}
