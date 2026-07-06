package gui.services;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Theme-aware vector button icons. Loads an SVG from {@code images/icons/<name>.svg} (the bundled
 * Tabler MIT set) and recolours it to the current Look-and-Feel {@code Button.foreground}, so the
 * icon stays crisp at any scale and follows light/dark themes - same treatment as the main toolbar.
 */
public final class SvgIcon {

    private SvgIcon() {
    }

    /** A {@code size} x {@code size} themed icon for {@code images/icons/<name>.svg}. */
    public static Icon themed(String name, int size) {
        FlatSVGIcon icon = new FlatSVGIcon("images/icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> UIManager.getColor("Button.foreground")));
        return icon;
    }

    /**
     * A {@code size} x {@code size} icon for {@code images/icons/<name>.svg} recoloured to a FIXED
     * {@code color} (ignores the theme foreground). Used to tint a glyph to a status colour - e.g. the
     * submit button following the order-sync state. After a light/dark switch the tint persists until
     * the caller recolours again.
     */
    public static Icon colored(String name, int size, java.awt.Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("images/icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }
}
