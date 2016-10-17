/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import model.Cenario;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//
import persistenceCommons.PersistenceException;
import persistence.iDao.ICenarioDao;

/**
 *
 * @author gurgel
 */
public class ClientCenarioDao implements ICenarioDao {

    private static final Log log = LogFactory.getLog(ClientCenarioDao.class);

    public Cenario get(Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Cenario get(int idPartida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
