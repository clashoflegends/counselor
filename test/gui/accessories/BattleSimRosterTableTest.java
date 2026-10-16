package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import control.BattleSimControler;
import e2e.TestWorld;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import model.Habilidade;
import model.Nacao;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import persistenceCommons.SettingsManager;
import static org.junit.jupiter.api.Assertions.*;

class BattleSimRosterTableTest {
    private static final String CONFIG = "battleSimRosterLayout";
    private static BattleSimControler controller() throws Exception {
        final var world = TestWorld.load();
        final var local = world.getLocais().values().stream().filter(one -> !one.getExercitos().isEmpty()).findFirst().orElseThrow();
        return new BattleSimControler(local);
    }
    private static BattleSimRosterTable table(BattleSimControler controller) {
        return new BattleSimRosterTable(controller, army -> { controller.setSelected(army); return true; }, () -> { });
    }

    @Test void realTableNavigationSkipsNationHeadersAndKeepsArmyIdentity() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                final var controller = controller();
                final var scenario = controller.getScenario();
                final ArmySim template = scenario.getArmies().get(0);
                for (int i = 0; i < 15; i++) {
                    final ArmySim army = new ArmySim(template); army.setCodigo("row-" + i);
                    if (i > 7) {
                        final Nacao nation = new Nacao(); nation.setCodigo("group-" + i); nation.setNome("Group " + i);
                        army.setNacao(nation);
                    }
                    scenario.addArmy(army, CombatScenario.Provenance.EXACT);
                }
                final var table = table(controller); table.refresh();
                final JViewport viewport = new JViewport(); viewport.setView(table); viewport.setSize(300, 100);
                table.setSize(new Dimension(300, table.getPreferredSize().height)); viewport.doLayout();
                for (String action : new String[]{"selectLastRow", "selectFirstRow", "selectLastRow", "scrollUpChangeSelection", "scrollDownChangeSelection"}) {
                    table.getActionMap().get(action).actionPerformed(new ActionEvent(table, 0, action));
                    assertTrue(table.getSelectedRow() >= 0);
                    assertFalse(String.valueOf(table.getValueAt(table.getSelectedRow(), 0)).contains(" vs "), "A header was selected");
                }
                table.getActionMap().get("selectFirstRow").actionPerformed(new ActionEvent(table, 0, "home"));
                assertEquals(1, table.getSelectedRow(), "Home reaches first army after header");
                final ArmySim selected = controller.getSelected();
                selected.setNacao(TestWorld.load().getNacoes().values().stream().filter(one -> one != selected.getNacao()).findFirst().orElseThrow());
                table.refresh();
                assertSame(selected, controller.getSelected());
                assertTrue(table.getSelectedRow() >= 0);
                assertTrue(table.getModel().getValueAt(0, 0).toString().length() > 0, "Group text is available to accessibility");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }

    @Test void unidentifiedLandKeepsQuantitiesAndKnownNavalStatistics() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                final var controller = controller();
                final var army = controller.getSelected(); army.getPelotoes().clear();
                add(army, "none", 1500, false, controller);
                add(army, "transport", 10, true, controller);
                army.setMoral(0); controller.getScenario().remArmy(army);
                controller.getScenario().addArmy(army, CombatScenario.Provenance.ESTIMATED);
                final var table = table(controller); table.refresh();
                final int row = table.getSelectedRow();
                assertTrue(table.getModel().getValueAt(row, 0).toString().contains("(?)"));
                final String counts = table.getModel().getValueAt(row, 2).toString();
                assertTrue(counts.contains("1,500") || counts.contains("1.500") || counts.contains("1500"));
                assertTrue(counts.contains("10")); assertTrue(counts.contains("?"));
                assertEquals("?", table.getModel().getValueAt(row, 3));
                assertEquals("?", table.getModel().getValueAt(row, 4));
                assertNotEquals("?", table.getModel().getValueAt(row, 5));
                assertFalse(army.getSizeBand().isEmpty());
                assertTrue(table.getModel().getValueAt(row, 0).toString().contains(army.getSizeBand()));
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static void add(ArmySim army, String code, int count, boolean naval, BattleSimControler controller) {
        final TipoTropa type = new TipoTropa(); type.setCodigo(code); type.setNome(code);
        final java.util.SortedMap<model.Terreno, Integer> stats = new java.util.TreeMap<>();
        stats.put(controller.getScenario().getTerreno(), 10);
        type.setAtaqueTerreno(stats); type.setDefesaTerreno(stats); type.setMovimentoTerreno(stats);
        if (naval) { final Habilidade ability = new Habilidade(); ability.setCodigo(";TTN;"); type.addHabilidade(ability); }
        final Pelotao platoon = new Pelotao(); platoon.setTipoTropa(type); platoon.setQtd(count); army.getPelotoes().put(code, platoon);
    }

    @Test void preferencesUseStableIdsClampWidthsAndNeverHideTheName() throws Exception {
        final var settings = SettingsManager.getInstance();
        final String previous = settings.getConfig(CONFIG, "");
        try {
            settings.setConfig(CONFIG, "compact;name:1:0,sea:99999:1,layers:-20:1,obsolete:700:1");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    final var table = table(controller()); table.refresh();
                    assertTrue(table.compact());
                    assertEquals(3, table.getColumnCount());
                    assertEquals("name", table.getColumnModel().getColumn(0).getIdentifier());
                    assertEquals(96, table.getColumnModel().getColumn(0).getPreferredWidth());
                    assertEquals(48, table.getColumn("layers").getPreferredWidth());
                    assertEquals(500, table.getColumn("sea").getPreferredWidth());
                    final JViewport threeColumnViewport = new JViewport();
                    threeColumnViewport.setSize(300, 100); threeColumnViewport.setView(table);
                    assertFalse(table.getScrollableTracksViewportWidth(), "Three optional columns must keep requested widths and scroll");
                    final var popup = table.options();
                    assertFalse(popup.getComponent(0).isEnabled());
                    ((JCheckBoxMenuItem) popup.getComponent(3)).doClick();
                    final var reopened = table(controller()); reopened.refresh();
                    assertEquals(4, reopened.getColumnCount());
                    assertEquals(500, reopened.getColumn("sea").getPreferredWidth());
                    assertTrue(reopened.compact());
                    final JViewport viewport = new JViewport(); viewport.setSize(300, 100); viewport.setView(reopened);
                    assertFalse(reopened.getScrollableTracksViewportWidth(), "Optional wide columns must scroll");
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
        } finally { settings.setConfigAndSaveToFile(CONFIG, previous); }
    }

    @Test void malformedPreferencesStillOpenWithUsableColumns() throws Exception {
        final var settings = SettingsManager.getInstance();
        final String previous = settings.getConfig(CONFIG, "");
        try {
            for (String saved : new String[]{";", ";;", "compact;name:bad", "comfortable;obsolete:99"}) {
                settings.setConfig(CONFIG, saved);
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        final var table = table(controller()); table.refresh();
                        assertEquals(3, table.getColumnCount());
                        assertEquals("name", table.getColumnModel().getColumn(0).getIdentifier());
                    } catch (Exception ex) { throw new RuntimeException(ex); }
                });
            }
        } finally { settings.setConfig(CONFIG, previous); }
    }
}
