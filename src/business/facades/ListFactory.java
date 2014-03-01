/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facades;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.PersistenceException;
import persistence.iDao.*;
import persistence.local.PersistFactory;
import persistence.local.WorldManager;

/**
 *
 * @author jmoura
 */
public class ListFactory {

    private static final Log log = LogFactory.getLog(ListFactory.class);

    public SortedMap<String, Local> listLocais() {
        ILocalDao localDao = PersistFactory.getLocalDao();
        try {
            return localDao.list(null);
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    public Local getLocal(String Coordenada) {
        ILocalDao localDao = PersistFactory.getLocalDao();
        try {
            return localDao.get(Coordenada);
        } catch (PersistenceException ex) {
            log.fatal("Local nao existe? ...", ex);
        }
        return null;
    }

    public Collection<Artefato> listArtefatos() {
        IArtefatoDao artefatoDao = PersistFactory.getArtefatoDao();
        try {
            return artefatoDao.list(null).values();
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
        }
        return null;

    }

    public Collection<Personagem> listPersonagens() {
        IPersonagemDao personagemDao = PersistFactory.getPersonagemDao();
        try {
            return personagemDao.list(null).values();
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    public SortedMap<String, Nacao> listNacoes() {
        INacaoDao nacaoDao = PersistFactory.getNacaoDao();
        try {
            return nacaoDao.list(null);
        } catch (PersistenceException ex) {
            log.fatal("Problemas na persistencia...", ex);
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public SortedMap<String, Cidade> listCidades() {
        try {
            ICidadeDao cidadeDao = PersistFactory.getCidadeDao();
            return cidadeDao.list(WorldManager.getInstance().getPartida());
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
        }
        return null;
    }

    public SortedMap<String, Exercito> listExercitos() {
        try {
            IExercitoDao exercitoDao = PersistFactory.getExercitoDao();
            return exercitoDao.list(WorldManager.getInstance().getPartida());
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    private Jogador getJogador(int id) {
        IJogadorDao jogadorDao = PersistFactory.getJogadorDao();
        try {
            return jogadorDao.get(id);
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    private Produto getProduto(int id) {
        IProdutoDao produtoDao = PersistFactory.getProdutoDao();
        try {
            return produtoDao.get(id);
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    public SortedMap<String, Ordem> listAcoes() {
        IOrdemDao ordemDao = PersistFactory.getOrdemDao();
        try {
            return ordemDao.list(null);
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
        }
        return null;
    }

    private List<Alianca> listAliancas(Partida partida) {
        IAliancaDao aliancaDao = PersistFactory.getAliancaDao();
        try {
            return aliancaDao.list();
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    private Alianca getAliancas(int id) {
        IAliancaDao aliancaDao = PersistFactory.getAliancaDao();
        try {
            return aliancaDao.get(id);
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
            System.exit(-1);
        }
        return null;
    }

    public List<Feitico> listFeiticos() {
        try {
            IFeiticoDao feiticoDao = PersistFactory.getFeiticoDao();
            return feiticoDao.list(null);
        } catch (PersistenceException ex) {
            log.fatal("texto..", ex);
        }
        return null;
    }

    public Collection<TipoTropa> listTropas() {
        return WorldManager.getInstance().getCenario().getTipoTropas().values();
    }
}
