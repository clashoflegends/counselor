/*
 * ScreenFit.java
 */
package gui.services;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes a remembered window rectangle fit the screens the machine has RIGHT NOW.
 *
 * The Counselor saves the main window's size and position on close and restores them on start
 * ({@code PbmApplication.saveFrameConfigs}). That was written for one fixed display and breaks the
 * moment the display set changes between runs, which for a docked laptop is most mornings:
 * <ul>
 * <li><b>oversized</b> - closed on a 2560-wide external monitor, reopened on the laptop panel, the
 *     remembered width is wider than the screen. This is the reported case;</li>
 * <li><b>off-screen</b> - a remembered x of 2560 points at a monitor that is no longer attached, so
 *     the window is restored somewhere the user cannot see or reach it.</li>
 * </ul>
 *
 * There is ONE saved slot, not one per display arrangement, so docked and undocked overwrite each
 * other. Fixing that properly means keying the geometry by a screen signature; this class does the
 * smaller, unconditional thing instead - whatever was remembered, make it visible and usable on the
 * screens that exist. A player never sees a window they cannot resize or drag.
 *
 * The geometry is a pure function of the saved rectangle and the usable screen rectangles, so it is
 * unit-tested without a display. Only {@link #usableScreens()} touches AWT.
 *
 * @author jmoura
 */
public final class ScreenFit implements Serializable {

    /** Below this a window is not usefully resizable or draggable, whatever the screen allows. */
    static final int MIN_WIDTH = 640;
    static final int MIN_HEIGHT = 480;

    private ScreenFit() {
    }

    /**
     * The remembered rectangle, moved and shrunk as little as necessary to sit wholly inside one of
     * {@code screens}.
     *
     * Picks the screen the window most overlaps, so a window left on the second monitor comes back
     * to the second monitor when that monitor is still there. With no overlap at all - the display
     * it was on has gone - it falls back to the first screen, which {@link #usableScreens()} returns
     * as the primary one, and centres there rather than clinging to a meaningless coordinate.
     *
     * @param saved the remembered rectangle; returned unchanged if {@code screens} is empty, since
     * guessing is worse than honouring what the player last chose.
     */
    public static Rectangle fit(Rectangle saved, List<Rectangle> screens) {
        if (saved == null || screens == null || screens.isEmpty()) {
            return saved;
        }
        final Rectangle screen = pickScreen(saved, screens);
        final Rectangle ret = new Rectangle(saved);

        //size first: a window wider than the screen can never be positioned wholly on it
        ret.width = Math.min(ret.width, screen.width);
        ret.height = Math.min(ret.height, screen.height);
        //...but never shrink below usable, even on a very small screen
        ret.width = Math.max(ret.width, Math.min(MIN_WIDTH, screen.width));
        ret.height = Math.max(ret.height, Math.min(MIN_HEIGHT, screen.height));

        if (!intersects(saved, screens)) {
            //the display it was on is gone - centre rather than keep a coordinate that means nothing
            ret.x = screen.x + (screen.width - ret.width) / 2;
            ret.y = screen.y + (screen.height - ret.height) / 2;
            return ret;
        }
        //nudge it back inside: right/bottom edge first, then the origin, so the title bar stays
        //reachable when the window is as wide as the screen
        ret.x = Math.min(ret.x, screen.x + screen.width - ret.width);
        ret.y = Math.min(ret.y, screen.y + screen.height - ret.height);
        ret.x = Math.max(ret.x, screen.x);
        ret.y = Math.max(ret.y, screen.y);
        return ret;
    }

    /** True when the rectangle is visible on any attached screen at all. */
    static boolean intersects(Rectangle saved, List<Rectangle> screens) {
        for (Rectangle screen : screens) {
            if (screen.intersects(saved)) {
                return true;
            }
        }
        return false;
    }

    /** The screen the rectangle overlaps most; the first screen when it overlaps none. */
    private static Rectangle pickScreen(Rectangle saved, List<Rectangle> screens) {
        Rectangle best = screens.get(0);
        long bestArea = -1;
        for (Rectangle screen : screens) {
            final Rectangle overlap = screen.intersection(saved);
            final long area = overlap.isEmpty() ? 0L : (long) overlap.width * overlap.height;
            if (area > bestArea) {
                bestArea = area;
                best = screen;
            }
        }
        return best;
    }

    /**
     * Every attached screen's USABLE area - the full bounds minus the taskbar and any other reserved
     * edge - with the primary display first, so it is the fallback in {@link #fit}.
     *
     * Returns an empty list in a headless JVM, which {@code fit} treats as "do not touch".
     */
    public static List<Rectangle> usableScreens() {
        final List<Rectangle> ret = new ArrayList<>();
        try {
            final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice primary = env.getDefaultScreenDevice();
            for (GraphicsDevice device : env.getScreenDevices()) {
                final Rectangle usable = usableBounds(device.getDefaultConfiguration());
                if (device == primary) {
                    ret.add(0, usable);
                } else {
                    ret.add(usable);
                }
            }
        } catch (RuntimeException e) {
            //headless (java.awt.HeadlessException is a RuntimeException) or a display that cannot be
            //queried: an empty list makes fit() leave the saved geometry alone
            return new ArrayList<>();
        }
        return ret;
    }

    private static Rectangle usableBounds(GraphicsConfiguration gc) {
        final Rectangle bounds = new Rectangle(gc.getBounds());
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

}
