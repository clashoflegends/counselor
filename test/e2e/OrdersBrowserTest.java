package e2e;

import baseLib.BaseModel;
import control.OrdemJogadorControler;
import control.facade.WorldFacadeCounselor;
import control.services.OrderEditTime;
import control.services.OrdersList;
import control.support.DispatchManager;
import gui.services.OrdersPanel;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
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
}
