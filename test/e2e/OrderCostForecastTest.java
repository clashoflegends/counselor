package e2e;

import baseLib.BaseModel;
import control.facade.WorldFacadeCounselor;
import model.Nacao;
import model.Ordem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the next-turn cost forecast, which is a set of pending orders kept ALONGSIDE the actors
 * (fed incrementally as orders are saved, cleared, or loaded from a file) rather than derived from
 * them. That parallel bookkeeping is what shipped a doubled forecast: an orders file read twice
 * registered every order a second time, so players saw twice the real cost.
 * <p>
 * The invariants below are the ones that break when the two sides drift apart.
 */
class OrderCostForecastTest {

    /** 1555 CriarAc - cost 2000 in the fixture scenario. */
    private static final int ORDEM_CUSTO_2000 = 1555;

    private static WorldFacadeCounselor wfc;
    private static BaseModel pc;
    private static Nacao nacao;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
        pc = TestWorld.freshPersonagem(wfc);
        nacao = pc.getNacao();
    }

    @BeforeEach
    void emptyTheForecast() {
        wfc.getMapPersonagemOrdens(nacao).clear();
    }

    @Test
    void theFixtureOrderActuallyCostsSomething() {
        // If this ever returns 0 the rest of the class silently stops asserting anything.
        assertTrue(wfc.getOrderCost(TestWorld.buildOrder(pc, order(), "Kingsguard"), nacao) > 0,
                "order " + ORDEM_CUSTO_2000 + " must have a cost for this test to mean anything");
    }

    @Test
    void aPendingOrderIsCountedOnce() {
        PersonagemOrdem po = TestWorld.buildOrder(pc, order(), "Kingsguard");
        int cost = wfc.getOrderCost(po, nacao);

        assertTrue(wfc.addNacaoPersonagemOrdens(nacao, po));
        assertEquals(cost, wfc.getNacaoOrderCost(nacao));
    }

    @Test
    void registeringTheSameOrderTwiceDoesNotDoubleItsCost() {
        PersonagemOrdem po = TestWorld.buildOrder(pc, order(), "Kingsguard");
        int cost = wfc.getOrderCost(po, nacao);

        wfc.addNacaoPersonagemOrdens(nacao, po);
        assertFalse(wfc.addNacaoPersonagemOrdens(nacao, po), "re-registering must be a no-op");

        assertEquals(cost, wfc.getNacaoOrderCost(nacao), "the same order was charged twice");
    }

    @Test
    void clearingAnOrderRemovesItsCost() {
        PersonagemOrdem po = TestWorld.buildOrder(pc, order(), "Kingsguard");
        wfc.addNacaoPersonagemOrdens(nacao, po);

        assertTrue(wfc.remNacaoPersonagemOrdens(nacao, po));
        assertEquals(0, wfc.getNacaoOrderCost(nacao));
    }

    /**
     * The doubling bug in one assertion. Re-reading an orders file builds NEW order objects, and the
     * forecast set holds them by identity, so a reload registers the same order all over again unless
     * the previous copies are dropped first - which is what the load path now does before it wipes the
     * actor. Two equal-but-distinct copies in the set must be treated as the accident they are.
     */
    @Test
    void twoDistinctCopiesOfTheSameOrderAreTheAccidentThatDoubledTheForecast() {
        PersonagemOrdem first = TestWorld.buildOrder(pc, order(), "Kingsguard");
        PersonagemOrdem reloaded = TestWorld.buildOrder(pc, order(), "Kingsguard");
        int cost = wfc.getOrderCost(first, nacao);

        wfc.addNacaoPersonagemOrdens(nacao, first);
        wfc.addNacaoPersonagemOrdens(nacao, reloaded);
        assertEquals(cost * 2, wfc.getNacaoOrderCost(nacao),
                "distinct instances are counted separately - this is why the load path must drop the "
                + "actor's registered orders before re-registering them");

        // Dropping the stale copy, as the order-file load does, restores the true cost.
        assertTrue(wfc.remNacaoPersonagemOrdens(nacao, first));
        assertEquals(cost, wfc.getNacaoOrderCost(nacao));
    }

    @Test
    void anEmptyForecastCostsNothing() {
        assertEquals(0, wfc.getNacaoOrderCost(nacao));
    }

    private static Ordem order() {
        return TestWorld.ordem(wfc, ORDEM_CUSTO_2000);
    }
}
