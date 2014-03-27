/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.subtabs;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import business.facade.NacaoFacade;
import control.NacaoRelacionamentoControl;
import gui.TabBase;
import java.io.Serializable;
import javax.swing.ComboBoxModel;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public final class SubTabRelacionamentos extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabRelacionamentos.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private NacaoRelacionamentoControl relControl = new NacaoRelacionamentoControl();
    NacaoFacade nacaoFacade = new NacaoFacade();

    /**
     * don't use
     */
    private SubTabRelacionamentos() {
        initComponents();
    }

    /**
     * Creates new form subTabRelacionamentos
     */
    public SubTabRelacionamentos(String vlDefault, Nacao nacao, ComboBoxModel nacaoComboModel, boolean all) {
        initComponents();
        relControl.setNacaoBase(nacao);
        relControl.setTabGui(this);
        //liga actionlisteners para a combo
        jcbNacao.setModel(nacaoComboModel);
        jcbNacao.setActionCommand("jcbNacao");
        jcbNacao.addItemListener(relControl);
        //seta model no combo do panel
        GenericoComboObject nacaoAlvoObj = (GenericoComboObject) jcbNacao.getModel().getSelectedItem();
        setRelacionamentoCombo((Nacao) nacaoAlvoObj.getObject());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgFiltro = new javax.swing.ButtonGroup();
        jcbNacao = new javax.swing.JComboBox();
        jcbRelacionamento = new javax.swing.JComboBox();

        jcbNacao.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        jcbNacao.setToolTipText(bundle.getString("TABRELACIONAMENTO.TOOLTIP.ALVO")); // NOI18N

        jcbRelacionamento.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jcbRelacionamento.setToolTipText(bundle.getString("TABRELACIONAMENTO.TOOLTIP.RELACIONAMENTO")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jcbNacao, 0, 336, Short.MAX_VALUE)
            .addComponent(jcbRelacionamento, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jcbNacao, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jcbRelacionamento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFiltro;
    private javax.swing.JComboBox jcbNacao;
    private javax.swing.JComboBox jcbRelacionamento;
    // End of variables declaration//GEN-END:variables

    public void setRelacionamentoCombo(Nacao nacaoAlvo) {
        final GenericoComboBoxModel relacionamentoComboModel = relControl.getRelacionamentoComboModel(nacaoAlvo);
        jcbRelacionamento.setModel(relacionamentoComboModel);
//        int index = relacionamentoComboModel.getIndexByDisplay(vlDefault);
//        jcbRelacionamento.setSelectedIndex(index);
    }

    /**
     *
     * @return Nacao;Relacionamento
     */
    public String getParametrosId() {
        //formata string para comando. Separado por ; e com o tipo no final
        String ret = "";
        GenericoComboObject par;
        par = (GenericoComboObject) jcbNacao.getSelectedItem();
        Nacao nacao = (Nacao) par.getObject();
        ret += nacaoFacade.getCodigo(nacao) + ";";
        par = (GenericoComboObject) jcbRelacionamento.getSelectedItem();
        ret += par.getComboId();
        return ret;
    }

    public String getParametrosDisplay() {
        String ret = "";
        GenericoComboObject par;
        par = (GenericoComboObject) jcbNacao.getSelectedItem();
        Nacao nacao = (Nacao) par.getObject();
        ret += nacaoFacade.getNome(nacao) + ";";
        par = (GenericoComboObject) jcbRelacionamento.getSelectedItem();
        ret += par.getComboDisplay();
        return ret;
    }
    //xxx:tem que fazer o reload funcionar agora. salva ordens, abre de novo. depois importa no server e processa ordem no judge.;
    //tem que filtrar as opcoes validas. Fazer tratamento do All para listar todas as opcoes. implementar filtro para uni/bi - new atributo?
}
