/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.File;
import java.io.Serializable;
import model.World;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.PersistenceException;
import persistenceCommons.SettingsManager;
import persistenceCommons.XmlManager;
import persistence.iDao.IWorldDao;

/**
 *
 * @author gurgel
 */
public class ClientWorldDao implements IWorldDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientWorldDao.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private World world;

    @Override
    public World get(File file) throws PersistenceException {
        try {
            this.world = (World) XmlManager.getInstance().get(file);
        } catch (ClassCastException ex) {
            throw new PersistenceException(labels.getString("ARQUIVO.INVALIDO"));
        }
        return this.world;
    }

    @Override
    public String save(World world, File file) throws PersistenceException {
        if (world != null) {
            XmlManager.getInstance().save(world, file);
        }
        return file.getName();
    }

    @Override
    /**
     * server only
     */
    public String save(World world) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
