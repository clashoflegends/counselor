/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.SortedMap;
import model.Exercito;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.local.WorldManager;
import persistence.iDao.IExercitoDao;

/**
 *
 * @author gurgel
 */
public class ClientExercitoDao implements IExercitoDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientExercitoDao.class);

    public SortedMap<String, Exercito> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getExercitos();
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SortedMap<String, Exercito> loadVisible(Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
