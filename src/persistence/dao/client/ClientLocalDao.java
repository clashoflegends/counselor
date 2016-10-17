/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.SortedMap;
import model.Local;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.iDao.ILocalDao;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class ClientLocalDao implements ILocalDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientLocalDao.class);

    @Override
    public SortedMap<String, Local> listWithVisibility(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getLocais();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Local get(String coord) throws PersistenceException {
        return WorldManager.getInstance().getLocais().get(coord);
    }

    @Override
    public Local getInfoBasico(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Local getInfoVisible(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Local getInfoFull(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void load(Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unload(Partida partida) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(Local local) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SortedMap<String, Local> listFull(Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
