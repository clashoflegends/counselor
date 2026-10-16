package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.BattleSimControler;
import gui.accessories.BattleSimReadinessPanel;
import gui.accessories.BattleSimWindow;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BattleSimReadinessWindowTest {
    @BeforeAll static void requiresDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Readiness window interactions require a display");
    }

    private static Object field(BattleSimWindow window, String name) throws Exception {
        final Field field = BattleSimWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(window);
    }

    private static void invoke(BattleSimWindow window, String name, Class<?>[] types, Object... args) throws Exception {
        final Method method = BattleSimWindow.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(window, args);
    }

    private static BattleSimWindow open(boolean placeholders) throws Exception {
        final var world = TestWorld.load();
        final Local origin = world.getLocais().values().stream().filter(one -> !one.getExercitos().isEmpty())
                .findFirst().orElseThrow();
        final CombatScenario scenario = new BattleSimControler(origin).getScenario();
        final ArmySim template = scenario.getArmies().get(0);
        final Nacao nation = template.getNacao();
        for (ArmySim army : java.util.List.copyOf(scenario.getArmies())) scenario.remArmy(army);
        scenario.setCityParticipates(false);
        final ArmySim first = new ArmySim(template);
        if (placeholders) {
            first.getPelotoes().clear();
            addPlaceholder(first, "none", 0, scenario);
            addPlaceholder(first, "ship", 50, scenario);
        } else {
            first.getPelotoes().values().forEach(platoon -> platoon.setQtd(0));
        }
        scenario.addArmy(first, CombatScenario.Provenance.EXACT);
        final Nacao other = world.getNacoes().values().stream().filter(one -> one != nation).findFirst().orElseThrow();
        final ArmySim second = new ArmySim(template);
        second.setCodigo("readiness-enemy");
        second.setNacao(other);
        second.getPelotoes().values().forEach(platoon -> platoon.setQtd(0));
        scenario.addArmy(second, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(nation, other, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(other, nation, RelationshipMatrix.SWORN_ENEMY);
        final Constructor<BattleSimWindow> ctor = BattleSimWindow.class.getDeclaredConstructor(BattleSimControler.class, String.class);
        ctor.setAccessible(true);
        final BattleSimWindow window = ctor.newInstance(new BattleSimControler(scenario), "Readiness test");
        window.setLocation(-20000, -20000);
        window.validate();
        return window;
    }

    private static void addPlaceholder(ArmySim army, String code, int quantity, CombatScenario scenario) {
        final TipoTropa type = new TipoTropa();
        type.setCodigo(code);
        type.setNome(code);
        final java.util.SortedMap<model.Terreno, Integer> stats = new java.util.TreeMap<>();
        stats.put(scenario.getTerreno(), 1);
        type.setAtaqueTerreno(stats);
        type.setDefesaTerreno(stats);
        type.setMovimentoTerreno(stats);
        final Pelotao platoon = new Pelotao();
        platoon.setTipoTropa(type);
        platoon.setQtd(quantity);
        army.getPelotoes().put(platoon.getCodigo(), platoon);
    }

    private static void show(BattleSimWindow window, BattleSimReadinessPanel.Topic topic) throws Exception {
        invoke(window, "showReadiness", new Class<?>[]{BattleSimReadinessPanel.Topic.class}, topic);
        final JDialog dialog = (JDialog) field(window, "readinessDialog");
        if (dialog != null) dialog.setLocation(-20000, -20000);
    }

    @Test void openGateExplanationUpdatesWhenTheBattleBecomesRunnable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            try {
                window = open(false);
                show(window, BattleSimReadinessPanel.Topic.GATE);
                final JTextArea explanation = (JTextArea) field(window, "readinessExplanation");
                final String blocked = explanation.getText();
                final BattleSimControler controller = (BattleSimControler) field(window, "controler");
                for (ArmySim army : controller.getScenario().getArmies()) army.getPelotoes().values().iterator().next().setQtd(100);
                invoke(window, "doEdited", new Class<?>[]{boolean.class}, false);
                assertTrue(controller.getScenario().getRunGate().isRunnable());
                assertNotEquals(blocked, explanation.getText());
                assertFalse(explanation.getText().isBlank());
            } catch (Exception ex) { throw new RuntimeException(ex); }
            finally { if (window != null) window.dispose(); }
        });
    }

    @Test void emptyKnownRowsNavigateToQuantity() throws Exception { checkTroopNavigation(false); }
    @Test void zeroPlaceholderIsSkippedForThePositiveUnidentifiedType() throws Exception { checkTroopNavigation(true); }
    @Test void blockedReadinessTargetsTheEmptyArmyQuantity() throws Exception {
        checkTroopNavigation(false, BattleSimReadinessPanel.Topic.GATE);
    }

    private void checkTroopNavigation(boolean placeholders) throws Exception {
        checkTroopNavigation(placeholders, BattleSimReadinessPanel.Topic.TROOPS);
    }

    private void checkTroopNavigation(boolean placeholders, BattleSimReadinessPanel.Topic topic) throws Exception {
        final BattleSimWindow[] window = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    window[0] = open(placeholders);
                    show(window[0], topic);
                    invoke(window[0], "chooseReadinessArmy", new Class<?>[]{});
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    final JTable table = (JTable) field(window[0], "platoons");
                    assertTrue(table.isEditing());
                    assertEquals(placeholders ? 1 : 2, table.getEditingColumn());
                    if (placeholders) {
                        final var model = (BattleSimControler.PlatoonTableModel) table.getModel();
                        assertTrue(model.getPlatoon(table.getEditingRow()).getQtd() > 0);
                    }
                    table.getCellEditor().cancelCellEditing();
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
        } finally { SwingUtilities.invokeAndWait(() -> { if (window[0] != null) window[0].dispose(); }); }
    }
}
