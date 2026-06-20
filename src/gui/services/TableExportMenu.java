package gui.services;

import gui.MainResultWindowGui;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 * Right-click "Copy all (with headers)" / "Export to CSV..." context menu for any {@link JTable}.
 * It reads through the table's view, so the current sort/filter order is what gets exported -
 * what the player sees is what they get. Extracted from {@code TabBase} so every table in the
 * app (data tabs, subtabs, World Builder, Battle Simulator) can share one implementation.
 */
public final class TableExportMenu {

    private static final Log log = LogFactory.getLog(TableExportMenu.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private TableExportMenu() {
    }

    /** Attach the menu to a table (and its header); the CSV filename uses a generic default. */
    public static void install(JTable table) {
        install(table, null);
    }

    /**
     * Attach the menu to a table (and its header).
     *
     * @param nameHint label used in the default CSV filename (e.g. the tab title); null -&gt; "table".
     */
    public static void install(final JTable table, final String nameHint) {
        final JPopupMenu menu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem(labels.getString("EXPORT.COPY"));
        copyItem.addActionListener(e -> copyTableToClipboard(table));
        menu.add(copyItem);

        JMenuItem csvItem = new JMenuItem(labels.getString("EXPORT.CSV"));
        csvItem.addActionListener(e -> exportTableToCsv(table, nameHint));
        menu.add(csvItem);

        table.setComponentPopupMenu(menu);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setComponentPopupMenu(menu);
        }
    }

    private static void copyTableToClipboard(JTable table) {
        String tsv = buildTableText(table, "\t", false);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(tsv), null);
        setStatus(labels.getString("EXPORT.COPIED").replace("{0}", String.valueOf(table.getRowCount())), table);
    }

    private static void exportTableToCsv(JTable table, String nameHint) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(labels.getString("EXPORT.TITLE"));
        fc.setSelectedFile(new File(defaultCsvName(table, nameHint)));
        if (fc.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) {
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
            SysApoio.showDialogError(labels.getString("EXPORT.ERROR") + " " + ex.getMessage(), table);
        }
    }

    /**
     * Renders the visible table (current sort/filter) to delimited text with a header row.
     * When {@code csvQuote} is true, fields are RFC-4180 quoted; otherwise tabs/newlines inside a
     * cell are flattened to spaces so the grid pastes cleanly into a spreadsheet.
     */
    private static String buildTableText(JTable table, String sep, boolean csvQuote) {
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

    private static String field(String value, String sep, boolean csvQuote) {
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
     * Picks the CSV field separator the way Excel does: a semicolon in locales whose decimal mark
     * is a comma (pt/es/it/ca and most of Europe), a comma otherwise (en). The app's chosen language
     * sets the default Locale, so this tracks the player's selection and opens one-click in Excel.
     */
    private static String csvSeparator() {
        char decimal = new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator();
        return (decimal == ',') ? ";" : ",";
    }

    /** Default CSV filename: {@code <openGameFileBase>-<nameHint>.csv}, both sanitized. */
    private static String defaultCsvName(JTable table, String nameHint) {
        String base = "counselor";
        MainResultWindowGui main = (MainResultWindowGui) SwingUtilities.getAncestorOfClass(MainResultWindowGui.class, table);
        if (main != null && main.getOpenFileName() != null && !main.getOpenFileName().isEmpty()) {
            base = main.getOpenFileName().replaceAll("(?i)\\.(rr|rc)\\.egf$", "").replaceAll("(?i)\\.egf$", "");
        }
        String tab = (nameHint == null || nameHint.isEmpty()) ? "table" : nameHint;
        return sanitizeFileName(base) + "-" + sanitizeFileName(tab) + ".csv";
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
    }

    private static void setStatus(String msg, JTable table) {
        MainResultWindowGui main = (MainResultWindowGui) SwingUtilities.getAncestorOfClass(MainResultWindowGui.class, table);
        if (main != null) {
            main.setStatusMsg(msg);
        } else {
            log.info(msg);
        }
    }
}
