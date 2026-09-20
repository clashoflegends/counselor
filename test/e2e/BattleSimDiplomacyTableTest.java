package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.BattleSimControler;
import model.Local;
import model.Nacao;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The diplomacy grid, over the nation table T-418 introduced.
 *
 * John: "For the BattleSim, the matrix is the law and canonical." So this grid is the simulator's
 * only input to who fights whom, and the cases below are the ones where a plausible-looking
 * implementation would be quietly wrong.
 *
 * The one that drove the rewrite is {@link #anEditAppliesToEveryArmyOfThatFaction}. Overrides used
 * to be keyed by army PAIR, so "he declares war on me" was a statement about two ArmySim objects:
 * it had to be repeated for every army he had on the hex, and any army added afterwards escaped it
 * entirely. Diplomacy is a property of factions, and the Judge reads it that way - army hostility is
 * nothing but {@code getNacaoControl().isInimigo(...)}.
 */
class BattleSimDiplomacyTableTest {

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    private static Local hex() {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno);
        return ret;
    }

    /** The army needs troops, or LayerParticipation answers NO_TROOPS before diplomacy is asked. */
    private static model.Pelotao platoon(int qtd) {
        final model.TipoTropa tipo = new model.TipoTropa();
        tipo.setCodigo("inf");
        tipo.setNome("inf");
        final model.Pelotao ret = new model.Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao) {
        final ArmySim ret = new ArmySim(nome, hex().getTerreno(), nacao);
        ret.setCodigo(nome);
        return ret;
    }

    private static BattleSimControler.DiplomacyTableModel modelOver(CombatScenario scenario) {
        return new BattleSimControler.DiplomacyTableModel(scenario);
    }

    /** Column {@code ii + 1} is nation {@code ii}: one leading column holds the row's own name. */
    @Test
    void theGridIsSquareWithOneExtraColumnForTheRowName() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("mine", nacao("m", "Mine")), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", nacao("t", "Theirs")), CombatScenario.Provenance.ESTIMATED);

        final BattleSimControler.DiplomacyTableModel model = modelOver(scenario);

        assertEquals(2, model.getRowCount());
        assertEquals(3, model.getColumnCount());
        assertEquals("Mine", ((Nacao) model.getValueAt(0, 0)).getNome());
        assertEquals("Theirs", model.getColumnName(2));
    }

    /**
     * The diagonal is a faction's view of itself, which the model fixes at neutral and will not let
     * anyone set - matching {@code Nacao.getRelacionamento}, which short-circuits on self.
     */
    @Test
    void aFactionCannotBeSetAgainstItself() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("mine", nacao("m", "Mine")), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", nacao("t", "Theirs")), CombatScenario.Provenance.ESTIMATED);
        final BattleSimControler.DiplomacyTableModel model = modelOver(scenario);

        assertNull(model.getValueAt(0, 1), "row 0 against nation 0 is the diagonal");
        assertFalse(model.isCellEditable(0, 1));
        assertTrue(model.isCellEditable(0, 2), "and every other cell is the player's to set");
    }

    /** Typing a declaration of war makes the pair fight, and says the player is the one who said so. */
    @Test
    void declaringWarInTheGridMakesThePairFight() {
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim ours = army("ours", mine), them = army("them", theirs);
        scenario.addArmy(ours, CombatScenario.Provenance.EXACT);
        scenario.addArmy(them, CombatScenario.Provenance.ESTIMATED);
        final BattleSimControler.DiplomacyTableModel model = modelOver(scenario);
        assertFalse(scenario.getMatrix().isInimigo(ours, them), "nothing said, so nothing fights");

        model.setValueAt(RelationshipMatrix.SWORN_ENEMY, 0, 2);

        assertTrue(scenario.getMatrix().isInimigo(ours, them));
        assertEquals(RelationshipMatrix.Origin.PLAYER_EDITED, model.getOrigin(0, 2));
        assertEquals(1, scenario.getEditedCount());
    }

    /**
     * ONE side declaring is enough, and the grid shows the consequence on both halves.
     *
     * The Judge's rule: {@code CombatArmy.isCombatCleared()} tests one direction and then writes
     * BOTH enemy lists. So the mirrored cell still reads "Peace pact" - nobody has claimed the other
     * faction changed its mind - while the pair is nonetheless hostile.
     */
    @Test
    void oneSidedWarIsStillAWarAndTheMirrorCellKeepsItsOwnValue() {
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("ours", mine), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("them", theirs), CombatScenario.Provenance.ESTIMATED);
        final BattleSimControler.DiplomacyTableModel model = modelOver(scenario);

        model.setValueAt(RelationshipMatrix.SWORN_ENEMY, 0, 2);

        assertTrue(model.isHostile(0, 2));
        assertTrue(model.isHostile(1, 1), "the pair fights, seen from either side");
        assertEquals(RelationshipMatrix.NEUTRAL, model.getValueAt(1, 1),
                "but his view was never stated and must not be invented");
    }

    /**
     * The rewrite's whole point: an edit is about FACTIONS, so every army of that faction inherits
     * it - including one added after the edit was made.
     */
    @Test
    void anEditAppliesToEveryArmyOfThatFaction() {
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim ours = army("ours", mine), first = army("first", theirs);
        scenario.addArmy(ours, CombatScenario.Provenance.EXACT);
        scenario.addArmy(first, CombatScenario.Provenance.ESTIMATED);

        modelOver(scenario).setValueAt(RelationshipMatrix.SWORN_ENEMY, 0, 2);

        final ArmySim reinforcement = army("second", theirs);
        scenario.addArmy(reinforcement, CombatScenario.Provenance.ESTIMATED);

        assertTrue(scenario.getMatrix().isInimigo(ours, first));
        assertTrue(scenario.getMatrix().isInimigo(ours, reinforcement),
                "an army that arrives after the declaration is still covered by it");
        assertEquals(1, scenario.getEditedCount(), "and it is ONE declaration, not one per army");
    }

    /** Reset is the undo: back to what the EGF and the game type say. */
    @Test
    void resetDiscardsEveryOverride() {
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim ours = army("ours", mine), them = army("them", theirs);
        scenario.addArmy(ours, CombatScenario.Provenance.EXACT);
        scenario.addArmy(them, CombatScenario.Provenance.ESTIMATED);
        modelOver(scenario).setValueAt(RelationshipMatrix.SWORN_ENEMY, 0, 2);
        assertTrue(scenario.getMatrix().isInimigo(ours, them));

        scenario.clearHostilityEdits();

        assertFalse(scenario.getMatrix().isInimigo(ours, them));
        assertEquals(0, scenario.getEditedCount());
    }

    /**
     * The city's owner gets a row even with no army of its own.
     *
     * It is a combat participant (T-417) and whether an army assaults the city is decided by this
     * same table, so leaving it out would hide the one row that answers "why is nobody attacking
     * this city?".
     */
    @Test
    void theCityOwnerHasARowWithoutOwningAnArmy() {
        final Nacao mine = nacao("m", "Mine"), seagard = nacao("s", "Seagard");
        final Local hex = hex();
        final model.Cidade cidade = new model.Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("Seagard");
        cidade.setNacao(seagard);
        cidade.setTamanho(3);
        hex.setCidade(cidade);

        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.addArmy(army("ours", mine), CombatScenario.Provenance.EXACT);

        final BattleSimControler.DiplomacyTableModel model = modelOver(scenario);

        assertEquals(2, model.getRowCount(), "the attacker and the city's owner");
        assertTrue(model.isCellEditable(0, 2), "and the assault is the player's to declare");
    }

    /**
     * Declaring war on the city's owner actually puts the army in the CITY layer.
     *
     * It did not. getParticipation asked the deriver for the city answer separately, and that call
     * re-derives from the EGF and the game type WITHOUT the player's overrides - so the grid turned
     * red, hasCombat() agreed, and the city layer went on reporting NOT_HOSTILE_TO_CITY with a
     * reason string blaming diplomacy. The army layer honoured his declaration and the city layer
     * silently did not, which is the precise failure this panel exists to let him fix.
     */
    @Test
    void declaringWarOnTheCityOwnerPutsTheArmyInTheCityLayer() {
        final Nacao mine = nacao("m", "Mine"), seagard = nacao("s", "Seagard");
        final Local hex = hex();
        final model.Cidade cidade = new model.Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("Seagard");
        cidade.setNacao(seagard);
        cidade.setTamanho(3);
        hex.setCidade(cidade);

        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim ours = army("ours", mine);
        ours.getPelotoes().put("inf", platoon(600));
        // ordered to storm the walls, so DIPLOMACY is the only thing left deciding it. The default
        // ATTACK_ARMY deliberately stops short of the city, and leaving it would have made this
        // test pass for the wrong reason before the edit and fail for the wrong reason after.
        ours.setCombatLevel(business.combat.CombatLevel.ATTACK_CITY);
        scenario.addArmy(ours, CombatScenario.Provenance.EXACT);
        assertFalse(scenario.getParticipation().get(ours).isIn(business.combat.CombatLayer.CITY),
                "willing to assault, but not at war with the owner");
        assertEquals(business.combat.LayerParticipation.Reason.NOT_HOSTILE_TO_CITY,
                scenario.getParticipation().get(ours).getReason(business.combat.CombatLayer.CITY),
                "and the reason must be the diplomacy, not the order");

        final BattleSimControler.DiplomacyTableModel model = modelOver(scenario);
        final int seagardColumn = model.getNacoes().get(0) == mine ? 2 : 1;
        final int mineRow = model.getNacoes().get(0) == mine ? 0 : 1;
        model.setValueAt(RelationshipMatrix.SWORN_ENEMY, mineRow, seagardColumn);

        assertTrue(scenario.getParticipation().get(ours).isIn(business.combat.CombatLayer.CITY),
                "his declaration must reach the city layer, not only the army layer");
    }
}
