/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import control.services.CenarioConverter;
import control.services.TipoTropaConverter;
import gui.tabs.TabTipoTropasGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Nacao;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class TipoTropaControler implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(TipoTropaControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabTipoTropasGui tabGui;
    private List<TipoTropa> listaExibida;

    public TipoTropaControler(TabTipoTropasGui tabGui) {
        this.tabGui = tabGui;
    }

    public TabTipoTropasGui getTabGui() {
        return tabGui;
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        Nacao nacao = null;
        try {
            nacao = (Nacao) filtro.getObject();
            listaExibida = TipoTropaConverter.listaByNacao(nacao);
        } catch (ClassCastException ex) {
            listaExibida = TipoTropaConverter.listaByFiltro(filtro.getComboId());
        }
        this.mainTableModel = TipoTropaConverter.getTropaModel(listaExibida);
        return this.mainTableModel;
    }

    public GenericoTableModel getAtaqueTableModel(List<TipoTropa> lista) {
        return TipoTropaConverter.getTerrainTableModel(lista, 0);
    }

    public GenericoTableModel getDefesaTableModel(List<TipoTropa> lista) {
        return TipoTropaConverter.getTerrainTableModel(lista, 1);
    }

    public GenericoTableModel getTaticaTableModel() {
        return CenarioConverter.getInstance().getTaticaTableModel();
    }

    public GenericoTableModel getMovimentacaoTableModel(List<TipoTropa> lista) {
        return TipoTropaConverter.getTerrainTableModel(lista, 2);
    }

    public GenericoTableModel getHabilidadeTableModel(TipoTropa tpTropa) {
        return TipoTropaConverter.getHabilidadeTableModel(tpTropa);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) event.getSource();
            if ("comboFiltro".equals(cb.getActionCommand())) {
                final GenericoComboObject elem = (GenericoComboObject) cb.getSelectedItem();
                getTabGui().setMainModel(getMainTableModel(elem));
            }
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
            if (!lsm.isSelectionEmpty()) {
                //testes
                int rowIndex = lsm.getAnchorSelectionIndex();
                int modelIndex = table.convertRowIndexToModel(rowIndex);
                TipoTropa tpTropa = (TipoTropa) listaExibida.get(modelIndex);
                getTabGui().doMuda(listaExibida, tpTropa);
            }
        } catch (RuntimeException ex) {
            // Was catch(IndexOutOfBoundsException) with an empty body: any failure building the troop
            // sub-tab models (e.g. the terrain-array overflow that left every terrain sub-tab blank in
            // game 886) was swallowed silently -> empty panes with no trace. Log it now so it is visible;
            // the terrain models are also sized defensively in TipoTropaConverter so this should not fire.
            log.warn("Failed to refresh troop sub-tabs on selection: " + ex, ex);
        }
    }
}
