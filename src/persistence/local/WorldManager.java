/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.local;

import java.io.File;
import model.World;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistenceCommons.SettingsManager;
import business.WorldManagerBase;

/**
 * Singleton para gerenciar o xml que deve ser único. TODO: depois tem que ver como fica o multi turno.
 *
 * @author gurgel
 */
public class WorldManager extends WorldManagerBase {

    private static final Log log = LogFactory.getLog(WorldManager.class);
    private static WorldManager instance;

    private WorldManager() {
    }

    public static synchronized WorldManager getInstance() {
        if (WorldManager.instance == null) {
            WorldManager.instance = new WorldManager();
        }
        return WorldManager.instance;
    }

    /* client only*/
    public void doStart(File file) throws PersistenceException {
        if ("client".equalsIgnoreCase(SettingsManager.getInstance().getConfigurationMode())) {
            world = PersistFactory.getWorldDao().get(file);
            log.debug("Criei instancia World");
        } else {
            throw new PersistenceException("Client ONLY, something is REALLY wrong!");
        }
    }

    public World getWorld() {
        return this.world;
    }
}
