package e2e;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.BattleSimControler;
import control.services.BattleSimVariants;
import gui.accessories.BattleSimWindow;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import javax.swing.Action;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BattleSimVariantsWindowTest {
    @BeforeAll static void display() { assumeFalse(GraphicsEnvironment.isHeadless()); }
    private static Object field(Object object, String name) throws Exception {
        final Field field = BattleSimWindow.class.getDeclaredField(name);
        field.setAccessible(true); return field.get(object);
    }
    static BattleSimWindow open() throws Exception {
        final var world = TestWorld.load();
        final var local = world.getLocais().values().stream().filter(one -> !one.getExercitos().isEmpty()).findFirst().orElseThrow();
        final var controller = new BattleSimControler(local);
        final CombatScenario scenario = controller.getScenario();
        scenario.setCityParticipates(false);
        final ArmySim template = scenario.getArmies().get(0);
        for (ArmySim army : java.util.List.copyOf(scenario.getArmies())) scenario.remArmy(army);
        final ArmySim first = new ArmySim(template), second = new ArmySim(template);
        first.setMoral(100); second.setMoral(100);
        first.getPelotoes().values().forEach(platoon -> platoon.setQtd(100));
        second.getPelotoes().values().forEach(platoon -> platoon.setQtd(100));
        second.setCodigo("variant-enemy");
        second.setNacao(world.getNacoes().values().stream().filter(one -> one != first.getNacao()).findFirst().orElseThrow());
        scenario.addArmy(first, CombatScenario.Provenance.EXACT);
        scenario.addArmy(second, CombatScenario.Provenance.EXACT);
        controller.setSelected(first);
        scenario.setRelacionamento(first.getNacao(), second.getNacao(), RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(second.getNacao(), first.getNacao(), RelationshipMatrix.SWORN_ENEMY);
        final var ctor = BattleSimWindow.class.getDeclaredConstructor(BattleSimControler.class, String.class);
        ctor.setAccessible(true);
        final BattleSimWindow window = ctor.newInstance(controller, "Variant test");
        window.setLocation(-20000, -20000); window.validate();
        return window;
    }
    @SuppressWarnings("unchecked") private static Action command(BattleSimWindow window, String name) throws Exception {
        return ((java.util.Map<String, Action>) field(window, "commands")).get(name);
    }

    @Test void pendingEditDiscardsTheCompletedRunBeforeItCanBePinned() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            try {
                window = open();
                final javax.swing.JButton run = (javax.swing.JButton) field(window, "run");
                assertTrue(run.isEnabled(), "An initially ready battle must have a clickable Run button");
                run.doClick();
                assertNotNull(field(window, "completedSnapshot"));
                assertTrue(command(window, "pinRun").isEnabled());
                final var spinner = (JSpinner) field(window, "morale");
                ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText("77");
                command(window, "pinRun").actionPerformed(new ActionEvent(window, 0, "pinRun"));
                assertNull(field(window, "completedSnapshot"));
                assertFalse(command(window, "pinRun").isEnabled());
                assertTrue(((BattleSimVariants) field(window, "variant")).family().pins().isEmpty());
            } catch (Exception ex) { throw new RuntimeException(ex); }
            finally { if (window != null) window.dispose(); }
        });
    }

    @Test void staleTurnRefusesRunAndKeepsFrozenTurnIdentity() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleSimWindow window = null;
            model.Partida game = null;
            int turn = 0;
            try {
                window = open();
                game = ((BattleSimControler) field(window, "controler")).getScenario().getPartida();
                turn = game.getTurno(); game.setTurno(turn + 1);
                command(window, "run").actionPerformed(new ActionEvent(window, 0, "run"));
                assertNull(((BattleSimControler) field(window, "controler")).getLastResult());
                assertEquals(turn, ((BattleSimVariants) field(window, "variant")).turn());
            } catch (Exception ex) { throw new RuntimeException(ex); }
            finally {
                if (game != null) game.setTurno(turn);
                if (window != null) window.dispose();
            }
        });
    }

    @Test void queuedTroopRefreshDoesNotRunAfterDisposal() throws Exception {
        final java.util.List<Throwable> uncaught = new java.util.concurrent.CopyOnWriteArrayList<>();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> uncaught.add(failure));
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    final BattleSimWindow window = open();
                    final javax.swing.JTable table = (javax.swing.JTable) field(window, "platoons");
                    table.getModel().setValueAt(123, 0, 2);
                    window.dispose();
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            SwingUtilities.invokeAndWait(() -> { });
            assertTrue(uncaught.isEmpty(), uncaught.toString());
        } finally { Thread.setDefaultUncaughtExceptionHandler(previous); }
    }
}
