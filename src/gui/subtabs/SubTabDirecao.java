/*
 * SubTabDirecaoExercito.java
 *
 * Created on June 13, 2009, 2:31 PM
 */
package gui.subtabs;

import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import business.facades.ListFactory;
import control.DirecaoControler;
import control.MapaControler;
import control.support.ActorInterface;
import gui.TabBase;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public final class SubTabDirecao extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabDirecao.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private String direcaoDisplay = "", direcaoTipo = "nr";
    private List<String> direcoesId = new ArrayList();
    private DirecaoControler dirExControl;
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
    public SubTabDirecao(String vlInicial, ActorInterface actorAtivo, MapaControler mapaControl, Ordem ordemSelecionada, boolean all) {
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
        setLimiteMovimento(3);
        setAgua(false);

        dirExControl = new DirecaoControler(this);

        //set initial tag
        this.doMovementTagsPaint(vlInicial);
        setListeners();
    }

    public SubTabDirecao(String vlInicial,Local local, int limit, boolean waterMov, MapaControler mapaControl, boolean all) {
        initComponents();

        setAll(all);
        this.setMapaControler(mapaControl);
        this.origem = local;
        this.destino = local;
        //FIXME: limite varia de acordo com a ordem. Salvar na ordem ou cenario.
        //FIXME: transformar em parametros no banco de dados.
        setLimiteMovimento(limit);
        setAgua(waterMov);

        dirExControl = new DirecaoControler(this);

        //set initial tag
//        this.doMovementTagsPaint(vlInicial);
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
        jLabel1 = new javax.swing.JLabel();
        jlDestino = new javax.swing.JLabel();

        setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

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

        jLabel1.setText(labels.getString("DESTINO: ")); // NOI18N
        jLabel1.setToolTipText(labels.getString("DESTINO.TOOLTIP")); // NOI18N

        jlDestino.setText("-");
        jlDestino.setToolTipText(labels.getString("DESTINO.TOOLTIP")); // NOI18N

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
                    .addComponent(jlDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                    .addComponent(jlDestino, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)))
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
    private javax.swing.JLabel jlDestino;
    private javax.swing.JLabel jlDisplay;
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
                movEx.setEvasivo(false);
                movEx.setComida(true);
                movEx.setDestino(proximoDestino);
                movEx.setOrigem(this.destino);
                movEx.setDirecao(ConverterFactory.direcaoToInt(direcao));
                //busca a ultima movimentacao valida (sem ficar parado).
                for (int ii = direcoesId.size() - 1; ii >= 0; ii--) {
                    String elem = direcoesId.get(ii);
                    if (!elem.equals("h")) {
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
    }

}
