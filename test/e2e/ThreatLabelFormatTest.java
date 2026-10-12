package e2e;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every Threat-column label formats with the arguments {@code CidadeConverter.getThreatMessage}
 * actually passes, <b>in all five bundles</b>.
 *
 * Same regression shape as {@link ReviewLabelFormatTest} and {@code WhatIfLabelFormatTest}: the
 * arguments live in Java and the specifiers in a properties file five translators edit, and
 * {@code String.format} takes varargs of Object, so a mismatch is valid Java that throws
 * {@code MissingFormatArgumentException} in front of a player.
 *
 * <b>Each language is loaded explicitly</b> rather than through {@code BundleManager}, which
 * resolves the JVM default - English on CI and on the maintainer's machine. The first version of
 * this test did exactly that, so its own stated premise, "a file five translators edit", was the one
 * thing it never checked.
 *
 * Two of the lines are POSITIONAL ({@code %1$s} to {@code %4$s}). Positional specifiers survive a
 * translator reordering the sentence, which is why they are used, but they also let a line reach for
 * an argument that is meaningless to it: {@code getAssumedTroop()} is null for a force whose
 * composition was counted, and {@code String.format} prints that as the text "null" rather than
 * throwing. So the assertions are on the OUTPUT, not merely on the absence of an exception.
 */
class ThreatLabelFormatTest {

    private static final String[] LANGS = {"", "pt", "es", "it", "ca"};

    /** The value this language defines itself, or null when it does not override the English. */
    private static String own(String lang, String key) {
        try {
            final ResourceBundle rb = ResourceBundle.getBundle("labels",
                    lang.isEmpty() ? Locale.ROOT : Locale.of(lang));
            return rb.containsKey(key) ? rb.getString(key) : null;
        } catch (MissingResourceException ex) {
            return null;
        }
    }

    private static List<String> everyTranslationOf(String key) {
        final List<String> ret = new ArrayList<>();
        for (String lang : LANGS) {
            final String text = own(lang, key);
            assertTrue(text != null, key + " is missing from labels"
                    + (lang.isEmpty() ? "" : "_" + lang)
                    + ".properties - did the bundled jar get resynced?");
            ret.add(text);
        }
        return ret;
    }

    /** The call CidadeConverter makes for a force whose composition was COUNTED. */
    @Test
    void theSeenLineFormatsInEveryLanguageAndNeverPrintsNull() {
        for (String text : everyTranslationOf("THREAT.TOOLTIP.SEEN")) {
            final String out = assertDoesNotThrow(
                    () -> String.format(text, "Ironborn Host", "Greyjoy", 1, ""));
            assertFalse(out.contains("null"), "a counted force has no assumed troop, so a line that "
                    + "reaches for the fourth argument prints the word null: " + text);
        }
    }

    /** And for one that was not counted, where that fourth argument is the whole point. */
    @Test
    void theAssumedLineFormatsInEveryLanguage() {
        for (String text : everyTranslationOf("THREAT.TOOLTIP.ASSUMED")) {
            final String out = assertDoesNotThrow(
                    () -> String.format(text, "Desconhecido", "Greyjoy", 2, "Heavy Cavalry"));
            assertTrue(out.contains("Heavy Cavalry"),
                    "the assumed troop must survive into the line, or a guess reads as a sighting");
            assertFalse(out.contains("null"), text);
        }
    }

    @Test
    void theCountedLinesTakeOneNumberInEveryLanguage() {
        for (String text : everyTranslationOf("THREAT.TOOLTIP.TITLE")) {
            assertDoesNotThrow(() -> String.format(text, 1));
        }
        for (String text : everyTranslationOf("THREAT.TOOLTIP.MORE")) {
            assertDoesNotThrow(() -> String.format(text, 7));
        }
    }

    /** The caveat and the headings take no arguments and must not grow any. */
    @Test
    void theArgumentlessLabelsStayArgumentlessInEveryLanguage() {
        for (String key : new String[]{"THREAT.COLUMN", "THREAT.TOOLTIP.CAVEAT",
            "MAP.THREAT.REACH", "MAP.THREAT.REACH.HINT"}) {
            for (String text : everyTranslationOf(key)) {
                assertDoesNotThrow(() -> String.format(text), key + " grew a format specifier");
            }
        }
    }
}
