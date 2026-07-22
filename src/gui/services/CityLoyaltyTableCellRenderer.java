/*
 * CityLoyaltyTableCellRenderer.java
 */
package gui.services;

import control.services.CityLoyalty;
import java.awt.Color;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colors the city-loyalty column to warn about imminent size reductions, following the same visual
 * language as {@link LifeTableCellRenderer} (the character life column): RED when the city is already
 * below the reduction threshold, ORANGE when it is within {@code amberMargin} points of it. Explicit
 * foreground/background pairs keep it readable under any Look and Feel (light or FlatLaf dark).
 *
 * The per-city threshold rides on the {@link CityLoyalty} cell value (computed in CidadeConverter from
 * PbmJudge's MilestoneProducaoBase.doCidadeTestFlip formula), so this renderer stays trivial.
 *
 * @author jmoura
 */
public class CityLoyaltyTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private final Color colorBgSelected = new Color(46, 106, 197);
    private final Color colorFgSelected = Color.WHITE;
    private final int amberMargin;

    public CityLoyaltyTableCellRenderer(int amberMargin) {
        super();
        this.amberMargin = amberMargin;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        this.setHorizontalAlignment(SwingConstants.RIGHT);

        boolean red = false;
        boolean amber = false;
        if (value instanceof CityLoyalty) {
            final CityLoyalty cl = (CityLoyalty) value;
            if (cl.isEligible()) {
                red = cl.getLoyalty() < cl.getThreshold();
                amber = !red && cl.getLoyalty() <= cl.getThreshold() + amberMargin;
            }
        }

        if (red) {
            c.setBackground(Color.RED);
            c.setForeground(Color.WHITE);
        } else if (amber) {
            c.setBackground(Color.ORANGE);
            c.setForeground(Color.BLACK);
        } else if (isSelected) {
            c.setBackground(colorBgSelected);
            c.setForeground(colorFgSelected);
        } else {
            // reset - DefaultTableCellRenderer reuses one component, so an uncolored cell must clear
            // any red/orange left over from a previous row on scroll/reuse.
            c.setBackground(table.getBackground());
            c.setForeground(table.getForeground());
        }
        return c;
    }
}
