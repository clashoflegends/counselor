package gui.services;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class FeedbackWindowTest {
    @BeforeAll static void display() { assumeFalse(GraphicsEnvironment.isHeadless()); }

    private static Object field(String name) throws Exception {
        Field field = Feedback.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    @Test void incomingMessagesKeepTheSelectedDetailsAndDoNotMakeTheViewModal() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Feedback.report(null, "First operation", "First details");
                Feedback.open(null);
                JDialog dialog = (JDialog) field("history");
                assertFalse(dialog.isModal());
                JList<?> list = (JList<?>) field("list");
                Object selected = list.getSelectedValue();
                Feedback.report(null, "Later operation", "Later details");
                assertSame(selected, list.getSelectedValue(), "background results may not replace the message being read");
            } catch (Exception ex) { throw new AssertionError(ex); }
            finally { try { ((JDialog) field("history")).dispose(); } catch (Exception ex) { throw new AssertionError(ex); } }
        });
        SwingUtilities.invokeAndWait(() -> {});
    }

    @Test void closingTheViewDetachesItFromHistoryAndReopeningRestoresDetails() throws Exception {
        final int[] before = {0};
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultListModel<?> entries = (DefaultListModel<?>) field("ENTRIES");
                before[0] = entries.getListDataListeners().length;
                Feedback.report(null, "Retained operation", "Retained details");
                Feedback.open(null);
                ((JDialog) field("history")).dispose();
            } catch (Exception ex) { throw new AssertionError(ex); }
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultListModel<?> entries = (DefaultListModel<?>) field("ENTRIES");
                assertEquals(before[0], entries.getListDataListeners().length);
                Feedback.open(null);
                assertNotNull(((JList<?>) field("list")).getSelectedValue());
                ((JDialog) field("history")).dispose();
            } catch (Exception ex) { throw new AssertionError(ex); }
        });
        SwingUtilities.invokeAndWait(() -> {});
    }
}
