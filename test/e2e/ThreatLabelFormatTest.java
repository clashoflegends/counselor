package e2e;

import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every Threat-column label formats with the arguments {@code CidadeConverter.getThreatMessage}
 * actually passes.
 *
 * Same regression shape as {@link ReviewLabelFormatTest} and {@code WhatIfLabelFormatTest}: the
 * arguments live in Java and the specifiers in a properties file five translators edit, and
 * {@code String.format} takes varargs of Object, so a mismatch is valid Java that throws
 * {@code MissingFormatArgumentException} in front of a player.
 *
 * Sharper here than usual because the two tooltip lines are POSITIONAL ({@code %1$s}, {@code %4$s}).
 * Positional specifiers survive a translator reordering the sentence, which is the whole reason to
 * use them, but it also means the fourth argument can be referenced by a line that is handed only
 * three meaningful ones - and the assumed-troop line is exactly that line.
 */
class ThreatLabelFormatTest {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private static String get(String key) {
        final String ret = labels.getString(key);
        assertTrue(ret != null && !ret.startsWith("N/A (Missing Translation"),
                key + " is missing from labels.properties - did the bundled jar get resynced?");
        return ret;
    }

    /** The call CidadeConverter makes for a force whose composition was counted. */
    @Test
    void theSeenLineFormatsWithTheArgumentsItIsGiven() {
        assertDoesNotThrow(() -> String.format(get("THREAT.TOOLTIP.SEEN"),
                "Ironborn Host", "Greyjoy", 1, null));
    }

    /**
     * And for one that was not. {@code getAssumedTroop()} is the fourth argument and is null on a
     * counted force, so a translator moving {@code %4$s} into the SEEN line would print "null" -
     * which is why both lines are formatted with the same four arguments and tested with them.
     */
    @Test
    void theAssumedLineFormatsWithTheArgumentsItIsGiven() {
        assertDoesNotThrow(() -> String.format(get("THREAT.TOOLTIP.ASSUMED"),
                "Desconhecido", "Greyjoy", 2, "Heavy Cavalry"));
    }

    @Test
    void theCountedLinesFormatWithOneNumber() {
        assertDoesNotThrow(() -> String.format(get("THREAT.TOOLTIP.TITLE"), 1));
        assertDoesNotThrow(() -> String.format(get("THREAT.TOOLTIP.MORE"), 7));
    }

    /** The caveat and the column header take no arguments and must not grow any. */
    @Test
    void theArgumentlessLabelsStayArgumentless() {
        for (String key : new String[]{"THREAT.COLUMN", "THREAT.TOOLTIP.CAVEAT",
            "MAP.THREAT.REACH", "MAP.THREAT.REACH.HINT"}) {
            final String text = get(key);
            assertDoesNotThrow(() -> String.format(text), key + " grew a format specifier");
        }
    }
}
