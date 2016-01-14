/*
 * SubTabDirecaoExercito.java
 *
 * Created on June 13, 2009, 2:31 PM
 */
package gui.subtabs;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import business.facades.ListFactory;
import control.DirecaoExercitoControler;
import control.MapaControler;
import control.support.ActorInterface;
import gui.TabBase;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.Local;
import model.Ordem;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public final class SubTabDirecaoExercito extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabDirecaoExercito.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private String direcaoDisplay = "", direcaoTipo = "nr";
    private List<String> direcoesId = new ArrayList();
    private DirecaoExercitoControler dirExControl;
    private Local origem, destino;
    private LocalFacade localFacade = new LocalFacade();
    private Ordem ordem;
    private ActorInterface actor;
    private final ListFactory listFactory = new ListFactory();
    private boolean agua = false, all = false;
    private int limiteMovimento = 0;

    /**
     * Creates new form SubTabDirecaoExercito
     */
    public SubTabDirecaoExercito(String vlInicial, ActorInterface actorAtivo, MapaControler mapaControl, Ordem ordemSelecionada, boolean all) {
        initComponents();

        setAll(all);
        this.setMapaControler(mapaControl);
        //setValorInicial(vlInicial);
        this.origem = actorAtivo.getLocal();
        this.destino = actorAtivo.getLocal();
        this.actor = actorAtivo;
        this.ordem = ordemSelecionada;
        //FIXME: limite varia de acordo com a ordem. Salvar na ordem ou cenario.
        //FIXME: transformar em parametros no banco de dados.
        if (ordem.getCodigo().equals("850")) {
            setLimiteMovimento(12);
            setAgua(false);
        } else if (ordem.getCodigo().equals("860")) {
            setLimiteMovimento(14);
            setAgua(false);
        } else if (ordem.getCodigo().equals("830")) {
            setLimiteMovimento(14);
            setAgua(true);
        } else {
            setLimiteMovimento(0);
            setAgua(false);
        }

        dirExControl = new DirecaoExercitoControler(this);

        //set combo.model
        GenericoComboBoxModel cbmTemp = getTropaModel();
        jcTipoTropa.setModel(cbmTemp);
        jcTipoTropa.setVisible(isAll());
        //set default item - comida
        ExercitoFacade ef = new ExercitoFacade();
        jcbComida.setSelected(!ef.isComida(actor.getExercito()));

        //set default item - slower troop
        int slow = -1, index = -1;
        for (int ii = 0; ii < cbmTemp.getSize(); ii++) {
            GenericoComboObject temp = (GenericoComboObject) cbmTemp.getElementAt(ii);
            TipoTropa tpTropa = (TipoTropa) temp.getObject();
            List<TipoTropa> tropas = new ArrayList<TipoTropa>();
            tropas.add(tpTropa);
            final int current = ef.getCustoMovimentoBase(tropas, origem.getTerreno(), false, isAgua());
            if (current > slow) {
                index = ii;
                slow = current;
            }
        }
        jcTipoTropa.setSelectedIndex(index);

        //set initial tag
        this.doMovementTagsPaint(vlInicial);
        setListeners();
    }

    public SubTabDirecaoExercito(Local local, int limitMov, boolean waterMov, MapaControler mapaControl) {
        initComponents();

        setAll(true);
        this.setMapaControler(mapaControl);
        this.origem = local;
        this.destino = local;
        //FIXME: limite varia de acordo com a ordem. Salvar na ordem ou cenario.
        //FIXME: transformar em parametros no banco de dados.
        setLimiteMovimento(limitMov);
        setAgua(waterMov);

        dirExControl = new DirecaoExercitoControler(this);

        //set combo.model
        GenericoComboBoxModel cbmTemp = getTropaModel();
        jcTipoTropa.setModel(cbmTemp);
        jcTipoTropa.setVisible(isAll());
        //set default item - comida
        jcbComida.setSelected(false);
        ExercitoFacade ef = new ExercitoFacade();

        //set default item - slower troop
        int slow = -1, index = -1;
        for (int ii = 0; ii < cbmTemp.getSize(); ii++) {
            GenericoComboObject temp = (GenericoComboObject) cbmTemp.getElementAt(ii);
            TipoTropa tpTropa = (TipoTropa) temp.getObject();
            List<TipoTropa> tropas = new ArrayList<TipoTropa>();
            tropas.add(tpTropa);
            final int current = ef.getCustoMovimentoBase(tropas, origem.getTerreno(), false, isAgua());
            if (current > slow) {
                index = ii;
                slow = current;
            }
        }
        jcTipoTropa.setSelectedIndex(index);

        setListeners();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jbNo = new javax.swing.JButton();
        jbNe = new javax.swing.JButton();
        jbC = new javax.swing.JButton();
        jbL = new javax.swing.JButton();
        jbO = new javax.swing.JButton();
        jbSe = new javax.swing.JButton();
        jbSo = new javax.swing.JButton();
        jbApaga = new javax.swing.JButton();
        jlDisplay = new javax.swing.JLabel();
        jrbNormal = new javax.swing.JRadioButton();
        jrbEvasivo = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jlDestino = new javax.swing.JLabel();
        jcbComida = new javax.swing.JCheckBox();
        jcTipoTropa = new javax.swing.JComboBox();

        jbNo.setText(labels.getString("NORTEOESTE.ABREVIADO")); // NOI18N
        jbNo.setActionCommand("nw");
        jbNo.setDefaultCapable(false);
        jbNo.setMaximumSize(new java.awt.Dimension(50, 20));
        jbNo.setMinimumSize(new java.awt.Dimension(50, 20));
        jbNo.setName("nw"); // NOI18N
        jbNo.setPreferredSize(new java.awt.Dimension(50, 20));

        jbNe.setText(labels.getString("NORTELESTE.ABREVIADO")); // NOI18N
        jbNe.setActionCommand("ne");
        jbNe.setDefaultCapable(false);
        jbNe.setMaximumSize(new java.awt.Dimension(50, 20));
        jbNe.setMinimumSize(new java.awt.Dimension(50, 20));
        jbNe.setName("ne"); // NOI18N
        jbNe.setPreferredSize(new java.awt.Dimension(50, 20));

        jbC.setText(labels.getString("CENTRO.ABREVIADO")); // NOI18N
        jbC.setActionCommand("h");
        jbC.setDefaultCapable(false);
        jbC.setMaximumSize(new java.awt.Dimension(50, 20));
        jbC.setMinimumSize(new java.awt.Dimension(50, 20));
        jbC.setName("h"); // NOI18N
        jbC.setPreferredSize(new java.awt.Dimension(50, 20));

        jbL.setText(labels.getString("LESTE.ABREVIADO")); // NOI18N
        jbL.setActionCommand("e");
        jbL.setDefaultCapable(false);
        jbL.setMaximumSize(new java.awt.Dimension(50, 20));
        jbL.setMinimumSize(new java.awt.Dimension(50, 20));
        jbL.setName("e"); // NOI18N
        jbL.setPreferredSize(new java.awt.Dimension(50, 20));

        jbO.setText(labels.getString("OESTE.ABREVIADO")); // NOI18N
        jbO.setActionCommand("w");
        jbO.setDefaultCapable(false);
        jbO.setMaximumSize(new java.awt.Dimension(50, 20));
        jbO.setMinimumSize(new java.awt.Dimension(50, 20));
        jbO.setName("w"); // NOI18N
        jbO.setPreferredSize(new java.awt.Dimension(50, 20));

        jbSe.setText(labels.getString("SULLESTE.ABREVIADO")); // NOI18N
        jbSe.setActionCommand("se");
        jbSe.setDefaultCapable(false);
        jbSe.setMaximumSize(new java.awt.Dimension(50, 20));
        jbSe.setMinimumSize(new java.awt.Dimension(50, 20));
        jbSe.setName("se"); // NOI18N
        jbSe.setPreferredSize(new java.awt.Dimension(50, 20));

        jbSo.setText(labels.getString("SULOESTE.ABREVIADO")); // NOI18N
        jbSo.setActionCommand("sw");
        jbSo.setDefaultCapable(false);
        jbSo.setMaximumSize(new java.awt.Dimension(50, 20));
        jbSo.setMinimumSize(new java.awt.Dimension(50, 20));
        jbSo.setName("sw"); // NOI18N
        jbSo.setPreferredSize(new java.awt.Dimension(50, 20));

        jbApaga.setText(labels.getString("APAGA"));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        jbApaga.setToolTipText(bundle.getString("MOVER.EXERCITO.ERASE.TOOLTIP")); // NOI18N
        jbApaga.setActionCommand("apaga");
        jbApaga.setDefaultCapable(false);
        jbApaga.setMaximumSize(new java.awt.Dimension(50, 20));
        jbApaga.setMinimumSize(new java.awt.Dimension(50, 20));
        jbApaga.setName("Erase"); // NOI18N
        jbApaga.setPreferredSize(new java.awt.Dimension(50, 20));

        jlDisplay.setText("-");
        jlDisplay.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        buttonGroup1.add(jrbNormal);
        jrbNormal.setSelected(true);
        jrbNormal.setText(labels.getString("NORMAL")); // NOI18N
        jrbNormal.setActionCommand("normal");

        buttonGroup1.add(jrbEvasivo);
        jrbEvasivo.setText(labels.getString("EVASIVO")); // NOI18N
        jrbEvasivo.setActionCommand("evasivo");

        jLabel1.setText(labels.getString("DESTINO: ")); // NOI18N
        jLabel1.setToolTipText(labels.getString("DESTINO.TOOLTIP")); // NOI18N

        jlDestino.setText("-");
        jlDestino.setToolTipText(labels.getString("DESTINO.TOOLTIP")); // NOI18N

        jcbComida.setText(labels.getString("SEM.COMIDA")); // NOI18N
        jcbComida.setToolTipText(bundle.getString("MOVER.EXERCITO.FOOD.TOOLTIP")); // NOI18N
        jcbComida.setActionCommand("comida");

        jcTipoTropa.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jcTipoTropa.setToolTipText(labels.getString("MOVER.EXERCITO.TIPOTROPA.TOOLTIP")); // NOI18N
        jcTipoTropa.setActionCommand("tipoTropaChanged");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlDestino, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jrbEvasivo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jlDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jrbNormal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jcTipoTropa, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(29, 29, 29)
                                .addComponent(jbNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbNe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jbO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(jbSo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbSe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(jbApaga, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jcbComida, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbNe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbSo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbSe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbApaga, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                    .addComponent(jlDestino, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbComida)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcTipoTropa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jrbNormal, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jrbEvasivo, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton jbApaga;
    private javax.swing.JButton jbC;
    private javax.swing.JButton jbL;
    private javax.swing.JButton jbNe;
    private javax.swing.JButton jbNo;
    private javax.swing.JButton jbO;
    private javax.swing.JButton jbSe;
    private javax.swing.JButton jbSo;
    private javax.swing.JComboBox jcTipoTropa;
    private javax.swing.JCheckBox jcbComida;
    private javax.swing.JLabel jlDestino;
    private javax.swing.JLabel jlDisplay;
    private javax.swing.JRadioButton jrbEvasivo;
    private javax.swing.JRadioButton jrbNormal;
    // End of variables declaration//GEN-END:variables

    public String getDirecoesIdTipo() {
        //formata string para comando. Separado por ; e com o tipo no final
        String ret = "";
        for (String direcao : direcoesId) {
            ret += direcao + ";";
        }
        ret += getDirecaoTipo();
        return ret;
    }

    public String getDirecaoDisplay() {
        return direcaoDisplay + getDirecaoTipo();
    }

    /**
     * @param direcao the direcaoId to add
     */
    private void addDirecaoId(String direcao) {
        this.direcoesId.add(direcao);
    }

    /**
     * @param direcaoDisplay the direcaoDisplay to set
     */
    private void setDirecaoDisplay(String direcaoDisplay) {
        this.direcaoDisplay = direcaoDisplay;
    }

    /**
     * @return the direcaoTipo
     */
    private String getDirecaoTipo() {
        return direcaoTipo;
    }

    /**
     * @param aDirecaoTipo the direcaoTipo to set
     */
    public void setDirecaoTipo(String aDirecaoTipo) {
        this.direcaoTipo = aDirecaoTipo;
    }

    public void doApaga() {
        this.direcoesId = new ArrayList<String>();
        this.setDirecaoDisplay("");
        this.destino = this.origem;
        this.getMapaControler().remMovementTag();
        this.updateGui();
    }

    public void doMovementTagAdd(String direcao) {
        //verifica quem eh o proximo Hex
        Local proximoDestino = getDestino(destino, direcao);
        if (proximoDestino == null) {
            //log.info("Cannot move in this direction.");
        } else {
            try {

                //cria objeto com todas as informacoes;
                MovimentoExercito movEx = new MovimentoExercito();
                movEx.setEvasivo(jrbEvasivo.isSelected());
                if (!isAll()) {
                    List<TipoTropa> list = new ArrayList<TipoTropa>();
                    GenericoComboBoxModel model = getTropaModel();
                    for (GenericoComboObject gco : model.getElementAll()) {
                        list.add((TipoTropa) gco.getObject());
                    }
                    movEx.addTropasAll(list);
                } else {
                    GenericoComboObject temp = (GenericoComboObject) jcTipoTropa.getSelectedItem();
                    movEx.addTropas((TipoTropa) temp.getObject());
                }
                movEx.setComida(!jcbComida.isSelected());
                movEx.setDestino(proximoDestino);
                movEx.setOrigem(this.destino);
                movEx.setDirecao(ConverterFactory.direcaoToInt(direcao));
                //busca a ultima movimentacao valida (sem ficar parado).
                for (int ii = direcoesId.size() - 1; ii >= 0; ii--) {
                    String elem = direcoesId.get(ii);
                    if (!elem.equals("h")) {
                        int dir = ConverterFactory.getDirecao(ConverterFactory.direcaoToInt(elem) + 3);
                        movEx.setDirecaoAnterior(ConverterFactory.direcaoToInt(elem) + 3);
                        // the +3 must be outside the direction...
                        break;
                    }
                }
                movEx.setLimiteMovimento(this.getLimiteMovimento());
                movEx.setPorAgua(isAgua());
                //salva o destino
                this.destino = proximoDestino;
                //desenha a tag
                this.getMapaControler().addMovementTag(movEx);
                //atualiza a GUI
                this.addDirecaoId(direcao);
                this.setDirecaoDisplay(this.direcaoDisplay + direcao + ";");
            } catch (NullPointerException ex) {
                //no troops of type?
            }
            this.updateGui();
        }
    }

    private void doMovementTagsPaint(String vlInicial) {
        String[] movs = vlInicial.split(";");
        for (String elem : movs) {
            //tipo de movimentacao ou vazio, ignorar.
            if (!elem.equals("nr") && !elem.equals("ev") && !elem.equals("")) {
                if (ConverterFactory.isDirecaoValid(elem)) {
                    //validates if there is garbage as parameter and cleanit up.
                    this.doMovementTagAdd(elem);
                }
            }
        }
    }

    /**
     * reinicia o movimento, calculando corretamente os valores.
     */
    public void doMovementTagsRepaint() {
        String temp = this.direcaoDisplay;
        //apaga todas as tags
        doApaga();
        //desenha novamente, considerando novos valores para comida/cavalaria
        doMovementTagsPaint(temp);
    }

    private void updateGui() {
        jlDisplay.setText(getDirecaoDisplay());
        this.jlDestino.setText(destino.getCoordenadas());
        if (this.direcaoTipo.equalsIgnoreCase("nr")) {
            this.jrbNormal.setSelected(true);
        } else {
            this.jrbEvasivo.setSelected(true);
        }
    }

    private Local getDestino(Local atual, String direcao) {
        Local hexVizinho = null;
        if (direcao.equals("h")) {
            hexVizinho = atual;
        } else {
            hexVizinho = listFactory.getLocal(localFacade.getIdentificacaoVizinho(atual, direcao));
        }
        return hexVizinho;
    }

    /**
     * @return the agua
     */
    public boolean isAgua() {
        return agua;
    }

    /**
     * @param agua the agua to set
     */
    private void setAgua(boolean agua) {
        this.agua = agua;
    }

    /**
     * @return the limiteMovimento
     */
    public int getLimiteMovimento() {
        return limiteMovimento;
    }

    /**
     * @param limiteMovimento the limiteMovimento to set
     */
    private void setLimiteMovimento(int limiteMovimento) {
        this.limiteMovimento = limiteMovimento;
    }

    /**
     * @return the all
     */
    public boolean isAll() {
        return all;
    }

    /**
     * @param all the all to set
     */
    public void setAll(boolean all) {
        this.all = all;
    }

    private void setListeners() {
        //add listeners
        jbNo.addActionListener(dirExControl);
        jbNe.addActionListener(dirExControl);
        jbC.addActionListener(dirExControl);
        jbL.addActionListener(dirExControl);
        jbO.addActionListener(dirExControl);
        jbSe.addActionListener(dirExControl);
        jbSo.addActionListener(dirExControl);
        jbApaga.addActionListener(dirExControl);
        jrbEvasivo.addActionListener(dirExControl);
        jrbNormal.addActionListener(dirExControl);
        jcTipoTropa.addActionListener(dirExControl);
        jcbComida.addActionListener(dirExControl);
    }

    private GenericoComboBoxModel getTropaModel() {
        if (isAll() || actor == null || actor.getExercito() == null) {
            return dirExControl.getTropaTipoComboModel(isAgua());
        } else {
            return dirExControl.getTropaTipoComboModel(actor.getExercito(), isAgua());
        }
    }
}
