/*
 * CityLoyaltyTableCellRenderer.java
 */
package gui.services;

import control.services.CityLoyalty;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colors the city-loyalty column to warn about imminent size reductions, using the shared LAF-aware
 * {@link LafTint} palette (light tints + dark-mode variants): RED when the city is already below the
 * reduction threshold, AMBER when it is within the amber margin of it.
 *
 * The RED/AMBER classification lives on the {@link CityLoyalty} cell value (shared with the cities-tab
 * risk filters, so coloring and filtering can never disagree), so this renderer stays trivial. The
 * selection highlight is left to the L&amp;F (cells are tinted only when not selected).
 *
 * @author jmoura
 */
public class CityLoyaltyTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    public CityLoyaltyTableCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        if (!isSelected) {
            LafTint tint = null;
            if (value instanceof CityLoyalty) {
                final CityLoyalty cl = (CityLoyalty) value;
                if (cl.isImminentDecay()) {
                    tint = LafTint.RED;
                } else if (cl.isAtDecayRisk()) {
                    tint = LafTint.AMBER;
                }
            }
            if (tint != null) {
                c.setBackground(tint.bg());
                c.setForeground(LafTint.fg());
            } else {
                // Explicit reset. DefaultTableCellRenderer reuses one component and stores the last
                // setBackground as its "unselected" color, which super reuses for the next unselected
                // cell - so a neutral cell must clear any tint left by a previously rendered row, or
                // zeros/neutrals inherit red/amber on scroll, sort, or filter changes.
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }
}
