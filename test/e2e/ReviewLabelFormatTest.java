package e2e;

import control.services.TurnReview.Kind;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every Review-turn row formats with the arguments the dialog actually passes for its kind.
 *
 * Same regression shape as {@link WhatIfLabelFormatTest}, which exists because crash 327108 shipped a
 * label whose specifier did not match the argument beside it. A compiler cannot help:
 * {@code String.format} takes varargs of Object, so a three-argument call against a four-specifier
 * label is valid Java that throws {@code MissingFormatArgumentException} in front of a player. Here
 * the risk is sharper still, because the ARGUMENTS live in {@code TurnReview} and the SPECIFIERS live
 * in a properties file that five translators edit, so the two halves can drift with nothing in the
 * way.
 *
 * Extra arguments are harmless, missing ones throw, which is why the English count is the contract
 * and why PbmCommons' own {@code LabelsIntegrityTest.noTranslationHasMoreArgumentsThanTheEnglish}
 * is the matching guard on the other four bundles.
 */
class ReviewLabelFormatTest {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    /** Same pattern as WhatIfLabelFormatTest: no space in the flag class, see its note. */
    private static final Pattern SPECIFIER = Pattern.compile("%(?!%)[-#+0,(]*\\d*(?:\\.\\d+)?([a-zA-Z])");

    private static int specifiers(String text) {
        final Matcher matcher = SPECIFIER.matcher(text);
        int ret = 0;
        while (matcher.find()) {
            ret++;
        }
        return ret;
    }

    private static String get(String key) {
        final String ret = labels.getString(key);
        assertTrue(ret != null && !ret.startsWith("N/A (Missing Translation"),
                key + " is missing from labels.properties - did the bundled jar get resynced?");
        return ret;
    }

    /**
     * The argument count TurnReview supplies for each kind. Kept beside the assertion rather than
     * derived, so changing one without the other is the thing that fails.
     */
    private static int argsFor(Kind kind) {
        switch (kind) {
            case OPEN_SLOT:
            case OPEN_PACKAGE:
                return 1;                   // the actor's name
            case SIZE_CAP:
                return 3;                   // city, blocking city, its hex
            case RENDEZVOUS:
                return 4;                   // mover, its destination, target, the target's destination
            case CONVERGE:
                return 2;                   // the hex, and the names walking into it
            default:
                throw new AssertionError("unmapped kind " + kind);
        }
    }

    private static String keyFor(Kind kind) {
        return "REVIEW." + kind.name();
    }

    /** Every kind, so adding one without a label or an argument count fails here. */
    @Test
    void everyFindingKindFormatsWithTheArgumentsItIsGiven() {
        for (Kind kind : Kind.values()) {
            final String text = get(keyFor(kind));
            final int wanted = argsFor(kind);
            assertEquals(wanted, specifiers(text),
                    keyFor(kind) + " must take exactly " + wanted + " arguments: " + text);
            final Object[] args = new Object[wanted];
            java.util.Arrays.fill(args, "x");
            assertDoesNotThrow(() -> String.format(text, args), keyFor(kind));
        }
    }

    /** The two summary lines, which take counts rather than names. */
    @Test
    void theSummaryLinesTakeTheirCounts() {
        final String empty = get("REVIEW.EMPTY");
        assertEquals(2, specifiers(empty), "orders and actors: " + empty);
        assertDoesNotThrow(() -> String.format(empty, 0, 0));

        final String count = get("REVIEW.COUNT");
        assertEquals(3, specifiers(count), "findings, orders and actors: " + count);
        assertDoesNotThrow(() -> String.format(count, 3, 20, 12));
    }

    /**
     * The chrome takes nothing. A specifier added to one of these throws, because the dialog shows
     * them with no formatting at all.
     */
    @Test
    void theChromeTakesNoArguments() {
        for (String key : new String[]{"REVIEW.TITLE", "REVIEW.TOOLBAR", "REVIEW.TOOLTIP",
            "REVIEW.HINT", "REVIEW.COL.ACTOR", "REVIEW.COL.SLOT", "REVIEW.COL.MESSAGE"}) {
            assertEquals(0, specifiers(get(key)), key + " must take no arguments");
        }
    }
}
