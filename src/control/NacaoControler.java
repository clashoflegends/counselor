/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import control.services.NacaoConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabNacoesGui;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class NacaoControler extends ControlBase implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(NacaoControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabNacoesGui tabNacaoGui;
    private List<Nacao> listaExibida;
    private int modelRowIndex = 0;

    public NacaoControler(TabNacoesGui tabGui) {
        this.tabNacaoGui = tabGui;
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_RELOAD);
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        listaExibida = NacaoConverter.listaByFiltro(filtro.getComboId());
        this.mainTableModel = NacaoConverter.getNacaoModel(listaExibida);
        return this.mainTableModel;
    }

    public GenericoTableModel getRelacionamentoTableModel(Nacao nacao) {
        if (nacao == null) {
            return (null);
        } else {
            GenericoTableModel nacaoModel = NacaoConverter.getRelacionamentoModel(nacao);
            return (nacaoModel);
        }
    }

    public GenericoTableModel getTropaTableModel(Nacao nacao) {
        if (nacao == null) {
            return (null);
        } else {
            GenericoTableModel nacaoModel = NacaoConverter.getTropaModel(nacao);
            return (nacaoModel);
        }
    }

    public TabNacoesGui getTabGui() {
        return tabNacaoGui;
    }

    public int getModelRowIndex() {
        return this.modelRowIndex;
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.ACTIONS_RELOAD) {
            getTabGui().setMainModel(getMainTableModel(getTabGui().getFiltro()));
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) event.getSource();
            if ("comboFiltro".equals(cb.getName())) {
                SettingsManager.getInstance().setConfigAndSaveToFile(getTabGui().getKeyFilterProperty(), cb.getSelectedIndex() + "");
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
                modelRowIndex = table.convertRowIndexToModel(rowIndex);
                Nacao nacao = (Nacao) listaExibida.get(modelRowIndex);
                getTabGui().doMudaNacao(nacao);
                //PENDING atualizar table mensagens
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }
}
