/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import business.facade.LocalFacade;
import control.MapaControler;
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
public class LocalTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final Log log = LogFactory.getLog(LocalTableCellRenderer.class);
    private MapaControler mapaControler;
    private final LocalFacade localFacade = new LocalFacade();
    private final Color colorBgSelected = new Color(46, 106, 197), colorBgNotSelected = Color.WHITE;
    private final Color colorFgSelected = Color.WHITE, colorFgNotSelected = Color.BLACK;
//    private Font fontSelected, fontNotSelected;

    public LocalTableCellRenderer(MapaControler aMapaControler, JTable table) {
        super();
        this.setMapaControler(aMapaControler, table);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        //Load collor on first time, so we can UN paint them...
//        if (isSelected && colorBgSelected == null) {
//            colorBgSelected = c.getBackground();
//            colorFgSelected = c.getForeground();
//            fontSelected = c.getFont();
//        } else if (!isSelected && colorBgNotSelected == null) {
//            colorBgNotSelected = c.getBackground();
//            colorFgNotSelected = c.getForeground();
//            fontNotSelected = c.getFont();
//        } else
        if (isSelected) {
            c.setBackground(colorBgSelected);
            c.setForeground(colorFgSelected);
//            c.setFont(fontSelected);
        } else {
            c.setBackground(colorBgNotSelected);
            c.setForeground(colorFgNotSelected);
//            c.setFont(fontNotSelected);
        }
        final boolean highlight = localFacade.getCoordenadas(mapaControler.getLocal()).equals(value);
        if (highlight) {
            c.setBackground(new Color(128, 128, 255));
//            c.setBackground(new Color(255, 255, 128));
            c.setForeground(Color.WHITE);
            Font font = c.getFont();
            font = new Font(font.getName(), font.getStyle() | Font.BOLD, font.getSize());
            c.setFont(font);
        } else {
            c.setFont(new Font(c.getFont().getName(), Font.PLAIN, c.getFont().getSize()));
        }
        this.setHorizontalAlignment(SwingConstants.CENTER);
        return c;
    }

    private void setMapaControler(MapaControler aMapaControler, JTable table) {
        this.mapaControler = aMapaControler;
        this.mapaControler.addTableLocal(table);
    }
}
