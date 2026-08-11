package e2e;

import gui.services.NumberTableCellRenderer;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import utils.StringIntSortedCell;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The army-size cell is a {@link StringIntSortedCell}: it sorts by the numeric size but DISPLAYS a
 * name - "Vast army", "Small navy", "Huge garrison" - which is also how the column tells a navy from
 * an army from a garrison. It carries that name in {@code toString()} and extends {@link Number} only
 * so the column sorts by size rather than alphabetically.
 * <p>
 * That makes it fragile in one specific way: any renderer that sees a {@code Number} and formats it
 * numerically throws the name away and shows a bare 1-5. The armies table declared the column
 * {@code Integer.class} by mistake for years, which was harmless only while the default Integer
 * renderer happened to call {@code toString()}.
 */
class ArmySizeCellRenderingTest {

    private static String render(Class<?> columnClass, Object value) {
        final DefaultTableModel model = new DefaultTableModel(new Object[][]{{value}}, new Object[]{"size"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnClass;
            }
        };
        final JTable table = new JTable(model);
        table.setDefaultRenderer(Integer.class, new NumberTableCellRenderer());
        return ((JLabel) table.getCellRenderer(0, 0)
                .getTableCellRendererComponent(table, value, false, false, 0, 0)).getText();
    }

    @Test
    void showsTheSizeNameNotTheNumber() {
        assertEquals("Vast army", render(StringIntSortedCell.class, new StringIntSortedCell(4, "Vast army")),
                "the armies table must show the size NAME, not its sort key");
    }

    @Test
    void keepsTheNavyAndGarrisonWording() {
        assertEquals("Small navy", render(StringIntSortedCell.class, new StringIntSortedCell(1, "Small navy")));
        assertEquals("Huge garrison", render(StringIntSortedCell.class, new StringIntSortedCell(5, "Huge garrison")));
    }

    /**
     * Defence in depth: even if a column is mis-declared as Integer, a Number that carries its own
     * display text must not be reformatted into a bare digit. This is the exact regression that turned
     * the size column into 1-5.
     */
    @Test
    void aNumberWithItsOwnTextSurvivesAnIntegerColumn() {
        assertEquals("Vast army", render(Integer.class, new StringIntSortedCell(4, "Vast army")),
                "a mis-declared column must not strip the cell's own wording");
    }

    @Test
    void plainIntegersStillGetGroupedThousands() {
        assertEquals(java.text.NumberFormat.getIntegerInstance().format(12345),
                render(Integer.class, 12345));
    }
}
