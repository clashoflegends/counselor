package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import control.BattleSimControler;
import model.Exercito;
import model.Habilidade;
import model.Local;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The new BattleSim window's platoon table, over the model the old one never had.
 *
 * The case worth pinning is the one that shipped broken for years and was fixed in 2.928: editing a
 * platoon in the simulator used to edit the real army loaded from the EGF, everywhere in the
 * Counselor, for the rest of the session. The fix lives in {@code ArmySim}, but the three-pane
 * rebuild is a NEW write path into those platoons, so it gets its own assertion rather than trusting
 * that the old one still covers it.
 */
class BattleSimPlatoonTableTest {

    private static TipoTropa troopType(String codigo, boolean ships) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (ships) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(";TTN;");
            hab.setNome(";TTN;");
            ret.addHabilidade(hab);
        }
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(50);
        return ret;
    }

    private static Terreno terreno() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno());
        return ret;
    }

    /** Every army has a banner - see T-441. The fixture carries one because reality does. */
    private static model.Nacao nacao() {
        final model.Nacao ret = new model.Nacao();
        ret.setCodigo("n");
        ret.setNome("House Tyrell");
        return ret;
    }

    /** A real EGF army, placed on a hex. Codigo before setLocal: the Local keys its index on it. */
    private static Exercito loadedArmy(Pelotao... pelotoes) {
        final Exercito ret = new Exercito();
        ret.setCodigo("a1");
        ret.setNome("Loaded");
        ret.setNacao(nacao());
        ret.setLocal(hex());
        for (Pelotao pelotao : pelotoes) {
            ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    private static BattleSimControler.PlatoonTableModel modelOver(CombatScenario scenario,
            ArmySim army) {
        return new BattleSimControler.PlatoonTableModel(scenario, army);
    }

    @Test
    void editingAQuantityDoesNotTouchTheLoadedArmy() {
        final Pelotao real = platoon(troopType("inf", false), 900);
        final Exercito loaded = loadedArmy(real);
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim simulated = new ArmySim(loaded);
        scenario.addArmy(simulated, CombatScenario.Provenance.ESTIMATED);

        final BattleSimControler.PlatoonTableModel model = modelOver(scenario, simulated);
        assertNotSame(real, model.getPlatoon(0), "the simulator owns its own platoons");

        model.setValueAt(1, 0, 2);      // Qty

        assertEquals(1, model.getPlatoon(0).getQtd());
        assertEquals(900, real.getQtd(), "the army loaded from the EGF must be untouched");
    }

    /** What the player types is his, and stops being reported as an outside estimate. */
    @Test
    void editingAValueFlipsProvenanceToManual() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim army = new ArmySim(loadedArmy(platoon(troopType("inf", false), 900)));
        scenario.addArmy(army, CombatScenario.Provenance.ESTIMATED);
        final BattleSimControler.PlatoonTableModel model = modelOver(scenario, army);

        assertEquals(CombatScenario.Provenance.ESTIMATED,
                scenario.getProvenance(model.getPlatoon(0)));

        model.setValueAt(400, 0, 2);

        assertEquals(CombatScenario.Provenance.MANUAL, scenario.getProvenance(model.getPlatoon(0)));
    }

    /**
     * The Lyr column, by the platoon's own troop type. A ship platoon and a land platoon sit in one
     * army and fight in different layers, which is the whole reason the column exists.
     */
    @Test
    void theLayerColumnMarksShipsSeparatelyFromLandTroops() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim army = new ArmySim(loadedArmy(
                platoon(troopType("sh", true), 40), platoon(troopType("inf", false), 600)));
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        final BattleSimControler.PlatoonTableModel model = modelOver(scenario, army);

        assertEquals(2, model.getRowCount());
        String ships = null, land = null;
        for (int row = 0; row < model.getRowCount(); row++) {
            // the Troops column holds the TipoTropa itself, because the player can retype it
            final TipoTropa tipo = (TipoTropa) model.getValueAt(row, 1);
            if ("sh".equals(tipo.getCodigo())) {
                ships = (String) model.getValueAt(row, 0);
            } else {
                land = (String) model.getValueAt(row, 0);
            }
        }
        assertEquals("N", ships);
        assertEquals("A", land);
    }

    /**
     * After and Lost exist so the engine fills them later. Until then they are not backed.
     *
     * They sit at 8 and 9 now: T-427 inserted Atk and Def at 6 and 7, which is the one thing the
     * rebuild was missing against the old window - it could say who fights whom and in what order
     * they die, and could not say who would win.
     */
    @Test
    void afterAndLostAreEmptyAndNotEditable() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim army = new ArmySim(loadedArmy(platoon(troopType("inf", false), 900)));
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        final BattleSimControler.PlatoonTableModel model = modelOver(scenario, army);

        assertEquals(10, model.getColumnCount());
        assertEquals("--", model.getValueAt(0, 8));
        assertEquals("--", model.getValueAt(0, 9));
        assertFalse(model.isCellEditable(0, 8));
        assertFalse(model.isCellEditable(0, 9));
        assertTrue(model.isCellEditable(0, 2), "quantity is the player's to type");
        assertTrue(model.isCellEditable(0, 1),
                "and so is the troop type - an army he cannot see needs one typed in from scratch");
    }

    /**
     * Attack and defence are computed, shown, and NOT editable.
     *
     * Read-only on purpose: they are derived from the platoon, the army's terrain and its nation,
     * so the way to change them is to change those. An editable cell here would let the player
     * type a strength his own troop list does not support.
     */
    @Test
    void attackAndDefenceAreComputedAndReadOnly() {
        final Exercito loaded = loadedArmy(platoon(troopType("inf", false), 900));
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim army = new ArmySim(loaded);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        final BattleSimControler.PlatoonTableModel model = modelOver(scenario, army);

        assertNotEquals("--", model.getValueAt(0, 6), "attack is computed, not a dash");
        assertNotEquals("--", model.getValueAt(0, 7), "and so is defence");
        assertFalse(model.isCellEditable(0, 6), "derived from the troops, not typed over them");
        assertFalse(model.isCellEditable(0, 7));
    }

    /**
     * There is no "army with no nation" case here any more, deliberately.
     *
     * T-441 made it impossible: {@code ScenarioLoader} forces a nation onto every army it reads and
     * the blank-army path falls back to the same stand-in. And T-440 was REVERSED on John's call -
     * the shared facade dereferences the nation unguarded on purpose, because server-side a
     * nationless army is corruption and halting is the correct outcome. So the invariant is proven
     * where it is established ({@code ScenarioLoaderTest.everyLoadedArmyHasANation}), not asserted
     * again here over a hand-built object that no longer represents anything real.
     */

    /**
     * The three transport columns appear only for an army that FLOATS or CARRIES. T-428.
     *
     * The old window showed capacity, cargo and ships-required for every army, including land hosts
     * where they are permanently useless - three columns of noise on the widest table in the
     * window.
     *
     * The predicate is "can carry", not "needs lifting". {@code getTransportesMinimo} is
     * {@code ceil(burden / SHIP_CAPACITY)}, so it is non-zero for EVERY land platoon with weight -
     * asking that question put the columns back on every army in the game, which is how the first
     * version of this got it wrong.
     */
    @Test
    void transportColumnsAppearOnlyForAnArmyThatFloats() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim land = new ArmySim(loadedArmy(platoon(troopType("inf", false), 900)));
        scenario.addArmy(land, CombatScenario.Provenance.EXACT);
        assertEquals(10, modelOver(scenario, land).getColumnCount(),
                "a land host has nothing to say about cargo");

        final ArmySim fleet = new ArmySim(loadedArmy(platoon(troopType("trireme", true), 46)));
        scenario.addArmy(fleet, CombatScenario.Provenance.EXACT);
        assertEquals(13, modelOver(scenario, fleet).getColumnCount(),
                "a fleet does, and gets capacity, cargo and ships-required");
    }

    /** No army selected is a real state, not a crash: an empty hex opens the window. */
    @Test
    void noSelectedArmyGivesAnEmptyTable() {
        assertEquals(0, modelOver(new CombatScenario(null, hex()), null).getRowCount());
    }
}
