/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import control.services.ExercitoConverter;
import gui.tabs.TabExercitosGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Exercito;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class ExercitoControler implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(ExercitoControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabExercitosGui tabExercitosGui;
    private List listaExibida;

    public ExercitoControler(TabExercitosGui tabGui) {
        this.tabExercitosGui = tabGui;
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        Nacao nacao;
        try {
            nacao = (Nacao) filtro.getObject();
            listaExibida = ExercitoConverter.listaByNacao(nacao);
        } catch (ClassCastException e) {
            listaExibida = ExercitoConverter.listaByFiltro(filtro.getComboId());
        }
        this.mainTableModel = ExercitoConverter.getExercitoModel(listaExibida);
        return this.mainTableModel;
    }

    public GenericoTableModel getPelotaoTableModel(Exercito exercito) {
        return ExercitoConverter.getPelotaoModel(exercito);
    }

    public TabExercitosGui getTabGui() {
        return tabExercitosGui;
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
                Exercito exercito = (Exercito) listaExibida.get(modelIndex);
                getTabGui().doMudaExercito(exercito);
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) actionEvent.getSource();
            doActionPerformedCombobox(cb);
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    private void doActionPerformedCombobox(JComboBox cb) {
        if (null == cb.getActionCommand()) {
            log.info(labels.getString("NOT.IMPLEMENTED") + cb.getActionCommand());
            return;
        }
        switch (cb.getActionCommand()) {
            case "comboFiltro":
                SettingsManager.getInstance().setConfigAndSaveToFile(getTabGui().getKeyFilterProperty(), cb.getSelectedIndex() + "");
                getTabGui().setMainModel(getMainTableModel((GenericoComboObject) cb.getSelectedItem()));
                break;
            default:
                log.info(labels.getString("NOT.IMPLEMENTED") + cb.getActionCommand());
                break;
        }
    }
}
