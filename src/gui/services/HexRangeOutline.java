package gui.services;

import business.converter.ConverterFactory;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Local;

/**
 * Builds the outline of a set of hexes as a single {@link Shape} in 1x map coordinates, so the map can
 * draw a thick border around "the hexes you may pick" instead of tagging each one.
 * <p>
 * <b>How, and why not the obvious way.</b> The obvious implementation is an {@code Area} union of every
 * hexagon, and it is correct - but {@code Area} does real boolean geometry and its cost is superlinear:
 * measured 19ms for 124 hexes, and a range-12 disc on a full map is ~469. That runs on the EDT every
 * time the player clicks an order, so this instead walks edges, which is O(n) and exact:
 * <ol>
 * <li>emit all 6 edges of every hex; an edge shared by two hexes of the set appears TWICE, an edge on
 * the perimeter appears ONCE - so the boundary is exactly the edges with a count of one. No neighbour
 * model is needed, which also means none of the offset-grid parity logic can be got wrong.</li>
 * <li>chain those edges end to end into closed loops, so the result is proper closed contours (one per
 * connected region, plus one per hole) rather than a heap of disjoint segments.</li>
 * </ol>
 * A range that runs off the map simply has no hexes to contribute there, so it clips at the map edge
 * with no special case, and a hole in the set comes out as its own contour.
 * <p>
 * Geometry note: the hex vertices are the same 1x grid {@code TagManager} draws the focus tag with -
 * a pointy-top hexagon 60 wide and 60 tall, quarter points at y=15 and y=45, laid out on a 60x45 pitch
 * with odd rows shifted 30px right. Neighbouring hexes therefore share vertex coordinates EXACTLY (all
 * integers), which is what lets a shared edge cancel by equality. Do not inset, round, or make these
 * coordinates floating point.
 */
public final class HexRangeOutline {

    /** Vertex offsets of one hexagon, relative to the tile origin returned by ConverterFactory.localToPoint. */
    private static final int[][] HEX_VERTICES = {{30, 0}, {60, 15}, {60, 45}, {30, 60}, {0, 45}, {0, 15}};

    private HexRangeOutline() {
    }

    /**
     * @param hexes the hexes inside the range (typically LocalFacade.getLocalRange(...).keySet(), so the
     *              outline uses the very same distance function the picker filters on)
     * @return the outline in 1x map coordinates, or null if there is nothing to draw
     */
    public static Shape build(Collection<Local> hexes) {
        if (hexes == null || hexes.isEmpty()) {
            return null;
        }
        final Set<Long> boundary = boundaryEdges(hexes);
        if (boundary.isEmpty()) {
            return null;
        }
        return chainIntoContours(boundary);
    }

    /** Edges belonging to exactly one hex of the set, keyed canonically so a shared edge cancels itself. */
    private static Set<Long> boundaryEdges(Collection<Local> hexes) {
        final Set<Long> once = new HashSet<>(hexes.size() * 4);
        for (Local hex : hexes) {
            if (hex == null) {
                continue;
            }
            final Point o = ConverterFactory.localToPoint(hex);
            for (int i = 0; i < 6; i++) {
                final int[] v1 = HEX_VERTICES[i];
                final int[] v2 = HEX_VERTICES[(i + 1) % 6];
                final long edge = edgeKey(o.x + v1[0], o.y + v1[1], o.x + v2[0], o.y + v2[1]);
                if (!once.add(edge)) {
                    once.remove(edge); // seen twice = interior, cancel it
                }
            }
        }
        return once;
    }

    /** Walk the boundary edges into closed subpaths. */
    private static Path2D.Double chainIntoContours(Set<Long> boundary) {
        final Map<Integer, List<Integer>> adjacency = new HashMap<>(boundary.size() * 2);
        for (long edge : boundary) {
            final int a = (int) (edge >> 32);
            final int b = (int) edge;
            adjacency.computeIfAbsent(a, k -> new ArrayList<>(2)).add(b);
            adjacency.computeIfAbsent(b, k -> new ArrayList<>(2)).add(a);
        }
        final Path2D.Double path = new Path2D.Double();
        final Set<Long> unused = new HashSet<>(boundary);
        while (!unused.isEmpty()) {
            final long seed = unused.iterator().next();
            final int start = (int) (seed >> 32);
            int current = start;
            path.moveTo(unpackX(current), unpackY(current));
            Integer next;
            while ((next = takeUnusedNeighbour(adjacency, unused, current)) != null) {
                current = next;
                if (current == start) {
                    break; // loop closed
                }
                path.lineTo(unpackX(current), unpackY(current));
            }
            path.closePath();
        }
        return path;
    }

    /** Consume one still-unused edge leaving {@code from}, returning its other end. */
    private static Integer takeUnusedNeighbour(Map<Integer, List<Integer>> adjacency, Set<Long> unused, int from) {
        final List<Integer> neighbours = adjacency.get(from);
        if (neighbours == null) {
            return null;
        }
        for (Integer to : neighbours) {
            if (unused.remove(edgeKey(from, to))) {
                return to;
            }
        }
        return null;
    }

    // --- packing ---------------------------------------------------------------------------------
    // A vertex packs into one int (16 bits each) and an undirected edge into one long (the two vertex
    // ints, smaller first). Map coordinates are well under 32k - a 3071-hex GoT map is ~3600px wide -
    // and this keeps the hot loop free of per-edge object allocation, which is the point of not using Area.

    private static int vertexKey(int x, int y) {
        return ((x & 0xFFFF) << 16) | (y & 0xFFFF);
    }

    private static int unpackX(int v) {
        return (v >>> 16) & 0xFFFF;
    }

    private static int unpackY(int v) {
        return v & 0xFFFF;
    }

    private static long edgeKey(int x1, int y1, int x2, int y2) {
        return edgeKey(vertexKey(x1, y1), vertexKey(x2, y2));
    }

    private static long edgeKey(int v1, int v2) {
        final int lo = Math.min(v1, v2);
        final int hi = Math.max(v1, v2);
        return (((long) lo) << 32) | (hi & 0xFFFFFFFFL);
    }
}
