package gui.accessories;

import business.combat.ArmySim;
import business.combat.TacticSweep;
import control.BattleSimControler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import msgs.BaseMsgs;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * Which tactic to pick, when his cannot be known. T-842b.
 *
 * <h3>The grid is the answer; the two lines above it are only a reading of it</h3>
 *
 * A single recommendation would be the wrong shape here, because the thing a player needs is not
 * "pick Flank" but "pick Flank, and the one thing that beats it is Ambush". So the 6x6 is shown
 * whole, rows being his tactics scored against yours, and the recommendations sit above it as
 * labelled interpretations rather than as the output.
 *
 * <h3>Safe and sharp are different claims and are never merged</h3>
 *
 * <b>Safe</b> is the best worst case against anything he might do. It assumes nothing.
 *
 * <b>Sharp</b> is the best worst case against the tactics he can actually afford, which is John's
 * pin-down: an army that needs its siege engines at the walls cannot pick the tactics that spend
 * them first, so those columns were never open to it. It is the stronger answer and it rests on an
 * ASSUMED objective - his combat level, which does not ride the EGF - so it is labelled with the
 * assumption rather than presented as fact. When his hands are free the two agree, and saying so is
 * itself useful.
 *
 * <h3>Greyed, not hidden</h3>
 *
 * Columns he cannot afford stay on screen in grey. "These are the tactics that do not get him what
 * he came for" is intelligence in its own right, and hiding them would leave the player unable to
 * see why the sharp answer differs from the safe one.
 */
public class TacticGridDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    /** Enough for six columns of a name and a figure without a horizontal scrollbar. */
    private static final int CELL_WIDTH = 96;
    private static final int NAME_WIDTH = 130;

    private final transient BattleSimControler controler;
    private final JComboBox<ArmySim> mine = new JComboBox<>();
    private final JComboBox<ArmySim> his = new JComboBox<>();
    private final JLabel safe = new JLabel(" ");
    private final JLabel sharp = new JLabel(" ");
    private final JTable grid = new JTable();
    private transient TacticSweep.Result result;

    public TacticGridDialog(BattleSimWindow owner, BattleSimControler controler) {
        super(owner, labels.getString("BATTLESIM.TACTICGRID.TITLE"), true);
        this.controler = controler;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildTop(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildTop() {
        final JPanel ret = new JPanel(new BorderLayout(0, 6));
        final JPanel pickers = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        for (ArmySim army : controler.getScenario().getArmies()) {
            mine.addItem(army);
            his.addItem(army);
        }
        // Sensible opening pair: the army he was looking at, and somebody else. A dialog that opens
        // with the same army on both sides answers nothing and looks broken.
        if (controler.getSelected() != null) {
            mine.setSelectedItem(controler.getSelected());
        }
        if (his.getItemCount() > 1 && his.getSelectedItem() == mine.getSelectedItem()) {
            his.setSelectedIndex(mine.getSelectedIndex() == 0 ? 1 : 0);
        }
        final ArmyRenderer renderer = new ArmyRenderer();
        mine.setRenderer(renderer);
        his.setRenderer(renderer);
        pickers.add(new JLabel(labels.getString("BATTLESIM.TACTICGRID.MINE")));
        pickers.add(mine);
        pickers.add(new JLabel(labels.getString("BATTLESIM.TACTICGRID.HIS")));
        pickers.add(his);
        ret.add(pickers, BorderLayout.NORTH);
        final JPanel advice = new JPanel(new java.awt.GridLayout(0, 1, 0, 2));
        advice.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 2));
        advice.add(safe);
        advice.add(sharp);
        ret.add(advice, BorderLayout.CENTER);
        return ret;
    }

    private JScrollPane buildGrid() {
        grid.setModel(new GridModel());
        grid.setRowSelectionAllowed(false);
        grid.setCellSelectionEnabled(true);
        grid.getTableHeader().setReorderingAllowed(false);
        grid.setDefaultRenderer(Object.class, new CellRenderer());
        final JScrollPane ret = new JScrollPane(grid);
        ret.setPreferredSize(new Dimension(NAME_WIDTH + CELL_WIDTH * 6 + 24, 190));
        ret.setBorder(BorderFactory.createTitledBorder(
                labels.getString("BATTLESIM.TACTICGRID.GRID")));
        return ret;
    }

    private JPanel buildButtons() {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
        final JButton run = new JButton(labels.getString("BATTLESIM.TACTICGRID.RUN"));
        run.setActionCommand("run");
        run.addActionListener(this);
        final JButton close = new JButton(labels.getString("BATTLESIM.DIPLOMACY.CLOSE"));
        close.setActionCommand("close");
        close.addActionListener(this);
        ret.add(run);
        ret.add(close);
        getRootPane().setDefaultButton(run);
        return ret;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if ("run".equals(event.getActionCommand())) {
            doRun();
        } else {
            dispose();
        }
    }

    private void doRun() {
        final int myIndex = mine.getSelectedIndex();
        final int hisIndex = his.getSelectedIndex();
        if (myIndex < 0 || hisIndex < 0 || myIndex == hisIndex) {
            safe.setText(labels.getString("BATTLESIM.TACTICGRID.SAMEARMY"));
            sharp.setText(" ");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            result = TacticSweep.sweep(controler.getScenario(),
                    controler.getScenario().getPartida() == null ? null
                            : controler.getScenario().getPartida().getCenario(),
                    myIndex, hisIndex);
            ((GridModel) grid.getModel()).fireTableStructureChanged();
            sizeColumns();
            safe.setText(safeText());
            sharp.setText(sharpText());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void sizeColumns() {
        if (grid.getColumnCount() == 0) {
            return;
        }
        grid.getColumnModel().getColumn(0).setPreferredWidth(NAME_WIDTH);
        for (int col = 1; col < grid.getColumnCount(); col++) {
            grid.getColumnModel().getColumn(col).setPreferredWidth(CELL_WIDTH);
        }
    }

    /** "Whatever he does, X is the safest: its worst case is <n> men left." */
    private String safeText() {
        if (result == null || result.getSafeChoice() < 0) {
            return " ";
        }
        return String.format(labels.getString("BATTLESIM.TACTICGRID.SAFE"),
                tacticName(result.getSafeChoice()), result.coverageOf(result.getSafeChoice()),
                result.getHisTactics().size());
    }

    /**
     * The sharp line, which has three quite different things to say and must not blur them.
     *
     * His hands free, his hands tied, or the battle already decided. Collapsing these into one
     * sentence would be the easy thing and would make the most valuable case - he is pinned -
     * indistinguishable from the most common one.
     */
    private String sharpText() {
        if (result == null || result.getSharpChoice() < 0) {
            return " ";
        }
        if (result.getHisViable().isEmpty()) {
            return labels.getString("BATTLESIM.TACTICGRID.HEISBEATEN");
        }
        if (result.getHisViable().size() == result.getHisTactics().size()) {
            return labels.getString("BATTLESIM.TACTICGRID.HEISFREE");
        }
        final StringBuilder names = new StringBuilder();
        for (Integer one : result.getHisViable()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(tacticName(one));
        }
        return String.format(labels.getString("BATTLESIM.TACTICGRID.SHARP"),
                names.toString(), tacticName(result.getSharpChoice()));
    }

    private static String tacticName(int tactic) {
        return tactic >= 0 && tactic < BaseMsgs.taticasLabel.length
                ? BaseMsgs.taticasLabel[tactic] : String.valueOf(tactic);
    }

    /** Rows are my tactics, columns his. The cell is what is left of me, and whether I got it. */
    private class GridModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return result == null ? 0 : result.getMyTactics().size();
        }

        @Override
        public int getColumnCount() {
            return result == null ? 0 : result.getHisTactics().size() + 1;
        }

        @Override
        public String getColumnName(int column) {
            if (result == null) {
                return "";
            }
            return column == 0 ? labels.getString("BATTLESIM.TACTICGRID.MYTACTIC")
                    : tacticName(result.getHisTactics().get(column - 1));
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (result == null) {
                return "";
            }
            if (column == 0) {
                return tacticName(result.getMyTactics().get(row));
            }
            final TacticSweep.Cell cell = result.get(row, column - 1);
            // The tick is the answer and the number is the cost of it. Showing only the number
            // would make a narrow win look like a bad result, and only the tick would hide that two
            // wins can differ by a thousand men.
            return String.format(labels.getString(cell.isMine()
                    ? "BATTLESIM.TACTICGRID.CELL.WIN" : "BATTLESIM.TACTICGRID.CELL.LOSS"),
                    cell.getMySurvivors());
        }
    }

    /** Wins stand out, his unaffordable columns recede, and the recommended row is marked. */
    private class CellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            final Component ret = super.getTableCellRendererComponent(table, value, selected,
                    focus, row, column);
            // Reset EVERY time. A renderer is one component reused down the column, so a colour set
            // on one row leaks onto the next unless every path assigns one.
            ret.setForeground(table.getForeground());
            ret.setFont(table.getFont());
            if (result == null || selected) {
                return ret;
            }
            if (column > 0 && !result.getHisViable().isEmpty()
                    && !result.getHisViable().contains(result.getHisTactics().get(column - 1))) {
                // Greyed rather than hidden: "he cannot afford this column" is intelligence.
                ret.setForeground(Color.GRAY);
            } else if (column > 0 && result.get(row, column - 1).isMine()) {
                ret.setFont(table.getFont().deriveFont(java.awt.Font.BOLD));
            }
            if (column == 0 && result.getMyTactics().get(row) == result.getSharpChoice()) {
                ret.setFont(table.getFont().deriveFont(java.awt.Font.BOLD));
            }
            return ret;
        }
    }

    /** Armies by their display name, which is what the roster calls them. */
    private class ArmyRenderer extends javax.swing.DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                int index, boolean selected, boolean focus) {
            final Component ret = super.getListCellRendererComponent(list, value, index, selected,
                    focus);
            if (value instanceof ArmySim) {
                setText(controler.getScenario().getDisplayName((ArmySim) value));
            }
            return ret;
        }
    }
}
