package e2e;

import baseLib.BaseModel;
import baseLib.GenericoTableModel;
import control.facade.WorldFacadeCounselor;
import control.services.NacaoConverter;
import java.util.List;
import model.Nacao;
import model.PersonagemOrdem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nations table's cost columns are a SNAPSHOT: the model is built once, and on an EGF open it is
 * built before the orders file is loaded. So the values are only ever right if something refreshes
 * them afterwards - which is exactly what was missing when every nation showed a cost of zero while
 * the forecast below it showed the real total.
 */
class NationCostColumnTest {

    private static final int ORDEM_CUSTO = 1555;

    private static WorldFacadeCounselor wfc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
    }

    @AfterEach
    void clearForecast() {
        for (java.util.Set<PersonagemOrdem> orders : wfc.getMapPersonagemOrdens().values()) {
            orders.clear();
        }
    }

    @Test
    void aModelBuiltBeforeTheOrdersLoadShowsZeroUntilItIsRefreshed() {
        final List<Nacao> rows = wfc.getNacoesJogadorAtivo();
        final Nacao nacao = rows.get(0);
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows);
        final int costCol = columnOf(model, "FINANCAS.COST.ACTIONS");
        assertEquals(0, ((Number) model.getValueAt(0, costCol)).intValue(), "no orders yet, so zero");

        // orders arrive after the table was built - the EGF-open sequence
        addCostlyOrder(nacao);
        assertEquals(0, ((Number) model.getValueAt(0, costCol)).intValue(),
                "the model is a snapshot: it cannot notice on its own");

        assertTrue(NacaoConverter.refreshOrderCostCells(model, rows, nacao));
        assertTrue(((Number) model.getValueAt(0, costCol)).intValue() < 0,
                "refresh did not bring the order cost into the table");
    }

    @Test
    void refreshingEveryRowCoversAWholeOrderFileLoad() {
        final List<Nacao> rows = wfc.getNacoesJogadorAtivo();
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows);
        addCostlyOrder(rows.get(0));

        NacaoConverter.refreshOrderCostCells(model, rows);

        assertTrue(((Number) model.getValueAt(0, columnOf(model, "FINANCAS.COST.ACTIONS"))).intValue() < 0);
    }

    @Test
    void balanceAfterOrdersTracksTheCost() {
        final List<Nacao> rows = wfc.getNacoesJogadorAtivo();
        final Nacao nacao = rows.get(0);
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows);
        final int treasury = ((Number) model.getValueAt(0, columnOf(model, "TREASURY"))).intValue();

        addCostlyOrder(nacao);
        NacaoConverter.refreshOrderCostCells(model, rows, nacao);

        final int projected = ((Number) model.getValueAt(0, columnOf(model, "FINANCAS.FORECAST.FINAL"))).intValue();
        assertEquals(control.services.FinancasConverter.getForecastBalance(nacao), projected,
                "the column must be the same number the forecast's bottom line shows");
        assertNotEquals(treasury, projected,
                "the forecast balance folds in upkeep and revenue, so it cannot equal bare treasury");
    }

    @Test
    void aNationOutsideTheCurrentFilterIsSilentlySkipped() {
        final List<Nacao> rows = wfc.getNacoesJogadorAtivo();
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows);
        final Nacao absent = foreignNation(rows);

        assertFalse(NacaoConverter.refreshOrderCostCells(model, rows, absent));
    }

    private static void addCostlyOrder(Nacao nacao) {
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        wfc.addNacaoPersonagemOrdens(nacao, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_CUSTO), "Kingsguard"));
    }

    private static int columnOf(GenericoTableModel model, String labelKey) {
        final String header = persistenceCommons.SettingsManager.getInstance().getBundleManager().getString(labelKey);
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (header.equals(model.getColumnName(col))) {
                return col;
            }
        }
        throw new IllegalStateException("column not in the nations table: " + labelKey);
    }

    private static Nacao foreignNation(List<Nacao> rows) {
        for (Nacao nacao : wfc.getNacoes().values()) {
            if (!rows.contains(nacao)) {
                return nacao;
            }
        }
        throw new IllegalStateException("fixture has no nation outside the active player's");
    }
}
