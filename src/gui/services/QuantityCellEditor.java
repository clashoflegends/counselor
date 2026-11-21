package gui.services;

import java.awt.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;

/** Integer table input that keeps invalid drafts visible without entering a modal event loop. */
public final class QuantityCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JTextField field = new JTextField();
    private final int minimum;
    private final int maximum;
    private final DecimalFormat format;
    private final Border normalBorder = field.getBorder();
    private Integer value;

    public QuantityCellEditor(int minimum, int maximum, String quantityLabel) {
        this.minimum = minimum;
        this.maximum = maximum;
        format = (DecimalFormat) NumberFormat.getIntegerInstance();
        format.setParseBigDecimal(true);
        field.setHorizontalAlignment(JTextField.TRAILING);
        field.setToolTipText(quantityLabel + ": " + minimum + " - " + maximum + " (Esc)");
        field.getAccessibleContext().setAccessibleDescription(field.getToolTipText());
        field.addActionListener(e -> stopCellEditing());
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateDraft(); }
            public void removeUpdate(DocumentEvent e) { validateDraft(); }
            public void changedUpdate(DocumentEvent e) { validateDraft(); }
        });
    }

    private Integer parsed() {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        ParsePosition pos = new ParsePosition(0);
        Number number = format.parse(text, pos);
        if (!(number instanceof BigDecimal decimal) || pos.getIndex() != text.length()) return null;
        try {
            int candidate = decimal.intValueExact();
            // DecimalFormat accepts malformed grouping ("1,,0", "12,34"). A grouped value must
            // match the locale's actual integer representation; ungrouped digits remain valid.
            boolean digits = text.codePoints().allMatch(Character::isDigit);
            if (!digits && !text.equals(format.format(candidate))) return null;
            return candidate < minimum || candidate > maximum ? null : candidate;
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private void validateDraft() {
        boolean valid = parsed() != null;
        field.setBorder(valid ? normalBorder : BorderFactory.createLineBorder(Toast.ERROR_BG, 2));
        field.putClientProperty("JComponent.outline", valid ? null : "error");
    }

    @Override public Component getTableCellEditorComponent(JTable table, Object value,
            boolean selected, int row, int column) {
        this.value = value instanceof Number number ? number.intValue() : minimum;
        field.setText(format.format(this.value));
        field.selectAll();
        return field;
    }

    @Override public Object getCellEditorValue() { return value; }

    @Override public boolean stopCellEditing() {
        Integer candidate = parsed();
        if (candidate == null) {
            validateDraft();
            field.requestFocusInWindow();
            return false;
        }
        value = candidate;
        return super.stopCellEditing();
    }
}
