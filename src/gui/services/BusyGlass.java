package gui.services;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Lightweight modal busy overlay shown on a window's glass pane while a background task runs
 * (e.g. submitting orders, which can take several seconds). Dims the window, shows an indeterminate
 * progress bar + message, and swallows mouse/keyboard input so the UI can't be touched mid-operation.
 *
 * Usage (on the EDT):
 *   BusyGlass busy = BusyGlass.show(someComponent, "Submitting orders...");
 *   // ... start SwingWorker; in done(): busy.hide();
 */
public class BusyGlass extends JComponent {

    private final JRootPane rootPane;
    private final Component previousGlass;

    private BusyGlass(JRootPane rootPane, String message) {
        this.rootPane = rootPane;
        this.previousGlass = rootPane.getGlassPane();

        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setOpaque(false);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        box.add(label, BorderLayout.NORTH);
        box.add(bar, BorderLayout.CENTER);
        box.setPreferredSize(new Dimension(260, 48));
        add(box);

        // Swallow input while visible so nothing underneath can be clicked or typed.
        addMouseListener(new MouseAdapter() {
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
            }
        });
        setFocusTraversalKeysEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 120)); // translucent dim
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /**
     * Shows the overlay on the root pane that contains {@code anchor}. Returns the instance so the
     * caller can hide it when the background task finishes. No-op fallback (returns an instance whose
     * hide() does nothing) if no root pane is found.
     */
    public static BusyGlass show(Component anchor, String message) {
        JRootPane rp = SwingUtilities.getRootPane(anchor);
        if (rp == null) {
            return null;
        }
        BusyGlass glass = new BusyGlass(rp, message);
        rp.setGlassPane(glass);
        glass.setVisible(true);
        glass.requestFocusInWindow();
        return glass;
    }

    /** Restores the previous glass pane. Safe to call from the EDT; tolerates a null instance. */
    public void hide() {
        setVisible(false);
        if (previousGlass != null) {
            rootPane.setGlassPane(previousGlass);
            previousGlass.setVisible(false);
        }
    }
}
