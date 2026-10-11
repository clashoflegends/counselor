package e2e;

import gui.services.WindowSlots;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The main window's geometry is remembered per DISPLAY ARRANGEMENT rather than once globally,
 * because a single slot was overwritten twice a day by a docked laptop: the size saved on dual
 * monitors was the size restored on the laptop panel.
 *
 * {@link ScreenFitTest} covers making a remembered rectangle usable on the screens that exist; this
 * covers remembering a different rectangle for each set of screens, and evicting sensibly when more
 * arrangements are seen than there are slots.
 *
 * The store is a plain map here - the production one is properties.config - which is the whole
 * reason {@code WindowSlots} takes a {@code Store} rather than reaching for SettingsManager.
 */
class WindowSlotsTest {

    private static final String BASE = "GuiMainWindow";
    private static final Rectangle LAPTOP = new Rectangle(0, 0, 1920, 1040);
    private static final Rectangle EXTERNAL = new Rectangle(1920, 0, 2560, 1400);

    /** A Store over a HashMap, which is all the production one does over the settings file. */
    private static final class MapStore implements WindowSlots.Store {

        private final Map<String, String> map = new HashMap<>();

        @Override
        public String get(String key, String defaultValue) {
            final String ret = map.get(key);
            return ret == null ? defaultValue : ret;
        }

        @Override
        public void set(String key, String value) {
            map.put(key, value);
        }
    }

    private static String sigLaptop() {
        return WindowSlots.signature(Collections.singletonList(LAPTOP));
    }

    private static String sigDocked() {
        return WindowSlots.signature(Arrays.asList(LAPTOP, EXTERNAL));
    }

    /** The reported scenario, end to end: each arrangement keeps its own window. */
    @Test
    void dockedAndUndockedKeepSeparateGeometry() {
        final MapStore store = new MapStore();
        final Rectangle onDesk = new Rectangle(2100, 120, 2200, 1200);
        final Rectangle onLaptop = new Rectangle(60, 40, 1400, 900);

        WindowSlots.save(store, BASE, sigDocked(), onDesk);
        WindowSlots.save(store, BASE, sigLaptop(), onLaptop);

        assertEquals(onDesk, WindowSlots.load(store, BASE, sigDocked()),
                "re-docking must give back the desk window, not the laptop one");
        assertEquals(onLaptop, WindowSlots.load(store, BASE, sigLaptop()));
    }

    /** Saving the same arrangement twice updates in place rather than consuming a second slot. */
    @Test
    void resavingAnArrangementReusesItsSlot() {
        final MapStore store = new MapStore();
        WindowSlots.save(store, BASE, sigLaptop(), new Rectangle(10, 10, 800, 600));
        WindowSlots.save(store, BASE, sigLaptop(), new Rectangle(20, 20, 900, 700));
        WindowSlots.save(store, BASE, sigDocked(), new Rectangle(2000, 0, 1500, 1000));
        WindowSlots.save(store, BASE, "3_1920x1040_2560x1400_1280x1024", new Rectangle(5, 5, 700, 500));

        //three distinct arrangements, three slots, nothing evicted
        assertEquals(new Rectangle(20, 20, 900, 700), WindowSlots.load(store, BASE, sigLaptop()));
        assertEquals(new Rectangle(2000, 0, 1500, 1000), WindowSlots.load(store, BASE, sigDocked()));
    }

    /** An arrangement never seen before has nothing to give, so the caller can fall back. */
    @Test
    void anUnknownArrangementReturnsNull() {
        final MapStore store = new MapStore();
        WindowSlots.save(store, BASE, sigLaptop(), new Rectangle(10, 10, 800, 600));
        assertNull(WindowSlots.load(store, BASE, sigDocked()));
        assertNull(WindowSlots.load(store, BASE, null));
    }

    /** The fourth arrangement evicts the one used longest ago, not an arbitrary one. */
    @Test
    void theLeastRecentlyUsedArrangementIsEvicted() {
        final MapStore store = new MapStore();
        WindowSlots.save(store, BASE, "A", new Rectangle(1, 1, 801, 601));
        WindowSlots.save(store, BASE, "B", new Rectangle(2, 2, 802, 602));
        WindowSlots.save(store, BASE, "C", new Rectangle(3, 3, 803, 603));
        //touch A so B is now the oldest
        WindowSlots.save(store, BASE, "A", new Rectangle(4, 4, 804, 604));
        WindowSlots.save(store, BASE, "D", new Rectangle(5, 5, 805, 605));

        assertNull(WindowSlots.load(store, BASE, "B"), "B was used longest ago and should be gone");
        assertEquals(new Rectangle(4, 4, 804, 604), WindowSlots.load(store, BASE, "A"));
        assertEquals(new Rectangle(3, 3, 803, 603), WindowSlots.load(store, BASE, "C"));
        assertEquals(new Rectangle(5, 5, 805, 605), WindowSlots.load(store, BASE, "D"));
    }

    /** Signatures key on screen SIZES and count, so replugging the same monitors still matches. */
    @Test
    void theSameScreensProduceTheSameSignatureWhereverTheySit() {
        final Rectangle movedExternal = new Rectangle(-2560, -180, 2560, 1400);
        assertEquals(WindowSlots.signature(Arrays.asList(LAPTOP, EXTERNAL)),
                WindowSlots.signature(Arrays.asList(LAPTOP, movedExternal)),
                "the same two monitors are the same setup even if one's origin moved");
        assertNotEquals(sigLaptop(), sigDocked(), "one screen is not the same setup as two");
    }

    /** A headless run must never overwrite a real arrangement's slot. */
    @Test
    void headlessGetsItsOwnSignature() {
        assertEquals("none", WindowSlots.signature(Collections.emptyList()));
        assertEquals("none", WindowSlots.signature(null));
        assertNotEquals("none", sigLaptop());
    }

    /** properties.config is hand-editable, so a mangled slot must read as absent, not crash. */
    @Test
    void aCorruptOrHalfWrittenSlotIsIgnored() {
        final MapStore store = new MapStore();
        store.set(BASE + "Slot0Sig", sigLaptop());
        store.set(BASE + "Slot0X", "not-a-number");
        store.set(BASE + "Slot0Y", "10");
        store.set(BASE + "Slot0W", "");
        store.set(BASE + "Slot0H", "600");
        assertNull(WindowSlots.load(store, BASE, sigLaptop()), "a slot with no usable size is no slot");
    }

    /** A zero or negative size is never recorded; it would restore an invisible window. */
    @Test
    void anEmptyRectangleIsNotSaved() {
        final MapStore store = new MapStore();
        WindowSlots.save(store, BASE, sigLaptop(), new Rectangle(10, 10, 0, 600));
        WindowSlots.save(store, BASE, sigLaptop(), null);
        assertNull(WindowSlots.load(store, BASE, sigLaptop()));
    }
}
