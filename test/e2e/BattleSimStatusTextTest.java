package e2e;

import business.combat.ArmySim;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.services.BattleSimConverter;
import java.util.List;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The BattleSim's two status lines, against the ways a LABEL breaks them silently.
 *
 * Neither of these can be caught by reading the properties file. A label key that no longer exists
 * does not throw: {@code BundleManager.getString} logs it and returns
 * "N/A (Missing Translation: ...)", which the player reads in the status bar. And a label that
 * carries its own leading space loses it to {@code Properties.load} before anybody sees it, in every
 * language at once, so the gap between two sentences has to come from the code that joins them.
 */
class BattleSimStatusTextTest {

    private static final BundleManager LABELS =
            SettingsManager.getInstance().getBundleManager();

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
        ret.setCodigo("0350");
        ret.setCoordenadas("0350");
        ret.setTerreno(terreno);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Local hex) {
        final TipoTropa tipo = new TipoTropa();
        tipo.setCodigo("inf");
        tipo.setNome("inf");
        final Pelotao pelotao = new Pelotao();
        pelotao.setTipoTropa(tipo);
        pelotao.setQtd(900);
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome);
        ret.setLocal(hex);
        ret.getPelotoes().put("inf", pelotao);
        return ret;
    }

    /**
     * Mine, a foe I have read, and a third party nobody can place: one assumed pair at least.
     *
     * The edit is put on MY pair with the foe, which is a read cell rather than an assumed one, so
     * the two counts stay independent and the status bar has to print both sentences.
     */
    private static CombatScenario threeWay(boolean edited) {
        final Jogador me = new Jogador();
        me.setCodigo("j1");
        me.setNome("me");
        final Nacao mine = nacao("m", "Mine"), foe = nacao("f", "Foe"), third = nacao("x", "Third");
        mine.setOwner(me);
        mine.getRelacionamentos().put(foe, RelationshipMatrix.SWORN_ENEMY);

        final Local hex = hex();
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.setObserver(me);
        ret.addArmy(army("mine", mine, hex), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("foe", foe, hex), CombatScenario.Provenance.ESTIMATED);
        ret.addArmy(army("third", third, hex), CombatScenario.Provenance.ESTIMATED);
        if (edited) {
            ret.setRelacionamento(mine, foe, RelationshipMatrix.SWORN_ENEMY);
        }
        return ret;
    }

    /**
     * Just me and a foe I have read, with one edit: nothing is assumed, so EDITED prints ALONE.
     *
     * Which is the usual case, and the one the label's own leading space would have indented.
     */
    private static CombatScenario editedOnly() {
        final Jogador me = new Jogador();
        me.setCodigo("j1");
        me.setNome("me");
        final Nacao mine = nacao("m", "Mine"), foe = nacao("f", "Foe");
        mine.setOwner(me);
        mine.getRelacionamentos().put(foe, RelationshipMatrix.SWORN_ENEMY);

        final Local hex = hex();
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.setObserver(me);
        ret.addArmy(army("mine", mine, hex), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("foe", foe, hex), CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(mine, foe, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    /** A run that resolved nothing: the hex engaged on no land layer at all. */
    private static CombatResult noRounds() {
        final CombatResult ret = new CombatResult();
        ret.setRounds(0);
        return ret;
    }

    /** The two status sentences are separated by the CODE, and do not run together. */
    @Test
    void theTwoStatusSentencesAreSeparated() {
        final String text = BattleSimConverter.getDerivationText(threeWay(true));
        final String edited = LABELS.getString("BATTLESIM.STATUS.EDITED");

        final int at = text.indexOf(edited.substring(0, edited.indexOf('%')));
        assertTrue(at > 0, "both sentences must be present: " + text);
        assertTrue(Character.isWhitespace(text.charAt(at - 1)),
                "a gap belongs between them, and it comes from the code: " + text);
    }

    /**
     * And alone, it does not open with one.
     *
     * This is what the label's own leading space would have produced had it ever survived the
     * loader: an indented sentence in the usual case, where nothing precedes it.
     */
    @Test
    void theEditedSentenceAloneIsNotIndented() {
        final CombatScenario scenario = editedOnly();

        assertEquals(0, scenario.getAssumedCount(),
                "the premise: nothing is guessed here, so EDITED prints on its own");
        assertEquals(1, scenario.getEditedCount(), "and there is exactly one override to report");

        final String text = BattleSimConverter.getDerivationText(scenario);

        assertEquals(String.format(LABELS.getString("BATTLESIM.STATUS.EDITED"), 1), text,
                "the sentence and nothing else - no label carries its own indent");
    }

    /**
     * Zero rounds says the same sentence in the status bar and in the results dialog.
     *
     * They were two separate keys holding byte-identical English, reached from the same predicate on
     * the same result. One fact gets one sentence; two copies only means one of them gets fixed.
     */
    @Test
    void noLandBattleReadsTheSameInBothPlaces() {
        final CombatResult result = noRounds();
        final List<String> verdict = BattleSimConverter.getVerdictLines(threeWay(false), result);
        final String expected = LABELS.getString("BATTLESIM.RESULT.NOLANDBATTLE");

        assertEquals(1, verdict.size(), "nothing happened, so there is one line to say so");
        assertEquals(expected, verdict.get(0));
        assertTrue(BattleSimConverter.getRunResultText(result).contains(expected),
                "the status bar says it too, from the same key");
    }

    /** And the key resolves: a missing label is returned, not thrown, so it has to be asserted. */
    @Test
    void theNoLandBattleKeyResolves() {
        final String text = LABELS.getString("BATTLESIM.RESULT.NOLANDBATTLE");

        assertFalse(text.startsWith("N/A"), "missing label: " + text);
        assertFalse(text.isEmpty(), "and it has to actually say something");
    }
}
