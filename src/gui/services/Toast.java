package gui.services;

import java.awt.Color;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Non-modal toast / snackbar. Slides up from the owner window's bottom-left (just above the status
 * bar), shows a message for ~10 seconds, then auto-dismisses. Click it to dismiss early. It never
 * steals focus and never blocks other operations - it is a plain owned {@link JWindow}, not a dialog.
 *
 * Reusable: call {@link #show(Window, String)} from anywhere; it marshals to the EDT itself. One
 * toast is shown at a time - a new one replaces the current (keeps it simple; stacking can be added
 * later if more concurrent use cases appear).
 *
 * Example: {@code Toast.show(SwingUtilities.getWindowAncestor(someComponent), "Turn received by Judge.");}
 */
public final class Toast {

    private static final int DISPLAY_MS = 10_000; // visible time before auto-dismiss
    private static final int MARGIN_X = 16;        // left inset from the owner's edge
    private static final int BOTTOM_GAP = 48;      // lifts the toast above the status bar
    private static final int SLIDE_PX = 40;        // how far below the resting spot it starts
    private static final int SLIDE_STEP = 5;       // px per animation tick
    private static final int SLIDE_TICK_MS = 15;
    private static final int MAX_WIDTH = 440;      // wrap longer messages instead of growing very wide

    private static final Color REST_BG = new Color(0x1a3c6e);  // resting blue
    private static final Color FLASH_BG = new Color(0x8a93a3); // grey flash to draw the eye
    private static final int FLASH_TICKS = 6;                  // ~6 * 150ms toggles (~0.9s) then settle

    private static Toast current; // single active toast; EDT-confined, no locking needed

    private final JWindow window;
    private final JPanel panel;
    private Timer slideTimer;
    private Timer holdTimer;
    private Timer flashTimer;

    private Toast(Window owner, String message, Icon icon) {
        window = new JWindow(owner);
        window.setFocusableWindowState(false); // never steal keyboard focus from the app
        window.setAutoRequestFocus(false);
        panel = new JPanel();
        panel.setBackground(REST_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x0f2647)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        if (icon != null) {
            label.setIcon(icon);
            label.setIconTextGap(10);
        }
        panel.add(label);
        window.setContentPane(panel);
        window.pack();
        if (window.getWidth() > MAX_WIDTH) {
            // Long message (e.g. a list of missing actions): wrap it instead of a screen-wide toast.
            label.setText("<html><body style='width:" + (MAX_WIDTH - 56) + "px'>" + escapeHtml(message) + "</body></html>");
            window.pack();
        }
        MouseAdapter clickToDismiss = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dismiss();
            }
        };
        panel.addMouseListener(clickToDismiss);
        label.addMouseListener(clickToDismiss);
    }

    /** Show a toast for {@code message}, anchored to {@code owner}'s bottom-left. Safe to call off-EDT. */
    public static void show(final Window owner, final String message) {
        show(owner, message, null);
    }

    /**
     * Convenience for callers that do not hold a window reference: anchors to the currently active
     * window, falling back to the first visible frame (the main Counselor window).
     */
    public static void show(final String message, final Icon icon) {
        Window w = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (w == null || !w.isShowing()) {
            for (Frame f : Frame.getFrames()) {
                if (f.isShowing()) {
                    w = f;
                    break;
                }
            }
        }
        show(w, message, icon);
    }

    /** As {@link #show(Window, String)} but with a leading icon (e.g. a warning/info icon). */
    public static void show(final Window owner, final String message, final Icon icon) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(owner, message, icon));
            return;
        }
        if (owner == null || !owner.isShowing() || message == null || message.isEmpty()) {
            return;
        }
        if (current != null) {
            current.dismiss(); // one at a time: replace the previous toast
        }
        current = new Toast(owner, message, icon);
        current.slideIn(owner);
    }

    private void slideIn(Window owner) {
        final int x = owner.getX() + MARGIN_X;
        final int targetY = owner.getY() + owner.getHeight() - window.getHeight() - BOTTOM_GAP;
        final int[] y = {targetY + SLIDE_PX};
        window.setLocation(x, y[0]);
        window.setVisible(true);
        slideTimer = new Timer(SLIDE_TICK_MS, null);
        slideTimer.addActionListener(e -> {
            y[0] -= SLIDE_STEP;
            if (y[0] <= targetY) {
                window.setLocation(x, targetY);
                slideTimer.stop();
                startFlash(); // grey<->blue flicker on arrival to draw attention
                holdTimer = new Timer(DISPLAY_MS, ev -> dismiss());
                holdTimer.setRepeats(false);
                holdTimer.start();
            } else {
                window.setLocation(x, y[0]);
            }
        });
        slideTimer.start();
    }

    /** Briefly flash the background grey<->blue (~0.9s) so a freshly-shown toast catches the eye. */
    private void startFlash() {
        final int[] ticks = {FLASH_TICKS};
        flashTimer = new Timer(150, null);
        flashTimer.addActionListener(e -> {
            if (ticks[0] <= 0) {
                panel.setBackground(REST_BG);
                flashTimer.stop();
                return;
            }
            panel.setBackground((ticks[0] % 2 == 0) ? FLASH_BG : REST_BG);
            ticks[0]--;
        });
        flashTimer.start();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void dismiss() {
        if (slideTimer != null) {
            slideTimer.stop();
        }
        if (flashTimer != null) {
            flashTimer.stop();
        }
        if (holdTimer != null) {
            holdTimer.stop();
        }
        window.setVisible(false);
        window.dispose();
        if (current == this) {
            current = null;
        }
    }
}
