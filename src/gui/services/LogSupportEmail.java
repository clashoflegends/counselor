package gui.services;

import control.services.LogSupportArchive;
import control.services.LogSupportArchive.Archive;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/** User-initiated local ZIP export and email draft; never sends email itself. */
public final class LogSupportEmail {
    private static final Logger LOG = Logger.getLogger(LogSupportEmail.class);
    private LogSupportEmail() {}

    public static void prepare(Component parent, JButton button, Function<String, String> labels) {
        if (!button.isEnabled()) return;
        if (!(Logger.getRootLogger().getAppender("logfile") instanceof FileAppender appender)
                || appender.getFile() == null) {
            error(parent, labels);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(labels.apply("ABOUT.LOGS.SAVE"));
        chooser.setFileFilter(new FileNameExtensionFilter("ZIP", "zip"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(new File("counselor-logs-" + LocalDate.now() + ".zip"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath();
        if (!selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            selected = selected.resolveSibling(selected.getFileName() + ".zip");
        }
        final Path destination = selected;
        final boolean replace = Files.exists(destination);
        if (replace && JOptionPane.showConfirmDialog(parent, labels.apply("ABOUT.LOGS.REPLACE"),
                labels.apply("ABOUT.LOGS.TITLE"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        button.setEnabled(false);
        String caption = button.getText();
        button.setText(labels.apply("ABOUT.LOGS.PREPARING"));
        new SwingWorker<Archive, Void>() {
            @Override protected Archive doInBackground() throws Exception {
                // Force the daily appender to rotate an old active log before choosing calendar dates.
                LOG.info("Preparing diagnostic log archive at the player's request.");
                LocalDate today = LocalDate.now();
                Charset encoding = appender.getEncoding() == null ? Charset.defaultCharset()
                        : Charset.forName(appender.getEncoding());
                return LogSupportArchive.create(Path.of(appender.getFile()), destination, today, encoding,
                        List.of(SettingsManager.getInstance().getConfig("playerToken", "")), replace);
            }
            @Override protected void done() {
                try {
                    Archive archive = get();
                    JPanel panel = createPanel(labels, archive, new DesktopActions());
                    // Open a draft once; the panel keeps retry and manual-copy options available.
                    JButton email = (JButton) panel.getClientProperty("emailButton");
                    email.doClick();
                    JOptionPane.showMessageDialog(parent, panel, labels.apply("ABOUT.LOGS.TITLE"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    LOG.warn("Could not prepare diagnostic log archive.");
                    error(parent, labels);
                } finally {
                    button.setText(caption);
                    button.setEnabled(true);
                }
            }
        }.execute();
    }

    private static void error(Component parent, Function<String, String> labels) {
        JOptionPane.showMessageDialog(parent, labels.apply("ABOUT.LOGS.FAILED"),
                labels.apply("ABOUT.LOGS.TITLE"), JOptionPane.ERROR_MESSAGE);
    }

    static JPanel createPanel(Function<String, String> labels, Archive archive, Actions actions) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JTextArea message = text(String.format(labels.apply("ABOUT.LOGS.READY"), archive.firstDay(), archive.lastDay())
                + "\n\n" + labels.apply("ABOUT.LOGS.INCLUDED") + "\n" + String.join("\n", archive.files()), 7);
        panel.add(message, BorderLayout.NORTH);
        JPanel details = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        JTextField path = new JTextField(archive.path().toString());
        path.setEditable(false);
        JTextField recipient = new JTextField(LogSupportArchive.GM_EMAIL);
        recipient.setEditable(false);
        details.add(path);
        details.add(recipient);
        panel.add(details, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        JPanel buttons = new JPanel();
        JButton email = new JButton(labels.apply("ABOUT.LOGS.EMAIL"));
        JButton folder = new JButton(labels.apply("ABOUT.LOGS.FOLDER"));
        buttons.add(email);
        buttons.add(folder);
        JTextArea failure = text("", 3);
        String version = "2." + SysApoio.getVersionClash("version_counselor");
        URI uri = LogSupportArchive.emailUri(String.format(labels.apply("ABOUT.LOGS.SUBJECT"), version),
                String.format(labels.apply("ABOUT.LOGS.BODY"), archive.path().getFileName(), version,
                        archive.firstDay(), archive.lastDay()));
        email.addActionListener(e -> launch(email, failure, labels.apply("ABOUT.LOGS.EMAIL.FAILED"), () -> actions.mail(uri)));
        folder.addActionListener(e -> launch(folder, failure, labels.apply("ABOUT.LOGS.FOLDER.FAILED"),
                () -> actions.openFolder(archive.path().getParent().toFile())));
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(failure, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        panel.putClientProperty("emailButton", email);
        return panel;
    }

    private static JTextArea text(String value, int rows) {
        JTextArea text = new JTextArea(value, rows, 54);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new JLabel().getFont());
        text.setBackground(new JPanel().getBackground());
        return text;
    }

    private static void launch(JButton button, JTextArea failure, String message, Operation operation) {
        button.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { operation.run(); return null; }
            @Override protected void done() {
                try { get(); failure.setText(""); }
                catch (Exception ex) { failure.setText(message); }
                finally { button.setEnabled(true); }
            }
        }.execute();
    }

    @FunctionalInterface private interface Operation { void run() throws IOException; }
    interface Actions {
        void mail(URI uri) throws IOException;
        void openFolder(File folder) throws IOException;
    }
    private static final class DesktopActions implements Actions {
        private Desktop desktop(Desktop.Action action) throws IOException {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(action)) {
                throw new IOException("Desktop action unavailable");
            }
            return Desktop.getDesktop();
        }
        @Override public void mail(URI uri) throws IOException { desktop(Desktop.Action.MAIL).mail(uri); }
        @Override public void openFolder(File folder) throws IOException { desktop(Desktop.Action.OPEN).open(folder); }
    }
}
