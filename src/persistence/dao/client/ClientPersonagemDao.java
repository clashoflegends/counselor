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
import persistence.PersistenceException;
import persistence.local.WorldManager;
import persistence.iDao.IPersonagemDao;

/**
 *
 * @author gurgel
 */
public class ClientPersonagemDao implements IPersonagemDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientPersonagemDao.class);

    public ClientPersonagemDao() {
    }

    public Personagem get(int id, int turno) throws PersistenceException {
        //server only
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SortedMap<String, Personagem> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getPersonagens();
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
