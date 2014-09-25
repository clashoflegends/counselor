/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.*;
import business.facades.ListFactory;
import business.facades.WorldFacade;
import java.io.Serializable;
import java.util.*;
import model.*;
import msgs.BaseMsgs;
import msgs.TitleFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import persistence.local.WorldManager;
import utils.StringRet;

/**
 *
 * @author Gurgel
 */
public class ExercitoConverter implements Serializable {

    public static final int FILTRO_PROPRIOS = 1;
    public static final int FILTRO_TODOS = 0;
    private static final Log log = LogFactory.getLog(ExercitoConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final Cenario cenario = WorldFacade.getInstance().getCenario();

    public static String getDisplayDetalhes(Exercito exercito) {
        if (exercito == null) {
            return "";
        }
        String ret = "";
        //personagens viajando com o comandante?
        SortedMap<String, Personagem> liderados = exercitoFacade.getLiderados(exercito);
        if (liderados != null && liderados.size() > 0) {
            String msg = labels.getString("PERSONAGENS.VIAJANDO.COM.EXERCITO") + "\n";
            for (Personagem personagem : liderados.values()) {
                msg += String.format("   - %s\n", personagem.getNome());
            }
            ret += msg;
        }
        Local local = exercitoFacade.getLocal(exercito);
        //cidade no local?
        if (localFacade.isCidade(local, exercitoFacade.getNacao(exercito))) {
            Cidade cidade = localFacade.getCidade(local);
            ret += String.format(labels.getString("CIDADE.AVISTADO") + "\n",
                    cidadeFacade.getTamanhoFortificacao(cidade),
                    cidadeFacade.getNome(cidade),
                    cidadeFacade.getNacaoNome(cidade));
        }
        //outro exercito no local?
        SortedMap<String, Exercito> exercitos = localFacade.getExercitos(local);
        for (Exercito elem : exercitos.values()) {
            //Um exército portando o estandarte da nação Esparta sob o comando do(a) Capitão Dilios esta aqui. 
            if (elem != exercito) {
                if (exercitoFacade.isGuarnicao(exercito)) {
                    ret += String.format(labels.getString("GUARNICAO.AVISTADO"),
                            exercitoFacade.getDescricaoTamanho(elem),
                            exercitoFacade.getNacaoNome(elem));
                } else if (exercitoFacade.isEsquadra(exercito) || exercitoFacade.getTamanhoEsquadra(exercito) > 0) {
                    ret += String.format(labels.getString("ESQUADRA.AVISTADO"),
                            exercitoFacade.getDescricaoTamanho(elem),
                            exercitoFacade.getNacaoNome(elem),
                            exercitoFacade.getComandanteTitulo(elem, WorldFacade.getInstance().getCenario()));
                } else {
                    ret += String.format(labels.getString("EXERCITO.AVISTADO"),
                            exercitoFacade.getDescricaoTamanho(elem),
                            exercitoFacade.getNacaoNome(elem),
                            exercitoFacade.getComandanteTitulo(elem, WorldFacade.getInstance().getCenario()));
                }
                ret += "\n";
            }
        }
        return ret;
    }

    private static Collection<Pelotao> listTropasTipoAll(Exercito exercito) {
        List<Pelotao> ret = new ArrayList<Pelotao>();
        for (TipoTropa tipoTropa : WorldManager.getInstance().getCenario().getTipoTropas().values()) {
            Pelotao pelotao;
            try {
                if (exercito.getPelotoes().get(tipoTropa.getCodigo()) != null) {
                    pelotao = exercito.getPelotoes().get(tipoTropa.getCodigo());
                } else {
                    pelotao = new Pelotao();
                    pelotao.setTipoTropa(tipoTropa);
                }
            } catch (NullPointerException ex) {
                pelotao = new Pelotao();
                pelotao.setTipoTropa(tipoTropa);
            }
            ret.add(pelotao);
        }
        return ret;
    }

    private static Collection<Pelotao> listTropasTipoTransfer(Exercito exercito) {
        List<Pelotao> ret = new ArrayList<Pelotao>();
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (pelotao.getTipoTropa().isTransferable()) {
                ret.add(pelotao);
            }
        }
        return ret;
    }

    private static Object[] toArray(Exercito exercito) {
        int ii = 0;
        Object[] cArray = new Object[getExercitoColNames().length];
        cArray[ii++] = exercitoFacade.getComandanteTitulo(exercito, WorldFacade.getInstance().getCenario());
        cArray[ii++] = exercitoFacade.getNacaoNome(exercito);
        Local local = exercitoFacade.getLocal(exercito);
        cArray[ii++] = localFacade.getCoordenadas(local);
        cArray[ii++] = exercitoFacade.getTropasCavalaria(exercito);
        cArray[ii++] = exercitoFacade.getTropasInfantaria(exercito);
        cArray[ii++] = exercitoFacade.getEsquadra(exercito);
        cArray[ii++] = exercitoFacade.getComida(exercito);
        cArray[ii++] = exercitoFacade.getUpkeepFood(exercito);
        cArray[ii++] = exercitoFacade.getMoral(exercito);
        cArray[ii++] = exercitoFacade.getUpkeepCost(exercito);
        cArray[ii++] = exercitoFacade.getSiege(exercito);
        cArray[ii++] = exercitoFacade.getAtaqueExercito(exercito, true);
        cArray[ii++] = exercitoFacade.getDefesaExercito(exercito, true);
        cArray[ii++] = exercitoFacade.getAtaqueExercito(exercito, false);
        cArray[ii++] = exercitoFacade.getDefesaExercito(exercito, false);
        if (cenarioFacade.hasShips(cenario)) {
            cArray[ii++] = exercitoFacade.getTransportesMinimo(exercito);
        } else {
            cArray[ii++] = "-";
        }
        return cArray;
    }

    public static GenericoTableModel getExercitoModel(List lista) {
        GenericoTableModel exercitoModel =
                new GenericoTableModel(getExercitoColNames(), getExercitosAsArray(lista),
                new Class[]{
            java.lang.String.class, java.lang.String.class, Local.class,
            java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
        });
        return exercitoModel;
    }

    private static String[] getExercitoColNames() {
        String[] colNames = {
            labels.getString("COMANDANTE"), labels.getString("NACAO"), labels.getString("LOCAL"),
            labels.getString("CAVALARIAS"), labels.getString("INFANTARIAS"), labels.getString("NAVIOS"),
            labels.getString("COMIDA"), labels.getString("COMIDA.CONSUMO"), labels.getString("MORAL"),
            labels.getString("CUSTO.MANUTENCAO"), labels.getString("MAQUINAS.GUERRA"),
            labels.getString("TROPA.ATAQUE.NAVAL"), labels.getString("TROPA.DEFESA.NAVAL"),
            labels.getString("TROPA.ATAQUE.TERRA"), labels.getString("TROPA.DEFESA.TERRA"), labels.getString("TRANSPORTE.MINIMO")
        };
        //"Terreno", "Clima","Moral","Transportes necessários","Movimentação", "CavPes", "CavLev", "InfPes", "InfLev", "Arq", "Escravos","Navios de Guerra", "Transportes", 
        return (colNames);
    }

    private static Object[][] getExercitosAsArray(List listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getExercitoColNames().length];
            Iterator lista = listaExibir.iterator();
            while (lista.hasNext()) {
                Exercito exercito = (Exercito) lista.next();
                // Converte um Exercito para um Array[] 
                ret[ii++] = ExercitoConverter.toArray(exercito);
            }
            return (ret);
        }
    }

    public static GenericoTableModel getInfoModel(Exercito exercito) {
        return new GenericoTableModel(getInfoColNames(), getInfoAsArray(exercito),
                new Class[]{java.lang.String.class, java.lang.String.class});
    }

    private static String[] getInfoColNames() {
        String[] colNames = {labels.getString("NOME"), labels.getString("VALOR")};
        return (colNames);
    }

    private static Object[][] getInfoAsArray(Exercito exercito) {
        if (exercito == null) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[5][getPelotaoColNames().length];
            try {
                ret[ii][0] = labels.getString("MORAL");
                ret[ii++][1] = exercito.getMoral() + "";
                if (cenarioFacade.hasShips(cenario)) {
                    ret[ii][0] = labels.getString("TRANSPORTE.MINIMO");
                    ret[ii++][1] = exercitoFacade.getTransportesMinimo(exercito) + "";
                }
                ret[ii][0] = labels.getString("MOVIMENTACAO");
                if (exercito.isMovimentacaoEvasiva()) {
                    ret[ii++][1] = BaseMsgs.tipoMovimentacao[1];
                } else {
                    ret[ii++][1] = BaseMsgs.tipoMovimentacao[0];
                }
                ret[ii][0] = labels.getString("TERRENO");
                ret[ii++][1] = exercitoFacade.getTerreno(exercito);
                ret[ii][0] = labels.getString("CLIMA");
                ret[ii++][1] = exercitoFacade.getClima(exercito);
            } catch (IndexOutOfBoundsException ex) {
            }
            return (ret);
        }
    }

    public static GenericoTableModel getPelotaoModel(Exercito exercito) {
        return new GenericoTableModel(getPelotaoColNames(), getPelotaoAsArray(exercito),
                new Class[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class,
            java.lang.Integer.class, java.lang.Integer.class
        });
    }

    private static String[] getPelotaoColNames() {
        String[] colNames = {labels.getString("NOME"), labels.getString("QTD"),
            labels.getString("TREINO"), labels.getString("ARMA"),
            labels.getString("ARMADURA"), labels.getString("TIPO"),
            labels.getString("TROPA.ATAQUE"), labels.getString("TROPA.DEFESA")};
        //String[] colNames = {"Nome", "Quantidade", "Treino", "Arma", "Armadura", "Tipo"};
        return (colNames);
    }

    private static Object[][] getPelotaoAsArray(Exercito exercito) {
        Collection<Pelotao> listaExibir = exercito.getPelotoes().values();
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getPelotaoColNames().length];
            for (Pelotao pelotao : listaExibir) {
                int nn = 0;
                ret[ii][nn++] = exercitoFacade.getNomeRaca(exercito, pelotao);
                ret[ii][nn++] = pelotao.getQtd();
                ret[ii][nn++] = pelotao.getTreino();
                ret[ii][nn++] = pelotao.getModAtaque();
                ret[ii][nn++] = pelotao.getModDefesa();
                ret[ii][nn++] = pelotao.getNome();
                ret[ii][nn++] = exercitoFacade.getAtaquePelotao(pelotao, exercito);
                ret[ii][nn++] = exercitoFacade.getDefesaPelotao(pelotao, exercito);
                ii++;
            }
            return (ret);
        }
    }

    public static List<Exercito> listaByNacao(Nacao filtro) {
        ListFactory listFactory = new ListFactory();
        List<Exercito> ret = new ArrayList();
        for (Exercito exercito : listFactory.listExercitos().values()) {
            if (filtro == null) {
                ret.add(exercito);
            } else if (filtro == exercito.getNacao()) {
                ret.add(exercito);
            }
        }
        return ret;
    }

    public static List<Exercito> listaByFiltro(String filtro) {
        ListFactory listFactory = new ListFactory();
        List<Exercito> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            ret.addAll(listFactory.listExercitos().values());
        } else if (filtro.equalsIgnoreCase("own")) {
            Jogador jativo = WorldFacade.getInstance().getJogadorAtivo();
            for (Exercito exercito : listFactory.listExercitos().values()) {
                try {
                    if (jativo.isNacao(exercito.getNacao())) {
                        ret.add(exercito);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("armymy")) {
            Jogador jativo = WorldFacade.getInstance().getJogadorAtivo();
            for (Exercito exercito : listFactory.listExercitos().values()) {
                try {
                    if (jativo.isNacao(exercito.getNacao()) && !exercitoFacade.isEsquadra(exercito)) {
                        ret.add(exercito);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("navymy")) {
            Jogador jativo = WorldFacade.getInstance().getJogadorAtivo();
            for (Exercito exercito : listFactory.listExercitos().values()) {
                try {
                    if (jativo.isNacao(exercito.getNacao()) && exercitoFacade.isEsquadra(exercito)) {
                        ret.add(exercito);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("garrisonmy")) {
            Jogador jativo = WorldFacade.getInstance().getJogadorAtivo();
            for (Exercito exercito : listFactory.listExercitos().values()) {
                try {
                    if (jativo.isNacao(exercito.getNacao()) && exercitoFacade.isGuarnicao(exercito)) {
                        ret.add(exercito);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("allies")) {
            Jogador jAtivo = WorldFacade.getInstance().getJogadorAtivo();
            for (Exercito exercito : listFactory.listExercitos().values()) {
                try {
                    if (jAtivo.isJogadorAliado(exercito.getNacao()) && !jAtivo.isNacao(exercito.getNacao())) {
                        ret.add(exercito);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("enemies")) {
            Jogador jAtivo = WorldFacade.getInstance().getJogadorAtivo();
            for (Exercito exercito : listFactory.listExercitos().values()) {
                try {
                    if (!jAtivo.isJogadorAliado(exercito.getNacao()) && !jAtivo.isNacao(exercito.getNacao())) {
                        ret.add(exercito);
                    }
                } catch (NullPointerException e) {
                }
            }
        }
        return ret;
    }

    public static GenericoTableModel getTropaTipoTableModel(Exercito exercito, SortedMap<String, Integer> vlInicial, int filtro) {
        GenericoTableModel model = new GenericoTableModel(
                getTropaTipoColNames(),
                getTropaTiposAsArray(exercito, vlInicial, filtro),
                new Class[]{
            java.lang.String.class, java.lang.Integer.class
        });
        boolean[] edit = new boolean[]{false, true};
        model.setEditable(edit);
        return model;
    }

    private static String[] getTropaTipoColNames() {
        String[] colNames = {labels.getString("TROPA"), labels.getString("QTD")};
        return (colNames);
    }

    /**
     *
     * @param exercito
     * @param vlInicial
     * @param filtro: all=0, current=1, transferable=2
     * @return
     */
    private static Object[][] getTropaTiposAsArray(Exercito exercito, SortedMap<String, Integer> vlInicial, int filtro) {
        Collection<Pelotao> list;
        if (filtro == 1) {
            try {
                list = exercito.getPelotoes().values();
            } catch (NullPointerException ex) {
                //no garrison at teh scene.
                list = new ArrayList<Pelotao>();
            }
        } else if (filtro == 2) {
            list = listTropasTipoTransfer(exercito);
        } else {
            list = listTropasTipoAll(exercito);
        }
        Object[][] ret = new Object[list.size()][getTropaTipoColNames().length];
        int ii = 0;
        for (Pelotao pelotao : list) {
            Integer qtd = 0;
            int nn = 0;
            //ret[ii][i++] = exercitoFacade.getNomeRaca(exercito, pelotao);
            //ret[ii][i++] = pelotao.getQtd();
            ret[ii][nn++] = pelotao;
            try {
                qtd = Math.max(0, vlInicial.get(pelotao.getCodigo()));
            } catch (NullPointerException e) {
                //qtd=0;
            }
            ret[ii][nn++] = qtd;
            ii++;
        }
        return (ret);
    }

    public static List<String> getInfo(Exercito exercito) {
        StringRet ret = new StringRet();
        /**
         * Um pequeno exército portando o estandarte do Rei-Fogo sob o comando
         * do(a) Comandante Tertis está Aqui.
         */
        ret.addTab(TitleFactory.displayExercitotitulo(exercito));
        return ret.getList();
    }

    public static GenericoComboBoxModel getTropaTipoComboModel(Exercito exercito, boolean water) {
        ExercitoFacade ef = new ExercitoFacade();
        //prep list
        Collection<TipoTropa> list = new ArrayList<TipoTropa>(WorldManager.getInstance().getCenario().getTipoTropas().size());
        //monta a lista de tropas
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            final TipoTropa tpTropa = pelotao.getTipoTropa();
            if (water && ef.isAgua(tpTropa)) {
                list.add(tpTropa);
            } else if (!water && !ef.isAgua(tpTropa)) {
                list.add(tpTropa);
            }
        }
        //se todos, ou se lista vazia.
        if (list.isEmpty()) {
            return getTropaTipoComboModelAll(water);
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(list.toArray(new TipoTropa[0]));
        return model;
    }

    public static GenericoComboBoxModel getTropaTipoComboModelAll(boolean water) {
        ExercitoFacade ef = new ExercitoFacade();
        //prep list
        Collection<TipoTropa> list = new ArrayList<TipoTropa>(WorldManager.getInstance().getCenario().getTipoTropas().size());
        //monta a lista de tropas
        for (TipoTropa tpTropa : WorldManager.getInstance().getCenario().getTipoTropas().values()) {
            if (water && ef.isAgua(tpTropa)) {
                list.add(tpTropa);
            } else if (!water && !ef.isAgua(tpTropa)) {
                list.add(tpTropa);
            }
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(list.toArray(new TipoTropa[0]));
        return model;

    }

    public static GenericoTableModel getBattleModel(List<ExercitoSim> lista) {
        GenericoTableModel exercitoModel =
                new GenericoTableModel(getBattleColNames(), getBattleAsArray(lista),
                new Class[]{
            java.lang.String.class,
            java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class,
            java.lang.String.class
        });
        return exercitoModel;
    }

    private static String[] getBattleColNames() {
        String[] colNames = {
            labels.getString("COMANDANTE"),
            labels.getString("TROPA.ATAQUE.NAVAL"), labels.getString("TROPA.DEFESA.NAVAL"),
            labels.getString("TROPA.ATAQUE.TERRA"), labels.getString("TROPA.DEFESA.TERRA"),
            labels.getString("NACAO")
        };
        return (colNames);
    }

    private static Object[][] getBattleAsArray(List<ExercitoSim> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getExercitoColNames().length];
            for (ExercitoSim exercito : listaExibir) {
                ret[ii++] = ExercitoConverter.battleToArray(exercito);
            }
            return (ret);
        }
    }

    private static Object[] battleToArray(ExercitoSim exercito) {
        int ii = 0;
        Object[] cArray = new Object[getExercitoColNames().length];
        cArray[ii++] = exercitoFacade.getComandanteTitulo(exercito, WorldFacade.getInstance().getCenario());
        cArray[ii++] = exercitoFacade.getAtaqueExercito(exercito, true);
        cArray[ii++] = exercitoFacade.getDefesaExercito(exercito, true);
        cArray[ii++] = exercitoFacade.getAtaqueExercito(exercito, false);
        cArray[ii++] = exercitoFacade.getDefesaExercito(exercito, false);
        cArray[ii++] = exercitoFacade.getNacaoNome(exercito);
        return cArray;
    }
}
