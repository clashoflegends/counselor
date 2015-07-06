/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import java.awt.*;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.table.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class LimitTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final Log log = LogFactory.getLog(LimitTableCellRenderer.class);
    private final Color colorBgSelected = new Color(46, 106, 197), colorBgNotSelected = Color.WHITE;
    private final Color colorFgSelected = Color.WHITE, colorFgNotSelected = Color.BLACK;
    private int limit = 0;

    public LimitTableCellRenderer(int limit) {
        super();
        this.limit = limit;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Integer current = (Integer) value;
        if (current <= limit && isSelected) {
            c.setBackground(colorBgSelected);
            c.setForeground(colorFgSelected);
        } else if (current > limit) {
            c.setBackground(Color.RED);
        } else {
            c.setBackground(colorBgNotSelected);
            c.setForeground(colorFgNotSelected);
        }
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        return c;
    }
}
