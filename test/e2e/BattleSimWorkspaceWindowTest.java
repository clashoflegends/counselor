package e2e;

import business.combat.ArmySim;
import control.BattleSimControler;
import gui.accessories.BattleSimWindow;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BattleSimWorkspaceWindowTest {
    @BeforeAll static void display() { assumeFalse(GraphicsEnvironment.isHeadless()); }
    private static Object field(Object object, String name) throws Exception {
        final var field = BattleSimWindow.class.getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    @SuppressWarnings("unchecked") private static Action command(BattleSimWindow window, String name) throws Exception {
        return ((java.util.Map<String, Action>) field(window, "commands")).get(name);
    }
    private static void layout(Container container) {
        container.doLayout(); for (Component child : container.getComponents()) if (child instanceof Container) layout((Container) child);
    }

    @Test void invalidSelectionRollsBackAndValidEditsFollowTheOldArmy() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            try {
                window = BattleSimVariantsWindowTest.open();
                final BattleSimControler controller = (BattleSimControler) field(window, "controler");
                final ArmySim original = controller.getSelected();
                command(window, "cloneArmy").actionPerformed(new ActionEvent(window, 0, "cloneArmy"));
                final ArmySim clone = controller.getSelected();
                final JTable roster = (JTable) field(window, "roster");
                final var morale = ((JSpinner.DefaultEditor) ((JSpinner) field(window, "morale")).getEditor()).getTextField();
                final int selected = roster.getSelectedRow();
                morale.setText("invalid");
                roster.changeSelection(1, 0, false, false);
                assertSame(clone, controller.getSelected()); assertEquals(selected, roster.getSelectedRow());
                assertEquals("invalid", morale.getText());
                morale.setText("77"); roster.changeSelection(1, 0, false, false);
                assertSame(original, controller.getSelected()); assertEquals(77, clone.getMoral());
                assertEquals(1, roster.getSelectedRow());
            } catch (Exception ex) { throw new RuntimeException(ex); }
            finally { if (window != null) window.dispose(); }
        });
    }

    @Test void resizingAfterLongButtonsWrapRetainsATroopRowAndKeepsCommandsAboveIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            try {
                window = BattleSimVariantsWindowTest.open();
                final JTable table = (JTable) field(window, "platoons");
                final javax.swing.JPanel buttons = (javax.swing.JPanel) field(window, "platoonCommands");
                for (Component component : buttons.getComponents()) if (component instanceof JButton button) button.setText(button.getText() + " extended translated label");
                window.setSize(1400, 900); layout(window);
                window.setSize(window.getMinimumSize()); layout(window); layout(window);
                assertTrue(table.getParent().getHeight() >= table.getRowHeight());
                final var buttonPoint = SwingUtilities.convertPoint(buttons, 0, 0, window);
                final var tablePoint = SwingUtilities.convertPoint(table, 0, 0, window);
                assertTrue(buttonPoint.y < tablePoint.y);
                final JTable roster = (JTable) field(window, "roster");
                assertTrue(roster.getScrollableTracksViewportWidth(), "Default small roster fits its viewport");
                assertTrue(roster.getParent().getHeight() >= roster.getRowHeight() * 2,
                        "One nation header and its army stay visible beside Ground");
                for (String name : new String[]{"terreno", "cityOwner", "citySize", "cityFortification", "cityLoyalty", "battlefieldPick"}) {
                    final Component control = (Component) field(window, name);
                    final javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, control);
                    final var bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), viewport);
                    assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= viewport.getWidth(), name + " extends beyond Ground viewport");
                }
            } catch (Exception ex) { throw new RuntimeException(ex); }
            finally { if (window != null) window.dispose(); }
        });
    }

    private static JMenuItem runMenu(Container container, Action action) {
        for (Component component : container.getComponents()) {
            if (component instanceof JMenu menu) {
                for (int i = 0; i < menu.getItemCount(); i++) {
                    final JMenuItem item = menu.getItem(i);
                    if (item != null && item.getAction() == action) return item;
                }
            }
            if (component instanceof Container child) { final JMenuItem found = runMenu(child, action); if (found != null) return found; }
        }
        return null;
    }
    private static void focus(Component component) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final java.awt.Window window = SwingUtilities.getWindowAncestor(component); window.toFront(); component.requestFocusInWindow();
        });
        for (int i = 0; i < 50 && KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != component; i++) Thread.sleep(20);
        assertSame(component, KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
    }
    private static void controlEnter(Component target) throws Exception {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new KeyEvent(target, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, '\n'));
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new KeyEvent(target, KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(), KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, '\n'));
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test void realMenuAcceleratorRespectsTextCellAndChildWindowFocus() throws Exception {
        final BattleSimWindow[] window = {null};
        final JTable[] roster = {null};
        final JTextField[] text = {null};
        final BattleSimControler[] controller = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    window[0] = BattleSimVariantsWindowTest.open(); window[0].setLocation(40, 40); window[0].setVisible(true);
                    roster[0] = (JTable) field(window[0], "roster");
                    text[0] = ((JSpinner.DefaultEditor) ((JSpinner) field(window[0], "morale")).getEditor()).getTextField();
                    controller[0] = (BattleSimControler) field(window[0], "controler");
                    // Remove the root binding to exercise JMenuItem's real accelerator registration.
                    window[0].getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .remove(KeyStroke.getKeyStroke("control ENTER"));
                    assertNotNull(runMenu(window[0], command(window[0], "run")));
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            focus(text[0]); controlEnter(text[0]); assertNull(controller[0].getLastResult());
            focus(roster[0]); controlEnter(roster[0]); assertNotNull(controller[0].getLastResult(), "The menu accelerator itself must run");
            final var first = controller[0].getLastResult();
            focus(text[0]);
            SwingUtilities.invokeAndWait(() -> {
                try { text[0].setText("77"); runMenu(window[0], command(window[0], "run")).doClick(); }
                catch (Exception ex) { throw new RuntimeException(ex); }
            });
            assertNotSame(first, controller[0].getLastResult()); assertEquals(77, controller[0].getSelected().getMoral());
            final var second = controller[0].getLastResult();
            final Component[] cell = {null};
            SwingUtilities.invokeAndWait(() -> {
                try {
                    final JTable table = (JTable) field(window[0], "platoons");
                    ((javax.swing.JTabbedPane) field(window[0], "bottomTabs")).setSelectedIndex(0);
                    assertTrue(table.editCellAt(0, 2)); cell[0] = table.getEditorComponent();
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            focus(cell[0]); controlEnter(cell[0]); assertSame(second, controller[0].getLastResult());
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ((JTable) field(window[0], "platoons")).getCellEditor().cancelCellEditing();
                    command(window[0], "compareRuns").actionPerformed(new ActionEvent(window[0], 0, "compareRuns"));
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            final JDialog child = (JDialog) field(window[0], "comparison");
            focus(child.getRootPane());
            SwingUtilities.invokeAndWait(() -> {
                try { command(window[0], "run").actionPerformed(new ActionEvent(child, 0, "run", ActionEvent.CTRL_MASK)); }
                catch (Exception ex) { throw new RuntimeException(ex); }
            });
            assertSame(second, controller[0].getLastResult());
            final Component[] loyalty = {null};
            SwingUtilities.invokeAndWait(() -> {
                try {
                    child.dispose();
                    ((javax.swing.JCheckBox) field(window[0], "cityParticipates")).doClick();
                    window[0].setMinimumSize(new java.awt.Dimension(1, 1)); window[0].setSize(960, 500);
                    layout(window[0]); layout(window[0]);
                    loyalty[0] = ((JSpinner.DefaultEditor) ((JSpinner) field(window[0], "cityLoyalty")).getEditor()).getTextField();
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            focus(loyalty[0]);
            SwingUtilities.invokeAndWait(() -> {
                final var viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, loyalty[0]);
                final var bounds = SwingUtilities.convertRectangle(loyalty[0].getParent(), loyalty[0].getBounds(), viewport.getView());
                assertTrue(viewport.getViewRect().contains(bounds), "Keyboard focus reveals a lower city control");
            });
        } finally { SwingUtilities.invokeAndWait(() -> { if (window[0] != null) window[0].dispose(); }); }
    }
}
