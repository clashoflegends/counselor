/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import business.ArmyPath;
import business.MapaManager;
import business.MovimentoExercito;
import business.MovimentoPersonagem;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import persistence.local.ListFactory;
import control.facade.WorldFacadeCounselor;
import control.services.LocalConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.MainMapaGui;
import gui.accessories.DialogHexView;
import gui.components.DialogTextArea;
import business.converter.ColorFactory;
import gui.services.ComponentFactory;
import gui.services.HexRangeOutline;
import gui.services.IPopupTabGui;
import gui.services.ScaledMapIcon;
import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import model.Cenario;
import model.Exercito;
import model.Jogador;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import radialMenu.RadialActions;
import radialMenu.RadialMenu;
import radialMenu.mapmenu.MapMenuManager;
import radialMenu.worldBuilder.WorldBuilderMenuManager;
import radialMenu.worldBuilder.WorldBuilderRadialActions;

/**
 *
 * @author gurgel
 */
public class MapaControler extends ControlBase implements Serializable, ItemListener, MouseListener {

    private static final Log log = LogFactory.getLog(MapaControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final LocalFacade localFacade = new LocalFacade();
    private ExercitoFacade exercitoFacade = null;
    private MapaManager mapaManager; //tem que criar depois para incluir o form
    private ListFactory listFactory = null;
    private MainMapaGui tabGui;
    private DialogTextArea hexInfo;
    private Local localAtual;
    // One-shot "pick a city on the map" callback (armed by CidadeAlvoPicker). Last-wins; cleared after
    // the next left-click consumes it, so a stale picker never keeps intercepting map clicks.
    private Consumer<Local> cityPickCallback;
    private final List<JTable> tables = new ArrayList<>();
    // Hex-range border for the active order parameter, cached on (origem, range) - see showRangeOutline.
    // ComponentFactory passes 9999 for the unbounded Coordenada* controls; that draws nothing.
    private static final int RANGE_UNBOUNDED = 999;
    /** properties.config ColorHexRange default - white, what the feature shipped with. */
    public static final String RANGE_COLOR_DEFAULT_HEX = "FFFFFF";
    private Object rangeOutlineOwner; // the picker that put the border up; identity, see clearRangeOutline
    private Local rangeOutlineOrigem;
    private int rangeOutlineRange;
    private Shape rangeOutlineShape;
    private RadialMenu rmActive;
    private MapMenuManager mapMenuManager;
    private DialogHexView hexView;
    private Jogador jogadorAtivo;

    public MapaControler(JPanel form) {
        initialize(form);
    }

    public final void initialize(JPanel form) {
        this.jogadorAtivo = WorldFacadeCounselor.getInstance().getPartida().getJogadorAtivo();
        listFactory = new ListFactory();
        clearRangeOutline(); // reused across EGF loads (MainResultWindowGui), so drop the previous world's border
        exercitoFacade = new ExercitoFacade();
        final Cenario cenario = WorldFacadeCounselor.getInstance().getCenario();
        mapaManager = new MapaManager(cenario, form);
        mapaManager.setLocais(listFactory.listLocais());
        registerDispatchManagerForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
        registerDispatchManagerForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        registerDispatchManagerForMsg(DispatchManager.LOCAL_MAP_REDRAW);
        registerDispatchManagerForMsg(DispatchManager.LOCAL_RANGE_CLICK);
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
        registerDispatchManagerForMsg(DispatchManager.GUI_STATUS_PERSIST);
        //inicialize locais
        WorldBuilderMenuManager.getInstance().doCanvasReset(mapaManager.getMapMaxSize(listFactory.listLocais().values()));
        WorldBuilderMenuManager.getInstance().setLocais(listFactory.listLocais());
        WorldBuilderMenuManager.getInstance().setNacoes(listFactory.listNacoes());
        WorldBuilderMenuManager.getInstance().setTerrenos(cenario.getTerrenos());
        mapMenuManager = new MapMenuManager();
        mapMenuManager.doCanvasReset(mapaManager.getMapMaxSize(listFactory.listLocais().values()));
        mapMenuManager.setLocais(listFactory.listLocais());
        mapMenuManager.setNacoes(listFactory.listNacoes());
        mapMenuManager.setTerrenos(cenario.getTerrenos());
        rmActive = getMapMenuManager().getMainMenu();

    }

    private ImageIcon printActionsOnMap() {
        return new ImageIcon(mapaManager.printActionsOnMap(listFactory.listLocais().values(), listFactory.listPersonagens(), getJogadorAtivo()));
    }

    public ImageIcon printMapaGeral() {
        return new ImageIcon(mapaManager.printMapaGeral(listFactory.listLocais().values(), listFactory.listPersonagens(), getJogadorAtivo()));
    }

    private ImageIcon refreshMapaGeral() {
        return new ImageIcon(mapaManager.redrawMapaGeral(listFactory.listLocais().values(), listFactory.listPersonagens(), getJogadorAtivo()));
    }

    public BufferedImage getMap() {
        return mapaManager.printMapaGeral(listFactory.listLocais().values(), listFactory.listPersonagens(), getJogadorAtivo());
    }

    public MainMapaGui getTabGui() {
        return tabGui;
    }

    public void setTabGui(MainMapaGui tabGui) {
        this.tabGui = tabGui;
    }

    /*
     * Define posicao para a tag de localizacao no mapa. (Hex duplo e vermelho...)
     */
    public void printTag(Local local) {
        hideRadialMenu();
        this.getTabGui().clearMovementTags();
        if (local == null) {
            this.getTabGui().hidefocusTag();
        } else {
            int[] pos = mapaManager.doCoordToPosition(local);
            this.getTabGui().setFocusTag(pos[0], pos[1]);
            updateLocal(local);
        }
    }

    public void remMovementTag() {
        this.getTabGui().clearMovementTags();
    }

    /** Map a click in the (zoomed) display space back to 1x map coords for hex lookup. Drag-to-scroll
     *  stays in display space and must NOT use this. */
    private Point scaleClick(Point p) {
        double z = getTabGui().getZoom();
        if (z == 1.0) {
            return p;
        }
        // Round (not truncate) the zoom-divided point: doPositionToCoord truncates again on the 60/45
        // grid pitch, so a second truncation here biased boundary clicks toward the up-left neighbour
        // at fractional zoom (KI-002). Rounding centres the error so the hit matches what was clicked.
        return new Point((int) Math.round(p.x / z), (int) Math.round(p.y / z));
    }

    public void addMovementTag(MovimentoExercito movimento) {
        //calcula posicao no grafico a partir da coordenada do local
        int[] pos = mapaManager.doCoordToPosition(movimento.getDestino());
        //desenha a movimentacao
        this.getTabGui().addMovementTagCumulative(pos[0], pos[1], movimento.getCustoMovimento(), movimento.getLimiteMovimento());
    }

    public void addMovementTag(HashMap<Local, ArmyPath> movimentos) {
        for (Local local : movimentos.keySet()) {
            //calcula posicao no grafico a partir da coordenada do local
            int[] pos = mapaManager.doCoordToPosition(local);
            //desenha a movimentacao
            this.getTabGui().addMovementTagNonCumulative(pos[0], pos[1], movimentos.get(local), 12);
        }
    }

    public void addMovementTagRange(HashMap<Local, Integer> movimentos) {
        for (Local local : movimentos.keySet()) {
            //calcula posicao no grafico a partir da coordenada do local
            int[] pos = mapaManager.doCoordToPosition(local);
            //desenha a movimentacao
            this.getTabGui().addMovementTagNonCumulative(pos[0], pos[1], movimentos.get(local), 12);
        }
    }

    /*
     * Define posicao para a tag de movimento de personagem. (???)
     */
    public void addMovementTag(MovimentoPersonagem movimento) {
        //calcula posicao no grafico a partir da coordenada do local
        int[] pos = mapaManager.doCoordToPosition(movimento.getDestino());
        //limpa tags existentes
        this.getTabGui().clearMovementTags();
        //desenha a movimentacao
        this.getTabGui().addMovementTagCumulative(pos[0], pos[1], movimento.getDistancia(), movimento.getLimiteMovimento());
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        GenericoComboObject par;
        if (event.getSource() instanceof JComboBox && event.getStateChange() == ItemEvent.SELECTED) {
            JComboBox cb = (JComboBox) event.getSource();
            try {
                if ("Coordenadas".equals(cb.getActionCommand())) {
                    par = (GenericoComboObject) cb.getModel().getSelectedItem();
                    addMovementTag((MovimentoPersonagem) par.getObject());
                }
            } catch (ClassCastException ex) {
            }
        }
    }

    /**
     * Shows a new popup for each mouse right click.
     *
     * @param local
     */
    private void showLocalInfo(Local local) {
        this.printTag(local);
        final String title = String.format(labels.getString("LOCAL.TITLE"), local.getCoordenadas());
        String text = LocalConverter.getInfo(local);
        if (SettingsManager.getInstance().getConfig("KeepPopupOpen", "0").equals("0") && hexInfo != null) {
            hexInfo.setText(text);
            hexInfo.setTitle(title);
            hexInfo.setVisible(true);
        } else {
            hexInfo = ComponentFactory.showDialogPopup(title, text, this.getTabGui());
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        try {
            final Local local = mapaManager.doPositionToCoord(scaleClick(event.getPoint()));
            if (cityPickCallback != null && SwingUtilities.isLeftMouseButton(event)) {
                //"pick city on map" mode: consume this left-click, hand the hex to the picker, disarm.
                final Consumer<Local> cb = cityPickCallback;
                cityPickCallback = null;
                cb.accept(local);
                return;
            }
            if (SwingUtilities.isRightMouseButton(event) && !SettingsManager.getInstance().isRadialMenu()) {
                showLocalInfo(local);
            } else if (SwingUtilities.isRightMouseButton(event) && SettingsManager.getInstance().isRadialMenu()) {
                showActiveRadialMenu(local);
            } else {
                //volta o menu ao padrao
                if (SettingsManager.getInstance().isWorldBuilder()) {
                    WorldBuilderMenuManager.getInstance().getListener().setCurrentAction(RadialActions.NONE);
                    hideRadialMenu();
                    rmActive = WorldBuilderMenuManager.getInstance().getRmWorldBuilder();
                } else if (rmActive == null) {
                    rmActive = getMapMenuManager().getMainMenu();
                }
                this.printTag(local);
            }
//            System.out.println(event.getPoint() + " => " + local.getCoordenadas());
        } catch (NullPointerException ex) {
            System.out.println(event.getPoint() + " => null");
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    /** Arm one-shot "pick a city on the map": the next left-click on the map calls back with the hex. */
    public void armCityPick(Consumer<Local> callback) {
        this.cityPickCallback = callback;
    }

    /** Cancel an armed city pick (e.g. the picker's map toggle was switched off). */
    public void cancelCityPick() {
        this.cityPickCallback = null;
    }

    /**
     * @return the local
     */
    public Local getLocal() {
        return localAtual;
    }

    /**
     * @param local the local to set
     */
    private void setLocal(Local local) {
        this.localAtual = local;
    }

    private void updateLocal(Local local) {
        setLocal(local);
        for (JTable table : tables) {
            table.repaint();
        }
        doHexViewUpdate();
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_CLICK, local);
    }

    public void addTableLocal(JTable table) {
        tables.add(table);
    }

    public void printTagArmyRange(Exercito exercito) {
        final Local local = exercitoFacade.getLocal(exercito);
        boolean isAgua = localFacade.isAgua(local);
        MovimentoExercito movEx = new MovimentoExercito();
        if (isAgua) {
            movEx.addTropasAll(exercitoFacade.getTipoTropasAgua(exercito));
        } else {
            movEx.addTropasAll(exercitoFacade.getTipoTropasTerra(exercito));
        }
        movEx.setComida(exercitoFacade.isComida(exercito));
        //set starting point
        movEx.setOrigem(exercitoFacade.getLocal(exercito));
        movEx.setPorAgua(isAgua);
        movEx.setLimiteMovimento(WorldFacadeCounselor.getInstance().getCenarioArmyMoveMaxPoints()); //exercito.getMovPontos()
        movEx.setEvasivo(false);

        HashMap<Local, ArmyPath> range = exercitoFacade.calculateArmyRangeHexes(listFactory.listLocais(), movEx);
        range.remove(local);
        addMovementTag(range);
    }

    public void printTagRange(Local local, int range) {
        HashMap<Local, Integer> localInRange;
        localInRange = localFacade.getLocalRange(local, range, false, listFactory.listLocais());
        localInRange.remove(local);
        addMovementTagRange(localInRange);
    }

    /**
     * Draw a thick border around every hex within {@code range} of {@code origem}, so a player filling a
     * hex parameter can see how far the order reaches without reading the combo. Unbounded ranges
     * (COORDENADA*, or the ALL checkbox) draw nothing - the whole map is in range and a border round the
     * map edge tells nobody anything.
     * <p>
     * The hex set comes from {@code getLocalRange}, i.e. the SAME distance function
     * {@code SubTabCoordenadas} filters its combo with, so the border can never disagree with the
     * picker's own idea of what is in range. It is a pure DISTANCE ring: a hex inside the border may
     * still be rejected on terrain grounds (water, cities-only), exactly as the combo already rejects it.
     * <p>
     * Cached on (origem, range): the union is a few hundred polygon merges and the parameter panel is
     * rebuilt on every actor/order click.
     */
    public void showRangeOutline(Object owner, Local origem, int range) {
        if (origem == null || range <= 0 || range >= RANGE_UNBOUNDED || getTabGui() == null) {
            // an unbounded picker owns no border, so it must not take down somebody else's: an order
            // carrying both a bounded and an unbounded coordinate would otherwise blank the map
            clearRangeOutline(owner);
            return;
        }
        rangeOutlineOwner = owner;
        if (origem.equals(rangeOutlineOrigem) && range == rangeOutlineRange && rangeOutlineShape != null) {
            getTabGui().setRangeOutlineColor(getRangeOutlineColor());
            getTabGui().setRangeOutline(rangeOutlineShape); // same ask, reuse the built shape
            return;
        }
        final Collection<Local> inRange
                = localFacade.getLocalRange(origem, range, false, listFactory.listLocais()).keySet();
        rangeOutlineShape = HexRangeOutline.build(inRange);
        rangeOutlineOrigem = origem;
        rangeOutlineRange = range;
        getTabGui().setRangeOutlineColor(getRangeOutlineColor());
        getTabGui().setRangeOutline(rangeOutlineShape);
    }

    /**
     * Border colour from properties.config {@code ColorHexRange} (6-hex, no '#'), defaulting to the white
     * the feature shipped with. The hex is validated here rather than leaning on ColorFactory, which
     * answers null for an empty string but DARK_GRAY for anything unparseable - a typo in a hand-edited
     * config would otherwise silently hand the player a grey border and no clue why.
     * <p>
     * Public and static because the Settings swatch and its colour picker must resolve it the SAME way:
     * reading the raw config in those two places instead showed a hand-edited {@code #FFAA00} as dark
     * grey in the dialog while the map drew white.
     */
    public static Color getRangeOutlineColor() {
        final String hex = SettingsManager.getInstance().getConfig("ColorHexRange", RANGE_COLOR_DEFAULT_HEX);
        if (hex == null || !hex.matches("(?i)[0-9a-f]{6}")) {
            return ScaledMapIcon.RANGE_COLOR_DEFAULT;
        }
        return ColorFactory.getColorBd(hex);
    }

    /**
     * Clear the border only if {@code owner} is still the picker that put it up.
     * <p>
     * Ownership is by IDENTITY, not by (origem, range). A rebuilt parameter panel can add the incoming
     * picker before Swing tears the outgoing one down - measured, both orders happen: {@code removeAll()}
     * then {@code add()} gives {@code -old +new}, while {@code add()} then {@code remove()} gives
     * {@code +new -old}. In that second order a (origem, range) test MATCHES whenever the two pickers
     * share them, which is the ordinary case of swapping between two range-8 orders on the same actor,
     * so the outgoing widget would wipe the border the incoming one just drew.
     */
    public void clearRangeOutline(Object owner) {
        if (owner != null && owner == rangeOutlineOwner) {
            clearRangeOutline();
        }
    }

    /** Remove the hex-range border (order/actor changed, picker gone, map cleared). */
    public void clearRangeOutline() {
        rangeOutlineOwner = null;
        rangeOutlineOrigem = null;
        rangeOutlineRange = 0;
        rangeOutlineShape = null;
        if (getTabGui() != null) {
            getTabGui().setRangeOutline(null);
        }
    }

    @Override
    public void receiveDispatch(int msgName, Local local) {
        if (msgName == DispatchManager.LOCAL_MAP_REDRAW) {
            tabGui.doMapa(this.printMapaGeral());
        }
    }

    @Override
    public void receiveDispatch(int msgName) {
        switch (msgName) {
            case DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES:
                tabGui.doMapa(this.refreshMapaGeral());
                // the map colour buttons in Settings all fire this; re-read ours so a colour change
                // applies at once instead of waiting for the next parameter-panel rebuild
                tabGui.setRangeOutlineColor(getRangeOutlineColor());
                break;
            case DispatchManager.ACTIONS_MAP_REDRAW:
                tabGui.doActionsOnMap(this.printActionsOnMap());
//                if (SettingsManager.getInstance().isConfig("drawPcPath", "1", "1") || SettingsManager.getInstance().isConfig("drawPcPath", "3", "1")) {
//                    tabGui.doActionsOnMap(this.printActionsOnMap());
//                } else {
//                    tabGui.doActionsOnMapHide();
//                }
                break;
            case DispatchManager.LOCAL_MAP_REDRAW_TAG:
                tabGui.setTag();
                break;
            case DispatchManager.GUI_STATUS_PERSIST:
                doConfigHexView();
                break;
            default:
                break;
        }
    }

    @Override
    public void receiveDispatch(int msgName, Local local, int range) {
        if (msgName == DispatchManager.LOCAL_RANGE_CLICK) {
            printTagRange(local, range);
        }
    }

    private void hideRadialMenu() {
        try {
            rmActive.doHide();
        } catch (NullPointerException ex) {
        }
    }

    public void showActiveRadialMenu(Local local) {
        if (rmActive == null && SettingsManager.getInstance().isWorldBuilder()) {
            rmActive = WorldBuilderMenuManager.getInstance().getRmWorldBuilder();
        } else if (rmActive == null) {
            rmActive = getMapMenuManager().getMainMenu();
        }
        rmActive.setHierarchyAncestor(this);

        this.printTag(local);
        tabGui.addRadialMenu(rmActive);
        rmActive.setLocalMenu(local);
        int[] pos = mapaManager.doCoordToPosition(local);
        double z = getTabGui().getZoom(); // place the menu over the hex in the zoomed display space
        rmActive.doActivate(new Point((int) Math.round(pos[0] * z), (int) Math.round(pos[1] * z)));
    }

    public void showDirectionsCentreMenu(Local local, WorldBuilderRadialActions action) {
        rmActive.doHide();
        rmActive = WorldBuilderMenuManager.getInstance().getRmDirectionsCentre(action);
        showActiveRadialMenu(local);
    }

    private void doConfigHexView() {
        //check status of popups
        if (SettingsManager.getInstance().isConfig("GuiHexViewDetachedStatus", IPopupTabGui.POPUP_FLOATING, IPopupTabGui.POPUP_HIDDEN)) {
            doCreateHexView();
        }
    }

    public void doHexViewToggle() {
        if (hexView == null) {
            doCreateHexView();
            SettingsManager.getInstance().setConfigAndSaveToFile("GuiHexViewDetachedStatus", IPopupTabGui.POPUP_FLOATING);
        } else if (hexView.isVisible()) {
            //hide
            hexView.setVisible(!hexView.isVisible());
            SettingsManager.getInstance().setConfigAndSaveToFile("GuiHexViewDetachedStatus", IPopupTabGui.POPUP_HIDDEN);
        } else {
            //display
            hexView.setVisible(!hexView.isVisible());
            SettingsManager.getInstance().setConfigAndSaveToFile("GuiHexViewDetachedStatus", IPopupTabGui.POPUP_FLOATING);
        }
    }

    private void doCreateHexView() {
        //create on first time
        if (hexView == null) {
            hexView = ComponentFactory.showDialogHexView(this.getTabGui());
        }
        doHexViewUpdate();
    }

    private void doHexViewUpdate() {
        if (hexView == null) {
            return;
        }
        final String title = String.format(labels.getString("LOCAL.TITLE"), getLocal().getCoordenadas());
        final String text = LocalConverter.getInfo(getLocal());
        hexView.setText(text);
        hexView.setTitle(title);
    }

    public MapaManager getMapaManager() {
        return mapaManager;
    }

    public void setMapaManager(MapaManager mapaManager) {
        this.mapaManager = mapaManager;
    }

    /**
     * @return the mapMenuManager
     */
    public MapMenuManager getMapMenuManager() {
        return mapMenuManager;
    }

    /**
     * @param mapMenuManager the mapMenuManager to set
     */
    public void setMapMenuManager(MapMenuManager mapMenuManager) {
        this.mapMenuManager = mapMenuManager;
    }

    /**
     * @return the jogadorAtivo
     */
    public Jogador getJogadorAtivo() {
        return jogadorAtivo;
    }

    /**
     * @param jogadorAtivo the jogadorAtivo to set
     */
    public void setJogadorAtivo(Jogador jogadorAtivo) {
        this.jogadorAtivo = jogadorAtivo;
    }

}
