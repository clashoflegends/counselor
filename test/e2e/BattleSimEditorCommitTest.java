package e2e;

import gui.services.BattleSimEditorCommit;
import javax.swing.DefaultCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleSimEditorCommitTest {
    private static JSpinner spinner(int value) {
        return new JSpinner(new SpinnerNumberModel(value, 0, 100, 1));
    }

    private static void type(JSpinner spinner, String text) {
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText(text);
    }

    @Test
    void invalidInputKeepsAllPendingValuesOutOfTheModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final JSpinner first = spinner(10), second = spinner(20);
            type(first, "77");
            type(second, "invalid");
            assertFalse(BattleSimEditorCommit.commit(new JTable(), first, second));
            assertEquals(10, first.getValue());
            assertEquals(20, second.getValue());
            assertEquals("invalid", ((JSpinner.DefaultEditor) second.getEditor()).getTextField().getText());
            type(second, "101");
            assertFalse(BattleSimEditorCommit.commit(new JTable(), first, second));
            type(second, "66");
            assertTrue(BattleSimEditorCommit.commit(new JTable(), first, second));
            assertEquals(77, first.getValue());
            assertEquals(66, second.getValue());
        });
    }

    @Test
    void rejectedTableEditPreventsTheSpinnerCommit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final JSpinner spinner = spinner(10);
            type(spinner, "77");
            final JTable table = new JTable(new DefaultTableModel(new Object[][]{{"old"}}, new Object[]{"value"}));
            table.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
                @Override public boolean stopCellEditing() { return false; }
            });
            assertTrue(table.editCellAt(0, 0));
            assertFalse(BattleSimEditorCommit.commit(table, spinner));
            assertTrue(table.isEditing());
            assertEquals(10, spinner.getValue());
        });
    }

    @Test
    void validTableAndSpinnerEditsCommitOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final JSpinner spinner = spinner(10);
            type(spinner, "77");
            final JTable table = new JTable(new DefaultTableModel(new Object[][]{{"old"}}, new Object[]{"value"}));
            final int[] writes = {0};
            table.getModel().addTableModelListener(event -> writes[0]++);
            assertTrue(table.editCellAt(0, 0));
            ((JTextField) table.getEditorComponent()).setText("new");
            assertTrue(BattleSimEditorCommit.commit(table, spinner));
            assertEquals("new", table.getValueAt(0, 0));
            assertEquals(77, spinner.getValue());
            assertEquals(1, writes[0]);
            assertTrue(BattleSimEditorCommit.commit(table, spinner));
            assertEquals(1, writes[0]);
        });
    }
}
