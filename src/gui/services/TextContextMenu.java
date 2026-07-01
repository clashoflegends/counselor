package gui.services;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 * App-wide clipboard support for EVERY text component (JTextArea, JTextField, JTextPane, ...):
 * <ul>
 *   <li>a right-click context menu with Cut / Copy / Paste / Select All, and</li>
 *   <li>a Ctrl+C safety net that copies the current selection.</li>
 * </ul>
 * Installed once at startup via a global AWT listener + a key post-processor, so it covers every
 * text field in every window and dialog without touching the individual .form files. The menu's
 * actions call {@code JTextComponent.copy()/cut()/...} directly, so Copy works regardless of what
 * shadows the default key binding (Swing itself ships no default context menu for text components).
 */
public final class TextContextMenu {

    private TextContextMenu() {
    }

    public static void install() {
        // Right-click anywhere on a text component -> context menu.
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (!(e instanceof MouseEvent)) {
                return;
            }
            MouseEvent me = (MouseEvent) e;
            if (me.isPopupTrigger() && me.getComponent() instanceof JTextComponent) {
                showMenu((JTextComponent) me.getComponent(), me);
            }
        }, AWTEvent.MOUSE_EVENT_MASK);

        // Ctrl+C safety net: some text areas don't fire the default copy binding. Copy the selection
        // ourselves. copy() is idempotent, so this is harmless even when the default binding DID fire.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED
                    && e.getKeyCode() == KeyEvent.VK_C
                    && (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (fo instanceof JTextComponent) {
                    copySelection((JTextComponent) fo);
                }
            }
            return false; // never consume - let normal processing continue
        });
    }

    private static void showMenu(JTextComponent tc, MouseEvent me) {
        if (!tc.isEnabled()) {
            return;
        }
        final BundleManager labels = SettingsManager.getInstance().getBundleManager();
        final boolean hasSelection = tc.getSelectedText() != null;
        final boolean editable = tc.isEditable();

        JPopupMenu menu = new JPopupMenu();
        // Write the clipboard directly (StringSelection via SysApoio) rather than tc.copy()/cut()/paste():
        // those route through the text component's TransferHandler, which does not land text on the
        // Windows clipboard here - the toolbar Copy uses this same direct path and works.
        menu.add(item(tx(labels, "TEXT.CUT", "Cut"), editable && hasSelection, () -> {
            if (copySelection(tc)) {
                tc.replaceSelection("");
            }
        }));
        menu.add(item(tx(labels, "TEXT.COPY", "Copy"), hasSelection, () -> copySelection(tc)));
        menu.add(item(tx(labels, "TEXT.PASTE", "Paste"), editable && clipboardHasText(), () -> {
            String s = clipboardString();
            if (s != null) {
                tc.replaceSelection(s);
            }
        }));
        menu.addSeparator();
        menu.add(item(tx(labels, "TEXT.SELECTALL", "Select All"), tc.getDocument().getLength() > 0, tc::selectAll));
        menu.show(tc, me.getX(), me.getY());
    }

    /** Copy the component's current selection to the system clipboard via the direct StringSelection
     *  path (bypasses the TransferHandler that tc.copy() relies on). Returns true if something was copied. */
    private static boolean copySelection(JTextComponent tc) {
        String sel = tc.getSelectedText();
        if (sel == null || sel.isEmpty()) {
            return false;
        }
        SysApoio.setClipboardContents(sel);
        return true;
    }

    private static String clipboardString() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return (data instanceof String) ? (String) data : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static JMenuItem item(String text, boolean enabled, Runnable action) {
        JMenuItem mi = new JMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        mi.setEnabled(enabled);
        return mi;
    }

    private static boolean clipboardHasText() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            return true; // if we can't inspect the clipboard, let Paste try anyway
        }
    }

    private static String tx(BundleManager labels, String key, String fallback) {
        String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
