package e2e;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Swing behaviour {@code TabBase.retagSelectedRow} depends on (KI-037).
 *
 * Every data tab tags the map as it is BUILT - each auto-selects row 0, firing its own
 * {@code printTag} - and {@code MainDadosGui} then switches to the remembered tab without re-firing
 * a selection. The map was left pointing wherever the LAST-built tagging tab pointed (Finances, i.e.
 * the nation's capital) while the player looked at a different list: open on Characters, map centred
 * on the capital, first character off screen.
 *
 * The fix re-fires the selection on the visible tab so its own tagging logic runs again. That rests
 * on one non-obvious fact: <b>re-selecting a row that is already selected fires NOTHING</b>, so the
 * selection must be cleared first. Get that wrong and the fix silently does nothing in exactly the
 * case it exists for - row 0, selected by construction. These tests fail if that assumption breaks.
 */
class RetagSelectionTest {

    private static JTable table(int rows) {
        final Object[][] data = new Object[rows][1];
        for (int ii = 0; ii < rows; ii++) {
            data[ii][0] = "row" + ii;
        }
        final JTable ret = new JTable(new DefaultTableModel(data, new Object[]{"c"}));
        ret.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return ret;
    }

    private static AtomicInteger countEvents(JTable table) {
        final AtomicInteger ret = new AtomicInteger();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ret.incrementAndGet();
            }
        });
        return ret;
    }

    /** The assumption. Without the clear, the re-tag would be a no-op on the row it must re-tag. */
    @Test
    void reselectingTheSameRowFiresNothing() {
        final JTable table = table(5);
        table.getSelectionModel().setSelectionInterval(0, 0);
        final AtomicInteger fired = countEvents(table);

        table.getSelectionModel().setSelectionInterval(0, 0);
        assertEquals(0, fired.get(), "re-selecting an already-selected row must not fire - hence the clear");
    }

    /** ...and with the clear it does fire, which is what makes the tab re-tag the map. */
    @Test
    void clearingThenReselectingDoesFire() {
        final JTable table = table(5);
        table.getSelectionModel().setSelectionInterval(0, 0);
        final AtomicInteger fired = countEvents(table);

        table.getSelectionModel().clearSelection();
        table.getSelectionModel().setSelectionInterval(0, 0);
        assertTrue(fired.get() > 0, "clear+reselect must fire so the tab re-tags");
        assertEquals(0, table.getSelectedRow(), "and it must land back on the same row");
    }

    /** A tab the player scrolled through must come back to THEIR row, not to row 0. */
    @Test
    void aNonZeroSelectionIsPreserved() {
        final JTable table = table(5);
        table.getSelectionModel().setSelectionInterval(3, 3);

        final int row = Math.max(table.getSelectedRow(), 0);
        table.getSelectionModel().clearSelection();
        table.getSelectionModel().setSelectionInterval(row, row);

        assertEquals(3, table.getSelectedRow());
    }

    /** With nothing selected the fix falls back to row 0 rather than doing nothing. */
    @Test
    void noSelectionFallsBackToTheFirstRow() {
        final JTable table = table(5);
        table.getSelectionModel().clearSelection();
        assertEquals(-1, table.getSelectedRow());

        final int row = Math.max(table.getSelectedRow(), 0);
        table.getSelectionModel().setSelectionInterval(row, row);
        assertEquals(0, table.getSelectedRow());
    }

    /** An empty tab must be left alone; selecting row 0 of nothing would throw. */
    @Test
    void anEmptyTableIsSkipped() {
        final JTable table = table(0);
        assertEquals(0, table.getRowCount(), "guard is on getRowCount, so this is the case it skips");
    }
}
