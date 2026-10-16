package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import model.Nacao;
import org.junit.jupiter.api.Test;
import persistenceCommons.BundleManager;
import static org.junit.jupiter.api.Assertions.*;

class BattleSimReadinessPanelTest {
    private static JButton badge(BattleSimReadinessPanel panel, String topic) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton
                    && topic.equals(((JButton) component).getClientProperty("BattleSim.readiness"))) {
                return (JButton) component;
            }
        }
        throw new AssertionError("Missing badge " + topic);
    }

    @Test
    void blockedGateRemainsKeyboardFocusableAndActivatesWithoutEditingAnything() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final AtomicReference<BattleSimReadinessPanel.Topic> chosen = new AtomicReference<>();
            final BattleSimReadinessPanel panel = new BattleSimReadinessPanel(new BundleManager(), chosen::set);
            final CombatScenario scenario = new CombatScenario();
            panel.refresh(scenario);
            final JButton gate = badge(panel, "GATE");
            assertTrue(gate.isEnabled());
            assertTrue(gate.isFocusable());
            assertFalse(gate.getAccessibleContext().getAccessibleName().isBlank());
            gate.doClick();
            assertEquals(BattleSimReadinessPanel.Topic.GATE, chosen.get());
            assertTrue(scenario.getArmies().isEmpty());
        });
    }

    @Test
    void warningsUseSeparateCountsAndDisappearWhenTheIssueIsResolved() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final BattleSimReadinessPanel panel = new BattleSimReadinessPanel(new BundleManager(), topic -> {});
            final CombatScenario scenario = new CombatScenario();
            final Nacao nation = new Nacao();
            nation.setCodigo("a");
            final ArmySim army = new ArmySim("army", null, nation);
            army.setMoral(0);
            scenario.addArmy(army, CombatScenario.Provenance.ESTIMATED);
            panel.refresh(scenario);
            assertTrue(badge(panel, "MORALE").isVisible());
            assertTrue(badge(panel, "TROOPS").isVisible());
            assertEquals("? 1", badge(panel, "MORALE").getText());
            army.setMoral(50);
            panel.refresh(scenario);
            assertFalse(badge(panel, "MORALE").isVisible());
            assertTrue(badge(panel, "TROOPS").isVisible());
        });
    }

    @Test
    void missingSvgHasAPaintableNonEmojiFallback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final javax.swing.Icon icon = BattleSimReadinessPanel.icon("missing-readiness-asset", "S");
            final BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
            final java.awt.Graphics graphics = image.getGraphics();
            icon.paintIcon(new JButton(), graphics, 0, 0);
            graphics.dispose();
            assertEquals(16, icon.getIconWidth());
            boolean painted = false;
            for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) painted |= image.getRGB(x, y) != 0;
            assertTrue(painted);
        });
    }
}
