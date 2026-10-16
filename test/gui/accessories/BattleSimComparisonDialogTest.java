package gui.accessories;

import business.combat.CombatResult;
import business.combat.CombatScenario;
import control.services.BattleSimRunSnapshot;
import control.services.BattleSimVariants;
import java.awt.GraphicsEnvironment;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import persistenceCommons.SettingsManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BattleSimComparisonDialogTest {
    private static Object field(Object object, String name) throws Exception {
        final var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true); return field.get(object);
    }
    private static BattleSimRunSnapshot pin(BattleSimVariants variant) {
        final var pin = BattleSimRunSnapshot.capture(variant, new CombatResult(), "report", SettingsManager.getInstance().getBundleManager());
        variant.family().pin(pin, -1); return pin;
    }

    @Test void siblingUpdatesKeepBaselineIdentityAndClosedOriginDoesNotLosePins() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            final var original = new BattleSimVariants(new CombatScenario(), "Duplicate");
            final var sibling = original.fork("Duplicate");
            BattleSimComparisonDialog dialog = null;
            try {
                pin(original); final var baselinePin = pin(sibling); pin(original);
                dialog = new BattleSimComparisonDialog(null, sibling.family());
                final JComboBox<?> baseline = (JComboBox<?>) field(dialog, "baseline");
                baseline.setSelectedIndex(1);
                sibling.family().remove(0);
                assertEquals(0, baseline.getSelectedIndex());
                assertTrue(baseline.getSelectedItem().toString().contains(baselinePin.time()));
                pin(sibling);
                assertEquals(3, baseline.getItemCount());
                original.close();
                assertEquals(3, sibling.family().pins().size());
                sibling.family().remove(0);
                assertFalse(((JLabel) field(dialog, "baselineNotice")).getText().isBlank());
                dialog.dispose();
                sibling.close();
                assertTrue(sibling.family().pins().isEmpty());
            } catch (Exception ex) { throw new RuntimeException(ex); }
            finally {
                if (dialog != null) dialog.dispose();
                original.close(); sibling.close();
            }
        });
    }
}
