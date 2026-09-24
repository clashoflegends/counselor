package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.LayerReport;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * The result of a run, in numbers. T-802.
 *
 * <h3>Numbers, not prose</h3>
 *
 * John, 2026-09-21: "Would be nice to be able to see the turn by turn results. And how many combat
 * rounds per layer. It could be the data points only, no narrative." So this is three things and
 * nothing else: a verdict line, a casualty summary, and ONE ROUNDS TABLE PER LAYER whose rows are
 * armies, columns are rounds, and cells are troops REMAINING. The narrative is T-803 and arrives
 * beside this, not instead of it.
 *
 * <h3>Remaining, not lost</h3>
 *
 * Losses answer "what did that cost"; remaining answers "was it enough", and the second is the
 * question the simulator is opened to ask. Reading down a column says who is still standing after
 * round 3; reading across a row says how fast an army is melting. The summary above gives the
 * differences for anyone who wants them.
 *
 * <h3>A layer that did not happen says why</h3>
 *
 * It does not vanish. "The city was never assaulted" and "the city held" are different answers to
 * the player's question, and today a third applies to two of the three layers: not simulated yet.
 * Leaving them out would let a land-only forecast read as a whole battle.
 *
 * <h3>Non-modal, like the diplomacy panel</h3>
 *
 * The player compares this against the platoon table and the roster while it is open, and re-runs
 * after changing a tactic. A modal dialog would make him close it to do either.
 */
public class BattleSimResultDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final BundleManager labels =
            SettingsManager.getInstance().getBundleManager();

    private final transient BattleSimControler controler;

    public BattleSimResultDialog(Frame owner, BattleSimControler controler) {
        super(owner, labels.getString("BATTLESIM.RESULTS.OPEN"), false);
        this.controler = controler;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new JScrollPane(buildBody()), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setSize(new Dimension(Math.min(760, Math.max(520, getWidth())),
                Math.min(640, Math.max(360, getHeight()))));
        setLocationRelativeTo(owner);
    }

    /** Rebuilt whole on every Run, because every number in it changes. */
    public void refresh() {
        getContentPane().removeAll();
        add(new JScrollPane(buildBody()), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private JPanel buildBody() {
        final JPanel ret = new JPanel();
        ret.setLayout(new BoxLayout(ret, BoxLayout.Y_AXIS));
        ret.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        final CombatResult result = controler.getLastResult();
        if (result == null) {
            ret.add(left(labels.getString("BATTLESIM.RESULTS.NONE")));
            return ret;
        }
        final CombatScenario scenario = controler.getScenario();
        setTitle(String.format(labels.getString("BATTLESIM.RESULTS.TITLE"),
                scenario.getLocal() == null ? "" : scenario.getLocal().getCoordenadas(),
                String.format(labels.getString("BATTLESIM.RESULTS.ROUNDS"), result.getRounds())));

        for (String line : BattleSimConverter.getVerdictLines(scenario, result)) {
            ret.add(bold(line));
        }
        ret.add(gap());
        ret.add(heading(labels.getString("BATTLESIM.RESULTS.CASUALTIES")));
        ret.add(table(new CasualtyModel(scenario, result)));

        // one table per layer, in the order they are fought, so the chain reads down the page
        for (CombatLayer layer : CombatLayer.values()) {
            ret.add(gap());
            final LayerReport report = controler.getLayerReport(layer);
            ret.add(heading(String.format(labels.getString("BATTLESIM.RESULTS.LAYER"),
                    layer.ordinal() + 1, labels.getString("BATTLESIM.LAYER." + layer.name())
                            .toUpperCase())));
            if (!report.isFought()) {
                ret.add(left(labels.getString(report.getNotFoughtReason())));
                continue;
            }
            ret.add(left(String.format(labels.getString("BATTLESIM.RESULTS.ROUNDS"),
                    report.getRounds()) + "  -  "
                    + labels.getString("BATTLESIM.RESULTS.REMAINING")));
            ret.add(table(new RoundsModel(report)));
        }

        if (hasNotesToShow(result)) {
            ret.add(gap());
            ret.add(heading(labels.getString("BATTLESIM.RESULTS.NOTES")));
            for (String note : result.getNotes()) {
                // The land-only caveat is already the reason printed against LAYER 1 and LAYER 3,
                // so repeating it here would state the same fact three times on one screen and
                // make the notes look longer than they are.
                if (!"BATTLESIM.RESULT.LANDONLY".equals(note)) {
                    ret.add(left("- " + labels.getString(note)));
                }
            }
        }
        return ret;
    }

    /** Whether anything is left once the caveat each layer already states is taken out. */
    private static boolean hasNotesToShow(CombatResult result) {
        for (String note : result.getNotes()) {
            if (!"BATTLESIM.RESULT.LANDONLY".equals(note)) {
                return true;
            }
        }
        return false;
    }

    private JPanel buildButtons() {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 6));
        final JButton close = new JButton(labels.getString("BATTLESIM.DIPLOMACY.CLOSE"));
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        ret.add(close);
        return ret;
    }

    // ------------------------------------------------------------------ pieces

    private JComponentRow gap() {
        return new JComponentRow(Box.createVerticalStrut(10));
    }

    private JComponentRow heading(String text) {
        final JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return new JComponentRow(label);
    }

    private JComponentRow bold(String text) {
        final JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return new JComponentRow(label);
    }

    private JComponentRow left(String text) {
        return new JComponentRow(new JLabel(text));
    }

    /**
     * A read-only table that sizes to its content.
     *
     * Inside a BoxLayout a JTable with no explicit height collapses to nothing, and its header
     * disappears entirely without a JScrollPane - which is the single most common way a Swing table
     * ends up invisible. Both are fixed here rather than at each call site.
     */
    private JComponentRow table(AbstractTableModel model) {
        final JTable table = new JTable(model);
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);
        final DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.TRAILING);
        for (int col = 1; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(right);
        }
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(170);
        }
        final JPanel holder = new JPanel(new BorderLayout());
        holder.add(table.getTableHeader(), BorderLayout.NORTH);
        holder.add(table, BorderLayout.CENTER);
        final int height = table.getRowHeight() * (model.getRowCount() + 1) + 4;
        holder.setPreferredSize(new Dimension(520, height));
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return new JComponentRow(holder);
    }

    /** Keeps every row left-aligned in the BoxLayout instead of drifting to the centre. */
    private static class JComponentRow extends JPanel {

        private static final long serialVersionUID = 1L;

        JComponentRow(java.awt.Component content) {
            super(new BorderLayout());
            setAlignmentX(LEFT_ALIGNMENT);
            // A table wants the width and brings its own height; a label wants neither, and
            // stretching one across the pane leaves it looking like a disabled text field.
            final boolean wide = content instanceof JPanel;
            add(content, wide ? BorderLayout.CENTER : BorderLayout.LINE_START);
            setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    content.getPreferredSize().height + 2));
        }
    }

    /** Before, after and the difference, per army. The summary the verdict line is drawn from. */
    private static class CasualtyModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private final transient java.util.List<ArmySim> armies;
        private final transient CombatResult result;

        CasualtyModel(CombatScenario scenario, CombatResult result) {
            this.armies = scenario.getArmies();
            this.result = result;
        }

        @Override
        public int getRowCount() {
            return armies.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return labels.getString("BATTLESIM.RESULTS.ARMY");
                case 1:
                    return labels.getString("BATTLESIM.RESULTS.BEFORE");
                case 2:
                    return labels.getString("BATTLESIM.RESULTS.AFTER");
                default:
                    return labels.getString("BATTLESIM.RESULTS.LOST");
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            final ArmySim army = armies.get(row);
            if (column == 0) {
                return army.getNome();
            }
            final int[] totals = BattleSimConverter.getArmyTotals(army, result);
            if (totals == null) {
                return "--";      // took no part: absent is not zero
            }
            return String.format("%,d", totals[column - 1]);
        }
    }

    /** Rows are armies, columns are rounds, cells are troops remaining. */
    private static class RoundsModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private final transient LayerReport report;

        RoundsModel(LayerReport report) {
            this.report = report;
        }

        @Override
        public int getRowCount() {
            return report.getArmies().size();
        }

        @Override
        public int getColumnCount() {
            return report.getRounds() + 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return labels.getString("BATTLESIM.RESULTS.ARMY");
            }
            if (column == 1) {
                return labels.getString("BATTLESIM.RESULTS.START");
            }
            // column 2 is the end of round 0, which is the first-strike round
            return column == 2 ? labels.getString("BATTLESIM.RESULTS.ROUNDFS")
                    : String.format(labels.getString("BATTLESIM.RESULTS.ROUND"), column - 2);
        }

        @Override
        public Object getValueAt(int row, int column) {
            final ArmySim army = report.getArmies().get(row);
            if (column == 0) {
                return army.getNome();
            }
            final int value = report.getRemaining(army, column - 1);
            return value < 0 ? "--" : String.format("%,d", value);
        }
    }
}
