/*
 * TabBase.java
 *
 * Created on April 24, 2008, 10:28 PM
 */
package gui;

import control.MapaControler;
import gui.services.ActorActionTableCellRenderer;
import gui.services.ColumnWidthsAdjuster;
import gui.services.LocalTableCellRenderer;
import gui.services.OpenSlotTableCellRenderer;
import java.io.Serializable;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import model.ActorAction;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.OpenSlotCounter;

/**
 *
 * @author gurgel
 */
public abstract class TabBase extends javax.swing.JRootPane implements Serializable {

    private static final Log log = LogFactory.getLog(TabBase.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private String dica;
    private String title;
    private String keyFilterProperty;
    private ImageIcon icone;
    private MapaControler mapaControler;
    private int filtroDefault = -9999;

    /**
     * Creates new form TabBase
     */
    public TabBase() {
        initComponents();
    }

    protected int getComboFiltroSize() {
        return 0;
    }

    public final String getKeyFilterProperty() {
        return keyFilterProperty;
    }

    public final void setKeyFilterProperty(String keyFilterProperty) {
        this.keyFilterProperty = keyFilterProperty;
        this.setFiltroDefault(SettingsManager.getInstance().getConfigAsInt(keyFilterProperty, "-9999"));
    }

    /**
     * Applies the default filter to the combo, clamping the index to the combo's actual model size so a
     * misconfigured filtro.default (or stale per-tab filter key) can never throw setSelectedIndex out of bounds.
     */
    protected final void applyFiltroDefault(javax.swing.JComboBox comboFiltro) {
        final int size = comboFiltro.getModel().getSize();
        if (size == 0) {
            return; //nothing to select
        }
        int idx = getFiltroDefault();
        if (idx < 0 || idx >= size) {
            idx = 0; //fall back to "All"
        }
        comboFiltro.setSelectedIndex(idx);
    }

    public final int getFiltroDefault() {
        if (filtroDefault == -9999 || this.getComboFiltroSize() <= filtroDefault) {
            //load default
            int vlFiltro = SettingsManager.getInstance().getConfigAsInt("filtro.default");
            //se diferente de 0,provavelmente eh um valor invalido, ou chave nao encontrada.
            if (vlFiltro != 1 || this.getComboFiltroSize() <= vlFiltro) {
                vlFiltro = 0;
            }
            this.setFiltroDefault(vlFiltro);
        }
        return filtroDefault;
    }

    private void setFiltroDefault(int filtroDefault) {
        this.filtroDefault = filtroDefault;
    }

    public String getDica() {
        return dica;
    }

    public final void setDica(String toolTip) {
        this.dica = toolTip;
    }

    public String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        this.title = title;
    }

    public ImageIcon getIcone() {
        return icone;
    }

    public final void setIcone(String iconeName) {
        try {
            this.icone = new ImageIcon(getClass().getResource(iconeName));
        } catch (NullPointerException ex) {
        }
    }

    public MapaControler getMapaControler() {
        return this.mapaControler;
    }

    protected final void setMapaControler(MapaControler mapaControl) {
        this.mapaControler = mapaControl;
    }

    public void doTagHide() {
        this.mapaControler.getTabGui().hidefocusTag();
    }

    protected void doConfigTableColumns(JTable table) {
        //set renders for Action columns
        table.setDefaultRenderer(ActorAction.class, new ActorActionTableCellRenderer(table));
        //set renders for Open Slot columns
        table.setDefaultRenderer(OpenSlotCounter.class, new OpenSlotTableCellRenderer(table));
        //set render for Local/Hex
        if (this.mapaControler != null) {
            table.setDefaultRenderer(Local.class, new LocalTableCellRenderer(this.mapaControler, table));
        }
        //Adjust all columns to fit.
        if (SettingsManager.getInstance().isTableColumnAdjust()) {
            final ColumnWidthsAdjuster cwa = new ColumnWidthsAdjuster();
            cwa.calcColumnWidths(table);
        }
        //Restore the player's saved sort + manual column widths for this table (overrides auto-fit).
        restoreTableLayout(table);
        //Double-click a row with a hex/Local to centre the map on it.
        installRowToMap(table);
        //Right-click export menu (copy-with-headers / save CSV). Shared by every data table.
        gui.services.TableExportMenu.install(table, getTitle());
    }

    private static final String LAYOUT_INSTALLED = "layoutListenersInstalled";
    private static final String ROW_TO_MAP_INSTALLED = "rowToMapInstalled";
    private static final String TABLE_SEQ = "tableConfigSeq";
    private int tableConfigSeq = 0;

    /** Stable per-table config key: tab class + a per-tab table ordinal + a hash of the column titles.
     *  The ordinal (assigned once, in construction order, cached on the table) disambiguates the
     *  multiple tables in one tab that share identical columns - e.g. TabTipoTropas' attack/defense/
     *  movement/ability subtables, all {Terrain, Value} - which a column-title hash alone collides on.
     *  A language switch changes the hash and just forgets the (cosmetic) pref, which is acceptable. */
    private String tableKey(JTable table) {
        Object seq = table.getClientProperty(TABLE_SEQ);
        if (seq == null) {
            seq = tableConfigSeq++;
            table.putClientProperty(TABLE_SEQ, seq);
        }
        StringBuilder cols = new StringBuilder();
        for (int c = 0; c < table.getColumnCount(); c++) {
            cols.append(table.getColumnName(c)).append('|');
        }
        return getClass().getSimpleName() + "#" + seq + "." + Integer.toHexString(cols.toString().hashCode());
    }

    /**
     * Restore the saved sort key + manual column widths for this table, and (once per table) attach
     * listeners that persist future changes. Sort survives filter/turn reloads; widths persist only
     * the player's manual header drags (the width listener ignores any change with no resizing column).
     */
    private void restoreTableLayout(final JTable table) {
        final String key = tableKey(table);
        final SettingsManager sm = SettingsManager.getInstance();

        // --- restore sort (the sorter is recreated on each setModel, so re-apply every time) ---
        final javax.swing.RowSorter<?> sorter = table.getRowSorter();
        if (sorter != null) {
            String savedSort = sm.getConfig("TableSort." + key, "");
            if (!savedSort.isEmpty()) {
                try {
                    String[] p = savedSort.split(":");
                    int col = Integer.parseInt(p[0]);
                    javax.swing.SortOrder order = javax.swing.SortOrder.valueOf(p[1]);
                    if (col >= 0 && col < table.getColumnCount()) {
                        sorter.setSortKeys(java.util.Collections.singletonList(new javax.swing.RowSorter.SortKey(col, order)));
                    }
                } catch (RuntimeException ignore) {
                    // stale/corrupt pref - ignore, leave default order
                }
            }
            sorter.addRowSorterListener((javax.swing.event.RowSorterEvent ev) -> {
                if (ev.getType() != javax.swing.event.RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    return;
                }
                java.util.List<? extends javax.swing.RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
                if (keys != null && !keys.isEmpty()) {
                    sm.setConfig("TableSort." + key, keys.get(0).getColumn() + ":" + keys.get(0).getSortOrder().name());
                } else {
                    sm.setConfig("TableSort." + key, ""); // user cycled the column back to unsorted - clear the pref
                }
            });
        }

        // --- restore manual column widths (override auto-fit), then track drags once per table ---
        final javax.swing.table.TableColumnModel cm = table.getColumnModel();
        String savedW = sm.getConfig("TableColW." + key, "");
        if (!savedW.isEmpty()) {
            String[] w = savedW.split(",");
            if (w.length == cm.getColumnCount()) {
                try {
                    for (int i = 0; i < w.length; i++) {
                        cm.getColumn(i).setPreferredWidth(Integer.parseInt(w[i]));
                    }
                } catch (RuntimeException ignore) {
                    // stale/corrupt pref - ignore, keep auto-fit widths
                }
            }
        }
        if (table.getClientProperty(LAYOUT_INSTALLED) == null) {
            table.putClientProperty(LAYOUT_INSTALLED, Boolean.TRUE);
            cm.addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
                @Override
                public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                    // Persist ONLY a genuine user header drag. A drag holds a resizing column for its
                    // whole duration; programmatic passes (auto-fit, our width restore, and the DEFERRED
                    // doLayout that re-fires margin events on the next EDT cycle) have none - so a timing
                    // flag can't catch them, but getResizingColumn() reliably can (antagonist-review catch).
                    javax.swing.table.JTableHeader header = table.getTableHeader();
                    if (header == null || header.getResizingColumn() == null) {
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < cm.getColumnCount(); i++) {
                        if (i > 0) {
                            sb.append(',');
                        }
                        sb.append(cm.getColumn(i).getWidth());
                    }
                    sm.setConfig("TableColW." + key, sb.toString());
                }

                @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) { }
                @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) { }
                @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) { }
                @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) { }
            });
        }
    }

    /** Double-click a table row that carries a {@link Local}: centre + focus the map on that hex.
     *  Guarded so the repeated doConfigTableColumns calls (one per filter/turn reload) don't stack
     *  duplicate listeners that would fire printTag several times per double-click. */
    private void installRowToMap(final JTable table) {
        if (table.getClientProperty(ROW_TO_MAP_INSTALLED) != null) {
            return;
        }
        table.putClientProperty(ROW_TO_MAP_INSTALLED, Boolean.TRUE);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() != 2 || mapaControler == null) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                javax.swing.table.TableModel m = table.getModel();
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object v = m.getValueAt(modelRow, c);
                    if (v instanceof Local) {
                        mapaControler.printTag((Local) v);
                        return;
                    }
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    protected DefaultComboBoxModel getDefaultComboBoxModelTodosProprio() {
        return new DefaultComboBoxModel(new String[]{labels.getString("FILTRO.TODOS"), labels.getString("FILTRO.PROPRIOS")});
    }

    private void applyGlobalFilter(String searchText) {
        // Make sure we have a row sorter
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) getMainLista().getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(getMainLista().getModel());
            getMainLista().setRowSorter(sorter);
        }

        if (searchText == null || searchText.trim().isEmpty()) {
            // No search text -> show all rows
            sorter.setRowFilter(null);
        } else {
            // Build a case-insensitive partial-match regex,
            // quoting any special regex characters from the user input:
            String regex = "(?i).*" + Pattern.quote(searchText.trim()) + ".*";

            try {
                // If you omit column indices here, it will search ALL columns
                sorter.setRowFilter(RowFilter.regexFilter(regex));
            } catch (java.util.regex.PatternSyntaxException e) {
                // In case the user types an invalid regex, fall back to no filter
                sorter.setRowFilter(null);
            }
        }
    }

    protected final void addDocumentListener(JTextField searchField) {
        //add listener to search field for filtering.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyGlobalFilter(searchField.getText());
                refocusField(searchField);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyGlobalFilter(searchField.getText());
                refocusField(searchField);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Typically not fired for plain text components, but just in case:
                applyGlobalFilter(searchField.getText());
            }
        });
    }

    private void refocusField(JTextField field) {
        SwingUtilities.invokeLater(() -> {
            field.requestFocusInWindow();
        });
    }

    public JTable getMainLista() {
        log.fatal("Can't use getMainLista() here. Needs to override int he tab.");
        return null;
    }
}
