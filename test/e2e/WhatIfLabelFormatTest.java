package e2e;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The what-if answers format with the arguments the dialog actually passes.
 *
 * <h3>The crash this is the regression for</h3>
 *
 * Crash 327108 (game 911, Counselor 930). {@code WhatIfDialog.sentence} passed the troop NAME as
 * the second argument to a label whose second specifier is {@code %,d}, and {@code String.format}
 * throws {@code IllegalFormatConversionException} rather than coercing. It fired on the SUCCESS
 * path - whenever the search found an answer - so the release's headline feature crashed for
 * everyone who used it and worked only when it failed to find anything.
 *
 * <h3>Why a LABEL test rather than a dialog test</h3>
 *
 * Because the label is the half that can change without anyone reading the Java. There are five
 * translations of each of these, and a translator who drops a specifier or adds one reintroduces
 * exactly this crash with no compiler and no reviewer in the way. Checking the SHAPE of the label
 * catches that; checking the dialog only catches an English regression.
 *
 * A compiler cannot help here at all: {@code String.format} takes varargs of Object, so passing
 * three arguments to a two-specifier format is valid Java and fails at runtime, in front of a
 * player.
 */
public class WhatIfLabelFormatTest {

    private static final BundleManager labels =
            SettingsManager.getInstance().getBundleManager();
    /** Any conversion: %d, %,d, %s and friends. Doubled %% is an escape and is not one. */
    private static final Pattern SPECIFIER = Pattern.compile("%(?!%)[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z])");

    private static int specifiers(String text) {
        final Matcher matcher = SPECIFIER.matcher(text);
        int ret = 0;
        while (matcher.find()) {
            ret++;
        }
        return ret;
    }

    private static String conversions(String text) {
        final Matcher matcher = SPECIFIER.matcher(text);
        final StringBuilder ret = new StringBuilder();
        while (matcher.find()) {
            ret.append(matcher.group(1));
        }
        return ret.toString();
    }

    /** Two integers: the threshold, then what the player has now. Nothing else. */
    @Test
    public void theFoundAnswersTakeExactlyTwoIntegers() {
        for (String key : new String[]{"BATTLESIM.WHATIF.EXACT", "BATTLESIM.WHATIF.ATLEAST"}) {
            final String text = labels.getString(key);
            assertEquals(2, specifiers(text), key + " must take exactly two arguments: " + text);
            assertEquals("dd", conversions(text), key + " must take two INTEGERS: " + text);
            assertDoesNotThrow(() -> String.format(text, 1850, 10), key);
        }
    }

    /** One integer: the ceiling that was searched to. */
    @Test
    public void theUnreachableAnswersTakeExactlyOneInteger() {
        for (String key : new String[]{"BATTLESIM.WHATIF.NONE", "BATTLESIM.WHATIF.STALEMATE"}) {
            final String text = labels.getString(key);
            assertEquals(1, specifiers(text), key + " must take exactly one argument: " + text);
            assertEquals("d", conversions(text), key + " must take an INTEGER: " + text);
            assertDoesNotThrow(() -> String.format(text, 200000), key);
        }
    }

    /** And the trial count, which is the one the crash did not reach. */
    @Test
    public void theTrialCountTakesOneInteger() {
        final String text = labels.getString("BATTLESIM.WHATIF.TRIALS");

        assertEquals(1, specifiers(text), text);
        assertEquals("d", conversions(text), text);
        assertDoesNotThrow(() -> String.format(text, 21));
    }

    /**
     * The answers that take NO arguments must take none.
     *
     * The mirror of the same mistake: a specifier added to one of these would throw
     * MissingFormatArgumentException, because the dialog appends them with no formatting at all.
     */
    @Test
    public void thePlainAnswersTakeNoArguments() {
        for (String key : new String[]{"BATTLESIM.WHATIF.ALREADY", "BATTLESIM.WHATIF.UNVERIFIED",
            "BATTLESIM.WHATIF.FLOOR", "BATTLESIM.WHATIF.CEILINGNOTE",
            "BATTLESIM.WHATIF.PROMPT"}) {
            final String text = labels.getString(key);
            assertEquals(0, specifiers(text), key + " is appended unformatted: " + text);
        }
    }

    /**
     * The focus line after a fill takes a string and an integer, IN THAT ORDER.
     *
     * Same family of bug, same dialog-adjacent code, and it has never been exercised by a test.
     */
    @Test
    public void theFillFocusLineTakesANameThenACount() {
        final String text = labels.getString("BATTLESIM.FILL.FOCUS");

        assertEquals("sd", conversions(text), text);
        assertDoesNotThrow(() -> String.format(text, "Colin Florent", 3357));
        assertFalse(text.isEmpty());
    }
}
