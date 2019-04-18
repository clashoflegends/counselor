/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import business.facade.LocalFacade;
import control.MapaControler;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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

        if (isSelected) {
            c.setBackground(colorBgSelected);
            c.setForeground(colorFgSelected);
        } else {
            c.setBackground(colorBgNotSelected);
            c.setForeground(colorFgNotSelected);
        }
        final boolean highlight = mapaControler.getLocal() == value || localFacade.getCoordenadas(mapaControler.getLocal()).equals(value);
        if (highlight) {
            c.setBackground(new Color(128, 128, 255));
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
