/*
 * LafTint.java
 */
package gui.services;

import java.awt.Color;

/**
 * Look-and-Feel-aware tint palette for table risk/status coloring: a light tint for light L&amp;Fs and a
 * matching dark tint under FlatLaf dark, so tinted cells never become bright "islands" in dark mode.
 * The values mirror {@link DifficultyColorTableCellRenderer}'s palette so the whole app tints
 * consistently (red/amber/green/blue with a readable foreground per theme).
 *
 * @author jmoura
 */
public enum LafTint {

    RED(new Color(255, 205, 210), new Color(95, 38, 42)),      // bad / critical
    AMBER(new Color(255, 243, 160), new Color(92, 78, 30)),    // warning
    GREEN(new Color(200, 235, 200), new Color(38, 74, 44)),    // good
    BLUE(new Color(197, 220, 247), new Color(38, 60, 92));     // very good

    private static final Color DARK_TEXT = new Color(0xE6E6E6);

    private final Color light;
    private final Color dark;

    LafTint(Color light, Color dark) {
        this.light = light;
        this.dark = dark;
    }

    /** Background tint for the active Look and Feel (dark variant under FlatLaf dark). */
    public Color bg() {
        return com.formdev.flatlaf.FlatLaf.isLafDark() ? dark : light;
    }

    /** Readable foreground for a tinted cell under the active Look and Feel. */
    public static Color fg() {
        return com.formdev.flatlaf.FlatLaf.isLafDark() ? DARK_TEXT : Color.BLACK;
    }
}
