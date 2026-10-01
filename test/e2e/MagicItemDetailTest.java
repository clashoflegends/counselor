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

    /**
     * The secondary-power TEXT is shown, because it is the field that actually carries data.
     *
     * Across games 88 and 96 - 67 magic items - every single one shipped an EMPTY habilidades map
     * while secundario was populated. A tab reading only the map prints a heading over nothing, which
     * is how this request came in.
     */
    @Test
    void theSecondaryPowerTextIsShownEvenWithNoPowerCodes() {
        final Artefato sword = item("Dawn", "A star-forged blade.");
        sword.setSecundario("Grants its bearer sight of hidden foes.");

        final String text = ArtefatoConverter.getDetailText(sword);

        assertTrue(text.contains(labels.getString("ITEM.SECONDARY")), "the heading: " + text);
        assertTrue(text.contains("Grants its bearer sight of hidden foes."),
                "and the text the scenario wrote: " + text);
    }

    /**
     * The scenario's "not written yet" placeholders stay out of the player's face.
     *
     * "History and details - To Be Defined" is what most of those 67 items carry, and in game 88 it
     * is in the history AND the secondary, so printing it raw would say it twice.
     */
    @Test
    void placeholderSecondariesAreSuppressed() {
        for (String placeholder : new String[]{"History and details - To Be Defined", "-", "  ", "TBD"}) {
            final Artefato sword = item("Dawn", "A star-forged blade.");
            sword.setSecundario(placeholder);
            final String text = ArtefatoConverter.getDetailText(sword);
            assertFalse(text.contains(labels.getString("ITEM.SECONDARY")),
                    "placeholder [" + placeholder + "] earned a heading: " + text);
        }
    }

    /**
     * With real powers present, the short secondary string is NOT printed beside them.
     *
     * Game 906 carries secundario "Bestow Good" on an item whose power is "Bestow Good Luck".
     * Printing both reads as a truncation bug, not as two facts.
     */
    @Test
    void theShortSecondaryStepsAsideForTheRealPowers() {
        final Artefato dagger = item("Obsidian Dagger", "Volcanic glass, older than the Wall.",
                power(";AGO;", "Bestow Good Luck"));
        dagger.setSecundario("Bestow Good");

        final String text = ArtefatoConverter.getDetailText(dagger);

        assertTrue(text.contains("Bestow Good Luck"), "the power is listed: " + text);
        assertEquals(1, text.split(java.util.regex.Pattern.quote("Bestow Good"), -1).length - 1,
                "the short restatement is printed as well, which reads as a truncation: " + text);
    }

    /**
     * A power's own modifiers are listed under it.
     *
     * Game 906's Obsidian Dagger: ;AGO; "Bestow Good Luck" with ;C50; "50% chance to activate" nested
     * inside it. The chance is what a player weighs when deciding whether to rely on the item.
     */
    @Test
    void aPowersModifiersAreListedUnderIt() {
        final Habilidade luck = power(";AGO;", "Bestow Good Luck");
        luck.addHabilidade(power(";C50;", "50% chance to activate"));
        final String text = ArtefatoConverter.getDetailText(
                item("Obsidian Dagger", "Volcanic glass.", luck));

        assertTrue(text.contains("Bestow Good Luck"), "the power: " + text);
        assertTrue(text.contains("50% chance to activate"), "and what modifies it: " + text);
        assertTrue(text.indexOf("Bestow Good Luck") < text.indexOf("50% chance to activate"),
                "the modifier belongs under its parent: " + text);
    }

    /** A rule identical to the power's own name is not printed twice. */
    @Test
    void aRuleThatOnlyRepeatsTheNameIsNotEchoed() {
        // DB.POWER.AGO is exactly "Bestow Good Luck", which is also the power's name here.
        final String text = ArtefatoConverter.getDetailText(
                item("Obsidian Dagger", "Volcanic glass.", power(";AGO;", "Bestow Good Luck")));

        assertEquals(1, text.split(java.util.regex.Pattern.quote("Bestow Good Luck"), -1).length - 1,
                "the rule repeats the name right under it: " + text);
    }

    /** A secondary that merely repeats the history is not printed twice. */
    @Test
    void aSecondaryThatRepeatsTheHistoryIsNotEchoed() {
        final Artefato sword = item("Dawn", "A star-forged blade.");
        sword.setSecundario("A star-forged blade.");

        final String text = ArtefatoConverter.getDetailText(sword);

        assertEquals(1, text.split(java.util.regex.Pattern.quote("A star-forged blade."), -1).length - 1,
                "the same sentence appears twice: " + text);
    }

    /** No item selected is not an error, and an item with no history is not either. */
    @Test
    void theEmptyCasesAreQuiet() {
        assertEquals("", ArtefatoConverter.getDetailText(null));
        final String text = ArtefatoConverter.getDetailText(item("Nameless", null));
        assertFalse(text.contains("null"), "a missing history is omitted, not printed: " + text);
    }
}
