/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.facade.JogadorFacade;
import business.facade.LocalFacade;
import business.facade.NacaoFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import control.facade.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.ComboBoxModel;
import model.ActorAction;
import model.Cidade;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Mercado;
import model.Nacao;
import model.Personagem;
import model.Produto;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.ListFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
import utils.OpenSlotCounter;
import utils.StringRet;

/**
 *
 * @author Gurgel
 */
public class CidadeConverter implements Serializable {

    private static final Log log = LogFactory.getLog(CidadeConverter.class);
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static GenericoTableModel getCidadeModel(List lista) {
        List<Class> classes = new ArrayList<>(30);
        GenericoTableModel cidadeModel = new GenericoTableModel(
                getCidadeColNames(classes),
                getCidadesAsArray(lista),
                classes.toArray(new Class[0]));
        return cidadeModel;
    }

    private static String[] getCidadeColNames(List<Class> classes) {
        List<String> colNames = new ArrayList<>(30);
        colNames.add(labels.getString("NOME"));
        classes.add(java.lang.String.class);
        if (WorldFacadeCounselor.getInstance().hasOrdensCidade()) {
            colNames.add(labels.getString("OPEN.SLOTS"));
            classes.add(OpenSlotCounter.class);
        }
        colNames.add(labels.getString("TAMANHO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("LOCAL"));
        classes.add(Local.class);
        colNames.add(labels.getString("IMPOSTOS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("ZONA.ECONOMICA"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("CUSTO.MANUTENCAO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("LEALDADE"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("LEALDADE.VARIACAO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("PRESENCAS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("RACA"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("CIDADE.DOCAS"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("FORTIFICACOES"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("CIDADE.DEFESA"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("COMIDA.GIVEN"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("OCULTO"));
        classes.add(java.lang.String.class);
        if (WorldFacadeCounselor.getInstance().hasCapitals()) {
            colNames.add(labels.getString("CIDADE.CAPITAL"));
            classes.add(java.lang.String.class);
        }
        colNames.add(labels.getString("SITIADO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("PONTOS.DOMINATION"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("NACAO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("TERRENO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("CLIMA"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("VENDA.BEST"));
        classes.add(java.lang.Integer.class);
        if (WorldFacadeCounselor.getInstance().hasResourceManagement()) {
            for (Produto produto : getResourceList()) {
                colNames.add(produto.getNome());
                classes.add(java.lang.Integer.class);
            }
        }

//        String[] colNames = {
//            labels.getString("IMPOSTOS"), labels.getString("ZONA.ECONOMICA"),
//            labels.getString("LEALDADE"), labels.getString("LEALDADE.VARIACAO"),
//            labels.getString("PRESENCAS"),
//            labels.getString("RACA"), labels.getString("CIDADE.DOCAS"),
//            labels.getString("FORTIFICACOES"),
//            labels.getString("CIDADE.DEFESA"),
//            labels.getString("OCULTO"), labels.getString("CIDADE.CAPITAL"), labels.getString("SITIADO"),
//            labels.getString("NACAO"),
//            labels.getString("TERRENO"), labels.getString("CLIMA")
//        };
        return (colNames.toArray(new String[0]));
    }

    private static Object[] toArray(Cidade cidade) {
        final Mercado mercado = WorldFacadeCounselor.getInstance().getMercado();
        int ii = 0;
        Object[] cArray = new Object[getCidadeColNames(new ArrayList<>(30)).length];
        cArray[ii++] = cidadeFacade.getNome(cidade);
        if (WorldFacadeCounselor.getInstance().hasOrdensCidade()) {
            //default to can receive orders
            final OpenSlotCounter openSlot = new OpenSlotCounter(ordemFacade.getOrdensOpenSlots(cidade));
            if (jogadorFacade.isMine(cidade, WorldFacadeCounselor.getInstance().getJogadorAtivo())
                    && cidadeFacade.isAtivo(cidade)) {
                //can receive orders
                openSlot.setStatus(ActorAction.STATUS_BLANK);
            } else {
                //can receive orders, but not from active player (enemy/team mate)
                openSlot.setStatus(ActorAction.STATUS_READONLY);
            }

            cArray[ii++] = openSlot;
        }
        cArray[ii++] = cidadeFacade.getTamanhoNome(cidade);
        cArray[ii++] = cidadeFacade.getLocal(cidade);

        cArray[ii++] = cidadeFacade.getArrecadacaoImpostos(cidade);
        cArray[ii++] = cidadeFacade.getProducao(cidade, WorldFacadeCounselor.getInstance().getCenario().getMoney(), WorldFacadeCounselor.getInstance().getCenario(), WorldFacadeCounselor.getInstance().getTurno());
        cArray[ii++] = cidadeFacade.getUpkeepMoney(cidade);
        cArray[ii++] = cidadeFacade.getLealdade(cidade);
        cArray[ii++] = cidadeFacade.getLealdadeDelta(cidade);
        cArray[ii++] = localFacade.getPersonagens(cidadeFacade.getLocal(cidade)).size();
        cArray[ii++] = cidadeFacade.getRacaNome(cidade);
        cArray[ii++] = cidadeFacade.getDocasNome(cidade);
        cArray[ii++] = cidadeFacade.getFortificacaoNome(cidade);
        cArray[ii++] = cidadeFacade.getDefesa(cidade);
        cArray[ii++] = cidadeFacade.getFoodGiven(cidade);
        cArray[ii++] = cidadeFacade.getOculto(cidade);
        if (WorldFacadeCounselor.getInstance().hasCapitals()) {
            cArray[ii++] = cidadeFacade.getCapital(cidade);
        }
        cArray[ii++] = cidadeFacade.getSitiado(cidade);
        cArray[ii++] = cidadeFacade.getPointsDomination(cidade);
        cArray[ii++] = cidadeFacade.getNacaoNome(cidade);
        cArray[ii++] = localFacade.getTerrenoNome(cidadeFacade.getLocal(cidade));
        cArray[ii++] = localFacade.getClima(cidadeFacade.getLocal(cidade));
        cArray[ii++] = cidadeFacade.getResourceBestSell(cidade, mercado, WorldFacadeCounselor.getInstance().getCenario(), WorldFacadeCounselor.getInstance().getTurno());
        if (WorldFacadeCounselor.getInstance().hasResourceManagement()) {
            for (Produto produto : getResourceList()) {
                int estoque = cidadeFacade.getEstoque(cidade, produto);
                estoque += cidadeFacade.getProducao(cidade, produto, WorldFacadeCounselor.getInstance().getCenario(), WorldFacadeCounselor.getInstance().getTurno());
                cArray[ii++] = estoque;
            }
        }
        return cArray;
    }

    private static Object[][] getCidadesAsArray(List listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", false, false, "", "", "", "", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getCidadeColNames(new ArrayList<>(30)).length];
            Iterator lista = listaExibir.iterator();
            while (lista.hasNext()) {
                Cidade cidade = (Cidade) lista.next();
                ret[ii++] = CidadeConverter.toArray(cidade);
            }
            return (ret);
        }
    }

    public static GenericoTableModel getPresencasModel(Cidade cidade) {
        GenericoTableModel presencasModel = new GenericoTableModel(
                getPresencasColNames(),
                getPresencasAsArray(cidade),
                new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class
                });
        return presencasModel;
    }

    private static String[] getPresencasColNames() {
        //String[] colNames = {"Avistado", "Nação", "Tipo"};
        String[] colNames = {labels.getString("AVISTADO"),
            labels.getString("NACAO"), labels.getString("TIPO")
        };
        return (colNames);
    }

    private static Object[][] getPresencasAsArray(Cidade cidade) {
        int ii = 0;
        Iterator lista = cidadeFacade.listaPresencas(cidade).iterator();
        Object[][] ret = new Object[cidadeFacade.listaPresencas(cidade).size()][getPresencasColNames().length];
        while (lista.hasNext()) {
            Object elem = lista.next();
            if (elem == null) {
                ret[ii][0] = labels.getString("?");
                ret[ii][1] = labels.getString("?");
                ret[ii][2] = labels.getString("OUTRO");
            } else if (elem.getClass().getSimpleName().equals("Personagem")) {
                Personagem personagem = (Personagem) elem;
                int i = 0;
                ret[ii][i++] = personagemFacade.getNome(personagem);
                ret[ii][i++] = personagemFacade.getNacaoNome(personagem);
                ret[ii][i++] = labels.getString("PERSONAGEM");
                ii++;
            } else if (elem.getClass().getSimpleName().equals("Exercito")) {
                Exercito exercito = (Exercito) elem;
                int i = 0;
                ret[ii][i++] = exercitoFacade.getComandanteTitulo(exercito, WorldFacadeCounselor.getInstance().getCenario());
                ret[ii][i++] = exercitoFacade.getNacaoNome(exercito);
                ret[ii][i++] = exercitoFacade.getDescricaoTamanho(exercito);
                ii++;
            } else {
                ret[ii][0] = labels.getString("?");
                ret[ii][1] = labels.getString("?");
                ret[ii][2] = labels.getString("OUTRO");
            }
        }
//        if (ii == 0) {
//            ret = new Object[1][getPresencasColNames().length];
//            int i = 0;
//            ret[ii][i++] = "-";
//            ret[ii][i++] = "-";
//            ret[ii][i++] = "-";
//            ii++;
//        }
        return (ret);
    }

    public static GenericoTableModel getProdutoModel(Cidade cidade) {
        GenericoTableModel produtoModel = new GenericoTableModel(
                getProdutoColNames(),
                getProdutosAsArray(cidade),
                new Class[]{
                    java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
                });
        return produtoModel;
    }

    private static String[] getProdutoColNames() {
        //String[] colNames = {"Produto", "Produção", "Estoque", "Total"};
        String[] colNames = {labels.getString("PRODUTO"), labels.getString("TOTAL"),
            labels.getString("PRODUCAO"),
            labels.getString("ESTOQUE"), labels.getString("VENDA")
        };
        return (colNames);
    }

    private static Object[][] getProdutosAsArray(Cidade cidade) {
        try {
            int ii = 0;
            Iterator lista = cidadeFacade.getEstoques(cidade).keySet().iterator();
            Object[][] ret = new Object[cidade.getEstoques().keySet().size()][getProdutoColNames().length];
            while (lista.hasNext()) {
                Produto produto = (Produto) lista.next();
                int i = 0;
                final int estoque = cidadeFacade.getEstoque(cidade, produto);
                final int producao = cidadeFacade.getProducao(cidade, produto, WorldFacadeCounselor.getInstance().getCenario(), WorldFacadeCounselor.getInstance().getTurno());
                final Mercado mercado = WorldFacadeCounselor.getInstance().getMercado();
                ret[ii][i++] = produto.getNome();
                ret[ii][i++] = estoque + producao;
                ret[ii][i++] = producao;
                ret[ii][i++] = estoque;
                ret[ii][i++] = cidadeFacade.getResourceSell(cidade, produto, mercado, WorldFacadeCounselor.getInstance().getCenario(), WorldFacadeCounselor.getInstance().getTurno());
                ii++;
                //PENDING adicionar o clima
            }
            return (ret);

        } catch (NullPointerException ex) {
            return null;
        }
    }

    public static List<Cidade> listaByNacao(Nacao filtro) {
        List<Cidade> ret = new ArrayList();
        for (Cidade cidade : listFactory.listCidades().values()) {
            if (filtro == null) {
                ret.add(cidade);
            } else if (filtro == cidadeFacade.getNacao(cidade)) {
                ret.add(cidade);
            }
        }
        return ret;
    }

    public static List listaByFiltro(String filtro) {
        final Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
        List<Cidade> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            ret.addAll(listFactory.listCidades().values());
        } else if (filtro.equalsIgnoreCase("own") && jAtivo != null) {
            for (Cidade cidade : listFactory.listCidades().values()) {
                if (jAtivo.isNacao(cidadeFacade.getNacao(cidade))) {
                    ret.add(cidade);
                }
            }
        } else if (filtro.equalsIgnoreCase("bigcitymy") && jAtivo != null) {
            for (Cidade cidade : listFactory.listCidades().values()) {
                if (jAtivo.isNacao(cidadeFacade.getNacao(cidade)) && cidadeFacade.isBigCity(cidade)) {
                    ret.add(cidade);
                }
            }
        } else if (filtro.equalsIgnoreCase("team") && jAtivo != null) {
            for (Cidade cidade : listFactory.listCidades().values()) {
                if (jAtivo.isJogadorAliado(cidadeFacade.getNacao(cidade)) || jAtivo.isNacao(cidadeFacade.getNacao(cidade))) {
                    ret.add(cidade);
                }
            }
        } else if (filtro.equalsIgnoreCase("allies") && jAtivo != null) {
            for (Cidade cidade : listFactory.listCidades().values()) {
                if (jAtivo.isJogadorAliado(cidadeFacade.getNacao(cidade)) && !jAtivo.isNacao(cidadeFacade.getNacao(cidade))) {
                    ret.add(cidade);
                }
            }
        } else if (filtro.equalsIgnoreCase("enemies") && jAtivo != null) {
            for (Cidade cidade : listFactory.listCidades().values()) {
                if (!jAtivo.isJogadorAliado(cidadeFacade.getNacao(cidade)) && !jAtivo.isNacao(cidadeFacade.getNacao(cidade))) {
                    ret.add(cidade);
                }
            }
        }
        return ret;
    }

    public static List listaProdutoByCidade(Cidade cidade) {
        List ret = new ArrayList();
        ret.add(labels.getString("Estoques") + ":");
        for (Integer valor : cidade.getEstoques().values()) {
            ret.add(valor);
        }
        return ret;
    }

    /**
     *
     * @param tipo 0=todos, 1=nacao, 2=outras nacoes
     * @param nacao
     * @return
     */
    public static ComboBoxModel getCidadeComboModel(int tipo, Nacao nacao) {
        Cidade[] items = null;
        switch (tipo) {
            case 0:
                items = listaCidadesAll();
                break;
            case 1:
                items = listaCidadesNacao(nacao);
                break;
//            throw new UnsupportedOperationException("Not yet implemented");
            default:
                break;
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;

    }

    private static Cidade[] listaCidadesNacao(Nacao nacao) {
        List ret = new ArrayList();
        ret.addAll(nacao.getCidades());
        return (Cidade[]) ret.toArray(new Cidade[0]);
    }

    private static Cidade[] listaCidadesNaoNacao(Nacao nacao) {
        List ret = new ArrayList();
        ret.addAll(listFactory.listCidades().values());
        ret.removeAll(nacao.getCidades());
        return (Cidade[]) ret.toArray(new Cidade[0]);
    }

    private static Cidade[] listaCidadesAll() {
        List ret = new ArrayList();
        ret.addAll(listFactory.listCidades().values());
        return (Cidade[]) ret.toArray(new Cidade[0]);
    }

    public static List<String> getInfo(Cidade cidade) {
        return getInfo(cidade, WorldFacadeCounselor.getInstance().getJogadorAtivo().getNacoes().values());
    }

    private static List<String> getInfo(Cidade cidade, Collection<Nacao> nations) {
        StringRet ret = new StringRet();
        if (cidade == null) {
            return ret.getList();
        }
        ret = getInfoDetailed(cidade);
        for (Nacao nation : nations) {
            final Nacao targetNation = cidadeFacade.getNacao(cidade);
            if (targetNation == null || targetNation == nation) {
                continue;
            }
            //print diplomacy
            ret.addTab(String.format("%s: %s", nation.getNome(), nacaoFacade.getRelacionamento(nation, targetNation)));
            //print if inactive
            if (!nacaoFacade.isAtiva(targetNation)) {
                ret.addTab(String.format("%s: %s", nation.getNome(), labels.getString("INATIVA")));
            }
        }
        return ret.getList();
    }

    private static StringRet getInfoDetailed(Cidade cidade) {
        StringRet ret = new StringRet();
        ret.add(String.format(labels.getString("CIDADE.CAPITAL.DA.NACAO"),
                cidade.getComboDisplay(),
                SysApoio.iif(cidade.isCapital(), labels.getString("(CAPITAL)"), ""),
                nacaoFacade.getNome(cidade.getNacao())));
        ret.addTab(String.format("%s: %s", labels.getString("TAMANHO"), BaseMsgs.cidadeTamanho[cidade.getTamanho()]));
        ret.addTab(String.format("%s: %s", labels.getString("FORTIFICACOES"), BaseMsgs.cidadeFortificacao[cidade.getFortificacao()]));
        if (cidade.getLealdade() > 0) {
            ret.addTab(String.format("%s: %s (%s)",
                    labels.getString("LEALDADE"),
                    cidade.getLealdade(),
                    cidade.getLealdade() - cidade.getLealdadeAnterior()));
        } else {
            ret.addTab(String.format("%s: %s", labels.getString("LEALDADE"), "?"));
        }
        final String cityDefense;
        if (cidade.getLealdade() > 0) {
            cityDefense = SysApoio.getFormatedNumber(cidadeFacade.getDefesa(cidade));
        } else {
            String cityDefenseLow = SysApoio.getFormatedNumber(cidadeFacade.getDefesa(cidade.getTamanho(), cidade.getFortificacao(), 1));
            String cityDefenseHigh = SysApoio.getFormatedNumber(cidadeFacade.getDefesa(cidade.getTamanho(), cidade.getFortificacao(), 100));
            cityDefense = String.format("%s - %s", cityDefenseLow, cityDefenseHigh);
        }
        ret.addTab(String.format("%s: %s", labels.getString("CIDADE.DEFESA"), cityDefense));
        ret.addTab(String.format("%s: %s", labels.getString("RACA"), cidadeFacade.getRacaNome(cidade)));
        ret.addTab(String.format("%s: %s", labels.getString("CIDADE.DOCAS"), BaseMsgs.cidadeDocas[cidade.getDocas()]));
        ret.addTab(String.format("%s: %s", labels.getString("OCULTO"), cidadeFacade.getOculto(cidade)));
        ret.addTab(String.format("%s: %s", labels.getString("SITIADO"), cidadeFacade.getSitiado(cidade)));
        getInfoResources(ret, cidade);
        return ret;
    }

    private static void getInfoResources(StringRet ret, Cidade cidade) {
        if (!WorldFacadeCounselor.getInstance().hasResourceManagement()) {
            //no resources
            return;
        }
        ret.add(String.format("%s %s %s %s %s", (Object[]) getProdutoColNames()));
        Object[][] resources = getProdutosAsArray(cidade);
        int qtRes = 0;
        for (Object[] resource : resources) {
            if ((Integer) resource[4] <= 0) {
                continue;
            }
            Object[] resLabels = new String[5];
            int ii = 0;
            resLabels[ii] = (String) resource[ii++];
            resLabels[ii] = SysApoio.getFormatedNumber((int) resource[ii++]);
            resLabels[ii] = SysApoio.getFormatedNumber((int) resource[ii++]);
            resLabels[ii] = SysApoio.getFormatedNumber((int) resource[ii++]);
            resLabels[ii] = SysApoio.getFormatedNumber((int) resource[ii++]);
            ret.addTab(String.format("%s: %s %s %s %s", resLabels));
            qtRes++;
        }
        if (qtRes == 0) {
            ret.add(String.format(labels.getString("NENHUM")));
        }
    }

    private static List<Produto> getResourceList() {
        List<Produto> list = new ArrayList<>();
        list.addAll(WorldFacadeCounselor.getInstance().getCenario().getProdutos().values());
        list.remove(WorldFacadeCounselor.getInstance().getCenario().getMoney());
        return list;
    }

    public static String getResultados(Cidade cidade) {
        final String info = LocalConverter.getInfo(cidadeFacade.getLocal(cidade));
//        if (!WorldFacadeCounselor.getInstance().hasOrdensCidade()) {
//            return info;
//        } else {
            return ordemFacade.getResultado(cidade) + "\n\n" + info;
//        }
    }
}
