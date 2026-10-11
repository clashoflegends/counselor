/*
 * CityThreatTableCellRenderer.java
 */
package gui.services;

import control.services.CidadeConverter;
import control.services.CityThreat;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Tints the Threat column RED where a hostile force could be standing on the city next turn and AMBER
 * where it could the turn after, and names the forces in the tooltip.
 *
 * Red here and not amber, unlike the city-size cap two columns over: a cap costs a wasted order, an
 * army arriving costs the city. The two-turn band is amber because it is also an UPPER bound - see
 * {@code ThreatReach} on why movement points not carrying over makes the second turn a ceiling.
 *
 * <b>One-directional, exactly like {@link CitySizeCapTableCellRenderer}.</b> A threatened row is
 * marked and an unthreatened one gets nothing, because an enemy army the player has not scouted is
 * not in his EGF at all: a mark is trustworthy, its absence is not. The tooltip says so in as many
 * words, since the column is the only place the player would think to look for that caveat.
 *
 * @author jmoura
 */
public class CityThreatTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    public CityThreatTableCellRenderer() {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                row, column);
        String tip = null;
        LafTint tint = null;
        if (value instanceof CityThreat && ((CityThreat) value).isThreatened()) {
            final CityThreat threat = (CityThreat) value;
            tip = CidadeConverter.getThreatMessage(threat);
            tint = threat.getTurns() <= 1 ? LafTint.RED : LafTint.AMBER;
        }
        if (!isSelected) {
            if (tint != null) {
                c.setBackground(tint.bg());
                c.setForeground(LafTint.fg());
            } else {
                // Explicit reset. DefaultTableCellRenderer reuses ONE component and keeps the last
                // setBackground as its unselected colour, so an untinted row would inherit the red
                // of a threatened one on scroll, sort or filter - and "this city is about to be
                // overrun" is the worst possible thing to leak onto a row at random.
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        // Always set, including to null: the tooltip leaks through renderer reuse exactly like the
        // background does.
        this.setToolTipText(tip);
        return c;
    }
}
