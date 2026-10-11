/*
 * TurnReviewDialog.java
 */
package gui.services;

import baseLib.BaseModel;
import business.facade.CenarioFacade;
import business.facade.LocalFacade;
import control.facade.WorldFacadeCounselor;
import control.services.TurnReview;
import control.services.TurnReview.Finding;
import gui.MainDadosGui;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import model.Local;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;

/**
 * "Review turn": one list of everything worth a second look before the orders go out.
 *
 * The client already warned about most of this, but each warning fired once, somewhere else, and then
 * was gone - the missing-slot toast at save time, the {@code ;SCC;} question at the moment one order
 * was saved, the convergence markers on the map. With twenty actors a player had nowhere to stand back
 * and look at the turn as a whole. {@link TurnReview} does the thinking; this renders it and gets out
 * of the way.
 *
 * <b>Read-only and modeless.</b> Nothing here blocks or changes a save, and this is never reached from
 * the save path, so a bug in a check can cost the player a confusing row but never an hour of orders.
 * Double-clicking a row reuses {@link QuickSearchDialog#jump} so a jump from here behaves exactly like
 * a jump from quick search.
 *
 * @author jmoura
 */
public final class TurnReviewDialog extends JDialog {

    private static final Log log = LogFactory.getLog(TurnReviewDialog.class);

    private final transient BundleManager labels;
    private final transient MainDadosGui dados;
    private final transient List<Finding> findings;

    private TurnReviewDialog(Window parent, MainDadosGui dados, BundleManager labels,
            List<Finding> findings, int orderCount, int actorCount) {
        super(parent, ModalityType.MODELESS);
        this.labels = labels;
        this.dados = dados;
        this.findings = findings;
        AppIcon.applyTo(this);
        setTitle(tx("REVIEW.TITLE", "Review turn"));

        final JTable table = buildTable();
        final JScrollPane scroll = new JScrollPane(table);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(760, 360));

        final JLabel summary = new JLabel(findings.isEmpty()
                ? String.format(tx("REVIEW.EMPTY", "Nothing to flag. %s orders across %s actors."),
                        orderCount, actorCount)
                : String.format(tx("REVIEW.COUNT", "%s to look at, across %s orders and %s actors."),
                        findings.size(), orderCount, actorCount));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 12));

        final JLabel hint = new JLabel(tx("REVIEW.HINT",
                "Double click a line to go to it. Nothing here blocks saving or sending."));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        hint.setEnabled(false);

        final JPanel north = new JPanel(new BorderLayout());
        north.add(summary, BorderLayout.NORTH);
        north.add(hint, BorderLayout.SOUTH);

        final JButton close = new JButton(tx("FECHAR", "Close"));
        close.addActionListener(e -> dispose());
        final JPanel buttons = new JPanel();
        buttons.add(close);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);
        installKeys();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Gathers the turn as it stands and shows the result.
     *
     * Convergence is READ from {@code MoveConvergence.getCurrent()} rather than recomputed, so this
     * list and the markers on the map can never disagree - and a player who silenced that overlay
     * (warnConvergingMoves=0) gets no rows for it here either, which is what silencing it meant.
     */
    public static void show(Component parent, MainDadosGui dados, BundleManager labels) {
        final Window w = (parent instanceof Window) ? (Window) parent
                : SwingUtilities.getWindowAncestor(parent);
        List<Finding> found;
        int orders = 0;
        int actors = 0;
        try {
            final WorldFacadeCounselor wfc = WorldFacadeCounselor.getInstance();
            final CenarioFacade cenarioFacade = new CenarioFacade();
            final List<BaseModel> actorList = wfc.getActors();
            final business.facade.OrdemFacade ordemFacade = new business.facade.OrdemFacade();
            for (BaseModel a : actorList) {
                //Same gate compute() uses. An autoloaded ally's turn file brings THEIR actors and
                //THEIR orders into this world read-only, and counting those would inflate the summary
                //with work the player cannot do anything about.
                if (a.getAcaoSize() > 0 && ordemFacade.isAtivo(wfc.getJogadorAtivo(), a)) {
                    actors++;
                    orders += a.getAcoes().size();
                }
            }
            found = TurnReview.compute(actorList, wfc.getJogadorAtivo(), wfc.getLocais(),
                    wfc.listPersonagens().values(), convergingNames(),
                    actor -> cenarioFacade.hasOrdens(wfc.getPartida(), actor),
                    wfc.getNationPackagesLimit());
        } catch (RuntimeException ex) {
            // A review is a convenience. It must never be the reason a player cannot finish a turn.
            log.warn("Turn review failed", ex);
            found = new ArrayList<>();
        }
        new TurnReviewDialog(w, dados, labels, found, orders, actors).setVisible(true);
    }

    /** The converging-moves overlay's current picture, flattened to hex -> the names walking in. */
    private static Map<Local, List<String>> convergingNames() {
        final Map<Local, List<String>> ret = new LinkedHashMap<>();
        for (Map.Entry<Local, List<MoveConvergence.Mover>> e
                : MoveConvergence.getCurrent().getByHex().entrySet()) {
            final List<String> names = new ArrayList<>(e.getValue().size());
            for (MoveConvergence.Mover m : e.getValue()) {
                names.add(m.getActorName());
            }
            ret.put(e.getKey(), names);
        }
        return ret;
    }

    // ---- table ----------------------------------------------------------------------------------

    private JTable buildTable() {
        final JTable ret = new JTable(new Model());
        ret.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ret.setAutoCreateRowSorter(true);
        ret.setFillsViewportHeight(true);
        ret.setDefaultRenderer(Object.class, new LevelRenderer());
        ret.getColumnModel().getColumn(0).setPreferredWidth(18);
        ret.getColumnModel().getColumn(1).setPreferredWidth(140);
        ret.getColumnModel().getColumn(2).setPreferredWidth(40);
        ret.getColumnModel().getColumn(3).setPreferredWidth(540);
        ret.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    jumpToSelected(ret);
                }
            }
        });
        return ret;
    }

    private void installKeys() {
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
    }

    /**
     * Jump to the actor the selected row is about.
     *
     * Matched on name AND hex, in that order of preference, because the quick-search index is built
     * from what the tables DISPLAY and a game can hold two characters with the same name once one has
     * died and released its name (KI-051). The hex settles it in practice; when it cannot, landing on
     * the wrong row of the right tab is a navigation annoyance, never a data change.
     */
    private void jumpToSelected(JTable table) {
        final int view = table.getSelectedRow();
        if (view < 0) {
            return;
        }
        final Finding f = findings.get(table.convertRowIndexToModel(view));
        if (f.getActor() == null) {
            return; // a converging-hex row belongs to nobody in particular
        }
        try {
            final String name = f.getActor().getNome();
            final String coord = coordOf(f.getActor());
            QuickSearchIndex.Entry best = null;
            for (QuickSearchIndex.Entry e : QuickSearchIndex.build(dados.getTabbedPane())) {
                if (!e.getName().equalsIgnoreCase(name)) {
                    continue;
                }
                if (coord != null && coord.equals(e.getCoord())) {
                    best = e;
                    break;
                }
                if (best == null) {
                    best = e;
                }
            }
            if (best != null) {
                final QuickSearchIndex.Entry target = best;
                SwingUtilities.invokeLater(() -> QuickSearchDialog.jump(dados, target));
            }
        } catch (RuntimeException ex) {
            log.warn("Turn review jump failed", ex);
        }
    }

    private static String coordOf(BaseModel actor) {
        if (actor instanceof Personagem && ((Personagem) actor).getLocal() != null) {
            return LocalFacade.getCoordenadas(((Personagem) actor).getLocal());
        }
        if (actor instanceof model.Cidade && ((model.Cidade) actor).getLocal() != null) {
            return LocalFacade.getCoordenadas(((model.Cidade) actor).getLocal());
        }
        return null;
    }

    private final class Model extends AbstractTableModel {

        private final String[] cols = {
            "", tx("REVIEW.COL.ACTOR", "Actor"),   // the level column is an icon, it needs no header
            tx("REVIEW.COL.SLOT", "Slot"), tx("REVIEW.COL.MESSAGE", "What to look at")};

        @Override
        public int getRowCount() {
            return findings.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final Finding f = findings.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return f.getLevel() == TurnReview.Level.WARN ? "!" : "i";
                case 1:
                    return f.getActor() == null ? "" : f.getActor().getNome();
                case 2:
                    return f.getSlot() < 0 ? "" : String.valueOf(f.getSlot() + 1);
                default:
                    return message(f);
            }
        }
    }

    /** Renders a finding into the bundle's sentence for its kind. */
    private String message(Finding f) {
        final Object[] args = f.getArgs();
        switch (f.getKind()) {
            case OPEN_SLOT:
                return String.format(tx("REVIEW.OPEN_SLOT",
                        "%s has at least one open action slot."), args);
            case OPEN_PACKAGE:
                return String.format(tx("REVIEW.OPEN_PACKAGE",
                        "%s still has setup points to spend."), args);
            case SIZE_CAP:
                return String.format(tx("REVIEW.SIZE_CAP",
                        "%s cannot grow while %s (%s) stands too close."), args);
            case RENDEZVOUS:
                return String.format(tx("REVIEW.RENDEZVOUS",
                        "%s moves to %s, but %s is heading for %s, so they will not meet."), args);
            case CONVERGE:
                return String.format(tx("REVIEW.CONVERGE",
                        "%s: %s are both moving into the same empty hex."), args);
            default:
                return "";
        }
    }

    /**
     * Amber on a WARN row. Resets background AND tooltip on every call: DefaultTableCellRenderer reuses
     * one component and remembers the last unselected colour it was given, so an untinted row inherits
     * the previous row's tint on scroll or sort without the explicit reset.
     */
    private final class LevelRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            final Finding f = findings.get(table.convertRowIndexToModel(row));
            if (!isSelected) {
                if (f.getLevel() == TurnReview.Level.WARN) {
                    c.setBackground(LafTint.AMBER.bg());
                    c.setForeground(LafTint.fg());
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            setToolTipText(column == 3 ? String.valueOf(value) : null);
            return c;
        }
    }

    /** Bundle lookup that falls back to English when the key has not reached the bundled jar yet. */
    private String tx(String key, String fallback) {
        final String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
