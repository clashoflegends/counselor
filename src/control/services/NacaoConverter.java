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
import business.facades.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
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

    public static final int ORDEM_COL_INDEX_START = 4;
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

    public static GenericoComboBoxModel getNacaoAllyComboModel(Nacao nacaoExcluida) {
        Nacao[] items = listNacoesAllyDisponiveis(nacaoExcluida);
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public static GenericoTableModel getNacaoModel(int filtro) {
        List<Class> classes = new ArrayList<Class>(30);
        GenericoTableModel nacaoModel = new GenericoTableModel(
                getNacaoColNames(classes),
                getNacoesAsArray(filtro),
                classes.toArray(new Class[0]));
        return nacaoModel;
    }

    private static Object[] toArray(Nacao nacao) {
        int ii = 0;
        Object[] cArray = new Object[getNacaoColNames(new ArrayList<Class>(30)).length];
        cArray[ii++] = nacaoFacade.getNome(nacao);
        cArray[ii++] = nacaoFacade.getRacaNome(nacao);
        cArray[ii++] = nacaoFacade.getPontosVitoria(nacao);
        cArray[ii++] = SysApoio.iif(nacaoFacade.isAtiva(nacao), labels.getString("ATIVA"), labels.getString("INATIVA"));
        if (WorldFacadeCounselor.getInstance().hasCapitals()) {
            cArray[ii++] = nacaoFacade.getCoordenadasCapital(nacao);
        }
        cArray[ii++] = acaoFacade.getPointsSetup(nacao);
        cArray[ii++] = nacaoFacade.getTropasQt(nacao);
        cArray[ii++] = nacaoFacade.getMoneySaldo(nacao);
        cArray[ii++] = nacaoFacade.getImpostos(nacao);
        cArray[ii++] = nacaoFacade.getLealdade(nacao);
        cArray[ii++] = nacaoFacade.getLealdadeAnterior(nacao);
        cArray[ii++] = nacaoFacade.getJogadorDisplay(nacao);
        cArray[ii++] = nacaoFacade.getJogadorEmail(nacao);
        return cArray;
    }

    private static String[] getNacaoColNames(List<Class> classes) {
        List<String> colNames = new ArrayList<String>(30);
        colNames.add(labels.getString("NOME"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("RACA"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("PONTOS.VITORIA"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("ATIVA"));
        classes.add(java.lang.String.class);
        if (WorldFacadeCounselor.getInstance().hasCapitals()) {
            colNames.add(labels.getString("CIDADE.CAPITAL"));
            classes.add(Local.class);
        }
        colNames.add(labels.getString("STARTUP.POINTS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("TROPAS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("TREASURY"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("IMPOSTOS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("LEALDADE"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("LEALDADE.VARIACAO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("JOGADOR"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("JOGADOR.EMAIL"));
        classes.add(java.lang.String.class);
        return (colNames.toArray(new String[0]));
    }

    private static Object[][] getNacoesAsArray(int filtro) {
        List listaExibir = listaByFiltro(filtro);
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getNacaoColNames(new ArrayList<Class>(30)).length];
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
                Jogador jativo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
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
        return listNacoes(nacaoExcluida, 0);
    }

    public static Nacao[] listNacoesAllyDisponiveis(Nacao nacaoExcluida) {
        return listNacoes(nacaoExcluida, 1);
    }

    /*
     * Filter == 0; all
     * Filter == 1; ally
     */
    private static Nacao[] listNacoes(Nacao nacaoExcluida, int filter) {
        //FIXME: como tratar nacao dos npcs na lista?
        List<Nacao> nationList = new ArrayList<Nacao>(listFactory.listNacoes().size());
        for (Nacao nacao : listFactory.listNacoes().values()) {
            if (nacao == nacaoExcluida) {
                continue;
            }
            if (filter == 1 && !nacaoFacade.isAliado(nacaoExcluida, nacao)) {
                continue;
            }
            nationList.add(nacao);
        }
        return nationList.toArray(new Nacao[0]);
    }
}
