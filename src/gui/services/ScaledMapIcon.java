package gui.services;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
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

    private Image base;     // 1x composed map
    private Image actions;  // 1x full-map actions overlay, or null when hidden
    private double zoom = 1.0;

    public void setBase(Image base) {
        this.base = base;
    }

    public void setActions(Image actions) {
        this.actions = actions;
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
        } finally {
            g2.dispose();
        }
    }
}
