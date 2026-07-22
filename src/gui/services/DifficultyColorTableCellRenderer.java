package gui.services;

import java.awt.Color;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * Tints the order Difficulty column by level to help new players gauge an
 * order's reliability at a glance:
 * <ul>
 * <li>red for Hard (DIFICIL)</li>
 * <li>yellow for Average (MEDIA)</li>
 * <li>green for Easy or Automatic (FACIL / AUTOMATICA)</li>
 * <li>blue for Varies and any other value (VARIADA / ...)</li>
 * </ul>
 * Language-agnostic by construction: the cell text and the comparison labels
 * both come from the active {@code labels} bundle, so matching works in every
 * supported language without a per-language lookup table. Empty/disabled order
 * slots (null or "-") are left uncolored. The selection highlight is preserved
 * for the selected row.
 *
 * @author jmoura
 */
public class DifficultyColorTableCellRenderer extends DefaultTableCellRenderer implements Serializable {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private static final Color HARD = new Color(255, 205, 210);   // light red
    private static final Color MEDIUM = new Color(255, 243, 160);  // light yellow
    private static final Color EASY = new Color(200, 235, 200);    // light green
    private static final Color OTHER = new Color(197, 220, 247);   // light blue

    // Dark-theme variants (used under a FlatLaf dark L&F) so difficulty tints aren't light islands.
    private static final Color HARD_DARK = new Color(95, 38, 42);
    private static final Color MEDIUM_DARK = new Color(92, 78, 30);
    private static final Color EASY_DARK = new Color(38, 74, 44);
    private static final Color OTHER_DARK = new Color(38, 60, 92);
    private static final Color DARK_TEXT = new Color(0xE6E6E6);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // When selected, keep the standard selection colors super already applied.
        if (!isSelected) {
            Color bg = colorFor(value);
            if (bg != null) {
                if (com.formdev.flatlaf.FlatLaf.isLafDark()) {
                    c.setBackground(darkTint(bg));
                    c.setForeground(DARK_TEXT);
                } else {
                    c.setBackground(bg);
                    c.setForeground(Color.BLACK);
                }
            } else {
                // Explicit reset. DefaultTableCellRenderer stores the last setBackground as its
                // "unselected" color and reuses it for the next unselected cell, so an unmatched cell
                // must clear any tint left by a previously rendered row - otherwise "-"/empty difficulty
                // slots inherit a colour on scroll, sort, or filter changes.
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }

    private static Color darkTint(Color lightTint) {
        if (lightTint == HARD) {
            return HARD_DARK;
        }
        if (lightTint == MEDIUM) {
            return MEDIUM_DARK;
        }
        if (lightTint == EASY) {
            return EASY_DARK;
        }
        return OTHER_DARK;
    }

    private Color colorFor(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        if (s.isEmpty() || "-".equals(s)) {
            return null;
        }
        if (s.equalsIgnoreCase(labels.getString("DIFICIL"))) {
            return HARD;
        }
        if (s.equalsIgnoreCase(labels.getString("MEDIA"))) {
            return MEDIUM;
        }
        if (s.equalsIgnoreCase(labels.getString("FACIL")) || s.equalsIgnoreCase(labels.getString("AUTOMATICA"))) {
            return EASY;
        }
        // VARIADA plus any other non-empty difficulty -> blue ("varies and others").
        return OTHER;
    }
}
