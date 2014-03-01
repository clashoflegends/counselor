/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import control.services.PartidaConverter;
import gui.tabs.TabPartidaGui;
import java.io.Serializable;
import javax.swing.ComboBoxModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class PartidaControler implements Serializable {

    private static final Log log = LogFactory.getLog(PartidaControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private TabPartidaGui tabGui;

    public PartidaControler(TabPartidaGui tabGui) {
        this.tabGui = tabGui;
    }

    public TabPartidaGui getTabGui() {
        return tabGui;
    }

    public GenericoTableModel getMainTableModel() {
        this.mainTableModel = PartidaConverter.getPartidaModel(WorldManager.getInstance().getPartida());
        return this.mainTableModel;
    }

    public ComboBoxModel listFiltro() {
        return new GenericoComboBoxModel(PartidaConverter.listFiltro());
    }
}
