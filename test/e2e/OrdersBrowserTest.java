package e2e;

import baseLib.BaseModel;
import control.OrdemJogadorControler;
import control.facade.WorldFacadeCounselor;
import control.services.OrderEditTime;
import control.services.OrdersList;
import control.support.DispatchManager;
import gui.services.OrdersPanel;
import java.io.File;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.*;
import persistenceCommons.SettingsManager;
import model.Comando;
import model.ComandoDetail;
import model.Ordem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.Test;
import persistenceCommons.XmlManager;
import static org.junit.jupiter.api.Assertions.*;

class OrdersBrowserTest {
    private static final Instant OLD = Instant.parse("2026-10-16T18:00:00Z");
    private static final Instant NEW = Instant.parse("2026-10-17T18:00:00Z");

    private PersonagemOrdem order(String... params) {
        Ordem action = new Ordem(); action.setCodigo("555"); action.setNome("Map Area"); action.setDescricao("Map Area");
        PersonagemOrdem po = new PersonagemOrdem(); po.setOrdem(action);
        po.setParametrosId(List.of(params)); po.setParametrosDisplay(List.of(params));
        po.setRecordedUpdateTime(OLD);
        return po;
    }

    @Test void noOpSavePreservesTimeEvenWhenDisplayLanguageChanges() {
        PersonagemOrdem before = order("0968"), after = order("0968");
        after.setParametrosDisplay(List.of("Localized target"));
        OrderEditTime.stamp(before, after, NEW);
        assertEquals(OLD, after.getRecordedUpdateTime());
    }

    @Test void realEditsAndNewSlotsGetSaveTime() {
        PersonagemOrdem before = order("0968"), after = order("0969");
        OrderEditTime.stamp(before, after, NEW);
        assertEquals(NEW, after.getRecordedUpdateTime());
        after = order("0968"); OrderEditTime.stamp(null, after, NEW);
        assertEquals(NEW, after.getRecordedUpdateTime());
        after = order("0968"); after.getOrdem().setCodigo("556");
        OrderEditTime.stamp(before, after, NEW);
        assertEquals(NEW, after.getRecordedUpdateTime());
    }

    @Test void noOpDoesNotInventMissingTime() {
        PersonagemOrdem before = order("0968"), after = order("0968");
        before.setRecordedUpdateTime(null);
        OrderEditTime.stamp(before, after, NEW);
        assertNull(after.getRecordedUpdateTime());
    }

    @Test void malformedTimestampIsUnknownWithoutChangingLegacyGetterContract() throws Exception {
        PersonagemOrdem po = order("0968");
        var field = PersonagemOrdem.class.getDeclaredField("updateTime"); field.setAccessible(true);
        field.set(po, "not a timestamp");
        assertNull(po.getRecordedUpdateTime());
        assertThrows(java.time.format.DateTimeParseException.class, po::getUpdateTime);
    }

    @Test void knownAndUnknownTimesRoundTripInExistingOrderFields() throws Exception {
        WorldFacadeCounselor world = TestWorld.load();
        BaseModel pc = TestWorld.freshPersonagem(world);
        Ordem action = TestWorld.ordem(world, 1555);
        Comando command = new Comando(); command.setInfos(world.getPartida());
        command.addComando(pc, action, new java.util.ArrayList<>(List.of("A")), new java.util.ArrayList<>(List.of("A")), OLD);
        command.addComando(pc, action, new java.util.ArrayList<>(List.of("B")), new java.util.ArrayList<>(List.of("B")), null);
        File file = File.createTempFile("orders_time", ".rc.egf"); file.deleteOnExit();
        world.doSaveOrdens(command, file);
        Comando read = (Comando) XmlManager.getInstance().get(file);
        assertEquals(OLD, read.getOrdens().get(0).getRecordedUpdateTime());
        assertNull(read.getOrdens().get(1).getRecordedUpdateTime());
        assertNotNull(read.getOrdens().get(1).getUpdateTime(), "legacy fallback remains available");
        String xml = XmlManager.getInstance().toXml(command);
        assertTrue(xml.contains("<updateTime>"));
        assertFalse(xml.contains("recordedUpdateTime"));
    }

    @Test void searchIsLiteralAndTokenizedAndDateUsesSelectedTimezone() throws Exception {
        BaseModel pc = TestWorld.freshPersonagem(TestWorld.load());
        PersonagemOrdem po = order("0968", "[area]");
        po.setRecordedUpdateTime(Instant.parse("2026-10-17T01:00:00Z")); pc.setAcao(2, po);
        var row = OrdersList.collect(List.of(pc), new TreeMap<>()).get(0);
        assertEquals(2, row.slot());
        assertTrue(OrdersList.matches(row, "MAP 0968 [area]", null, ZoneId.of("UTC")));
        assertFalse(OrdersList.matches(row, "0969", null, ZoneId.of("UTC")));
        assertTrue(OrdersList.matches(row, "", LocalDate.of(2026, 10, 17), ZoneId.of("UTC")));
        assertFalse(OrdersList.matches(row, "", LocalDate.of(2026, 10, 17), ZoneId.of("America/Edmonton")));
        po.setRecordedUpdateTime(null);
        assertFalse(OrdersList.matches(row, "", LocalDate.of(2026, 10, 17), ZoneId.of("UTC")));
        assertTrue(OrdersList.matches(row, "", null, ZoneId.of("UTC")));
        pc.remAcoes(); assertTrue(OrdersList.collect(List.of(pc), new TreeMap<>()).isEmpty());
    }

    @Test void tableSortsInstantsAndRefreshesAfterDispatchWithoutLosingSelection() throws Exception {
        WorldFacadeCounselor world = TestWorld.load();
        BaseModel pc = TestWorld.freshPersonagem(world);
        world.getActors().forEach(BaseModel::remAcoes);
        pc.setAcao(0, order("old"));
        PersonagemOrdem newest = order("new"); newest.setRecordedUpdateTime(NEW); pc.setAcao(1, newest);
        PersonagemOrdem unknown = order("unknown"); unknown.setRecordedUpdateTime(null); pc.setAcao(2, unknown);
        AtomicReference<OrdersPanel> panel = new AtomicReference<>();
        AtomicReference<OrdemJogadorControler> control = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            panel.set(new OrdersPanel()); control.set(new OrdemJogadorControler(panel.get()));
            var table = panel.get().table();
            assertEquals(NEW, table.getValueAt(0, 0)); assertEquals(OLD, table.getValueAt(1, 0));
            assertNull(table.getValueAt(2, 0));
            table.setRowSelectionInterval(1, 1);
            pc.setAcao(3, order("added"));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_COUNT);
        });
        SwingUtilities.invokeAndWait(() -> {
            var table = panel.get().table();
            assertEquals(4, table.getRowCount());
            assertEquals("old", table.getValueAt(table.getSelectedRow(), 3));
            table.moveColumn(0, 2);
            for (int r = 0; r < table.getRowCount(); r++) {
                table.prepareRenderer(table.getCellRenderer(r, 2), r, 2);
            }
        });
    }

    @Test void waterlineUsesOnlyKnownOwnCharacterTimes() {
        model.Jogador me = new model.Jogador(); me.setId(11);
        model.Jogador ally = new model.Jogador(); ally.setId(22);
        model.Nacao mine = new model.Nacao(); mine.setId(1); mine.setCodigo("M"); mine.setOwner(me); mine.setNome("Mine");
        model.Nacao theirs = new model.Nacao(); theirs.setId(2); theirs.setCodigo("A"); theirs.setOwner(ally); theirs.setNome("Ally");
        // Merged nation membership must not make an ally count as my PC.
        me.addNacao(theirs);
        model.Personagem pc = new model.Personagem(); pc.setNacao(mine); pc.setNome("Own"); pc.setCodigo("P");
        model.Personagem other = new model.Personagem(); other.setNacao(theirs); other.setNome("Other"); other.setCodigo("Q");
        pc.setAcao(0, order("own"));
        PersonagemOrdem later = order("later"); later.setRecordedUpdateTime(NEW);
        other.setAcao(0, later); mine.setAcao(0, later);
        var rows = OrdersList.collect(List.of(pc, other, mine), new TreeMap<>());
        assertEquals(OLD, OrdersList.latestOwnCharacterTime(rows, me));
        pc.getAcao(0).setRecordedUpdateTime(null);
        assertNull(OrdersList.latestOwnCharacterTime(rows, me));
        assertNull(OrdersList.latestOwnCharacterTime(rows, null));
    }

    @Test void filtersKeepRelativeDatesAndClearResetsAllFiltersWithoutChangingLayout() throws Exception {
        WorldFacadeCounselor world = TestWorld.load();
        BaseModel pc = TestWorld.freshPersonagem(world);
        world.getActors().forEach(BaseModel::remAcoes);
        Instant yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        PersonagemOrdem po = order("yesterday"); po.setRecordedUpdateTime(yesterday); pc.setAcao(0, po);
        PersonagemOrdem old = order("old"); old.setRecordedUpdateTime(yesterday.minusSeconds(172800)); pc.setAcao(1, old);
        SwingUtilities.invokeAndWait(() -> {
            OrdersPanel panel = new OrdersPanel();
            JTextField search = named(panel, "ordersSearch", JTextField.class);
            JTextField since = named(panel, "ordersSince", JTextField.class);
            JComboBox<?> players = named(panel, "ordersPlayers", JComboBox.class);
            JComboBox<?> nations = named(panel, "ordersNations", JComboBox.class);
            JComboBox<?> zones = named(panel, "ordersTimezone", JComboBox.class);
            JLabel reference = named(panel, "ordersWaterline", JLabel.class);
            String baseline = reference.getText();
            search.setText("yesterday"); players.setSelectedIndex(1); nations.setSelectedIndex(1);
            since.setText(yesterday.atZone(ZoneId.of("UTC")).toLocalDate().toString());
            assertEquals(1, panel.table().getRowCount());
            assertTrue(renderedTime(panel, 0).startsWith(OrdersPanel.tx("YESTERDAY")));
            panel.refresh();
            assertTrue(renderedTime(panel, 0).startsWith(OrdersPanel.tx("YESTERDAY")));
            panel.table().moveColumn(0, 2);
            assertTrue(renderedTime(panel, 0).startsWith(OrdersPanel.tx("YESTERDAY")));
            search.setText("no matches");
            assertEquals(0, panel.table().getRowCount());
            assertEquals(baseline, reference.getText(), "filters cannot move the waterline");
            zones.setSelectedIndex(1);
            named(panel, "ordersMovement", JCheckBox.class).doClick();
            since.setText("invalid date");
            buttons(panel).stream().filter(b -> b.getText().equals(OrdersPanel.tx("CLEAR"))).findFirst().orElseThrow().doClick();
            assertEquals(2, panel.table().getRowCount());
            assertEquals("", search.getText()); assertEquals("", since.getText());
            assertEquals(0, players.getSelectedIndex()); assertEquals(0, nations.getSelectedIndex());
            assertFalse(named(panel, "ordersMovement", JCheckBox.class).isSelected());
            assertEquals(1, zones.getSelectedIndex(), "timezone is a display preference, not a filter");
            zones.setSelectedIndex(0);
            assertTrue(renderedTime(panel, 0).startsWith(OrdersPanel.tx("YESTERDAY")));
        });
    }

    @Test void moreColumnsAndDividerSurviveReopeningAndClampToSmallPanes() throws Exception {
        TestWorld.load();
        SettingsManager settings = SettingsManager.getInstance();
        String columns = settings.getConfig("OrdersTab.MoreColumns", "0");
        String divider = settings.getConfig("OrdersTab.Divider", "400");
        try {
            SwingUtilities.invokeAndWait(() -> {
                settings.setConfig("OrdersTab.MoreColumns", "0"); settings.setConfig("OrdersTab.Divider", "400");
                OrdersPanel first = new OrdersPanel();
                JCheckBox more = named(first, "ordersMoreColumns", JCheckBox.class);
                assertEquals(4, first.table().getColumnCount()); more.doClick();
                assertEquals(8, first.table().getColumnCount());
                JSplitPane split = named(first, "ordersDivider", JSplitPane.class);
                resizeSplit(split, 800); split.setDividerLocation(275);
                first.clearFilters();
                assertTrue(more.isSelected()); assertEquals(275, split.getDividerLocation());
                OrdersPanel reopened = new OrdersPanel();
                assertTrue(named(reopened, "ordersMoreColumns", JCheckBox.class).isSelected());
                assertEquals(8, reopened.table().getColumnCount());
                JSplitPane restored = named(reopened, "ordersDivider", JSplitPane.class);
                resizeSplit(restored, 800);
                assertEquals(275, restored.getDividerLocation());
                resizeSplit(restored, 210);
                assertTrue(restored.getDividerLocation() <= 210 - restored.getDividerSize() - 90);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                settings.setConfig("OrdersTab.MoreColumns", columns); settings.setConfig("OrdersTab.Divider", divider);
            });
        }
    }

    @Test void sharedArrowsAndPaintedWaterlineFollowTimeAndSortButNotFilters() throws Exception {
        WorldFacadeCounselor world = TestWorld.load();
        BaseModel own = TestWorld.freshPersonagem(world);
        BaseModel ally = world.getActors().stream().filter(actor -> actor instanceof model.Personagem
                && actor.getNacao() != null && !OrdersList.ownedBy(actor.getNacao(), world.getJogadorAtivo()))
                .findFirst().orElseThrow();
        world.getActors().forEach(BaseModel::remAcoes);
        own.setAcao(0, order("own"));
        Instant[] times = {NEW, OLD, OLD.minusSeconds(600), null};
        for (int i = 0; i < times.length; i++) {
            PersonagemOrdem po = order("shared" + i); po.setRecordedUpdateTime(times[i]); ally.setAcao(i, po);
        }
        SwingUtilities.invokeAndWait(() -> {
            OrdersPanel panel = new OrdersPanel(); JTable table = panel.table();
            String baseline = named(panel, "ordersWaterline", JLabel.class).getText();
            for (int row = 0; row < table.getRowCount(); row++) {
                assertEquals(NEW.equals(table.getValueAt(row, 0)), renderedTime(panel, row).startsWith("\u2191 "));
            }
            assertPaintedBoundary(table, 1, true);
            named(panel, "ordersPlayers", JComboBox.class).setSelectedIndex(2); // Shared only
            assertEquals(baseline, named(panel, "ordersWaterline", JLabel.class).getText());
            assertPaintedBoundary(table, 1, true);
            table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
            assertPaintedBoundary(table, 3, true);
            table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));
            assertPaintedBoundary(table, 1, false);
            own.getAcao(0).setRecordedUpdateTime(null); panel.refresh();
            assertFalse(named(panel, "ordersWaterline", JLabel.class).isVisible());
            for (int row = 0; row < table.getRowCount(); row++) assertFalse(renderedTime(panel, row).startsWith("\u2191 "));
            assertPaintedBoundary(table, 1, false);
        });
    }

    private static void assertPaintedBoundary(JTable table, int row, boolean expected) {
        table.setSize(630, table.getRowCount() * table.getRowHeight()); table.doLayout();
        var image = new java.awt.image.BufferedImage(630, table.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        table.paint(graphics); graphics.dispose();
        int y = table.getCellRect(row, 0, true).y;
        assertEquals(expected, image.getRGB(610, y) == table.getSelectionBackground().getRGB());
    }

    private static void resizeSplit(JSplitPane split, int height) {
        split.setSize(630, height);
        split.dispatchEvent(new ComponentEvent(split, ComponentEvent.COMPONENT_RESIZED));
    }

    private static String renderedTime(OrdersPanel panel, int row) {
        JTable table = panel.table(); int column = table.convertColumnIndexToView(0);
        return ((JLabel) table.prepareRenderer(table.getCellRenderer(row, column), row, column)).getText();
    }

    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) return type.cast(c);
            if (c instanceof Container child) {
                T found = named(child, name, type); if (found != null) return found;
            }
        }
        return null;
    }

    private static List<AbstractButton> buttons(Container root) {
        List<AbstractButton> result = new java.util.ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton button) result.add(button);
            if (c instanceof Container child) result.addAll(buttons(child));
        }
        return result;
    }
}
