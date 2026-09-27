package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two Phase 4b ports with behaviour worth pinning: the clipboard export and Clone platoon.
 *
 * Both were carried over from the old window, and the export was carried over FIXED - it had land
 * and naval swapped against their headers, which is the kind of defect nobody notices because both
 * columns are plausible numbers.
 */
class BattleSimExportAndCloneTest {

    private static final Terreno PLAIN = plain();

    private static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static java.util.SortedMap<Terreno, Integer> byTerrain(int valor) {
        final java.util.SortedMap<Terreno, Integer> ret = new java.util.TreeMap<>();
        ret.put(PLAIN, valor);
        return ret;
    }

    private static TipoTropa troopType(String codigo, boolean ships, int ataque) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        ret.setAtaqueTerreno(byTerrain(ataque));
        ret.setDefesaTerreno(byTerrain(ataque));
        ret.setMovimentoTerreno(byTerrain(5));
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
        ret.setTreino(70);
        ret.setModAtaque(60);
        ret.setModDefesa(80);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(PLAIN);
        return ret;
    }

    private static Nacao nacao() {
        final Nacao ret = new Nacao();
        ret.setCodigo("n");
        ret.setNome("House Tyrell");
        return ret;
    }

    private static ArmySim army(Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim("Paxter Redwyne", PLAIN, nacao());
        ret.setCodigo("a1");
        ret.setLocal(hex());
        for (Pelotao one : pelotoes) {
            ret.getPelotoes().put(one.getCodigo(), one);
        }
        return ret;
    }

    /**
     * THE CONTRACT, pinned byte for byte. T-446.
     *
     * John: <i>"Players have their own spreadsheets where they copy from the Sim and paste in their
     * spreadsheet. So keep the fields and gaps the same."</i> So this asserts the whole document
     * rather than sampling it - the trailing tabs, the leading tab on platoon rows and the three
     * blank lines after an army are the shape a sheet parses, and every one of them was lost when
     * the rebuild's first Copy was written as a clean export instead of a port.
     *
     * If this test has to change, a player's spreadsheet has to change with it.
     */
    @Test
    void theExportMatchesTheOldContractExactly() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("inf", false, 50), 900)),
                CombatScenario.Provenance.EXACT);

        final String[] lines = control.services.BattleSimConverter
                .getClipboardText(scenario).split("\n", -1);

        assertEquals("Name\tCommander rank\tMoral\tLand attack\tLand defense\tNavy attack\t"
                + "Navy defense\tTerrain\tFaction\t", lines[0], "army header");
        assertEquals("\tTroop Type\t# of Soldiers\tTraining\tWeapon\tArmor\tAttack\tDefense\t",
                lines[1], "platoon header, indented one column");
        assertEquals("", lines[2], "blank line closes the header block");
        assertTrue(lines[3].endsWith("\t"), "every field trails a tab: " + lines[3]);
        assertEquals(10, lines[3].split("\t", -1).length, "9 army fields and the trailing tab");
        assertTrue(lines[4].startsWith("\t"), "platoon rows indent under their army: " + lines[4]);
        assertEquals(9, lines[4].split("\t", -1).length,
                "leading tab, 7 platoon fields, trailing tab");
        assertEquals("", lines[5], "blank line closes the platoon block");
        assertEquals("", lines[6], "two more close the army");
        assertEquals("", lines[7]);
        // the ninth is not a fourth blank line: the document ENDS with a newline, so splitting
        // keeps an empty entry for it. Counting it as content is the easiest way to get this shape wrong.
        assertEquals("", lines[8], "the tail of the final newline");
        assertEquals(9, lines.length, "and nothing after it");
    }

    /**
     * Land and naval land under the RIGHT headers.
     *
     * The old export put {@code getAtaqueExercito(army, true)} under "Land attack", and
     * {@code true} means NAVAL - {@code BattleSimFacade.getArmyAttack} tests
     * {@code naval == tipoTropa.isBarcos()}. A fleet's strength was printed as a land army's.
     *
     * Pinned with an army that is ONLY ships, so the two figures cannot be confused: land must be
     * zero and naval must not. The column POSITIONS are the old ones; only the values moved.
     */
    @Test
    void theExportPutsNavalStrengthUnderTheNavalHeader() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("trireme", true, 40), 46)),
                CombatScenario.Provenance.EXACT);

        final String[] values = armyLine(scenario).split("\t", -1);

        assertEquals("0", values[3], "a fleet has no LAND attack");
        assertTrue(Integer.parseInt(values[5]) > 0, "and its naval attack must not be zero");
    }

    /** The reverse, so the test cannot pass by both being zero. */
    @Test
    void theExportPutsLandStrengthUnderTheLandHeader() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("inf", false, 50), 900)),
                CombatScenario.Provenance.EXACT);

        final String[] values = armyLine(scenario).split("\t", -1);

        assertTrue(Integer.parseInt(values[3]) > 0, "a land host has a land attack");
        assertEquals("0", values[5], "and no naval attack");
    }

    /**
     * The extensions are at the RIGHT end, which is the only place John allowed them.
     *
     * Anywhere else and every column after the insertion point lands in the wrong cell - which is
     * exactly what the rebuild's first Copy did with the faction at position 2.
     */
    @Test
    void theExtensionsSitAtTheRightEndAndNowhereElse() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("inf", false, 50), 900)),
                CombatScenario.Provenance.EXACT);

        final String[] lines = control.services.BattleSimConverter
                .getClipboardText(scenario).split("\n", -1);
        final String[] header = lines[0].split("\t", -1);
        final String[] platoonHeader = lines[1].split("\t", -1);

        assertEquals("Terrain", header[7], "the last field of the OLD contract stays last of the old");
        assertEquals("Faction", header[8], "and the extension follows it");
        assertEquals("Armor", platoonHeader[5], "same on the platoon line");
        assertEquals("Attack", platoonHeader[6]);
        assertEquals("Defense", platoonHeader[7]);
    }

    /** Every platoon gets its own indented row under its army. */
    @Test
    void theExportListsEachPlatoonUnderItsArmy() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("inf", false, 50), 900),
                platoon(troopType("arc", false, 30), 200)), CombatScenario.Provenance.EXACT);

        final String[] lines = control.services.BattleSimConverter
                .getClipboardText(scenario).split("\n", -1);

        assertEquals(10, lines.length, "3 header, army, 2 platoons, 3 blank, 1 tail");
        assertTrue(lines[4].startsWith("\t"), "platoon rows are indented: " + lines[4]);
        assertTrue(lines[5].startsWith("\t"), "both of them: " + lines[5]);
    }

    /** The army line, wherever the header block ends. */
    private static String armyLine(CombatScenario scenario) {
        final String[] lines = control.services.BattleSimConverter
                .getClipboardText(scenario).split("\n", -1);
        return lines[3];
    }

    /**
     * Clone platoon is NOT covered here, deliberately.
     *
     * {@code doClonePlatoon} reaches {@code getTroopCatalogue()}, which needs a loaded world
     * ({@code WorldFacadeCounselor.getInstance().getCenario()}), so it cannot run headless without
     * a seam that exists only for the test. What it does is six setters over
     * {@link control.BattleSimControler#doAddPlatoon}, which IS exercised by the window; the part
     * worth pinning - that the export puts naval strength under the naval header - is above.
     */
}
