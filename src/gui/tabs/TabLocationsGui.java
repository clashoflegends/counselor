/*
 * TabCidadesGui.java
 *
 * Created on April 23, 2008, 11:40 AM
 */
package gui.tabs;

import baseLib.GenericoComboObject;
import business.facade.CenarioFacade;
import business.facade.LocalFacade;
import control.LocationControler;
import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import control.services.FiltroConverter;
import control.services.LocalConverter;
import gui.TabBase;
import gui.services.IAcaoGui;
import gui.subtabs.SubTabBaseList;
import gui.subtabs.SubTabPopup;
import java.io.Serializable;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.ActorAction;
import model.Cenario;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
import utils.OpenSlotCounter;

/**
 *
 * @author gurgel
 */
public class TabLocationsGui extends TabBase implements Serializable, IAcaoGui {

    private static final Log LOG = LogFactory.getLog(TabLocationsGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private LocationControler locationControl;
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final LocalFacade localFacade = new LocalFacade();
    private final SubTabPopup stResults = new SubTabPopup();
    private final SubTabBaseList stPersonagens = new SubTabBaseList();
    private final SubTabBaseList stProdutos = new SubTabBaseList();
    private Cenario cenario;

    /**
     * Creates new form TabHexagonosGui
     */
    public TabLocationsGui(String titulo, String dica, MapaControler mapaControl) {
        initComponents();
        //Basico
        setIcone("/images/hex_base.png");
        setTitle(titulo);
        setDica(dica);
        this.setMapaControler(mapaControl);
        this.setKeyFilterProperty("GuiFilterHex");

        initConfig();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        comboFiltro = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        qtHexes = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        detalhesHex = new javax.swing.JTabbedPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        toggleShowCampRestrictions = new javax.swing.JToggleButton();

        jLabel3.setText(labels.getString("LISTAR:")); // NOI18N

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Todos", "Próprios" }));
        comboFiltro.setName("comboFiltro"); // NOI18N

        jLabel5.setText(labels.getString("TOTAL:")); // NOI18N

        qtHexes.setText("999");

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(SysApoio.parseInt(SettingsManager.getInstance().getConfig("localSplitSize", "200")));
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jSplitPane1PropertyChange(evt);
            }
        });
        jSplitPane1.setRightComponent(detalhesHex);

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
        jtMainLista.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jtMainLista.setName(""); // NOI18N
        jtMainLista.setShowVerticalLines(false);
        jScrollPane3.setViewportView(jtMainLista);

        jSplitPane1.setLeftComponent(jScrollPane3);

        jToolBar1.setRollover(true);

        toggleShowCampRestrictions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/hex_redfog.png"))); // NOI18N
        toggleShowCampRestrictions.setSelected(isCampRestrictionSelected());
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        toggleShowCampRestrictions.setToolTipText(bundle.getString("SETTINGS.DISPLAY.FILTER.SHOWCITYCAP")); // NOI18N
        toggleShowCampRestrictions.setActionCommand("showCampRestriction");
        toggleShowCampRestrictions.setFocusable(false);
        toggleShowCampRestrictions.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleShowCampRestrictions.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(toggleShowCampRestrictions);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 227, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addGap(2, 2, 2)
                .addComponent(qtHexes))
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(qtHexes)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jSplitPane1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jSplitPane1PropertyChange
        if (evt.getPropertyName().equals(javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
            String splitHeight = evt.getNewValue().toString();
            LOG.debug("Split hex pane divisor modified to " + splitHeight + " px.");
            SettingsManager.getInstance().setConfig("localSplitSize", splitHeight);
        }
    }//GEN-LAST:event_jSplitPane1PropertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JTabbedPane detalhesHex;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTable jtMainLista;
    private javax.swing.JLabel qtHexes;
    private javax.swing.JToggleButton toggleShowCampRestrictions;
    // End of variables declaration//GEN-END:variables

    private void initConfig() {
        cenario = WorldFacadeCounselor.getInstance().getCenario();
        jtMainLista.setAutoCreateColumnsFromModel(true);
        jtMainLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtMainLista.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        comboFiltro.setName("comboFiltro");
        comboFiltro.setModel(FiltroConverter.getFiltroComboModelByJogador(WorldManager.getInstance().getPartida().getJogadorAtivo(), 5));
        comboFiltro.setSelectedIndex(this.getFiltroDefault());

        //create hexes control
        locationControl = new LocationControler(this);

        //adiciona listeners
        comboFiltro.addActionListener(locationControl);
        jtMainLista.getSelectionModel().addListSelectionListener(locationControl);
        toggleShowCampRestrictions.addActionListener(locationControl);

        TableModel model = locationControl.getMainTableModel((GenericoComboObject) comboFiltro.getSelectedItem());
        this.setMainModel(model);
        stResults.setFontText(detalhesHex.getFont());
        doAddTabs();
    }

    @Override
    public void setValueAt(ActorAction actorAction, int ordIndex, int openSlotsQt) {
        //set Col=1 at front, before skills so that it doesn't have to calculate where is the column.
        OpenSlotCounter openSlotCounter = (OpenSlotCounter) this.jtMainLista.getModel().getValueAt(locationControl.getModelRowIndex(), 1);
        openSlotCounter.setOpenSlotQt(openSlotsQt);
        this.jtMainLista.getModel().setValueAt(openSlotCounter, locationControl.getModelRowIndex(), 1);
    }

    public JTable getMainLista() {
        return jtMainLista;
    }

    public final void setMainModel(TableModel model) {
        stPersonagens.setListModelClear();
        stProdutos.setListModelClear();
        this.jtMainLista.setModel(model);
        this.doConfigTableColumns(jtMainLista);
        this.updateGui();
        this.doTagHide();
        this.jtMainLista.getSelectionModel().setSelectionInterval(0, 0);
    }

    public GenericoComboObject getFiltro() {
        return (GenericoComboObject) comboFiltro.getSelectedItem();
    }

    public void updateGui() {
        this.qtHexes.setText(getMainLista().getRowCount() + "");
    }

    private void doPrintTag(Local hex) {
        try {
            getMapaControler().printTag(hex);
        } catch (NullPointerException ex) {
            this.doTagHide();
        }
    }

    private void doAddTabs() {
        //config tabs
        detalhesHex.addTab(labels.getString("RESULTADOS"), new ImageIcon(getClass().getResource("/images/write-document-20x20.png")), stResults, labels.getString("RESULTADOS.TOOLTIP"));
        if (cenarioFacade.hasResourceManagement(cenario)) {
            detalhesHex.addTab(labels.getString("ESTOQUES"), new ImageIcon(getClass().getResource("/images/financas.gif")), stProdutos, labels.getString("ESTOQUE.TOOLTIP"));
        }
        detalhesHex.addTab(labels.getString("PRESENCAS"), new ImageIcon(getClass().getResource("/images/hex_personagem.gif")), stPersonagens, labels.getString("PRESENCA.TOOLTIP"));
        stResults.setGuiConfig("GuiLocalResults");
    }

    public void doMudaHexClear() {
        this.doTagHide();
        stProdutos.setListModelClear();
        stPersonagens.setListModelClear();
        stResults.setText("", "");
    }

    public void doMudaHex(Local hex) {
        doPrintTag(hex);
        stProdutos.setListModel(LocalConverter.getProdutoModel(hex));
        stPersonagens.setListModel(LocalConverter.getPresencasModel(hex));
        final String popupTitle = labels.getString("RESULTADOS.OF") + ": " + LocalFacade.getCoordenadas(hex);
        stResults.setText(popupTitle, locationControl.getResultados(hex));
    }

    @Override
    protected int getComboFiltroSize() {
        return this.comboFiltro.getModel().getSize();
    }

    public boolean isCampRestrictionSelected() {
        return SettingsManager.getInstance().getConfig("showCampRestriction", "1").equals("1");
    }
}