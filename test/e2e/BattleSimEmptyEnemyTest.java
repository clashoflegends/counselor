package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.services.BattleSimConverter;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An enemy the player cannot see into arrives EMPTY, and the window has to say what to do about it.
 *
 * Game 906 turn 3 hex 0452, twice reported as "I still can't run simulation when I am not involved".
 * House Tyrell's two fleets arrive at army-visibility 1, which exports a nation, a commander name
 * and a size band and NO platoons at all. Hostility was correct, the roster was correct, and every
 * layer correctly declined - so Run was disabled and the only thing on screen was a generic
 * sentence about armies not reaching each other, on a hex where the whole answer was that two
 * NAMED armies had nothing in them.
 *
 * Diagnosing is not teaching. These tests pin the two places that now say what to do.
 */
class BattleSimEmptyEnemyTest {

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
        terreno.setAncoravel(true);
        final Local ret = new Local();
        ret.setCodigo("0452");
        ret.setCoordenadas("0452");
        ret.setTerreno(terreno);
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

    /** With platoons when counted, with NOTHING when the player cannot see into it. */
    private static ArmySim army(String nome, Nacao nacao, Local hex, boolean counted) {
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome);
        ret.setLocal(hex);
        if (counted) {
            ret.getPelotoes().put("inf", platoon(4304));
        }
        return ret;
    }

    /** 0452: Jaime ashore, Colin Florent an unscouted shell, at war and unable to fight. */
    private static CombatScenario lannisport() {
        final Nacao lannister = nacao("l", "House Lannister"), tyrell = nacao("y", "House Tyrell");
        final Local hex = hex();
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.addArmy(army("Jaime Lannister", lannister, hex, true),
                CombatScenario.Provenance.ESTIMATED);
        ret.addArmy(army("Colin Florent", tyrell, hex, false),
                CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(lannister, tyrell, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(tyrell, lannister, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    private static ArmySim find(CombatScenario scenario, String nome) {
        for (ArmySim army : scenario.getArmies()) {
            if (nome.equals(army.getNome())) {
                return army;
            }
        }
        throw new IllegalStateException("no army called " + nome);
    }

    /** The status bar names the army the player has to fill in, instead of describing the weather. */
    @Test
    void theStatusBarNamesTheArmyThatCannotBeSeenInto() {
        final String reason = BattleSimConverter.getRunDisabledReason(lannisport());

        assertTrue(reason.contains("Colin Florent"),
                "the blocker has a name and the status bar must use it: " + reason);
        assertTrue(reason.contains("Add platoon"),
                "and it must say what to do, not only what is wrong: " + reason);
        assertFalse(reason.contains("Jaime Lannister"),
                "Jaime is countable - sending the player to edit him would be a wild goose chase: "
                + reason);
    }

    /** And the army panel, which is where the player is already looking, carries the cure. */
    @Test
    void theArmyPanelSaysWhatToDoAboutAnEmptyEnemy() {
        final CombatScenario scenario = lannisport();
        final ArmySim empty = find(scenario, "Colin Florent");

        final String text =
                BattleSimConverter.getFightsIn(scenario.getParticipation().get(empty));

        assertTrue(text.contains("no troops visible in it"), "the diagnosis stays: " + text);
        assertTrue(text.contains("Add platoon"), "and now the cure is beside it: " + text);
    }

    /**
     * An army that CAN be counted gets no instruction, because it has no problem to fix.
     *
     * Jaime is stopped by "no enemy present", which is a consequence of the other army being empty
     * and not something he does anything about. A cure printed on every row would be noise, and
     * noise on every row is how the one that matters gets skipped - which is how this started.
     */
    /**
     * The roster row FLAGS the army, so the blocker is visible without selecting anything.
     *
     * An unscouted army used to render as a bare name with nothing after it, next to rows carrying
     * "29,259/78,665" - which reads as a rendering fault, not as missing intelligence. Twice the
     * conclusion drawn was that the window was broken.
     */
    @Test
    void theRosterFlagsAnArmyThePlayerCannotCount() {
        final CombatScenario scenario = lannisport();
        final ArmySim empty = find(scenario, "Colin Florent");
        final ArmySim jaime = find(scenario, "Jaime Lannister");

        final String flagged = control.services.BattleSimConverter.getArmyTitle(
                empty, scenario.getParticipation().get(empty));
        final String plain = control.services.BattleSimConverter.getArmyTitle(
                jaime, scenario.getParticipation().get(jaime));

        assertTrue(flagged.startsWith("❓"),
                "the mark leads the row so a column can be read down: " + flagged);
        assertFalse(plain.startsWith("❓"),
                "an army with troops in it carries no mark: " + plain);
    }

    /**
     * Every row keeps its three layer slots, INCLUDING the rows that fight nowhere.
     *
     * The block used to be suppressed for an army in no layer, on the argument that three dots
     * carry no information. The FFA hex is where that argument breaks: before anybody declares a
     * war no army engages at all, so every row lost its block at the same time and the player was
     * left with a disabled Run button and nothing on screen connected to it - "run simulation was
     * disabled but no glyphs". 0452 is the same shape: neither army engages, so both rows must
     * still show the slots that say so.
     */
    @Test
    void anArmyThatFightsNowhereStillShowsItsThreeLayerSlots() {
        final CombatScenario scenario = lannisport();
        final ArmySim empty = find(scenario, "Colin Florent");
        final ArmySim jaime = find(scenario, "Jaime Lannister");

        assertFalse(scenario.getParticipation().get(jaime).isInAnyLayer(),
                "the premise: on this hex nobody engages at all");

        final String idle = BattleSimConverter.getArmyTitle(
                jaime, scenario.getParticipation().get(jaime));
        final String unseen = BattleSimConverter.getArmyTitle(
                empty, scenario.getParticipation().get(empty));

        // three NOT_IN_LAYER marks, sea then land then city, in the slots that never move
        assertTrue(idle.contains("[· · ·]"),
                "an army in no layer says so in all three slots: " + idle);
        assertTrue(unseen.contains("[· · ·]"),
                "and so does the one nobody can see into: " + unseen);
    }

    /** And the row says what the player WAS told, rather than going blank. */
    @Test
    void anUncountableArmyShowsItsReportedBandInsteadOfNothing() {
        final CombatScenario scenario = lannisport();
        final ArmySim empty = find(scenario, "Colin Florent");
        empty.setSizeBand("huge navy");
        empty.setSizeBandLand("vast army");

        final String title = control.services.BattleSimConverter.getArmyTitle(
                empty, scenario.getParticipation().get(empty));

        assertTrue(title.contains("vast army"),
                "the LAND band, which for a fleet is the force it can put ashore: " + title);
    }

    @Test
    void anArmyWithNothingToFixIsNotGivenAnInstruction() {
        final CombatScenario scenario = lannisport();
        final ArmySim jaime = find(scenario, "Jaime Lannister");

        final String text =
                BattleSimConverter.getFightsIn(scenario.getParticipation().get(jaime));

        assertTrue(text.contains("no enemy present"), "his own diagnosis is unchanged: " + text);
        assertFalse(text.contains("Add platoon"),
                "but there is nothing for HIM to add: " + text);
    }
}
