/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoTableModel;
import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import business.facade.PersonagemFacade;
import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import model.Artefato;
import model.Cenario;
import model.Exercito;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Mercado;
import model.Personagem;
import model.Produto;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.ListFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.StringRet;

/**
 *
 * @author jmoura
 */
public class LocalConverter implements Serializable {

    private static final Log log = LogFactory.getLog(LocalConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final ListFactory listFactory = new ListFactory();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final int turn = WorldFacadeCounselor.getInstance().getTurno();
    private static final Cenario scenario = WorldFacadeCounselor.getInstance().getCenario();
    private static final Mercado mercado = WorldFacadeCounselor.getInstance().getMercado();
    private static final boolean hasResourceManagement = WorldFacadeCounselor.getInstance().hasResourceManagement();
    private static final boolean hasCapitals = WorldFacadeCounselor.getInstance().hasCapitals();
    private static final Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();

    public static String getInfo(Local local) {
        StringRet ret = new StringRet();
        //City
        ret.add(CidadeConverter.getInfo(local.getCidade()));
        //local
        //"Local : @ 3103 em Planície O Clima é Polar"
        ret.add(String.format(labels.getString("TERRENO.CLIMA"),
                local.getTerreno().getNome(),
                BaseMsgs.localClima[local.getClima()]));
        //landmarks and others
        ret.add("\n");
        for (Habilidade hab : local.getHabilidades().values()) {
            if (hab.getCodigo().equals(";-;")) {
                continue;
            }
            ret.add(hab.getNome());
        }
        //City 
        if (localFacade.isCidade(local)) {
            for (Habilidade hab : local.getCidade().getHabilidades().values()) {
                if (hab.getCodigo().equals(";-;")) {
                    continue;
                }
                ret.add(hab.getNome());
            }
        }
        //personagens
        if (local.getPersonagens().values().size() > 0) {
            ret.add("\n");
            ret.add(labels.getString("PERSONAGENS.LOCAL"));
            if (SettingsManager.getInstance().getConfig("HexInfoPcSorting", "N").equals("N")) {
                //sort by nation
                final List<Personagem> personagens = new ArrayList<>(local.getPersonagens().values());
                ComparatorFactory.getComparatorNationSorter(personagens);

                for (Personagem personagem : personagens) {
                    ret.add(PersonagemConverter.getInfo(personagem));
                }
            } else {
                //sort alphabetcaly
                for (Personagem personagem : local.getPersonagens().values()) {
                    ret.add(PersonagemConverter.getInfo(personagem));
                }
            }
        }
        //exercitos
        if (local.getExercitos().values().size() > 0) {
            ret.add("\n");
            ret.add(labels.getString("EXERCITOS"));
            for (Exercito exercito : local.getExercitos().values()) {
                ret.add(ExercitoConverter.getInfo(exercito));
            }
        }
        //artefatos
        if (local.getArtefatos().values().size() > 0) {
            ret.add("\n");
            ret.add(labels.getString("ARTEFATOS"));
            for (Artefato artefato : local.getArtefatos().values()) {
                ret.add(ArtefatoConverter.getInfo(artefato));
            }
        }
        return ret.getText();
    }

    public static GenericoTableModel getProdutoModel(Local hex) {
        GenericoTableModel produtoModel = new GenericoTableModel(
                getProdutoColNames(),
                getProdutosAsArray(hex),
                new Class[]{
                    java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
                });
        return produtoModel;
    }

    private static String[] getProdutoColNames() {
        //String[] colNames = {"Produto", "Produção", "Estoque", "Total"};
        String[] colNames = {labels.getString("PRODUTO"), labels.getString("PRODUCAO.FINAL"),
            labels.getString("VENDA"),
            labels.getString("PRODUCAO.CLIMA"), labels.getString("PRODUCAO.BASE")
        };
        return (colNames);
    }

    private static Object[][] getProdutosAsArray(Local hex) {
        try {
            int ii = 0;
            Iterator lista = localFacade.getProduction(hex).keySet().iterator();
            Object[][] ret = new Object[localFacade.getProduction(hex).keySet().size()][getProdutoColNames().length];
            while (lista.hasNext()) {
                Produto product = (Produto) lista.next();
                int i = 0;
                ret[ii][i++] = product.getNome();
                ret[ii][i++] = localFacade.getProduction(hex, product, scenario, turn);
                ret[ii][i++] = localFacade.getProductionSell(hex, product, mercado, scenario, turn);
                ret[ii][i++] = localFacade.getProductionClimate(hex, product);
                ret[ii][i++] = localFacade.getProductionNatural(hex, product);
                ii++;
            }
            return (ret);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public static GenericoTableModel getPresencasModel(Local local) {
        GenericoTableModel presencasModel = new GenericoTableModel(
                getPresencasColNames(),
                getPresencasAsArray(local),
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

    private static Object[][] getPresencasAsArray(Local local) {
        int ii = 0;
        Iterator lista = localFacade.listaPresencas(local).iterator();
        Object[][] ret = new Object[localFacade.listaPresencas(local).size()][getPresencasColNames().length];
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
                ret[ii][i++] = exercitoFacade.getComandanteTitulo(exercito, scenario);
                ret[ii][i++] = exercitoFacade.getNacaoNome(exercito);
                ret[ii][i++] = exercitoFacade.getDescricaoTamanho(exercito);
                ii++;
            } else {
                ret[ii][0] = labels.getString("?");
                ret[ii][1] = labels.getString("?");
                ret[ii][2] = labels.getString("OUTRO");
            }
        }
        return (ret);
    }

    private static String[] getLocalColNames(List<Class> classes) {
        List<String> colNames = new ArrayList<>(30);
        colNames.add(labels.getString("LOCAL"));
        classes.add(Local.class);
        colNames.add(labels.getString("CIDADE"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("NOME"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("TERRENO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("PRESENCAS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("CIDADE.DOCAS"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("TAMANHO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("FORTIFICACOES"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("NACAO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("CLIMA"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("VENDA.BEST"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("VENDA.BEST"));
        classes.add(java.lang.Integer.class);
        if (hasResourceManagement) {
            for (Produto product : scenario.getProdutos().values()) {
                colNames.add(product.getNome());
                classes.add(java.lang.Integer.class);
            }
        }

        return (colNames.toArray(new String[0]));
    }

    private static Object[] toArray(Local local) {
        int ii = 0;
        Object[] cArray = new Object[getLocalColNames(new ArrayList<>(30)).length];
        //start data structure definition
        cArray[ii++] = local;
        cArray[ii++] = cidadeFacade.getNome(local);
        cArray[ii++] = localFacade.getNome(local);
        cArray[ii++] = localFacade.getTerrenoNome(local);
        cArray[ii++] = localFacade.getPersonagens(local).size();
        cArray[ii++] = localFacade.getDocasNome(local);
        cArray[ii++] = localFacade.getTamanhoNome(local);
        cArray[ii++] = localFacade.getFortificacaoNome(local);
        cArray[ii++] = localFacade.getNacaoNome(local);
        cArray[ii++] = localFacade.getClima(local);
        cArray[ii++] = cidadeFacade.getResourceBestSell(local, mercado, scenario, turn);
        cArray[ii++] = localFacade.getProductionBestSell(local, mercado, scenario, turn);
        if (hasResourceManagement) {
            for (Produto product : scenario.getProdutos().values()) {
                cArray[ii++] = localFacade.getProduction(local, product, scenario, turn);
            }
        }
        return cArray;
    }

    private static Object[][] getLocalAsArray(List listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", false, false, "", "", "", "", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getLocalColNames(new ArrayList<>(30)).length];
            Iterator lista = listaExibir.iterator();
            while (lista.hasNext()) {
                Local local = (Local) lista.next();
                ret[ii++] = LocalConverter.toArray(local);
            }
            return (ret);
        }
    }

    public static List listaByFiltro(String filtro) {
        List<Local> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            ret.addAll(listFactory.listLocais().values());
        }
        return ret;
    }

    public static GenericoTableModel getLocalModel(List lista) {
        List<Class> classes = new ArrayList<>(30);
        GenericoTableModel cidadeModel = new GenericoTableModel(
                getLocalColNames(classes),
                getLocalAsArray(lista),
                classes.toArray(new Class[0]));
        return cidadeModel;
    }

}
