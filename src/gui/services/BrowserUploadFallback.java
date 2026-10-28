package gui.services;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import modelWeb.PartidaJogadorWebInfo;
import persistenceCommons.WebCounselorManager;
import persistenceCommons.WebCounselorManager.UploadResult;

/** Recovery for an upload intercepted by the host's browser verification page. */
public final class BrowserUploadFallback {
    // Open our normal page, never a URL or script supplied by the failed response.
    static final URI MY_GAMES = URI.create("http://clashlegends.com/PbmSite/p_mygames.php");

    private BrowserUploadFallback() { }

    public static void show(Component parent, Function<String, String> labels, File attachment,
            PartidaJogadorWebInfo info, UploadResult result) {
        JPanel panel = createPanel(labels, attachment, info, result, new DesktopActions());
        String title = result.code() == WebCounselorManager.ERROR_BROWSER_VERIFICATION
                ? "UPLOAD.BROWSER.TITLE" : "WEBSITE.HTML.TITLE";
        JOptionPane.showOptionDialog(parent, panel, labels.apply(title),
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{labels.apply("UPLOAD.BROWSER.CLOSE")}, null);
    }

    /** Package-visible so tests exercise the real buttons without launching external applications. */
    static JPanel createPanel(Function<String, String> labels, File attachment, PartidaJogadorWebInfo info,
            UploadResult result, Actions actions) {
        File savedFile = attachment.getAbsoluteFile();
        boolean selfUpload = !info.isOnBehalf() && !info.isShadow();
        String guidance = info.isShadow() ? labels.apply("UPLOAD.BROWSER.SHADOW")
                : info.isOnBehalf() ? labels.apply("UPLOAD.BROWSER.BEHALF")
                : String.format(labels.apply("UPLOAD.BROWSER.SELF"), info.getPlayerLogin(), info.getGameId());
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        String explanation = result.code() == WebCounselorManager.ERROR_BROWSER_VERIFICATION
                ? "UPLOAD.BROWSER.MESSAGE" : "UPLOAD.HTML.MESSAGE";
        JTextArea message = new JTextArea(labels.apply(explanation) + "\n\n"
                + WebsiteResponseMessages.format(labels, result.message()) + "\n\n" + guidance);
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setColumns(56);
        message.setRows(11);
        message.setFont(new JLabel().getFont());
        message.setBackground(panel.getBackground());
        panel.add(new JScrollPane(message), BorderLayout.NORTH);

        JPanel details = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        details.add(new JLabel(labels.apply("UPLOAD.BROWSER.FILE")));
        JTextField path = new JTextField(savedFile.getPath());
        path.setEditable(false);
        path.setColumns(56);
        details.add(path);
        if (selfUpload) {
            JTextField url = new JTextField(MY_GAMES.toString());
            url.setEditable(false);
            details.add(url);
        }
        panel.add(details, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JTextArea failure = new JTextArea(2, 56);
        failure.setEditable(false);
        failure.setLineWrap(true);
        failure.setWrapStyleWord(true);
        failure.setFont(message.getFont());
        failure.setBackground(panel.getBackground());
        if (selfUpload) {
            JButton website = new JButton(labels.apply("UPLOAD.BROWSER.OPEN"));
            website.addActionListener(event -> launch(website, failure,
                    labels.apply("UPLOAD.BROWSER.OPEN.FAILED"), () -> actions.browse(MY_GAMES)));
            buttons.add(website);
        }
        JButton folder = new JButton(labels.apply("UPLOAD.BROWSER.FOLDER"));
        folder.addActionListener(event -> launch(folder, failure,
                labels.apply("UPLOAD.BROWSER.FOLDER.FAILED"), () -> {
                    if (!savedFile.isFile()) {
                        throw new IOException("Saved file is unavailable");
                    }
                    actions.openFolder(savedFile.getParentFile());
                }));
        buttons.add(folder);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(failure, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private static void launch(JButton button, JTextArea failure, String failureText, Operation operation) {
        button.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                operation.run();
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    failure.setText("");
                } catch (Exception ex) {
                    // Keep the copyable path/URL and both actions available after an OS launch failure.
                    failure.setText(failureText);
                } finally {
                    button.setEnabled(true);
                }
            }
        }.execute();
    }

    @FunctionalInterface
    private interface Operation { void run() throws Exception; }

    interface Actions {
        void browse(URI uri) throws IOException;
        void openFolder(File folder) throws IOException;
    }

    private static final class DesktopActions implements Actions {
        private Desktop desktop(Desktop.Action action) throws IOException {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(action)) {
                throw new IOException("Desktop action is unavailable");
            }
            return Desktop.getDesktop();
        }
        @Override public void browse(URI uri) throws IOException {
            desktop(Desktop.Action.BROWSE).browse(uri);
        }
        @Override public void openFolder(File folder) throws IOException {
            desktop(Desktop.Action.OPEN).open(folder);
        }
    }
}
