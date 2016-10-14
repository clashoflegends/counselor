/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.dao.client;

import java.io.Serializable;
import java.util.List;
import model.Cenario;
import model.Feitico;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistence.local.WorldManager;
import persistence.iDao.IFeiticoDao;


/**
 *
 * @author gurgel
 */
public class ClientFeiticoDao implements IFeiticoDao, Serializable {

    private static final Log log = LogFactory.getLog(ClientFeiticoDao.class);

    public ClientFeiticoDao() {
    }

    public Feitico get(int id) throws PersistenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Feitico> list(Cenario cenario) throws PersistenceException {
        return WorldManager.getInstance().getCenario().getFeiticos();
    }
}
