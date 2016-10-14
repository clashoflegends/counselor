/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.SortedMap;
import model.Partida;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.iDao.IPersonagemDao;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class ClientPersonagemDao implements IPersonagemDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientPersonagemDao.class);

    public ClientPersonagemDao() {
    }

    @Override
    public Personagem get(int id, int turno) throws PersistenceException {
        //server only
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SortedMap<String, Personagem> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getPersonagens();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Personagem get(int id, Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
