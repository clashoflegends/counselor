package gui.services;

import baseLib.GenericoComboObject;
import business.MovimentoExercito;
import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import control.support.ActorInterface;
import control.support.ActorInterfaceFactory;
import e2e.TestWorld;
import gui.subtabs.SubTabDirecaoExercito;
import gui.subtabs.SubTabDirecaoExercitoPreview;
import gui.subtabs.ArmyMovementEditorBase;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import persistenceCommons.SettingsManager;
import model.Exercito;
import model.Habilidade;
import model.Local;
import model.Ordem;
import model.Pelotao;
import model.Personagem;
import model.PersonagemOrdem;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmyDestinationPickTest {

    @BeforeAll
    static void load() throws Exception {
        TestWorld.load();
    }

    @Test
    void previewDisplaysLegacyDirectionAliasesWithoutChangingTheirSavedTokens() throws Exception {
        inWorld(f -> {
            var settings = SettingsManager.getInstance();
            var labels = settings.getBundleManager();
            var preview = new SubTabDirecaoExercitoPreview("NE;H;L;SE;ev", f.actor,
                    new RecordingMap(), f.order, false);
            assertEquals("NE;H;L;SE;ev", preview.getDirecaoDisplay());
            JLabel route = component(preview, JLabel.class, "armyRoute");
            assertEquals(String.join("  ", labels.getString("NORTELESTE.ABREVIADO"),
                    labels.getString("ARMY.EDITOR.HOLD"), labels.getString("LESTE.ABREVIADO"),
                    labels.getString("SULLESTE.ABREVIADO")), route.getText());
            for (String direction : List.of("nw", "sw", "w")) {
                String code = new business.facade.LocalFacade().getIdentificacaoVizinho(f.start, direction);
                if (!f.locals.containsKey(code)) {
                    f.hex(code, f.plain);
                }
            }
            installMap(f);
            var aliases = new SubTabDirecaoExercitoPreview("NO;C;SE;SO;NE;O;L;nr", f.actor,
                    new RecordingMap(), f.order, false);
            assertEquals("NO;C;SE;SO;NE;O;L;nr", aliases.getDirecaoDisplay());
            assertEquals(String.join("  ", labels.getString("NORTEOESTE.ABREVIADO"),
                    labels.getString("ARMY.EDITOR.HOLD"), labels.getString("SULLESTE.ABREVIADO"),
                    labels.getString("SULOESTE.ABREVIADO"), labels.getString("NORTELESTE.ABREVIADO"),
                    labels.getString("OESTE.ABREVIADO"), labels.getString("LESTE.ABREVIADO")),
                    component(aliases, JLabel.class, "armyRoute").getText());
        });
    }

    @Test
    void factorySelectsOneLayoutAndBothLayoutsPreserveCanonicalOrdersAndCosts() throws Exception {
        inWorld(f -> {
            var settings = SettingsManager.getInstance();
            String previous = settings.getConfig("ArmyMovementEditor", "preview");
            try {
                for (boolean all : new boolean[]{false, true}) {
                    for (int allowance : new int[]{8, 12, 14}) {
                        Ordem order = new Ordem();
                        order.addHabilidade(ability(String.format(";AM%02d;", allowance)));
                        List<List<String>> results = new ArrayList<>();
                        for (String layout : List.of("classic", "preview", "", "unknown")) {
                            settings.setConfig("ArmyMovementEditor", layout);
                            RecordingMap map = new RecordingMap();
                            ComponentFactory factory = new ComponentFactory();
                            factory.setActor(f.actor);
                            factory.setMapaControler(map);
                            factory.setAllSelected(all);
                            ArmyMovementEditorBase editor = (ArmyMovementEditorBase) factory.getParametroComponent(
                                    "Direcao_Ex", "ne;h;e;se;ev", "ne;h;e;se;ev", order);
                            assertEquals(!"classic".equals(layout), editor instanceof SubTabDirecaoExercitoPreview);
                            assertEquals(allowance, editor.getLimiteMovimento());
                            if (all) {
                                assertTrue(selectTroop(editor, f.cavalry));
                            }
                            button(editor, "comida").doClick();
                            editor.doMovementTagsRepaint();
                            assertEquals("ne;h;e;se;ev", editor.getDirecoesIdTipo());
                            editor.doApagaUltimo();
                            editor.doMovementTagAdd("se");
                            assertTrue(map.steps.stream().allMatch(step -> step.getLimiteMovimento() == allowance));
                            List<String> ids = new ArrayList<>();
                            List<String> displays = new ArrayList<>();
                            factory.getParametros(editor, ids, displays);
                            results.add(List.of(ids.get(0), displays.get(0), Integer.toString(map.cost()),
                                    map.steps.stream().map(step -> step.getCustoMovimento() + ":"
                                            + step.getDirecaoAnterior()).toList().toString()));
                        }
                        for (List<String> result : results) {
                            assertEquals(results.get(0), result, "parameter and step parity");
                        }
                    }
                }
            } finally {
                settings.setConfig("ArmyMovementEditor", previous);
            }
        });
    }

    @Test
    void previewDestinationEnterReplacesOnlySuccessfulRoutesAndDoesNotSaveOrders() throws Exception {
        inWorld(f -> {
            RecordingMap map = new RecordingMap();
            var preview = new TestPreview(f.actor, map, f.order, false);
            JTextField destination = component(preview, JTextField.class, "armyDestination");
            JLabel feedback = component(preview, JLabel.class, "armyFeedback");
            assertNotNull(destination);
            destination.setText("0404");
            destination.postActionEvent();
            assertEquals("ne;e;se;nr", preview.getDirecoesIdTipo());
            assertEquals(6, map.cost());
            destination.setText("invalid");
            destination.postActionEvent();
            assertEquals("ne;e;se;nr", preview.getDirecoesIdTipo());
            assertEquals("invalid", destination.getText());
            assertEquals(SettingsManager.getInstance().getBundleManager()
                    .getString("ARMY.PATH.PICK.INVALID"), feedback.getText());
            assertTrue(f.character.getAcoes().isEmpty());
            for (String command : List.of("jbBackspace", "apaga", "h")) {
                AbstractButton icon = button(preview, command);
                assertEquals("", icon.getText());
                assertNotNull(icon.getIcon());
                assertFalse(icon.getAccessibleContext().getAccessibleName().isBlank());
            }
            assertFalse(component(preview, JLabel.class, "armyCost").getText().isBlank());
            assertFalse(preview.getDirecaoDisplay().contains("Hold"), "saved display remains canonical");
        });
    }

    @Test
    void previewPickerUsesCurrentSettingsAndEscapeRestoresItsPreviousBinding() throws Exception {
        inWorld(f -> {
            RecordingMap map = new RecordingMap();
            var preview = new TestPreview(f.actor, map, f.order, false);
            AbstractButton pick = button(preview, "armyDestinationPick");
            KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
            var inputs = preview.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputs.put(escape, "previousEscape");
            pick.doClick();
            Object action = inputs.get(escape);
            assertEquals("cancelArmyDestinationPick", action);
            preview.getActionMap().get(action).actionPerformed(new ActionEvent(preview, 0, ""));
            assertEquals("previousEscape", inputs.get(escape));
            assertFalse(pick.isSelected());
            assertNull(map.callback);
            pick.doClick();
            button(preview, "comida").doClick();
            map.pick(f.target);
            assertEquals(9, map.cost());
            assertEquals("previousEscape", inputs.get(escape));
            pick.doClick();
            Consumer<Local> old = map.callback;
            Consumer<Local> other = local -> { };
            map.armCityPick(other);
            preview.hideForTest();
            assertTrue(map.isCityPickArmed(other));
            old.accept(f.start);
            assertEquals("ne;e;se;nr", preview.getDirecoesIdTipo());
        });
    }

    @Test
    void previewExplainsNoShipsAndAllowsAllSimulation() throws Exception {
        inWorld(f -> {
            Local[] targets = sailingMap(f);
            var actual = new TestPreview(f.actor, new RecordingMap(), f.order, false);
            assertFalse(component(actual, JTextField.class, "armyDestination").isEnabled());
            assertEquals(SettingsManager.getInstance().getBundleManager().getString("ARMY.PATH.NO.SHIPS"),
                    component(actual, JLabel.class, "armyFeedback").getText());
            assertFalse(actual.doMovementRouteTo(targets[1]));
            var simulated = new TestPreview(f.actor, new RecordingMap(), f.order, true);
            assertTrue(selectTroop(simulated, f.ship));
            assertTrue(simulated.doMovementRouteTo(targets[1]));
            assertTrue(component(simulated, JLabel.class, "armyAssumptions").getText().contains(f.ship.getNome()));
        });
    }

    @Test
    void savedMovementReopensWithItsModeCostsAndCanonicalParameters() throws Exception {
        inWorld(f -> {
            for (String mode : List.of("nr", "ev")) {
                RecordingMap originalMap = new RecordingMap();
                TestPanel original = f.panel(originalMap, false);
                if ("ev".equals(mode)) {
                    button(original, "evasivo").doClick();
                }
                for (String direction : List.of("e", "h", "e")) {
                    original.doMovementTagAdd(direction);
                }
                String savedId = original.getDirecoesIdTipo();
                String savedDisplay = original.getDirecaoDisplay();
                ComponentFactory factory = new ComponentFactory();
                List<String> ids = new ArrayList<>();
                List<String> displays = new ArrayList<>();
                factory.getParametros(original, ids, displays);
                assertEquals(List.of(savedId), ids);
                assertEquals(List.of(savedDisplay), displays);
                List<Integer> costs = originalMap.steps.stream()
                        .map(MovimentoExercito::getCustoMovimento).toList();

                RecordingMap reopenedMap = new RecordingMap();
                TestPanel reopened = new TestPanel(displays.get(0), f.actor, reopenedMap, f.order, false);
                assertEquals(savedId, reopened.getDirecoesIdTipo());
                assertEquals(savedDisplay, reopened.getDirecaoDisplay());
                List<String> reopenedIds = new ArrayList<>();
                List<String> reopenedDisplays = new ArrayList<>();
                factory.getParametros(reopened, reopenedIds, reopenedDisplays);
                assertEquals(ids, reopenedIds);
                assertEquals(displays, reopenedDisplays);
                assertTrue(button(reopened, "ev".equals(mode) ? "evasivo" : "normal").isSelected());
                assertEquals(costs, reopenedMap.steps.stream()
                        .map(MovimentoExercito::getCustoMovimento).toList(), "including the first step");
                reopened.doMovementTagsRepaint();
                assertEquals(savedDisplay, reopened.getDirecaoDisplay());
                assertEquals(costs, reopenedMap.steps.stream()
                        .map(MovimentoExercito::getCustoMovimento).toList());
                reopened.doApagaUltimo();
                assertEquals("e;h;" + mode, reopened.getDirecoesIdTipo());
                reopened.doApaga();
                assertEquals(mode, reopened.getDirecoesIdTipo());
            }
            TestPanel legacy = new TestPanel("e;h;e", f.actor, new RecordingMap(), f.order, false);
            assertEquals("e;h;e;nr", legacy.getDirecoesIdTipo());
            assertTrue(button(legacy, "normal").isSelected());
        });
    }

    @Test
    void actualArmyUsesAllItsTroopsWhileAllModeUsesTheSelectedType() throws Exception {
        inWorld(f -> {
            RecordingMap map = new RecordingMap();
            TestPanel actual = f.panel(map, false);
            assertTrue(selectTroop(actual, f.cavalry));
            assertTrue(actual.doMovementRouteTo(f.target));
            assertEquals("ne;e;se;nr", actual.getDirecoesIdTipo());
            assertEquals(6, map.cost());

            TestPanel all = f.panel(map, true);
            assertTrue(selectTroop(all, f.cavalry));
            assertTrue(all.doMovementRouteTo(f.target));
            assertEquals("e;e;nr", all.getDirecoesIdTipo());
            assertEquals(3, map.cost());
        });
    }

    @Test
    void mapPickUsesFoodSettingsAtClickTimeAndRemainsEditableWithoutSavingTheOrder() throws Exception {
        inWorld(f -> {
            RecordingMap map = new RecordingMap();
            TestPanel panel = f.panel(map, false);
            AbstractButton pick = button(panel, "armyDestinationPick");
            assertNotNull(pick.getIcon());
            assertEquals("", pick.getText(), "the map icon replaces the copied erase glyph");
            panel.doMovementTagAdd("h");
            pick.doClick();
            assertTrue(pick.isSelected());
            AbstractButton noFood = button(panel, "comida");
            assertFalse(noFood.isSelected());
            noFood.doClick();
            map.pick(f.target);
            assertFalse(pick.isSelected());
            assertNull(map.callback);
            assertEquals("ne;e;se;nr", panel.getDirecoesIdTipo());
            assertEquals(9, map.cost(), "per-edge food rounding also matches the map tags");
            assertTrue(f.character.getAcoes().isEmpty(), "selecting a path does not save an order");
            panel.doApagaUltimo();
            assertEquals("ne;e;nr", panel.getDirecoesIdTipo());
            panel.doMovementTagAdd("se");
            assertEquals("ne;e;se;nr", panel.getDirecoesIdTipo());
        });
    }

    @Test
    void unreachablePicksKeepTheExistingPathAndPickingTheOriginClearsIt() throws Exception {
        inWorld(f -> {
            RecordingMap map = new RecordingMap();
            TestPanel panel = f.panel(map, false);
            panel.setDirecaoTipo("ev");
            panel.doMovementTagAdd("h");
            String before = panel.getDirecoesIdTipo();
            assertFalse(panel.doMovementRouteTo(null));
            assertEquals(before, panel.getDirecoesIdTipo());
            Local outside = new Local();
            outside.setCodigo("9999");
            assertFalse(panel.doMovementRouteTo(outside));
            assertEquals(before, panel.getDirecoesIdTipo());
            assertTrue(panel.doMovementRouteTo(f.start));
            assertEquals("ev", panel.getDirecoesIdTipo());
            assertTrue(map.steps.isEmpty());
        });
    }

    @Test
    void secondClickAndHiddenPanelsCancelOnlyTheirOwnPicker() throws Exception {
        inWorld(f -> {
            RecordingMap map = new RecordingMap();
            TestPanel panel = f.panel(map, false);
            AbstractButton pick = button(panel, "armyDestinationPick");
            pick.doClick();
            Consumer<Local> stale = map.callback;
            assertTrue(map.isCityPickArmed(stale));
            pick.doClick();
            assertFalse(pick.isSelected());
            assertFalse(map.isCityPickArmed(stale));
            stale.accept(f.target);
            assertEquals("nr", panel.getDirecoesIdTipo());

            pick.doClick();
            stale = map.callback;
            Consumer<Local> newerPicker = local -> { };
            map.armCityPick(newerPicker);
            panel.hideForTest();
            assertFalse(pick.isSelected());
            assertTrue(map.isCityPickArmed(newerPicker), "hide cannot disarm another window's picker");
            stale.accept(f.target);
            assertEquals("nr", panel.getDirecoesIdTipo());
            map.cancelCityPick(newerPicker);
        });
    }

    @Test
    void plans24PointsWhileMapTagsRetainThe12PointPerTurnAllowance() throws Exception {
        inWorld(f -> {
            f.order.getHabilidades().clear();
            f.order.addHabilidade(ability(";AM12;"));
            f.locals.clear();
            f.start.setEstrada("");
            f.locals.put(f.start.getCodigo(), f.start);
            for (int col = 3; col <= 11; col++) {
                f.hex(String.format("%02d04", col), f.plain);
            }
            installMap(f);
            RecordingMap map = new RecordingMap();
            TestPanel panel = f.panel(map, false);
            assertTrue(panel.doMovementRouteTo(f.locals.get("1004")));
            assertEquals(24, map.cost());
            assertEquals(8, map.steps.size());
            assertTrue(map.steps.stream().allMatch(step -> step.getLimiteMovimento() == 12));
            String accepted = panel.getDirecoesIdTipo();
            assertFalse(panel.doMovementRouteTo(f.locals.get("1104")));
            assertEquals(accepted, panel.getDirecoesIdTipo());
        });
    }

    @Test
    void sailingWithoutShipsRejectsCoastAndSeaEvenWhenTheScenarioOffersShips() throws Exception {
        inWorld(f -> {
            Local[] targets = sailingMap(f);
            for (boolean zeroQuantity : new boolean[]{false, true}) {
                if (zeroQuantity) {
                    addShips(f, f.ship, 0);
                }
                RecordingMap map = new RecordingMap();
                PersonagemOrdem saved = new PersonagemOrdem();
                saved.setOrdem(f.order);
                saved.setParametrosId(new ArrayList<>(List.of("e;e;nr")));
                f.character.setAcao(0, saved);
                TestPanel actual = new TestPanel("e;e;nr", f.actor, map, f.order, false);
                AbstractButton pick = button(actual, "armyDestinationPick");
                assertFalse(pick.isEnabled());
                assertEquals(persistenceCommons.SettingsManager.getInstance().getBundleManager()
                        .getString("ARMY.PATH.NO.SHIPS"), pick.getToolTipText());
                assertFalse(actual.doMovementRouteTo(targets[0]));
                assertFalse(actual.doMovementRouteTo(targets[1]));
                actual.doMovementTagAdd("e");
                assertEquals("nr", actual.getDirecoesIdTipo());
                assertTrue(map.steps.isEmpty());
                assertFalse(button(actual, "e").isEnabled());
                assertSame(saved, f.character.getAcoes().get(0));
                assertEquals(List.of("e;e;nr"), saved.getParametrosId(), "restoring invalid input cannot edit saved orders");
            }
            RecordingMap map = new RecordingMap();
            TestPanel all = f.panel(map, true);
            assertTrue(button(all, "armyDestinationPick").isEnabled());
            assertTrue(selectTroop(all, f.ship));
            assertTrue(all.doMovementRouteTo(targets[0]));
            assertTrue(all.doMovementRouteTo(targets[1]));
            assertEquals(2, map.cost(), "All can still simulate ships along coast and into open sea");
        });
    }

    @Test
    void bothOrderEditorsAndMovSimPreserveInputButDisplayOnlyTheLandingPrefix() throws Exception {
        inWorld(f -> {
            Local[] targets = sailingMap(f);
            Local landing = targets[0]; landing.setTerreno(f.forest); landing.setLanding("63");
            f.ship.getMovimentoTerreno().put(f.forest, 5);
            addShips(f, f.ship, 10);
            for (boolean preview : new boolean[]{false, true}) {
                RecordingMap map = new RecordingMap();
                ArmyMovementEditorBase editor = preview
                        ? new SubTabDirecaoExercitoPreview("E;H;E;nr", f.actor, map, f.order, false)
                        : new SubTabDirecaoExercito("E;H;E;nr", f.actor, map, f.order, false);
                assertEquals("E;H;E;nr", editor.getDirecoesIdTipo());
                assertEquals(1, map.steps.size());
                assertSame(landing, map.steps.get(0).getDestino());
                assertEquals(5, map.cost());
                editor.doMovementTagsRepaint();
                assertEquals("E;H;E;nr", editor.getDirecoesIdTipo());
                assertEquals(1, map.steps.size());
                assertFalse(editor.doMovementRouteTo(targets[1]));
            }
            RecordingMap map = new RecordingMap();
            var simulator = new SubTabDirecaoExercito(f.character.getLocal(), 14, true, map);
            assertTrue(selectTroop(simulator, f.ship));
            for (String direction : List.of("e", "h", "e")) simulator.doMovementTagAdd(direction);
            assertEquals(1, map.steps.size());
            assertSame(landing, map.steps.get(0).getDestino());
            assertEquals(5, map.cost());
        });
    }

    @Test
    void actualSailingUsesPositiveShipTypesAndIgnoresZeroQuantityShips() throws Exception {
        inWorld(f -> {
            Local[] targets = sailingMap(f);
            addShips(f, f.ship, 2);
            TipoTropa absent = new TipoTropa();
            absent.setCodigo("ABSENT_SHIP");
            absent.setNome("Absent ship");
            absent.addHabilidade(ability(";TTN;"));
            absent.getMovimentoTerreno().put(targets[0].getTerreno(), 100);
            absent.getMovimentoTerreno().put(f.water, 100);
            addShips(f, absent, 0);
            RecordingMap map = new RecordingMap();
            TestPanel actual = f.panel(map, false);
            assertTrue(button(actual, "armyDestinationPick").isEnabled());
            assertTrue(actual.doMovementRouteTo(targets[1]));
            assertEquals("e;e;nr", actual.getDirecoesIdTipo());
            assertEquals(2, map.cost());
            assertTrue(map.steps.stream().allMatch(MovimentoExercito::isPorAgua));
        });
    }

    private static Habilidade ability(String code) {
        Habilidade ability = new Habilidade();
        ability.setCodigo(code);
        return ability;
    }

    private static void addShips(Fixture f, TipoTropa type, int quantity) {
        Pelotao ships = new Pelotao();
        ships.setTipoTropa(type);
        ships.setQtd(quantity);
        f.character.getExercito().getPelotoes().put(type.getCodigo(), ships);
    }

    private static Local[] sailingMap(Fixture f) {
        f.order.addHabilidade(ability(";AMW;"));
        f.locals.clear();
        Terreno coast = new Terreno();
        coast.setCodigo("C");
        coast.setNome("Coast");
        coast.setAgua(true);
        f.ship.getMovimentoTerreno().put(coast, 1);
        Local pentos = f.hex("3248", coast);
        Local coastalTarget = f.hex("3348", coast);
        Local seaTarget = f.hex("3448", f.water);
        f.character.setLocal(pentos);
        f.character.getExercito().setLocal(pentos);
        installMap(f);
        return new Local[]{coastalTarget, seaTarget};
    }

    private static void installMap(Fixture f) {
        var locals = WorldFacadeCounselor.getInstance().getLocais();
        locals.clear();
        locals.putAll(f.locals);
    }

    private static void inWorld(Consumer<Fixture> test) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var locals = WorldFacadeCounselor.getInstance().getLocais();
            var saved = new TreeMap<>(locals);
            var troops = WorldFacadeCounselor.getInstance().getCenario().getTipoTropas();
            var savedTroops = new TreeMap<>(troops);
            try {
                Fixture f = new Fixture();
                locals.clear();
                locals.putAll(f.locals);
                troops.clear();
                for (TipoTropa troop : List.of(f.infantry, f.cavalry, f.ship)) {
                    troops.put(troop.getCodigo(), troop);
                }
                test.accept(f);
            } finally {
                locals.clear();
                locals.putAll(saved);
                troops.clear();
                troops.putAll(savedTroops);
            }
        });
    }

    private static AbstractButton button(Container parent, String nameOrCommand) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton button && (nameOrCommand.equals(button.getName())
                    || nameOrCommand.equals(button.getActionCommand()))) {
                return button;
            }
            if (child instanceof Container container) {
                AbstractButton found = button(container, nameOrCommand);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T component(Container parent, Class<T> type, String name) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = component(container, type, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean selectTroop(Container parent, TipoTropa troop) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JComboBox<?> combo) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    if (combo.getItemAt(i) instanceof GenericoComboObject item
                            && item.getObject() == troop) {
                        combo.setSelectedIndex(i);
                        return true;
                    }
                }
            }
            if (child instanceof Container container) {
                if (selectTroop(container, troop)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class Fixture extends ArmyRouteFinderTest.Fixture {
        final Personagem character = new Personagem();
        final ActorInterface actor;
        final Ordem order = new Ordem();

        Fixture() {
            character.setCodigo("route-test");
            character.setLocal(start);
            Exercito army = new Exercito();
            army.setCodigo("route-army");
            army.setLocal(start);
            army.setComida(1000);
            for (TipoTropa troop : List.of(infantry, cavalry)) {
                troop.setUpkeepFood(1);
                Pelotao platoon = new Pelotao();
                platoon.setTipoTropa(troop);
                platoon.setQtd(100);
                army.getPelotoes().put(troop.getCodigo(), platoon);
            }
            character.setExercito(army);
            actor = ActorInterfaceFactory.getInstance().getActorInterface(character);
            Habilidade limit = new Habilidade();
            limit.setCodigo(";AM14;");
            order.addHabilidade(limit);
        }

        TestPanel panel(RecordingMap map, boolean all) {
            return new TestPanel(actor, map, order, all);
        }
    }

    private static final class TestPanel extends SubTabDirecaoExercito {
        private boolean shown = true;

        TestPanel(ActorInterface actor, MapaControler map, Ordem order, boolean all) {
            this("", actor, map, order, all);
        }

        TestPanel(String initial, ActorInterface actor, MapaControler map, Ordem order, boolean all) {
            super(initial, actor, map, order, all);
        }

        @Override
        public boolean isShowing() {
            return shown;
        }

        void hideForTest() {
            shown = false;
            HierarchyEvent event = new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                    this, this, HierarchyEvent.SHOWING_CHANGED);
            for (var listener : getHierarchyListeners()) {
                listener.hierarchyChanged(event);
            }
        }
    }

    private static final class TestPreview extends SubTabDirecaoExercitoPreview {
        private boolean shown = true;

        TestPreview(ActorInterface actor, MapaControler map, Ordem order, boolean all) {
            super("", actor, map, order, all);
        }

        @Override
        public boolean isShowing() {
            return shown;
        }

        void hideForTest() {
            shown = false;
            HierarchyEvent event = new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                    this, this, HierarchyEvent.SHOWING_CHANGED);
            for (var listener : getHierarchyListeners()) {
                listener.hierarchyChanged(event);
            }
        }
    }

    private static final class RecordingMap extends MapaControler {
        final List<MovimentoExercito> steps = new ArrayList<>();
        Consumer<Local> callback;

        RecordingMap() {
            super(new JPanel());
        }

        @Override
        public void armCityPick(Consumer<Local> callback) {
            super.armCityPick(callback);
            this.callback = callback;
        }

        @Override
        public void cancelCityPick(Consumer<Local> callback) {
            super.cancelCityPick(callback);
            if (this.callback == callback) {
                this.callback = null;
            }
        }

        void pick(Local local) {
            Consumer<Local> selected = callback;
            cancelCityPick(selected);
            selected.accept(local);
        }

        @Override
        public void addMovementTag(MovimentoExercito movement) {
            steps.add(movement);
        }

        @Override
        public void remMovementTag() {
            steps.clear();
        }

        int cost() {
            return steps.stream().mapToInt(MovimentoExercito::getCustoMovimento).sum();
        }
    }
}
