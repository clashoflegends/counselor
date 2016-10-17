/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.SortedMap;
import model.Cenario;
import model.Ordem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.iDao.IOrdemDao;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class ClientOrdemDao implements IOrdemDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientOrdemDao.class);

    @Override
    public SortedMap<String, Ordem> list(Cenario cenario) throws PersistenceException {
        return WorldManager.getInstance().getOrdens();
    }

    @Override
    public Ordem get(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
