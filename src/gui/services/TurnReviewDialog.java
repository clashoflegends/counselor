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
import javax.swing.JCheckBox;
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
import persistenceCommons.SettingsManager;

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
 * <p>
 * <b>It is a SNAPSHOT, and modeless, so it must not outlive its game.</b> The findings hold live
 * {@code BaseModel} actors and the jump reaches through the {@code MainDadosGui} that was open when it
 * was built, both of which are replaced on the next EGF load. {@code QuickSearchDialog} solves this by
 * being modal; this one cannot be, since the whole point is to work through the list with the tabs in
 * front of you. So it is registered in {@code ComponentFactory.disposeSecondaryWindows} instead, and
 * {@code ComponentFactoryStaleWindowTest} pins that registration. Within one turn it is still a
 * snapshot: fix an order and the row stays until Review is opened again.
 *
 * @author jmoura
 */
public final class TurnReviewDialog extends JDialog {

    private static final Log log = LogFactory.getLog(TurnReviewDialog.class);

    /**
     * Whether the open-slot rows are shown. Persisted, because a player who turns them off wants
     * them off next turn too - and the whole point of hiding them is that they are the rows he has
     * already decided not to read.
     */
    public static final String SHOW_OPEN_SLOTS = "reviewShowOpenSlots";

    private final transient BundleManager labels;
    private final transient MainDadosGui dados;
    /** Every finding the pass produced. */
    private final transient List<Finding> findings;
    /** The subset currently on screen. The table reads THIS, never {@link #findings}. */
    private final transient List<Finding> visible = new ArrayList<>();
    private transient JLabel summary;
    private transient JTable table;
    private transient javax.swing.JTextArea detail;
    private final transient int orderCount;
    private final transient int actorCount;
    private final transient boolean failed;

    private TurnReviewDialog(Window parent, MainDadosGui dados, BundleManager labels,
            List<Finding> findings, int orderCount, int actorCount, boolean failed) {
        super(parent, ModalityType.MODELESS);
        this.labels = labels;
        this.dados = dados;
        this.findings = findings;
        this.orderCount = orderCount;
        this.actorCount = actorCount;
        this.failed = failed;
        AppIcon.applyTo(this);
        setTitle(tx("REVIEW.TITLE", "Review turn"));

        applyFilter(showOpenSlots());
        this.table = buildTable();
        final JScrollPane scroll = new JScrollPane(table);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(760, 360));

        this.summary = new JLabel(summaryText());
        summary.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 12));

        final JLabel hint = new JLabel(tx("REVIEW.HINT",
                "Double click a line to go to it. Nothing here blocks saving or sending."));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        hint.setEnabled(false);

        final JCheckBox showSlots = new JCheckBox(tx("REVIEW.FILTER.OPENSLOTS",
                "Show actors with nothing entered"), showOpenSlots());
        showSlots.setToolTipText(tx("REVIEW.FILTER.OPENSLOTS.HINT",
                "Early in a turn almost every actor is unplanned, and those rows bury the ones worth reading."));
        showSlots.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));
        showSlots.addActionListener(e -> {
            SettingsManager.getInstance().setConfig(SHOW_OPEN_SLOTS,
                    showSlots.isSelected() ? "1" : "0");
            SettingsManager.getInstance().doConfigSave(SHOW_OPEN_SLOTS);
            applyFilter(showSlots.isSelected());
            ((AbstractTableModel) table.getModel()).fireTableDataChanged();
            updateDetail();
            summary.setText(summaryText());
        });

        final JPanel north = new JPanel(new BorderLayout());
        north.add(summary, BorderLayout.NORTH);
        final JPanel below = new JPanel(new BorderLayout());
        below.add(showSlots, BorderLayout.NORTH);
        below.add(hint, BorderLayout.SOUTH);
        north.add(below, BorderLayout.SOUTH);

        final JButton close = new JButton(tx("REVIEW.CLOSE", "Close"));
        close.addActionListener(e -> dispose());
        final JPanel buttons = new JPanel();
        buttons.add(close);
        detail = new javax.swing.JTextArea(4, 60);
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(table.getFont());
        detail.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        detail.getAccessibleContext().setAccessibleName(tx("REVIEW.COL.MESSAGE", "What to look at"));
        table.getSelectionModel().addListSelectionListener(e -> updateDetail());
        final JPanel south = new JPanel(new BorderLayout());
        south.add(new JScrollPane(detail), BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);
        //DISPOSE, not the JDialog default of HIDE: closing with the window X would otherwise leave a
        //hidden dialog holding every actor of this turn, and one per Review the player opened.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        if (dados == null) {
            //Same guard QuickSearchDialog.show carries. Without it, clicking Review before a turn is
            //open throws inside the gather below and renders as a clean bill of health.
            Toast.show(w, label(labels, "REVIEW.NOTURN", "Open a turn file first"));
            return;
        }
        List<Finding> found;
        boolean failed = false;
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
            final java.util.concurrent.atomic.AtomicInteger skipped
                    = new java.util.concurrent.atomic.AtomicInteger();
            found = TurnReview.compute(actorList, wfc.getJogadorAtivo(), wfc.getLocais(),
                    wfc.listPersonagens().values(), convergingNames(),
                    actor -> cenarioFacade.hasOrdens(wfc.getPartida(), actor),
                    wfc.getNationPackagesLimit(), wfc.hasCitySizeCap(), skipped);
            found.addAll(control.services.RecruitmentReview.compute(actorList,
                    wfc.getJogadorAtivo(), wfc.getPartida()));
            //A partial review is not a clean one. compute() guards each actor so one bad one cannot
            //take the list down, and without this that guard let twenty actors with one unreadable
            //among them render as "Nothing to flag".
            failed = skipped.get() > 0;
        } catch (RuntimeException ex) {
            // A review is a convenience. It must never be the reason a player cannot finish a turn -
            // but it must not claim the turn is clean either. The empty list and the failure are
            // rendered differently for exactly that reason: this dialog's only job is to say whether
            // the turn is ready, so "I could not look" and "I looked and found nothing" are opposite
            // answers and the player has to be able to tell them apart.
            log.warn("Turn review failed", ex);
            found = new ArrayList<>();
            failed = true;
        }
        //WARN rows first: in a twenty-actor turn the open-slot rows outnumber the ones the feature
        //exists to surface, and the player reads from the top. The column sorter still works.
        found.sort((a, b) -> a.getLevel().compareTo(b.getLevel()));
        new TurnReviewDialog(w, dados, labels, found, orders, actors, failed).setVisible(true);
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

    private static boolean showOpenSlots() {
        //default OFF: at the start of a turn nearly every actor is unplanned, so these rows are the
        //bulk of the list and the ones the player is least likely to need told about
        return "1".equals(SettingsManager.getInstance().getConfig(SHOW_OPEN_SLOTS, "0"));
    }

    /**
     * An "I have not planned this one yet" row, as opposed to something that will go wrong.
     *
     * Both kinds count: {@code OPEN_PACKAGE} is the same statement about a nation with budget left,
     * and hiding one while keeping the other would read as a bug.
     */
    public static boolean isOpenSlot(TurnReview.Kind kind) {
        return kind == TurnReview.Kind.OPEN_SLOT || kind == TurnReview.Kind.OPEN_PACKAGE;
    }

    /**
     * Which of the four summary lines applies.
     *
     * Split out and public so the one rule that matters can be tested without a display: <b>never
     * say "nothing to flag" while something is hidden</b>. This dialog's whole job is to answer
     * whether the turn is ready, and a filter that empties the list must say the list was filtered -
     * otherwise it answers "I looked and found nothing" when the truth is "you asked me not to show
     * you the only thing I found".
     */
    public static String summaryKey(int visible, int hidden, boolean failed) {
        if (failed) {
            return "REVIEW.FAILED";
        }
        if (visible > 0) {
            return "REVIEW.COUNT";
        }
        return hidden > 0 ? "REVIEW.ONLYOPEN" : "REVIEW.EMPTY";
    }

    private void applyFilter(boolean withOpenSlots) {
        visible.clear();
        for (Finding f : findings) {
            if (withOpenSlots || !isOpenSlot(f.getKind())) {
                visible.add(f);
            }
        }
    }

    private int hiddenCount() {
        return findings.size() - visible.size();
    }

    /**
     * The line above the table.
     *
     * <b>"Nothing to flag" is never printed while rows are hidden.</b> This dialog's only job is to
     * say whether the turn is ready, so a filter that empties the list must say the list was
     * filtered - otherwise the feature would be answering "I looked and found nothing" when the true
     * answer is "you asked me not to show you the only thing I found".
     */
    private String summaryText() {
        final int hidden = hiddenCount();
        switch (summaryKey(visible.size(), hidden, failed)) {
            case "REVIEW.FAILED":
                return tx("REVIEW.FAILED", "The review could not be completed, so this list is not "
                        + "a clean bill of health.");
            case "REVIEW.ONLYOPEN":
                return String.format(tx("REVIEW.ONLYOPEN",
                        "Nothing to flag except %s actors with nothing entered yet."), hidden);
            case "REVIEW.EMPTY":
                return String.format(
                        tx("REVIEW.EMPTY", "Nothing to flag. %s orders across %s actors."),
                        orderCount, actorCount);
            default:
                final String ret = String.format(
                        tx("REVIEW.COUNT", "%s to look at, across %s orders and %s actors."),
                        visible.size(), orderCount, actorCount);
                return hidden == 0 ? ret : ret + " " + String.format(
                        tx("REVIEW.HIDDEN", "(%s unplanned actors hidden.)"), hidden);
        }
    }

    // ---- table ----------------------------------------------------------------------------------

    private void updateDetail() {
        int row = table.getSelectedRow();
        int model = row < 0 || row >= table.getRowCount() ? -1 : table.convertRowIndexToModel(row);
        detail.setText(model < 0 || model >= visible.size() ? "" : message(visible.get(model)));
        detail.setCaretPosition(0);
    }

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
        ret.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "review-jump");
        ret.getActionMap().put("review-jump", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { jumpToSelected(ret); }
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
        final Finding f = visible.get(table.convertRowIndexToModel(view));
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
            return visible.size();
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
            final Finding f = visible.get(rowIndex);
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
            case RECRUIT_SCOPE:
                return tx("REVIEW.RECRUIT_SCOPE", "Recruitment demand uses current cities/stocks and SNA effects. Other orders, movement, production and success rolls are not simulated. Gold checks troop purchases only, before action/new-army fees.");
            case RECRUIT_CONDITIONAL:
                return tx("REVIEW.RECRUIT_CONDITIONAL", "Muster demand is conditional on success and city eligibility; it competes with ordinary recruitment.");
            case RECRUIT_INCOMPLETE:
                return tx("REVIEW.RECRUIT_INCOMPLETE", "Recruitment could not be fully checked for this order/city. Check its inputs and dependencies.");
            case RECRUIT_ELIGIBILITY:
                return String.format(tx("REVIEW.RECRUIT_ELIGIBILITY", "%s cannot currently recruit %s (race or required facility)."), args);
            case RECRUIT_LIMIT:
                return String.format(tx("REVIEW.RECRUIT_LIMIT", "Requested %s troops; current capacity/per-order limit allows at most %s in %s, before materials/gold."), args);
            case RECRUIT_CAPACITY:
                return String.format(tx("REVIEW.RECRUIT_CAPACITY", "%s: combined recruitment demand is %s slots; current capacity is %s (including conditional orders)."), args);
            case RECRUIT_MATERIAL:
                return String.format(tx("REVIEW.RECRUIT_MATERIAL", "%s: recruitment requests need %s: %s; current stock %s (including conditional orders)."), args);
            case RECRUIT_EQUIPMENT:
                return String.format(tx("REVIEW.RECRUIT_EQUIPMENT", "%s: %s demand for weapons %s, armour %s; %s left after recruitment requests. Equipment may be reduced; troop counts may still fit."), args);
            case RECRUIT_PROVISION:
                return String.format(tx("REVIEW.RECRUIT_PROVISION", "%s: requested army food %s; %s left after recruitment requests. Provisioning may be reduced."), args);
            case RECRUIT_GOLD:
                return String.format(tx("REVIEW.RECRUIT_GOLD", "Troop purchase requests alone need %s gold; current treasury %s. Action/new-army fees and other income/spending are not included."), args);
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
            final Finding f = visible.get(table.convertRowIndexToModel(row));
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

    private String tx(String key, String fallback) {
        return label(labels, key, fallback);
    }

    /** Bundle lookup that falls back to English when the key has not reached the bundled jar yet. */
    private static String label(BundleManager labels, String key, String fallback) {
        final String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
