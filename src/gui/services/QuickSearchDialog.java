package gui.services;

import gui.MainDadosGui;
import gui.TabBase;
import gui.services.QuickSearchIndex.Entry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;

/**
 * Quick search / jump-to: one keystroke (Ctrl+P, Cmd+P on macOS) opens a search box over the main
 * window; type any part of a name, a hex coordinate, an order, or anything from a row's turn RESULT
 * text, and Enter jumps straight to that row in its tab with the map centred on its hex.
 *
 * <p>The index ({@link QuickSearchIndex}) is rebuilt every time the box opens, so it always matches
 * what the player currently has listed, needs no per-tab code, and picks up any tab added later for
 * free.
 *
 * <p>Modal on purpose: it must not outlive the {@link MainDadosGui} it indexed, which is rebuilt on
 * every EGF load. It is also registered in {@code ComponentFactory.disposeSecondaryWindows} as a
 * backstop.
 *
 * @author GM Team
 */
public final class QuickSearchDialog extends JDialog {

    private static final Log log = LogFactory.getLog(QuickSearchDialog.class);
    /**
     * Rendered result rows. A jump box you scroll sixty rows in has already failed - the status line
     * says how many more there are, and the answer is to type another word, not to scroll.
     */
    private static final int MAX_RESULTS = 60;

    private final BundleManager labels;
    private final MainDadosGui dados;
    private final List<Entry> index;
    private final DefaultListModel<Entry> shown = new DefaultListModel<>();
    private final JTextField query = new JTextField();
    private final JList<Entry> results = new JList<>(shown);
    private final JLabel status = new JLabel();

    private QuickSearchDialog(Window parent, MainDadosGui dados, BundleManager labels) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.labels = labels;
        this.dados = dados;
        this.index = QuickSearchIndex.build(dados.getTabbedPane());
        AppIcon.applyTo(this);
        setTitle(tx("SEARCH.QUICK.TITLE", "Find"));
        buildContent();
        setSize(new Dimension(520, 380));
        setLocationRelativeTo(parent);
    }

    /**
     * Open the search box over the main window. Says so and stops when no turn is loaded, because
     * there is nothing to search.
     */
    public static void show(Component parent, MainDadosGui dados, BundleManager labels) {
        final Window w = (parent instanceof Window) ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        if (dados == null) {
            Toast.show(w, label(labels, "SEARCH.QUICK.NOTURN", "Open a turn file first"));
            return;
        }
        new QuickSearchDialog(w, dados, labels).setVisible(true);
    }

    private void buildContent() {
        final String hint = tx("SEARCH.QUICK.HINT", "Name, hex coordinate, order or result - Enter to jump");
        query.setToolTipText(hint);
        //Searching on every keystroke with no debounce is deliberate: a full pass over ~1,800 indexed
        //rows costs well under a millisecond even with a turn's narrative folded into each haystack
        //(measured 0.17-0.49ms), so a coalescing delay would only add latency to a result list that
        //is already instant. The lag this box originally had was HTML cell measurement, not the search.
        query.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });

        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer(new HitRenderer());
        //Without a fixed height a JList measures EVERY row to size itself. That measurement was the
        //whole of the original typing lag (165ms per keystroke for 300 HTML rows); pinning the height
        //means it measures none of them.
        results.setFixedCellHeight(new JLabel("Wg").getPreferredSize().height + 6);
        results.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    jumpToSelected();
                }
            }
        });

        status.setFont(status.getFont().deriveFont(Font.PLAIN));
        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));

        final JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        top.add(new JLabel(hint), BorderLayout.NORTH);
        top.add(query, BorderLayout.CENTER);

        final JPanel main = new JPanel(new BorderLayout());
        main.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        main.add(new JScrollPane(results), BorderLayout.CENTER);
        main.add(status, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(main, BorderLayout.CENTER);

        installKeys();
        refresh();
    }

    /**
     * Keep the caret in the search box while Up/Down/Enter drive the result list - the box is useless
     * if the player has to Tab across to move the selection. Esc closes without jumping.
     */
    private void installKeys() {
        final javax.swing.InputMap im = query.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "qsDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "qsUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "qsGo");
        query.getActionMap().put("qsDown", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                move(1);
            }
        });
        query.getActionMap().put("qsUp", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                move(-1);
            }
        });
        query.getActionMap().put("qsGo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                jumpToSelected();
            }
        });
        //Enter also works once the player has clicked into the result list (Up/Down are native there).
        results.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "qsGo");
        results.getActionMap().put("qsGo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                jumpToSelected();
            }
        });
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "qsClose");
        getRootPane().getActionMap().put("qsClose", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        //The box is useless if the player has to click into it first.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                query.requestFocusInWindow();
            }
        });
    }

    private void move(int delta) {
        if (shown.isEmpty()) {
            return;
        }
        int i = results.getSelectedIndex() + delta;
        i = Math.max(0, Math.min(shown.size() - 1, i));
        results.setSelectedIndex(i);
        results.ensureIndexIsVisible(i);
    }

    /** Re-rank after the typing pause. */
    private void refresh() {
        shown.clear();
        final String raw = query.getText().trim();
        if (raw.isEmpty()) {
            status.setText(tx("SEARCH.QUICK.TYPE", "Start typing to search"));
            return;
        }
        final QuickSearchIndex.Matches m = QuickSearchIndex.search(index, raw, MAX_RESULTS);
        shown.addAll(m.getHits()); //one ListDataEvent, not one per row
        if (shown.isEmpty()) {
            status.setText(tx("SEARCH.QUICK.EMPTY", "Nothing found"));
            return;
        }
        results.setSelectedIndex(0);
        results.ensureIndexIsVisible(0);
        status.setText(m.getTotal() > shown.size()
                ? String.format(tx("SEARCH.QUICK.MORE", "... and %d more"), m.getTotal() - shown.size())
                : " ");
    }

    /**
     * Name on the left, hex + tab dimmed on the right. Two plain labels rather than one HTML label:
     * same look, none of the HTML parsing that made the list slow to size.
     */
    private static final class HitRenderer extends JPanel implements javax.swing.ListCellRenderer<Entry> {

        private final JLabel name = new JLabel();
        private final JLabel where = new JLabel();

        HitRenderer() {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            add(name, BorderLayout.WEST);
            add(where, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Entry> list, Entry value,
                int i, boolean selected, boolean focused) {
            name.setText(value.getName());
            //On the Locations tab the row's name IS its hex, so showing the coordinate again just
            //prints it twice.
            final String coord = value.getCoord();
            where.setText(coord.isEmpty() || coord.equals(value.getName())
                    ? value.getTabTitle() : coord + "   " + value.getTabTitle());
            final Color bg = selected ? list.getSelectionBackground() : list.getBackground();
            final Color fg = selected ? list.getSelectionForeground() : list.getForeground();
            setBackground(bg);
            setOpaque(true);
            name.setForeground(fg);
            //Dim the secondary text, but stay readable on the selection colour in either theme.
            where.setForeground(selected ? fg : blend(fg, bg, 0.45f));
            return this;
        }

        /** {@code ratio} of the way from {@code a} to {@code b}. Theme-agnostic: derived from the
         *  list's own colours, so it dims correctly on light and FlatLaf dark alike. */
        private static Color blend(Color a, Color b, float ratio) {
            final float r = Math.max(0f, Math.min(1f, ratio));
            return new Color(
                    Math.round(a.getRed() * (1 - r) + b.getRed() * r),
                    Math.round(a.getGreen() * (1 - r) + b.getGreen() * r),
                    Math.round(a.getBlue() * (1 - r) + b.getBlue() * r));
        }
    }

    private void jumpToSelected() {
        final Entry e = results.getSelectedValue();
        if (e == null) {
            return;
        }
        dispose();
        //Off the modal loop before touching the tabs: selecting a row fires the tab's own selection
        //listener, which rebuilds the whole detail panel (portrait, orders sub-tab). That is exactly
        //the work a mouse click on the row does - just let it run on a clean EDT cycle.
        SwingUtilities.invokeLater(() -> doJump(e));
    }

    private void doJump(Entry e) {
        try {
            final javax.swing.JTabbedPane tabs = dados.getTabbedPane();
            if (e.getTabIndex() >= tabs.getTabCount()
                    || !(tabs.getComponentAt(e.getTabIndex()) instanceof TabBase)) {
                return; //tab set changed under us
            }
            //Writes LastDataTab, so Counselor reopens on the tab the player last jumped to. Intended.
            tabs.setSelectedIndex(e.getTabIndex());
            final TabBase tab = (TabBase) tabs.getComponentAt(e.getTabIndex());
            final JTable table = tab.getMainLista();
            if (table == null || e.getModelRow() >= table.getModel().getRowCount()) {
                return;
            }
            int view = table.convertRowIndexToView(e.getModelRow());
            if (view < 0) {
                tab.clearSearchFilter(); //the row is real; the tab's own search box was hiding it
                view = table.convertRowIndexToView(e.getModelRow());
            }
            if (view < 0) {
                return;
            }
            table.setRowSelectionInterval(view, view);
            table.scrollRectToVisible(table.getCellRect(view, 0, true));
            centreMap(tab, table.getModel(), e.getModelRow());
            //Focus LAST and on its own cycle: clearing the tab's search box above makes its
            //DocumentListener queue an invokeLater that pulls focus back into that box, and a queued
            //grab beats a synchronous one from this event. Queueing ours behind it lands on the row.
            SwingUtilities.invokeLater(table::requestFocusInWindow);
        } catch (RuntimeException ex) {
            //A jump is a convenience; never let a stale index take the client down.
            log.warn("Quick search jump failed", ex);
        }
    }

    /**
     * Centre the map on the row's hex, exactly as a double-click on the row does. Some tabs already do
     * this from their selection listener; repeating it is harmless and covers the ones that do not.
     */
    private void centreMap(TabBase tab, TableModel model, int modelRow) {
        if (tab.getMapaControler() == null) {
            return;
        }
        for (int c = 0; c < model.getColumnCount(); c++) {
            final Object v = model.getValueAt(modelRow, c);
            if (v instanceof Local) {
                tab.getMapaControler().printTag((Local) v);
                return;
            }
        }
    }

    private String tx(String key, String fallback) {
        return label(labels, key, fallback);
    }

    /** Bundle lookup that falls back to English when the key has not reached the bundled jar yet. */
    private static String label(BundleManager labels, String key, String fallback) {
        final String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
