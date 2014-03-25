/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import control.services.CenarioConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabPackagesGui;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import model.Habilidade;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class PackageControler extends ControlBase implements Serializable, TableModelListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(PackageControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private TabPackagesGui tabGui;
    private List<Habilidade> listaExibida = new ArrayList();

    public PackageControler(TabPackagesGui tabGui) {
        this.tabGui = tabGui;
        registerDispatchManagerForMsg(DispatchManager.PACKAGE_RELOAD);
    }

    public TabPackagesGui getTabGui() {
        return tabGui;
    }

    public GenericoTableModel getMainTableModel() {
        listaExibida.clear();
        listaExibida.addAll(WorldManager.getInstance().getPackages().values());
        this.mainTableModel = CenarioConverter.getPackageModel(listaExibida, WorldManager.getInstance().getNacoesJogadorAtivo());
        return this.mainTableModel;
    }

    public ComboBoxModel listFiltro() {
        return new GenericoComboBoxModel(CenarioConverter.listFiltro());
    }

    public String getAjuda(Habilidade hab) {
        String ret = "";
        for (Habilidade habilidade : hab.getHabilidades().values()) {
            ret += habilidade.getNome() + "\n";
        }
        return ret;
    }

    private void doMarkPackage(String nacaoName, final Boolean addHab, final Habilidade hab) {
        Nacao nacaoAlvo = null;
        for (Nacao nacao : WorldManager.getInstance().getNacoesJogadorAtivo()) {
            if (nacao.getNome().equals(nacaoName)) {
                nacaoAlvo = nacao;
                break;
            }
        }
        try {
            if (addHab) {
                nacaoAlvo.addHabilidade(hab);
            } else {
                nacaoAlvo.remHabilidade(hab);
            }
        } catch (NullPointerException ex) {
            //nacao nao encontrada.
            log.error("Nation not found!");
        }
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.PACKAGE_RELOAD) {
            getTabGui().doLoadModel();
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
                Habilidade hab = (Habilidade) listaExibida.get(modelIndex);
                getTabGui().doMudaPackage(hab);
                //PENDING atualizar table mensagens
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        int row = e.getFirstRow();
        int column = e.getColumn();
        TableModel model = (TableModel) e.getSource();
        final Boolean addHab = (Boolean) model.getValueAt(row, column);
        final Habilidade hab = (Habilidade) model.getValueAt(row, 0);
        String nacaoName = model.getColumnName(column);
        doMarkPackage(nacaoName, addHab, hab);
    }
}
