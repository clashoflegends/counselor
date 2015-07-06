/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.converter.ConverterFactory;
import business.services.ComparatorFactory;
import control.services.CenarioConverter;
import control.services.TipoTropaConverter;
import gui.subtabs.SubTabCasualtyGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import model.Local;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class CasualtyControler implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(CasualtyControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final SubTabCasualtyGui tabGui;
    private List<TipoTropa> listaExibida;
    private final Local local;

    public CasualtyControler(SubTabCasualtyGui tabGui, Local local) {
        this.tabGui = tabGui;
        this.local = local;
    }

    private SubTabCasualtyGui getTabGui() {
        return tabGui;
    }

    public GenericoTableModel getMainTableModel(String filtro, String tactic) {
        if (tactic.equalsIgnoreCase("pa")) {
            listaExibida = new ArrayList<TipoTropa>();
        } else {
            listaExibida = TipoTropaConverter.listaByFiltroCasualty(filtro);
        }
        //sort array
        ComparatorFactory.getComparatorCasualtiesTipoTropaSorter(listaExibida, ConverterFactory.taticaToInt(tactic), local.getTerreno());
        this.mainTableModel = TipoTropaConverter.getCasualtyModel(listaExibida, local.getTerreno());
        return this.mainTableModel;
    }

    public ComboBoxModel listFiltro() {
        return new GenericoComboBoxModel(TipoTropaConverter.listFiltroLW());
    }

    public ComboBoxModel listFiltroTactic() {
        return CenarioConverter.getInstance().getTaticaComboModel();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) event.getSource();
            if ("comboFiltro".equals(cb.getActionCommand())) {
                getTabGui().setMainModel(getMainTableModel(getTabGui().getFiltro(), getTabGui().getFiltroTactic()));
            }
        }
    }
}
