/*
 * TabExercitosGui.java
 *
 * Created on April 23, 2008, 11:37 AM
 */
package gui.tabs;

import baseLib.GenericoComboBoxModel;
import control.MapaControler;
import control.WorldBuilderControl;
import control.services.FiltroConverter;
import gui.TabBase;
import java.io.Serializable;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class TabWorldBuilderGui extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(TabWorldBuilderGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final WorldBuilderControl builderControl;

    /**
     * Creates new form TabExercitosGui
     */
    public TabWorldBuilderGui(String titulo, String dica, MapaControler mapaControl) {
        initComponents();
        //Basico
        setIcone("/images/world.png");
        setTitle(titulo);
        setDica(dica);
        this.setMapaControler(mapaControl);
        builderControl = new WorldBuilderControl(this);
        //config filters
        comboFiltro.setName("comboFiltro");
        comboFiltro.setModel(FiltroConverter.getFiltroComboModelByJogador(WorldManager.getInstance().getPartida().getJogadorAtivo(), 4));
        comboFiltro.setSelectedIndex(this.getFiltroDefault());
        //adiciona listeners
        jcbTerrain.setActionCommand("jcbTerrain");

//        this.setMainModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        qtExercitos = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboFiltro = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listaDetalhes = new javax.swing.JTextPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        jtListaInfos = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jCoordenada = new javax.swing.JLabel();
        jcbTerrain = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jcbClimate = new javax.swing.JComboBox();

        qtExercitos.setText(labels.getString("QTD")); // NOI18N

        jLabel2.setText(labels.getString("TOTAL:")); // NOI18N

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Todos", "Próprios" }));

        jLabel1.setText(labels.getString("LISTAR:")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(27, 27, 27)
                .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(qtExercitos)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(qtExercitos)
                    .addComponent(jLabel2)
                    .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(null);

        listaDetalhes.setBorder(null);
        jScrollPane1.setViewportView(listaDetalhes);

        jScrollPane4.setBorder(null);

        jtListaInfos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Nome", "Valor"
            }
        ));
        jScrollPane4.setViewportView(jtListaInfos);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        jLabel3.setText(bundle.getString("WB.TAB.COORDENADA")); // NOI18N

        jCoordenada.setText("jLabel4");

        jcbTerrain.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel5.setText(bundle.getString("WB.TAB.TERRENO")); // NOI18N

        jLabel4.setText(bundle.getString("WB.TAB.CLIMA")); // NOI18N

        jcbClimate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jcbClimate, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jcbTerrain, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCoordenada, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jCoordenada))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jcbTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jcbClimate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(81, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JLabel jCoordenada;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JComboBox jcbClimate;
    private javax.swing.JComboBox jcbTerrain;
    private javax.swing.JTable jtListaInfos;
    private javax.swing.JTextPane listaDetalhes;
    private javax.swing.JLabel qtExercitos;
    // End of variables declaration//GEN-END:variables

    public void setCoordenada(String txt) {
        this.jCoordenada.setText(txt);
    }

    public void setTerrenoModel(GenericoComboBoxModel terrenoComboModel, Terreno terreno) {
        jcbTerrain.removeActionListener(builderControl);
        this.jcbTerrain.setModel(terrenoComboModel);
        final int index = terrenoComboModel.getIndexByDisplay(terreno.getComboDisplay());
        this.jcbTerrain.setSelectedIndex(index);
        this.jcbTerrain.setMaximumRowCount(terrenoComboModel.getSize());
        jcbTerrain.addActionListener(builderControl);
    }
}
