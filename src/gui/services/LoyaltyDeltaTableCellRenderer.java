/*
 * LoyaltyDeltaTableCellRenderer.java
 */
package gui.services;

import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colors the city loyalty-change ("Loyalty var") column as a diverging scale, so a player can spot
 * cities gaining or losing loyalty at a glance. Uses the shared LAF-aware {@link LafTint} palette
 * (light tints + dark-mode variants), same as the loyalty column and the Difficulty column:
 *
 * <pre>
 *   delta &lt; -9 : red    (sharp drop)
 *   delta &lt; -2 : amber  (mild drop)
 *   delta &gt; +2 : green  (mild gain)
 *   delta &gt; +9 : blue   (strong gain)
 *   otherwise  : neutral
 * </pre>
 *
 * The selection highlight is left to the L&amp;F (cells are tinted only when not selected), matching
 * DifficultyColorTableCellRenderer.
 *
 * @author jmoura
 */
public class LoyaltyDeltaTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final int STRONG_DROP = -9;
    private static final int MILD_DROP = -2;
    private static final int MILD_GAIN = 2;
    private static final int STRONG_GAIN = 9;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        if (!isSelected) {
            LafTint tint = null;
            if (value instanceof Integer) {
                final int delta = (Integer) value;
                if (delta < STRONG_DROP) {
                    tint = LafTint.RED;
                } else if (delta < MILD_DROP) {
                    tint = LafTint.AMBER;
                } else if (delta > STRONG_GAIN) {
                    tint = LafTint.BLUE;
                } else if (delta > MILD_GAIN) {
                    tint = LafTint.GREEN;
                }
            }
            if (tint != null) {
                c.setBackground(tint.bg());
                c.setForeground(LafTint.fg());
            } else {
                // Explicit reset (see CityLoyaltyTableCellRenderer): clear any tint left on the reused
                // component so neutral deltas (-2..+2) don't inherit a colour on scroll/sort/filter.
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }
}
