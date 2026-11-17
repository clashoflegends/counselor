package e2e;

import business.MovimentoExercito;
import business.facade.LocalFacade;
import control.MapaControler;
import gui.subtabs.SubTabDirecaoExercito;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import model.Local;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementBackspaceTest {

    @BeforeAll
    static void loadWorld() throws Exception {
        TestWorld.load();
    }

    @Test
    void replacesOnlyTheLastStepForArmiesAndFleets() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (boolean water : new boolean[]{false, true}) {
                RecordingMap map = new RecordingMap();
                SubTabDirecaoExercito panel = panel(map, water);
                for (String direction : new String[]{"e", "h", "h", "h", "h", "h", "h", "w"}) {
                    panel.doMovementTagAdd(direction);
                }
                assertEquals("e;h;h;h;h;h;h;w;nr", panel.getDirecoesIdTipo());

                Object action = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .get(KeyStroke.getKeyStroke("BACK_SPACE"));
                assertNotNull(action);
                panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, ""));
                assertEquals("e;h;h;h;h;h;h;nr", panel.getDirecoesIdTipo());
                assertEquals(7, map.steps.size());
                assertEquals(map.steps.get(0).getDestino(), map.steps.get(6).getDestino());

                panel.doMovementTagAdd("sw");
                assertEquals("e;h;h;h;h;h;h;sw;nr", panel.getDirecoesIdTipo());
                RecordingMap freshMap = new RecordingMap();
                SubTabDirecaoExercito fresh = panel(freshMap, water);
                for (String direction : new String[]{"e", "h", "h", "h", "h", "h", "h", "sw"}) {
                    fresh.doMovementTagAdd(direction);
                }
                assertEquals(fresh.getDirecaoDisplay(), panel.getDirecaoDisplay());
                assertEquals(freshMap.snapshot(), map.snapshot(), "destination, cost and prior direction");
            }
        });
    }

    @Test
    void buttonDeletesWholeDirectionsAndHoldsThenStopsAtEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecordingMap map = new RecordingMap();
            SubTabDirecaoExercito panel = panel(map, false);
            JButton backspace = button(panel, "jbBackspace");
            assertNotNull(backspace);
            panel.setDirecaoTipo("ev");
            panel.doMovementTagAdd("sw");
            panel.doMovementTagAdd("h");
            backspace.doClick();
            assertEquals("sw;ev", panel.getDirecoesIdTipo());
            assertEquals(1, map.steps.size());
            backspace.doClick();
            assertEquals("ev", panel.getDirecoesIdTipo());
            assertTrue(map.steps.isEmpty());
            backspace.doClick();
            assertEquals("ev", panel.getDirecoesIdTipo());
            panel.doMovementTagAdd("e");
            assertEquals(origin(), map.steps.get(0).getOrigem());
            panel.doApaga();
            assertEquals("ev", panel.getDirecoesIdTipo());
            assertTrue(map.steps.isEmpty(), "clear all remains available");
        });
    }

    private static SubTabDirecaoExercito panel(RecordingMap map, boolean water) {
        return new SubTabDirecaoExercito(origin(), 14, water, map);
    }

    @Test
    void backspaceStaysBesideEraseWhenThePanelWidens() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SubTabDirecaoExercito panel = panel(new RecordingMap(), false);
            JButton erase = button(panel, "Erase");
            JButton backspace = button(panel, "jbBackspace");
            assertNotNull(erase);
            assertNotNull(backspace);
            assertSame(erase.getParent(), backspace.getParent());
            for (int width : new int[]{180, 500}) {
                panel.setSize(width, 380);
                panel.doLayout();
                assertTrue(backspace.getWidth() > 0);
                assertEquals(erase.getY() + erase.getBaseline(erase.getWidth(), erase.getHeight()),
                        backspace.getY() + backspace.getBaseline(backspace.getWidth(), backspace.getHeight()));
                int gap = backspace.getX() - (erase.getX() + erase.getWidth());
                assertTrue(gap >= 0 && gap <= 12, "erase/backspace gap at width " + width);
            }
        });
    }

    private static Local origin() {
        LocalFacade facade = new LocalFacade();
        var locals = control.facade.WorldFacadeCounselor.getInstance().getLocais();
        return locals.values().stream().filter(local -> {
            Local east = locals.get(facade.getIdentificacaoVizinho(local, "e"));
            return east != null
                    && locals.containsKey(facade.getIdentificacaoVizinho(east, "sw"))
                    && locals.containsKey(facade.getIdentificacaoVizinho(local, "sw"));
        }).findFirst().orElseThrow();
    }

    private static JButton button(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton b && name.equals(b.getName())) {
                return b;
            }
            if (child instanceof Container c) {
                JButton found = button(c, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static class RecordingMap extends MapaControler {
        final List<MovimentoExercito> steps = new ArrayList<>();

        RecordingMap() {
            super(new JPanel());
        }

        @Override
        public void addMovementTag(MovimentoExercito step) {
            steps.add(step);
        }

        @Override
        public void remMovementTag() {
            steps.clear();
        }

        List<String> snapshot() {
            return steps.stream().map(step -> step.getDestino().getCoordenadas() + ":"
                    + step.getCustoMovimento() + ":" + step.getDirecaoAnterior()).toList();
        }
    }
}
