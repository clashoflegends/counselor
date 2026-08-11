package e2e;

import baseLib.BaseModel;
import control.facade.WorldFacadeCounselor;
import control.support.ActorInterfaceFactory;
import gui.tabs.TabFinancesGui;
import javax.swing.table.TableModel;
import model.Nacao;
import model.Personagem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import persistenceCommons.SettingsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Player report: the Total orders cost / Forecast Balance columns "only change when the turn opens -
 * when you enter orders, nothing happens".
 * <p>
 * The cause was not the dispatch: it arrived, and the forecast set was updated. The controller decided
 * whether to repaint from whether IT had won the race to mutate that shared set. Every turn-open
 * builds a new Finances tab with a new controller while the previous one is still registered (dispatch
 * receivers are weak, so a discarded tab lingers until GC), so from the second turn-open onward the
 * stale invisible tab consumed the state change and the visible one concluded "nothing changed".
 * Opening a turn still looked right because ACTIONS_RELOAD repaints every row unconditionally.
 * <p>
 * These drive the REAL save chokepoint through the REAL tab.
 */
class FinancesLiveRefreshTest {

    private static final int ORDEM_CUSTO = 1555;

    private static WorldFacadeCounselor wfc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
    }

    private static TabFinancesGui newFinancesTab() {
        return new TabFinancesGui("Finances", "Finances", null);
    }

    /** A tab snapshots the forecast when it is built, so every test starts from an empty one. */
    @org.junit.jupiter.api.BeforeEach
    void emptyTheForecast() {
        for (java.util.Set<model.PersonagemOrdem> orders : wfc.getMapPersonagemOrdens().values()) {
            orders.clear();
        }
    }

    /** Saving an order must move the cells of the tab the player is looking at. */
    @Test
    void savingAnOrderUpdatesTheVisibleTable() {
        final BaseModel pc = TestWorld.freshPersonagem(wfc);
        final Nacao nacao = pc.getNacao();
        final TabFinancesGui tab = newFinancesTab();

        final int before = costCell(tab, nacao);
        save(pc);

        assertNotEquals(before, costCell(tab, nacao), "entering an order did not update the table");
        assertEquals(wfc.getNacaoOrderCost(nacao) * -1, costCell(tab, nacao));
    }

    /**
     * The regression itself. A second tab is exactly what reopening a turn produces, with the first
     * still registered for dispatches until it is collected. The NEWEST tab is the visible one and it
     * must refresh, even though an older controller already applied the same change to the shared set.
     */
    @Test
    void aReopenedTurnStillUpdatesWhileEnteringOrders() {
        final BaseModel pc = TestWorld.freshPersonagem(wfc);
        final Nacao nacao = pc.getNacao();
        final TabFinancesGui stale = newFinancesTab();
        final TabFinancesGui visible = newFinancesTab();

        final int before = costCell(visible, nacao);
        save(pc);

        assertNotEquals(before, costCell(visible, nacao),
                "the visible tab did not update - an older tab consumed the change");
        assertEquals(costCell(stale, nacao), costCell(visible, nacao),
                "every live tab should end up showing the same cost");
    }

    /** What the confirm button on an order actually calls. */
    private static void save(BaseModel pc) {
        ActorInterfaceFactory.getInstance().getActorInterface((Personagem) pc)
                .doOrderSave(0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_CUSTO), "Kingsguard"));
    }

    private static int costCell(TabFinancesGui tab, Nacao nacao) {
        final TableModel model = tab.getMainLista().getModel();
        return ((Number) model.getValueAt(rowOf(model, nacao), columnOf(model, "FINANCAS.COST.ACTIONS"))).intValue();
    }

    private static int rowOf(TableModel model, Nacao nacao) {
        final String name = new business.facade.NacaoFacade().getNome(nacao);
        for (int rr = 0; rr < model.getRowCount(); rr++) {
            if (name.equals(model.getValueAt(rr, 0))) {
                return rr;
            }
        }
        throw new IllegalStateException("nation not in the table: " + name);
    }

    private static int columnOf(TableModel model, String labelKey) {
        final String header = SettingsManager.getInstance().getBundleManager().getString(labelKey);
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (header.equals(model.getColumnName(col))) {
                return col;
            }
        }
        throw new IllegalStateException("column not found: " + labelKey);
    }
}
