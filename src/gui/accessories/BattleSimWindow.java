package gui.accessories;

import baseLib.BaseModel;
import baseLib.GenericoComboObject;
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
import javax.swing.DefaultListCellRenderer;
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
    private final JLabel cityText = new JLabel();
    private final JSpinner cityLoyalty = spinner(0, 0, 100);
    private final JComboBox<Object> citySize = new JComboBox<>();
    private final JComboBox<Object> cityFortification = new JComboBox<>();
    private final JComboBox<Object> tactic = new JComboBox<>();
    private final JSpinner commander = spinner(0, 0, 100);
    private final JSpinner morale = spinner(0, 0, 100);
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
        final BattleSimCellRenderer renderer = new BattleSimCellRenderer();
        for (JComboBox<?> one : new JComboBox<?>[]{nacao, terreno, combatLevel, target, tactic}) {
            one.setRenderer(renderer);
        }
        setMinimumSize(new Dimension(900, 560));
        pack();
        doRefresh();
    }

    private static JSpinner spinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
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
        ret.setBorder(BorderFactory.createTitledBorder(labels.getString("BATTLESIM.GROUND.TITLE")));

        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 2));
        row.add(new JLabel(labels.getString("TERRENO")));
        terreno.setActionCommand("terreno");
        terreno.addActionListener(this);
        row.add(terreno);
        ret.add(row);

        cityParticipates.setActionCommand("city");
        cityParticipates.addActionListener(this);
        ret.add(cityParticipates);

        // A city is passive: it is attacked, it damages the attackers, and it takes the result.
        // So it gets inputs and a read-out, not orders.
        ret.add(cityRow(labels.getString("TAMANHO"), citySize, "citySize"));
        ret.add(cityRow(labels.getString("FORTIFICACOES"), cityFortification, "cityFort"));
        ret.add(cityRow(labels.getString("LEALDADE"), cityLoyalty, null));
        cityLoyalty.addChangeListener(this);
        cityText.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));
        ret.add(cityText);
        return ret;
    }

    private JPanel cityRow(String text, java.awt.Component field, String command) {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 1));
        ret.add(new JLabel(text));
        if (command != null && field instanceof JComboBox) {
            ((JComboBox<?>) field).setActionCommand(command);
            ((JComboBox<?>) field).addActionListener(this);
        }
        ret.add(field);
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

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        ret.add(fightsIn, gbc);
        gbc.gridy = 6;
        ret.add(source, gbc);

        for (JSpinner one : new JSpinner[]{commander, morale, attackBonus, defenseBonus}) {
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
        tactic.setModel(control.services.CenarioConverter.getInstance().getTaticaComboModel());
        tactic.setSelectedIndex(indexOfTactic(army.getTatica()));
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

        final Cidade city = scenario.getCidade();
        cityParticipates.setEnabled(city != null);
        cityParticipates.setSelected(scenario.isCityParticipates());
        final boolean editable = scenario.getCidadeAtiva() != null;
        for (java.awt.Component one : new java.awt.Component[]{citySize, cityFortification,
            cityLoyalty}) {
            one.setEnabled(editable);
        }
        citySize.setModel(new DefaultComboBoxModel<>(cityLevels(true)));
        cityFortification.setModel(new DefaultComboBoxModel<>(cityLevels(false)));
        if (city != null) {
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

    /**
     * The tactic combo carries {@code GenericoComboObject}s whose id is the tactic number, so the
     * player picks "Flanking" and the model still gets the integer the engine wants.
     */
    private int selectedTactic() {
        final Object chosen = tactic.getSelectedItem();
        if (chosen instanceof GenericoComboObject) {
            return persistenceCommons.SysApoio.parseInt(
                    ((GenericoComboObject) chosen).getComboId());
        }
        return 0;
    }

    private int indexOfTactic(int tatica) {
        for (int ii = 0; ii < tactic.getItemCount(); ii++) {
            final Object one = tactic.getItemAt(ii);
            if (one instanceof GenericoComboObject
                    && persistenceCommons.SysApoio.parseInt(
                            ((GenericoComboObject) one).getComboId()) == tatica) {
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
        doRefresh();
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (refreshing) {
            return;
        }
        if (event.getSource() == cityLoyalty) {
            controler.setCityLealdade((Integer) cityLoyalty.getValue());
            doRefresh();
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
        doRefresh();
    }
}
