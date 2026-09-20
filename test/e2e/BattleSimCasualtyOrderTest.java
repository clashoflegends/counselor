package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.converter.ConverterFactory;
import control.BattleSimControler;
import model.Habilidade;
import model.Local;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The platoon table's ROW ORDER is the order the platoons take casualties, and it follows the tactic.
 *
 * John, 2026-09-19: "one important use of the current battlesim is for players to visualize the
 * sequence of casualties, which they use to select tactics for the army then look at the sequence of
 * the platoons... in some terrains catapults will die before infantry unless guerrilla is selected.
 * Which is important as you want to spend the infantry to win the army combat to use the catapults
 * in the city layer."
 *
 * So the order is the feature, not presentation. The three-pane rebuild had it listing
 * {@code getPelotoes().values()} - a TreeMap keyed by troop-type codigo, i.e. ALPHABETICAL - which
 * looks every bit as authoritative as a casualty order while answering a different question. The
 * comparator is the Judge's own, reached through the same {@code ComparatorFactory} call it makes
 * inside {@code ExercitoControlFacade.getTropasTerraSortedCloned}.
 */
class BattleSimCasualtyOrderTest {

    private static final Terreno PLAIN = terreno("P", "Plain");

    private static Terreno terreno(String codigo, String nome) {
        final Terreno ret = new Terreno();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        ret.setAncoravel(true);
        return ret;
    }

    /**
     * Troop types whose stats differ enough for the tactic to reorder them.
     *
     * Attack, defense and movement are PER TERRAIN - {@code SortedMap<Terreno, Integer>} - which is
     * exactly why "in some terrains catapults will die before infantry". The comparator ranks on
     * those three in an order that changes per tactic: Charge leads with attack, Guerrilla with the
     * slowest.
     */
    private static TipoTropa troopType(String codigo, int ataque, int defesa, int movimento) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        ret.setAtaqueTerreno(terrainValue(ataque));
        ret.setDefesaTerreno(terrainValue(defesa));
        ret.setMovimentoTerreno(terrainValue(movimento));
        return ret;
    }

    private static java.util.SortedMap<Terreno, Integer> terrainValue(int valor) {
        final java.util.SortedMap<Terreno, Integer> ret = new java.util.TreeMap<>();
        ret.put(PLAIN, valor);
        return ret;
    }

    private static TipoTropa ship(String codigo) {
        final TipoTropa ret = troopType(codigo, 10, 10, 10);
        final Habilidade hab = new Habilidade();
        hab.setCodigo(";TTN;");
        hab.setNome(";TTN;");
        ret.addHabilidade(hab);
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    /** The same Terreno instance the stat maps are keyed on, or every lookup misses. */
    private static Local hex() {
        final Terreno terreno = PLAIN;
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno);
        return ret;
    }

    /** Codigos chosen so ALPHABETICAL order is a distinct, detectable order of its own. */
    private static ArmySim armyWithMixedTroops(Local hex) {
        final ArmySim ret = new ArmySim("Host", hex.getTerreno(), null);
        ret.setCodigo("a1");
        for (Pelotao one : new Pelotao[]{
            platoon(troopType("aaa_catapult", 90, 5, 1), 20),
            platoon(troopType("mmm_infantry", 30, 40, 5), 900),
            platoon(troopType("zzz_cavalry", 60, 20, 9), 200)}) {
            ret.getPelotoes().put(one.getCodigo(), one);
        }
        return ret;
    }

    private static String order(CombatScenario scenario, ArmySim army) {
        final BattleSimControler.PlatoonTableModel model =
                new BattleSimControler.PlatoonTableModel(scenario, army);
        final StringBuilder ret = new StringBuilder();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (ret.length() > 0) {
                ret.append(",");
            }
            ret.append(model.getPlatoon(row).getTipoTropa().getCodigo());
        }
        return ret.toString();
    }

    /**
     * Changing the tactic changes the sequence. The whole reason a player opens this table.
     *
     * Charge and Guerrilla rank on opposite things - Charge leads with attack, Guerrilla with the
     * slowest unit - so an army of a siege engine, infantry and cavalry cannot come out the same
     * way under both unless the sort is being ignored.
     */
    @Test
    void theTacticChangesTheCasualtySequence() {
        final Local hex = hex();
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim army = armyWithMixedTroops(hex);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);

        army.setTatica(ConverterFactory.taticaToInt("ca"));      // Charge
        final String charge = order(scenario, army);
        army.setTatica(ConverterFactory.taticaToInt("gu"));      // Guerrilla
        final String guerrilla = order(scenario, army);

        assertNotEquals(charge, guerrilla,
                "the tactic must reorder the platoons - charge=" + charge + " guerrilla=" + guerrilla);
    }

    /**
     * And the order is NOT the map's own, which is alphabetical by troop codigo.
     *
     * This is the bug being pinned. The codigos above sort to catapult, infantry, cavalry; at least
     * one tactic must disagree with that, or the table is simply printing the TreeMap.
     */
    @Test
    void theOrderIsNotTheAlphabeticalMapOrder() {
        final Local hex = hex();
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim army = armyWithMixedTroops(hex);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        final String alphabetical = "aaa_catapult,mmm_infantry,zzz_cavalry";

        boolean anyDiffers = false;
        for (String tatica : new String[]{"ca", "fl", "pa", "ce", "gu", "em"}) {
            army.setTatica(ConverterFactory.taticaToInt(tatica));
            if (!alphabetical.equals(order(scenario, army))) {
                anyDiffers = true;
            }
        }
        assertTrue(anyDiffers, "every tactic produced the TreeMap's alphabetical order");
    }

    /**
     * Ships come first and are not ranked by the land comparator.
     *
     * The Judge's land sort drops them outright ({@code !isBarcos()} in
     * {@code getTropasTerraSortedCloned}), so ordering them by it would be inventing an answer to a
     * question about a different layer.
     */
    @Test
    void shipsAreListedFirstAndNotRankedByTheLandComparator() {
        final Local hex = hex();
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim army = armyWithMixedTroops(hex);
        final Pelotao triremes = platoon(ship("zzz_trireme"), 46);
        army.getPelotoes().put(triremes.getCodigo(), triremes);
        army.setTatica(ConverterFactory.taticaToInt("ca"));
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);

        final String order = order(scenario, army);

        assertTrue(order.startsWith("zzz_trireme,"),
                "ships lead, whatever their codigo sorts to: " + order);
    }

    /** All the platoons are still there, whatever the order. */
    @Test
    void sortingLosesNothing() {
        final Local hex = hex();
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim army = armyWithMixedTroops(hex);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);

        assertEquals(3, new BattleSimControler.PlatoonTableModel(scenario, army).getRowCount());
    }
}
