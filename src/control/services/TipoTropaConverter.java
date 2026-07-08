/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoTableModel;
import baseLib.IBaseModel;
import business.facade.NacaoFacade;
import persistence.local.ListFactory;
import control.facade.WorldFacadeCounselor;
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
            final Set<TipoTropa> tropas = new TreeSet<>();
            for (Nacao nacao : nacoes) {
                tropas.addAll(nacaoFacade.getTropas(nacao).keySet());
            }
            ret.addAll(tropas);
        } else if (filtro.equalsIgnoreCase("team") && jAtivo != null) {
            final Set<TipoTropa> tropas = new TreeSet<>();
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(ally) || jAtivo.isNacao(ally)) {
                    tropas.addAll(nacaoFacade.getTropas(ally).keySet());
                }
            }
            ret.addAll(tropas);
        } else if (filtro.equalsIgnoreCase("allies") && jAtivo != null) {
            final Set<TipoTropa> tropas = new TreeSet<>();
            for (Nacao ally : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(ally) && !jAtivo.isNacao(ally)) {
                    tropas.addAll(nacaoFacade.getTropas(ally).keySet());
                }
            }
            ret.addAll(tropas);
        } else if (filtro.equalsIgnoreCase("enemies") && jAtivo != null) {
            final Set<TipoTropa> tropas = new TreeSet<>();
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
        } else if (filtro.equalsIgnoreCase("active")) {
            final Set<TipoTropa> tropas = new TreeSet<>();
            for (Nacao nation : listFactory.listNacoes().values()) {
                if (nacaoFacade.isAtiva(nation)) {
                    tropas.addAll(nacaoFacade.getTropas(nation).keySet());
                }
            }
            ret.addAll(tropas);
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
        return new ArrayList<>(nacaoFacade.getTropas(filtro).keySet());
    }

    public static IBaseModel[] listFiltroTroopHab() {
        List<IBaseModel> lista = new ArrayList<>();
        Set<Habilidade> habList = new TreeSet<>();
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

    /** The per-terrain value map for the requested sub-tab: 0=attack, 1=defense, else=movement. */
    private static SortedMap<Terreno, Integer> terrenoMap(TipoTropa tpTropa, int tipo) {
        switch (tipo) {
            case 0:
                return tpTropa.getAtaqueTerreno();
            case 1:
                return tpTropa.getDefesaTerreno();
            default:
                return tpTropa.getMovimentoTerreno();
        }
    }

    public static GenericoTableModel getTerrainTableModel(List<TipoTropa> lista, int tipo) {
        if (lista == null || lista.isEmpty()) {
            return null; // caller (TabTipoTropasGui.setXxxModel) renders its empty fallback
        }
        // Size the table to the WIDEST troop's terrain map for this tab. Troops can have non-uniform terrain
        // sets (game 886 has 2 troops with 10 terrains while the rest have 9); the old code sized columns from
        // troop[0]'s attack map and then wrote each troop's own values, so a wider troop overran the row array
        // -> ArrayIndexOutOfBounds, which TipoTropaControler.valueChanged swallowed as an IndexOutOfBounds ->
        // every terrain sub-tab silently stayed empty. Size to the max + bound each row so nothing overflows.
        TipoTropa widest = lista.get(0);
        int maxN = 0;
        for (TipoTropa t : lista) {
            final SortedMap<Terreno, Integer> m = terrenoMap(t, tipo);
            final int n = (m == null) ? 0 : m.size();
            if (n > maxN) {
                maxN = n;
                widest = t;
            }
        }
        final String[] colNames = getTerrainColNames(widest, tipo, maxN);
        final Class[] classes = new Class[colNames.length];
        classes[0] = java.lang.String.class;
        for (int c = 1; c < classes.length; c++) {
            classes[c] = java.lang.Integer.class;
        }
        return new GenericoTableModel(colNames, getTerrainAsArray(lista, tipo, colNames.length), classes);
    }

    private static String[] getTerrainColNames(TipoTropa widest, int tipo, int maxN) {
        final List<String> colNames = new ArrayList<>();
        colNames.add(labels.getString("TROPA.NOME"));
        final SortedMap<Terreno, Integer> m = terrenoMap(widest, tipo);
        if (m != null) {
            for (Terreno terreno : m.keySet()) {
                colNames.add(terreno == null ? "?" : terreno.getNome());
            }
        }
        while (colNames.size() < maxN + 1) { // pad if the widest map had null keys
            colNames.add("?");
        }
        return (colNames.toArray(new String[0]));
    }

    private static Object[][] getTerrainAsArray(List<TipoTropa> lista, int tipo, int cols) {
        Object[][] ret = new Object[lista.size()][cols];
        int ii = 0;
        for (TipoTropa tpTropa : lista) {
            int nn = 0;
            ret[ii][nn++] = tpTropa.getNome();
            // Iterate values() directly (not keySet()+get(): a reference-keyed TreeMap can miss get() after
            // EGF deserialize). Bound by cols so a troop with more terrains than the header never overflows.
            final SortedMap<Terreno, Integer> listaTerreno = terrenoMap(tpTropa, tipo);
            if (listaTerreno != null) {
                for (Integer valor : listaTerreno.values()) {
                    if (nn >= cols) {
                        break;
                    }
                    ret[ii][nn++] = (valor == null || valor == 999) ? 0 : valor;
                }
            }
            ii++;
        }
        return (ret);
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
        final List<String> colNames = new ArrayList<>();
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
        // get() may return null (terrain absent from this troop's map, or a reference-keyed TreeMap whose
        // lookup misses after EGF deserialization). The old "!= 999" auto-unboxed null -> NPE. Null-safe now.
        final Integer ret = listaTerreno.get(terreno);
        return (ret == null || ret == 999) ? 0 : ret;
    }
}
