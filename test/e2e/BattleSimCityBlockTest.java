package e2e;

import business.combat.ArmySim;
import business.combat.CombatChain;
import business.combat.CombatLevel;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.services.BattleSimConverter;
import java.util.IllegalFormatException;
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
import persistenceCommons.BundleManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The city answers for itself, under its own layer. T-849.
 *
 * <h3>The report</h3>
 *
 * John, 2026-09-29: with several armies storming one city "it is not clear if the city
 * captured/razed or not. In particular when the attacking armies fail to capture it but still have
 * troops remaining. I need to interpret each army icon."
 *
 * <h3>Why the per-army marks were not the bug</h3>
 *
 * They were already right: {@code CombatChain.outcomeOf} stamps LOST on every attacker of a repelled
 * assault, whatever each of them has left. The problem is that the city's fate is a fact about the
 * CITY, and reading it off a column of army rows is inference. The layer-3 table cannot state it
 * either - its rows are armies and its cells are troops remaining.
 *
 * <h3>What is pinned here</h3>
 *
 * That the block appears for the case he reported, that every outcome says where the city ends up
 * rather than leaving a blank, that a hex nobody assaulted adds nothing, and that every new label
 * survives being formatted with its real arguments. That last one is not ceremony: crash 327108 was
 * a troop NAME handed to a {@code %,d}, it threw rather than coercing, and it fired only on the
 * success path - so the headline feature worked in exactly the case nobody tested.
 */
class BattleSimCityBlockTest {

    private static final BundleManager labels = new BundleManager();

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

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
        return hexWithCity(owner, fortificacao, 1);
    }

    /**
     * Size matters to the OUTCOME, not just to the defence.
     * <p>
     * A captured camp is razed by rule - {@code CityCombatResolver.isRaze} - so a size-1 city can
     * never produce CAPTURED, and a fixture built on one silently tests the razed path while
     * claiming to test capture.
     */
    private static Local hexWithCity(Nacao owner, int fortificacao, int tamanho) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        terreno.setAncoravel(true);
        final Cidade cidade = new Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("Riverrun");
        cidade.setNacao(owner);
        cidade.setTamanho(tamanho);
        cidade.setFortificacao(fortificacao);
        cidade.setLealdade(50);
        final Local ret = new Local();
        ret.setCodigo("1141");
        ret.setCoordenadas("1141");
        ret.setTerreno(terreno);
        ret.setCidade(cidade);
        return ret;
    }

    private static ArmySim besieger(String nome, Nacao nacao, Local hex, int qtd, CombatLevel level) {
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome);
        ret.setLocal(hex);
        ret.getPelotoes().put("inf", platoon(hex.getTerreno(), qtd));
        ret.setCombatLevel(level);
        ret.setMoral(100);
        return ret;
    }

    private static String joined(List<String> lines) {
        final StringBuilder ret = new StringBuilder();
        for (String line : lines) {
            ret.append(line).append('\n');
        }
        return ret.toString();
    }

    /** Two nations at the same walls, which is the shape his report came from. */
    private static CombatScenario twoBesiegers(Local hex, Nacao owner, Nacao one, Nacao two,
            int qtdOne, int qtdTwo, CombatLevel level) {
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.setRelacionamento(one, owner, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(two, owner, RelationshipMatrix.SWORN_ENEMY);
        ret.addArmy(besieger("Joron Blacktide", one, hex, qtdOne, level),
                CombatScenario.Provenance.EXACT);
        ret.addArmy(besieger("Aeron Pyke", two, hex, qtdTwo, level),
                CombatScenario.Provenance.EXACT);
        return ret;
    }

    /**
     * HIS case: the assault fails and both armies walk away with men.
     *
     * The rounds table shows two rows of survivors and cannot say what happened to the walls. The
     * block has to say it in words, and has to say where the city ended up.
     */
    @Test
    void aRepelledAssaultWithSurvivorsSaysSoAndSaysWhoKeepsTheCity() {
        final Nacao tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully, 5);   // a fortress against token forces
        final CombatScenario scenario = twoBesiegers(hex, tully,
                nacao("g", "House Greyjoy"), nacao("l", "House Lannister"),
                1, 1, CombatLevel.ATTACK_CITY);

        final CombatResult result = new CombatChain().resolve(scenario, null);
        final String block = joined(BattleSimConverter.getCityLines(scenario, result));

        assertFalse(block.isEmpty(), "an assault was fought, so the city has something to say");
        assertTrue(block.contains("walls were not breached"),
                "the walls held and the block has to say it: " + block);
        assertTrue(block.contains("House Tully"),
                "and has to say who still owns the city afterwards: " + block);
        // the two numbers the whole layer turns on, so a player can see which side of the verdict
        // he disagrees with rather than being told only the conclusion
        assertTrue(block.contains(labels.getString("BATTLESIM.CITY.RESULT.DEFENCE").split("%")[0]),
                "the defence that had to be beaten: " + block);
        assertTrue(block.contains(labels.getString("BATTLESIM.CITY.RESULT.ATTACK").split("%")[0]),
                "and the attack that was brought: " + block);
        // one line per attacker, because the summed figure is the only thing the rule uses and a
        // player with several armies at the walls needs to know which of them is carrying it
        assertTrue(block.contains("Joron Blacktide") && block.contains("Aeron Pyke"),
                "each attacker's contribution: " + block);
    }

    /** When it falls, the block names the army that ends up holding it AND its nation. */
    @Test
    void aCapturedCityNamesItsNewHolder() {
        final Nacao tully = nacao("t", "House Tully");
        final Nacao greyjoy = nacao("g", "House Greyjoy");
        final Local hex = hexWithCity(tully, 0, 3);   // a real city, not a camp: see hexWithCity
        final CombatScenario scenario = twoBesiegers(hex, tully, greyjoy,
                nacao("l", "House Lannister"), 9000, 10, CombatLevel.ATTACK_CITY);

        final CombatResult result = new CombatChain().resolve(scenario, null);
        final String block = joined(BattleSimConverter.getCityLines(scenario, result));

        assertTrue(block.contains("Riverrun"), "the city is named: " + block);
        // The claimant is CityResult.getOwner(), which mirrors doCityCaptured: highest recomputed
        // attack wins, so the 9,000-strong army takes it and not the token one beside it.
        assertTrue(block.contains("Joron Blacktide") && block.contains("House Greyjoy"),
                "the forecast says who ends up behind the walls: " + block);
        // and the headline at the top of the page carries the nation too, so the answer is there
        // before a page of tables is scrolled
        assertTrue(joined(BattleSimConverter.getVerdictLines(scenario, result))
                .contains("House Greyjoy"), "the verdict line names the taker as well");
    }

    /** Razed leaves nobody holding it, and that is an answer, not a blank. */
    @Test
    void aRazedCityIsHeldByNobody() {
        final Nacao tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully, 0);
        final CombatScenario scenario = twoBesiegers(hex, tully,
                nacao("g", "House Greyjoy"), nacao("l", "House Lannister"),
                9000, 9000, CombatLevel.RAZE_CITY);

        final CombatResult result = new CombatChain().resolve(scenario, null);
        final String block = joined(BattleSimConverter.getCityLines(scenario, result));

        assertTrue(block.contains(labels.getString("BATTLESIM.CITY.RESULT.RAZED")),
                "a destroyed city has no new owner and the block says so: " + block);
    }

    /**
     * A city nobody assaulted contributes nothing.
     *
     * Otherwise every land battle fought within sight of a city would carry a block about a siege
     * that never happened.
     */
    @Test
    void aCityNobodyAssaultedAddsNothing() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully, 2);
        final CombatScenario scenario = twoBesiegers(hex, tully, greyjoy, tully, 800, 800,
                CombatLevel.ATTACK_ARMY);
        scenario.setRelacionamento(greyjoy, tully, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(tully, greyjoy, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new CombatChain().resolve(scenario, null);

        assertTrue(BattleSimConverter.getCityLines(scenario, result).isEmpty(),
                "nobody laid a hand on the city, so it has nothing to report");
    }

    /** Nothing before a run, and no throw either. */
    @Test
    void thereIsNoBlockBeforeARun() {
        assertTrue(BattleSimConverter.getCityLines(null, null).isEmpty());
    }

    /**
     * Every new key formatted with its REAL arguments, in every language.
     *
     * Crash 327108 shipped because a label was only ever formatted in the case that did not fire.
     * These are numeric specifiers next to string ones, which is exactly the mix that throws.
     */
    @Test
    void everyCityLabelFormatsWithItsRealArguments() {
        final String[][] withArgs = {
            {"BATTLESIM.VERDICT.CITY.CAPTURED.BY", "s", "s"},
            {"BATTLESIM.CITY.RESULT.DEFENCE", "d"},
            {"BATTLESIM.CITY.RESULT.DEFENCE.SIEGE", "d", "d"},
            {"BATTLESIM.CITY.RESULT.ATTACK", "d"},
            {"BATTLESIM.CITY.RESULT.ATTACK.ARMY", "s", "d", "d"},
            {"BATTLESIM.CITY.RESULT.HOLDS", "s", "s"},
            {"BATTLESIM.CITY.RESULT.HELD", "s"},
        };
        for (String language : new String[]{"", "pt", "es", "ca", "it"}) {
            for (String[] spec : withArgs) {
                final Object[] args = new Object[spec.length - 1];
                for (int ii = 0; ii < args.length; ii++) {
                    args[ii] = "s".equals(spec[ii + 1]) ? (Object) "Riverrun" : (Object) 12345L;
                }
                try {
                    final String out = String.format(text(language, spec[0]), args);
                    assertFalse(out.contains("%"),
                            spec[0] + " [" + language + "] left a specifier unfilled: " + out);
                } catch (IllegalFormatException ex) {
                    fail(spec[0] + " [" + language + "] threw on its real arguments: " + ex);
                }
            }
            // the three with no arguments still have to exist in every bundle
            for (String key : new String[]{"BATTLESIM.CITY.RESULT.RAZED",
                "BATTLESIM.CITY.RESULT.NOSURVIVOR", "BATTLESIM.CITY.RESULT.NOBODY"}) {
                assertFalse(text(language, key).trim().isEmpty(),
                        key + " is missing from [" + language + "]");
            }
        }
    }

    /**
     * One key in one language.
     *
     * A locale with no bundle of its own falls back to the default, which is what happens at runtime
     * too - so this measures the text a player in that language actually gets, not the file.
     */
    private static String text(String language, String key) {
        return language.isEmpty()
                ? labels.getString(key) : labels.getString(key, new java.util.Locale(language));
    }
}
