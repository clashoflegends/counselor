package gui.services;

import business.combat.ArmySim;
import control.BattleSimControler;
import e2e.TestWorld;
import gui.MainResultWindowGui;
import gui.accessories.BattleSimWindow;
import gui.accessories.TacticGridDialog;
import gui.accessories.WhatIfDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import model.Local;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Actual product windows, exercised on a disposable display/configuration for QA. */
class ModelessFlowTest {
    @BeforeAll static void display() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        assumeTrue(Boolean.getBoolean("clash.qa.isolated"), "Run only with a disposable QA configuration");
        TestWorld.load();
    }

    private static Object field(Object instance, String name) throws Exception {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void invoke(Object instance, String name) throws Exception {
        Method method = instance.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(instance);
    }

    private static BattleSimWindow battle() throws Exception {
        Local local = TestWorld.load().getLocais().values().stream()
                .filter(one -> !one.getExercitos().isEmpty()).findFirst().orElseThrow();
        return new BattleSimWindow(local);
    }

    @Test void anOlderSettingsFieldCannotOverwriteANewerTokenFromAnotherWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var settings = persistenceCommons.SettingsManager.getInstance();
            String original = settings.getConfig("playerToken", "");
            try {
                settings.setConfig("playerToken", "qa-old");
                var form = new gui.accessories.MainSettingsGui(null);
                JTextField token = (JTextField) field(form, "playerTokenTextField");
                var gained = new java.awt.event.FocusEvent(token, java.awt.event.FocusEvent.FOCUS_GAINED);
                for (var listener : token.getFocusListeners()) listener.focusGained(gained);
                settings.setConfig("playerToken", "qa-new");
                var lost = new java.awt.event.FocusEvent(token, java.awt.event.FocusEvent.FOCUS_LOST);
                for (var listener : token.getFocusListeners()) listener.focusLost(lost);
                assertEquals("qa-new", settings.getConfig("playerToken", ""));
                assertEquals("qa-new", token.getText());
            } catch (Exception ex) { throw new AssertionError(ex); }
            finally { settings.setConfigAndSaveToFile("playerToken", original); }
        });
    }

    @Test void settingsAreOwnedModelessAndRepeatedOpenReusesTheSameWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame("Settings QA");
            try {
                MainResultWindowGui main = new MainResultWindowGui(null);
                owner.add(main);
                owner.setSize(1024, 800);
                owner.setVisible(true);
                Object controller = field(main, "wc");
                invoke(controller, "doConfig");
                JDialog first = (JDialog) field(controller, "settingsDialog");
                assertFalse(first.isModal());
                assertSame(owner, first.getOwner());
                assertTrue(owner.isEnabled());
                invoke(controller, "doConfig");
                assertSame(first, field(controller, "settingsDialog"));
                first.dispose();
            } catch (Exception ex) { throw new AssertionError(ex); }
            finally { Toast.dismissCurrent(); owner.dispose(); }
        });
    }

    @Test void settingsCancelRestoresItsDraftEvenAfterAnUnrelatedSaveWithoutRevertingMainWindowChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var settings = persistenceCommons.SettingsManager.getInstance();
            String prior = settings.getConfig("AutoSaveActions");
            String external = settings.getConfig("qa.main.preference");
            try {
                settings.setConfig("AutoSaveActions", "0");
                var form = new gui.accessories.MainSettingsGui(null);
                control.SettingsControler controller = (control.SettingsControler) field(form, "settingsControler");
                var checkbox = new javax.swing.JCheckBox();
                checkbox.setSelected(true);
                controller.actionPerformed(new java.awt.event.ActionEvent(checkbox, 0, "autoSaveOrders"));
                assertEquals("1", settings.getConfig("AutoSaveActions"));
                settings.setConfigAndSaveToFile("qa.main.preference", "external");
                form.cancelEdits();
                settings.doConfigRestore("qa");
                assertEquals("0", settings.getConfig("AutoSaveActions"));
                assertEquals("external", settings.getConfig("qa.main.preference"));
            } catch (Exception ex) { throw new AssertionError(ex); }
            finally {
                if (prior == null) settings.removeConfig("AutoSaveActions"); else settings.setConfig("AutoSaveActions", prior);
                if (external == null) settings.removeConfig("qa.main.preference"); else settings.setConfig("qa.main.preference", external);
                settings.saveToFile();
            }
        });
    }

    @Test void analysisToolsAreModelessAndUseIndependentScenarioSnapshots() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow parent = null;
            WhatIfDialog whatIf = null;
            TacticGridDialog tactics = null;
            try {
                parent = battle();
                BattleSimControler original = (BattleSimControler) field(parent, "controler");
                ArmySim army = original.getSelected();
                var platoon = army.getPelotoes().values().iterator().next();
                int quantity = platoon.getQtd();
                whatIf = new WhatIfDialog(parent, original, army, platoon);
                tactics = new TacticGridDialog(parent, original);
                assertFalse(whatIf.isModal());
                assertFalse(tactics.isModal());
                BattleSimControler whatIfCopy = (BattleSimControler) field(whatIf, "controler");
                BattleSimControler tacticsCopy = (BattleSimControler) field(tactics, "controler");
                assertNotSame(original.getScenario(), whatIfCopy.getScenario());
                assertNotSame(original.getScenario(), tacticsCopy.getScenario());
                int armyIndex = original.getScenario().getArmies().indexOf(army);
                platoon.setQtd(quantity + 1);
                assertEquals(quantity, whatIfCopy.getScenario().getArmies().get(armyIndex).getPelotoes().get(platoon.getCodigo()).getQtd());
                assertEquals(quantity, tacticsCopy.getScenario().getArmies().get(armyIndex).getPelotoes().get(platoon.getCodigo()).getQtd());
            } catch (Exception ex) { throw new AssertionError(ex); }
            finally {
                if (whatIf != null) whatIf.dispose();
                if (tactics != null) tactics.dispose();
                if (parent != null) parent.dispose();
            }
        });
    }

    @Test void variantRenameAndCloneNeedNoInputDialog() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow parent = null;
            Frame[] before = Frame.getFrames();
            try {
                parent = battle();
                JTextField name = (JTextField) field(parent, "variantName");
                name.setText("QA variant");
                name.postActionEvent();
                assertEquals("QA variant", parent.getTitle());
                name.setText("cancelled draft");
                name.getActionMap().get("cancelName").actionPerformed(null);
                assertEquals("QA variant", name.getText());
                invoke(parent, "doCloneWindow");
                boolean cloned = false;
                for (Frame frame : Frame.getFrames()) {
                    if (frame != parent && frame instanceof BattleSimWindow && !java.util.Arrays.asList(before).contains(frame)) {
                        assertEquals("QA variant 2", frame.getTitle());
                        assertTrue(frame.isShowing());
                        cloned = true;
                    }
                }
                assertTrue(cloned);
            } catch (Exception ex) { throw new AssertionError(ex); }
            finally {
                for (Frame frame : Frame.getFrames()) {
                    if (!java.util.Arrays.asList(before).contains(frame)) frame.dispose();
                }
            }
        });
    }
}
