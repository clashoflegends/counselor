/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoTableModel;
import baseLib.IBaseModel;
import business.facade.NacaoFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import model.Habilidade;
import model.Jogador;
import model.Nacao;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class TipoTropaConverter implements Serializable {

    private static final Log log = LogFactory.getLog(TipoTropaConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final ListFactory listFactory = new ListFactory();
    private static final NacaoFacade nacaoFacade = new NacaoFacade();

    public static List<TipoTropa> listaByFiltro(String filtro) {
        final Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
        final List<TipoTropa> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            //todos
            ret.addAll(listFactory.listTropas());
        } else if (filtro.equalsIgnoreCase("own")) {
            final Collection<Nacao> nacoes = jAtivo.getNacoes().values();
            final Set<TipoTropa> tropas = new TreeSet<TipoTropa>();
            for (Nacao nacao : nacoes) {
                tropas.addAll(nacaoFacade.getTropas(nacao).keySet());
            }
            ret.addAll(tropas);
        } else if (filtro.equalsIgnoreCase("allies") && jAtivo != null) {
            final Set<TipoTropa> tropas = new TreeSet<TipoTropa>();
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(ally) && !jAtivo.isNacao(ally)) {
                    tropas.addAll(nacaoFacade.getTropas(ally).keySet());
                }
            }
            ret.addAll(tropas);
        } else if (filtro.equalsIgnoreCase("enemies") && jAtivo != null) {
            final Set<TipoTropa> tropas = new TreeSet<TipoTropa>();
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (!jAtivo.isJogadorAliado(ally) && !jAtivo.isNacao(ally)) {
                    tropas.addAll(nacaoFacade.getTropas(ally).keySet());
                }
            }
            ret.addAll(tropas);
        } else if (filtro.equalsIgnoreCase("fast")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (tpTropa.isFastMovement()) {
                    ret.add(tpTropa);
                }
            }
        } else if (filtro.equalsIgnoreCase("regular")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (!tpTropa.isFastMovement()) {
                    ret.add(tpTropa);
                }
            }
        } else if (filtro.equalsIgnoreCase("trasnfer")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (tpTropa.isTransferable()) {
                    ret.add(tpTropa);
                }
            }
        } else if (filtro.equalsIgnoreCase("siege")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (tpTropa.isSiege()) {
                    ret.add(tpTropa);
                }
            }
        } else if (filtro.equalsIgnoreCase("land")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (!tpTropa.isBarcos()) {
                    ret.add(tpTropa);
                }
            }
        } else if (filtro.equalsIgnoreCase("barcos")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (tpTropa.isBarcos()) {
                    ret.add(tpTropa);
                }
            }
        }
        return ret;
    }

    public static List<TipoTropa> listaByNacao(Nacao filtro) {
        return new ArrayList<TipoTropa>(nacaoFacade.getTropas(filtro).keySet());
    }

    public static IBaseModel[] listFiltroTroopHab() {
        List<IBaseModel> lista = new ArrayList<IBaseModel>();
        Set<Habilidade> habList = new TreeSet<Habilidade>();
        for (TipoTropa tpTropa : listFactory.listTropas()) {
            for (Habilidade habilidade : tpTropa.getHabilidades().values()) {
                habList.add(habilidade);
            }
        }
        for (Habilidade habilidade : habList) {
            lista.add(habilidade);
        }
        return lista.toArray(new IBaseModel[0]);
    }

    public static List<TipoTropa> listaByFiltroHab(String filtro) {
        List<TipoTropa> tropas = new ArrayList();
        for (TipoTropa tpTropa : listFactory.listTropas()) {
            if (tpTropa.hasHabilidade(filtro)) {
                tropas.add(tpTropa);
            }
        }
        return tropas;
    }

    public static GenericoTableModel getTropaModel(List<TipoTropa> lista) {
        GenericoTableModel model = new GenericoTableModel(
                getTipoTropaColNames(),
                getTipoTropaAsArray(lista),
                new Class[]{
                    java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class
                });
        return model;
    }

    private static String[] getTipoTropaColNames() {
        String[] colNames = {
            labels.getString("TROPA.NOME"),
            labels.getString("TROPA.RECRUIT.MONEY"), labels.getString("TROPA.UPKEEP.MONEY"),
            labels.getString("TROPA.UPKEEP.FOOD"),
            labels.getString("TROPA.RECRUIT.TIME"), labels.getString("TROPA.MOVIMENTO")};
        return (colNames);
    }

    private static Object[][] getTipoTropaAsArray(List<TipoTropa> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getTipoTropaColNames().length];
            for (TipoTropa tpTropa : listaExibir) {
                // Converte TipoTropa para um Array[] 
                int nn = 0;
                ret[ii][nn++] = tpTropa.getNome();
                ret[ii][nn++] = tpTropa.getRecruitCostMoney();
                ret[ii][nn++] = tpTropa.getUpkeepMoney();
                ret[ii][nn++] = tpTropa.getUpkeepFood();
                ret[ii][nn++] = tpTropa.getRecruitCostTime();
                ret[ii][nn++] = tpTropa.getMovimento();
                ii++;
            }
            return (ret);
        }
    }

    public static GenericoTableModel getCasualtyModel(List<TipoTropa> listaExibida, Terreno terreno) {
        GenericoTableModel model = new GenericoTableModel(
                getCasualtyColNames(),
                getCasualtyAsArray(listaExibida, terreno),
                new Class[]{
                    java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class
                });
        return model;
    }

    private static String[] getCasualtyColNames() {
        String[] colNames = {
            labels.getString("TROPA.NOME"),
            labels.getString("TROPA.MOVIMENTACAO"),
            labels.getString("TROPA.ATAQUE"), labels.getString("TROPA.DEFESA"),
            labels.getString("TROPA.RECRUIT.MONEY"), labels.getString("TROPA.UPKEEP.MONEY"),
            labels.getString("TROPA.UPKEEP.FOOD"),
            labels.getString("TROPA.RECRUIT.TIME"), labels.getString("TROPA.MOVIMENTO")
        };
        return (colNames);
    }

    private static Object[][] getCasualtyAsArray(List<TipoTropa> listaExibir, Terreno terreno) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getCasualtyColNames().length];
            for (TipoTropa tpTropa : listaExibir) {
                // Converte TipoTropa para um Array[] 
                int nn = 0;
                ret[ii][nn++] = tpTropa.getNome();
                ret[ii][nn++] = getTerrenoValue(tpTropa.getMovimentoTerreno(), terreno);
                ret[ii][nn++] = getTerrenoValue(tpTropa.getAtaqueTerreno(), terreno);
                ret[ii][nn++] = getTerrenoValue(tpTropa.getDefesaTerreno(), terreno);
                ret[ii][nn++] = tpTropa.getRecruitCostMoney();
                ret[ii][nn++] = tpTropa.getUpkeepMoney();
                ret[ii][nn++] = tpTropa.getUpkeepFood();
                ret[ii][nn++] = tpTropa.getRecruitCostTime();
                ret[ii][nn++] = tpTropa.getMovimento();
                ii++;
            }
            return (ret);
        }
    }

    public static GenericoTableModel getTerrainTableModel(List<TipoTropa> lista, int tipo) {
        final String[] colNames = getTerrainColNames(lista.get(0));
        GenericoTableModel model = new GenericoTableModel(
                colNames,
                getTerrainAsArray(lista, tipo, colNames.length),
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

    private static String[] getTerrainColNames(TipoTropa tpTropa) {
        final List<String> colNames = new ArrayList<String>();
        colNames.add(labels.getString("TROPA.NOME"));
        for (Terreno terreno : tpTropa.getAtaqueTerreno().keySet()) {
            colNames.add(terreno.getNome());
        }
        return (colNames.toArray(new String[0]));
    }

    private static Object[][] getTerrainAsArray(List<TipoTropa> lista, int tipo, int size) {
        if (lista.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            Object[][] ret = new Object[lista.size()][size];
            SortedMap<Terreno, Integer> listaTerreno;
            int ii = 0;
            for (TipoTropa tpTropa : lista) {
                if (tipo == 0) {
                    listaTerreno = tpTropa.getAtaqueTerreno();
                } else if (tipo == 1) {
                    listaTerreno = tpTropa.getDefesaTerreno();
                } else {
                    listaTerreno = tpTropa.getMovimentoTerreno();
                }
                int nn = 0;
                ret[ii][nn++] = tpTropa.getNome();
                for (Terreno terreno : listaTerreno.keySet()) {
                    ret[ii][nn++] = getTerrenoValue(listaTerreno, terreno);
                }
                ii++;
            }
            return (ret);
        }
    }

    public static GenericoTableModel getHabilidadeTableModel(TipoTropa tpTropa) {
        final String[] colNames = getHabilidadeColNames();
        GenericoTableModel model = new GenericoTableModel(
                colNames,
                getHabilidadeAsArray(tpTropa.getHabilidades().values()),
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

    private static String[] getHabilidadeColNames() {
        final List<String> colNames = new ArrayList<String>();
        colNames.add(labels.getString("TROPA.HABILIDADE"));
        return (colNames.toArray(new String[0]));
    }

    private static Object[][] getHabilidadeAsArray(Collection<Habilidade> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getHabilidadeColNames().length];
            for (Habilidade hab : listaExibir) {
                // Converte TipoTropa para um Array[] 
                int nn = 0;
                ret[ii][nn++] = hab.getNome();
                //ret[ii][nn++] = hab.getValor();
                ii++;
            }
            return (ret);
        }
    }

    private static int getTerrenoValue(SortedMap<Terreno, Integer> listaTerreno, Terreno terreno) {
        int ret;
        // Converte TipoTropa para um Array[] 
        if (listaTerreno.get(terreno) != 999) {
            ret = listaTerreno.get(terreno);
        } else {
            ret = 0;
        }
        return ret;
    }
}
