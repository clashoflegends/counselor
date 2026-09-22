package gui.accessories;

import baseLib.BaseModel;
import baseLib.GenericoComboObject;
import business.ImageManager;
import business.combat.ArmySim;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.converter.ConverterFactory;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Terreno;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * BattleSim, the three-pane rebuild (design 4.1).
 *
 * <h3>Hand-laid-out on purpose</h3>
 *
 * There is no {@code .form} beside this class and there should not be one. It is a sister window to
 * {@link BattleCasualtySimulatorNew}, which keeps its Matisse form untouched, and which of the two
 * opens is a {@code properties.config} switch - see {@code radialMenu.RmActionListener}. Two windows
 * over one shared model, so the new layout can be tried without putting the working one at risk.
 * John's call, 2026-09-19: rebuild it by hand now, bring it back into Matisse later if it is worth
 * doing.
 *
 * <h3>Where the thinking is</h3>
 *
 * Not here. This class lays out widgets and forwards events; every question it asks is answered by
 * {@code CombatScenario} and friends in PbmCommons, and every word it shows comes from
 * {@link BattleSimConverter}. That is what lets the rules be tested without opening a window, and it
 * is what makes this file boring, which is the point.
 *
 * <h3>Run is disabled, and says why</h3>
 *
 * There is no engine yet. The reason sits in the status bar rather than a tooltip, because a tooltip
 * on a disabled button is unreliable across platforms and this is the one message a player needs
 * when the button does nothing - which is the complaint that started this whole rebuild.
 */
public class BattleSimWindow extends JFrame implements ActionListener, ChangeListener,
        TreeSelectionListener {

    private static final long serialVersionUID = 1L;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private final transient BattleSimControler controler;

    /**
     * The roster, with a tooltip that explains the battle-result mark on each row.
     *
     * An emoji is a guess unless something says what it means, and glyph rendering varies by
     * machine - so the hover answers in words what the mark says in a picture, and neither depends
     * on the other being legible.
     */
    private final JTree roster = new JTree() {
        private static final long serialVersionUID = 1L;

        @Override
        public String getToolTipText(java.awt.event.MouseEvent event) {
            final javax.swing.tree.TreePath path = getPathForLocation(event.getX(), event.getY());
            if (path == null
                    || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                return null;
            }
            final Object user =
                    ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            return user instanceof BattleSimControler.ArmyNode
                    ? ((BattleSimControler.ArmyNode) user).getHint() : null;
        }
    };
    /**
     * Fills the pane when the columns fit, scrolls when they do not.
     *
     * Neither resize mode alone gets this right. {@code AUTO_RESIZE_LAST_COLUMN} forces the table
     * to the viewport width always, so a fleet - which carries three extra transport columns
     * (T-428) - crushed thirteen columns into the pane and truncated the headers to "Trai...",
     * "We...", "Carg...". {@code AUTO_RESIZE_OFF} fixes that and breaks the common case instead: a
     * land army's ten columns no longer reach the right edge and leave a band of empty grey.
     *
     * {@code getScrollableTracksViewportWidth} is the seam between the two. Answering TRUE hands
     * the table to the viewport, which is what makes the auto-resize mode spread the columns to
     * fill; answering FALSE lets the table keep its own width and the scroll pane put a bar under
     * it. So: true while the columns fit, false once they do not.
     *
     * Measured against the columns' PREFERRED widths rather than {@code getPreferredSize()}, which
     * reports their CURRENT widths - those move every time auto-resize spreads them, and feeding
     * that back into the decision makes the answer oscillate with the pane. The preferred widths
     * are set once per model swap in {@link #configurePlatoonColumns} and never drift.
     */
    private final JTable platoons = new JTable() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() == null) {
                return true;
            }
            int wanted = 0;
            for (int ii = 0; ii < getColumnModel().getColumnCount(); ii++) {
                wanted += getColumnModel().getColumn(ii).getPreferredWidth();
            }
            return wanted <= getParent().getWidth();
        }
    };
    private final JLabel status = new JLabel();
    private final JLabel runReason = new JLabel();
    private final JLabel armyTitle = new JLabel();
    private final JLabel fightsIn = new JLabel();
    private final JLabel source = new JLabel();
    private final JLabel sizeBand = new JLabel();
    private final JLabel strength = new JLabel();
    /** What the platoon table's row order means for the selected army. T-437. */
    private final JLabel casualtyMode = new JLabel();
    private final JButton run = new JButton(labels.getString("BATTLESIM.RUN.SIMULATION"));
    private final JButton diplomacy = new JButton(labels.getString("BATTLESIM.DIPLOMACY"));

    private final JComboBox<Object> nacao = new JComboBox<>();
    private final JComboBox<Object> terreno = new JComboBox<>();
    private final JComboBox<CombatLevel> combatLevel = new JComboBox<>(CombatLevel.values());
    private final JComboBox<Object> target = new JComboBox<>();
    private final JCheckBox cityParticipates =
            new JCheckBox(labels.getString("BATTLESIM.CITY.PARTICIPATES"));
    private final JLabel cityText = new JLabel();
    private final JComboBox<Object> cityOwner = new JComboBox<>();
    /** Loyalty is a percentage and the city model treats it as one. */
    private final JSpinner cityLoyalty = spinner(0, 0, 100, 1);
    private final JComboBox<Object> citySize = new JComboBox<>();
    private final JComboBox<Object> cityFortification = new JComboBox<>();
    private final JComboBox<Object> tactic = new JComboBox<>();
    /**
     * Ranges taken from what the GAME can actually produce (T-436), not from a guess.
     *
     * <b>Commander skill: 0 and up, with NO ceiling.</b> The natural skill is capped at
     * {@code PersonagemControl.MAX_PC_SKILL} = 100, raisable per game by {@code ;GSS;} - but
     * artefacts add on top of that cap through {@code Personagem.sumPericiaComandante}, so the
     * EFFECTIVE value the EGF carries can exceed it and nothing in the model clamps it. A ceiling
     * of 100 would have silently edited an artefact-boosted commander DOWN the moment his army was
     * selected, which is the clamp-on-load trap in its purest form.
     *
     * <b>Morale: 0 to 100.</b> The Judge clamps it to 1..100 in {@code ExercitoControl.setMoral},
     * so 100 is authoritative. The 0 is kept deliberately: an army the player has not scouted
     * arrives with morale 0 because the server never exported it, and the live EGF confirms it
     * (189 armies, minimum 0). A floor of 1 would rewrite every unscouted army on load.
     *
     * <b>Attack and defence bonus: 0 and up, step 100, no ceiling and NO NEGATIVES.</b> These are
     * spell effects: {@code Ordem247CombatAttackBonus} adds {@code 20 * getPericia()} and they
     * stack across casters, so real values run into the thousands - the previous -100..100 was
     * wrong by two orders of magnitude. Negatives are not offered because the Judge cannot produce
     * one: {@code ExercitoControl} floors the defence bonus at {@code Math.max(bonusDefesa - dano,
     * 0)}. Offering one would let the player build a scenario the turn cannot.
     *
     * Neither bonus is carried in the EGF at all - {@code model.Exercito.getAttackBonus()} is
     * hardcoded to return 0 - so they always load as 0 and are purely the player's what-if.
     */
    private final JSpinner commander = spinner(0, 0, null, 1);
    private final JSpinner morale = spinner(0, 0, 100, 1);
    private final JSpinner attackBonus = spinner(0, 0, null, 100);
    private final JSpinner defenseBonus = spinner(0, 0, null, 100);

    /** Guards the listeners while the editor is being repopulated from the model. */
    private boolean refreshing = false;
    /** Built once: the catalogue does not change, and this is reattached after every model swap. */
    private DefaultCellEditor troopTypeEditor;
    /** The non-modal diplomacy grid, kept so a second click raises it instead of stacking one. */
    private transient BattleSimDiplomacyDialog diplomacyDialog;
    /**
     * Built once, reattached after every model swap. Renderers are stateless, and allocating a
     * fresh pair on each of the dozens of refreshes was pure churn.
     */
    private final transient DefaultTableCellRenderer centredCell = centredRenderer();
    private final transient DefaultTableCellRenderer troopTypeCell = troopTypeRenderer();

    /**
     * A platoon edit refreshes the window like every other edit, one turn of the event queue later.
     *
     * Deferred rather than immediate because this fires from inside {@code setValueAt}, which the
     * cell editor calls on its way out; {@link #doRefresh} replaces the table's model, and
     * replacing a model under an editor that has not finished stopping is how a Swing table ends up
     * writing into the wrong row. {@code invokeLater} lets the edit finish first.
     */
    private final transient TableModelListener platoonEdits = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent event) {
            if (refreshing || event.getFirstRow() == TableModelEvent.HEADER_ROW
                    || event.getType() != TableModelEvent.UPDATE) {
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!refreshing) {
                        doEdited(false);
                    }
                }
            });
        }
    };

    public BattleSimWindow(Local local) {
        this.controler = new BattleSimControler(local);
        setTitle(String.format(labels.getString("BATTLESIM.TITLE"),
                local == null ? "" : local.getCoordenadas()));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildPanes(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        final BattleSimCellRenderer renderer = new BattleSimCellRenderer();
        for (JComboBox<?> one : new JComboBox<?>[]{nacao, terreno, combatLevel, target, tactic,
            citySize, cityFortification, cityOwner}) {
            one.setRenderer(renderer);
        }
        // guarded: setting a combo's model fires an action for the newly selected item, and the
        // listeners are already attached by now - unguarded, building the models would write
        // index 0 back into the first army's tactic and the city's size before the window opened
        final JComboBox<Object> types = new JComboBox<>(controler.getTroopCatalogue().toArray());
        types.setRenderer(renderer);
        troopTypeEditor = new DefaultCellEditor(types) {
            private static final long serialVersionUID = 1L;

            /**
             * Makes room for a troop type the catalogue does not hold, instead of losing the edit.
             *
             * {@code JComboBox.setSelectedItem} REFUSES a value its model does not contain and
             * keeps the previous selection, and {@code DefaultCellEditor} would then hand that
             * stale selection back to {@code setValueAt} - silently retyping the platoon to
             * whatever was picked last. Types outside the catalogue are real: the placeholder
             * {@code none}/{@code ship} an unscouted enemy arrives with, and {@code ;STS;}
             * side-loaded specials. Inserting the value is the honest answer; dropping it is not.
             */
            @Override
            public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                final DefaultComboBoxModel<Object> model =
                        (DefaultComboBoxModel<Object>) types.getModel();
                if (value != null && model.getIndexOf(value) < 0) {
                    model.insertElementAt(value, 0);
                }
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        };

        refreshing = true;
        try {
            buildComboModels();
        } finally {
            refreshing = false;
        }
        setIconImage(scenarioIcon());
        // pack first so every pane gets its natural height, then enforce a floor: a packed
        // BattleSim on an empty hex is small enough that the platoon table has nowhere to appear
        setMinimumSize(new Dimension(860, 560));
        pack();
        setSize(Math.max(getWidth(), 980), Math.max(getHeight(), 640));
        doRefresh();
    }

    /**
     * @param max null for no ceiling, which is the right answer whenever a value LOADED from the
     *            EGF could exceed any bound we invent. A SpinnerNumberModel silently clamps
     *            {@code setValue} to its bounds, so too low a maximum does not warn - it edits the
     *            army as the player selects it.
     */
    private static JSpinner spinner(int value, int min, Integer max, int step) {
        return new JSpinner(new SpinnerNumberModel(Integer.valueOf(value), Integer.valueOf(min),
                max, Integer.valueOf(step)));
    }

    /**
     * Fills every combo ONCE, at construction.
     *
     * Rebuilding a combo's model on each refresh broke keyboard navigation outright: arrowing
     * through a closed combo fires an action per step, the action refreshed the window, and the
     * refresh replaced the model and reset the selection - so the list snapped back on every key.
     * Models are static data; only the SELECTION follows the scenario.
     *
     * The nation lists hold EVERY nation in the world, not just the ones with an army on this hex.
     * Offering only the nations present made the editor a trap: retype the one enemy to match the
     * one friend and the hex has a single nation, no combat, and no way to put it back - the
     * nation that was there is no longer in the list. Adding or cloning an army made it worse,
     * since everything new inherited the survivor.
     */
    private void buildComboModels() {
        final Object[] nacoes = nacoes();
        nacao.setModel(new DefaultComboBoxModel<>(nacoes));
        cityOwner.setModel(new DefaultComboBoxModel<>(nacoes));

        final Object[] targets = new Object[nacoes.length + 1];
        targets[0] = labels.getString("BATTLESIM.TARGET.ALL");
        System.arraycopy(nacoes, 0, targets, 1, nacoes.length);
        target.setModel(new DefaultComboBoxModel<>(targets));

        terreno.setModel(new DefaultComboBoxModel<>(terrenos()));
        tactic.setModel(control.services.CenarioConverter.getInstance().getTaticaComboModel());
        citySize.setModel(new DefaultComboBoxModel<>(cityLevels(true)));
        cityFortification.setModel(new DefaultComboBoxModel<>(cityLevels(false)));
    }

    /**
     * The window's taskbar icon: the hex's terrain, as the sibling accessory windows do
     * ({@code ArmyMoveSimulator}, {@code TroopsCasualtiesList}, and the old BattleSim). Falls back
     * to the application icon, because a window with no icon gets Java's default coffee cup.
     */
    private java.awt.Image scenarioIcon() {
        final Terreno ground = controler.getScenario().getTerreno();
        final java.awt.Image ret = ground == null ? null
                : ImageManager.getInstance().getTerrainImages(ground.getCodigo());
        return ret == null ? ImageManager.getInstance().getIconApp() : ret;
    }

    /**
     * Renders what goes in every combo here.
     *
     * Needed because {@code BaseModel.toString()} answers "Tyrell-model.Nacao@1a2b3c", so a nation
     * or terrain dropped into a combo unrendered shows its class name and hash to the player. The
     * old window avoided this by routing everything through {@code GenericoComboBoxModel}; this one
     * renders at the view instead, which also lets an enum carry a translated name rather than
     * ATTACK_ARMY.
     */
    private static class BattleSimCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                Object value, int index, boolean selected, boolean focused) {
            final Object shown;
            if (value instanceof CombatLevel) {
                shown = BattleSimConverter.getCombatLevelName((CombatLevel) value);
            } else if (value instanceof BaseModel) {
                shown = ((BaseModel) value).getNome();
            } else {
                shown = value;
            }
            return super.getListCellRendererComponent(list, shown, index, selected, focused);
        }
    }

    // ------------------------------------------------------------------ layout

    /**
     * The army buttons on the left, Run on the right.
     *
     * Run is pushed to the far edge rather than sitting in the row because it is the only button
     * that does something to the whole scenario; the other three act on one army. Grouping by what
     * a control affects is the cheapest way to make a toolbar readable.
     */
    private JPanel buildToolbar() {
        final JPanel ret = new JPanel(new BorderLayout());
        ret.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        final JPanel left = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        left.add(button("BATTLESIM.ARMY.ADD", "addArmy"));
        left.add(button("BATTLESIM.ARMY.CLONE", "cloneArmy"));
        left.add(button("BATTLESIM.ARMY.REMOVE", "removeArmy"));
        // Diplomacy sits with the army buttons rather than beside Run, because it edits the
        // scenario like they do. The matrix IS the law for who fights whom (T-418), so this is not
        // an advanced option tucked away - it is the other half of setting up the battle.
        left.add(diplomacy);
        diplomacy.setActionCommand("diplomacy");
        diplomacy.addActionListener(this);
        // Reference and export, ported from the old window (T-429, T-430, T-434, T-435). None of
        // them edits the scenario, which is why they sit apart from the army buttons.
        left.add(tooltipped(button("TATICA", "tactics"), "BATTLESIM.TATICA.HINT"));
        left.add(button("TROOPCASUALTIES.BORDER.TITLE", "casualties"));
        left.add(tooltipped(button("BATTLESIM.COPY", "copy"), "COPIAR.ARMY.ACOES"));
        left.add(tooltipped(button("MENU.ABOUT", "about"), "BATTLESIM.ABOUT.TOOLTIP"));
        ret.add(left, BorderLayout.LINE_START);

        run.setActionCommand("run");
        run.addActionListener(this);
        run.setEnabled(false);
        final JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING, 4, 0));
        right.add(run);
        ret.add(right, BorderLayout.LINE_END);
        return ret;
    }

    private static JButton tooltipped(JButton button, String tooltipKey) {
        button.setToolTipText(labels.getString(tooltipKey));
        return button;
    }

    private JButton button(String key, String command) {
        final JButton ret = new JButton(labels.getString(key));
        ret.setActionCommand(command);
        ret.addActionListener(this);
        return ret;
    }

    private JSplitPane buildPanes() {
        final JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildArmyEditor(), buildPlatoonTable());
        // the editor is a fixed-height form and the table is the part worth growing, so all the
        // slack goes to the table
        right.setResizeWeight(0.0);
        right.setOneTouchExpandable(true);
        right.setBorder(null);

        final JSplitPane ret = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), right);
        ret.setResizeWeight(0.28);
        ret.setOneTouchExpandable(true);
        ret.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        return ret;
    }

    private JPanel buildLeft() {
        roster.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        roster.setRootVisible(false);
        // without this the overridden getToolTipText is never consulted
        javax.swing.ToolTipManager.sharedInstance().registerComponent(roster);
        roster.setShowsRootHandles(true);
        roster.addTreeSelectionListener(this);

        roster.setRowHeight(0);     // let the renderer decide, so it scales with the font

        final JScrollPane scroll = new JScrollPane(roster);
        scroll.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.ARMIES.TITLE")));

        final JPanel ret = new JPanel(new BorderLayout(0, 4));
        ret.add(scroll, BorderLayout.CENTER);
        ret.add(buildGround(), BorderLayout.SOUTH);
        ret.setPreferredSize(new Dimension(300, 480));
        ret.setMinimumSize(new Dimension(240, 240));
        return ret;
    }

    /**
     * Terrain and the city: the ground both sides are standing on.
     *
     * One grid rather than a stack of rows, so every label lines up on the same right edge and the
     * fields on the same left one. The previous version gave each row its own FlowLayout, which
     * left the labels ragged and made four unrelated-looking controls out of four related ones.
     */
    private JPanel buildGround() {
        final JPanel ret = new JPanel(new GridBagLayout());
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.GROUND.TITLE")));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        terreno.setActionCommand("terreno");
        terreno.addActionListener(this);
        addRow(ret, gbc, 0, labels.getString("TERRENO"), terreno);

        cityParticipates.setActionCommand("city");
        cityParticipates.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        ret.add(cityParticipates, gbc);

        // A city is passive: it is attacked, it damages the attackers, and it takes the result.
        // So it gets inputs and a read-out, not orders.
        citySize.setActionCommand("citySize");
        citySize.addActionListener(this);
        cityFortification.setActionCommand("cityFort");
        cityFortification.addActionListener(this);
        cityLoyalty.addChangeListener(this);
        cityOwner.setActionCommand("cityOwner");
        cityOwner.addActionListener(this);
        addRow(ret, gbc, 2, labels.getString("NACAO"), cityOwner);
        addRow(ret, gbc, 3, labels.getString("TAMANHO"), citySize);
        addRow(ret, gbc, 4, labels.getString("FORTIFICACOES"), cityFortification);
        addRow(ret, gbc, 5, labels.getString("LEALDADE"), cityLoyalty);

        cityText.setFont(cityText.getFont().deriveFont(Font.PLAIN));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 4, 2, 4);
        ret.add(cityText, gbc);
        return ret;
    }

    /** One label-and-field row, label right-aligned against the field's left edge. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String text,
            java.awt.Component field) {
        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        final JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        panel.add(label, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        panel.add(field, gbc);
    }

    private JPanel buildArmyEditor() {
        final JPanel ret = new JPanel(new GridBagLayout());
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.ARMY.TITLE")));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        armyTitle.setHorizontalAlignment(SwingConstants.LEADING);
        armyTitle.setFont(armyTitle.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(2, 4, 8, 4);
        ret.add(armyTitle, gbc);
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridwidth = 1;

        nacao.setActionCommand("nacao");
        nacao.addActionListener(this);
        addPair(ret, gbc, 1, 0, labels.getString("NACAO"), nacao);
        tactic.setActionCommand("tactic");
        tactic.addActionListener(this);
        addPair(ret, gbc, 1, 2, labels.getString("TATICA"), tactic);
        addPair(ret, gbc, 2, 0, labels.getString("COMANDANTE"), commander);
        addPair(ret, gbc, 2, 2, labels.getString("MORAL"), morale);
        addPair(ret, gbc, 3, 0, labels.getString("BATTLESIM.ATTACK.BONUS"), attackBonus);
        addPair(ret, gbc, 3, 2, labels.getString("BATTLESIM.DEFENSE.BONUS"), defenseBonus);

        combatLevel.setActionCommand("level");
        combatLevel.addActionListener(this);
        addPair(ret, gbc, 4, 0, labels.getString("BATTLESIM.COMBAT.LEVEL"), combatLevel);
        target.setActionCommand("target");
        target.addActionListener(this);
        addPair(ret, gbc, 4, 2, labels.getString("BATTLESIM.COMBAT.TARGET"), target);

        // the two derived lines: what this army will actually do, and how much to trust it.
        // Given air above them so they read as a conclusion rather than another field.
        for (JLabel one : new JLabel[]{strength, sizeBand, fightsIn, source}) {
            one.setFont(one.getFont().deriveFont(Font.PLAIN));
        }
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(10, 4, 1, 4);
        // The four numbers that answer "who is stronger" - the old window's whole reason to
        // exist, and the one thing this rebuild was still missing. Read-only: they are computed
        // from the platoons, the terrain and the nation, so the way to change them is to change
        // those.
        ret.add(strength, gbc);
        gbc.gridy = 6;
        gbc.insets = new Insets(6, 4, 1, 4);
        // the server's own description of how big this army is. A LABEL, never a field: the band
        // is what the player was told, and turning it into an editable number would invite him to
        // treat a guess as data. What he types instead is the platoon list below.
        ret.add(sizeBand, gbc);
        gbc.gridy = 7;
        gbc.insets = new Insets(1, 4, 1, 4);
        ret.add(fightsIn, gbc);
        gbc.gridy = 8;
        gbc.insets = new Insets(1, 4, 4, 4);
        ret.add(source, gbc);

        for (JSpinner one : new JSpinner[]{commander, morale, attackBonus, defenseBonus}) {
            one.addChangeListener(this);
        }
        return ret;
    }

    /**
     * One label-and-field pair in the army editor's two-column form.
     *
     * Labels are right-aligned so both columns present a single edge to the fields. Left-aligned
     * labels of different lengths are the main reason a form of this shape looks unfinished.
     */
    private void addPair(JPanel panel, GridBagConstraints gbc, int row, int col, String text,
            java.awt.Component field) {
        gbc.gridy = row;
        gbc.gridx = col;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        final JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        panel.add(label, gbc);
        gbc.gridx = col + 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        panel.add(field, gbc);
        gbc.weightx = 0;
    }

    /**
     * The platoon table.
     *
     * Column widths are set rather than left to divide evenly: eight equal columns truncated the
     * "Weapons" header while leaving the one-character Lyr column absurdly wide. Lyr, After and Lost
     * are centred because they hold a marker rather than a quantity; the four editable numbers keep
     * the default right alignment, which is what makes them scannable as a column.
     */
    private JPanel buildPlatoonTable() {
        // Right-click Copy / Export to CSV, as the old window had on both its tables (T-431). Only
        // the platoon table here: the roster is a JTree, and the armies are already covered by the
        // toolbar's Copy, which exports them WITH their platoons.
        gui.services.TableExportMenu.install(platoons, "battlesim-platoons");
        platoons.setFillsViewportHeight(true);
        platoons.setRowHeight(Math.max(20, platoons.getRowHeight()));
        // Spreads the slack when the columns fit; the tracksViewportWidth override on the field
        // is what stops it squeezing when they do not.
        platoons.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        platoons.getTableHeader().setReorderingAllowed(false);
        platoons.setShowGrid(false);
        platoons.setIntercellSpacing(new Dimension(0, 1));

        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        buttons.add(button("BATTLESIM.PLATOON.ADD", "addPlatoon"));
        buttons.add(tooltipped(button("BATTLESIM.PLATOON.CLONE", "clonePlatoon"),
                "BATTLESIM.PLATOON.CLONE.HINT"));
        buttons.add(button("BATTLESIM.PLATOON.REMOVE", "removePlatoon"));

        // The row order is the casualty sequence - but NOT always, and the label says which.
        // Above the table rather than below it, because it governs how the rows are to be read and
        // a caption underneath arrives too late.
        casualtyMode.setFont(casualtyMode.getFont().deriveFont(Font.PLAIN));
        casualtyMode.setBorder(BorderFactory.createEmptyBorder(1, 4, 4, 4));

        final JPanel ret = new JPanel(new BorderLayout());
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.PLATOON.TITLE")));
        ret.add(casualtyMode, BorderLayout.NORTH);
        ret.add(new JScrollPane(platoons), BorderLayout.CENTER);
        ret.add(buttons, BorderLayout.SOUTH);
        return ret;
    }

    /**
     * Swaps in the selected army's platoons and re-arms the listener that makes an edit count.
     *
     * Without the listener the platoon table was the one editor whose changes stopped at its own
     * row: zero every platoon of an army and the roster still badged it as fighting, the group
     * totals still counted its troops, and Run still blamed the missing engine rather than the hex
     * having no combat left in it. {@link #doRefresh}'s own javadoc names retyping a platoon as the
     * example of an edit that moves something else.
     */
    private void setPlatoonModel() {
        final javax.swing.table.TableModel model = controler.getPlatoonModel();
        model.addTableModelListener(platoonEdits);
        platoons.setModel(model);
        configurePlatoonColumns();
    }

    /** Applied after every model swap, because a new model discards the column settings. */
    private void configurePlatoonColumns() {
        if (platoons.getColumnCount() < 10) {
            return;
        }
        // 10 columns normally, 13 for an army that floats (T-428) - so widths are applied up to
        // whatever the model actually has rather than to a fixed count.
        // Wide enough for the HEADER, not just the value - a truncated header is what sent the
        // player looking for this in the first place. The first ten sum to 640px so a land army
        // fits the pane at the default window size and never scrolls; a fleet adds 182px of
        // transport columns and does, which is the case that actually needs the bar.
        final int[] widths = {34, 130, 56, 68, 70, 58, 60, 60, 54, 50, 72, 58, 52};
        for (int ii = 0; ii < Math.min(widths.length, platoons.getColumnCount()); ii++) {
            // BOTH: preferred is what the fit test reads, width is what is on screen right now.
            // Setting only preferred would leave columns at whatever the last auto-resize spread
            // them to, so a table that once scrolled would keep its stretched columns afterwards.
            platoons.getColumnModel().getColumn(ii).setPreferredWidth(widths[ii]);
            platoons.getColumnModel().getColumn(ii).setWidth(widths[ii]);
        }
        for (int ii : new int[]{0, 8, 9}) {
            platoons.getColumnModel().getColumn(ii).setCellRenderer(centredCell);
        }
        // the troop type is an object, not a name: it has to be pickable, and the same renderer
        // that keeps nations out of BaseModel.toString() keeps troop types out of it too
        platoons.getColumnModel().getColumn(1).setCellEditor(troopTypeEditor);
        platoons.getColumnModel().getColumn(1).setCellRenderer(troopTypeCell);
    }

    private static DefaultTableCellRenderer centredRenderer() {
        final DefaultTableCellRenderer ret = new DefaultTableCellRenderer();
        ret.setHorizontalAlignment(SwingConstants.CENTER);
        return ret;
    }

    private static DefaultTableCellRenderer troopTypeRenderer() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setText(value instanceof BaseModel ? ((BaseModel) value).getNome() : "");
            }
        };
    }

    /**
     * Two facts, at opposite ends: how the scenario decided who fights whom, and why Run is off.
     *
     * Separated because they answer different questions and were previously run together in one
     * string, where the second sentence disappeared into the first. Run's reason sits at the edge
     * nearest the button it is about.
     */
    private JPanel buildStatusBar() {
        final JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 5, 8));
        for (JLabel one : new JLabel[]{status, runReason}) {
            one.setFont(one.getFont().deriveFont(Font.PLAIN));
        }
        runReason.setHorizontalAlignment(SwingConstants.TRAILING);
        bar.add(status, BorderLayout.CENTER);
        bar.add(runReason, BorderLayout.LINE_END);

        final JPanel ret = new JPanel(new BorderLayout());
        ret.add(new JSeparator(), BorderLayout.NORTH);
        ret.add(bar, BorderLayout.CENTER);
        return ret;
    }

    // ------------------------------------------------------------------ refresh

    /**
     * Repaints everything from the scenario.
     *
     * Wholesale rather than targeted, because almost every edit can move something else: retyping a
     * platoon changes a group's troop total, changing a nation can move an army between groups, and
     * a diplomacy edit can move several at once. Working out which widgets a given edit touches is
     * how a view goes quietly stale, and the whole thing is a few dozen rows.
     */
    private void doRefresh() {
        doRefresh(true);
    }

    /**
     * Refreshes after an EDIT, which also throws the last run away.
     *
     * A result describes the battle as it was set up when Run was pressed. Change a tactic, a
     * quantity or a diplomacy cell and it describes something else, so After and Lost go back to
     * "--" rather than keeping casualties from a fight the player has since edited away. Leaving
     * them on screen would be the worst kind of wrong: numbers that look live and are not.
     *
     * Every path that mutates the scenario comes through here; {@link #doRefresh} is for the two
     * that do not - building the window, and showing the run itself.
     */
    private void doEdited() {
        doEdited(true);
    }

    /** @param includePlatoons false when the platoon table is the SOURCE of the edit. */
    private void doEdited(boolean includePlatoons) {
        controler.clearResult();
        doRefresh(includePlatoons);
    }

    /**
     * @param includePlatoons false when the platoon table is the SOURCE of the edit.
     *
     * Replacing a JTable's model clears its row selection and drops any editor that has opened
     * since, so refreshing the platoon table in response to a platoon edit knocked the player out
     * of the row he was typing in: enter a quantity, press Tab, and the selection vanished before
     * the next cell could take it. He would have had to re-click for every field of every platoon,
     * on the exact screen T-420 built for entering a composition by hand.
     *
     * Nothing is lost by skipping it. The selected army has not changed, and the model has already
     * fired its own row update - the platoon table is the one pane that is ALREADY current when
     * this path runs. Everything downstream of the edit (the roster badges and group totals, the
     * editor's "Fights in" line, the city line, the assumption count, Run's reason) still moves.
     */
    private void doRefresh(boolean includePlatoons) {
        refreshing = true;
        try {
            roster.setModel(controler.getRosterModel());
            for (int ii = 0; ii < roster.getRowCount(); ii++) {
                roster.expandRow(ii);
            }
            reselectInRoster();
            if (includePlatoons) {
                setPlatoonModel();
            }
            refreshEditor();
            refreshGround();
            final int nacoes = controler.getScenario().getNacoes().size();
            diplomacy.setEnabled(nacoes > 1);
            diplomacy.setToolTipText(nacoes > 1 ? null
                    : labels.getString("BATTLESIM.DIPLOMACY.EMPTY"));
            status.setText(BattleSimConverter.getDerivationText(controler.getScenario()));
            // R-40: the button's state and the sentence beside it come from the SAME gate, so
            // they cannot drift into saying different things.
            run.setEnabled(BattleSimConverter.isRunnable(controler.getScenario()));
            // One label, two jobs, and they can never both apply: before a run it says why Run is
            // off (empty when it is on), after one it says what the run did and what it could not
            // do. Both belong at the edge nearest the button they are about.
            runReason.setText(controler.getLastResult() == null
                    ? BattleSimConverter.getRunDisabledReason(controler.getScenario())
                    : BattleSimConverter.getRunResultText(controler.getLastResult()));
        } finally {
            refreshing = false;
        }
    }

    /**
     * Puts the selection highlight back on the army the editor is showing.
     *
     * The roster is rebuilt whole on every refresh, and a rebuilt JTree has nothing selected - so
     * editing any field silently cleared the highlight and left the player unable to see which army
     * the form belonged to. Matched by army identity rather than by row, because an edit can move
     * an army to a different group.
     */
    private void reselectInRoster() {
        final ArmySim wanted = controler.getSelected();
        if (wanted == null) {
            return;
        }
        for (int row = 0; row < roster.getRowCount(); row++) {
            final Object node = roster.getPathForRow(row).getLastPathComponent();
            if (!(node instanceof DefaultMutableTreeNode)) {
                continue;
            }
            final Object user = ((DefaultMutableTreeNode) node).getUserObject();
            if (user instanceof BattleSimControler.ArmyNode
                    && ((BattleSimControler.ArmyNode) user).getArmy() == wanted) {
                roster.setSelectionRow(row);
                return;
            }
        }
    }

    private void refreshEditor() {
        final ArmySim army = controler.getSelected();
        final boolean any = army != null;
        for (java.awt.Component one : new java.awt.Component[]{nacao, tactic, commander, morale,
            attackBonus, defenseBonus, combatLevel, target}) {
            one.setEnabled(any);
        }
        if (!any) {
            armyTitle.setText("");
            casualtyMode.setText("");
            strength.setText("");
            sizeBand.setText("");
            fightsIn.setText("");
            source.setText("");
            return;
        }
        armyTitle.setText(army.getNome());
        nacao.setSelectedItem(army.getNacao());
        target.setSelectedItem(army.getTargetNacao() == null
                ? labels.getString("BATTLESIM.TARGET.ALL") : army.getTargetNacao());
        combatLevel.setSelectedItem(army.getCombatLevel());
        tactic.setSelectedIndex(indexOfTactic(army.getTatica()));
        commander.setValue(army.getComandantePericia());
        morale.setValue(army.getMoral());
        attackBonus.setValue(army.getAttackBonus());
        defenseBonus.setValue(army.getArmyDefenseBonus());
        casualtyMode.setText(BattleSimConverter.getCasualtyModeText(army,
                control.facade.WorldFacadeCounselor.getInstance().getCenario()));
        strength.setText(BattleSimConverter.getArmyStrength(army));
        sizeBand.setText(BattleSimConverter.getSizeBandText(army));
        fightsIn.setText(BattleSimConverter.getFightsIn(controler.getParticipation(army)));
        source.setText(String.format(labels.getString("BATTLESIM.SOURCE"),
                BattleSimConverter.getProvenanceName(
                        controler.getScenario().getProvenance(army))));
    }

    private void refreshGround() {
        final CombatScenario scenario = controler.getScenario();
        terreno.setSelectedItem(scenario.getTerreno());

        final Cidade city = scenario.getCidade();
        cityParticipates.setEnabled(city != null);
        cityParticipates.setSelected(scenario.isCityParticipates());
        final boolean editable = scenario.getCidadeAtiva() != null;
        for (java.awt.Component one : new java.awt.Component[]{cityOwner, citySize,
            cityFortification, cityLoyalty}) {
            one.setEnabled(editable);
        }
        if (city != null) {
            cityOwner.setSelectedItem(city.getNacao());
            citySize.setSelectedIndex(clampIndex(city.getTamanho(), citySize.getItemCount()));
            cityFortification.setSelectedIndex(
                    clampIndex(city.getFortificacao(), cityFortification.getItemCount()));
            cityLoyalty.setValue(city.getLealdade());
        }
        cityText.setText(BattleSimConverter.getCityText(scenario));
    }

    /**
     * The six size or fortification steps, named.
     *
     * Index IS the value - "Ruins" is 0, "Metropolis" is 5 - which is why these are built by
     * counting rather than from a map: the combo's selected index is what the model wants.
     */
    private Object[] cityLevels(boolean size) {
        final business.facade.CidadeFacade facade = new business.facade.CidadeFacade();
        final Object[] ret = new Object[6];
        for (int ii = 0; ii < ret.length; ii++) {
            ret[ii] = size ? facade.getTamanhoNome(ii) : facade.getFortificacaoNome(ii);
        }
        return ret;
    }

    private static int clampIndex(int value, int count) {
        return Math.max(0, Math.min(count - 1, value));
    }

    /** Every nation in the world, by name. See {@link #buildComboModels} for why not just these. */
    private Object[] nacoes() {
        final java.util.List<Nacao> ret = new java.util.ArrayList<>(
                control.facade.WorldFacadeCounselor.getInstance().getNacoes().values());
        java.util.Collections.sort(ret, new java.util.Comparator<Nacao>() {
            @Override
            public int compare(Nacao one, Nacao other) {
                return String.valueOf(one.getNome()).compareToIgnoreCase(
                        String.valueOf(other.getNome()));
            }
        });
        return ret.toArray();
    }

    /**
     * The tactic combo carries {@code GenericoComboObject}s whose id is a two-letter CODE, not a
     * number, so the translation is {@link ConverterFactory}'s and never {@code parseInt}'s.
     *
     * This was wrong and it corrupted armies. The ids come from {@code BaseMsgs.taticasGb} and read
     * {@code "ca"}, {@code "fl"}, {@code "pa"}, {@code "ce"}, {@code "gu"}, {@code "em"};
     * {@code SysApoio.parseInt} returns its error sentinel {@code -9999} for every one of them. So
     * selecting an army - not editing it, merely selecting it - wrote {@code setTatica(-9999)}, and
     * {@link #indexOfTactic} comparing {@code -9999} against a real tactic never matched, so the
     * combo always displayed the FIRST entry regardless of what the army was actually doing.
     * {@code TitleFactory.getTaticaNome} then swallows the out-of-range index and answers "Padrao",
     * which is exactly the silent plausible default that hides a bug for years.
     *
     * {@code ConverterFactory.taticaToInt}/{@code taticaToCodigo} are the shared pair that already
     * knows this mapping, including the four naval tactics the GB list does not carry.
     */
    private int selectedTactic() {
        final Object chosen = tactic.getSelectedItem();
        if (chosen instanceof GenericoComboObject) {
            return ConverterFactory.taticaToInt(((GenericoComboObject) chosen).getComboId());
        }
        return 0;
    }

    private int indexOfTactic(int tatica) {
        final String wanted = ConverterFactory.taticaToCodigo(tatica);
        for (int ii = 0; ii < tactic.getItemCount(); ii++) {
            final Object one = tactic.getItemAt(ii);
            if (one instanceof GenericoComboObject
                    && wanted.equalsIgnoreCase(((GenericoComboObject) one).getComboId())) {
                return ii;
            }
        }
        return 0;
    }

    private Object[] terrenos() {
        final java.util.List<Object> ret = new java.util.ArrayList<>(
                control.facade.WorldFacadeCounselor.getInstance().getCenario().getTerrenos().values());
        return ret.toArray();
    }

    // ------------------------------------------------------------------ events

    /**
     * Roster selection. Guarded like every other repopulation, which it was NOT.
     *
     * It checked {@code refreshing} on the way in but never raised it, and then called
     * {@link #refreshEditor}, whose whole job is to push the army's values into eight widgets.
     * {@code JComboBox.setSelectedItem} fires its action unconditionally, so all four combo
     * handlers and all four spinner handlers ran as though the PLAYER had just edited them: eight
     * write-backs and eight full window refreshes for one click on a tree node. Seven wrote a value
     * back over itself; the eighth wrote the tactic, and with the {@code parseInt} bug above that
     * meant merely CLICKING an army set its tactic to -9999.
     */
    @Override
    public void valueChanged(TreeSelectionEvent event) {
        if (refreshing) {
            return;
        }
        final Object node = roster.getLastSelectedPathComponent();
        if (!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        final Object user = ((DefaultMutableTreeNode) node).getUserObject();
        if (user instanceof BattleSimControler.ArmyNode) {
            refreshing = true;
            try {
                controler.setSelected(((BattleSimControler.ArmyNode) user).getArmy());
                setPlatoonModel();
                refreshEditor();
            } finally {
                refreshing = false;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (refreshing) {
            return;
        }
        final String command = event.getActionCommand();
        if ("addArmy".equals(command)) {
            controler.doAddArmy();
        } else if ("cloneArmy".equals(command)) {
            controler.doCloneArmy();
        } else if ("removeArmy".equals(command)) {
            controler.doRemoveArmy();
        } else if ("diplomacy".equals(command)) {
            // The dialog edits the scenario directly and refreshes this window as it goes, so there
            // is nothing to do here afterwards - and nothing to undo if he closes it, because
            // "Reset to derived" is the undo and it lives in the dialog.
            //
            // It is NON-MODAL (John wants to read the Nations tab while editing), so it is held in
            // a field: a second click must raise the one that is open rather than stack another
            // dialog over it, each with its own copy of the grid.
            if (diplomacyDialog == null || !diplomacyDialog.isDisplayable()) {
                diplomacyDialog = new BattleSimDiplomacyDialog(this, controler, new Runnable() {
                    @Override
                    public void run() {
                        doEdited();
                    }
                });
            }
            diplomacyDialog.setVisible(true);
            diplomacyDialog.toFront();
            return;
        } else if ("addPlatoon".equals(command)) {
            if (controler.doAddPlatoon() == null) {
                return;     // nothing selected, or every troop type already present
            }
        } else if ("tactics".equals(command)) {
            control.support.WindowPopupText.showWindowTable(
                    control.services.CenarioConverter.getInstance().getTaticaTableModel(),
                    labels.getString("BATTLESIM.TATICA.HINT"), this);
            return;
        } else if ("casualties".equals(command)) {
            new TroopsCasualtiesList(controler.getScenario().getTerreno()).setVisible(true);
            return;
        } else if ("copy".equals(command)) {
            // ClipboardHelper, not SysApoio directly: it shows a toast if the clipboard refuses,
            // which a silent no-op would not.
            gui.services.ClipboardHelper.copy(
                    BattleSimConverter.getClipboardText(controler.getScenario()));
            control.support.DispatchManager.getInstance().sendDispatchForMsg(
                    control.support.DispatchManager.STATUS_BAR_MSG,
                    labels.getString("COPIAR.ARMY.DETAILS"));
            return;
        } else if ("run".equals(command)) {
            // The scenario is not touched: LandCombatResolver fights with copies, so this is a
            // question the player can ask again after changing a tactic. doRefresh() - NOT
            // doEdited() - because the result must survive the repaint that shows it.
            controler.doRun();
            // doRefresh(false) plus a row update rather than the full refresh: After and Lost are
            // the only cells that changed, and swapping the table's model would clear the row the
            // player had selected to watch.
            doRefresh(false);
            // GUARDED, and it has to be: platoonEdits listens for UPDATE events and treats one as a
            // player edit, which would clear the very result this is repainting. The guard is read
            // synchronously inside the fire, so raising it here stops the listener before it can
            // schedule anything.
            refreshing = true;
            try {
                if (platoons.getModel() instanceof javax.swing.table.AbstractTableModel
                        && platoons.getRowCount() > 0) {
                    ((javax.swing.table.AbstractTableModel) platoons.getModel())
                            .fireTableRowsUpdated(0, platoons.getRowCount() - 1);
                }
            } finally {
                refreshing = false;
            }
            return;
        } else if ("about".equals(command)) {
            control.support.WindowPopupText.showWindowText(
                    labels.getString("BATTLESIM.DISCLAIMER.NEW.TEXT"),
                    labels.getString("BATTLESIM.DISCLAIMER.NEW.TITLE"), this);
            return;
        } else if ("clonePlatoon".equals(command)) {
            final int cloneRow = platoons.getSelectedRow();
            if (cloneRow < 0
                    || !(platoons.getModel() instanceof BattleSimControler.PlatoonTableModel)) {
                return;
            }
            if (controler.doClonePlatoon(((BattleSimControler.PlatoonTableModel)
                    platoons.getModel()).getPlatoon(cloneRow)) == null) {
                return;     // nothing selected, or every troop type already present
            }
        } else if ("removePlatoon".equals(command)) {
            final int row = platoons.getSelectedRow();
            if (row < 0 || !(platoons.getModel() instanceof BattleSimControler.PlatoonTableModel)) {
                return;
            }
            controler.doRemovePlatoon(((BattleSimControler.PlatoonTableModel) platoons.getModel())
                    .getPlatoon(row));
        } else if ("terreno".equals(command)) {
            controler.setTerreno((Terreno) terreno.getSelectedItem());
        } else if ("city".equals(command)) {
            controler.setCityParticipates(cityParticipates.isSelected());
        } else if ("cityOwner".equals(command)) {
            controler.setCityOwner((Nacao) cityOwner.getSelectedItem());
        } else if ("citySize".equals(command)) {
            controler.setCityTamanho(citySize.getSelectedIndex());
        } else if ("cityFort".equals(command)) {
            controler.setCityFortificacao(cityFortification.getSelectedIndex());
        } else if ("level".equals(command)) {
            controler.setCombatLevel((CombatLevel) combatLevel.getSelectedItem());
        } else if ("target".equals(command)) {
            final Object chosen = target.getSelectedItem();
            controler.setTargetNacao(chosen instanceof Nacao ? (Nacao) chosen : null);
        } else if ("tactic".equals(command) && controler.getSelected() != null) {
            controler.getSelected().setTatica(selectedTactic());
        } else if ("nacao".equals(command) && controler.getSelected() != null) {
            controler.getSelected().setNacao((Nacao) nacao.getSelectedItem());
        } else {
            return;
        }
        doEdited();
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (refreshing) {
            return;
        }
        if (event.getSource() == cityLoyalty) {
            controler.setCityLealdade((Integer) cityLoyalty.getValue());
            doEdited();
            return;
        }
        final ArmySim army = controler.getSelected();
        if (army == null) {
            return;
        }
        final Object src = event.getSource();
        if (src == commander) {
            army.setComandante((Integer) commander.getValue());
        } else if (src == morale) {
            army.setMoral((Integer) morale.getValue());
        } else if (src == attackBonus) {
            army.setBonusAttack((Integer) attackBonus.getValue());
        } else if (src == defenseBonus) {
            army.setBonusDefense((Integer) defenseBonus.getValue());
        } else {
            return;
        }
        doEdited();
    }
}
