package e2e;

import business.facade.LocalFacade;
import control.facade.WorldFacadeCounselor;
import gui.services.ScoutFootprint;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import model.Local;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import persistence.local.WorldManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the packing rule the scout-overlap overlay teaches: two scout footprints stop touching once
 * their centres are 3 hexes apart. Headless - set arithmetic over the real map, no Swing.
 * <p>
 * These numbers are the whole reason the feature can give advice, so they are asserted against the
 * live {@code LocalFacade} distance function rather than recomputed here. If the Judge ever widens the
 * reveal radius past 1, {@code ScoutFootprint.SCOUT_RADIUS} changes and these expectations move with
 * it - which is the point of failing loudly here rather than drawing a quietly wrong ring.
 */
class ScoutFootprintTest {

    private static final LocalFacade localFacade = new LocalFacade();

    @BeforeAll
    static void loadWorld() throws Exception {
        TestWorld.load();
    }

    /** A scout uncovers the centre plus its six neighbours - seven hexes, never the picker's range. */
    @Test
    void footprintIsSevenHexesAwayFromTheMapEdge() {
        final Local centre = inlandHex();
        assertNotNull(centre, "test map must contain a hex with all six neighbours");
        assertEquals(7, footprint(centre).size(), "centre + 6 neighbours");
    }

    /**
     * The packing table. Distance 0 wastes the whole action, 1 wastes four hexes, 2 wastes one or two,
     * and 3 wastes nothing - "keep centres at least 3 apart" falls out of this row.
     */
    @Test
    void overlapShrinksToNothingAtDistanceThree() {
        final Local centre = inlandHex();
        final Set<Local> base = footprint(centre);
        boolean sawOne = false, sawTwo = false, sawThree = false;
        for (Local other : listLocais().values()) {
            final int d = localFacade.getDistancia(centre, other);
            if (d > 3 || footprint(other).size() != 7) {
                continue; // ignore edge-clipped footprints, they are a smaller set by definition
            }
            final Set<Local> shared = new HashSet<>(base);
            shared.retainAll(footprint(other));
            switch (d) {
                case 0:
                    assertEquals(7, shared.size(), "same centre shares everything");
                    break;
                case 1:
                    assertEquals(4, shared.size(), "adjacent centres share 4 of 7");
                    sawOne = true;
                    break;
                case 2:
                    assertTrue(shared.size() == 1 || shared.size() == 2,
                            "centres 2 apart share 1 or 2, got " + shared.size());
                    sawTwo = true;
                    break;
                default:
                    assertEquals(0, shared.size(), "centres 3 apart must not overlap at all");
                    sawThree = true;
                    break;
            }
        }
        assertTrue(sawOne && sawTwo && sawThree, "map must exercise distances 1, 2 and 3");
    }

    /** Nothing queued, nothing to warn about - and never a null to guard at every call site. */
    @Test
    void emptyInputYieldsEmptyCoverage() {
        final ScoutFootprint.Coverage coverage = ScoutFootprint.compute(null, null, null);
        assertTrue(coverage.isEmpty());
        assertTrue(coverage.getOverlappedHexes().isEmpty());
        assertTrue(coverage.getRingsAt(inlandHex()).isEmpty(), "getRingsAt must never return null");
    }

    /**
     * Run the real thing over the real world: whatever the test EGF happens to have queued, the picture
     * it produces must be self-consistent. This is what stops the two halves of the feature - the map
     * shading and the hex-info line - from ever disagreeing, since both read this one object.
     */
    @Test
    void coverageIsSelfConsistentOnTheRealWorld() {
        final ScoutFootprint.Coverage coverage = ScoutFootprint.compute(
                WorldManager.getInstance().getPersonagens().values(),
                WorldFacadeCounselor.getInstance().getPartida().getJogadorAtivo(),
                listLocais());
        assertNotNull(coverage);
        for (ScoutFootprint.Ring ring : coverage.getRings()) {
            assertNotNull(ring.getCentre(), "a ring must know where it is centred");
            assertTrue(ring.getHexes().size() >= 1 && ring.getHexes().size() <= 7,
                    "a footprint is 7 hexes, or fewer only when clipped by the map edge");
            assertTrue(ring.getHexes().contains(ring.getCentre()), "the centre is always covered");
            // every hex a ring claims must report that ring back
            for (Local hex : ring.getHexes()) {
                assertTrue(coverage.getRingsAt(hex).contains(ring),
                        "getRingsAt must list every ring covering the hex");
            }
        }
        // an "overlapped" hex is exactly one that more than one ring claims
        for (Local hex : coverage.getOverlappedHexes()) {
            assertTrue(coverage.getRingsAt(hex).size() > 1, "overlap means 2+ rings on the hex");
        }
    }

    /** The cached picture the hex-info panel reads is never null, even before the first refresh. */
    @Test
    void currentCoverageIsNeverNull() {
        final ScoutFootprint.Coverage saved = ScoutFootprint.getCurrent();
        try {
            ScoutFootprint.setCurrent(null);
            assertNotNull(ScoutFootprint.getCurrent());
            assertTrue(ScoutFootprint.getCurrent().isEmpty());
        } finally {
            // it is a static; leaving it clobbered would bleed into any later test that reads it
            ScoutFootprint.setCurrent(saved);
        }
    }

    private Set<Local> footprint(Local centre) {
        return new HashSet<>(localFacade.getLocalRange(
                centre, ScoutFootprint.SCOUT_RADIUS, false, listLocais()).keySet());
    }

    /** A hex far enough from the edge that its footprint is not clipped. */
    private Local inlandHex() {
        for (Local local : listLocais().values()) {
            if (footprint(local).size() == 7) {
                return local;
            }
        }
        return null;
    }

    private static SortedMap<String, Local> listLocais() {
        return WorldManager.getInstance().getLocais();
    }
}
