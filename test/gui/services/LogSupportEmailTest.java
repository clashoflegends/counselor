package gui.services;

import control.services.LogSupportArchive;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LogSupportEmailTest {
    @Test void everyLocaleShowsAttachmentInstructionsAndIncludedFiles(@TempDir Path directory) throws Exception {
        var archive = new LogSupportArchive.Archive(directory.resolve("logs.zip"), LocalDate.of(2026, 1, 30),
                LocalDate.of(2026, 2, 1), List.of("counselor.log.2026-01-30", "counselor.log"));
        SwingUtilities.invokeAndWait(() -> {
            for (String language : List.of("", "pt", "es", "it", "ca")) {
                ResourceBundle labels = ResourceBundle.getBundle("labels", language.isEmpty() ? Locale.ROOT : Locale.of(language));
                JPanel panel = LogSupportEmail.createPanel(labels::getString, archive, new FakeActions());
                String text = text(panel);
                assertTrue(text.contains("2026-01-30"));
                assertTrue(text.contains("2026-02-01"));
                assertTrue(text.contains("counselor.log"));
                assertTrue(text.contains(archive.path().toString()));
                assertTrue(text.contains(LogSupportArchive.GM_EMAIL));
                assertFalse(text.contains("ABOUT.LOGS."));
                assertFalse(text.contains("%s"));
                assertFalse(text.contains("\u00c3"), "Mojibake in " + language);
                assertFalse(text.contains("\uFFFD"));
                assertTrue(String.format(labels.getString("ABOUT.LOGS.BODY"), "logs.zip", "2.940", archive.firstDay(), archive.lastDay()).contains("\n"));
            }
        });
    }

    @Test void failedEmailLaunchLeavesCopyablePathAndAllowsRetry(@TempDir Path directory) throws Exception {
        ResourceBundle labels = ResourceBundle.getBundle("labels", Locale.ROOT);
        var archive = new LogSupportArchive.Archive(directory.resolve("logs.zip"), LocalDate.now().minusDays(2),
                LocalDate.now(), List.of("counselor.log"));
        FakeActions actions = new FakeActions();
        AtomicReference<JPanel> panel = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            panel.set(LogSupportEmail.createPanel(labels::getString, archive, actions));
            JButton email = (JButton) panel.get().getClientProperty("emailButton");
            email.addPropertyChangeListener("enabled", e -> { if (Boolean.TRUE.equals(e.getNewValue())) finished.countDown(); });
            email.doClick();
        });
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(text(panel.get()).contains(labels.getString("ABOUT.LOGS.EMAIL.FAILED")));
            assertTrue(text(panel.get()).contains(archive.path().toString()));
            assertTrue(((JButton) panel.get().getClientProperty("emailButton")).isEnabled());
        });
        assertTrue(actions.uri.toString().startsWith("mailto:" + LogSupportArchive.GM_EMAIL));
        assertFalse(actions.onEdt);
    }

    private static String text(Component component) {
        StringBuilder text = new StringBuilder();
        if (component instanceof JTextComponent field) text.append(field.getText());
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) text.append(text(child));
        }
        return text.toString();
    }

    private static final class FakeActions implements LogSupportEmail.Actions {
        volatile URI uri;
        volatile boolean onEdt;
        @Override public void mail(URI uri) throws IOException {
            this.uri = uri;
            onEdt = SwingUtilities.isEventDispatchThread();
            throw new IOException("Simulated missing email application");
        }
        @Override public void openFolder(File folder) {}
    }
}
