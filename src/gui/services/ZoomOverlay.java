package gui.services;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * A transient, semi-transparent "150%" badge shown while the player zooms the map (Ctrl+wheel).
 * Each {@link #show(String)} refreshes the text to full opacity and restarts a 5s hold timer; once
 * the player stops changing the zoom, it holds for 5s then fades out. Self-contained: it sizes itself
 * to the text and paints a rounded translucent pill, so the caller only positions it.
 */
public class ZoomOverlay extends JComponent {

    private static final Font FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final int PAD_X = 16, PAD_Y = 9, ARC = 16;
    private static final int HOLD_MS = 5000;   // visible after the last change

    private String text = "";
    private float alpha = 0f;
    private final Timer holdTimer;
    private final Timer fadeTimer;

    public ZoomOverlay() {
        setOpaque(false);
        setFont(FONT);
        // fadeTimer first (its lambda refers to itself via getSource(), not the blank-final field),
        // so holdTimer's lambda can safely capture an already-assigned fadeTimer.
        fadeTimer = new Timer(40, e -> {
            alpha -= 0.08f;
            if (alpha <= 0f) {
                alpha = 0f;
                ((Timer) e.getSource()).stop();
                setVisible(false);
            }
            repaint();
        });
        holdTimer = new Timer(HOLD_MS, e -> fadeTimer.start());
        holdTimer.setRepeats(false);
    }

    /** Show (or refresh) the badge at full opacity and restart the 5s hold. */
    public void show(String msg) {
        this.text = msg;
        this.alpha = 1f;
        fadeTimer.stop();
        setVisible(true);
        holdTimer.restart();
        repaint();
    }

    /** Preferred badge size for the current text - the caller uses this to position the overlay. */
    public Dimension badgeSize() {
        FontMetrics fm = getFontMetrics(FONT);
        return new Dimension(fm.stringWidth(text) + 2 * PAD_X, fm.getHeight() + 2 * PAD_Y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (alpha <= 0f || text.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(FONT);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
            g2.setColor(Color.WHITE);
            g2.setFont(FONT);
            g2.drawString(text, PAD_X, PAD_Y + fm.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
