package e2e;

import control.services.CitySizeCap;
import gui.services.CitySizeCapTableCellRenderer;
import gui.services.LafTint;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.Local;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The city-size column shows a NAME ("Town", "Big City") but must sort by the numeric size, and it
 * tints amber when {@code ;SCC;} caps the city's growth.
 *
 * Two traps are under test here, both of which have bitten this codebase before:
 * {@code DefaultTableCellRenderer} reuses one component across every cell, so an untinted row must
 * explicitly clear the background AND the tooltip a tinted row left behind, or the mark bleeds down
 * the table on the first scroll or sort. The column was also declared {@code Integer.class} for years
 * while holding a plain String, which is why it sorted Burgh, Camp, City, Metropolis, Town.
 */
class CitySizeCapCellRenderingTest {

    private static Local hex(String coords) {
        final Local ret = new Local();
        ret.setCodigo(coords);
        ret.setCoordenadas(coords);
        return ret;
    }

    private static CitySizeCap blocked() {
        return new CitySizeCap(3, "Town", hex("1412"));
    }

    private static CitySizeCap clear() {
        return new CitySizeCap(3, "Town", null);
    }

    private static JTable table(Object... values) {
        final Object[][] rows = new Object[values.length][1];
        for (int ii = 0; ii < values.length; ii++) {
            rows[ii][0] = values[ii];
        }
        final DefaultTableModel model = new DefaultTableModel(rows, new Object[]{"size"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return CitySizeCap.class;
            }
        };
        return new JTable(model);
    }

    private static JLabel render(JTable table, CitySizeCapTableCellRenderer renderer, int row) {
        return (JLabel) renderer.getTableCellRendererComponent(
                table, table.getValueAt(row, 0), false, false, row, 0);
    }

    @Test
    void showsTheSizeNameNotTheNumber() {
        final JTable table = table(clear());
        assertEquals("Town", render(table, new CitySizeCapTableCellRenderer(), 0).getText());
    }

    @Test
    void sortsBySizeNotAlphabetically() {
        final CitySizeCap town = new CitySizeCap(3, "Town", null);
        final CitySizeCap metropolis = new CitySizeCap(5, "Metropolis", null);
        assertEquals(-1, Integer.signum(town.compareTo(metropolis)),
                "a town is smaller than a metropolis, however the names sort");
    }

    @Test
    void aCappedCityIsTinted() {
        final JTable table = table(blocked());
        final JLabel cell = render(table, new CitySizeCapTableCellRenderer(), 0);
        assertEquals(LafTint.AMBER.bg(), cell.getBackground());
    }

    /**
     * The absence of a blocker must never be painted: the client cannot see a big city hidden from its
     * EGF, so "no mark" has to stay silent rather than promise an upgrade will go through.
     */
    @Test
    void anUncappedCityIsNotTintedAndCarriesNoTooltip() {
        final JTable table = table(clear());
        final JLabel cell = render(table, new CitySizeCapTableCellRenderer(), 0);
        assertEquals(table.getBackground(), cell.getBackground());
        assertNull(cell.getToolTipText());
    }

    @Test
    void aTintDoesNotBleedIntoTheNextRow() {
        final JTable table = table(blocked(), clear());
        final CitySizeCapTableCellRenderer renderer = new CitySizeCapTableCellRenderer();
        final JLabel first = render(table, renderer, 0);
        assertNotEquals(table.getBackground(), first.getBackground(), "row 0 is the capped one");

        final JLabel second = render(table, renderer, 1);
        assertEquals(table.getBackground(), second.getBackground(),
                "the renderer is reused, so an untinted row must reset the background");
        assertNull(second.getToolTipText(),
                "the tooltip leaks through renderer reuse exactly like the background");
    }
}
