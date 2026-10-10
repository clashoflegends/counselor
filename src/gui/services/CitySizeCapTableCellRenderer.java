/*
 * CitySizeCapTableCellRenderer.java
 */
package gui.services;

import control.services.CidadeConverter;
import control.services.CitySizeCap;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Tints the city-size column AMBER when a neighbouring city caps this one's growth under {@code ;SCC;},
 * and names the blocker in the tooltip. Without it the player only learns of the cap by spending a turn
 * on the upgrade order and reading the refusal in the turn result (game 915, turn 6).
 *
 * Deliberately one-directional: a blocked city is marked, an unblocked one gets nothing. The rule can
 * miss a blocker that is hidden from the player's EGF, so a mark is trustworthy but its absence is not,
 * and a positive "you can upgrade this" colour would be a promise the client cannot keep.
 * See {@link CitySizeCap} for why this is evaluated on own cities only.
 *
 * AMBER rather than RED on purpose: RED two columns over already means "this city is about to LOSE a
 * size", and a capped city has lost nothing.
 *
 * @author jmoura
 */
public class CitySizeCapTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    public CitySizeCapTableCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String tip = null;
        if (value instanceof CitySizeCap && ((CitySizeCap) value).isBlocked()) {
            tip = CidadeConverter.getSizeCapMessage((CitySizeCap) value);
        }
        if (!isSelected) {
            if (tip != null) {
                c.setBackground(LafTint.AMBER.bg());
                c.setForeground(LafTint.fg());
            } else {
                // Explicit reset. DefaultTableCellRenderer reuses one component and stores the last
                // setBackground as its "unselected" color, which super reuses for the next unselected
                // cell - so a neutral cell must clear any tint left by a previously rendered row, or
                // untinted sizes inherit amber on scroll, sort, or filter changes.
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        // Always set, including to null: the tooltip leaks through renderer reuse exactly like the
        // background does, so a capped row would otherwise lend its text to every later row.
        this.setToolTipText(tip);
        return c;
    }
}
