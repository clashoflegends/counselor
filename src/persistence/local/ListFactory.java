/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.local;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.Alianca;
import model.Artefato;
import model.Cidade;
import model.Exercito;
import model.Feitico;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.Personagem;
import model.Produto;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.iDao.IAliancaDao;
import persistence.iDao.IArtefatoDao;
import persistence.iDao.ICidadeDao;
import persistence.iDao.IExercitoDao;
import persistence.iDao.IFeiticoDao;
import persistence.iDao.IJogadorDao;
import persistence.iDao.ILocalDao;
import persistence.iDao.INacaoDao;
import persistence.iDao.IOrdemDao;
import persistence.iDao.IPersonagemDao;
import persistence.iDao.IProdutoDao;
import persistenceCommons.PersistenceException;

/**
 *
 * @author jmoura
 */
public class ListFactory implements Serializable {

    private static final Log log = LogFactory.getLog(ListFactory.class);

    public SortedMap<String, Local> listLocais() {
        ILocalDao localDao = PersistFactory.getLocalDao();
        try {
            return localDao.listWithVisibility(null);
        } catch (PersistenceException ex) {
            throw new UnsupportedOperationException("text...", ex);
        }
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
            throw new UnsupportedOperationException("text...", ex);
        }
    }

    public SortedMap<String, Nacao> listNacoes() {
        INacaoDao nacaoDao = PersistFactory.getNacaoDao();
        try {
            return nacaoDao.list(null);
        } catch (PersistenceException ex) {
            log.fatal("Problemas na persistencia...", ex);
            throw new UnsupportedOperationException("Not yet implemented...");
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
            throw new UnsupportedOperationException("text...", ex);
        }
    }

    private Jogador getJogador(int id) {
        IJogadorDao jogadorDao = PersistFactory.getJogadorDao();
        try {
            return jogadorDao.get(id);
        } catch (PersistenceException ex) {
            throw new UnsupportedOperationException("text...", ex);
        }
    }

    private Produto getProduto(int id) {
        IProdutoDao produtoDao = PersistFactory.getProdutoDao();
        try {
            return produtoDao.get(id);
        } catch (PersistenceException ex) {
            throw new UnsupportedOperationException("text...", ex);
        }
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
            throw new UnsupportedOperationException("text...", ex);
        }
    }

    private Alianca getAliancas(int id) {
        IAliancaDao aliancaDao = PersistFactory.getAliancaDao();
        try {
            return aliancaDao.get(id);
        } catch (PersistenceException ex) {
            throw new UnsupportedOperationException("text...", ex);
        }
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
