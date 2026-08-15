package gui.services;

import gui.TabBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The searchable index behind {@link QuickSearchDialog}, and the matching/ranking over it. Split out
 * from the dialog so it can be exercised headlessly - a ranking regression is otherwise invisible
 * until a player complains that the thing they typed is buried.
 *
 * <p>Entries come from the data tabs' table MODELS, not from what is currently visible, so a row
 * hidden by a tab's search box is still findable (the dialog clears that box when it jumps). A row
 * the tab's FILTER combo excludes is not in the model at all and so is not indexed.
 *
 * @author GM Team
 */
public final class QuickSearchIndex {

    private static final Log log = LogFactory.getLog(QuickSearchIndex.class);

    private QuickSearchIndex() {
    }

    /** One listed row: where it lives, how it reads, and the text a query is matched against. */
    public static final class Entry {

        private final int tabIndex;
        private final String tabTitle;
        private final int modelRow;
        private final String name;
        private final String coord;
        private final String haystack;

        Entry(int tabIndex, String tabTitle, int modelRow, String name, String coord, String haystack) {
            this.tabIndex = tabIndex;
            this.tabTitle = tabTitle;
            this.modelRow = modelRow;
            this.name = name;
            this.coord = coord;
            this.haystack = haystack;
        }

        public int getTabIndex() {
            return tabIndex;
        }

        public String getTabTitle() {
            return tabTitle;
        }

        public int getModelRow() {
            return modelRow;
        }

        public String getName() {
            return name;
        }

        /** The row's hex coordinate, or empty when the row has no {@code Local} column. */
        public String getCoord() {
            return coord;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("<html><b>").append(esc(name)).append("</b>");
            if (!coord.isEmpty()) {
                sb.append(" &nbsp;<font color=\"gray\">").append(esc(coord)).append("</font>");
            }
            sb.append(" &nbsp;&ndash;&nbsp;<font color=\"gray\">").append(esc(tabTitle)).append("</font></html>");
            return sb.toString();
        }

        private static String esc(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    /**
     * Walk every data tab's main list and record one entry per model row. Every cell contributes its
     * displayed text, so the haystack covers names, nations, terrain, hex coordinates ({@code Local}
     * renders as its coordinate) and order names ({@code ActorAction} renders as the order's name) -
     * the "name, hex or order" quick search promises. Tabs with no list, and the blank placeholder
     * row an empty tab carries, contribute nothing.
     */
    public static List<Entry> build(JTabbedPane tabs) {
        final List<Entry> out = new ArrayList<>();
        if (tabs == null) {
            return out;
        }
        for (int t = 0; t < tabs.getTabCount(); t++) {
            if (!(tabs.getComponentAt(t) instanceof TabBase)) {
                continue;
            }
            final TabBase tab = (TabBase) tabs.getComponentAt(t);
            final JTable table;
            try {
                table = tab.getMainLista();
            } catch (RuntimeException ex) {
                log.debug("Quick search skipped tab " + tabs.getTitleAt(t) + ": " + ex);
                continue;
            }
            //TabBase's own getMainLista logs FATAL and returns null; a tab that never overrode it must
            //not be walked, or every Ctrl+P would spray FATALs into the crash pipeline.
            if (table == null || table.getModel() == null) {
                continue;
            }
            indexTable(out, t, tabs.getTitleAt(t), table.getModel());
        }
        return out;
    }

    /** Index one tab's table model into {@code out}. Split from {@link #build} so it is testable. */
    public static void indexTable(List<Entry> out, int tabIndex, String tabTitle, TableModel model) {
        final int cols = model.getColumnCount();
        for (int r = 0; r < model.getRowCount(); r++) {
            String name = "";
            String coord = "";
            final StringBuilder hay = new StringBuilder(64);
            for (int c = 0; c < cols; c++) {
                final Object v;
                try {
                    v = model.getValueAt(r, c);
                } catch (RuntimeException ex) {
                    continue; //short placeholder row - nothing to read in this column
                }
                if (v == null) {
                    continue;
                }
                //String.valueOf returns the toString() result verbatim, so a cell whose toString() is
                //null (ActorAction and Local both delegate to a model getter) yields null, not "null".
                final String cell = String.valueOf(v);
                if (cell == null) {
                    continue;
                }
                final String s = cell.trim();
                if (s.isEmpty()) {
                    continue;
                }
                if (name.isEmpty()) {
                    name = s; //first non-blank cell - the name column on every data tab
                }
                if (coord.isEmpty() && v instanceof Local) {
                    coord = s; //Local.toString() is the hex coordinate
                }
                hay.append(s).append('\n');
            }
            if (name.isEmpty()) {
                continue; //blank placeholder row of an empty tab
            }
            out.add(new Entry(tabIndex, tabTitle, r, name, coord, hay.toString().toLowerCase()));
        }
    }

    /**
     * Rank the entries matching {@code query}, capped at {@code max}. Every whitespace-separated token
     * must appear somewhere in the row, so "daemon 11" narrows by name and hex together. An exact hex
     * match comes first, then rows whose NAME carries the first token, then everything else - so
     * typing a number does not bury the name hits under every row that happens to hold that value in
     * some column. A blank query matches nothing (the dialog shows a prompt instead).
     */
    public static List<Entry> match(List<Entry> index, String query, int max) {
        final List<Entry> exact = new ArrayList<>();
        final List<Entry> byName = new ArrayList<>();
        final List<Entry> other = new ArrayList<>();
        final String raw = (query == null) ? "" : query.trim().toLowerCase();
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        final String[] tokens = raw.split("\\s+");
        for (Entry e : index) {
            boolean all = true;
            for (String tk : tokens) {
                if (!e.haystack.contains(tk)) {
                    all = false;
                    break;
                }
            }
            if (!all) {
                continue;
            }
            if (e.coord.equalsIgnoreCase(raw)) {
                exact.add(e);
            } else if (e.name.toLowerCase().contains(tokens[0])) {
                byName.add(e);
            } else {
                other.add(e);
            }
        }
        final List<Entry> ranked = new ArrayList<>();
        for (List<Entry> bucket : Arrays.asList(exact, byName, other)) {
            for (Entry e : bucket) {
                if (ranked.size() >= max) {
                    return ranked;
                }
                ranked.add(e);
            }
        }
        return ranked;
    }

    /** Total matches for {@code query}, ignoring the display cap - drives the "and N more" line. */
    public static int countMatches(List<Entry> index, String query) {
        return match(index, query, Integer.MAX_VALUE).size();
    }
}
