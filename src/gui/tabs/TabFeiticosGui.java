/*
 * TabFeiticosGui.java
 *
 * Created on April 23, 2008, 11:37 AM
 */
package gui.tabs;

import control.FeiticoControler;
import gui.TabBase;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.Feitico;
import business.converter.TitleFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author  gurgel
 */
public final class TabFeiticosGui extends TabBase implements Serializable {

    private static final Log LOG = LogFactory.getLog(TabFeiticosGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final FeiticoControler feiticoControl;

    /** Creates new form TabFeiticosGui */
    public TabFeiticosGui(String titulo, String dica) {
        initComponents();
        //Basico
        setIcone("/images/help_icon.gif");
        setTitle(titulo);
        setDica(dica);

        //configura grid
        jtMainLista.setAutoCreateColumnsFromModel(true);
        jtMainLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtMainLista.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jtMainLista.setAutoCreateRowSorter(true);
        comboFiltro.setActionCommand("comboFiltro");
        //Cria o Controle da lista de acoes
        feiticoControl = new FeiticoControler(this);
        TableModel model = feiticoControl.getMainTableModel((String) comboFiltro.getSelectedItem());
        this.setMainModel(model);

        //adiciona listeners
        comboFiltro.setModel(feiticoControl.getLivroComboModel());
        comboFiltro.addActionListener(feiticoControl);
        jtMainLista.getSelectionModel().addListSelectionListener(feiticoControl);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jlListar = new javax.swing.JLabel();
        comboFiltro = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        qtFeiticos = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        listaHelp = new javax.swing.JTextPane();

        jlListar.setText(labels.getString("LISTAR:")); // NOI18N

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Todos" }));

        jLabel2.setText(labels.getString("TOTAL:")); // NOI18N

        qtFeiticos.setText(labels.getString("QTD")); // NOI18N

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(SettingsManager.getInstance().getConfigAsInt("spellsSplitSize", "200"));
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jSplitPane1PropertyChange(evt);
            }
        });

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
        jtMainLista.setName(""); // NOI18N
        jScrollPane3.setViewportView(jtMainLista);

        jSplitPane1.setLeftComponent(jScrollPane3);

        jScrollPane1.setBorder(null);

        listaHelp.setBorder(null);
        jScrollPane1.setViewportView(listaHelp);

        jSplitPane1.setRightComponent(jScrollPane1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(qtFeiticos))
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jlListar, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(77, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(qtFeiticos))
                .addGap(18, 18, 18)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(6, 6, 6)
                            .addComponent(jlListar))
                        .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(424, 424, 424)))
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
            LOG.debug("Split spells pane divisor modified to " + splitHeight + " px.");
            SettingsManager.getInstance().setConfig("spellsSplitSize", splitHeight);            
        }
    }//GEN-LAST:event_jSplitPane1PropertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel jlListar;
    private javax.swing.JTable jtMainLista;
    private javax.swing.JTextPane listaHelp;
    private javax.swing.JLabel qtFeiticos;
    // End of variables declaration//GEN-END:variables

    public JTable getMainLista() {
        return jtMainLista;
    }

    public void setMainModel(TableModel model) {
        this.setHelp("");
        this.jtMainLista.setModel(model);
        this.doConfigTableColumns(jtMainLista);
        this.updateGui();
    }

    private void updateGui() {
        this.qtFeiticos.setText(getMainLista().getRowCount() + "");
    }

    public void setHelp(String historia) {
        this.listaHelp.setText(historia);
    }

    public void doMudaFeitico(Feitico feitico) {
        try {
            setHelp(TitleFactory.getFeiticoDisplay(feitico));
        } catch (NullPointerException ex) {
            setHelp("");
        }
    }
}
