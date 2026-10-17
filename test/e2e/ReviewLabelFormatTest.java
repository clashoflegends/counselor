package e2e;

import control.services.TurnReview;
import control.services.TurnReview.Kind;
import gui.services.TurnReviewDialog;
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

    private static final String[] LANGS = {"", "pt", "es", "it", "ca"};

    private static String get(String key) {
        final String ret = labels.getString(key);
        assertTrue(ret != null && !ret.startsWith("N/A (Missing Translation"),
                key + " is missing from labels.properties - did the bundled jar get resynced?");
        return ret;
    }

    /**
     * The same key in every bundle the client ships.
     *
     * {@code BundleManager} resolves the JVM default - English on CI and on the maintainer's
     * machine - so checking through it tested one of the five files that five translators edit.
     * {@code LabelsIntegrityTest} is a partial backstop but runs against PbmCommons SOURCE rather
     * than the jar the Counselor loads, and it does not catch a conversion-type change: a
     * translator turning {@code %s} into {@code %d} throws {@code IllegalFormatConversionException}
     * on a String argument. These labels are formatted inside {@code getValueAt} during PAINT, so
     * any of those is a crash-exit rather than a bad row.
     */
    private static java.util.List<String> everyTranslationOf(String key) {
        final java.util.List<String> ret = new java.util.ArrayList<>();
        for (String lang : LANGS) {
            try {
                final java.util.ResourceBundle rb = java.util.ResourceBundle.getBundle("labels",
                        lang.isEmpty() ? java.util.Locale.ROOT : java.util.Locale.of(lang));
                assertTrue(rb.containsKey(key), key + " missing from labels"
                        + (lang.isEmpty() ? "" : "_" + lang) + ".properties");
                ret.add(rb.getString(key));
            } catch (java.util.MissingResourceException ex) {
                throw new AssertionError("labels" + lang + " not on the classpath", ex);
            }
        }
        return ret;
    }

    /**
     * The argument count TurnReview supplies for each kind. Kept beside the assertion rather than
     * derived, so changing one without the other is the thing that fails.
     */
    private static int argsFor(Kind kind) {
        switch (kind) {
            case RECRUIT_SCOPE:
            case RECRUIT_CONDITIONAL:
            case RECRUIT_INCOMPLETE:
                return 0;
            case RECRUIT_ELIGIBILITY:
            case RECRUIT_GOLD:
                return 2;
            case RECRUIT_LIMIT:
            case RECRUIT_CAPACITY:
            case RECRUIT_PROVISION:
                return 3;
            case RECRUIT_MATERIAL:
                return 4;
            case RECRUIT_EQUIPMENT:
                return 5;
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

    /** Every kind, in every bundle, so adding one without a label or an argument count fails here. */
    @Test
    void everyFindingKindFormatsWithTheArgumentsItIsGiven() {
        for (Kind kind : Kind.values()) {
            final int wanted = argsFor(kind);
            for (String text : everyTranslationOf(keyFor(kind))) {
                assertEquals(wanted, specifiers(text),
                        keyFor(kind) + " must take exactly " + wanted + " arguments: " + text);
                final Object[] args = new Object[wanted];
                java.util.Arrays.fill(args, "x");
                assertDoesNotThrow(() -> String.format(text, args), keyFor(kind) + ": " + text);
            }
        }
    }

    /** The two summary lines, which take counts rather than names. */
    @Test
    void theSummaryLinesTakeTheirCounts() {
        for (String empty : everyTranslationOf("REVIEW.EMPTY")) {
            assertEquals(2, specifiers(empty), "orders and actors: " + empty);
            assertDoesNotThrow(() -> String.format(empty, 0, 0), empty);
        }
        for (String count : everyTranslationOf("REVIEW.COUNT")) {
            assertEquals(3, specifiers(count), "findings, orders and actors: " + count);
            assertDoesNotThrow(() -> String.format(count, 3, 20, 12), count);
        }
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

    /** The open-slot filter's own lines, in every bundle. */
    @Test
    void theFilterLinesFormatInEveryLanguage() {
        for (String text : everyTranslationOf("REVIEW.ONLYOPEN")) {
            assertEquals(1, specifiers(text), "a count of hidden actors: " + text);
            assertDoesNotThrow(() -> String.format(text, 12), text);
        }
        for (String text : everyTranslationOf("REVIEW.HIDDEN")) {
            assertEquals(1, specifiers(text), "a count of hidden actors: " + text);
            assertDoesNotThrow(() -> String.format(text, 12), text);
        }
        for (String key : new String[]{"REVIEW.FILTER.OPENSLOTS", "REVIEW.FILTER.OPENSLOTS.HINT"}) {
            for (String text : everyTranslationOf(key)) {
                assertEquals(0, specifiers(text), key + " takes no arguments: " + text);
            }
        }
    }

    /**
     * <b>"Nothing to flag" is never shown while the filter is hiding something.</b>
     *
     * The dialog exists to answer whether the turn is ready. A filter that empties the list has to
     * say the list was filtered, or the answer "I looked and found nothing" is given when the truth
     * is "you asked me not to show you the only thing I found". This is the same class of error as
     * a failed review rendering as a clean one, which this feature has now had twice.
     */
    @Test
    void anEmptyListNeverReadsAsCleanWhileRowsAreHidden() {
        assertEquals("REVIEW.ONLYOPEN", TurnReviewDialog.summaryKey(0, 7, false));
        assertEquals("REVIEW.EMPTY", TurnReviewDialog.summaryKey(0, 0, false));
        assertEquals("REVIEW.COUNT", TurnReviewDialog.summaryKey(3, 7, false));
        assertEquals("REVIEW.FAILED", TurnReviewDialog.summaryKey(0, 0, true),
                "a review that could not finish outranks every other line");
        assertEquals("REVIEW.FAILED", TurnReviewDialog.summaryKey(5, 7, true));
    }

    /**
     * Both halves of "nothing entered yet" hide together.
     *
     * {@code OPEN_PACKAGE} is the same statement about a nation with budget left; hiding one kind
     * while keeping the other would read as a bug, and nothing else may be swept up with them.
     */
    @Test
    void theFilterHidesBothUnplannedKindsAndNothingElse() {
        for (Kind kind : Kind.values()) {
            final boolean expected = kind == Kind.OPEN_SLOT || kind == Kind.OPEN_PACKAGE;
            assertEquals(expected, TurnReviewDialog.isOpenSlot(kind),
                    kind + " on the wrong side of the filter");
        }
    }
}
