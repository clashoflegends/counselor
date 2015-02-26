/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import control.services.TipoTropaConverter;
import gui.subtabs.SubTabTroopHabGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class TroopHabControler implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(TroopHabControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private SubTabTroopHabGui tabGui;
    private List<TipoTropa> listaExibida;

    public TroopHabControler(SubTabTroopHabGui tabGui) {
        this.tabGui = tabGui;
    }

    private SubTabTroopHabGui getTabGui() {
        return tabGui;
    }

    public GenericoTableModel getMainTableModel(String filtro) {
        listaExibida = TipoTropaConverter.listaByFiltroHab(filtro);
        //sort array
        this.mainTableModel = TipoTropaConverter.getTropaModel(listaExibida);
        return this.mainTableModel;
    }

    public ComboBoxModel listFiltro() {
        return new GenericoComboBoxModel(TipoTropaConverter.listFiltroTroopHab());
    }


    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) event.getSource();
            if ("comboFiltro".equals(cb.getActionCommand())) {
                GenericoComboObject elem = (GenericoComboObject) cb.getSelectedItem();
                getTabGui().setMainModel(getMainTableModel(elem.getComboId()));
            }
        }
    }
}
