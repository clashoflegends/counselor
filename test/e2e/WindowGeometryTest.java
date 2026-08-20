package e2e;

import gui.services.WindowGeometry;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A window remembered on a monitor that is later unplugged opens where the player cannot see or drag
 * it. The decision is deliberately a pure function so it can be pinned here: CI is headless, and
 * anything touching GraphicsEnvironment throws there.
 *
 * The dangerous failure is not the offscreen window, it is a false negative: almost every player is
 * single-monitor with a perfectly good saved position, and relocating THOSE windows on every launch
 * would be a far more widely felt bug than the one being fixed. Most of these tests guard that side.
 */
class WindowGeometryTest {

    /** Laptop panel, taskbar already subtracted. */
    private static final Rectangle LAPTOP = new Rectangle(0, 0, 1920, 1040);
    /** A second monitor to the RIGHT of it. */
    private static final Rectangle RIGHT = new Rectangle(1920, 0, 1920, 1040);
    /** A second monitor to the LEFT, which gives legitimately NEGATIVE coordinates. */
    private static final Rectangle LEFT = new Rectangle(-1920, 0, 1920, 1040);

    private static List<Rectangle> laptopOnly() {
        return Collections.singletonList(new Rectangle(LAPTOP));
    }

    // --- the reported bug ---

    @Test
    void positionOnAnUnpluggedRightMonitorIsUnreachable() {
        assertFalse(WindowGeometry.isReachable(new Rectangle(2400, 300, 500, 400), laptopOnly()),
                "saved on the second monitor, which is now gone");
    }

    @Test
    void theSamePositionIsFineWhileThatMonitorIsStillAttached() {
        assertTrue(WindowGeometry.isReachable(new Rectangle(2400, 300, 500, 400),
                Arrays.asList(new Rectangle(LAPTOP), new Rectangle(RIGHT))));
    }

    // --- false negatives, the expensive kind of mistake ---

    @Test
    void anOrdinarySinglePlayerPositionIsLeftAlone() {
        assertTrue(WindowGeometry.isReachable(new Rectangle(300, 200, 500, 400), laptopOnly()));
    }

    @Test
    void negativeCoordinatesAreValidOnAMonitorLeftOfPrimary() {
        assertTrue(WindowGeometry.isReachable(new Rectangle(-1500, 200, 500, 400),
                Arrays.asList(new Rectangle(LEFT), new Rectangle(LAPTOP))),
                "a left-hand monitor gives negative origins; rejecting them would break a working setup");
    }

    @Test
    void theVeryTopLeftPixelIsReachable() {
        assertTrue(WindowGeometry.isReachable(new Rectangle(0, 0, 500, 400), laptopOnly()));
    }

    @Test
    void aNarrowWindowIsJudgedOnItsOwnWidth() {
        //40px wide: the grab strip can never be 60px, so the rule must not demand it.
        assertTrue(WindowGeometry.isReachable(new Rectangle(300, 200, 40, 400), laptopOnly()));
    }

    // --- the L-shaped desktop, which a union rectangle gets wrong ---

    @Test
    void deadSpaceInAnLShapedDesktopIsNotReachable() {
        //Laptop 1920x1040 beside a taller 4K panel. The bounding box of the two covers y=1500 at
        //x=200, but no actual screen does.
        final List<Rectangle> screens = Arrays.asList(
                new Rectangle(LAPTOP), new Rectangle(1920, 0, 3840, 2160));
        assertFalse(WindowGeometry.isReachable(new Rectangle(200, 1500, 500, 400), screens),
                "inside the union bounding box but off every real screen");
        assertTrue(WindowGeometry.isReachable(new Rectangle(2200, 1500, 500, 400), screens),
                "same height, but this one is genuinely on the tall panel");
    }

    // --- partly off an edge ---

    @Test
    void aTitleBarAboveTheTopEdgeCannotBeGrabbed() {
        assertFalse(WindowGeometry.isReachable(new Rectangle(300, -20, 500, 400), laptopOnly()));
    }

    @Test
    void aWindowHangingOffTheRightEdgeKeepsEnoughToGrab() {
        //100px of the window still on screen, more than the 60px grab strip.
        assertTrue(WindowGeometry.isReachable(new Rectangle(1820, 300, 500, 400), laptopOnly()));
    }

    @Test
    void aWindowWithOnlySliverShowingIsUnreachable() {
        assertFalse(WindowGeometry.isReachable(new Rectangle(1900, 300, 500, 400), laptopOnly()),
                "20px of title bar is not enough to grab");
    }

    @Test
    void aWindowBelowTheTaskbarIsUnreachable() {
        //Inside the raw 1080-high screen, but below the usable area once the taskbar is subtracted.
        assertFalse(WindowGeometry.isReachable(new Rectangle(300, 1060, 500, 400), laptopOnly()));
    }

    // --- corrupt config ---

    @Test
    void theParseFailureSentinelIsTreatedAsUnreachable() {
        //SysApoio.parseInt answers -9999 for anything unparseable, so a damaged properties.config
        //would otherwise place the window permanently out of reach.
        assertFalse(WindowGeometry.isReachable(new Rectangle(-9999, -9999, 500, 400), laptopOnly()));
    }

    // --- nothing to judge against ---

    @Test
    void noScreensMeansCannotJudge() {
        assertFalse(WindowGeometry.isReachable(new Rectangle(300, 200, 500, 400), Collections.emptyList()));
        assertFalse(WindowGeometry.isReachable(new Rectangle(300, 200, 500, 400), null));
        assertFalse(WindowGeometry.isReachable(null, laptopOnly()));
    }

    @Test
    void usableScreensNeverThrowsAndIsEmptyWhenHeadless() {
        final List<Rectangle> screens = WindowGeometry.usableScreens();
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            assertTrue(screens.isEmpty(), "headless must report nothing to judge against");
        }
        for (Rectangle r : screens) {
            assertTrue(r.width > 0 && r.height > 0, "a screen with no usable area should be dropped");
        }
    }
}
