/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class ColumnWidthsAdjuster {

    public void calcColumnWidths(JTable table) {
        JTableHeader header = table.getTableHeader();
        TableCellRenderer defaultHeaderRenderer = null;
        if (header != null) {
            defaultHeaderRenderer = header.getDefaultRenderer();
        }
        TableColumnModel columns = table.getColumnModel();
        TableModel data = table.getModel();
        int margin = columns.getColumnMargin(); // only JDK1.3
        int rowCount = data.getRowCount();
        int totalWidth = 0;
        for (int i = columns.getColumnCount() - 1; i >= 0; --i) {
            TableColumn column = columns.getColumn(i);
            int columnIndex = column.getModelIndex();
            int width = -1;
            TableCellRenderer h = column.getHeaderRenderer();
            if (h == null) {
                h = defaultHeaderRenderer;
            }
            if (h != null) {
                // Not explicitly impossible
                Component c = h.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, i);
                width = c.getPreferredSize().width;
            }
            for (int row = rowCount - 1; row >= 0; --row) {
                TableCellRenderer r = table.getCellRenderer(row, i);
                if (r instanceof ActorActionTableCellRenderer && SettingsManager.getInstance().isConfig("TableActionColumnAdjust", "0", "0")) {
                    //don't readjust
                    break;
                }
                Component c = r.getTableCellRendererComponent(table,
                        data.getValueAt(row, columnIndex),
                        false, false, row, i);
                width = Math.max(width, c.getPreferredSize().width);
            }
            if (width >= 0) {
                column.setPreferredWidth(width + margin + 2); // <1.3: without margin
            } else; // ???
            totalWidth += column.getPreferredWidth();
        }
        if (SettingsManager.getInstance().isConfig("TableRowAdjust", "1", "1")) {
            updateRowHeights(table);
        }
    }

    private void updateRowHeights(JTable table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();

            for (int column = 0; column < table.getColumnCount(); column++) {
                Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }

            table.setRowHeight(row, rowHeight);
        }
    }
}
