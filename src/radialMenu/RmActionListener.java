/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu;

import control.services.LocalConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.accessories.ArmyMoveSimulator;
import gui.accessories.BattleCasualtySimulator;
import gui.accessories.BattleCasualtySimulatorNew;
import gui.accessories.TroopsCasualtiesList;
import gui.components.DialogTextArea;
import gui.services.ComponentFactory;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.SortedMap;
import javax.swing.SwingUtilities;
import model.Local;
import model.Nacao;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import radialMenu.mapmenu.MapMenuRadialActions;
import static radialMenu.mapmenu.MapMenuRadialActions.ARMY_MOVEMENT_SIMULATOR;
import static radialMenu.mapmenu.MapMenuRadialActions.LOCAL_INFO;
import radialMenu.worldBuilder.WorldBuilderRadialActions;

/**
 *
 * @author jmoura
 */
public class RmActionListener extends ControlBase implements Serializable, MouseListener {

    private static final Log log = LogFactory.getLog(RmActionListener.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private Local localMenu;
    private Enum currentAction = RadialActions.NONE;
    private final RadialEvents events = new RadialEvents();
    private RadialButton rbActive;
    private DialogTextArea hexInfo;

    public RmActionListener(SortedMap<String, Local> locais) {
        events.setLocais(locais);
        registerDispatchManagerForMsg(DispatchManager.LOCAL_MAP_CLICK);
    }

    @Override
    public void receiveDispatch(int msgName, Local aLocal) {
        if (msgName == DispatchManager.LOCAL_MAP_CLICK) {
            localMenu = aLocal;
        }
    }

    public Enum getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(Enum action) {
        currentAction = action;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getSource() instanceof RadialButton) {
            RadialButton rb = (RadialButton) event.getSource();
            if (SwingUtilities.isLeftMouseButton(event)) {
                actionPerformed(rb);
            } else if (SwingUtilities.isRightMouseButton(event)) {
                try {
                    rb.getHierarchyAncestor().showActiveRadialMenu(rb.getLocal());
                } catch (NullPointerException e) {
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        if (event.getSource() instanceof RadialButton) {
            RadialButton rb = (RadialButton) event.getSource();
            if (rb.hasSubMenu()) {
                openSubMenu(rb);
            } else if (!rb.isSubMenu()) {
                closeSubMenu(rbActive);
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private void actionPerformed(RadialButton rb) {
        if (rb.hasSubMenu()) {
            //mouse entered
            return;
        }
        final String actionCommand = rb.getActionCommand();
        log.debug("Current Action is: " + currentAction + " / " + actionCommand);
        log.debug("Next Action is: " + rb.getRadialAction().toString() + " / " + actionCommand);
        setCurrentAction(rb.getRadialAction());
        try {
            if (currentAction instanceof WorldBuilderRadialActions) {
                doWorldBuilderActions(rb, actionCommand, (WorldBuilderRadialActions) currentAction);
            } else if (currentAction instanceof MapMenuRadialActions) {
                doMapMenuActions(rb, actionCommand, (MapMenuRadialActions) currentAction);
            }
        } catch (NullPointerException ex) {
            log.error(ex);
        }
    }

    private void openSubMenu(RadialButton rb) {
        this.rbActive = rb;
        rb.updateButtons();
    }

    private void closeSubMenu(RadialButton rb) {
        this.rbActive = null;
        try {
            rb.closeSubMenu();
        } catch (NullPointerException ex) {
        }
    }

    private void doWorldBuilderActions(RadialButton rb, final String actionCommand, WorldBuilderRadialActions action) {
        switch (action) {
            case MAIN_ROAD:
                rb.getHierarchyAncestor().showDirectionsCentreMenu(rb.getLocal(), WorldBuilderRadialActions.BUILD_ROADS);
                break;
            case MAIN_RIVER:
                rb.getHierarchyAncestor().showDirectionsCentreMenu(rb.getLocal(), WorldBuilderRadialActions.BUILD_RIVER);
                break;
            case MAIN_STREAM:
                rb.getHierarchyAncestor().showDirectionsCentreMenu(rb.getLocal(), WorldBuilderRadialActions.BUILD_STREAM);
                break;
            case MAIN_BRIDGE:
                rb.getHierarchyAncestor().showDirectionsCentreMenu(rb.getLocal(), WorldBuilderRadialActions.BUILD_BRIDGES);
                break;
            case MAIN_LANDING:
                rb.getHierarchyAncestor().showDirectionsCentreMenu(rb.getLocal(), WorldBuilderRadialActions.BUILD_LANDING);
                break;
            case MAIN_SHALLOW:
                rb.getHierarchyAncestor().showDirectionsCentreMenu(rb.getLocal(), WorldBuilderRadialActions.BUILD_SHALLOW);
                break;
            case BUILD_ROADS:
                events.toogleRoad(localMenu, actionCommand);
                break;
            case BUILD_RIVER:
                events.toogleRiver(localMenu, actionCommand);
                break;
            case BUILD_STREAM:
                events.toogleStream(localMenu, actionCommand);
                break;
            case BUILD_BRIDGES:
                events.toogleBridge(localMenu, actionCommand);
                break;
            case BUILD_LANDING:
                events.toogleLanding(localMenu, actionCommand);
                break;
            case BUILD_SHALLOW:
                events.toogleShalow(localMenu, actionCommand);
                break;
            case CHANGE_TERRAIN:
                events.changeTerrain(localMenu, (Terreno) rb.getBaseModel());
                break;
            case CHANGE_CITY_NATION:
                events.changeCityNation(localMenu, (Nacao) rb.getBaseModel());
                break;
            case CHANGE_CLIMATE:
                log.info("muda clima: " + actionCommand);
                break;
            default:
                log.info("default: " + actionCommand);
        }
    }

    private void doMapMenuActions(RadialButton rb, String actionCommand, MapMenuRadialActions action) {
        switch (action) {
            case COMBAT_SIMULATOR:
                createBattleSim(rb);
                break;
            case ARMY_MOVEMENT_SIMULATOR:
                createArmyMovSim(rb, false);
                break;
            case NAVY_MOVEMENT_SIMULATOR:
                createArmyMovSim(rb, true);
                break;
            case LOCAL_INFO:
                showLocalInfo(rb);
                break;
            case LOCAL_CASUALTIES:
                showLocalCasualties(rb);
                break;
            case RANGE_PLOT:
                showRangePlot(rb);
                break;
            default:
                log.info("Radial Action not implemented yet: " + actionCommand);
        }
    }

    private void showLocalInfo(RadialButton rb) {
        final Local local = rb.getLocal();
        final String title = String.format(labels.getString("LOCAL.TITLE"), local.getCoordenadas());
        String text = LocalConverter.getInfo(local);

        if (SettingsManager.getInstance().getConfig("KeepPopupOpen", "0").equals("0") && hexInfo != null) {
            hexInfo.setText(text);
            hexInfo.setTitle(title);
            hexInfo.setVisible(true);
        } else {
            hexInfo = ComponentFactory.showDialogPopup(title, text, rb);
        }
    }

    private void showLocalCasualties(RadialButton rb) {
        TroopsCasualtiesList casualtiesSim = new TroopsCasualtiesList(rb.getLocal());
        casualtiesSim.setLocationRelativeTo(rb);
        casualtiesSim.setVisible(true);
    }

    private void createBattleSim(RadialButton rb) {
        final boolean newBattleSim = SettingsManager.getInstance().getConfig("BatleSimNew", "0").equalsIgnoreCase("1");
        if (newBattleSim) {
            BattleCasualtySimulatorNew battleSim = new BattleCasualtySimulatorNew(rb.getLocal());
            battleSim.setLocationRelativeTo(rb);
            battleSim.setVisible(true);
            /*
            BattleSimFx battleSim = new BattleSimFx();
            battleSim.start();
            create a blank new FX window.
            start over Counselor (setLocationRelativeTo)?
            list armies.
            go from there.
             */
        } else {
            BattleCasualtySimulator battleSim = new BattleCasualtySimulator(rb.getLocal());
            battleSim.setLocationRelativeTo(rb);
            battleSim.setVisible(true);
        }
    }

    private void createArmyMovSim(RadialButton rb, boolean water) {
        ArmyMoveSimulator marchSim = new ArmyMoveSimulator(rb.getLocal(), water);
        marchSim.setLocationRelativeTo(rb);
        marchSim.setVisible(true);
    }

    private void showRangePlot(RadialButton rb) {
        final Local local = rb.getLocal();
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_RANGE_CLICK, local, 20);
    }
}
