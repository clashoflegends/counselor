package gui.accessories;

import business.combat.RelationshipMatrix;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import model.Nacao;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * The diplomacy panel: the nation-by-nation table, shown and editable. T-418.
 *
 * <h3>Why this is the centre of the simulator and not a detail</h3>
 *
 * John, 2026-09-19: "For the BattleSim, the matrix is the law and canonical." That is literally the
 * Judge's structure - {@code ExercitoControl.isInimigo(inimigo)} is nothing but
 * {@code getNacaoControl().isInimigo(inimigo.getNacaoControl())} - so this table is the whole input
 * to who fights whom. Everything else on the BattleSim window decides how hard they hit.
 *
 * <h3>Square, not triangular</h3>
 *
 * A cell is what the ROW faction thinks of the COLUMN faction, and the two halves need not agree. A
 * vassal and its lord hold opposite values, and a declaration of war is unilateral - it changes only
 * the declarer's row, and the reciprocity rules in {@code NacaoControl.doArrumaRelacionamentos} are
 * commented-out stubs, so nothing brings the other side into line. Folding the grid in half would
 * mean picking one of the two to show and silently dropping the other.
 *
 * One declaration is still enough to start a battle, because the Judge writes BOTH enemy lists the
 * moment either direction reads hostile. That is why both mirrored cells are painted as fighting
 * even when only one of them says so: the player should see the consequence, not just his own half.
 *
 * <h3>The grey cells are the point</h3>
 *
 * The Counselor loads ONE results EGF, so the only complete relationship rows in it are the
 * observer's own. Every other cell is a guess, and R-15 exists because a guessed peace looks exactly
 * like a known one once it reaches a number. They are drawn in grey and they are editable, which is
 * the whole answer to "what if he has allies I cannot see".
 *
 * <h3>What IS inferred, and what still is not</h3>
 *
 * Mirroring landed 2026-09-21 on John's call - "all diplomacy is bidirectional (which it is in
 * almost every case)" - so a cell whose own row is unreadable takes the OTHER direction's value and
 * is marked MIRRORED, shown in italic. It is an inference and it is labelled as one, which is the
 * whole difference from the pass-two rules still deliberately unwritten: team flags and transitive
 * alliance would fill a cell with something no row ever said.
 *
 * A pair neither side can read is assumed HOSTILE, also on John's call. The status bar counts those
 * and says so.
 */
public class BattleSimDiplomacyDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    /** Grey for a guess. Mid-grey on purpose: it is the one value legible against either theme. */
    private static final Color ASSUMED = new Color(0x88, 0x88, 0x88);
    /** This pair is going to fight. Two of them, because one red cannot serve both themes. */
    private static final Color HOSTILE_ON_LIGHT = new Color(0xA3, 0x1D, 0x1D);
    private static final Color HOSTILE_ON_DARK = new Color(0xFF, 0x6B, 0x6B);

    /**
     * Is this a dark theme? Perceived luminance, not a plain average - the eye weights green most.
     *
     * Asked of the table rather than of a setting, because the table is what the cell is painted
     * on and a LookAndFeel change does not announce itself to this dialog.
     */
    private static boolean isDark(Color background) {
        return (background.getRed() * 299 + background.getGreen() * 587
                + background.getBlue() * 114) / 1000 < 128;
    }

    /**
     * The "not applicable" shade for the diagonal, DERIVED from the table rather than fixed.
     *
     * It was a hardcoded {@code 0xE8E8E8}, which is near-white: on John's dark theme every diagonal
     * cell came out as a glaring pale block that read as an empty editable field - the opposite of
     * "there is nothing here to set". A small step from the table's own background toward its
     * foreground reads as a subtle inset in either theme and as a light box in neither.
     */
    private static Color diagonalOf(JTable table) {
        final Color bg = table.getBackground();
        final Color fg = table.getForeground();
        return new Color((bg.getRed() * 7 + fg.getRed()) / 8,
                (bg.getGreen() * 7 + fg.getGreen()) / 8,
                (bg.getBlue() * 7 + fg.getBlue()) / 8);
    }

    private final transient BattleSimControler controler;
    private final JTable grid = new JTable();
    private final transient Runnable onChange;

    /**
     * NON-MODAL, deliberately.
     *
     * John, 2026-09-21: "Can we make the Diplomacy window non-modal? I wanted to check the tab
     * nations to edit and I can't flip between both."
     *
     * That is the actual workflow: the answer to "are these two at war?" lives on the Nations tab,
     * and a modal dialog made the player close the thing he was filling in to go and look it up.
     * The matrix is also the one panel where an edit is a HYPOTHESIS - "suppose he declares on me" -
     * so being able to hold it open beside the evidence is the point.
     *
     * Because it is non-modal, {@code onChange} fires on every EDIT rather than only on close: the
     * window behind has to follow along while the player works, not snap to the new answer minutes
     * later. The caller keeps the instance so a second click raises this dialog instead of stacking
     * another one, each with its own copy of the grid.
     *
     * @param owner    the BattleSim window, which owns this dialog and disposes it
     * @param onChange run after every edit and again on close, so the window behind stays true
     */
    public BattleSimDiplomacyDialog(Frame owner, BattleSimControler controler, Runnable onChange) {
        super(owner, labels.getString("BATTLESIM.DIPLOMACY.TITLE"), false);
        this.controler = controler;
        this.onChange = onChange;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildHint(), BorderLayout.NORTH);
        add(new JScrollPane(grid), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        buildGrid();
        setMinimumSize(new Dimension(640, 320));
        pack();
        setSize(Math.max(getWidth(), 720), Math.min(Math.max(getHeight(), 360), 640));
        setLocationRelativeTo(owner);
    }

    private JPanel buildHint() {
        final JPanel ret = new JPanel(new BorderLayout(0, 2));
        ret.setBorder(BorderFactory.createEmptyBorder(8, 10, 6, 10));
        ret.add(wrapped("BATTLESIM.DIPLOMACY.HINT", Font.PLAIN), BorderLayout.NORTH);
        ret.add(wrapped("BATTLESIM.DIPLOMACY.LEGEND", Font.PLAIN), BorderLayout.SOUTH);
        return ret;
    }

    /** HTML so a long sentence wraps instead of running off the side of a narrow dialog. */
    private JLabel wrapped(String key, int style) {
        final JLabel ret = new JLabel("<html>" + labels.getString(key) + "</html>");
        ret.setFont(ret.getFont().deriveFont(style));
        return ret;
    }

    private JPanel buildButtons() {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 6));
        ret.add(button("BATTLESIM.DIPLOMACY.RESET", "reset"));
        ret.add(button("BATTLESIM.DIPLOMACY.CLOSE", "close"));
        return ret;
    }

    private JButton button(String key, String command) {
        final JButton ret = new JButton(labels.getString(key));
        ret.setActionCommand(command);
        ret.addActionListener(this);
        return ret;
    }

    /**
     * Builds the grid and its editor.
     *
     * The row header column is frozen at a readable width and the value columns are left to share
     * what is left, because a faction name is long and "Peace pact" is not.
     */
    private void buildGrid() {
        final BattleSimControler.DiplomacyTableModel model = controler.getDiplomacyModel();
        grid.setModel(model);
        grid.setRowHeight(Math.max(22, grid.getRowHeight()));
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        grid.getTableHeader().setReorderingAllowed(false);
        grid.setCellSelectionEnabled(true);
        if (grid.getColumnCount() > 0) {
            grid.getColumnModel().getColumn(0).setPreferredWidth(160);
            grid.getColumnModel().getColumn(0).setCellRenderer(new NationNameRenderer());
        }
        // non-modal: the window behind must follow each edit, not wait for the dialog to close
        model.addTableModelListener(event -> {
            if (onChange != null) {
                onChange.run();
            }
        });
        final DiplomacyCellRenderer renderer = new DiplomacyCellRenderer(model);
        final DefaultCellEditor editor = new DefaultCellEditor(relationshipCombo());
        for (int ii = 1; ii < grid.getColumnCount(); ii++) {
            grid.getColumnModel().getColumn(ii).setCellRenderer(renderer);
            grid.getColumnModel().getColumn(ii).setCellEditor(editor);
        }
    }

    /** The seven steps, by their own names. No new vocabulary: this is what the game calls them. */
    private JComboBox<Integer> relationshipCombo() {
        final Integer[] valores = new Integer[RelationshipMatrix.LORD - RelationshipMatrix.SWORN_ENEMY + 1];
        for (int ii = 0; ii < valores.length; ii++) {
            valores[ii] = RelationshipMatrix.SWORN_ENEMY + ii;
        }
        final JComboBox<Integer> ret = new JComboBox<>(valores);
        ret.setRenderer(new ListCellRenderer<Integer>() {
            private final DefaultTableCellRenderer delegate = new DefaultTableCellRenderer();

            @Override
            public Component getListCellRendererComponent(JList<? extends Integer> list,
                    Integer value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel label = (JLabel) delegate;
                label.setText(value == null ? "" : BattleSimConverter.getRelationshipName(value));
                label.setOpaque(true);
                label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                return label;
            }
        });
        return ret;
    }

    /** The leading column: the row's own faction, by name. */
    private static class NationNameRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        protected void setValue(Object value) {
            setText(value instanceof Nacao ? String.valueOf(((Nacao) value).getNome()) : "");
        }
    }

    /**
     * Paints a cell so its ORIGIN is visible without a click.
     *
     * The distinction this exists for is grey versus not: grey means nobody told the player, and it
     * is the only thing on the screen that says which numbers his conclusion is actually resting on.
     * Bold means he said so himself, so he can tell his own what-if from the game's facts after
     * walking away and coming back.
     *
     * Colours are set on EVERY path, never left to fall through. A DefaultTableCellRenderer is
     * shared across cells, so a colour set on one cell and not cleared on the next bleeds down the
     * column - the renderer-leak trap that has bitten this codebase before.
     */
    private static class DiplomacyCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        private final transient BattleSimControler.DiplomacyTableModel model;

        DiplomacyCellRenderer(BattleSimControler.DiplomacyTableModel model) {
            this.model = model;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int modelRow = table.convertRowIndexToModel(row);
            final int modelColumn = table.convertColumnIndexToModel(column);
            final RelationshipMatrix.Origin origin = model.getOrigin(modelRow, modelColumn);

            if (value == null) {
                // the diagonal: a faction's view of itself, which the model fixes at neutral
                setText("");
                setBackground(isSelected ? table.getSelectionBackground() : diagonalOf(table));
                setForeground(table.getForeground());
                setFont(getFont().deriveFont(Font.PLAIN));
                return this;
            }
            setText(BattleSimConverter.getRelationshipName((Integer) value));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            // bold = the player said so; italic = mirrored from the other direction, which is a
            // strong inference but still an inference; plain = read or set by the game type
            final int style;
            if (origin == RelationshipMatrix.Origin.PLAYER_EDITED) {
                style = Font.BOLD;
            } else if (origin == RelationshipMatrix.Origin.MIRRORED) {
                style = Font.ITALIC;
            } else {
                style = Font.PLAIN;
            }
            setFont(getFont().deriveFont(style));
            if (origin == RelationshipMatrix.Origin.ASSUMED) {
                setForeground(ASSUMED);
            } else if (model.isHostile(modelRow, modelColumn)) {
                setForeground(isDark(table.getBackground())
                        ? HOSTILE_ON_DARK : HOSTILE_ON_LIGHT);
            } else {
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if ("reset".equals(event.getActionCommand())) {
            controler.doResetDiplomacy();
            buildGrid();
            return;
        }
        dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (onChange != null) {
            onChange.run();
        }
    }
}
