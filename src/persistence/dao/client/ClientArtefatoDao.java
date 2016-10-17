/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.SortedMap;
import model.Artefato;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.local.WorldManager;
import persistence.iDao.IArtefatoDao;

/**
 *
 * @author gurgel
 */
public class ClientArtefatoDao implements IArtefatoDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientArtefatoDao.class);

    @Override
    public SortedMap<String, Artefato> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getArtefatos();
    }

    @Override
    public Artefato get(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Artefato get(int id, Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
