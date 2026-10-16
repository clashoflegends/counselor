package e2e;

import business.combat.ArmySim;
import control.BattleSimControler;
import gui.accessories.BattleSimWindow;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import model.Local;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Actual Swing listener ordering; numeric helper tests alone cannot exercise window refreshes. */
class BattleSimWindowEditingTest {
    @BeforeAll
    static void requiresDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Window interaction tests require a display");
    }

    private static BattleSimWindow open() throws Exception {
        final Local local = TestWorld.load().getLocais().values().stream()
                .filter(one -> !one.getExercitos().isEmpty()).findFirst().orElseThrow();
        final BattleSimWindow window = new BattleSimWindow(local);
        window.validate();
        return window;
    }

    private static Object field(BattleSimWindow window, String name) throws Exception {
        final Field field = BattleSimWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(window);
    }

    private static void type(JSpinner spinner, String value) {
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText(value);
    }

    @Test
    void spinnerIncrementCommitsAnotherPendingSpinnerWithoutUndoingTheIncrement() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            try {
                window = open();
                final ArmySim army = ((BattleSimControler) field(window, "controler")).getSelected();
                final int next = army.getComandantePericia() == 80 ? 81 : 80;
                type((JSpinner) field(window, "morale"), "77");
                ((JSpinner) field(window, "commander")).setValue(next);
                assertEquals(77, army.getMoral());
                assertEquals(next, army.getComandantePericia());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                if (window != null) window.dispose();
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidPendingTextBlocksSpinnerAndMenuCommandWithoutLosingTheText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            try {
                window = open();
                final BattleSimControler controler = (BattleSimControler) field(window, "controler");
                final ArmySim army = controler.getSelected();
                final int original = army.getComandantePericia();
                final JSpinner morale = (JSpinner) field(window, "morale");
                type(morale, "invalid");
                ((JSpinner) field(window, "commander")).setValue(original == 80 ? 81 : 80);
                assertEquals(original, army.getComandantePericia());
                final int count = controler.getScenario().getArmies().size();
                final Map<String, Action> commands = (Map<String, Action>) field(window, "commands");
                commands.get("cloneArmy").actionPerformed(new ActionEvent(new JButton(), 0, "cloneArmy"));
                assertEquals(count, controler.getScenario().getArmies().size());
                assertEquals("invalid", ((JSpinner.DefaultEditor) morale.getEditor()).getTextField().getText());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                if (window != null) window.dispose();
            }
        });
    }

    @Test
    void queuedTableRefreshDoesNotStopTheNextCellEditor() throws Exception {
        final BattleSimWindow[] window = {null};
        final JTable[] table = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    window[0] = open();
                    table[0] = (JTable) field(window[0], "platoons");
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            // Drain initial resize/layout events before simulating a user's first keystroke.
            SwingUtilities.invokeAndWait(() -> {
                try {
                    assertTrue(table[0].editCellAt(0, 2));
                    ((JTextField) table[0].getEditorComponent()).setText("123");
                    assertTrue(table[0].getCellEditor().stopCellEditing());
                    assertTrue(table[0].editCellAt(0, 3));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(table[0].isEditing());
                assertEquals(3, table[0].getEditingColumn());
                ((JTextField) table[0].getEditorComponent()).setText("45");
                assertTrue(table[0].getCellEditor().stopCellEditing());
                assertTrue(table[0].editCellAt(0, 4));
            });
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(table[0].isEditing());
                assertEquals(4, table[0].getEditingColumn());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (table[0] != null && table[0].isEditing()) table[0].getCellEditor().cancelCellEditing();
                if (window[0] != null) window[0].dispose();
            });
        }
    }
}
