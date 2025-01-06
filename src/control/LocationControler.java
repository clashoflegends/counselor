/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import control.services.LocalConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabLocationsGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class LocationControler extends ControlBase implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(LocationControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabLocationsGui tabHexagonosGui;
    private List<Local> listaExibida;
    private int modelRowIndex = 0;

    public LocationControler(TabLocationsGui tabGui) {
        this.tabHexagonosGui = tabGui;
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_RELOAD);
//        registerDispatchManagerForMsg(DispatchManager.LOCAL_CITY_REDRAW);
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        listaExibida = LocalConverter.listaByFiltro(filtro.getComboId());
        this.mainTableModel = LocalConverter.getLocalModel(listaExibida);
        return this.mainTableModel;
    }

    public TabLocationsGui getTabGui() {
        return tabHexagonosGui;
    }

    public String getResultados(Local hex) {
        return LocalConverter.getInfo(hex);
    }

    public int getModelRowIndex() {
        return this.modelRowIndex;
    }

    @Override
    public void receiveDispatch(int msgName, Local local) {
        if (msgName == DispatchManager.LOCAL_CITY_REDRAW) {
            getTabGui().setMainModel(getMainTableModel(getTabGui().getFiltro()));
        }
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.ACTIONS_RELOAD) {
            getTabGui().setMainModel(getMainTableModel(getTabGui().getFiltro()));
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        try {
            JTable table = this.getTabGui().getMainLista();
            ListSelectionModel lsm = (ListSelectionModel) event.getSource();
            if (lsm.isSelectionEmpty()) {
                getTabGui().doMudaHexClear();
            } else {
                //testes
                int rowIndex = lsm.getAnchorSelectionIndex();
                modelRowIndex = table.convertRowIndexToModel(rowIndex);
                Local hex = (Local) listaExibida.get(modelRowIndex);

                getTabGui().doMudaHex(hex);
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JTable) {
            SysApoio.imp(labels.getString("OPS.JTABLE.EVENT"));
        } else if (e.getSource() instanceof JComboBox) {
//        if("comboBoxChanged") {
            JComboBox cb = (JComboBox) e.getSource();
            if ("comboFiltro".equals(cb.getName())) {
                SettingsManager.getInstance().setConfigAndSaveToFile(getTabGui().getKeyFilterProperty(), cb.getSelectedIndex() + "");
                getTabGui().setMainModel(getMainTableModel((GenericoComboObject) cb.getSelectedItem()));
            }
        } else if (e.getSource() instanceof JButton) {
            JButton jbTemp = (JButton) e.getSource();
            doActionPerformendButtom(jbTemp);
        } else if (e.getSource() instanceof JToggleButton) {
            JToggleButton jbTemp = (JToggleButton) e.getSource();
            doActionPerformedToggle(jbTemp);
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    private void doActionPerformendButtom(JButton jbTemp) {
        //implement stuff here when a buttom is added.
    }

    private void doActionPerformedToggle(JToggleButton jbTemp) {
        if (null == jbTemp.getActionCommand()) {
            log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
            return;
        }
        switch (jbTemp.getActionCommand()) {
            case "showCampRestriction":
                doDrawCampRestriction(jbTemp);
                break;
            default:
                log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
                break;
        }
    }

    private void doDrawCampRestriction(JToggleButton button) {
        int settingValue = button.isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("showCampRestriction", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("showCampRestriction");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }
}
