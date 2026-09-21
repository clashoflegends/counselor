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
     * Land and naval land under the RIGHT headers.
     *
     * The old export put {@code getAtaqueExercito(army, true)} under "Land attack", and
     * {@code true} means NAVAL - {@code BattleSimFacade.getArmyAttack} tests
     * {@code naval == tipoTropa.isBarcos()}. A fleet's strength was printed as a land army's.
     *
     * Pinned with an army that is ONLY ships, so the two figures cannot be confused: land must be
     * zero and naval must not.
     */
    @Test
    void theExportPutsNavalStrengthUnderTheNavalHeader() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("trireme", true, 40), 46)),
                CombatScenario.Provenance.EXACT);

        final String[] lines = control.services.BattleSimConverter.getClipboardText(scenario).split("\n");
        final String[] header = lines[0].split("\t");
        final String[] values = lines[1].split("\t");

        // header order: commander, nation, morale, land atk, land def, naval atk, naval def
        assertEquals(7, header.length, lines[0]);
        assertEquals("0", values[3], "a fleet has no LAND attack: " + lines[1]);
        assertTrue(Integer.parseInt(values[5]) > 0,
                "and its naval attack must not be zero: " + lines[1]);
    }

    /** The reverse, so the test cannot pass by both being zero. */
    @Test
    void theExportPutsLandStrengthUnderTheLandHeader() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("inf", false, 50), 900)),
                CombatScenario.Provenance.EXACT);

        final String[] values = control.services.BattleSimConverter
                .getClipboardText(scenario).split("\n")[1].split("\t");

        assertTrue(Integer.parseInt(values[3]) > 0, "a land host has a land attack");
        assertEquals("0", values[5], "and no naval attack");
    }

    /** Every platoon gets its own indented row under its army. */
    @Test
    void theExportListsEachPlatoonUnderItsArmy() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army(platoon(troopType("inf", false, 50), 900),
                platoon(troopType("arc", false, 30), 200)), CombatScenario.Provenance.EXACT);

        final String[] lines = control.services.BattleSimConverter.getClipboardText(scenario).split("\n");

        assertEquals(4, lines.length, "header, army, and one row per platoon");
        assertTrue(lines[2].startsWith("\t"), "platoon rows are indented: " + lines[2]);
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
