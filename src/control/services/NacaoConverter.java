/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import baseLib.SysApoio;
import business.facade.AcaoFacade;
import business.facade.NacaoFacade;
import business.facades.ListFactory;
import business.facades.WorldFacade;
import java.io.Serializable;
import java.util.*;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class NacaoConverter implements Serializable {

    public static final int ORDEM_COL_INDEX_START = 5;
    public static final int FILTRO_PROPRIOS = 1;
    public static final int FILTRO_TODOS = 0;
    private static final Log log = LogFactory.getLog(NacaoConverter.class);
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static GenericoComboBoxModel getNacaoComboModel(Nacao nacaoExcluida) {
        Nacao[] items = listNacoesDisponiveis(nacaoExcluida);
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public static GenericoTableModel getNacaoModel(int filtro) {
        GenericoTableModel nacaoModel = new GenericoTableModel(
                getNacaoColNames(),
                getNacoesAsArray(filtro),
                new Class[]{
            java.lang.String.class,
            java.lang.String.class,
            java.lang.Integer.class,
            java.lang.String.class,
            Local.class,
            java.lang.Integer.class,
            java.lang.Integer.class,
            java.lang.Integer.class,
            java.lang.Integer.class,
            java.lang.String.class,
            java.lang.String.class
        });
        return nacaoModel;
    }

    private static Object[] toArray(Nacao nacao) {
        int ii = 0;
        Object[] cArray = new Object[getNacaoColNames().length];
        cArray[ii++] = nacaoFacade.getNome(nacao);
        cArray[ii++] = nacaoFacade.getRacaNome(nacao);
        cArray[ii++] = nacaoFacade.getPontosVitoria(nacao);
        cArray[ii++] = SysApoio.iif(nacaoFacade.isAtiva(nacao), labels.getString("ATIVA"), labels.getString("INATIVA"));
        cArray[ii++] = nacaoFacade.getCoordenadasCapital(nacao);
        cArray[ii++] = acaoFacade.getPointsSetup(nacao);
        cArray[ii++] = nacaoFacade.getTropasQt(nacao);
        cArray[ii++] = nacaoFacade.getMoneySaldo(nacao);
        cArray[ii++] = nacaoFacade.getImpostos(nacao);
        cArray[ii++] = nacaoFacade.getJogadorDisplay(nacao);
        cArray[ii++] = nacaoFacade.getJogadorEmail(nacao);
        return cArray;
    }

    private static String[] getNacaoColNames() {
        String[] colNames = {
            labels.getString("NOME"),
            labels.getString("RACA"),
            labels.getString("PONTOS.VITORIA"),
            labels.getString("ATIVA"),
            labels.getString("CIDADE.CAPITAL"),
            labels.getString("STARTUP.POINTS"),
            labels.getString("TROPAS"),
            labels.getString("TREASURY"),
            labels.getString("IMPOSTOS"),
            labels.getString("JOGADOR"),
            labels.getString("JOGADOR.EMAIL")
        };
        return (colNames);
    }

    private static Object[][] getNacoesAsArray(int filtro) {
        List listaExibir = listaByFiltro(filtro);
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getNacaoColNames().length];
            Iterator lista = listaExibir.iterator();
            while (lista.hasNext()) {
                Nacao nacao = (Nacao) lista.next();
                // Converte um Nacao para um Array[] 
                ret[ii++] = NacaoConverter.toArray(nacao);
            }
            return (ret);
        }
    }

    public static GenericoTableModel getTropaModel(Nacao nacao) {
        GenericoTableModel model = new GenericoTableModel(getTropaColNames(),
                getTropaAsArray(nacao),
                new Class[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class});
        return model;
    }

    private static String[] getTropaColNames() {
        String[] colNames = {labels.getString("TROPA"), labels.getString("TIPO"), labels.getString("CUSTO.MANUTENCAO")};
        return (colNames);
    }

    private static Object[][] getTropaAsArray(Nacao nacao) {
        SortedMap<TipoTropa, Integer> listaExibir = nacaoFacade.getTropas(nacao);
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", 0}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getTropaColNames().length];
            for (TipoTropa tpTropa : listaExibir.keySet()) {
                // Converte TipoTropa para um Array[] 
                ret[ii][0] = tpTropa.getNome();
                ret[ii][1] = SysApoio.iif(listaExibir.get(tpTropa) == 1, labels.getString("TROPA.ESPECIAL"), labels.getString("TROPA.REGULAR"));
                ret[ii][2] = tpTropa.getUpkeepMoney();
                ii++;
            }
            return (ret);
        }
    }

    public static GenericoTableModel getRelacionamentoModel(Nacao nacao) {
        GenericoTableModel model = new GenericoTableModel(getRelacionamentoColNames(),
                getRelacionamentoAsArray(nacao),
                new Class[]{java.lang.String.class, java.lang.String.class});
        return model;
    }

    private static String[] getRelacionamentoColNames() {
        String[] colNames = {labels.getString("NACAO"), labels.getString("RELACIONAMENTO")};
        return (colNames);
    }

    private static Object[][] getRelacionamentoAsArray(Nacao nacao) {
        Collection<Nacao> listaExibir = nacaoFacade.getRelacionamentos(nacao).keySet();
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getRelacionamentoColNames().length];
            for (Nacao nacaoAlvo : listaExibir) {
                if (nacao != nacaoAlvo) {
                    ret[ii][0] = nacaoAlvo.getNome();
                    ret[ii][1] = nacaoFacade.getRelacionamento(nacao, nacaoAlvo);
                    //ret[ii][1] = Msgs.nacaoRelacionamento[nacao.getRelacionamento(nacaoAlvo) + 2];
                    ii++;
                }
            }
            return (ret);
        }
    }

    public static List<Nacao> listaByFiltro(int filtro) {
        List<Nacao> ret = new ArrayList();
        for (Nacao nacao : listFactory.listNacoes().values()) {
            if (filtro == FILTRO_TODOS) {
                ret.add(nacao);
            } else if (filtro == FILTRO_PROPRIOS) {
                Jogador jativo = WorldFacade.getInstance().getJogadorAtivo();
                Jogador jnacao = null;
                if (nacao.getOwner() != null) {
                    jnacao = nacaoFacade.getOwner(nacao);
                }
//                log.info(jativo + "/" + jnacao);
                if (jativo == jnacao) {
                    ret.add(nacao);
                }
            }
        }
        return ret;
    }

    public static Nacao[] listNacoesDisponiveis(Nacao nacaoExcluida) {
        //FIXME: como tratar nacao dos npcs na lista?
        int mod = 0;
        if (nacaoExcluida != null) {
            mod = 1;
        }
        Nacao[] ret = new Nacao[listFactory.listNacoes().size() - mod];
        Iterator lista = listFactory.listNacoes().values().iterator();
        int i = 0;
        while (lista.hasNext()) {
            Nacao nacao = (Nacao) lista.next();
            if (nacao != nacaoExcluida) {
                ret[i++] = nacao;
            }
        }
        return ret;
    }
}
