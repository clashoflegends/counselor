/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.SortedMap;
import model.Cidade;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.iDao.ICidadeDao;
import persistence.local.WorldManager;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public class ClientCidadeDao implements ICidadeDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientCidadeDao.class);

    @Override
    public Cidade get(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SortedMap<String, Cidade> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getCidades();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(Cidade cidade, Partida partida) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Cidade get(ResultSet rs, Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
