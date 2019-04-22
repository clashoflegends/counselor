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
import utils.OpenSlotCounter;

/**
 *
 * @author jmoura
 */
public class OpenSlotTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final Log log = LogFactory.getLog(OpenSlotTableCellRenderer.class);
    private final Color colorBgSelected = new Color(128, 128, 255), colorBgNotSelected = Color.WHITE;
    private final Color colorFgSelected = Color.WHITE, colorFgNotSelected = Color.BLACK;
    private final Color colorBgMissing = new Color(255, 230, 230), colorFgDisable = Color.LIGHT_GRAY;

    public OpenSlotTableCellRenderer(JTable table) {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        //select custom settings
        if (isSelected) {
            c.setBackground(colorBgSelected);
            c.setForeground(colorFgSelected);
        } else {
            c.setBackground(colorBgNotSelected);
            c.setForeground(colorFgNotSelected);
            try {
                OpenSlotCounter counter = (OpenSlotCounter) value;
                if (counter.isEditable()) {
                    c.setBackground(colorBgMissing);
                }
                if (counter.isDisabled()) {
                    c.setForeground(colorFgDisable);
                }
            } catch (NullPointerException ex) {
                log.error("OpenSlotTableCellRenderer issue: " + value);
            } catch (ClassCastException ex) {
                log.debug("ClassCastException issue on OpenSlotTableCellRenderer, no doubles? " + value);
            }
        }
        //apply cell style
        this.setHorizontalAlignment(SwingConstants.RIGHT);

        return c;
    }
}
