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
import model.ActorAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class ActorActionTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final Log log = LogFactory.getLog(ActorActionTableCellRenderer.class);
    private final Color colorBgSelected = new Color(128, 128, 255), colorBgNotSelected = Color.WHITE;
    private final Color colorFgSelected = Color.WHITE, colorFgNotSelected = Color.BLACK;
    private final Color colorBgMissing = new Color(255, 230, 230), colorBgMissingAlly = new Color(255, 250, 250);
    private final Color colorFgDisable = Color.LIGHT_GRAY, colorFgReadOnly = Color.LIGHT_GRAY;

    public ActorActionTableCellRenderer(JTable table) {
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
        try {
            ActorAction actorAction = (ActorAction) value;
            if (actorAction.isValid()) {
                //not in use, leave the defaults go by
            }
            if (actorAction.isBlank()) {
                background = colorBgMissing;
            }
            if (actorAction.isBlankAlly()) {
                //if it does not paint, then it is ok to leave ally the same as enemy
                if (SettingsManager.getInstance().isConfig("GuiAllyMissingOrdersPaint", "1", "1")) {
                    background = colorBgMissingAlly;
                }
                alignment = SwingConstants.CENTER;
            }
            if (actorAction.isDisabled()) {
                foreground = colorFgDisable;
                alignment = SwingConstants.CENTER;
            }
            if (actorAction.isReadonly()) {
                foreground = colorFgReadOnly;
            }
        } catch (NullPointerException ex) {
            log.error("ActorActionTableCellRenderer issue: " + value);
        } catch (ClassCastException ex) {
            log.debug("ClassCastException issue, no doubles? " + value);
        }
        //apply cell style
        this.setHorizontalAlignment(alignment);
        c.setBackground(background);
        c.setForeground(foreground);
        return c;
    }
}
