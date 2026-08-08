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
import model.Mercado;
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

    /** Which tab a nations table is being built for; they show the same facts in a different order. */
    public enum Layout {
        /** Nations tab: everything, resources as QUANTITY, startup-package points right after the name. */
        NATIONS,
        /** Finances tab: money first, resources as the GOLD they would fetch, no startup-package column. */
        FINANCES
    }

    /** One table column: its header, its type, and how to read it off a nation. */
    private static final class Col {

        private final String header;
        private final Class<?> type;
        private final java.util.function.Function<Nacao, Object> value;

        Col(String header, Class<?> type, java.util.function.Function<Nacao, Object> value) {
            this.header = header;
            this.type = type;
            this.value = value;
        }
    }

    public static GenericoTableModel getNacaoModel(List<Nacao> lista) {
        return getNacaoModel(lista, Layout.NATIONS);
    }

    public static GenericoTableModel getNacaoModel(List<Nacao> lista, Layout layout) {
        final List<Col> cols = columns(layout);
        final String[] names = new String[cols.size()];
        final Class[] classes = new Class[cols.size()];
        for (int cc = 0; cc < cols.size(); cc++) {
            names[cc] = cols.get(cc).header;
            classes[cc] = cols.get(cc).type;
        }
        final Object[][] data = new Object[lista.size()][cols.size()];
        for (int rr = 0; rr < lista.size(); rr++) {
            for (int cc = 0; cc < cols.size(); cc++) {
                data[rr][cc] = cols.get(cc).value.apply(lista.get(rr));
            }
        }
        return new GenericoTableModel(names, data, classes);
    }

    /**
     * The columns of a nations table, in display order.
     * <p>
     * Two are conditional on the scenario: the turn-0 startup-package points (only while nation orders
     * are open, and only on the Nations tab - the Finances tab has no use for them) and the capital
     * coordinates (only in scenarios with capitals). Everything else is always present, so a layout is
     * just a different order over the same list.
     */
    private static List<Col> columns(Layout layout) {
        final Cenario cenario = WFC.getCenario();
        final boolean hasStartupPoints = cenarioFacade.hasOrdensNacao(WFC.getPartida());
        final boolean hasCapitals = WFC.hasCapitals();

        final Col name = new Col(labels.getString("NOME"), String.class, n -> nacaoFacade.getNome(n));
        final Col startup = new Col(labels.getString("STARTUP.POINTS"), Integer.class, n -> acaoFacade.getPointsSetup(n));
        final Col alliance = new Col(labels.getString("ALIANCA"), String.class, n -> nacaoFacade.getTeamFlag(n));
        final Col culture = new Col(labels.getString("RACA"), String.class, n -> nacaoFacade.getRacaNome(n));
        final Col active = new Col(labels.getString("ATIVA"), String.class,
                n -> SysApoio.iif(nacaoFacade.isAtiva(n), labels.getString("ATIVA"), labels.getString("INATIVA")));
        final Col capital = new Col(labels.getString("CIDADE.CAPITAL"), Local.class, n -> nacaoFacade.getCoordenadasCapital(n));
        final Col charSlots = new Col(labels.getString("PERSONAGENS.SLOT"), Integer.class, n -> nacaoFacade.getPersonagensSlot(n, cenario));
        final Col charKnown = new Col(labels.getString("PERSONAGENS.KNOWN"), Integer.class, n -> nacaoFacade.getPersonagens(n));
        final Col loyalty = new Col(labels.getString("LEALDADE"), Integer.class, n -> nacaoFacade.getLealdade(n));
        final Col loyaltyVar = new Col(labels.getString("LEALDADE.VARIACAO"), Integer.class, n -> nacaoFacade.getLealdadeAnterior(n));
        final Col troops = new Col(labels.getString("TROPAS"), Integer.class, n -> nacaoFacade.getTropasQt(n, WFC.getExercitos()));
        final Col victory = new Col(labels.getString("PONTOS.VITORIA"), Integer.class, n -> nacaoFacade.getPointVictory(n));
        final Col domination = new Col(labels.getString("PONTOS.DOMINATION"), Integer.class, n -> nacaoFacade.getPointsDomination(n));
        final Col taxes = new Col(labels.getString("IMPOSTOS"), Integer.class, n -> nacaoFacade.getImpostos(n));
        final Col treasury = new Col(labels.getString("TREASURY"), Integer.class, n -> nacaoFacade.getMoneySaldo(n));
        // Refreshed cell-by-cell as orders are entered - see refreshOrderCostCells. Zero for nations
        // that are not yours, the only ones you cannot enter orders for.
        final Col ordersCost = new Col(labels.getString("FINANCAS.COST.ACTIONS"), Integer.class, n -> WFC.getNacaoOrderCost(n) * -1);
        final Col forecast = new Col(labels.getString("FINANCAS.FORECAST.COLUMN"), Integer.class, n -> FinancasConverter.getForecastBalance(n));
        final Col player = new Col(labels.getString("JOGADOR"), String.class, n -> nacaoFacade.getJogadorDisplay(n));
        final Col email = new Col(labels.getString("JOGADOR.EMAIL"), String.class, n -> nacaoFacade.getJogadorEmail(n));

        final List<Col> ret = new ArrayList<>(30);
        if (layout == Layout.FINANCES) {
            ret.add(name);
            ret.add(alliance);
            ret.add(taxes);
            ret.add(treasury);
            ret.add(ordersCost);
            ret.add(forecast);
            ret.addAll(resourceColumns(layout, cenario));
            ret.add(loyalty);
            ret.add(loyaltyVar);
            ret.add(troops);
            ret.add(victory);
            ret.add(domination);
            ret.add(culture);
            ret.add(active);
            if (hasCapitals) {
                ret.add(capital);
            }
            ret.add(charSlots);
            ret.add(charKnown);
        } else {
            ret.add(name);
            if (hasStartupPoints) {
                //turn-0 spending sits where the player looks first, and only while it is being spent
                ret.add(startup);
            }
            ret.add(alliance);
            ret.add(culture);
            ret.add(active);
            if (hasCapitals) {
                ret.add(capital);
            }
            ret.add(charSlots);
            ret.add(charKnown);
            ret.add(loyalty);
            ret.add(loyaltyVar);
            ret.add(troops);
            ret.add(victory);
            ret.add(domination);
            ret.add(taxes);
            ret.add(treasury);
            ret.add(ordersCost);
            ret.add(forecast);
            ret.addAll(resourceColumns(layout, cenario));
        }
        ret.add(player);
        ret.add(email);
        return ret;
    }

    /**
     * One column per tradeable resource. The Nations tab counts units (stock plus expected production);
     * the Finances tab is about money, so it shows what selling all of them would fetch at the current
     * market price - the same figure the Market sub-tab totals per resource. Falls back to units in a
     * scenario with no market to price them against.
     */
    private static List<Col> resourceColumns(Layout layout, Cenario cenario) {
        final List<Col> ret = new ArrayList<>(10);
        final Mercado mercado = WFC.getMercado();
        final boolean asGold = layout == Layout.FINANCES && mercado != null;
        for (Produto produto : cenarioFacade.listProdutos(cenario, 1)) {
            final Produto p = produto;
            ret.add(new Col(p.getNome(), Integer.class, n -> {
                final int units = nacaoFacade.getProducao(n, p, cenario, WFC.getTurno()) + nacaoFacade.getEstoque(n, p);
                return asGold ? units * mercado.getProdutoVlVenda(p) : units;
            }));
        }
        return ret;
    }

    /**
     * Model index of the startup-package points column, or -1 when it is not displayed. Resolved by
     * header rather than a constant: it is only present while nation orders are open, and it sits in a
     * different place per layout, so a fixed index would paint or overwrite the wrong column.
     */
    public static int getStartupPointsColumn(javax.swing.table.TableModel model) {
        final String header = labels.getString("STARTUP.POINTS");
        for (int col = 0; model != null && col < model.getColumnCount(); col++) {
            if (header.equals(model.getColumnName(col))) {
                return col;
            }
        }
        return -1;
    }

    /**
     * Refreshes the order-cost and balance-after-orders cells for one nation, in place.
     * <p>
     * The nations table model is a SNAPSHOT: it is built once (tab construction or a filter change)
     * and never rebuilt, and on an EGF open it is built BEFORE the orders file is loaded - so without
     * this the two columns sit at zero for the whole session. Columns are matched by header name
     * because the column set is conditional (nation orders, capitals, one per product), so a fixed
     * index would hit a different column in some scenarios.
     *
     * @return true when a cell was actually updated
     */
    public static boolean refreshOrderCostCells(GenericoTableModel model, List<Nacao> rows, Nacao nacao) {
        if (model == null || rows == null || nacao == null) {
            return false;
        }
        final int row = rows.indexOf(nacao);
        if (row < 0 || row >= model.getRowCount()) {
            return false; //nation filtered out of the current view
        }
        final int cost = WFC.getNacaoOrderCost(nacao) * -1;
        boolean ret = setCell(model, row, labels.getString("FINANCAS.COST.ACTIONS"), cost);
        ret |= setCell(model, row, labels.getString("FINANCAS.FORECAST.COLUMN"), FinancasConverter.getForecastBalance(nacao));
        return ret;
    }

    /** Refreshes every displayed row, for when a whole order set has just been loaded or cleared. */
    public static void refreshOrderCostCells(GenericoTableModel model, List<Nacao> rows) {
        if (rows == null) {
            return;
        }
        for (Nacao nacao : rows) {
            refreshOrderCostCells(model, rows, nacao);
        }
    }

    /** Sets a cell by column header, if that column is present. DefaultTableModel repaints itself. */
    private static boolean setCell(GenericoTableModel model, int row, String columnHeader, Object value) {
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (columnHeader.equals(model.getColumnName(col))) {
                model.setValueAt(value, row, col);
                return true;
            }
        }
        return false;
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
