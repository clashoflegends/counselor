package e2e;

import control.services.ArtefatoConverter;
import model.Artefato;
import model.Habilidade;
import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A magic item explains itself, wherever it is shown. T-850.
 *
 * <h3>The request</h3>
 *
 * A player, 2026-09-30: "when a character is carrying a magic item, it would be useful if the tab
 * that contains magic items for that character also contains the help information for that magic
 * item. Things like dormant powers and whether or not it can move would be good to see instantly
 * instead of looking them up."
 *
 * <h3>What is pinned</h3>
 *
 * That the text carries the history and the dormant powers the Magic Items tab already showed, and
 * that each power now brings its own RULE with it. The second part is the whole point: a line
 * reading "Immovable" answers only somebody who already knows what it means, and the rule that makes
 * it an answer - "cannot be moved" - lives in {@code DB.POWER.IMN}, which until now nothing read.
 *
 * <p>
 * The lookup is also the fragile part. Codes arrive wrapped as {@code ;IMN;} while the bundle keys
 * are bare, and a code with no entry has to stay silent rather than print
 * "N/A (Missing Translation: ...)" - which is what {@code BundleManager.getString} answers on a
 * miss, after logging FATAL.
 */
class MagicItemDetailTest {

    private static final BundleManager labels = new BundleManager();

    private static Habilidade power(String codigo, String nome) {
        final Habilidade ret = new Habilidade();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    private static Artefato item(String nome, String historia, Habilidade... powers) {
        final Artefato ret = new Artefato();
        ret.setCodigo(nome);
        ret.setNome(nome);
        ret.setPrimario("Combat");
        ret.setValor(25);
        ret.setDescricao("Sword");
        ret.setHistoria(historia);
        for (Habilidade one : powers) {
            ret.addHabilidade(one);
        }
        return ret;
    }

    @Test
    void theHistoryAndThePrimaryPowerAreBothThere() {
        final String text = ArtefatoConverter.getDetailText(
                item("Dawn", "Forged from the heart of a fallen star."));

        assertTrue(text.contains("Forged from the heart of a fallen star."),
                "the item's history: " + text);
        assertTrue(text.contains("Combat") && text.contains("25"),
                "and what it does, with its value: " + text);
        assertTrue(text.contains("Sword"), "and what kind of thing it is: " + text);
    }

    /** The one the player actually asked for. */
    @Test
    void aDormantPowerBringsItsOwnRuleWithIt() {
        final String text = ArtefatoConverter.getDetailText(
                item("Dawn", "Forged from a fallen star.", power(";IMN;", "Immovable")));

        assertTrue(text.contains(labels.getString("ITEM.SECONDARY")),
                "the dormant-powers heading: " + text);
        assertTrue(text.contains("Immovable"), "the power's name: " + text);
        // DB.POWER.IMN - asserted against the RESOLVED bundle value rather than a copy of its
        // English, so rewording the rule does not turn this into a test that passes by matching
        // nothing.
        assertTrue(text.contains(labels.getString("DB.POWER.IMN")),
                "and the rule that makes the name an answer: " + text);
    }

    /** Several powers, each with its own rule, none swallowing another. */
    @Test
    void everyPowerGetsItsOwnLine() {
        final String text = ArtefatoConverter.getDetailText(
                item("Dawn", "A star-forged blade.",
                        power(";IMN;", "Immovable"), power(";AH;", "Histories")));

        assertTrue(text.contains(labels.getString("DB.POWER.IMN")), "first rule: " + text);
        assertTrue(text.contains(labels.getString("DB.POWER.AH")), "second rule: " + text);
    }

    /**
     * A code the bundle has never heard of stays silent.
     *
     * Going through BundleManager would have printed "N/A (Missing Translation: DB.POWER.ZZZZ)" into
     * the player's item description and logged it FATAL. An optional footnote is not a missing label.
     */
    @Test
    void anUnknownPowerCodePrintsNoApology() {
        final String text = ArtefatoConverter.getDetailText(
                item("Dawn", "A star-forged blade.", power(";ZZZZ;", "Mystery")));

        assertTrue(text.contains("Mystery"), "the power is still listed: " + text);
        assertFalse(text.contains("Missing Translation"),
                "but no apology leaks into the item description: " + text);
        assertFalse(text.contains("N/A"), "nor an N/A: " + text);
    }

    /** An item with no dormant powers gets no heading for them. */
    @Test
    void anItemWithNoDormantPowersSaysNothingAboutThem() {
        final String text = ArtefatoConverter.getDetailText(item("Plain Sword", "Nothing special."));

        assertFalse(text.contains(labels.getString("ITEM.SECONDARY")),
                "an empty heading is worse than no heading: " + text);
        assertTrue(text.contains("Nothing special."), "the history is still there: " + text);
    }

    /** No item selected is not an error, and an item with no history is not either. */
    @Test
    void theEmptyCasesAreQuiet() {
        assertEquals("", ArtefatoConverter.getDetailText(null));
        final String text = ArtefatoConverter.getDetailText(item("Nameless", null));
        assertFalse(text.contains("null"), "a missing history is omitted, not printed: " + text);
    }
}
