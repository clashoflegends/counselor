/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import java.awt.Color;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class LifeTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final Log log = LogFactory.getLog(LifeTableCellRenderer.class);
    private final Color colorBgSelected = new Color(46, 106, 197), colorBgNotSelected = Color.WHITE;
    private final Color colorFgSelected = Color.WHITE, colorFgNotSelected = Color.BLACK;
    private int limitRed = 20;
    private int limitAmber = 60;

    public LifeTableCellRenderer(int limitRed, int limitAmber) {
        super();
        this.limitRed = limitRed;
        this.limitAmber = limitAmber;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Integer current;
        try {
            current = (Integer) value;
            if (current == null) {
                current = 0;
            }
        } catch (ClassCastException e) {
            current = 0;
            log.error("bad value in LifeTableCellRenderer = " + value);
        }
        if (current > limitAmber && isSelected) {
            c.setBackground(colorBgSelected);
            c.setForeground(colorFgSelected);
        } else if (current <= limitRed) {
            c.setBackground(Color.RED);
        } else if (current <= limitAmber) {
            c.setBackground(Color.ORANGE);
        } else {
            c.setBackground(colorBgNotSelected);
            c.setForeground(colorFgNotSelected);
        }
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        return c;
    }
}
