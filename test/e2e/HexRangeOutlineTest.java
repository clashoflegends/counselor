package e2e;

import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import gui.services.HexRangeOutline;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.Local;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import persistence.local.WorldManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Geometry of the order-parameter hex-range border. Headless: builds Shapes only, no Swing.
 * <p>
 * The union is what makes the border correct, so the assertions are about the union collapsing shared
 * edges (a blob of hexes must be ONE contour, not one per hex) and about the outline honouring the real
 * map extent rather than an analytically computed ring.
 */
class HexRangeOutlineTest {

    private static final LocalFacade localFacade = new LocalFacade();

    @BeforeAll
    static void loadWorld() throws Exception {
        TestWorld.load();
    }

    /** One hex outlines as a single hexagon, 60x60 on the 1x map grid. */
    @Test
    void singleHexIsOneHexagon() {
        Local one = listLocais().values().iterator().next();
        Shape s = HexRangeOutline.build(List.of(one));
        assertNotNull(s, "a single hex must produce an outline");
        Rectangle b = s.getBounds();
        assertEquals(60, b.width, "hex width");
        assertEquals(60, b.height, "hex height");
        assertEquals(1, contourCount(s));
    }

    /** Empty / null input draws nothing rather than an empty artefact. */
    @Test
    void emptyInputDrawsNothing() {
        assertNull(HexRangeOutline.build(null));
        assertNull(HexRangeOutline.build(new ArrayList<>()));
    }

    /**
     * The load-bearing property: neighbouring hexes share vertices EXACTLY, so the union removes the edge
     * between them. If the geometry ever drifts off the integer grid this leaves internal seams and the
     * border gets drawn through the middle of the range.
     */
    @Test
    void neighboursMergeIntoOneContour() {
        Collection<Local> ring1 = localFacade.getLocalRange(centreHex(), 1, false, listLocais()).keySet();
        //origin + 6 neighbours: anything less means centreHex() drifted to an edge and the test went soft
        assertEquals(7, ring1.size(), "expected a full unclipped ring of neighbours");
        assertEquals(1, contourCount(HexRangeOutline.build(ring1)),
                "a solid blob of hexes must have exactly one contour");
    }

    /** A bigger range is still one contour, and grows in both directions. */
    @Test
    void largerRangeIsStillOneContourAndBigger() {
        Local origem = centreHex();
        Shape small = HexRangeOutline.build(localFacade.getLocalRange(origem, 1, false, listLocais()).keySet());
        Shape big = HexRangeOutline.build(localFacade.getLocalRange(origem, 3, false, listLocais()).keySet());
        assertEquals(1, contourCount(big));
        assertTrue(big.getBounds().width > small.getBounds().width, "range 3 must be wider than range 1");
        assertTrue(big.getBounds().height > small.getBounds().height, "range 3 must be taller than range 1");
    }

    /**
     * Map edges: a range centred near the top-left corner clips to the map instead of running off it.
     * getLocalRange only ever returns hexes that exist, so this holds by construction - the test pins it
     * so a future "compute the ring analytically" optimisation cannot regress it.
     */
    @Test
    void rangeAtMapCornerClipsToTheMap() {
        Local corner = listLocais().get("0101");
        assertNotNull(corner, "test world should have hex 0101");
        Rectangle b = HexRangeOutline.build(
                localFacade.getLocalRange(corner, 4, false, listLocais()).keySet()).getBounds();
        assertTrue(b.x >= 0, "must not extend left of the map: " + b.x);
        assertTrue(b.y >= 0, "must not extend above the map: " + b.y);
    }

    /** Every hex the picker accepts sits inside the border. */
    @Test
    void everyInRangeHexIsInsideTheOutline() {
        Local origem = centreHex();
        Collection<Local> inRange = localFacade.getLocalRange(origem, 3, false, listLocais()).keySet();
        Shape s = HexRangeOutline.build(inRange);
        for (Local l : inRange) {
            Point p = ConverterFactory.localToPoint(l);
            assertTrue(s.contains(p.x + 30, p.y + 30),
                    "hex " + l.getCodigo() + " is in range and must be inside the border");
        }
    }

    /** A hex beyond the range falls outside the border. */
    @Test
    void outOfRangeHexIsOutsideTheOutline() {
        Local origem = centreHex();
        Shape s = HexRangeOutline.build(localFacade.getLocalRange(origem, 2, false, listLocais()).keySet());
        for (Local l : listLocais().values()) {
            if (localFacade.getDistancia(origem, l) > 3) {
                Point p = ConverterFactory.localToPoint(l);
                assertFalse(s.contains(p.x + 30, p.y + 30),
                        "hex " + l.getCodigo() + " is out of range and must be outside the border");
                return;
            }
        }
        fail("test world too small to hold an out-of-range hex");
    }

    // --- helpers -----------------------------------------------------------------------------------

    private SortedMap<String, Local> listLocais() {
        return WorldManager.getInstance().getLocais();
    }

    /**
     * The hex furthest from any map edge, so a small range stays unclipped.
     * <p>
     * Must measure distance to the NEAREST edge on all four sides. Maximizing {@code min(row, col)}
     * instead picks the far corner: on this fixture it returned 1111, whose column 11 is the map maximum,
     * so a "centre" hex sat on the right edge and range 3 yielded 20 hexes rather than an unclipped ~37 -
     * which let the ring assertions below pass on a clipped half-ring.
     */
    private Local centreHex() {
        int maxRow = 0, maxCol = 0;
        for (Local l : listLocais().values()) {
            maxRow = Math.max(maxRow, LocalFacade.getRow(l));
            maxCol = Math.max(maxCol, LocalFacade.getCol(l));
        }
        Local best = null;
        int bestScore = -1;
        for (Local l : listLocais().values()) {
            final int row = LocalFacade.getRow(l), col = LocalFacade.getCol(l);
            final int score = Math.min(Math.min(row, col), Math.min(maxRow - row, maxCol - col));
            if (score > bestScore) {
                bestScore = score;
                best = l;
            }
        }
        assertNotNull(best);
        return best;
    }

    /** Number of closed contours (SEG_MOVETO occurrences) in the shape. */
    private static int contourCount(Shape s) {
        int n = 0;
        double[] c = new double[6];
        for (PathIterator it = s.getPathIterator(null); !it.isDone(); it.next()) {
            if (it.currentSegment(c) == PathIterator.SEG_MOVETO) {
                n++;
            }
        }
        return n;
    }
}
