package gui.services;

import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 * Single funnel for "copy to system clipboard" from the Counselor. Routes through
 * {@link SysApoio#setClipboardContents(String)} (which retries a locked Windows clipboard, then gives up
 * gracefully) and, when the copy could not reach the clipboard, shows a red error {@link Toast} so the
 * player knows it failed instead of silently getting nothing. Use this instead of calling
 * {@code SysApoio.setClipboardContents} directly from UI code.
 */
public final class ClipboardHelper {

    private ClipboardHelper() {
    }

    /** Copy {@code text} to the clipboard; on failure show a red auto-dismiss toast. */
    public static void copy(String text) {
        if (!SysApoio.setClipboardContents(text)) {
            Toast.showError(SettingsManager.getInstance().getBundleManager().getString("CLIPBOARD.COPY.FAIL"));
        }
    }
}
