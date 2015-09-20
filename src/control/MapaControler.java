/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.SysApoio;
import baseLib.SysProperties;
import business.ArmyPath;
import business.MapaManager;
import business.MovimentoExercito;
import business.MovimentoPersonagem;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import control.services.LocalConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.MainMapaGui;
import gui.components.DialogTextArea;
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
import model.Exercito;
import model.Jogador;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import persistence.local.WorldManager;
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
    private static final LocalFacade localFacade = new LocalFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static MapaManager mapaManager; //tem que criar depois para incluir o form
    private static final ListFactory listFactory = new ListFactory();
    private MainMapaGui tabGui;
    private DialogTextArea hexInfo;
    private Local localAtual;
    private final List<JTable> tables = new ArrayList<JTable>();
    private RadialMenu rmActive;
    private final MapMenuManager mapMenuManager;

    public MapaControler(JPanel form) {
        mapaManager = new MapaManager(WorldFacadeCounselor.getInstance().getCenario(), form);
        registerDispatchManagerForMsg(DispatchManager.LOCAL_MAP_REDRAW);
        registerDispatchManagerForMsg(DispatchManager.LOCAL_RANGE_CLICK);
        //inicialize locais
        WorldBuilderMenuManager.getInstance().doCanvasReset(mapaManager.getMapMaxSize(listFactory.listLocais().values()));
        WorldBuilderMenuManager.getInstance().setLocais(listFactory.listLocais());
        WorldBuilderMenuManager.getInstance().setNacoes(listFactory.listNacoes());
        WorldBuilderMenuManager.getInstance().setTerrenos(WorldFacadeCounselor.getInstance().getCenario().getTerrenos());
        mapMenuManager = new MapMenuManager();
        mapMenuManager.doCanvasReset(mapaManager.getMapMaxSize(listFactory.listLocais().values()));
        mapMenuManager.setLocais(listFactory.listLocais());
        mapMenuManager.setNacoes(listFactory.listNacoes());
        mapMenuManager.setTerrenos(WorldFacadeCounselor.getInstance().getCenario().getTerrenos());
    }

    public ImageIcon getTagImage() {
        return mapaManager.getTagImage();
    }

    public ImageIcon printMapaGeral() {
        final Jogador jogadorAtivo = WorldManager.getInstance().getPartida().getJogadorAtivo();
        return new ImageIcon(mapaManager.printMapaGeral(listFactory.listLocais().values(), listFactory.listPersonagens(), jogadorAtivo));
    }

    public BufferedImage getMap() {
        final Jogador jogadorAtivo = WorldManager.getInstance().getPartida().getJogadorAtivo();
        return mapaManager.printMapaGeral(listFactory.listLocais().values(), listFactory.listPersonagens(), jogadorAtivo);
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
        if (SysProperties.getProps("KeepPopupOpen", "0").equals("0") && hexInfo != null) {
            hexInfo.setText(text);
            hexInfo.setTitle(title);
            hexInfo.setVisible(true);
        } else {
            hexInfo = SysApoio.showDialogPopup(title, text, this.getTabGui());
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        try {
            final Local local = mapaManager.doPositionToCoord(event.getPoint(), listFactory.listLocais());
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
                    rmActive = mapMenuManager.getMainMenu();
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
        movEx.setLimiteMovimento(14); //exercito.getMovPontos()
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
            rmActive = mapMenuManager.getMainMenu();
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
}
