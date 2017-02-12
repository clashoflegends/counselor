/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import business.facade.OrdemFacade;
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
public class ActorOrderTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final Log log = LogFactory.getLog(ActorOrderTableCellRenderer.class);
    private final Color colorBgSelected = new Color(128, 128, 255), colorBgNotSelected = Color.WHITE;
    private final Color colorFgSelected = Color.WHITE, colorFgNotSelected = Color.BLACK;
    private final Color colorBgMissing = new Color(255, 251, 251), colorFgDisable = Color.LIGHT_GRAY;

    public ActorOrderTableCellRenderer(JTable table) {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        //set defaults
        int alignment = SwingConstants.LEFT;
        Color foreground = colorFgNotSelected;
        Color background = colorBgNotSelected;

        //select custom settings
        if (isSelected) {
            background = colorBgSelected;
            foreground = colorFgSelected;
        }
        if (value.equals(OrdemFacade.ACTIONMISSING)) {
            background = colorBgMissing;
        }
        if (value.equals(OrdemFacade.ACTIONDISABLED)) {
            foreground = colorFgDisable;
            alignment = SwingConstants.CENTER;
        }
        //apply cell style
        this.setHorizontalAlignment(alignment);
        c.setBackground(background);
        c.setForeground(foreground);
        return c;
    }
}
