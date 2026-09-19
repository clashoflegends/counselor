package control;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.ScenarioLoader;
import business.combat.ScenarioRoster;
import business.facade.NacaoFacade;
import control.facade.WorldFacadeCounselor;
import control.services.BattleSimConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * The BattleSim window's controller: one {@link CombatScenario} and the Swing models over it.
 *
 * <h3>Why this is not BattleSimulatorControlerNew</h3>
 *
 * That controller keeps the scenario's state as loose fields - {@code armiesList}, {@code
 * armySelected}, {@code terreno}, {@code cityClone}, {@code rowIndex} - which is exactly the
 * scattering {@code CombatScenario} was built to replace. Wiring a new window to it would have
 * recreated the problem, and its {@code IBattleSimulator} seam bakes in a JTable roster where the
 * design asks for a tree. So this is a second controller over the same shared model, and the old one
 * is left untouched so the old window keeps working behind the feature flag.
 *
 * <h3>Everything derived is derived on demand</h3>
 *
 * The roster grouping, the participation badges and the status line are recomputed from the scenario
 * whenever the view refreshes, never cached here. The Judge re-evaluates participation between
 * layers, so a held copy would be wrong by construction, and every input to the grouping is
 * something the player can edit in this window.
 */
public class BattleSimControler {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private final CombatScenario scenario;
    private ArmySim selected;

    public BattleSimControler(Local local) {
        final WorldFacadeCounselor world = WorldFacadeCounselor.getInstance();
        this.scenario = new ScenarioLoader().load(world.getPartida(), local,
                world.getJogadorAtivo(), getUnknownCityOwner());
        if (!scenario.getArmies().isEmpty()) {
            this.selected = scenario.getArmies().get(0);
        }
    }

    /**
     * Who to stand in as the owner of a city the player cannot see the owner of.
     *
     * Every city has an owner and the shared combat code assumes one, so the answer is to supply
     * the missing input rather than to branch around the shared method. The Barbarians are the
     * right stand-in: they hold whatever nobody else does, and treating an unknown holder as
     * barbarian is the same assumption the game already makes elsewhere.
     *
     * Finding them is Counselor knowledge, which is why it lives here and not in the loader:
     * {@code isNacaoBarbarian} is owner id 1 plus the name, and only the client has the nation list
     * to scan. Null when this world has no barbarians, which simply leaves the city as it was.
     */
    private Nacao getUnknownCityOwner() {
        final NacaoFacade facade = new NacaoFacade();
        for (Nacao nacao : WorldFacadeCounselor.getInstance().getNacoes().values()) {
            if (facade.isNacaoBarbarian(nacao)) {
                return nacao;
            }
        }
        return null;
    }

    public CombatScenario getScenario() {
        return scenario;
    }

    public ArmySim getSelected() {
        return selected;
    }

    public void setSelected(ArmySim army) {
        this.selected = army;
    }

    public LayerParticipation getParticipation(ArmySim army) {
        return scenario.getParticipation().get(army);
    }

    // ------------------------------------------------------------------ roster

    /**
     * The roster tree: a node per non-empty group, an army leaf under each.
     *
     * Rebuilt whole rather than mutated, because the grouping can change from any edit - retyping an
     * army's nation moves it, and a diplomacy edit can move several at once. A tree that tried to
     * patch itself would need to know which edits can do that, and would eventually be wrong.
     */
    public DefaultTreeModel getRosterModel() {
        final ScenarioRoster roster = ScenarioRoster.of(scenario);
        final Map<ArmySim, LayerParticipation> participation = scenario.getParticipation();
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                labels.getString("BATTLESIM.ARMIES.TITLE"));
        for (ScenarioRoster.Group group : roster.getGroups()) {
            final DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                    BattleSimConverter.getGroupTitle(roster, group));
            for (ArmySim army : roster.getArmies(group)) {
                node.add(new DefaultMutableTreeNode(new ArmyNode(army, participation.get(army))));
            }
            root.add(node);
        }
        return new DefaultTreeModel(root);
    }

    /** A roster leaf. Keeps the army itself, so selection does not have to match on a label. */
    public static class ArmyNode {

        private final ArmySim army;
        private final String text;

        ArmyNode(ArmySim army, LayerParticipation participation) {
            this.army = army;
            this.text = BattleSimConverter.getArmyTitle(army, participation);
        }

        public ArmySim getArmy() {
            return army;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    // ------------------------------------------------------------------ armies

    public void doAddArmy() {
        final ArmySim army = new ArmySim(labels.getString("BATTLESIM.ARMY.NEW"),
                scenario.getTerreno(), firstNacao());
        army.setCodigo("sim" + System.identityHashCode(army));
        scenario.addArmy(army, CombatScenario.Provenance.MANUAL);
        this.selected = army;
    }

    public void doCloneArmy() {
        if (selected == null) {
            return;
        }
        final ArmySim army = new ArmySim(selected);
        army.setCodigo("sim" + System.identityHashCode(army));
        scenario.addArmy(army, scenario.getProvenance(selected));
        this.selected = army;
    }

    public void doRemoveArmy() {
        if (selected == null) {
            return;
        }
        scenario.remArmy(selected);
        this.selected = scenario.getArmies().isEmpty() ? null : scenario.getArmies().get(0);
    }

    private Nacao firstNacao() {
        if (selected != null && selected.getNacao() != null) {
            return selected.getNacao();
        }
        for (ArmySim army : scenario.getArmies()) {
            if (army.getNacao() != null) {
                return army.getNacao();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ platoons

    public PlatoonTableModel getPlatoonModel() {
        return new PlatoonTableModel(scenario, selected);
    }

    /**
     * The platoon table.
     *
     * {@code After} and {@code Lost} are present and always show {@code --}. They are there so the
     * engine fills cells later instead of forcing a table redesign, and they are deliberately not
     * backed by anything.
     */
    public static class PlatoonTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        /** Only the four the player may edit are writable; see the ownership boundary. */
        private static final int COL_LAYER = 0, COL_TROOP = 1, COL_QTD = 2, COL_TRAINING = 3,
                COL_WEAPON = 4, COL_ARMOUR = 5, COL_AFTER = 6, COL_LOST = 7;

        private final CombatScenario scenario;
        private final List<Pelotao> platoons = new ArrayList<>();

        public PlatoonTableModel(CombatScenario scenario, ArmySim army) {
            this.scenario = scenario;
            if (army != null) {
                platoons.addAll(army.getPelotoes().values());
            }
        }

        public Pelotao getPlatoon(int row) {
            return platoons.get(row);
        }

        @Override
        public int getRowCount() {
            return platoons.size();
        }

        @Override
        public int getColumnCount() {
            return 8;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_LAYER:
                    return labels.getString("BATTLESIM.COL.LAYER");
                case COL_TROOP:
                    return labels.getString("TROPA");
                case COL_QTD:
                    // "Qty", not TAMANHO/"Size" - army SIZE is a different thing in this game
                    // (the 1-5 scale in the armies table), and reusing the word here misreads
                    return labels.getString("BATTLESIM.COL.QTD");
                case COL_TRAINING:
                    return labels.getString("TREINO");
                case COL_WEAPON:
                    return labels.getString("ARMA");
                case COL_ARMOUR:
                    return labels.getString("ARMADURA");
                case COL_AFTER:
                    return labels.getString("BATTLESIM.COL.AFTER");
                default:
                    return labels.getString("BATTLESIM.COL.LOST");
            }
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column >= COL_QTD && column <= COL_ARMOUR ? Integer.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= COL_QTD && column <= COL_ARMOUR;
        }

        @Override
        public Object getValueAt(int row, int column) {
            final Pelotao pelotao = platoons.get(row);
            final TipoTropa tipo = pelotao.getTipoTropa();
            switch (column) {
                case COL_LAYER:
                    return CombatLayer.of(tipo).getBadge();
                case COL_TROOP:
                    return tipo == null ? "" : tipo.getNome();
                case COL_QTD:
                    return pelotao.getQtd();
                case COL_TRAINING:
                    return pelotao.getTreino();
                case COL_WEAPON:
                    return pelotao.getModAtaque();
                case COL_ARMOUR:
                    return pelotao.getModDefesa();
                default:
                    return "--";
            }
        }

        /**
         * Writing a cell writes the SIMULATOR's platoon, which the scenario owns outright, and marks
         * it as the player's own number. Both halves matter: the first is why this cannot reach the
         * loaded world, the second is why the value stops being reported as an outside estimate.
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
            if (!(value instanceof Integer)) {
                return;
            }
            final Pelotao pelotao = platoons.get(row);
            final int qt = Math.max(0, (Integer) value);
            switch (column) {
                case COL_QTD:
                    pelotao.setQtd(qt);
                    break;
                case COL_TRAINING:
                    pelotao.setTreino(qt);
                    break;
                case COL_WEAPON:
                    pelotao.setModAtaque(qt);
                    break;
                case COL_ARMOUR:
                    pelotao.setModDefesa(qt);
                    break;
                default:
                    return;
            }
            scenario.setEdited(pelotao);
            fireTableRowsUpdated(row, row);
        }
    }

    // ------------------------------------------------------------------ army edits

    public void setCombatLevel(CombatLevel level) {
        if (selected != null) {
            selected.setCombatLevel(level);
        }
    }

    public void setTargetNacao(Nacao nacao) {
        if (selected != null) {
            selected.setTargetNacao(nacao);
        }
    }

    public void setTerreno(Terreno terreno) {
        scenario.setTerreno(terreno);
    }

    public void setCityParticipates(boolean participates) {
        scenario.setCityParticipates(participates);
    }

    public void setCityLealdade(int lealdade) {
        scenario.setCityLealdade(lealdade);
    }

    public void setCityTamanho(int tamanho) {
        scenario.setCityTamanho(tamanho);
    }

    public void setCityFortificacao(int fortificacao) {
        scenario.setCityFortificacao(fortificacao);
    }
}
