/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu.worldBuilder;

import business.ImageFactory;
import java.awt.Point;
import java.io.Serializable;
import java.util.SortedMap;
import model.Local;
import model.Nacao;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import radialMenu.RadialActionsDirections;
import radialMenu.RadialButton;
import radialMenu.RadialMenu;
import radialMenu.RmActionListener;

/**
 *
 * @author jmoura
 */
public class WorldBuilderMenuManager implements Serializable {

    private static final Log log = LogFactory.getLog(WorldBuilderMenuManager.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static WorldBuilderMenuManager instance;
    private static RmActionListener listener;
    private final ImageFactory imageFactory = new ImageFactory();
    private RadialMenu rmWorldBuilder;
    private RadialMenu rmDirectionsCentre;
    private SortedMap<String, Local> locais;
    private SortedMap<String, Nacao> nacoes;
    private SortedMap<String, Terreno> terrenos;
    private Point canvasSize;

    public synchronized static WorldBuilderMenuManager getInstance() {
        if (WorldBuilderMenuManager.instance == null) {
            WorldBuilderMenuManager.instance = new WorldBuilderMenuManager();
        }
        return WorldBuilderMenuManager.instance;
    }

    /**
     * @return the listener
     */
    public RmActionListener getListener() {
        if (listener == null) {
            listener = new RmActionListener(locais);
        }
        return listener;
    }

    public RadialMenu getRmWorldBuilder() {
        if (rmWorldBuilder == null) {
            //basic config
            rmWorldBuilder = new RadialMenu(getCanvasSize());
            rmWorldBuilder.setLocais(locais);
            rmWorldBuilder.setOptionListener(getListener());
            RadialButton menu;

            //add terrain
            menu = doConfigOption(WorldBuilderRadialActions.MAIN_TERRAIN);
            doCreateTerrainSubMenu(menu);
            //add city
            menu = doConfigOption(WorldBuilderRadialActions.MAIN_CITY);
            doCreateNacaoSubMenu(menu);

            //add decorations
            for (WorldBuilderRadialActions rra : WorldBuilderRadialActions.WorldBuilderMainMenu) {
                doConfigOption(rra);
            }
        }
        return rmWorldBuilder;
    }

    public RadialMenu getRmDirectionsCentre(WorldBuilderRadialActions action) {
        final int initAngle = 120;
        if (rmDirectionsCentre == null) {
            rmDirectionsCentre = new RadialMenu(getCanvasSize());
            rmDirectionsCentre.setLocais(locais);
            rmDirectionsCentre.setOptionListener(getListener());
            int opt = -1;
            for (RadialActionsDirections ra : RadialActionsDirections.DirectionsMenu) {
                RadialButton menu = new RadialButton(getListener().getCurrentAction());
                menu.setText(labels.getString(ra.getLabel()));
                menu.setIcon(imageFactory.getArrow(initAngle + 180 + 60 * opt));
                menu.setRolloverIcon(imageFactory.getYellowBall());
                menu.setActionCommand(ra.getActionCommand());
                menu.setRadialAction(action);
                menu.addMouseListener(rmWorldBuilder.getOptionListener());
                rmDirectionsCentre.addRootMenuItem(menu);
                opt++;
            }
        } else {
            for (RadialButton menu : rmDirectionsCentre.getRootMenu()) {
                menu.setRadialAction(action);
            }
        }
        rmDirectionsCentre.setDirection(true);
        rmDirectionsCentre.setInitAngle(initAngle);
        return rmDirectionsCentre;
    }

    /**
     * @param locais the locais to set
     */
    public void setLocais(SortedMap<String, Local> locais) {
        this.locais = locais;
    }

    public void setNacoes(SortedMap<String, Nacao> nacoes) {
        this.nacoes = nacoes;
    }

    public void setTerrenos(SortedMap<String, Terreno> terrenos) {
        this.terrenos = terrenos;
    }

    public void doCanvasReset(Point mapMaxSize) {
        canvasSize = mapMaxSize;
        rmWorldBuilder = null;
        rmDirectionsCentre = null;
        listener = null;
    }

    /**
     * @return the canvasSize
     */
    private Point getCanvasSize() {
        return canvasSize;
    }

    private void doCreateTerrainSubMenu(RadialButton menu) {
        //create terrain submenu
        for (Terreno terreno : terrenos.values()) {
            RadialButton subMenu = new RadialButton(WorldBuilderRadialActions.CHANGE_TERRAIN);
            subMenu.setText(terreno.getNome());
            subMenu.setIcon(imageFactory.getBlueBall());
            subMenu.setRolloverIcon(imageFactory.getYellowBall());
            subMenu.setActionCommand(terreno.getCodigo());
            subMenu.addMouseListener(rmWorldBuilder.getOptionListener());
            subMenu.setBaseItem(terreno);
            subMenu.setVisible(false);
            subMenu.setSubMenu(true);
            menu.addSubMenuItem(subMenu);
            rmWorldBuilder.add(subMenu);
        }
    }

    private void doCreateNacaoSubMenu(RadialButton menu) {
        //create terrain submenu
        for (Nacao terreno : nacoes.values()) {
            RadialButton subMenu = new RadialButton(WorldBuilderRadialActions.CHANGE_CITY_NATION);
            subMenu.setText(terreno.getNome());
            subMenu.setIcon(imageFactory.getBlueBall());
            subMenu.setRolloverIcon(imageFactory.getYellowBall());
            subMenu.setActionCommand(terreno.getCodigo());
            subMenu.addMouseListener(rmWorldBuilder.getOptionListener());
            subMenu.setBaseItem(terreno);
            subMenu.setVisible(false);
            subMenu.setSubMenu(true);
            menu.addSubMenuItem(subMenu);
            rmWorldBuilder.add(subMenu);
        }
    }

    private RadialButton doConfigOption(WorldBuilderRadialActions ra) {
        RadialButton menu = new RadialButton(ra);
        menu.setText(labels.getString(ra.getLabel()));
        menu.setIcon(imageFactory.getBlueBall());
        menu.setRolloverIcon(imageFactory.getYellowBall());
        menu.setActionCommand(ra.toString());
        menu.addMouseListener(rmWorldBuilder.getOptionListener());
        rmWorldBuilder.addRootMenuItem(menu);
        return menu;
    }
}
