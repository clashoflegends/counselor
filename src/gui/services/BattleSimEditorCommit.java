package gui.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;

/** Validates pending numeric text before a BattleSim command can consume the editors. */
public final class BattleSimEditorCommit {
    private BattleSimEditorCommit() {
    }

    public static boolean commit(JTable table, JSpinner... spinners) {
        final List<Object> values = new ArrayList<>();
        for (JSpinner spinner : spinners) {
            if (!spinner.isEnabled() || !(spinner.getEditor() instanceof JSpinner.DefaultEditor)) {
                values.add(spinner.getValue());
                continue;
            }
            final JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            try {
                final Object value = field.getFormatter().stringToValue(field.getText());
                if (spinner.getModel() instanceof SpinnerNumberModel && value instanceof Number) {
                    final SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
                    final double number = ((Number) value).doubleValue();
                    if ((model.getMinimum() != null && ((Number) model.getMinimum()).doubleValue() > number)
                            || (model.getMaximum() != null && ((Number) model.getMaximum()).doubleValue() < number)) {
                        throw new ParseException("Outside editor range", 0);
                    }
                }
                values.add(value);
            } catch (ParseException | IllegalArgumentException ex) {
                field.requestFocusInWindow();
                field.selectAll();
                javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(field);
                return false;
            }
        }
        if (table != null && table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            table.getEditorComponent().requestFocusInWindow();
            return false;
        }
        for (int i = 0; i < spinners.length; i++) {
            spinners[i].setValue(values.get(i));
        }
        return true;
    }
}
