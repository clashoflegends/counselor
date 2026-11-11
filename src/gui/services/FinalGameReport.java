package gui.services;

import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import persistenceCommons.BundleManager;

/** Opens the protected website report; a loaded EGF cannot establish current Site eligibility. */
public final class FinalGameReport {
    private FinalGameReport() { }

    public static URI address(int gameId) {
        if (gameId <= 0) { throw new IllegalArgumentException("Invalid game ID"); }
        return URI.create("http://clashlegends.com/PbmSite/p_chronicle.php?id=" + gameId + "#stats");
    }

    public static void open(Component parent, BundleManager labels, int gameId) {
        final URI uri = address(gameId);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(uri); return true;
                    }
                } catch (Exception ex) { /* The selectable address below is the recovery path. */ }
                return false;
            }
            @Override protected void done() {
                try { if (get()) { return; } } catch (Exception ex) { /* Show the address. */ }
                JTextArea text = new JTextArea(labels.getString("VDASH.REPORT.BROWSER") + "\n\n" + uri);
                text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
                text.setColumns(48); text.setRows(4);
                JOptionPane.showMessageDialog(parent, text, labels.getString("VDASH.REPORT"), JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }
}
