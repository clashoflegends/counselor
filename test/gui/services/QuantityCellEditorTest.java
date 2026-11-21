package gui.services;

import java.awt.event.ActionEvent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuantityCellEditorTest {
    private JTable table() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{25}}, new Object[]{"Quantity"}));
        table.getColumnModel().getColumn(0).setCellEditor(new QuantityCellEditor(0, 100000, "Quantity"));
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        assertTrue(table.editCellAt(0, 0));
        return table;
    }

    @Test void invalidDraftsSurviveRepeatedFocusLossValidationWithoutWritingTheModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String bad : new String[]{"", " ", "abc", "12x", "1.5", "-1", "100001", "999999999999999999999999",
                    "1,,0", "12,34", "1,", ",1"}) {
                JTable table = table();
                JTextField field = (JTextField) table.getEditorComponent();
                field.setText(bad);
                for (int i = 0; i < 4; i++) assertFalse(table.getCellEditor().stopCellEditing(), bad);
                assertTrue(table.isEditing());
                assertEquals(bad, field.getText());
                assertEquals(25, table.getValueAt(0, 0));
                assertEquals("error", field.getClientProperty("JComponent.outline"));
                field.setText("4321");
                assertTrue(table.getCellEditor().stopCellEditing());
                assertEquals(4321, table.getValueAt(0, 0));
            }
        });
    }

    @Test void enterCommitsExactlyOnceAfterCorrection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table();
            int[] writes = {0};
            table.getModel().addTableModelListener(e -> writes[0]++);
            JTextField field = (JTextField) table.getEditorComponent();
            field.setText("invalid");
            field.postActionEvent();
            assertTrue(table.isEditing());
            assertEquals(0, writes[0]);
            field.setText("74");
            field.postActionEvent();
            assertFalse(table.isEditing());
            assertEquals(74, table.getValueAt(0, 0));
            assertEquals(1, writes[0]);
        });
    }

    @Test void escapeCancelsAnInvalidDraftWithoutCommittingOrClamping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table();
            ((JTextField) table.getEditorComponent()).setText("99999999999999999999");
            table.getActionMap().get("cancel").actionPerformed(new ActionEvent(table, 0, "cancel"));
            assertFalse(table.isEditing());
            assertEquals(25, table.getValueAt(0, 0));
        });
    }

    @Test void bothRangeBoundariesAndLocaleFormattedIntegersCommit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int quantity : new int[]{0, 100000, 98765}) {
                JTable table = table();
                ((JTextField) table.getEditorComponent()).setText(java.text.NumberFormat.getIntegerInstance().format(quantity));
                assertTrue(table.getCellEditor().stopCellEditing());
                assertEquals(quantity, table.getValueAt(0, 0));
                assertInstanceOf(Integer.class, table.getValueAt(0, 0));
            }
        });
    }
}
