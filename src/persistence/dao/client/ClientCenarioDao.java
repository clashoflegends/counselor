/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import model.Cenario;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.iDao.ICenarioDao;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public class ClientCenarioDao implements ICenarioDao {

    private static final Log log = LogFactory.getLog(ClientCenarioDao.class);

    @Override
    public Cenario get(Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
