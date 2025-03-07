/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import control.services.AcaoConverter;
import control.services.FiltroConverter;
import control.services.PersonagemConverter;
import gui.tabs.TabOrdensGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import model.Ordem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class OrdemJogadorControler implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(OrdemJogadorControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabOrdensGui tabGui;
    private List<PersonagemOrdem> listaExibida;

    public OrdemJogadorControler(TabOrdensGui tabGui) {
        this.tabGui = tabGui;
    }

    public String getAjuda(Ordem ordem) {
        return AcaoConverter.getAjuda(ordem);
    }

    public GenericoTableModel getMainTableModel(String filtro) {
        this.listaExibida = PersonagemConverter.listaOrdensBySequence();
        this.mainTableModel = PersonagemConverter.getOrdemModel(listaExibida);
        return this.mainTableModel;
    }

    public TabOrdensGui getTabGui() {
        return tabGui;
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
                PersonagemOrdem personagem = (PersonagemOrdem) listaExibida.get(modelIndex);
                getTabGui().doMudaAcao(personagem);
                //PENDING atualizar table mensagens
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    public ComboBoxModel getFiltroComboModel() {
        return FiltroConverter.getFiltroComboModelAll();
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
