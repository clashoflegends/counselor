package gui.services;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Keeps a remembered window position reachable.
 *
 * A player docks a laptop, drags Hex View or a detached Results popup onto the second monitor, closes
 * Counselor (position saved), then undocks. On the next launch the saved coordinates name a screen
 * that no longer exists, so the window opens where nobody can see or drag it. The window has focus
 * and is doing its job; it is simply nowhere.
 *
 * The decision is a PURE function ({@link #isReachable}) so it can be unit-tested with no display -
 * CI runners are headless and {@code GraphicsEnvironment.getScreenDevices()} throws there.
 *
 * Three things this deliberately does NOT do:
 * <ul>
 * <li><b>No union rectangle.</b> {@code Rectangle.union} is a bounding box, so an L-shaped desktop
 * (laptop beside a taller external) reports dead space as valid and a window parked in the notch
 * would pass while still being invisible. Every test is against ONE device.</li>
 * <li><b>No "negative coordinates are wrong" shortcut.</b> A monitor placed left of or above the
 * primary has negative origins, so x=-1500 is an ordinary position for a working setup.</li>
 * <li><b>No touching a position that is fine.</b> If the saved spot is reachable the coordinates are
 * used byte-identical: no clamping, no rounding, no centring. Almost every player is single-monitor
 * and already fine; moving their windows would be a worse bug than the one being fixed.</li>
 * </ul>
 */
public final class WindowGeometry {

    private static final Log log = LogFactory.getLog(WindowGeometry.class);

    /** Height of the grab strip we require, about one title bar. */
    private static final int TITLE_H = 24;
    /** How much of that strip must be on screen before a player can realistically grab and drag it. */
    private static final int GRAB_W = 60;
    /** We only ever test the top-left corner: a window is reachable if its title bar is. */
    private static final int SLAB_W = 120;

    private WindowGeometry() {
    }

    /**
     * PURE: can the player reach this window to move it?
     *
     * True when a grabbable slab of the window's title bar lies inside a SINGLE screen's usable area.
     * Testing the title bar rather than "is it substantially visible" matters: a window showing only
     * its bottom-right corner passes an area test and still cannot be dragged anywhere.
     *
     * @param saved the remembered bounds; width is used only to size the slab, height is ignored
     * @param usableScreens each screen's bounds MINUS its taskbar/dock insets
     * @return false when the position is unreachable, and false when there is nothing to judge against
     */
    public static boolean isReachable(Rectangle saved, List<Rectangle> usableScreens) {
        if (saved == null || usableScreens == null || usableScreens.isEmpty()) {
            return false;
        }
        final int slabW = (saved.width > 0) ? Math.min(saved.width, SLAB_W) : SLAB_W;
        final Rectangle slab = new Rectangle(saved.x, saved.y, slabW, TITLE_H);
        final int needW = Math.min(GRAB_W, slabW);
        for (Rectangle screen : usableScreens) {
            if (screen == null) {
                continue;
            }
            //Rectangle.intersection returns negative extents when they do not overlap, which fails
            //these tests on its own - no isEmpty() check needed.
            final Rectangle hit = slab.intersection(screen);
            if (hit.width >= needW && hit.height >= TITLE_H) {
                return true;
            }
        }
        return false;
    }

    /**
     * Usable area of every attached screen, i.e. bounds minus the taskbar or dock. Insets matter: a
     * window can sit fully inside getBounds() with its title bar under a taskbar, which is the same
     * unreachable state this class exists to prevent.
     *
     * @return one rectangle per screen, or an EMPTY list when there is no display to ask (headless CI,
     * or a toolkit that refuses) - callers must read empty as "cannot judge", never as "nothing fits"
     */
    public static List<Rectangle> usableScreens() {
        final List<Rectangle> out = new ArrayList<>();
        if (GraphicsEnvironment.isHeadless()) {
            return out;
        }
        try {
            final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice device : devices) {
                final GraphicsConfiguration gc = device.getDefaultConfiguration();
                if (gc == null) {
                    continue;
                }
                final Rectangle b = new Rectangle(gc.getBounds());
                try {
                    final Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                    b.x += in.left;
                    b.y += in.top;
                    b.width -= (in.left + in.right);
                    b.height -= (in.top + in.bottom);
                } catch (RuntimeException ex) {
                    //no insets available: the raw bounds are still a usable answer
                }
                if (b.width > 0 && b.height > 0) {
                    out.add(b);
                }
            }
        } catch (RuntimeException ex) {
            //HeadlessException and friends are all RuntimeException; an unreadable layout must mean
            //"cannot judge", never "relocate everything".
            log.warn("Could not read screen layout, leaving saved window positions untouched: " + ex);
            out.clear();
        }
        return out;
    }

    /**
     * Place {@code w} at the remembered spot when the player can still reach it there, otherwise put it
     * over {@code fallbackParent} (the Counselor window), which is by definition on a live screen.
     *
     * Call this INSTEAD of setLocation, and before setVisible. Pass the saved size rather than the
     * live one: callers set the size via setPreferredSize/pack afterwards, so the window has no
     * meaningful width yet.
     *
     * @param fallbackParent may be null, in which case an unreachable window is centred on the default
     * screen
     */
    public static void restoreLocation(Window w, int x, int y, int width, int height, Component fallbackParent) {
        if (w == null) {
            return;
        }
        final List<Rectangle> screens = usableScreens();
        if (screens.isEmpty()) {
            //Nothing to judge against: keep the old behaviour rather than moving a window on a guess.
            w.setLocation(x, y);
            return;
        }
        final Rectangle saved = new Rectangle(x, y, width, height);
        if (isReachable(saved, screens)) {
            w.setLocation(x, y);
            return;
        }
        log.info(String.format("Window position %d,%d is off every screen %s - moving it back over the main window",
                x, y, screens));
        w.setLocationRelativeTo(fallbackParent);
    }
}
