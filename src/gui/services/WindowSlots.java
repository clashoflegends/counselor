/*
 * WindowSlots.java
 */
package gui.services;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.List;

/**
 * Remembers the main window's geometry PER DISPLAY ARRANGEMENT, a few arrangements at a time.
 *
 * The Counselor used to keep one slot, so a docked laptop overwrote it twice a day: the geometry
 * saved on dual monitors was the geometry restored on the laptop panel, and vice versa. {@link
 * ScreenFit} made that merely usable - clamped to a screen that exists - but it could not give back
 * the layout the player actually had. This does.
 *
 * Each arrangement is identified by {@link #signature}, and the geometry is stored against it.
 * Dock, undock and dock again and the window returns to where it was in each, because each wrote to
 * its own slot. {@value #SLOTS} slots cover a laptop, a desk and one other without the settings file
 * growing without bound; the least recently used is evicted, so the arrangements a player actually
 * uses stay and a one-off projector does not displace them permanently.
 *
 * <b>Still clamp the result.</b> A matching signature means the same screen sizes, not necessarily
 * the same positions, and a settings file can be hand-edited. {@code ScreenFit.fit} stays the last
 * step on the load path.
 *
 * Deliberately pure over a small {@link Store}, so the slot and eviction logic is unit-tested
 * against a plain map with no settings file and no display.
 *
 * @author jmoura
 */
public final class WindowSlots implements Serializable {

    /** How many display arrangements to remember. A laptop, a desk, and one spare. */
    public static final int SLOTS = 3;

    /** The settings file, narrowed to what this needs so tests can pass a map. */
    public interface Store {

        String get(String key, String defaultValue);

        void set(String key, String value);
    }

    private WindowSlots() {
    }

    /**
     * A stable identity for the current display arrangement: how many screens, and the SIZE of each.
     *
     * Sizes but not origins, on purpose. Replugging the same two monitors can renumber them or shift
     * one's origin without changing anything the player would call a different setup, and treating
     * that as a new arrangement would silently evict the slot they wanted. Screens arrive from
     * {@link ScreenFit#usableScreens()} with the primary first, which keeps the signature stable
     * across reboots.
     *
     * An empty screen list - headless - yields a signature of its own rather than matching any real
     * arrangement, so a headless run can never overwrite a real slot.
     */
    public static String signature(List<Rectangle> screens) {
        if (screens == null || screens.isEmpty()) {
            return "none";
        }
        final StringBuilder ret = new StringBuilder();
        ret.append(screens.size());
        for (Rectangle screen : screens) {
            ret.append('_').append(screen.width).append('x').append(screen.height);
        }
        return ret.toString();
    }

    /**
     * The geometry last saved for this arrangement, or null when it has never been seen - in which
     * case the caller should fall back to the legacy single-slot keys, so an upgrading player keeps
     * the window they had.
     */
    public static Rectangle load(Store store, String base, String signature) {
        final int slot = findSlot(store, base, signature);
        if (slot < 0) {
            return null;
        }
        final int width = asInt(store.get(key(base, slot, "W"), "-1"), -1);
        final int height = asInt(store.get(key(base, slot, "H"), "-1"), -1);
        if (width <= 0 || height <= 0) {
            //a half-written slot is not a slot
            return null;
        }
        return new Rectangle(asInt(store.get(key(base, slot, "X"), "0"), 0),
                asInt(store.get(key(base, slot, "Y"), "0"), 0), width, height);
    }

    /**
     * Record this arrangement's geometry, reusing its slot if it has one and evicting the least
     * recently used otherwise. Writes a monotonic sequence number so eviction has an order to work
     * from; the sequence only ever grows, which is what makes "least recently used" meaningful
     * across sessions.
     */
    public static void save(Store store, String base, String signature, Rectangle bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return;
        }
        int slot = findSlot(store, base, signature);
        if (slot < 0) {
            slot = findVictim(store, base);
        }
        store.set(key(base, slot, "Sig"), signature);
        store.set(key(base, slot, "Seq"), String.valueOf(nextSeq(store, base)));
        store.set(key(base, slot, "X"), String.valueOf(bounds.x));
        store.set(key(base, slot, "Y"), String.valueOf(bounds.y));
        store.set(key(base, slot, "W"), String.valueOf(bounds.width));
        store.set(key(base, slot, "H"), String.valueOf(bounds.height));
    }

    /** The slot already holding this signature, or -1. */
    private static int findSlot(Store store, String base, String signature) {
        if (signature == null) {
            return -1;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            if (signature.equals(store.get(key(base, slot, "Sig"), ""))) {
                return slot;
            }
        }
        return -1;
    }

    /** An empty slot if there is one, else the one used longest ago. */
    private static int findVictim(Store store, String base) {
        int victim = 0;
        long oldest = Long.MAX_VALUE;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (store.get(key(base, slot, "Sig"), "").isEmpty()) {
                return slot;
            }
            final long seq = asLong(store.get(key(base, slot, "Seq"), "0"), 0L);
            if (seq < oldest) {
                oldest = seq;
                victim = slot;
            }
        }
        return victim;
    }

    private static long nextSeq(Store store, String base) {
        long max = 0L;
        for (int slot = 0; slot < SLOTS; slot++) {
            max = Math.max(max, asLong(store.get(key(base, slot, "Seq"), "0"), 0L));
        }
        return max + 1L;
    }

    private static String key(String base, int slot, String field) {
        return base + "Slot" + slot + field;
    }

    private static int asInt(String value, int defaultValue) {
        return (int) asLong(value, defaultValue);
    }

    /** Tolerant on purpose: properties.config is a hand-editable text file. */
    private static long asLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value.trim());
        } catch (NullPointerException | NumberFormatException e) {
            return defaultValue;
        }
    }
}
