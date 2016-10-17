/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.iDao.IPartidaDao;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class ClientPartidaDao implements IPartidaDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientPartidaDao.class);
    private Partida partida;

    @Override
    public Partida get(int idPartida, int turno) throws PersistenceException {
        if (!isCache()) {
            log.info("Carregando partida");
            partida = WorldManager.getInstance().getPartida();
            addCache(partida);
        }
        return partida;
    }

    private boolean isCache() {
        return (partida != null);
    }

    private void addCache(Partida aPartida) {
        partida = aPartida;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean setRun(int idPartida) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
