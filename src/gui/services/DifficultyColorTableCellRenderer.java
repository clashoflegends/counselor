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

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // When selected, keep the standard selection colors super already applied.
        if (!isSelected) {
            Color bg = colorFor(value);
            if (bg != null) {
                c.setBackground(bg);
                c.setForeground(Color.BLACK);
            } else {
                // Reset: the renderer component is reused across cells, so an
                // unmatched cell must clear any tint left by a previous paint.
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
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
