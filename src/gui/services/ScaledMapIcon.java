package gui.services;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;

/**
 * Paints the map image (and an optional full-map actions overlay) scaled by a zoom factor.
 * <p>
 * Used for the high-DPI map zoom: instead of materializing a giant {@code zoom}-times-larger
 * {@code BufferedImage} (memory grows with zoom squared, and there are two full-map images), the 1x
 * images are kept and scaled at paint time via {@code Graphics2D.scale}. Memory stays flat regardless
 * of zoom level, and Java2D only rasterizes within the Graphics clip (the visible viewport region),
 * so scrolling a large zoomed map stays cheap.
 */
public class ScaledMapIcon implements Icon {

    /** On-screen thickness of the range outline, in device pixels (kept constant across zoom levels). */
    private static final float RANGE_STROKE_PX = 4f;
    /** Matches the ColorHexRange default in MapaControler - keep the two in step. */
    public static final Color RANGE_COLOR_DEFAULT = Color.WHITE;

    /** On-screen thickness of a scout footprint outline, in device pixels. Thinner than the range border. */
    private static final float SCOUT_STROKE_PX = 2.5f;
    /**
     * Magenta, and deliberately the only magenta on the map.
     * <p>
     * The first cut drew these blue and cyan, which was wrong twice over: rivers are blue and are drawn
     * along hex EDGES exactly where a footprint outline runs, and {@code ImageManager} already spends
     * {@code Color.BLUE} on own movement paths and {@code Color.CYAN} on allied ones. Terrain is greens,
     * browns, tans, greys and dark blues; the other overlays are blue, cyan, gold and red. Nothing uses
     * magenta, so it cannot be mistaken for a river, a road, or somebody's march.
     */
    private static final Color SCOUT_COLOR = new Color(235, 40, 225);
    /**
     * Own vs allied is told apart by the RHYTHM of the dash, not by hue: a second colour would have had
     * to come out of the same exhausted palette. Long dashes are mine, short ticks are an ally.
     * Lengths are 1x map units and are divided by the zoom when stroking, like the width.
     */
    private static final float[] SCOUT_DASH_MINE = {9f, 7f};
    private static final float[] SCOUT_DASH_ALLY = {3f, 5f};
    /**
     * Wash over ground two or more queued scouts both uncover - the same magenta, so coverage simply
     * reads as "more of it". Alpha, never a Composite: the actions layer runs at SrcOver 1.0 throughout
     * and nothing downstream would restore a composite change, so setting one here would tint every ring
     * drawn afterwards.
     */
    private static final Color SCOUT_COLOR_OVERLAP = new Color(235, 40, 225, 70);
    /** Marching-ants offset, advanced by the map's animation timer. 1x units. */
    private float dashPhase = 0f;
    /** Where paintIcon last drew, so the animation can repaint just the footprints. */
    private int lastX = 0, lastY = 0;

    private Image base;     // 1x composed map
    private Image actions;  // 1x full-map actions overlay, or null when hidden
    private Shape rangeOutline; // 1x hex-range border (vector, so it stays crisp at any zoom), or null
    private Color rangeColor = RANGE_COLOR_DEFAULT;
    private Color rangeHalo = haloFor(RANGE_COLOR_DEFAULT);
    // Scout footprints, 1x vector for the same reason the range border is: the actions bitmap is
    // upscaled bicubically, which smears a thin outline at any zoom above 1.
    private List<Shape> scoutMine = Collections.emptyList();
    private List<Shape> scoutAlly = Collections.emptyList();
    private List<Shape> scoutOverlap = Collections.emptyList();
    private double zoom = 1.0;

    public void setBase(Image base) {
        this.base = base;
    }

    public void setActions(Image actions) {
        this.actions = actions;
    }

    /** Border around the hexes an order parameter may target; null clears it. Coordinates are 1x map space. */
    public void setRangeOutline(Shape rangeOutline) {
        this.rangeOutline = rangeOutline;
    }

    /**
     * The hexes each queued scout order will uncover, and the ground more than one of them covers.
     * Each list holds closed 1x hex contours; pass empty lists to clear. Overlap shapes are filled, the
     * other two are stroked.
     */
    public void setScoutOverlay(List<Shape> mine, List<Shape> ally, List<Shape> overlap) {
        this.scoutMine = (mine == null) ? Collections.<Shape>emptyList() : mine;
        this.scoutAlly = (ally == null) ? Collections.<Shape>emptyList() : ally;
        this.scoutOverlap = (overlap == null) ? Collections.<Shape>emptyList() : overlap;
    }

    /** Player-chosen border colour (properties.config ColorHexRange); null restores the default. */
    public void setRangeColor(Color rangeColor) {
        this.rangeColor = (rangeColor == null) ? RANGE_COLOR_DEFAULT : rangeColor;
        this.rangeHalo = haloFor(this.rangeColor);
    }

    /**
     * The under-stroke that keeps the border readable on any terrain. It is the OPPOSITE of the chosen
     * colour, not a fixed black: a player who picks a dark blue border would otherwise get a black halo
     * on a dark sea and lose the outline entirely. Rec.601 luma, translucent so the map still shows.
     */
    private static Color haloFor(Color c) {
        final double luma = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        return luma > 0.5 ? new Color(0, 0, 0, 140) : new Color(255, 255, 255, 160);
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public double getZoom() {
        return zoom;
    }

    @Override
    public int getIconWidth() {
        return base == null ? 0 : (int) Math.ceil(base.getWidth(null) * zoom);
    }

    @Override
    public int getIconHeight() {
        return base == null ? 0 : (int) Math.ceil(base.getHeight(null) * zoom);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (base == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            lastX = x;
            lastY = y;
            g2.translate(x, y);
            g2.scale(zoom, zoom);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(base, 0, 0, c);
            if (actions != null) {
                g2.drawImage(actions, 0, 0, c);
            }
            paintScouts(g2);
            if (rangeOutline != null) {
                // Drawn INSIDE the scale transform so the 1x geometry lands on the right hexes, but with the
                // stroke width divided by the zoom so the border keeps a constant on-screen thickness - at the
                // 0.5 manual zoom floor a plain 4px stroke would otherwise render hairline.
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final float w = (float) (RANGE_STROKE_PX / zoom);
                // contrasting under-stroke first, so the border reads on both pale desert and dark sea
                g2.setStroke(new BasicStroke(w * 1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(rangeHalo);
                g2.draw(rangeOutline);
                g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(rangeColor);
                g2.draw(rangeOutline);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Wasted ground first as a flat wash, then the footprints over it, so a shaded hex still reads as
     * belonging to both rings that claim it. Same constant-thickness trick as the range border.
     * <p>
     * Each overlap shape is ONE hexagon, filled individually rather than as a union: a unioned outline
     * has no enforced contour orientation, and under the default non-zero winding rule a hole in the
     * union would fill solid.
     */
    private void paintScouts(Graphics2D g2) {
        if (scoutOverlap.isEmpty() && scoutMine.isEmpty() && scoutAlly.isEmpty()) {
            return;
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(SCOUT_COLOR_OVERLAP);
        for (Shape hex : scoutOverlap) {
            g2.fill(hex);
        }
        final float w = (float) (SCOUT_STROKE_PX / zoom);
        paintDashed(g2, scoutAlly, SCOUT_DASH_ALLY, w);
        paintDashed(g2, scoutMine, SCOUT_DASH_MINE, w);
    }

    /**
     * A dashed footprint over a dashed halo of the same rhythm, so each dash carries its own contrasting
     * edge. That is what makes one colour work across all five tilesets: on the pale desert set the halo
     * is dark, on the dark sea it is light, and the magenta never has to win against the terrain alone.
     * <p>
     * Dash lengths and the phase are divided by the zoom for the same reason the width is - so the
     * pattern keeps a constant on-screen rhythm instead of stretching as the player zooms in.
     */
    private void paintDashed(Graphics2D g2, List<Shape> rings, float[] dash, float w) {
        if (rings.isEmpty()) {
            return;
        }
        final float[] scaled = new float[dash.length];
        for (int ii = 0; ii < dash.length; ii++) {
            scaled[ii] = (float) (dash[ii] / zoom);
        }
        final float phase = (float) (dashPhase / zoom);
        g2.setStroke(new BasicStroke(w * 2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, scaled, phase));
        g2.setColor(haloFor(SCOUT_COLOR));
        for (Shape ring : rings) {
            g2.draw(ring);
        }
        g2.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, scaled, phase));
        g2.setColor(SCOUT_COLOR);
        for (Shape ring : rings) {
            g2.draw(ring);
        }
    }

    /**
     * Advance the marching ants. Driven by the map's timer rather than a timer in here, because an Icon
     * has no repaint handle of its own.
     */
    public void setDashPhase(float dashPhase) {
        this.dashPhase = dashPhase;
    }

    /**
     * The area the scout overlay occupies, in the coordinates of the component that paints this icon, or
     * null when there is nothing to animate. Lets the animation repaint a few hexes instead of the whole
     * map - on a zoomed map a full repaint means bicubically rescaling the entire base image every frame.
     */
    public Rectangle getScoutRepaintBounds() {
        Rectangle union = null;
        for (List<Shape> group : java.util.Arrays.asList(scoutMine, scoutAlly, scoutOverlap)) {
            for (Shape s : group) {
                union = (union == null) ? s.getBounds() : union.union(s.getBounds());
            }
        }
        if (union == null) {
            return null;
        }
        // 1x -> component space, then grow by the stroke so the outer edge of the halo is included
        final int pad = (int) Math.ceil(SCOUT_STROKE_PX * 2) + 2;
        return new Rectangle(
                lastX + (int) Math.floor(union.x * zoom) - pad,
                lastY + (int) Math.floor(union.y * zoom) - pad,
                (int) Math.ceil(union.width * zoom) + pad * 2,
                (int) Math.ceil(union.height * zoom) + pad * 2);
    }
}
