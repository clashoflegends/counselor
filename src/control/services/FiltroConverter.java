/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.IBaseModel;
import baseLib.SysProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import model.Jogador;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class FiltroConverter implements Serializable {

    private static final Log log = LogFactory.getLog(FiltroConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static ComboBoxModel getFiltroComboModelByJogador(Jogador jogadorAtivo, int options) {
        boolean filterMine = "1".equals(SysProperties.getProps("filter.mine", "0"));
        List<IBaseModel> lista = new ArrayList<IBaseModel>();
        lista.add(new GenericoComboObject(labels.getString("FILTRO.TODOS"), "all"));
        lista.add(new GenericoComboObject(labels.getString("FILTRO.PROPRIOS"), "own"));
        if (options == 1) {
            //army options
            lista.add(new GenericoComboObject(labels.getString("FILTRO.ARMY.MEU"), "armymy"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.NAVY.MEU"), "navymy"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.GARRISON.MEU"), "garrisonmy"));
        }
        lista.add(new GenericoComboObject(labels.getString("FILTRO.ALLIES"), "allies"));
        lista.add(new GenericoComboObject(labels.getString("FILTRO.ENEMIES"), "enemies"));
        if (options == 2) {
            //char options
            lista.add(new GenericoComboObject(labels.getString("FILTRO.CAPITAL"), "capital"));
            lista.add(new GenericoComboObject(labels.getString("FILTRO.ARMY"), "army"));
        }
        for (Nacao nacao : NacaoConverter.listNacoesDisponiveis(null)) {
            if (filterMine) {
                if (nacao.getOwner() == jogadorAtivo) {
                    lista.add(nacao);
                }
            } else {
                lista.add(nacao);
            }
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new IBaseModel[0]), true);
        return model;
    }
}
