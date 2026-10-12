package e2e;

import gui.services.ScreenFit;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Counselor remembers the main window's size and position, which was written for one fixed
 * display and breaks whenever the display set changes between runs. For a docked laptop that is
 * most mornings.
 *
 * The reported case (John, 2026-10-11): closed at night on dual monitors, reopened in the morning on
 * the laptop alone, and <b>the main window came back oversized</b> - the remembered width was wider
 * than the only screen now attached. The sibling failure is a remembered x that points at a monitor
 * which is no longer there, leaving the window somewhere the player cannot see or drag.
 *
 * {@code ScreenFit.fit} is a pure function of the saved rectangle and the usable screen rectangles
 * precisely so these can be tested without a display. Coordinates below use a 1920x1040 laptop (the
 * taskbar already subtracted) and a 2560x1400 external monitor to its right.
 */
class ScreenFitTest {

    private static final Rectangle LAPTOP = new Rectangle(0, 0, 1920, 1040);
    private static final Rectangle EXTERNAL = new Rectangle(1920, 0, 2560, 1400);

    private static List<Rectangle> docked() {
        return Arrays.asList(LAPTOP, EXTERNAL);
    }

    private static List<Rectangle> laptopOnly() {
        return Collections.singletonList(LAPTOP);
    }

    private static void assertWithin(Rectangle got, Rectangle screen) {
        assertTrue(got.width <= screen.width && got.height <= screen.height,
                got + " is larger than " + screen);
        assertTrue(screen.contains(got), got + " is not wholly inside " + screen);
    }

    /** The reported bug: a window sized for the external monitor, reopened on the laptop alone. */
    @Test
    void anOversizedWindowIsShrunkToTheRemainingScreen() {
        final Rectangle saved = new Rectangle(100, 50, 2400, 1300);
        final Rectangle got = ScreenFit.fit(saved, laptopOnly());
        assertWithin(got, LAPTOP);
    }

    /** Its sibling: the monitor it was on has gone, so the coordinate means nothing. */
    @Test
    void aWindowOnADetachedMonitorComesBackCentredOnTheOneThatIsLeft() {
        final Rectangle saved = new Rectangle(2600, 300, 900, 700);
        final Rectangle got = ScreenFit.fit(saved, laptopOnly());
        assertWithin(got, LAPTOP);
        assertEquals((LAPTOP.width - got.width) / 2, got.x, "should be centred horizontally");
        assertEquals((LAPTOP.height - got.height) / 2, got.y, "should be centred vertically");
        assertEquals(900, got.width, "a window that already fits must keep its size");
        assertEquals(700, got.height);
    }

    /** Still docked: the second monitor is there, so the window must stay on it, untouched. */
    @Test
    void aWindowOnASecondMonitorIsLeftAloneWhileThatMonitorExists() {
        final Rectangle saved = new Rectangle(2000, 100, 1200, 900);
        assertEquals(saved, ScreenFit.fit(saved, docked()));
    }

    /** A window that already fits the primary screen must not be moved at all. */
    @Test
    void aWindowThatAlreadyFitsIsUnchanged() {
        final Rectangle saved = new Rectangle(40, 60, 1200, 800);
        assertEquals(saved, ScreenFit.fit(saved, laptopOnly()));
        assertEquals(saved, ScreenFit.fit(saved, docked()));
    }

    /** Hanging off the right edge: pulled back, not resized, because it still fits. */
    @Test
    void aWindowOverhangingAnEdgeIsNudgedBackRatherThanShrunk() {
        final Rectangle saved = new Rectangle(1500, 900, 800, 600);
        final Rectangle got = ScreenFit.fit(saved, laptopOnly());
        assertWithin(got, LAPTOP);
        assertEquals(800, got.width, "it fits, so the size must not change");
        assertEquals(600, got.height);
        assertEquals(LAPTOP.width - 800, got.x);
        assertEquals(LAPTOP.height - 600, got.y);
    }

    /**
     * Shrinking happens before repositioning. A window wider than the screen can never be placed
     * wholly inside it, so getting this order wrong would leave the title bar off the left edge and
     * the window undraggable - the exact state this class exists to prevent.
     */
    @Test
    void theTitleBarStaysReachableWhenTheWindowIsAsWideAsTheScreen() {
        final Rectangle saved = new Rectangle(-300, -200, 4000, 3000);
        final Rectangle got = ScreenFit.fit(saved, laptopOnly());
        assertEquals(LAPTOP.x, got.x);
        assertEquals(LAPTOP.y, got.y);
        assertWithin(got, LAPTOP);
    }

    /** A headless JVM reports no screens; guessing would be worse than honouring what was saved. */
    @Test
    void withNoScreensTheSavedGeometryIsReturnedUntouched() {
        final Rectangle saved = new Rectangle(2600, 300, 900, 700);
        assertSame(saved, ScreenFit.fit(saved, Collections.emptyList()));
        assertSame(saved, ScreenFit.fit(saved, null));
    }

    /** Never shrink to something the player cannot use, even from absurd saved values. */
    @Test
    void aTinySavedWindowIsBroughtUpToAUsableSize() {
        final Rectangle got = ScreenFit.fit(new Rectangle(10, 10, 40, 30), laptopOnly());
        assertTrue(got.width >= 640 && got.height >= 480, "too small to resize or drag: " + got);
        assertWithin(got, LAPTOP);
    }

    /**
     * A window the player deliberately stretched across two monitors comes back as they left it.
     *
     * Everything else in {@code fit} forces the window onto ONE screen, which is right for a window
     * that has outgrown its display and wrong for this one: it overlaps each monitor by less than
     * its own area, so {@code pickScreen} returns whichever it covers more and the clamp cuts it to
     * that monitor's width. Nothing about the display set changed, so nothing should move - and the
     * player would have had to re-stretch it on every single launch.
     */
    @Test
    void aWindowSpanningTwoMonitorsIsLeftAlone() {
        final Rectangle spanning = new Rectangle(0, 0, 3800, 1000);

        assertEquals(spanning, ScreenFit.fit(spanning, List.of(LAPTOP, EXTERNAL)),
                "it is wholly on screen; the single-screen clamp must not touch it");
    }

    /** But one hanging off the far edge is still pulled back. */
    @Test
    void aWindowHangingPastTheLastMonitorIsStillPulledBack() {
        final Rectangle overhanging = new Rectangle(0, 0, 5000, 1000);

        final Rectangle got = ScreenFit.fit(overhanging, List.of(LAPTOP, EXTERNAL));

        assertTrue(got.width < overhanging.width, "the part past the right-hand edge is invisible");
    }

    /** And a window in the gap between two mismatched monitors is not "wholly visible". */
    @Test
    void aWindowOverAGapBetweenMismatchedMonitorsIsRefitted() {
        //EXTERNAL is 1400 tall, LAPTOP 1040: a tall window spanning both hangs into dead space
        final Rectangle intoTheGap = new Rectangle(1000, 0, 3000, 1300);

        assertNotEquals(intoTheGap, ScreenFit.fit(intoTheGap, List.of(LAPTOP, EXTERNAL)));
    }
}
