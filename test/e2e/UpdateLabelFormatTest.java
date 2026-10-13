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
 * Every About-box update label exists and formats in all five bundles, with the arguments
 * {@code MainAboutBox} actually passes.
 *
 * Same regression shape as {@link ThreatLabelFormatTest} and {@link ReviewLabelFormatTest}: the
 * arguments live in Java and the specifiers in a properties file five translators edit, so a
 * mismatch is valid Java that throws in front of a player. These two lines are the ones a player
 * sees after pressing a button they pressed on purpose, which is the worst moment to show them a
 * stack trace.
 *
 * Each language is loaded explicitly rather than through {@code BundleManager}, which resolves the
 * JVM default - English on CI and on the maintainer's machine, so a bundle-wide test would never
 * read the four translations at all.
 */
class UpdateLabelFormatTest {

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

    /** The button text once a check has found a newer release. */
    @Test
    void theDownloadButtonFormatsInEveryLanguage() {
        for (String text : everyTranslationOf("ABOUT.UPDATE.LINK")) {
            final String out = assertDoesNotThrow(() -> String.format(text, "2.935"));
            assertTrue(out.contains("2.935"), "the version is the whole point of the line: " + text);
        }
    }

    /** The reassuring answer, which carries the version the player is actually running. */
    @Test
    void theUpToDateLineFormatsInEveryLanguage() {
        for (String text : everyTranslationOf("ABOUT.UPDATE.CURRENT")) {
            final String out = assertDoesNotThrow(() -> String.format(text, "2.934"));
            assertTrue(out.contains("2.934"), "the running version is the whole point of the line: " + text);
            assertFalse(out.contains("null"), text);
        }
    }

    /** The two argument-free lines still have to EXIST in every bundle, or a FATAL is logged. */
    @Test
    void theArgumentFreeLinesExistInEveryLanguage() {
        for (String key : new String[]{"ABOUT.UPDATE.CHECK", "ABOUT.UPDATE.CHECKING", "ABOUT.UPDATE.FAILED"}) {
            for (String text : everyTranslationOf(key)) {
                assertFalse(text.trim().isEmpty(), key + " is present but empty");
                assertFalse(text.contains("%"), key + " takes no arguments but carries a specifier: " + text);
            }
        }
    }
}
