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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
import persistenceCommons.SysApoio;
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
        //Right-click export menu (copy-with-headers / save CSV). Shared by every data tab.
        installExportMenu(table);
    }

    /**
     * Attaches a right-click context menu to the table (and its header) offering
     * "Copy all (with headers)" and "Export to CSV...". Honors the current view
     * order/filter, so what the player sees is what gets exported.
     */
    private void installExportMenu(final JTable table) {
        final JPopupMenu menu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem(labels.getString("EXPORT.COPY"));
        copyItem.addActionListener(e -> copyTableToClipboard(table));
        menu.add(copyItem);

        JMenuItem csvItem = new JMenuItem(labels.getString("EXPORT.CSV"));
        csvItem.addActionListener(e -> exportTableToCsv(table));
        menu.add(csvItem);

        table.setComponentPopupMenu(menu);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setComponentPopupMenu(menu);
        }
    }

    private void copyTableToClipboard(JTable table) {
        String tsv = buildTableText(table, "\t", false);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(tsv), null);
        setStatus(labels.getString("EXPORT.COPIED").replace("{0}", String.valueOf(table.getRowCount())), table);
    }

    private void exportTableToCsv(JTable table) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(labels.getString("EXPORT.TITLE"));
        fc.setSelectedFile(new File(defaultCsvName(table)));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = fc.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".csv")) {
            target = new File(target.getParentFile(), target.getName() + ".csv");
        }
        // UTF-8 BOM so Excel opens accented names correctly (ASCII-safe source).
        try (Writer w = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
            w.write((char) 0xFEFF);
            w.write(buildTableText(table, csvSeparator(), true));
            setStatus(labels.getString("EXPORT.SAVED").replace("{0}", target.getName()), table);
        } catch (IOException ex) {
            log.error("CSV export failed", ex);
            SysApoio.showDialogError(labels.getString("EXPORT.ERROR") + " " + ex.getMessage(), this);
        }
    }

    /**
     * Renders the visible table (current sort/filter) to delimited text with a header row.
     * When {@code csvQuote} is true, fields are RFC-4180 quoted; otherwise tabs/newlines
     * inside a cell are flattened to spaces so the grid pastes cleanly into a spreadsheet.
     */
    private String buildTableText(JTable table, String sep, boolean csvQuote) {
        int cols = table.getColumnCount();
        int rows = table.getRowCount();
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < cols; c++) {
            if (c > 0) {
                sb.append(sep);
            }
            sb.append(field(table.getColumnName(c), sep, csvQuote));
        }
        sb.append('\n');
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c > 0) {
                    sb.append(sep);
                }
                Object v = table.getValueAt(r, c);
                sb.append(field(v == null ? "" : String.valueOf(v), sep, csvQuote));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String field(String value, String sep, boolean csvQuote) {
        if (value == null) {
            value = "";
        }
        if (csvQuote) {
            if (value.contains("\"") || value.contains(sep) || value.contains("\n") || value.contains("\r")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
        // Clipboard TSV: keep one cell per column, one row per line.
        return value.replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }

    /**
     * Picks the CSV field separator the way Excel does: a semicolon in locales
     * whose decimal mark is a comma (pt/es/it/ca and most of Europe), a comma
     * otherwise (en). The app's chosen language sets the default Locale via
     * SettingsManager.setLanguage(), so this tracks the player's selection and
     * lets the file open one-click in their Excel.
     */
    private String csvSeparator() {
        char decimal = new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator();
        return (decimal == ',') ? ";" : ",";
    }

    /** Default CSV filename: {@code <openGameFileBase>-<tabTitle>.csv}, both sanitized. */
    private String defaultCsvName(JTable table) {
        String base = "counselor";
        MainResultWindowGui main = (MainResultWindowGui) SwingUtilities.getAncestorOfClass(MainResultWindowGui.class, table);
        if (main != null && main.getOpenFileName() != null && !main.getOpenFileName().isEmpty()) {
            base = main.getOpenFileName().replaceAll("(?i)\\.(rr|rc)\\.egf$", "").replaceAll("(?i)\\.egf$", "");
        }
        String tab = (getTitle() == null || getTitle().isEmpty()) ? "table" : getTitle();
        return sanitizeFileName(base) + "-" + sanitizeFileName(tab) + ".csv";
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
    }

    private void setStatus(String msg, JTable table) {
        MainResultWindowGui main = (MainResultWindowGui) SwingUtilities.getAncestorOfClass(MainResultWindowGui.class, table);
        if (main != null) {
            main.setStatusMsg(msg);
        } else {
            log.info(msg);
        }
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
