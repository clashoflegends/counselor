/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;
import model.ComandoDetail;
import model.Nacao;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.iDao.INacaoDao;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class ClientNacaoDao implements INacaoDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientNacaoDao.class);

    @Override
    public SortedMap<String, Nacao> list(Partida partida) throws PersistenceException {
        return WorldManager.getInstance().getNacoes();
    }

    @Override
    public Nacao get(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Nacao get(int id, Partida partida) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet. Jugde exclusive use"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean salvaOrdens(int idPartida, int turno, int idJogador, String creationTimeStamp, int idNacao, String ordensCsv, String packages, List<ComandoDetail> comDet) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
