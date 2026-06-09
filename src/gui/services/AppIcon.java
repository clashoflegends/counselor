package gui.services;

import business.ImageManager;
import java.awt.Window;

/**
 * Applies the Counselor app icon (the hex icon) to a window's title bar / taskbar entry.
 *
 * Dialogs created with a throwaway {@code new JFrame()} owner otherwise show the default Java
 * "coffee cup" icon. Call {@link #applyTo(Window)} on any JDialog/JFrame that isn't already given a
 * themed icon (e.g. terrain/chart icons are intentional and left alone).
 */
public final class AppIcon {

    private AppIcon() {
    }

    public static void applyTo(Window w) {
        if (w != null) {
            w.setIconImage(ImageManager.getInstance().getIconApp());
        }
    }
}
