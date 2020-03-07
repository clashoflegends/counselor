/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.NacaoFacade;
import persistence.local.ListFactory;
import control.facade.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Produto;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author Gurgel
 */
public class NacaoConverter implements Serializable {

    public static final int ORDEM_COL_INDEX_START = 4;
    private static final Log log = LogFactory.getLog(NacaoConverter.class);
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final WorldFacadeCounselor WFC = WorldFacadeCounselor.getInstance();

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

    public static GenericoComboBoxModel getNacaoNoEnemySwornComboModel(Nacao nacaoExcluida) {
        Nacao[] items = listNacoesNoEnemySwornDisponiveis(nacaoExcluida);
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public static GenericoTableModel getNacaoModel(List<Nacao> lista) {
        List<Class> classes = new ArrayList<Class>(30);
        GenericoTableModel nacaoModel = new GenericoTableModel(
                getNacaoColNames(classes),
                getNacoesAsArray(lista),
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
        if (cenarioFacade.hasOrdensNacao(WFC.getPartida())) {
            cArray[ii++] = acaoFacade.getPointsSetup(nacao);
        }
        if (WFC.hasCapitals()) {
            cArray[ii++] = nacaoFacade.getCoordenadasCapital(nacao);
        }
        final Cenario cenario = WFC.getCenario();
        final Collection<Exercito> exercitos = WFC.getExercitos();
        cArray[ii++] = nacaoFacade.getPersonagens(nacao);
        cArray[ii++] = nacaoFacade.getPersonagensSlot(nacao, cenario);
        cArray[ii++] = nacaoFacade.getTropasQt(nacao, exercitos);
//        int valorAcoes = 0;
//        for (PersonagemOrdem po : WFC.getMapPersonagemOrdens(nacao)) {
//            valorAcoes -= acaoFacade.getCusto(po);
//        }
//        cArray[ii++] = valorAcoes;
        cArray[ii++] = nacaoFacade.getMoneySaldo(nacao);
        cArray[ii++] = nacaoFacade.getImpostos(nacao);
        Produto[] produtos = cenarioFacade.listProdutos(cenario, 1);
        for (Produto produto : produtos) {
            int prod = nacaoFacade.getProducao(nacao, produto, cenario, WorldFacadeCounselor.getInstance().getTurno());
            int est = nacaoFacade.getEstoque(nacao, produto);
            cArray[ii++] = prod + est;
        }
        cArray[ii++] = nacaoFacade.getLealdade(nacao);
        cArray[ii++] = nacaoFacade.getLealdadeAnterior(nacao);
        cArray[ii++] = nacaoFacade.getTeamFlag(nacao);
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
        if (cenarioFacade.hasOrdensNacao(WFC.getPartida())) {
            //add if for startup points here.
            colNames.add(labels.getString("STARTUP.POINTS"));
            classes.add(java.lang.Integer.class);
        }
        if (WFC.hasCapitals()) {
            colNames.add(labels.getString("CIDADE.CAPITAL"));
            classes.add(Local.class);
        }
        colNames.add(labels.getString("PERSONAGENS.KNOWN"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("PERSONAGENS.SLOT"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("TROPAS"));
        classes.add(java.lang.Integer.class);
//        colNames.add(labels.getString("FINANCAS.COST.ACTIONS"));
//        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("TREASURY"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("IMPOSTOS"));
        classes.add(java.lang.Integer.class);
        final Cenario cenario = WFC.getCenario();
        Produto[] produtos = cenarioFacade.listProdutos(cenario, 1);
        for (Produto produto : produtos) {
            colNames.add(produto.getNome());
            classes.add(java.lang.Integer.class);
        }
        colNames.add(labels.getString("LEALDADE"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("LEALDADE.VARIACAO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("ALIANCA"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("JOGADOR"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("JOGADOR.EMAIL"));
        classes.add(java.lang.String.class);
        return (colNames.toArray(new String[0]));
    }

    private static Object[][] getNacoesAsArray(List<Nacao> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getNacaoColNames(new ArrayList<Class>(1)).length];
            for (Nacao nacao : listaExibir) {
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

    public static GenericoTableModel getRelacionamentoAllModel() {
        final String[] allNationsColNames = getRelacionamentoAllColNames();
        GenericoTableModel model = new GenericoTableModel(allNationsColNames,
                getRelacionamentoAllAsArray(allNationsColNames),
                new Class[]{java.lang.String.class, java.lang.String.class});
        return model;
    }

    private static String[] getRelacionamentoColNames() {
        String[] colNames = {labels.getString("NACAO"), labels.getString("DIPLOMACY")};
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
                if (nacao == nacaoAlvo) {
                    continue;
                }
                ret[ii][0] = nacaoAlvo.getNome();
                ret[ii][1] = nacaoFacade.getRelacionamento(nacao, nacaoAlvo);
                ii++;
            }
            return (ret);
        }
    }

    private static String[] getRelacionamentoAllColNames() {
        final List<String> nations = new ArrayList<String>();
        //+1 for header first column
        nations.add(" ");
        for (Nacao nation : listFactory.listNacoes().values()) {
            if (!nacaoFacade.isAtivaPC(nation)) {
                //exclude inactive nations (in WDO it is a major issue)
                continue;
            }
            nations.add(nation.getNome());
        }
        return (nations.toArray(new String[0]));
    }

    private static Object[][] getRelacionamentoAllAsArray(String[] allNationsColNames) {
        Collection<Nacao> listaExibir = listFactory.listNacoes().values();
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        }
        /*
            Steps
                build basic table
                confirm colunms and rows
                paint red/green?
            
         */
        int ii = 0;
        Object[][] ret = new Object[allNationsColNames.length - 1][allNationsColNames.length];
        for (Nacao nacaoRow : listaExibir) {
            if (!nacaoFacade.isAtivaPC(nacaoRow)) {
                //exclude inactive nations (in WDO it is a major issue)
                continue;
            }
            ret[ii][0] = nacaoRow.getNome();
            int nn = 1;
            for (Nacao nacaoCol : listaExibir) {
                if (!nacaoFacade.isAtivaPC(nacaoCol)) {
                    //exclude inactive nations (in WDO it is a major issue)
                    continue;
                }
                if (nacaoRow == nacaoCol) {
                    //don't write if the same
                    ret[ii][nn++] = "";
                } else {
                    ret[ii][nn++] = nacaoFacade.getRelacionamento(nacaoRow, nacaoCol);
                }
            }
            ii++;
        }
        return (ret);
    }

    public static List<Nacao> listaByFiltro(String filtro) {
        final Jogador jAtivo = WFC.getJogadorAtivo();
        List<Nacao> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            //todos
            ret.addAll(listFactory.listNacoes().values());
        } else if (filtro.equalsIgnoreCase("active")) {
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (ally.isAtiva()) {
                    ret.add(ally);
                }
            }
        } else if (filtro.equalsIgnoreCase("own")) {
            ret.addAll(jAtivo.getNacoes().values());
        } else if (filtro.equalsIgnoreCase("team") && jAtivo != null) {
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(ally) || jAtivo.isNacao(ally)) {
                    ret.add(ally);
                }
            }
        } else if (filtro.equalsIgnoreCase("allies") && jAtivo != null) {
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(ally) && !jAtivo.isNacao(ally)) {
                    ret.add(ally);
                }
            }
        } else if (filtro.equalsIgnoreCase("enemies") && jAtivo != null) {
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (!jAtivo.isJogadorAliado(ally) && !jAtivo.isNacao(ally)) {
                    ret.add(ally);
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

    public static Nacao[] listNacoesNoEnemySwornDisponiveis(Nacao nacaoExcluida) {
        return listNacoes(nacaoExcluida, 2);
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
            if (filter == 2 && nacaoFacade.isEnemySworn(nacaoExcluida, nacao)) {
                continue;
            }
            nationList.add(nacao);
        }
        return nationList.toArray(new Nacao[0]);
    }
}
