package e2e;

import baseLib.BaseModel;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import control.services.OrdersHashService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The orders hash is what the "orders sent?" indicator compares against the copy the site holds, so
 * a hash that moves on its own reads as "unsent" to a player who did send, and a hash that fails to
 * move reads as "sent" for orders that were never uploaded. Both are wrong in a way the player acts
 * on. These pin the two properties the service documents: order-of-entry independence, and
 * sensitivity to anything that actually changes the submission.
 */
class OrdersHashTest {

    private static final int ORDEM_A = 1555;
    private static final int ORDEM_B = 1942;

    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static WorldFacadeCounselor wfc;
    private static BaseModel pc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
        pc = TestWorld.freshPersonagem(wfc);
    }

    @BeforeEach
    void clearOrders() {
        pc.remAcoes();
    }

    @Test
    void hashesTheLoadedTurn() {
        assertNotNull(OrdersHashService.computeHash(wfc));
    }

    @Test
    void doesNotDependOnTheOrderTheActionsWereEnteredIn() {
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        ordemFacade.setOrdem(pc, 1, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_B), "0101"));
        String entered = OrdersHashService.computeHash(wfc);

        pc.remAcoes();
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_B), "0101"));
        ordemFacade.setOrdem(pc, 1, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));

        assertEquals(entered, OrdersHashService.computeHash(wfc),
                "reshuffling the same actions must not look like an unsent change");
    }

    @Test
    void changesWhenAnOrderParameterChanges() {
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        String before = OrdersHashService.computeHash(wfc);

        pc.remAcoes();
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Queensguard"));

        assertNotEquals(before, OrdersHashService.computeHash(wfc),
                "an edited target must not still read as sent");
    }

    @Test
    void changesWhenAnOrderIsAddedAndReturnsWhenItIsRemoved() {
        String empty = OrdersHashService.computeHash(wfc);

        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        String withOrder = OrdersHashService.computeHash(wfc);
        assertNotEquals(empty, withOrder, "adding an order must move the hash");

        pc.remAcoes();
        assertEquals(empty, OrdersHashService.computeHash(wfc),
                "removing the order must restore the previous hash");
    }

    @Test
    void isStableAcrossRepeatedComputationOfTheSameOrders() {
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        assertEquals(OrdersHashService.computeHash(wfc), OrdersHashService.computeHash(wfc));
    }
}
