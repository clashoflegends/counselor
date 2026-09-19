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

    /** A real EGF army, placed on a hex. Codigo before setLocal: the Local keys its index on it. */
    private static Exercito loadedArmy(Pelotao... pelotoes) {
        final Exercito ret = new Exercito();
        ret.setCodigo("a1");
        ret.setNome("Loaded");
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
            if ("sh".equals(model.getValueAt(row, 1))) {
                ships = (String) model.getValueAt(row, 0);
            } else {
                land = (String) model.getValueAt(row, 0);
            }
        }
        assertEquals("N", ships);
        assertEquals("A", land);
    }

    /** After and Lost exist so the engine fills them later. Until then they are not backed. */
    @Test
    void afterAndLostAreEmptyAndNotEditable() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim army = new ArmySim(loadedArmy(platoon(troopType("inf", false), 900)));
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        final BattleSimControler.PlatoonTableModel model = modelOver(scenario, army);

        assertEquals("--", model.getValueAt(0, 6));
        assertEquals("--", model.getValueAt(0, 7));
        assertFalse(model.isCellEditable(0, 6));
        assertFalse(model.isCellEditable(0, 7));
        assertTrue(model.isCellEditable(0, 2), "quantity is the player's to type");
        assertFalse(model.isCellEditable(0, 1), "the troop type is not retyped in this column");
    }

    /** No army selected is a real state, not a crash: an empty hex opens the window. */
    @Test
    void noSelectedArmyGivesAnEmptyTable() {
        assertEquals(0, modelOver(new CombatScenario(null, hex()), null).getRowCount());
    }
}
