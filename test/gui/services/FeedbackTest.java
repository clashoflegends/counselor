package gui.services;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {
    @Test void cancelledRecoveryKeepsBothChoicesAvailable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls = new AtomicInteger();
            JPanel panel = Feedback.choices("Unsaved", new String[]{"Save As", "Retry"},
                    () -> false, () -> { calls.incrementAndGet(); return true; });
            JPanel buttons = (JPanel) panel.getComponent(1);
            JButton saveAs = (JButton) buttons.getComponent(0);
            JButton retry = (JButton) buttons.getComponent(1);
            saveAs.doClick(0);
            assertTrue(saveAs.isEnabled());
            assertTrue(retry.isEnabled());
            retry.doClick(0);
            assertEquals(1, calls.get());
            assertFalse(saveAs.isEnabled());
        });
    }
    @Test void recoveryChoicesExecuteOnlyOnceAndCannotRunTheirSiblingAfterwards() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls = new AtomicInteger();
            JPanel panel = Feedback.actions("Uncertain delivery", new String[]{"Retry", "Save As"},
                    calls::incrementAndGet, () -> calls.addAndGet(100));
            JPanel buttons = (JPanel) panel.getComponent(1);
            JButton retry = (JButton) buttons.getComponent(0);
            JButton saveAs = (JButton) buttons.getComponent(1);
            retry.doClick(0);
            retry.doClick(0);
            saveAs.doClick(0);
            assertEquals(1, calls.get());
        });
    }

    @Test void laterNotificationsDoNotEraseEarlierRecoveryAndRecordingOpensNoWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                var entriesField = Feedback.class.getDeclaredField("ENTRIES");
                entriesField.setAccessible(true);
                DefaultListModel<?> entries = (DefaultListModel<?>) entriesField.get(null);
                int before = entries.size();
                JPanel recovery = Feedback.actions("Unsaved plan", new String[]{"Retry"}, () -> {});
                Feedback.report(null, "Unsaved", recovery);
                Object saved = entries.get(0);
                Feedback.report(null, "Later message", "A later message");
                assertEquals(before + 2, entries.size());
                assertSame(saved, entries.get(1));
                var history = Feedback.class.getDeclaredField("history");
                history.setAccessible(true);
                assertNull(history.get(null), "recording may not construct/open a dialog");
            } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        });
    }

    @Test void allFiveLocalesHaveTheCompleteFeedbackVocabulary() {
        var expected = ResourceBundle.getBundle("gui.services.feedback", Locale.ENGLISH).keySet();
        for (String language : new String[]{"en", "pt", "es", "it", "ca"}) {
            ResourceBundle bundle = ResourceBundle.getBundle("gui.services.feedback", Locale.forLanguageTag(language));
            assertEquals(expected, bundle.keySet());
            for (String key : expected) assertFalse(bundle.getString(key).isBlank(), language + ":" + key);
        }
    }
}
