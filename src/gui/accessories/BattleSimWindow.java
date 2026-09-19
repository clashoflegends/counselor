package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
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

    private final JTree roster = new JTree();
    private final JTable platoons = new JTable();
    private final JLabel status = new JLabel();
    private final JLabel armyTitle = new JLabel();
    private final JLabel fightsIn = new JLabel();
    private final JLabel source = new JLabel();
    private final JButton run = new JButton(labels.getString("BATTLESIM.RUN.SIMULATION"));

    private final JComboBox<Object> nacao = new JComboBox<>();
    private final JComboBox<Object> terreno = new JComboBox<>();
    private final JComboBox<CombatLevel> combatLevel = new JComboBox<>(CombatLevel.values());
    private final JComboBox<Object> target = new JComboBox<>();
    private final JCheckBox cityParticipates =
            new JCheckBox(labels.getString("BATTLESIM.CITY.PARTICIPATES"));
    private final JSpinner commander = spinner(0, 0, 100);
    private final JSpinner morale = spinner(0, 0, 100);
    private final JSpinner tactic = spinner(0, 0, 9);
    private final JSpinner attackBonus = spinner(0, -100, 100);
    private final JSpinner defenseBonus = spinner(0, -100, 100);

    /** Guards the listeners while the editor is being repopulated from the model. */
    private boolean refreshing = false;

    public BattleSimWindow(Local local) {
        this.controler = new BattleSimControler(local);
        setTitle(String.format(labels.getString("BATTLESIM.TITLE"),
                local == null ? "" : local.getCoordenadas()));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildPanes(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setMinimumSize(new Dimension(900, 560));
        pack();
        doRefresh();
    }

    private static JSpinner spinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildToolbar() {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 4));
        ret.add(button("BATTLESIM.ARMY.ADD", "addArmy"));
        ret.add(button("BATTLESIM.ARMY.CLONE", "cloneArmy"));
        ret.add(button("BATTLESIM.ARMY.REMOVE", "removeArmy"));
        ret.add(Box.createHorizontalStrut(16));
        run.setActionCommand("run");
        run.addActionListener(this);
        run.setEnabled(false);
        ret.add(run);
        return ret;
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
        right.setResizeWeight(0.0);
        final JSplitPane ret = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), right);
        ret.setResizeWeight(0.3);
        return ret;
    }

    private JPanel buildLeft() {
        roster.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        roster.setRootVisible(false);
        roster.setShowsRootHandles(true);
        roster.addTreeSelectionListener(this);

        final JPanel ret = new JPanel(new BorderLayout());
        ret.add(new JScrollPane(roster), BorderLayout.CENTER);
        ret.add(buildGround(), BorderLayout.SOUTH);
        ret.setPreferredSize(new Dimension(280, 480));
        return ret;
    }

    /** Terrain and the city toggle: the ground both sides are standing on. */
    private JPanel buildGround() {
        final JPanel ret = new JPanel();
        ret.setLayout(new BoxLayout(ret, BoxLayout.Y_AXIS));
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.LOCAL.TITLE")));

        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 2));
        row.add(new JLabel(labels.getString("TERRENO")));
        terreno.setActionCommand("terreno");
        terreno.addActionListener(this);
        row.add(terreno);
        ret.add(row);

        cityParticipates.setActionCommand("city");
        cityParticipates.addActionListener(this);
        ret.add(cityParticipates);
        return ret;
    }

    private JPanel buildArmyEditor() {
        final JPanel ret = new JPanel(new GridBagLayout());
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.ARMY.TITLE")));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        armyTitle.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        ret.add(armyTitle, gbc);
        gbc.gridwidth = 1;

        nacao.setActionCommand("nacao");
        nacao.addActionListener(this);
        addPair(ret, gbc, 1, 0, labels.getString("NACAO"), nacao);
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

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        ret.add(fightsIn, gbc);
        gbc.gridy = 6;
        ret.add(source, gbc);

        for (JSpinner one : new JSpinner[]{tactic, commander, morale, attackBonus, defenseBonus}) {
            one.addChangeListener(this);
        }
        return ret;
    }

    private void addPair(JPanel panel, GridBagConstraints gbc, int row, int col, String text,
            java.awt.Component field) {
        gbc.gridy = row;
        gbc.gridx = col;
        gbc.weightx = 0;
        panel.add(new JLabel(text), gbc);
        gbc.gridx = col + 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        gbc.weightx = 0;
    }

    private JPanel buildPlatoonTable() {
        platoons.setFillsViewportHeight(true);
        final JPanel ret = new JPanel(new BorderLayout());
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.PLATOON.TITLE")));
        ret.add(new JScrollPane(platoons), BorderLayout.CENTER);
        return ret;
    }

    private JPanel buildStatusBar() {
        final JPanel ret = new JPanel(new BorderLayout());
        ret.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        ret.add(status, BorderLayout.CENTER);
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
        refreshing = true;
        try {
            roster.setModel(controler.getRosterModel());
            for (int ii = 0; ii < roster.getRowCount(); ii++) {
                roster.expandRow(ii);
            }
            platoons.setModel(controler.getPlatoonModel());
            refreshEditor();
            refreshGround();
            status.setText(BattleSimConverter.getDerivationText(controler.getScenario())
                    + "   " + BattleSimConverter.getRunDisabledReason(controler.getScenario()));
        } finally {
            refreshing = false;
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
            fightsIn.setText("");
            source.setText("");
            return;
        }
        armyTitle.setText(army.getNome());
        nacao.setModel(new DefaultComboBoxModel<>(nacoes()));
        nacao.setSelectedItem(army.getNacao());
        target.setModel(new DefaultComboBoxModel<>(targets()));
        target.setSelectedItem(army.getTargetNacao() == null
                ? labels.getString("BATTLESIM.TARGET.ALL") : army.getTargetNacao());
        combatLevel.setSelectedItem(army.getCombatLevel());
        tactic.setValue(army.getTatica());
        commander.setValue(army.getComandantePericia());
        morale.setValue(army.getMoral());
        attackBonus.setValue(army.getAttackBonus());
        defenseBonus.setValue(army.getArmyDefenseBonus());
        fightsIn.setText(BattleSimConverter.getFightsIn(controler.getParticipation(army)));
        source.setText(String.format(labels.getString("BATTLESIM.SOURCE"),
                BattleSimConverter.getProvenanceName(
                        controler.getScenario().getProvenance(army))));
    }

    private void refreshGround() {
        final CombatScenario scenario = controler.getScenario();
        terreno.setModel(new DefaultComboBoxModel<>(terrenos()));
        terreno.setSelectedItem(scenario.getTerreno());
        cityParticipates.setEnabled(scenario.getCidade() != null);
        cityParticipates.setSelected(scenario.isCityParticipates());
    }

    /** Nations already present in the scenario. Enough to retype an army, without a world list. */
    private Object[] nacoes() {
        final java.util.List<Object> ret = new java.util.ArrayList<>();
        for (ArmySim army : controler.getScenario().getArmies()) {
            if (army.getNacao() != null && !ret.contains(army.getNacao())) {
                ret.add(army.getNacao());
            }
        }
        return ret.toArray();
    }

    private Object[] targets() {
        final java.util.List<Object> ret = new java.util.ArrayList<>();
        ret.add(labels.getString("BATTLESIM.TARGET.ALL"));
        for (Object one : nacoes()) {
            ret.add(one);
        }
        return ret.toArray();
    }

    private Object[] terrenos() {
        final java.util.List<Object> ret = new java.util.ArrayList<>(
                control.facade.WorldFacadeCounselor.getInstance().getCenario().getTerrenos().values());
        return ret.toArray();
    }

    // ------------------------------------------------------------------ events

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
            controler.setSelected(((BattleSimControler.ArmyNode) user).getArmy());
            platoons.setModel(controler.getPlatoonModel());
            refreshEditor();
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
        } else if ("terreno".equals(command)) {
            controler.setTerreno((Terreno) terreno.getSelectedItem());
        } else if ("city".equals(command)) {
            controler.setCityParticipates(cityParticipates.isSelected());
        } else if ("level".equals(command)) {
            controler.setCombatLevel((CombatLevel) combatLevel.getSelectedItem());
        } else if ("target".equals(command)) {
            final Object chosen = target.getSelectedItem();
            controler.setTargetNacao(chosen instanceof Nacao ? (Nacao) chosen : null);
        } else if ("nacao".equals(command) && controler.getSelected() != null) {
            controler.getSelected().setNacao((Nacao) nacao.getSelectedItem());
        } else {
            return;
        }
        doRefresh();
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        final ArmySim army = controler.getSelected();
        if (refreshing || army == null) {
            return;
        }
        final Object src = event.getSource();
        if (src == tactic) {
            army.setTatica((Integer) tactic.getValue());
        } else if (src == commander) {
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
        doRefresh();
    }
}
