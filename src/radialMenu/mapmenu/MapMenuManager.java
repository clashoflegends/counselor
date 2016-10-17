/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu.mapmenu;

import business.ImageFactory;
import java.awt.Point;
import java.io.Serializable;
import java.util.SortedMap;
import model.Local;
import model.Nacao;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import radialMenu.*;

/**
 *
 * @author jmoura
 */
public class MapMenuManager implements Serializable {

    private static final Log log = LogFactory.getLog(MapMenuManager.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static RmActionListener listener;
    private final ImageFactory imageFactory = new ImageFactory();
    private RadialMenu rmWorldBuilder;
    private SortedMap<String, Local> locais;
    private SortedMap<String, Nacao> nacoes;
    private SortedMap<String, Terreno> terrenos;
    private Point canvasSize;

//    public synchronized static MapMenuManager getInstance() {
//        if (MapMenuManager.instance == null) {
//            MapMenuManager.instance = new MapMenuManager();
//        }
//        return MapMenuManager.instance;
//    }
    /**
     * @return the listener
     */
    public RmActionListener getListener() {
        if (listener == null) {
            listener = new RmActionListener(locais);
        }
        return listener;
    }

    public RadialMenu getMainMenu() {
        if (rmWorldBuilder == null) {
            //basic config
            rmWorldBuilder = new RadialMenu(getCanvasSize());
            rmWorldBuilder.setLocais(locais);
            rmWorldBuilder.setOptionListener(getListener());
            RadialButton menu;

            menu = doConfigOption(MapMenuRadialActions.LOCAL_INFO);
            menu = doConfigOption(MapMenuRadialActions.RANGE_PLOT);
            menu = doConfigOption(MapMenuRadialActions.COMBAT_SIMULATOR);
            menu = doConfigOption(MapMenuRadialActions.LOCAL_CASUALTIES);
            menu = doConfigOption(MapMenuRadialActions.ARMY_MOVEMENT_SIMULATOR);
            menu = doConfigOption(MapMenuRadialActions.NAVY_MOVEMENT_SIMULATOR);
//            doCreateTerrainSubMenu(menu);
        }
        return rmWorldBuilder;
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
        this.canvasSize = mapMaxSize;
        rmWorldBuilder = null;
        listener = null;
    }

    /**
     * @return the canvasSize
     */
    private Point getCanvasSize() {
        return canvasSize;
    }

//    private void doCreateTerrainSubMenu(RadialButton menu) {
//        //create terrain submenu
//        for (Terreno terreno : terrenos.values()) {
//            RadialButton subMenu = new RadialButton(MapMenuRadialActions.COMBAT_SIMULATOR);
//            subMenu.setText(terreno.getNome());
//            subMenu.setIcon(imageFactory.getBlueBall());
//            subMenu.setRolloverIcon(imageFactory.getYellowBall());
//            subMenu.setActionCommand(terreno.getCodigo());
//            subMenu.addMouseListener(rmWorldBuilder.getOptionListener());
//            subMenu.setBaseItem(terreno);
//            subMenu.setVisible(false);
//            subMenu.setSubMenu(true);
//            menu.addSubMenuItem(subMenu);
//            rmWorldBuilder.add(subMenu);
//        }
//    }

    private RadialButton doConfigOption(MapMenuRadialActions ra) {
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
