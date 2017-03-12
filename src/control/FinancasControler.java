/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import business.facade.AcaoFacade;
import control.services.FinancasConverter;
import control.services.NacaoConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabFinancasGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Nacao;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
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
    private final TabFinancasGui tabGui;
    private List<Nacao> listaExibida;
  //  private final List<PersonagemOrdem> listaPersonagemOrdens = new ArrayList<PersonagemOrdem>();
    private Map<Nacao, List<PersonagemOrdem>> mapPersonagemOrdens = new HashMap<Nacao, List<PersonagemOrdem>>();
    private Nacao nacao;
    private final AcaoFacade acaoFacade = new AcaoFacade();

    public FinancasControler(TabFinancasGui tabGui) {
        this.tabGui = tabGui;
        registerDispatchManager();
        registerDispatchManagerForMsg(DispatchManager.CLEAR_FINANCES_FORECAST);
        registerDispatchManagerForMsg(DispatchManager.SET_NACAO_SELECTED);
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

    
    public GenericoTableModel getProjecaoTableModel(Nacao nacao) {
        List<PersonagemOrdem> listPo = new ArrayList<PersonagemOrdem>();
        if (mapPersonagemOrdens.containsKey(nacao)) {
            listPo = mapPersonagemOrdens.get(nacao);
        }
        return FinancasConverter.getProjecaoTableModel(nacao, listPo);
    }
    
    public GenericoTableModel getProjecaoTableModel(Nacao nacao, List<PersonagemOrdem> listPo) {
        mapPersonagemOrdens.put(nacao, listPo);
        return FinancasConverter.getProjecaoTableModel(nacao, listPo);
    }

    public TabFinancasGui getTabGui() {
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
                nacao = (Nacao) listaExibida.get(modelIndex);               
                getTabGui().doMudaNacao(nacao);
                //PENDING atualizar table mensagens
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void receiveDispatch(PersonagemOrdem antes, PersonagemOrdem depois) {
        boolean refresh = false;
        //retira "antes" da lista
        if (acaoFacade.getCusto(antes) > 0) {
            if (mapPersonagemOrdens.containsKey(nacao)) {
                mapPersonagemOrdens.get(nacao).remove(antes);
                refresh = true;
            }
            
        }

        //receive msg to add to finances forecast
        if (acaoFacade.getCusto(depois) > 0) {
            if (mapPersonagemOrdens.containsKey(nacao)) {
                mapPersonagemOrdens.get(nacao).add(depois);
                refresh = true;
            }
            
        }
        if (refresh) {      
           JTable table = this.getTabGui().getMainLista();
          
           Nacao selectedNacao = (Nacao) listaExibida.get(table.getSelectedRow()); 
           if (selectedNacao.equals(nacao)) {
               tabGui.setProjecaoModel(getProjecaoTableModel(nacao, mapPersonagemOrdens.get(nacao)));
           }
        }
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.CLEAR_FINANCES_FORECAST) {
            mapPersonagemOrdens.clear();
        } else if (msgName == DispatchManager.SET_NACAO_SELECTED) {
            this.nacao = findNacao(Integer.parseInt(txt));
            if (!mapPersonagemOrdens.containsKey(nacao)) {
                mapPersonagemOrdens.put(nacao, new ArrayList<PersonagemOrdem>());
            }
        }
    }
    
    /**
     * Copied from WorldControler
     * @param idNacao
     * @return 
     */
    private Nacao findNacao(int idNacao) {
        for (Nacao nacion : WorldManager.getInstance().getNacoes().values()) {
            int id = nacion.getId();
            if (nacion.getId() == idNacao) {
                return nacion;
            }
        }
        return null;
    }
}
