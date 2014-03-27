/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import business.facade.NacaoFacade;
import gui.subtabs.SubTabRelacionamento;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import javax.swing.JComboBox;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class NacaoRelacionamentoControl implements ItemListener, Serializable {

    private static final Log log = LogFactory.getLog(NacaoRelacionamentoControl.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private SubTabRelacionamento tabGui;
    private Nacao nacaoBase;
    private NacaoFacade nacaoFacade = new NacaoFacade();

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() instanceof JComboBox && event.getStateChange() == ItemEvent.SELECTED) {
            JComboBox cb = (JComboBox) event.getSource();
            try {
                if ("jcbNacao".equals(cb.getActionCommand())) {
                    GenericoComboObject par;
                    par = (GenericoComboObject) cb.getModel().getSelectedItem();
                    getTabGui().setRelacionamentoCombo((Nacao) par.getObject());
                }
            } catch (ClassCastException ex) {
            }
        }
    }

    public GenericoComboBoxModel getRelacionamentoComboModel(Nacao nacaoAlvo) {
        String[][] itens = nacaoFacade.listRelacionamentosTipo(this.nacaoBase, nacaoAlvo);
        return new GenericoComboBoxModel(itens);
    }

    /**
     * @return the nacaoBase
     */
    public Nacao getNacaoBase() {
        return nacaoBase;
    }

    /**
     * @param nacaoBase the nacaoBase to set
     */
    public void setNacaoBase(Nacao nacaoBase) {
        this.nacaoBase = nacaoBase;
    }

    /**
     * @return the tabGui
     */
    public SubTabRelacionamento getTabGui() {
        return tabGui;
    }

    /**
     * @param tabGui the tabGui to set
     */
    public void setTabGui(SubTabRelacionamento tabGui) {
        this.tabGui = tabGui;
    }
}
