package e2e;

import business.combat.ArmySim;
import business.combat.CombatChain;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.LayerReport;
import business.combat.RelationshipMatrix;
import control.services.BattleSimConverter;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a sea battle READS, which is a different question from whether its numbers are right.
 *
 * Every defect this covers shipped once already in the city layer and was found by looking at a
 * screenshot rather than by any test: a table headed with the wrong round numbers, a caption
 * counting the wrong thing, and a verdict that did not mention the layer at all. The sea layer
 * would have repeated all three.
 */
class BattleSimSeaReportTest {

    /** One instance: a troop type's attack and defence are keyed BY TERRAIN object. */
    private static final Terreno WATER = water();

    private static Terreno water() {
        final Terreno ret = new Terreno();
        ret.setCodigo("O");
        ret.setNome("High seas");
        ret.setAgua(true);
        return ret;
    }

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    private static TipoTropa shipType(String codigo) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome("Triremes");
        final SortedMap<Terreno, Integer> attack = new TreeMap<>();
        attack.put(WATER, 60);
        final SortedMap<Terreno, Integer> defence = new TreeMap<>();
        defence.put(WATER, 40);
        final SortedMap<Terreno, Integer> movement = new TreeMap<>();
        movement.put(WATER, 5);
        ret.setAtaqueTerreno(attack);
        ret.setDefesaTerreno(defence);
        ret.setMovimentoTerreno(movement);
        final Habilidade naval = new Habilidade();
        naval.setCodigo(";TTN;");
        naval.setNome(";TTN;");
        ret.addHabilidade(naval);
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(50);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("2442");
        ret.setCoordenadas("2442");
        ret.setTerreno(WATER);
        return ret;
    }

    private static ArmySim fleet(String nome, Nacao nacao, Local local, int ships) {
        final ArmySim ret = new ArmySim(nome, local.getTerreno(), nacao);
        ret.setCodigo(nome);
        ret.setLocal(local);
        ret.setTerreno(local.getTerreno());
        ret.getPelotoes().put("ng", platoon(shipType("ng"), ships));
        ret.setCombatLevel(CombatLevel.ATTACK_ARMY);
        ret.setMoral(100);
        return ret;
    }

    /** A big fleet and a small one, so the small one is destroyed and the verdict has to say so. */
    private static CombatScenario oneSidedBattle() {
        final Nacao mine = nacao("m", "House Arryn"), foe = nacao("f", "King's Court");
        final Local local = hex();
        final CombatScenario ret = new CombatScenario(null, local);
        ret.setTerreno(WATER);
        ret.addArmy(fleet("Armada", mine, local, 900), CombatScenario.Provenance.EXACT);
        ret.addArmy(fleet("Skiff", foe, local, 3), CombatScenario.Provenance.EXACT);
        ret.setRelacionamento(mine, foe, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(foe, mine, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    private static String joined(List<String> lines) {
        final StringBuilder ret = new StringBuilder();
        for (String line : lines) {
            ret.append(line).append('\n');
        }
        return ret.toString();
    }

    /** A sea battle with no land battle after it is not "no battle". */
    @Test
    void aPureSeaBattleIsNotReportedAsNoBattle() {
        final CombatScenario scenario = oneSidedBattle();
        final CombatResult result = new CombatChain().resolve(scenario, null);

        assertTrue(result.getRounds(CombatLayer.NAVY) > 0, "they fought at sea");
        final String noBattle = new BundleManager().getString("BATTLESIM.RESULT.NOLANDBATTLE");
        assertFalse(joined(BattleSimConverter.getVerdictLines(scenario, result)).contains(noBattle),
                "a fleet at the bottom of the sea is not an empty hex");
    }

    /** And the verdict names the fleet that went down. */
    @Test
    void theVerdictNamesTheFleetThatWasDestroyed() {
        final CombatScenario scenario = oneSidedBattle();
        final String verdict = joined(BattleSimConverter.getVerdictLines(scenario,
                new CombatChain().resolve(scenario, null)));

        assertTrue(verdict.contains("Skiff"), "the loser is named: " + verdict);
        assertTrue(verdict.contains("fleet"), "and what it lost: " + verdict);
    }

    /**
     * Nobody may still read "the sea layer is not simulated".
     *
     * The note and its label are gone; this is the assertion that keeps them gone.
     */
    @Test
    void nothingClaimsTheSeaLayerIsUnsimulated() {
        final CombatScenario scenario = oneSidedBattle();
        final CombatResult result = new CombatChain().resolve(scenario, null);

        for (String note : result.getNotes()) {
            assertFalse(note.contains("NAVYNOTSIMULATED"), "retired: " + note);
        }
        final LayerReport sea = LayerReport.of(scenario, result, CombatLayer.NAVY);
        assertTrue(sea.isFought(), "the layer that just sank a fleet did not 'not happen'");
    }

    /**
     * The cost line says SHIPS AND TROOPS for an army that fought at sea.
     *
     * Its total includes hulls, because the sea battle put hulls at risk. Calling that "troops"
     * was wrong by 12 in the first three-layer battle anybody looked at.
     */
    @Test
    void theCostLineNamesWhatItCounted() {
        final CombatScenario scenario = oneSidedBattle();
        final String verdict = joined(BattleSimConverter.getVerdictLines(scenario,
                new CombatChain().resolve(scenario, null)));

        assertTrue(verdict.contains("ships and troops"),
                "hulls are in this total and the sentence has to say so: " + verdict);
    }
}
