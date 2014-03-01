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
import persistence.PersistenceException;
import persistence.local.WorldManager;
import persistence.iDao.IArtefatoDao;

/**
 *
 * @author gurgel
 */
public class ClientArtefatoDao implements IArtefatoDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientArtefatoDao.class);

    public SortedMap<String, Artefato> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getArtefatos();
    }

    public Artefato get(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
