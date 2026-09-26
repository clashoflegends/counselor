package e2e;

import business.combat.ArmySim;
import business.combat.CombatChain;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.services.BattleSimConverter;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The verdict describes the BATTLE, and a siege is a battle even when nobody fought ashore.
 *
 * Two defects, both of them the same mistake: the round count was the LAND battle's, and three
 * player-facing places read it as "did anything happen". An army that stormed a city and razed it
 * produced zero rounds, so the verdict was replaced wholesale by "No land battle took place on this
 * hex", the status line said the same, and the title read "0 rounds" - for a battle that had just
 * destroyed a city and killed several hundred men. The third defect is the quiet one: even when the
 * land battle DID happen, the verdict listed who held the field and never said what became of the
 * walls.
 */
class BattleSimCityVerdictTest {

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    /** Attack and defence are PER TERRAIN on TipoTropa, so a flat number has to be a map. */
    private static SortedMap<Terreno, Integer> byTerrain(Terreno terreno, int valor) {
        final SortedMap<Terreno, Integer> ret = new TreeMap<>();
        ret.put(terreno, valor);
        return ret;
    }

    private static Pelotao platoon(Terreno terreno, int qtd) {
        final TipoTropa tipo = new TipoTropa();
        tipo.setCodigo("inf");
        tipo.setNome("Infantry");
        tipo.setAtaqueTerreno(byTerrain(terreno, 60));
        tipo.setDefesaTerreno(byTerrain(terreno, 40));
        tipo.setMovimentoTerreno(byTerrain(terreno, 5));
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(50);
        return ret;
    }

    private static Local hexWithCity(Nacao owner, int fortificacao) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        terreno.setAncoravel(true);
        final Cidade cidade = new Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("Riverrun");
        cidade.setNacao(owner);
        cidade.setTamanho(1);
        cidade.setFortificacao(fortificacao);
        cidade.setLealdade(50);
        final Local ret = new Local();
        ret.setCodigo("1141");
        ret.setCoordenadas("1141");
        ret.setTerreno(terreno);
        ret.setCidade(cidade);
        return ret;
    }

    private static ArmySim besieger(String nome, Nacao nacao, Local hex, int qtd) {
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome);
        ret.setLocal(hex);
        ret.getPelotoes().put("inf", platoon(hex.getTerreno(), qtd));
        ret.setCombatLevel(CombatLevel.ATTACK_CITY);
        ret.setMoral(100);
        return ret;
    }

    /** One army, one ungarrisoned city, nothing to fight ashore: the whole battle is the assault. */
    private static CombatScenario pureAssault(Local hex, Nacao attacker, Nacao owner, int qtd) {
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        ret.addArmy(besieger("Joron Blacktide", attacker, hex, qtd),
                CombatScenario.Provenance.EXACT);
        return ret;
    }

    private static String joined(List<String> lines) {
        final StringBuilder ret = new StringBuilder();
        for (String line : lines) {
            ret.append(line).append('\n');
        }
        return ret.toString();
    }

    /**
     * A siege with no land battle in front of it is not "no battle".
     *
     * The bail is on the round count, so this is the test that pins the count to every layer rather
     * than to the land one.
     */
    @Test
    void aPureCityAssaultIsNotReportedAsNoBattle() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully, 0);
        final CombatScenario scenario = pureAssault(hex, greyjoy, tully, 5000);

        final CombatResult result = new CombatChain().resolve(scenario, null);

        assertTrue(result.getRounds() > 0,
                "the assault is a round of this battle, and the only one");
        final String verdict = joined(BattleSimConverter.getVerdictLines(scenario, result));
        assertFalse(verdict.contains("No battle was fought"),
                "a razed city is not an empty hex: " + verdict);
        assertFalse(BattleSimConverter.getRunResultText(result).contains("No battle was fought"),
                "and the status line agrees with the verdict");
    }

    /** And it says WHICH city, and what became of it. */
    @Test
    void theVerdictNamesTheCityAndItsFate() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully, 0);
        final CombatScenario scenario = pureAssault(hex, greyjoy, tully, 5000);

        final String verdict =
                joined(BattleSimConverter.getVerdictLines(scenario,
                        new CombatChain().resolve(scenario, null)));

        assertTrue(verdict.contains("Riverrun"), "the city has a name: " + verdict);
        assertTrue(verdict.contains("was taken") || verdict.contains("was razed")
                || verdict.contains("fell"), "and a fate: " + verdict);
    }

    /** A repulse is a verdict too, and the one the player most needs to see before he commits. */
    @Test
    void wallsThatHoldAreReportedAsHolding() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        // a token force against a fortress: ataqueTotal cannot reach the defense
        final Local hex = hexWithCity(tully, 5);
        final CombatScenario scenario = pureAssault(hex, greyjoy, tully, 1);

        final String verdict =
                joined(BattleSimConverter.getVerdictLines(scenario,
                        new CombatChain().resolve(scenario, null)));

        assertTrue(verdict.contains("Riverrun") && verdict.contains("walls were not breached"),
                "the walls held, and that is the result: " + verdict);
    }

    /**
     * A hex with a city nobody attacked gets no city line at all.
     *
     * Otherwise every land battle fought within sight of a city would carry a sentence about a
     * siege that never happened.
     */
    @Test
    void aCityNobodyAttackedIsNotMentioned() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully, 2);
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim one = besieger("Joron Blacktide", greyjoy, hex, 800);
        one.setCombatLevel(CombatLevel.ATTACK_ARMY);     // armies only, not the city
        final ArmySim two = besieger("Brynden Tully", tully, hex, 800);
        two.setCombatLevel(CombatLevel.ATTACK_ARMY);
        scenario.addArmy(one, CombatScenario.Provenance.EXACT);
        scenario.addArmy(two, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(greyjoy, tully, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(tully, greyjoy, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new CombatChain().resolve(scenario, null);

        assertTrue(result.getRounds(CombatLayer.ARMY) > 0, "they did fight ashore");
        final String verdict = joined(BattleSimConverter.getVerdictLines(scenario, result));
        assertFalse(verdict.contains("Riverrun"),
                "nobody laid a hand on the city: " + verdict);
    }
}
