/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.IBaseModel;
import business.facades.ListFactory;
import business.services.ComparatorBaseDisplayModelSorter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ComboBoxModel;
import model.Jogador;
import model.Nacao;
import model.TipoTropa;
import msgs.TitleFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class FiltroConverter implements Serializable {

    private static final Log log = LogFactory.getLog(FiltroConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final ListFactory listFactory = new ListFactory();

    public static ComboBoxModel getFiltroComboModelByJogador(Jogador jogadorAtivo, int options) {
        boolean filterMine = "1".equals(SettingsManager.getInstance().getConfig("filter.mine", "0"));
        List<IBaseModel> lista = new ArrayList<IBaseModel>();
        lista.add(new GenericoComboObject(labels.getString("FILTRO.TODOS"), "all"));
        lista.add(new GenericoComboObject(labels.getString("FILTRO.PROPRIOS"), "own"));
        if (options == 1) {
            //army options
            lista.add(new GenericoComboObject(labels.getString("FILTRO.ARMY.MEU"), "armymy"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.NAVY.MEU"), "navymy"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.GARRISON.MEU"), "garrisonmy"));
        }
        if (options == 5) {
            //army options
            lista.add(new GenericoComboObject(labels.getString("FILTRO.BIGCITY.MEU"), "bigcitymy"));
        }
        if (options == 6) {
            //Active options
            lista.add(new GenericoComboObject(labels.getString("FILTRO.ACTIVE"), "active"));
        }
        lista.add(new GenericoComboObject(labels.getString("FILTRO.ALLIES"), "allies"));
        lista.add(new GenericoComboObject(labels.getString("FILTRO.ENEMIES"), "enemies"));
        if (options == 2) {
            //char options
            lista.add(new GenericoComboObject(labels.getString("FILTRO.MYINFO"), "mypcinfo"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.CAPITAL"), "capital"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.ARMY"), "army"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.DOUBLES"), "double"));
            for (String skillName : TitleFactory.getTipoSkill()) {
                lista.add(new GenericoComboObject(labels.getString(skillName), skillName));
            }
        }
        if (options == 3) {
            lista.add(new GenericoComboObject(labels.getString("TROPA.FILTRO.CAVALARIA"), "fast"));
            lista.add(new GenericoComboObject(labels.getString("TROPA.FILTRO.INFANTARIA"), "regular"));
            lista.add(new GenericoComboObject(labels.getString("TROPA.FILTRO.LAND"), "land"));
            lista.add(new GenericoComboObject(labels.getString("TROPA.FILTRO.BARCOS"), "barcos"));
            lista.add(new GenericoComboObject(labels.getString("TROPA.FILTRO.SIEGE"), "siege"));
            lista.add(new GenericoComboObject(labels.getString("TROPA.FILTRO.TRASNFER"), "trasnfer"));
        }
        if (options > 0) {
            final List<Nacao> listNacao = new ArrayList<Nacao>(NacaoConverter.listNacoesDisponiveis(null).length);
            for (Nacao nacao : NacaoConverter.listNacoesDisponiveis(null)) {
                if (filterMine) {
                    if (nacao.getOwner() == jogadorAtivo) {
                        listNacao.add(nacao);
                    }
                } else {
                    listNacao.add(nacao);
                }
            }
            Collections.sort(listNacao, new ComparatorBaseDisplayModelSorter(false));
            lista.addAll(listNacao);
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new IBaseModel[0]), true);
        return model;
    }

    public static String[][] listFiltroLW() {
        String[][] ret = new String[3][2];
        int ii = 0;
        ret[ii][0] = labels.getString("FILTRO.TODOS"); //Display
        ret[ii++][1] = "Todos"; //Id
        ret[ii][0] = labels.getString("TROPA.FILTRO.LAND"); //Display
        ret[ii++][1] = "Land"; //Id
        ret[ii][0] = labels.getString("TROPA.FILTRO.BARCOS"); //Display
        ret[ii++][1] = "Barcos"; //Id
        return ret;
    }

    public static List<TipoTropa> listaByFiltroCasualty(String filtro) {
        List<TipoTropa> tropas = new ArrayList();
        if (filtro.equalsIgnoreCase("Todos")) {
            //todos
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                tropas.add(tpTropa);
            }
        } else if (filtro.equalsIgnoreCase("Land")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (!tpTropa.isBarcos()) {
                    tropas.add(tpTropa);
                }
            }
        } else if (filtro.equalsIgnoreCase("Barcos")) {
            for (TipoTropa tpTropa : listFactory.listTropas()) {
                if (tpTropa.isBarcos()) {
                    tropas.add(tpTropa);
                }
            }
        }
        return tropas;
    }

    public static GenericoComboBoxModel getTipoPersonagemComboModel() {
        String[][] itens = FiltroConverter.listTipoPersonagem();
        return new GenericoComboBoxModel(itens);
    }

    public static String[][] listTipoPersonagem() {
        String[][] ret = new String[TitleFactory.getTipoPersonagem().length + 1][2];
        int ii = 0;
        ret[ii][0] = labels.getString("TODOS"); //Display
        ret[ii++][1] = "Todos"; //Id
        for (String elem : TitleFactory.getTipoPersonagem()) {
            ret[ii][0] = elem;
            ret[ii++][1] = elem;
        }
        return ret;
    }

}
