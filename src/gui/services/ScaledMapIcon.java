package gui.services;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
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

    private Image base;     // 1x composed map
    private Image actions;  // 1x full-map actions overlay, or null when hidden
    private Shape rangeOutline; // 1x hex-range border (vector, so it stays crisp at any zoom), or null
    private Color rangeColor = RANGE_COLOR_DEFAULT;
    private Color rangeHalo = haloFor(RANGE_COLOR_DEFAULT);
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
            g2.translate(x, y);
            g2.scale(zoom, zoom);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(base, 0, 0, c);
            if (actions != null) {
                g2.drawImage(actions, 0, 0, c);
            }
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
}
