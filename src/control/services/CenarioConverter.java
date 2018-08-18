/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import baseLib.GenericoTableModelPerCell;
import baseLib.IBaseModel;
import business.facade.CenarioFacade;
import business.services.ComparatorFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import javax.swing.ComboBoxModel;
import model.Cenario;
import model.Habilidade;
import model.Nacao;
import model.Produto;
import model.Raca;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class CenarioConverter implements Serializable {

    private static final Log log = LogFactory.getLog(CenarioConverter.class);
    private static CenarioConverter instance;
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static Cenario cenario;

    private CenarioConverter() {
    }

    public static synchronized CenarioConverter getInstance() {
        if (CenarioConverter.instance == null) {
            CenarioConverter.instance = new CenarioConverter();
        }
        CenarioConverter.cenario = WorldManager.getInstance().getCenario();
        return CenarioConverter.instance;
    }

    /**
     *
     * @param tipo 0=todos, 1=0-ouro, 2=Weapon, 3=Armor
     * @return
     */
    public GenericoComboBoxModel getProdutoComboModel(int tipo) {
        Produto[] items = cenarioFacade.listProdutos(cenario, tipo);
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public GenericoComboBoxModel getTaticaComboModel() {
        String[][] items = cenarioFacade.listTaticas(cenario);
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public GenericoTableModel getTaticaTableModel() {
        SortedMap<Integer, String> taticas = cenarioFacade.listTaticasAsList(cenario);
        GenericoTableModel model = new GenericoTableModel(
                getTaticaColNames(taticas),
                getTaticaAsArray(taticas),
                new Class[]{
                    java.lang.String.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class
                });
        return model;
    }

    private String[] getTaticaColNames(SortedMap<Integer, String> taticas) {
        final List<String> colNames = new ArrayList<String>();
        colNames.add(labels.getString("TATICA"));
        for (String value : taticas.values()) {
            colNames.add(value);
        }
        return (colNames.toArray(new String[0]));
    }

    private Object[][] getTaticaAsArray(SortedMap<Integer, String> lista) {
        if (lista.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        }
        Object[][] ret = new Object[lista.size()][lista.size() + 1];
        int ii = 0;
        for (Integer keyA : lista.keySet()) {
            int nn = 0;
            ret[ii][nn++] = lista.get(keyA);
            for (Integer keyB : lista.keySet()) {
                ret[ii][nn++] = cenarioFacade.getTaticaBonus(cenario, keyA, keyB);
            }
            ii++;
        }
        return (ret);
    }

    public ComboBoxModel getTropaTipoComboModel(Raca racaCidade, Raca racaNacao) {
        List<TipoTropa> lista = new ArrayList<TipoTropa>();
        lista.addAll(cenarioFacade.getTipoTropas(cenario, racaCidade, racaNacao));
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new TipoTropa[0]));
        return model;
    }

    public ComboBoxModel getTropaTipoComboModel() {
        List<TipoTropa> lista = new ArrayList<TipoTropa>();
        lista.addAll(cenarioFacade.getTipoTropas(cenario));
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new TipoTropa[0]));
        return model;
    }

    public List<TipoTropa> getTropaTipo() {
        List<TipoTropa> lista = new ArrayList<TipoTropa>();
        lista.addAll(cenarioFacade.getTipoTropas(cenario));
        return lista;
    }

    public boolean hasCidadeOrdens() {
        return !CenarioFacade.isSw(cenario);
    }

    public static String[][] listFiltro() {
        String[][] ret = new String[1][2];
        ret[0][0] = labels.getString("FILTRO.TODOS"); //Display
        ret[0][1] = "Todos"; //Id
        return ret;
    }

    public static GenericoTableModel getPackageModel(List<Habilidade> packages, List<Nacao> nacoes) {
        GenericoTableModelPerCell model = new GenericoTableModelPerCell(
                getPackageColNames(),
                getPackageAsArray(packages, nacoes),
                new Class[]{
                    Habilidade.class, java.lang.Integer.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class,
                    java.lang.Boolean.class, java.lang.Boolean.class
                });
        model.setEditable(getPackageEditable(packages, nacoes));
        return model;
    }

    private static String[] getPackageColNames() {
        String[] colNames = new String[WorldManager.getInstance().getNacoesJogadorAtivo().size() + 2];
        int ii = 0;
        colNames[ii++] = labels.getString("NOME");
        colNames[ii++] = labels.getString("CUSTO");
        for (Nacao nacao : WorldManager.getInstance().getNacoesJogadorAtivo()) {
            colNames[ii++] = nacao.getNome();
        }
        return (colNames);
    }

    private static Object[][] getPackageAsArray(List<Habilidade> listaExibir, List<Nacao> nacoes) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getPackageColNames().length];
            ComparatorFactory.getComparatorComboDisplaySorter(listaExibir);
            // Converte habilidades para um Array[] 
            for (Habilidade hab : listaExibir) {
                int nn = 0;
                ret[ii][nn++] = hab;
                ret[ii][nn++] = hab.getCost();
                for (Nacao nacao : nacoes) {
                    ret[ii][nn++] = (Boolean) nacao.hasHabilidade(hab.getCodigo());
                }
                ii++;
            }
            return (ret);
        }
    }

    private static boolean[][] getPackageEditable(List<Habilidade> listaExibir, List<Nacao> nacoes) {
        if (listaExibir.isEmpty()) {
            boolean[][] ret = {{false, false}};
            return (ret);
        } else {
            int ii = 0;
            boolean[][] editMap = new boolean[listaExibir.size()][getPackageColNames().length];
            ComparatorFactory.getComparatorComboDisplaySorter(listaExibir);
            // Converte habilidades para um Array[] 
            for (Habilidade hab : listaExibir) {
                List<Habilidade> filters = new ArrayList<Habilidade>();
                for (Habilidade superAbilities : hab.getHabilidades().values()) {
                    //add hab filters
                    if (superAbilities.isFilter()) {
                        filters.add(superAbilities);
                    }
                }
                int nn = 0;
                editMap[ii][nn++] = false;
                editMap[ii][nn++] = false;
                for (Nacao nacao : nacoes) {
                    //if hab filter matches nation filter
                    if (filters.isEmpty()) {
                        editMap[ii][nn] = true;
                    } else {
                        for (Habilidade habilidade : nacao.getHabilidades().values()) {
                            //only shows if nation has the same filter
                            if (filters.contains(habilidade)) {
                                editMap[ii][nn] = true;
                                break;
                            } else {
                                editMap[ii][nn] = false;
                            }
                        }
                    }
                    nn++;
                }
                ii++;
            }
            return (editMap);
        }
    }

    public GenericoComboBoxModel getTerrainComboModel() {
        final IBaseModel[] items = cenarioFacade.listTerrains(cenario);
        final GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }
}
