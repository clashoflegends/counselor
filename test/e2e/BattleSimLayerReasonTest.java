package e2e;

import business.combat.ArmySim;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Participation is reported LAYER BY LAYER, N A C, and never as one sentence for the whole army.
 *
 * John, 2026-09-19: "The participation is layer by layer. N A C. There are enough edge cases that
 * there is always a situation that someone will participate of 0 to 3 of them."
 *
 * The line used to print the FIRST non-fighting reason, asked of the land layer first. Live example
 * from hex 1141: four Greyjoy armies sitting on a Tully city read "Takes no part in this battle (no
 * enemy present)" - true of the land layer, and it buried the one answer the player could act on.
 * The city layer said "not ordered to assault", which he fixes by setting Combat level to Attack
 * city. The line existed to tell him that and told him the opposite.
 */
class BattleSimLayerReasonTest {

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    private static Pelotao platoon(int qtd) {
        final TipoTropa tipo = new TipoTropa();
        tipo.setCodigo("inf");
        tipo.setNome("inf");
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    /** A hex with a city, as 1141 is. Anchorable ground, so nothing is blocked by the landing. */
    private static Local hexWithCity(Nacao owner) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        terreno.setAncoravel(true);
        final Cidade cidade = new Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("Riverrun");
        cidade.setNacao(owner);
        cidade.setTamanho(3);
        final Local ret = new Local();
        ret.setCodigo("1141");
        ret.setCoordenadas("1141");
        ret.setTerreno(terreno);
        ret.setCidade(cidade);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Local hex) {
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome);
        ret.getPelotoes().put("inf", platoon(500));
        return ret;
    }

    private static String fightsIn(CombatScenario scenario, ArmySim army) {
        return BattleSimConverter.getFightsIn(scenario.getParticipation().get(army));
    }

    /**
     * Hex 1141, reproduced: armies of one faction on another faction's city, ordered to attack
     * armies only.
     *
     * The land layer has nothing to say - they are all the same faction, so there is no enemy
     * ashore. The CITY layer is the one holding them back, and it must say so.
     */
    @Test
    void theCityReasonIsNotBuriedByTheLandReason() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully);
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim one = army("Joron Blacktide", greyjoy, hex);
        scenario.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        scenario.addArmy(army("Nute the Barber", greyjoy, hex), CombatScenario.Provenance.ESTIMATED);
        // at war with the city's owner, so ONLY the combat level is stopping the assault
        scenario.setRelacionamento(greyjoy, tully, RelationshipMatrix.SWORN_ENEMY);

        final String text = fightsIn(scenario, one);

        assertTrue(text.contains("sea:"), "every layer answers, even the irrelevant one");
        assertTrue(text.contains("land:"), "every layer answers");
        assertTrue(text.contains("city:"), "and the city layer above all, here");
        assertTrue(text.contains("not ordered to assault"),
                "the one thing he can act on must be on screen: " + text);
    }

    /** Fix the order and the same army fights, with the city layer saying so. */
    @Test
    void orderingTheAssaultPutsItInTheCityLayer() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully);
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim one = army("Joron Blacktide", greyjoy, hex);
        one.setCombatLevel(CombatLevel.ATTACK_CITY);
        scenario.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(greyjoy, tully, RelationshipMatrix.SWORN_ENEMY);

        final String text = fightsIn(scenario, one);

        assertTrue(text.contains("takes part"), "the city layer now says it fights: " + text);
        assertTrue(text.contains("Fights in:"), "and the summary line agrees");
    }

    /**
     * The roster tooltip answers BEFORE a run, and for an army taking no part. T-838.
     *
     * Those are the two moments somebody hovers a row of dots to ask what they mean, and both used
     * to return null: the hover stayed silent on exactly the question it exists to answer. The dots
     * themselves stay - in brackets between the name and the troop count they cannot be read as a
     * truncated name, and the fixed slot is what lets a column of armies be read straight down.
     */
    @Test
    void theLayerTooltipAnswersBeforeAnyRun() {
        final Nacao greyjoy = nacao("g", "House Greyjoy"), tully = nacao("t", "House Tully");
        final Local hex = hexWithCity(tully);
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim one = army("Joron Blacktide", greyjoy, hex);
        scenario.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(greyjoy, tully, RelationshipMatrix.SWORN_ENEMY);

        // result == null: nothing has been run yet
        final String hint = BattleSimConverter.getLayerHint(one,
                scenario.getParticipation().get(one), null);

        assertTrue(hint != null && !hint.isEmpty(), "the dots have to explain themselves");
        assertTrue(hint.contains("city:"), "and every layer answers: " + hint);
    }

    /** An army in no layer at all gets the same courtesy, run or no run. */
    @Test
    void theLayerTooltipAnswersForAnArmyInNoLayer() {
        final Nacao mine = nacao("m", "Mine");
        final Local hex = hexWithCity(mine);
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim alone = army("Alone", mine, hex);
        scenario.addArmy(alone, CombatScenario.Provenance.EXACT);

        final String hint = BattleSimConverter.getLayerHint(alone,
                scenario.getParticipation().get(alone), null);

        assertTrue(hint != null && hint.contains("Takes no part in this battle"),
                "the one army the player opened the window to ask about: " + hint);
    }

    /**
     * An army in NO layer still gets three answers, one per layer.
     *
     * This is the 0-of-3 end of John's "0 to 3", and the case the whole line exists for.
     */
    @Test
    void anArmyThatFightsNowhereStillExplainsAllThreeLayers() {
        final Nacao mine = nacao("m", "Mine");
        final Local hex = hexWithCity(mine);
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim alone = army("Alone", mine, hex);
        scenario.addArmy(alone, CombatScenario.Provenance.EXACT);

        final String text = fightsIn(scenario, alone);

        assertTrue(text.contains("Takes no part in this battle"), text);
        for (String layer : new String[]{"sea:", "land:", "city:"}) {
            assertTrue(text.contains(layer), "missing " + layer + " in: " + text);
        }
    }

    /** The city summary is three short facts, so the siege clause cannot be cut off the end. */
    @Test
    void theCitySummaryWrapsInsteadOfRunningOffTheEdge() {
        final Local hex = hexWithCity(nacao("t", "House Tully"));
        final CombatScenario scenario = new CombatScenario(null, hex);

        final String text = BattleSimConverter.getCityText(scenario);

        assertTrue(text.startsWith("<html>") && text.endsWith("</html>"), text);
        assertTrue(text.contains("<br>"), "three facts on three lines: " + text);
        assertTrue(text.contains("round 0"),
                "the siege clause is the half that used to get truncated away: " + text);
    }

    /** The controller is untouched by this; kept so the model still builds against it. */
    @Test
    void theRosterStillBuilds() {
        final Nacao mine = nacao("m", "Mine");
        final Local hex = hexWithCity(mine);
        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.addArmy(army("Alone", mine, hex), CombatScenario.Provenance.EXACT);

        assertTrue(new BattleSimControler.DiplomacyTableModel(scenario).getRowCount() >= 1);
    }
}
