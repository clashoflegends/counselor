package control;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.RelationshipMatrix;
import business.combat.ScenarioLoader;
import business.combat.ScenarioRoster;
import business.facade.NacaoFacade;
import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import control.services.BattleSimConverter;
import business.facade.CenarioFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /**
     * The troop types the player may put in an army: the scenario's whole catalogue.
     *
     * Not filtered by race or recruitability, unlike the recruit order's list. This is a what-if
     * tool and the army being described is usually somebody else's, so the question "could I
     * recruit this?" is the wrong one - the right one is "what do I think is standing there?"
     */
    public List<TipoTropa> getTroopCatalogue() {
        final List<TipoTropa> ret = new ArrayList<>(new CenarioFacade()
                .getTipoTropas(WorldFacadeCounselor.getInstance().getCenario()));
        Collections.sort(ret, new Comparator<TipoTropa>() {
            @Override
            public int compare(TipoTropa one, TipoTropa other) {
                return String.valueOf(one.getNome()).compareToIgnoreCase(
                        String.valueOf(other.getNome()));
            }
        });
        return ret;
    }

    /**
     * Adds a platoon of the first troop type the army does not already hold.
     *
     * One platoon per troop type, because {@code Pelotao.getCodigo()} IS its troop type's codigo
     * and the army's map is keyed on it - a second platoon of the same type would silently replace
     * the first. The player retypes it afterwards if he wanted a different one.
     */
    public Pelotao doAddPlatoon() {
        if (selected == null) {
            return null;
        }
        for (TipoTropa tipo : getTroopCatalogue()) {
            if (selected.getPelotoes().containsKey(tipo.getCodigo())) {
                continue;
            }
            final Pelotao ret = new Pelotao();
            ret.setTipoTropa(tipo);
            ret.setQtd(0);
            selected.getPelotoes().put(ret.getCodigo(), ret);
            return ret;
        }
        return null;
    }

    public void doRemovePlatoon(Pelotao pelotao) {
        if (selected != null && pelotao != null) {
            selected.getPelotoes().remove(pelotao.getCodigo());
        }
    }

    /**
     * Retypes a platoon, which REKEYS it: the map is keyed by troop type.
     *
     * Refused when the army already holds that type, because the put would drop the platoon the
     * player was editing. Refusing is the honest answer - the alternative is to merge two platoons
     * behind his back.
     */
    public boolean setPlatoonType(Pelotao pelotao, TipoTropa tipo) {
        if (selected == null || pelotao == null || tipo == null
                || selected.getPelotoes().containsKey(tipo.getCodigo())) {
            return false;
        }
        selected.getPelotoes().remove(pelotao.getCodigo());
        pelotao.setTipoTropa(tipo);
        selected.getPelotoes().put(pelotao.getCodigo(), pelotao);
        scenario.setEdited(pelotao);
        return true;
    }

    /**
     * The diplomacy grid: every nation in the battle as a row AND as a column.
     *
     * Square on purpose. A cell is what the ROW nation thinks of the COLUMN nation, which is how
     * {@code Nacao.getRelacionamento} stores it, and the two halves of a pair genuinely can differ -
     * a vassal and its lord hold opposite values, and a unilateral declaration of war changes only
     * the declarer's row. Collapsing it to a triangle would have to pick one of the two to show and
     * silently discard the other.
     */
    public DiplomacyTableModel getDiplomacyModel() {
        return new DiplomacyTableModel(scenario);
    }

    /**
     * The nation-by-nation table, as a Swing model. T-418.
     *
     * The matrix is re-derived from the scenario after every edit rather than patched in place,
     * because an edit can move more than the cell it was typed into: {@code getRelationships}
     * rebuilds from the EGF and the game type with the overrides laid on top, and the overrides are
     * the only durable state. Rebuilding is a pass over a handful of nations.
     */
    public static class DiplomacyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final transient CombatScenario scenario;
        private final transient List<Nacao> nacoes;
        private transient RelationshipMatrix matrix;

        public DiplomacyTableModel(CombatScenario scenario) {
            this.scenario = scenario;
            this.matrix = scenario == null ? new RelationshipMatrix() : scenario.getRelationships();
            this.nacoes = new ArrayList<>(matrix.getNacoes());
        }

        /** The nations, in table order. Column {@code ii + 1} is {@code getNacoes().get(ii)}. */
        public List<Nacao> getNacoes() {
            return nacoes;
        }

        @Override
        public int getRowCount() {
            return nacoes.size();
        }

        /** One leading column for the row's own name, then one per nation. */
        @Override
        public int getColumnCount() {
            return nacoes.size() + 1;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return labels.getString("BATTLESIM.DIPLOMACY.NATION");
            }
            return String.valueOf(nacoes.get(column - 1).getNome());
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Nacao.class : Integer.class;
        }

        /** The diagonal is a nation's view of itself, which the model fixes at neutral. */
        @Override
        public boolean isCellEditable(int row, int column) {
            return column > 0 && row != column - 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return nacoes.get(row);
            }
            if (row == column - 1) {
                return null;
            }
            return matrix.getValor(nacoes.get(row), nacoes.get(column - 1));
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (!(value instanceof Integer) || !isCellEditable(row, column) || scenario == null) {
                return;
            }
            scenario.setRelacionamento(nacoes.get(row), nacoes.get(column - 1), (Integer) value);
            // one edit can change what the rest of the table reports, so re-read the whole thing
            matrix = scenario.getRelationships();
            fireTableDataChanged();
        }

        /** Where this cell's answer came from, for the renderer. Null on the diagonal. */
        public RelationshipMatrix.Origin getOrigin(int row, int column) {
            if (column <= 0 || row == column - 1) {
                return null;
            }
            return matrix.getOrigin(nacoes.get(row), nacoes.get(column - 1));
        }

        /** Will these two fight? Asked of the PAIR, so it is true on both sides of the diagonal. */
        public boolean isHostile(int row, int column) {
            if (column <= 0 || row == column - 1) {
                return false;
            }
            return matrix.isHostile(nacoes.get(row), nacoes.get(column - 1));
        }
    }

    /** Back to what the EGF and the game type say, discarding every override. */
    public void doResetDiplomacy() {
        scenario.clearHostilityEdits();
    }

    public PlatoonTableModel getPlatoonModel() {
        return new PlatoonTableModel(scenario, selected, this);
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
        private final BattleSimControler owner;
        private final transient ArmySim army;
        private final List<Pelotao> platoons = new ArrayList<>();

        public PlatoonTableModel(CombatScenario scenario, ArmySim army) {
            this(scenario, army, null);
        }

        PlatoonTableModel(CombatScenario scenario, ArmySim army, BattleSimControler owner) {
            this.scenario = scenario;
            this.owner = owner;
            this.army = army;
            doSort();
        }

        /**
         * Puts the platoons in the order they will DIE. The row order is the information.
         *
         * John, 2026-09-19: "one important use of the current battlesim is for players to visualize
         * the sequence of casualties, which they use to select tactics for the army then look at
         * the sequence of the platoons... in some terrains catapults will die before infantry
         * unless guerrilla is selected. Which is important as you want to spend the infantry to win
         * the army combat to use the catapults in the city layer."
         *
         * So this is not decoration and not a convenience sort. It was
         * {@code getPelotoes().values()}, which is a TreeMap keyed by troop-type codigo - in other
         * words ALPHABETICAL, and alphabetical order looks exactly as authoritative as casualty
         * order while answering a completely different question.
         *
         * {@code ComparatorCasualtiesSorter} is the Judge's own, reached through the same
         * {@code ComparatorFactory} call the Judge makes inside
         * {@code ExercitoControlFacade.getTropasTerraSortedCloned}. Sharing is total here, with no
         * recompose gap to work around: the Judge's overload takes a {@code partidaId} and then
         * never uses it - both {@code ComparatorFactory} overloads build the identical
         * {@code new ComparatorCasualtiesSorter(tatica, terreno)}.
         *
         * Ships first and unsorted, because they are a different layer with a different question:
         * the Judge's land sort drops them outright ({@code !isBarcos()}), so ordering them by a
         * land comparator would be inventing an answer.
         */
        private void doSort() {
            platoons.clear();
            if (army == null) {
                return;
            }
            final List<Pelotao> land = new ArrayList<>();
            for (Pelotao pelotao : army.getPelotoes().values()) {
                if (pelotao.getTipoTropa() != null && pelotao.getTipoTropa().isBarcos()) {
                    platoons.add(pelotao);
                } else {
                    land.add(pelotao);
                }
            }
            Terreno terreno = scenario == null ? null : scenario.getTerreno();
            if (terreno == null) {
                terreno = army.getTerreno();
            }
            ComparatorFactory.getComparatorCasualtiesPelotaoSorter(land, army.getTatica(), terreno);
            platoons.addAll(land);
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
            if (column == COL_TROOP) {
                return TipoTropa.class;
            }
            return column >= COL_QTD && column <= COL_ARMOUR ? Integer.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == COL_TROOP || (column >= COL_QTD && column <= COL_ARMOUR);
        }

        @Override
        public Object getValueAt(int row, int column) {
            final Pelotao pelotao = platoons.get(row);
            final TipoTropa tipo = pelotao.getTipoTropa();
            switch (column) {
                case COL_LAYER:
                    return CombatLayer.of(tipo).getBadge();
                case COL_TROOP:
                    return tipo;
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
            if (column == COL_TROOP) {
                // retyping REKEYS the platoon, so the owning army has to do it - see setPlatoonType
                if (owner != null && value instanceof TipoTropa
                        && owner.setPlatoonType(platoons.get(row), (TipoTropa) value)) {
                    // and it MOVES it: the casualty order is by troop type, so a retyped platoon
                    // belongs somewhere else in the sequence. Re-sorted here rather than left to
                    // the next refresh, which would have shown a stale order in the meantime -
                    // and the order is the whole point of this table. Quantity edits do not
                    // re-sort, because the comparator never looks at a quantity.
                    doSort();
                    fireTableDataChanged();
                }
                return;
            }
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

    /**
     * Who holds the city. Editable because the owner is one of the inputs the player may have to
     * supply: 27 of the 207 cities in a live EGF arrive with no visible owner, and the combat
     * formula reads the owner's powers.
     */
    public void setCityOwner(Nacao nacao) {
        if (nacao != null && scenario.getCidade() != null) {
            scenario.getCidade().setNacao(nacao);
        }
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
