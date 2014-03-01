/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.local;

//import persistence.SettingsManager;
import java.io.Serializable;
import persistence.dao.client.ClientArtefatoDao;
import persistence.dao.client.ClientCidadeDao;
import persistence.dao.client.ClientExercitoDao;
import persistence.dao.client.ClientFeiticoDao;
import persistence.dao.client.ClientLocalDao;
import persistence.dao.client.ClientNacaoDao;
import persistence.dao.client.ClientOrdemDao;
import persistence.dao.client.ClientPartidaDao;
import persistence.dao.client.ClientPersonagemDao;
import persistence.dao.client.ClientWorldDao;
import persistence.iDao.*;

/**
 *
 * @author gurgel
 */
public class PersistFactory implements Serializable {

    private static IAliancaDao aliancaDao;
    private static IArtefatoDao artefatoDao;
    private static ICenarioDao cenarioDao;
    private static ICidadeDao cidadeDao;
    private static IExercitoDao exercitoDao;
    private static IFeiticoDao feiticoDao;
    private static IJogadorDao jogadorDao;
    private static ILocalDao localDao;
    private static IMercadoDao mercadoDao;
    private static INacaoDao nacaoDao;
    private static IOrdemDao ordemDao;
    private static IPartidaDao partidaDao;
    private static IProdutoDao produtoDao;
    private static IPersonagemDao personagemDao;
    private static IRacaDao racaDao;
    private static IWorldDao worldDao;

    public static IAliancaDao getAliancaDao() {
        return aliancaDao;
    }

    public static IArtefatoDao getArtefatoDao() {
        if (artefatoDao == null) {
            artefatoDao = new ClientArtefatoDao();
        }
        return artefatoDao;
    }

    public static ICidadeDao getCidadeDao() {
        if (cidadeDao == null) {
            cidadeDao = new ClientCidadeDao();
        }
        return cidadeDao;
    }

    public static IExercitoDao getExercitoDao() {
        if (exercitoDao == null) {
            exercitoDao = new ClientExercitoDao();
        }
        return exercitoDao;
    }

    public static IFeiticoDao getFeiticoDao() {
        if (feiticoDao == null) {
            feiticoDao = new ClientFeiticoDao();
        }
        return feiticoDao;
    }

    public static IJogadorDao getJogadorDao() {
        return jogadorDao;
    }

    public static ILocalDao getLocalDao() {
        if (localDao == null) {
            localDao = new ClientLocalDao();
        }
        return localDao;
    }

    public static IMercadoDao getMercadoDao() {
        return mercadoDao;
    }

    public static INacaoDao getNacaoDao() {
        if (nacaoDao == null) {
            nacaoDao = new ClientNacaoDao();
        }
        return nacaoDao;
    }

    public static IOrdemDao getOrdemDao() {
        if (ordemDao == null) {
            ordemDao = new ClientOrdemDao();
        }
        return ordemDao;
    }

    public static IPartidaDao getPartidaDao() {
        if (partidaDao == null) {
            partidaDao = new ClientPartidaDao();
        }
        return partidaDao;
    }

    public static IProdutoDao getProdutoDao() {
        return produtoDao;
    }

    public static IPersonagemDao getPersonagemDao() {
        if (personagemDao == null) {
            personagemDao = new ClientPersonagemDao();
        }
        return personagemDao;
    }

    public static IRacaDao getRacaDao() {
        return racaDao;
    }

    public static IWorldDao getWorldDao() {
        if (worldDao == null) {
            worldDao = new ClientWorldDao();
        }
        return worldDao;
    }
}
