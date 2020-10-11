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
import gui.services.ComponentFactory;
import gui.services.IPopupTabGui;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
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
    private final List<JTable> tables = new ArrayList<JTable>();
    private RadialMenu rmActive;
    private MapMenuManager mapMenuManager;
    private DialogHexView hexView;
    private Jogador jogadorAtivo;

    public MapaControler(JPanel form) {
        initialize(form);
    }

    public void initialize(JPanel form) {
        this.jogadorAtivo = WorldFacadeCounselor.getInstance().getPartida().getJogadorAtivo();
        listFactory = new ListFactory();
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
            final Local local = mapaManager.doPositionToCoord(event.getPoint());
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
        localInRange = localFacade.getLocalRange(local, range, true, listFactory.listLocais());
        localInRange.remove(local);
        addMovementTagRange(localInRange);
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
                break;
            case DispatchManager.ACTIONS_MAP_REDRAW:
                if (!SettingsManager.getInstance().isConfig("drawPcPath", "1", "1") && !SettingsManager.getInstance().isConfig("drawPcPath", "3", "1")) {
                    tabGui.doActionsOnMapHide();
                } else {
                    tabGui.doActionsOnMap(this.printActionsOnMap());
                }
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
        rmActive.doActivate(new Point(pos[0], pos[1]));
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
