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
    private static final WorldFacadeCounselor WFC = WorldFacadeCounselor.getInstance();
    private final FinancasConverter finConv = new FinancasConverter();

    public FinancasControler(TabFinancesGui tabGui) {
        this.tabGui = tabGui;
        registerDispatchManager();
        registerDispatchManagerForMsg(DispatchManager.CLEAR_FINANCES_FORECAST);
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
            //FIXME: How to refresh the Cost of Orders in the main table?
            tabGui.setProjecaoModel(getProjecaoTableModel(nation));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SET_LABEL_MONEY, nation.getId() + "");
        }
    }

    @Override
    public void receiveDispatch(int msgName, String idNation) {
        if (msgName == DispatchManager.CLEAR_FINANCES_FORECAST) {
            for (Set<PersonagemOrdem> lists : WFC.getMapPersonagemOrdens().values()) {
                //clear each array, no need to clear the array itself.
                lists.clear();
            }
        }
    }
}
