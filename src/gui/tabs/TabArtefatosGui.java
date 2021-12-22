/*
 * TabArtefatosGui.java
 *
 * Created on April 23, 2008, 11:37 AM
 */
package gui.tabs;

import baseLib.GenericoComboObject;
import control.ArtefatoControler;
import control.MapaControler;
import control.services.FiltroConverter;
import gui.TabBase;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.Artefato;
import model.Habilidade;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class TabArtefatosGui extends TabBase implements Serializable {

    private static final Log LOG = LogFactory.getLog(TabArtefatosGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final ArtefatoControler artefatoControl;

    /**
     * Creates new form TabArtefatosGui
     */
    public TabArtefatosGui(String titulo, String dica, MapaControler mapaControl) {
        initComponents();
        //Basico
        setIcone("/images/hex_artefato.gif");
        setTitle(titulo);
        setDica(dica);
        this.setMapaControler(mapaControl);
        this.setKeyFilterProperty("GuiFilterItem");

        //configura grid
        jtMainLista.setAutoCreateColumnsFromModel(true);
        jtMainLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtMainLista.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        //jtMainLista.setPreferredScrollableViewportSize(new Dimension(200, 70));
        jtMainLista.setAutoCreateRowSorter(true);
        comboFiltro.setName("comboFiltro");
        comboFiltro.setModel(FiltroConverter.getFiltroComboModelByJogador(WorldManager.getInstance().getPartida().getJogadorAtivo(), 4));
        comboFiltro.setSelectedIndex(this.getFiltroDefault());
        //Cria o Controle da lista de artefatos
        artefatoControl = new ArtefatoControler(this);

        //adiciona listeners
        comboFiltro.addActionListener(artefatoControl);
        jtMainLista.getSelectionModel().addListSelectionListener(artefatoControl);

        TableModel model = artefatoControl.getMainTableModel((GenericoComboObject) comboFiltro.getSelectedItem());
        this.setMainModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        comboFiltro = new javax.swing.JComboBox();
        qtArtefatos = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        listaHistoria = new javax.swing.JTextPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(350, 450));

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Todos", "Próprios" }));

        qtArtefatos.setText(labels.getString("QTD")); // NOI18N

        jLabel2.setText(labels.getString("TOTAL:")); // NOI18N

        jLabel3.setText(labels.getString("LISTAR:")); // NOI18N

        jSplitPane1.setDividerLocation(SysApoio.parseInt(SettingsManager.getInstance().getConfig("itemsSplitSize", "200")));
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jSplitPane1PropertyChange(evt);
            }
        });

        listaHistoria.setEditable(false);
        listaHistoria.setBorder(null);
        listaHistoria.setFocusable(false);
        listaHistoria.setMaximumSize(new java.awt.Dimension(1500, 1500));
        jSplitPane1.setBottomComponent(listaHistoria);
        listaHistoria.getAccessibleContext().setAccessibleName("");

        jScrollPane3.setBorder(null);

        jtMainLista.setAutoCreateRowSorter(true);
        jtMainLista.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Nome", "Tamanho", "Nação", "Local", "Title 5"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jtMainLista);

        jSplitPane1.setLeftComponent(jScrollPane3);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(244, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(qtArtefatos)
                .addContainerGap())
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel3)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(147, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(qtArtefatos)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(417, Short.MAX_VALUE)))
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

    private void jSplitPane1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jSplitPane1PropertyChange
        if (evt.getPropertyName().equals(javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY) ) {
            String splitHeight = evt.getNewValue().toString();
            LOG.debug("Split items pane divisor modified to " + splitHeight + " px.");
            SettingsManager.getInstance().setConfig("itemsSplitSize", splitHeight);
        } 
    }//GEN-LAST:event_jSplitPane1PropertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jtMainLista;
    private javax.swing.JTextPane listaHistoria;
    private javax.swing.JLabel qtArtefatos;
    // End of variables declaration//GEN-END:variables

    public JTable getMainLista() {
        return jtMainLista;
    }

    public final void setMainModel(TableModel model) {
        this.setAreaText();
        this.jtMainLista.setModel(model);
        jtMainLista.doLayout();
        this.doConfigTableColumns(jtMainLista);
        this.updateGui();
        this.doTagHide();
        this.jtMainLista.getSelectionModel().setSelectionInterval(0, 0);
    }

    public int getFiltro() {
        return comboFiltro.getSelectedIndex();
    }

    private void updateGui() {
        this.qtArtefatos.setText(getMainLista().getRowCount() + "");
    }

    private void setAreaText(Artefato artefato) {
        String temp = String.format("%s\n\n- %s:", artefato.getHistoria(), labels.getString("ITEM.SECONDARY"));
        for (Habilidade habilidade : artefato.getHabilidades().values()) {
            temp += "\n" + habilidade.getNome();
        }
        this.listaHistoria.setText(temp);
    }

    private void setAreaText() {
        this.listaHistoria.setText("");
    }

    public void doMudaArtefato(Artefato artefato) {
        try {
            getMapaControler().printTag(artefato.getLocal());
            setAreaText(artefato);
        } catch (NullPointerException ex) {
            setAreaText();
            this.doTagHide();
        }
    }

    @Override
    protected int getComboFiltroSize() {
        return this.comboFiltro.getModel().getSize();
    }
}
