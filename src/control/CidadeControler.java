/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import baseLib.SysApoio;
import control.services.CidadeConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabCidadesGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Cidade;
import model.Local;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class CidadeControler extends ControlBase implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(CidadeControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabCidadesGui tabCidadesGui;
    private List listaExibida;

    public CidadeControler(TabCidadesGui tabGui) {
        this.tabCidadesGui = tabGui;
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_RELOAD);
//        registerDispatchManagerForMsg(DispatchManager.LOCAL_CITY_REDRAW);
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        Nacao nacao = null;
        try {
            nacao = (Nacao) filtro.getObject();
            listaExibida = CidadeConverter.listaByNacao(nacao);
        } catch (ClassCastException e) {
            listaExibida = CidadeConverter.listaByFiltro(filtro.getComboId());
        }
        this.mainTableModel = CidadeConverter.getCidadeModel(listaExibida);
        return this.mainTableModel;
    }

    public TabCidadesGui getTabGui() {
        return tabCidadesGui;
    }

    public String getResultados(Cidade cidade) {
        return CidadeConverter.getResultados(cidade);
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
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JTable) {
            SysApoio.imp(labels.getString("OPS.JTABLE.EVENT"));
        } else if (e.getSource() instanceof JComboBox) {
//        if("comboBoxChanged") {
            JComboBox cb = (JComboBox) e.getSource();
            if ("comboFiltro".equals(cb.getName())) {
                getTabGui().setMainModel(getMainTableModel((GenericoComboObject) cb.getSelectedItem()));
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
            if (lsm.isSelectionEmpty()) {
                getTabGui().doMudaCidadeClear();
            } else {
                //testes
                int rowIndex = lsm.getAnchorSelectionIndex();
                int modelIndex = table.convertRowIndexToModel(rowIndex);
                Cidade cidade = (Cidade) listaExibida.get(modelIndex);
                getTabGui().doMudaCidade(cidade);
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }
}
