/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import business.facade.AcaoFacade;
import control.facade.WorldFacadeCounselor;
import control.services.FinancasConverter;
import control.services.NacaoConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabFinancesGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Nacao;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class FinancasControler extends ControlBase implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(FinancasControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabFinancesGui tabGui;
    private List<Nacao> listaExibida;
    private final AcaoFacade acaoFacade = new AcaoFacade();
    private final business.facade.NacaoFacade nacaoFacade = new business.facade.NacaoFacade();
    private static final WorldFacadeCounselor WFC = WorldFacadeCounselor.getInstance();
    private final FinancasConverter finConv = new FinancasConverter();

    public FinancasControler(TabFinancesGui tabGui) {
        this.tabGui = tabGui;
        registerDispatchManager();
        registerDispatchManagerForMsg(DispatchManager.CLEAR_FINANCES_FORECAST);
        // An order FILE has just been loaded (or cleared). The nations table model is built during tab
        // construction, which on an EGF open happens BEFORE the orders are loaded, so its cost columns
        // would otherwise sit at zero all session - the Nations tab rebuilds on this same signal.
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_RELOAD);
    }

    public GenericoTableModel getExtratoTableModel(Nacao nacao) {
        return FinancasConverter.getExtratoTableModel(nacao);
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        listaExibida = NacaoConverter.listaByFiltro(filtro.getComboId());
        this.mainTableModel = NacaoConverter.getNacaoModel(listaExibida);
        return this.mainTableModel;
    }

    public GenericoTableModel getMercadoTableModel(Nacao nacao) {
        return FinancasConverter.getMercadoModel(nacao);
    }

    public GenericoTableModel getProjecaoTableModel(Nacao nation) {
        final Set<PersonagemOrdem> listPo = WFC.getMapPersonagemOrdens(nation);
        return finConv.getProjecaoTableModel(nation, listPo);
    }

    private TabFinancesGui getTabGui() {
        return tabGui;
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
                int modelIndex = table.convertRowIndexToModel(rowIndex);
                getTabGui().doMudaNacao((Nacao) listaExibida.get(modelIndex));
                //PENDING atualizar table mensagens
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void receiveDispatch(Nacao nation, PersonagemOrdem before, PersonagemOrdem after) {
        boolean refresh = false;
        //retira "antes" da lista
        if (WFC.getOrderCost(before, nation) > 0 && WFC.remNacaoPersonagemOrdens(nation, before)) {
            refresh = true;
        }

        //receive msg to add to finances forecast
        if (WFC.getOrderCost(after, nation) > 0 && WFC.addNacaoPersonagemOrdens(nation, after)) {
            refresh = true;
        }

        //if cost changed, then recalculate
        if (refresh) {
            //only repaint the forecast when the order belongs to the nation on display; a save for another nation
            //(team or on-behalf orders) used to overwrite the selected nation's forecast with the other nation's.
            if (tabGui.getNacaoSelecionada() == nation) {
                tabGui.setProjecaoModel(getProjecaoTableModel(nation));
            }
            //the nations table keeps showing its own row for every nation, so its order-cost cells are
            //refreshed whatever is selected - this is what used to force a click away and back.
            NacaoConverter.refreshOrderCostCells(mainTableModel, listaExibida, nation);
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SET_LABEL_MONEY, nation.getId() + "");
        }
    }

    @Override
    public void receiveDispatch(int msgName, String idNation) {
        if (msgName == DispatchManager.ACTIONS_RELOAD) {
            NacaoConverter.refreshOrderCostCells(mainTableModel, listaExibida);
        }
        if (msgName == DispatchManager.CLEAR_FINANCES_FORECAST) {
            for (Set<PersonagemOrdem> lists : WFC.getMapPersonagemOrdens().values()) {
                //clear each array, no need to clear the array itself.
                lists.clear();
            }
            NacaoConverter.refreshOrderCostCells(mainTableModel, listaExibida);
        }
    }
}
