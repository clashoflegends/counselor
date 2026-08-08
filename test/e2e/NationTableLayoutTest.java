package e2e;

import baseLib.GenericoTableModel;
import control.facade.WorldFacadeCounselor;
import control.services.NacaoConverter;
import java.util.ArrayList;
import java.util.List;
import model.Nacao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import persistenceCommons.SettingsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Nations and Finances tabs show the same facts about a nation in a different order, and the
 * Finances one prices the resources instead of counting them. Both orders are a deliberate reading
 * sequence, so they are pinned here - the columns used to live in two hand-maintained parallel arrays
 * (headers in one, values in another) where a single insertion silently shifted every value right.
 */
class NationTableLayoutTest {

    private static WorldFacadeCounselor wfc;
    private static List<Nacao> rows;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
        rows = wfc.getNacoesJogadorAtivo();
    }

    @Test
    void nationsTabLeadsWithIdentityAndEndsWithResources() {
        final List<String> headers = headersOf(NacaoConverter.Layout.NATIONS);
        final List<String> expected = new ArrayList<>(List.of(
                "NOME", "ALIANCA", "RACA", "ATIVA", "CIDADE.CAPITAL", "PERSONAGENS.SLOT", "PERSONAGENS.KNOWN",
                "LEALDADE", "LEALDADE.VARIACAO", "TROPAS", "PONTOS.VITORIA", "PONTOS.DOMINATION",
                "IMPOSTOS", "TREASURY", "FINANCAS.COST.ACTIONS", "FINANCAS.FORECAST.COLUMN"));
        assertHeadOf(headers, expected);
        assertTailIs(headers, List.of("JOGADOR", "JOGADOR.EMAIL"));
    }

    @Test
    void financesTabLeadsWithMoney() {
        final List<String> headers = headersOf(NacaoConverter.Layout.FINANCES);
        assertHeadOf(headers, List.of(
                "NOME", "ALIANCA", "IMPOSTOS", "TREASURY", "FINANCAS.COST.ACTIONS", "FINANCAS.FORECAST.COLUMN"));
        assertTailIs(headers, List.of("JOGADOR", "JOGADOR.EMAIL"));
    }

    /** The turn-0 spending column belongs on the Nations tab only, and second when it is there. */
    @Test
    void startupPackagePointsAreNationsTabOnly() {
        final String startup = label("STARTUP.POINTS");
        final List<String> nations = headersOf(NacaoConverter.Layout.NATIONS);
        final List<String> finances = headersOf(NacaoConverter.Layout.FINANCES);

        assertFalse(finances.contains(startup), "the startup-package column has no place on Finances");
        if (nations.contains(startup)) {
            assertEquals(1, nations.indexOf(startup), "startup points belong right after the name");
        }
    }

    /** Whoever paints or writes that column must find it by header, never by a fixed index. */
    @Test
    void theStartupColumnIsFoundByHeader() {
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows, NacaoConverter.Layout.NATIONS);
        final int col = NacaoConverter.getStartupPointsColumn(model);
        if (col >= 0) {
            assertEquals(label("STARTUP.POINTS"), model.getColumnName(col));
        }
        assertEquals(-1, NacaoConverter.getStartupPointsColumn(
                NacaoConverter.getNacaoModel(rows, NacaoConverter.Layout.FINANCES)),
                "Finances has no startup column, so the lookup must say so rather than guess");
    }

    @Test
    void bothLayoutsCarryTheSameColumnsApartFromStartup() {
        final List<String> nations = new ArrayList<>(stripGoldMarker(headersOf(NacaoConverter.Layout.NATIONS)));
        final List<String> finances = stripGoldMarker(headersOf(NacaoConverter.Layout.FINANCES));
        nations.remove(label("STARTUP.POINTS"));

        assertTrue(nations.containsAll(finances) && finances.containsAll(nations),
                "a column present on one tab and missing from the other is a reordering mistake");
    }

    /** Finances marks its resource columns "<name> $" because they hold gold, not units. */
    @Test
    void financesMarksResourceColumnsAsMoney() {
        final List<String> finances = headersOf(NacaoConverter.Layout.FINANCES);
        final List<String> nations = headersOf(NacaoConverter.Layout.NATIONS);
        final List<String> marked = new ArrayList<>();
        for (String header : finances) {
            if (header.endsWith(" $")) {
                marked.add(header.substring(0, header.length() - 2));
            }
        }
        assertFalse(marked.isEmpty(), "no resource column is marked as money");
        for (String resource : marked) {
            assertTrue(nations.contains(resource),
                    "the marker must only be added, never rename the column: " + resource);
        }
    }

    /** Compares layouts on the underlying column, ignoring the Finances money marker. */
    private static List<String> stripGoldMarker(List<String> headers) {
        final List<String> ret = new ArrayList<>(headers.size());
        for (String header : headers) {
            ret.add(header.endsWith(" $") ? header.substring(0, header.length() - 2) : header);
        }
        return ret;
    }

    @Test
    void everyRowIsFilledForEveryColumn() {
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows, NacaoConverter.Layout.FINANCES);
        assertEquals(rows.size(), model.getRowCount());
        for (int rr = 0; rr < model.getRowCount(); rr++) {
            for (int cc = 0; cc < model.getColumnCount(); cc++) {
                model.getValueAt(rr, cc); //a misaligned layout throws or returns a value of the wrong type
            }
        }
    }

    private static List<String> headersOf(NacaoConverter.Layout layout) {
        final GenericoTableModel model = NacaoConverter.getNacaoModel(rows, layout);
        final List<String> ret = new ArrayList<>();
        for (int col = 0; col < model.getColumnCount(); col++) {
            ret.add(model.getColumnName(col));
        }
        return ret;
    }

    /** Asserts the given label keys appear in this order at the front, skipping absent optional ones. */
    private static void assertHeadOf(List<String> headers, List<String> labelKeys) {
        int at = 0;
        for (String key : labelKeys) {
            final String header = label(key);
            if (!headers.contains(header)) {
                continue; //scenario-dependent column (capital), not displayed here
            }
            assertEquals(header, headers.get(at++), "column " + (at) + " is not " + key);
        }
    }

    private static void assertTailIs(List<String> headers, List<String> labelKeys) {
        for (int ii = 0; ii < labelKeys.size(); ii++) {
            assertEquals(label(labelKeys.get(ii)), headers.get(headers.size() - labelKeys.size() + ii));
        }
    }

    private static String label(String key) {
        return SettingsManager.getInstance().getBundleManager().getString(key);
    }
}
