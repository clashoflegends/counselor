/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.IBaseModel;
import business.facades.WorldFacadeCounselor;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabWorldBuilderGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import model.Local;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class WorldBuilderControl extends ControlBase implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(WorldBuilderControl.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private TabWorldBuilderGui tabGui;
    private Local local;

    public WorldBuilderControl(TabWorldBuilderGui aTabGui) {
        this.tabGui = aTabGui;
        registerDispatchManagerForMsg(DispatchManager.LOCAL_MAP_CLICK);
    }

    @Override
    public void receiveDispatch(int msgName, Local aLocal) {
        if (msgName == DispatchManager.LOCAL_MAP_CLICK) {
            local = aLocal;
            getTabGui().setCoordenada(aLocal.getCoordenadas());
            getTabGui().setTerrenoModel(getTerrenoComboModel(), aLocal.getTerreno());
        }
    }

    private static GenericoComboBoxModel getTerrenoComboModel() {
        List<IBaseModel> lista = new ArrayList<IBaseModel>();
        for (IBaseModel elem : WorldFacadeCounselor.getInstance().getCenario().getTerrenos().values()) {
            lista.add(elem);
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new IBaseModel[0]));
        return model;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (actionEvent.getSource() instanceof JButton) {
            log.info(labels.getString("OPS.JBUTTON.EVENT"));
        } else if (actionEvent.getSource() instanceof JComboBox) {
            actionOnTabGui(actionEvent);
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    private void actionOnTabGui(ActionEvent actionEvent) {
        JComboBox jcbTerrain = (JComboBox) actionEvent.getSource();
        if ("jcbTerrain".equals(jcbTerrain.getActionCommand())) {
            try {
                final GenericoComboObject obj = (GenericoComboObject) jcbTerrain.getModel().getSelectedItem();
                local.setTerreno((Terreno) obj.getObject());
                local.setChanged(true);
                //redraw mapa
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW, local);
            } catch (NullPointerException ex) {
                //nao faz nada, ordens nao disponiveis...
            }
        }
    }


    /**
     * @return the tabGui
     */
    public TabWorldBuilderGui getTabGui() {
        return tabGui;
    }

    /**
     * @param tabGui the tabGui to set
     */
    public void setTabGui(TabWorldBuilderGui tabGui) {
        this.tabGui = tabGui;
    }

}
