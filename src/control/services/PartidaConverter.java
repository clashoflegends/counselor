/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoTableModel;
import business.facade.CenarioFacade;
import control.PartidaControler;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import model.Cenario;
import model.Habilidade;
import model.Partida;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class PartidaConverter implements Serializable {

    private static final Log log = LogFactory.getLog(PartidaControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static String[][] listFiltro() {
        String[][] ret = new String[1][2];
        ret[0][0] = labels.getString("FILTRO.TODOS"); //Display
        ret[0][1] = "Todos"; //Id
        return ret;
    }

    public static GenericoTableModel getPartidaModel(Partida partida) {
        GenericoTableModel model = new GenericoTableModel(
                getPartidaColNames(),
                getPartidaAsArray(partida),
                new Class[]{
                    java.lang.String.class, java.lang.String.class
                });
        return model;
    }

    private static String[] getPartidaColNames() {
        String[] colNames = {
            labels.getString("NOME"), labels.getString("VALOR")};
        return (colNames);
    }

    private static Object[][] getPartidaAsArray(Partida partida) {
        final CenarioFacade cenarioFacade = new CenarioFacade();
        int ii = 0;
        // Converte Partida para um Array[] 
        List<String[]> lista = new ArrayList<String[]>();
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.NOME"), partida.getNome()});
        final Cenario cenario = partida.getCenario();
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.CENARIO"), cenario.getNome()});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.TURNO"), partida.getTurno() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.TURNO.PROXIMO"), partida.getTurnoNext() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.TURNO.MAX"), partida.getTurnoMax() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.MAXPERS"), cenario.getNumMaxPersonagem() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.MAXMOV"), cenario.getNumMaxMovimento() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.MINORDENS"), cenario.getNumOrdensPers() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.MAXORDENS"), cenario.getNumMaxOrdens() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.MAXARTI"), cenario.getNumMaxArtefatos() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.MONEY"), cenario.getMoney().getNome()});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.NUMACTIONS"), cenario.getOrdens().values().size() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.NUMFEITICOS"), cenario.getFeiticos().size() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.NUMTROPAS"), cenario.getTipoTropas().values().size() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.NUMPRODUTO"), cenario.getProdutos().values().size() + ""});
        lista.add(new String[]{labels.getString("PARTIDA.LABEL.NUMTATICAS"), cenarioFacade.listTaticas(cenario).length + ""});
        for (Habilidade hab : partida.getHabilidades().values()) {
            lista.add(new String[]{labels.getString("PARTIDA.LABEL.HABGAME"), hab.getNome()});
        }
        for (Habilidade hab : cenario.getHabilidades().values()) {
            lista.add(new String[]{labels.getString("PARTIDA.LABEL.HABCENARIO"), hab.getNome()});
        }
        Set<Habilidade> habList = new TreeSet<Habilidade>();
        for (TipoTropa tpTropa : cenario.getTipoTropas().values()) {
            for (Habilidade habilidade : tpTropa.getHabilidades().values()) {
                habList.add(habilidade);
            }
        }
        for (Habilidade habilidade : habList) {
            lista.add(new String[]{labels.getString("PARTIDA.LABEL.HABTROPA"), habilidade.getNome()});
        }
//        lista.add(new String[]{labels.getString("PARTIDA.LABEL."), partida.getCenario()});
        return (lista.toArray(new String[0][0]));
    }
}
