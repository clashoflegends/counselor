/*
 * NumberTableCellRenderer.java
 */
package gui.services;

import java.awt.Component;
import java.io.Serializable;
import java.text.NumberFormat;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Default renderer for every whole-number column in the app: grouped thousands (123,456 / -123,123)
 * and a red tint on negatives, so a loss reads as a loss at a glance and a six-figure treasury does
 * not have to be counted digit by digit.
 * <p>
 * Installed as the table-level default for {@code Integer.class} in
 * {@code TabBase.doConfigTableColumns}, which every data table and sub-table goes through. A column
 * with its own renderer (loyalty, loyalty delta, difficulty, startup points) keeps it - Swing asks
 * the column first and only falls back to the class default.
 * <p>
 * Grouping follows the player's locale, so the separator matches the rest of their system.
 *
 * @author jmoura
 */
public class NumberTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        // Only PLAIN whole numbers are reformatted. A Number subclass that carries its own display
        // text - StringIntSortedCell, which extends Number purely so a column sorts by size while
        // showing "Vast army" / "Small navy" - must keep its toString(), or the wording is replaced by
        // its sort key. That is exactly how the armies table's size column became a bare 1-5.
        final boolean plain = value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte;
        final boolean negative = plain && ((Number) value).longValue() < 0;
        if (plain) {
            setText(FORMAT.format(value));
        }
        if (!isSelected) {
            if (negative) {
                c.setBackground(LafTint.RED.bg());
                c.setForeground(LafTint.fg());
            } else {
                // Explicit reset: the renderer component is reused across cells, and setBackground
                // poisons its unselectedBackground, so a positive cell would otherwise inherit the tint
                // of a negative one on scroll/sort/filter (see CityLoyaltyTableCellRenderer).
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }
}
