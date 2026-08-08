package e2e;

import gui.services.LafTint;
import gui.services.NumberTableCellRenderer;
import java.awt.Component;
import java.text.NumberFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Every whole-number column in the app renders through this one renderer: grouped thousands, and a
 * red tint when the number is negative.
 * <p>
 * The trap it has to survive is that Swing reuses ONE component for every cell, and setBackground
 * poisons its unselected background - so a positive cell drawn after a negative one inherits the tint
 * unless it resets explicitly. That has bitten this codebase twice already (city loyalty, difficulty).
 */
class NumberCellRenderingTest {

    private static JTable table;
    private static NumberTableCellRenderer renderer;

    @BeforeAll
    static void buildTable() {
        table = new JTable(new DefaultTableModel(new Object[][]{{0}}, new Object[]{"n"}));
        renderer = new NumberTableCellRenderer();
    }

    private static Component render(Object value) {
        return renderer.getTableCellRendererComponent(table, value, false, false, 0, 0);
    }

    @Test
    void groupsThousands() {
        assertEquals(NumberFormat.getIntegerInstance().format(123456), ((JLabel) render(123456)).getText());
    }

    @Test
    void groupsNegativeThousands() {
        assertEquals(NumberFormat.getIntegerInstance().format(-123123), ((JLabel) render(-123123)).getText());
    }

    @Test
    void tintsANegativeNumber() {
        assertEquals(LafTint.RED.bg(), render(-1).getBackground());
    }

    @Test
    void leavesAPositiveNumberAlone() {
        assertEquals(table.getBackground(), render(1).getBackground());
    }

    @Test
    void treatsZeroAsNotNegative() {
        assertEquals(table.getBackground(), render(0).getBackground());
    }

    /** The reuse trap: a positive cell drawn right after a negative one must not inherit the tint. */
    @Test
    void doesNotLeakTheTintOntoTheNextCell() {
        render(-500);
        assertNotEquals(LafTint.RED.bg(), render(500).getBackground(),
                "the red tint leaked onto a positive cell");
        assertEquals(table.getBackground(), render(500).getBackground());
    }

    @Test
    void leavesBlankAndNonNumericCellsUntouched() {
        assertEquals("", ((JLabel) render(null)).getText());
        assertEquals(table.getBackground(), render(null).getBackground());
        assertEquals("n/a", ((JLabel) render("n/a")).getText());
    }
}
