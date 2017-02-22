/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.CenarioFacade;
import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.ComboBoxModel;
import model.Cidade;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Mercado;
import model.Nacao;
import model.Personagem;
import model.Produto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class CidadeConverter implements Serializable {

    private static final Log log = LogFactory.getLog(CidadeConverter.class);
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static GenericoTableModel getCidadeModel(List lista) {
        List<Class> classes = new ArrayList<Class>(30);
        GenericoTableModel cidadeModel = new GenericoTableModel(
                getCidadeColNames(classes),
                getCidadesAsArray(lista),
                classes.toArray(new Class[0]));
//        GenericoTableModel cidadeModel = new GenericoTableModel(
//                getCidadeColNames(), getCidadesAsArray(lista),
//                new Class[]{
//            java.lang.String.class, java.lang.String.class,
//            Local.class, java.lang.Integer.class,
//            java.lang.Integer.class,
//            java.lang.Integer.class, java.lang.Integer.class,
//            java.lang.Integer.class,
//            java.lang.String.class, java.lang.String.class,
//            java.lang.String.class, java.lang.Integer.class,
//            java.lang.String.class, java.lang.String.class,
//            java.lang.String.class, java.lang.String.class,
//            java.lang.String.class, java.lang.String.class
//        });
        return cidadeModel;
    }

    private static String[] getCidadeColNames(List<Class> classes) {
        List<String> colNames = new ArrayList<String>(30);
        colNames.add(labels.getString("NOME"));
        classes.add(java.lang.String.class);
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
        Object[] cArray = new Object[getCidadeColNames(new ArrayList<Class>(30)).length];
        cArray[ii++] = cidadeFacade.getNome(cidade);
        cArray[ii++] = cidadeFacade.getTamanhoNome(cidade);
        cArray[ii++] = cidadeFacade.getCoordenadas(cidade);

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
            Object[][] ret = new Object[listaExibir.size()][getCidadeColNames(new ArrayList<Class>(30)).length];
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
        if (tipo == 0) {
            items = listaCidadesAll();
        } else if (tipo == 1) {
            items = listaCidadesNacao(nacao);
        } else {
//            throw new UnsupportedOperationException("Not yet implemented");
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
        return cidadeFacade.getInfo(cidade, WorldFacadeCounselor.getInstance().getJogadorAtivo().getNacoes().values());
    }

    private static List<Produto> getResourceList() {
        List<Produto> list = new ArrayList<Produto>();
        list.addAll(WorldFacadeCounselor.getInstance().getCenario().getProdutos().values());
        list.remove(WorldFacadeCounselor.getInstance().getCenario().getMoney());
        return list;
    }

    public static String getResultados(Cidade cidade) {
        final CenarioFacade cenarioFacade = new CenarioFacade();
        final String info = LocalConverter.getInfo(cidadeFacade.getLocal(cidade));
        if (!cenarioFacade.hasOrdensCidade(WorldFacadeCounselor.getInstance().getCenario())) {
            return info;
        } else {
            return ordemFacade.getResultado(cidade) + "\n" + info;
        }
    }
}
