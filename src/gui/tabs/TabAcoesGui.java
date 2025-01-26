/*
 * TabAcoesGui.java
 *
 * Created on April 23, 2008, 11:37 AM
 */
package gui.tabs;

import control.AcaoControler;
import gui.TabBase;
import gui.subtabs.SubTabTextArea;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.Ordem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public final class TabAcoesGui extends TabBase {

    private static final Log LOG = LogFactory.getLog(TabAcoesGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private AcaoControler acaoControl;
    private final SubTabTextArea stResults = new SubTabTextArea();

    /**
     * Creates new form TabAcoesGui
     */
    public TabAcoesGui(String titulo, String dica) {
        initComponents();
        //Basico
        setIcone("/images/help_icon.gif");
        setTitle(titulo);
        setDica(dica);
        initConfig();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        comboFiltro = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        qtAcoes = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();
        jpResult = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jLabel3.setText(labels.getString("LISTAR:")); // NOI18N

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All" }));

        jLabel2.setText(labels.getString("TOTAL:")); // NOI18N

        qtAcoes.setText(labels.getString("QTD")); // NOI18N

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(SettingsManager.getInstance().getConfigAsInt("actionsSplitSize", "200"));
        jSplitPane1.setDividerSize(3);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.5);
        jSplitPane1.setAutoscrolls(true);
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
        jScrollPane3.setViewportView(jtMainLista);

        jSplitPane1.setLeftComponent(jScrollPane3);

        javax.swing.GroupLayout jpResultLayout = new javax.swing.GroupLayout(jpResult);
        jpResult.setLayout(jpResultLayout);
        jpResultLayout.setHorizontalGroup(
            jpResultLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jpResultLayout.setVerticalGroup(
            jpResultLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 191, Short.MAX_VALUE)
        );

        jSplitPane1.setBottomComponent(jpResult);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        jLabel4.setText(bundle.getString("TAB.SEARCH.LABEL")); // NOI18N

        searchField.setToolTipText(bundle.getString("TAB.SEARCH.TOOLTIP")); // NOI18N
        searchField.setMinimumSize(new java.awt.Dimension(80, 20));
        searchField.setPreferredSize(new java.awt.Dimension(80, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(qtAcoes)
                .addContainerGap())
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(qtAcoes)
                    .addComponent(jLabel2)
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE))
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
        if (evt.getPropertyName().equals(javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
            String splitHeight = evt.getNewValue().toString();
            LOG.debug("Split actions pane divisor modified to " + splitHeight + " px.");
            SettingsManager.getInstance().setConfig("actionsSplitSize", splitHeight);
        }
    }//GEN-LAST:event_jSplitPane1PropertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPanel jpResult;
    private javax.swing.JTable jtMainLista;
    private javax.swing.JLabel qtAcoes;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables

    @Override
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
        this.qtAcoes.setText(getMainLista().getRowCount() + "");
    }

    private void setHelp(String help) {
        this.stResults.setText(help);
    }

    public void doMudaAcao(Ordem ordem) {
        try {
            setHelp(acaoControl.getAjuda(ordem));
        } catch (NullPointerException ex) {
            setHelp("");
        }
    }

    private void initConfig() {
        //configura grid
        jtMainLista.setAutoCreateColumnsFromModel(true);
        jtMainLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtMainLista.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jtMainLista.setAutoCreateRowSorter(true);
        comboFiltro.setActionCommand("comboFiltro");
        //Cria o Controle da lista de acoes
        acaoControl = new AcaoControler(this);
        TableModel model = acaoControl.getMainTableModel((String) comboFiltro.getSelectedItem());
        this.setMainModel(model);
        jSplitPane1.setBottomComponent(stResults);
        //stResults.replace(jpResult);

        //adiciona listeners
        addDocumentListener(searchField);
        comboFiltro.setModel(acaoControl.getTipoPersonagemComboModel());
        comboFiltro.addActionListener(acaoControl);
        jtMainLista.getSelectionModel().addListSelectionListener(acaoControl);
    }
}
