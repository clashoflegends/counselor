/*
 * TabCidadesGui.java
 *
 * Created on April 23, 2008, 11:40 AM
 */
package gui.tabs;

import baseLib.GenericoComboObject;
import business.facade.CenarioFacade;
import business.facade.CidadeFacade;
import business.facade.JogadorFacade;
import control.CidadeControler;
import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import control.services.CidadeConverter;
import control.services.FiltroConverter;
import gui.TabBase;
import gui.services.IAcaoGui;
import gui.subtabs.SubTabBaseList;
import gui.subtabs.SubTabOrdem;
import gui.subtabs.SubTabPopup;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.ActorAction;
import model.Cenario;
import model.Cidade;
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
public class TabCidadesGui extends TabBase implements Serializable, IAcaoGui {

    private static final Log LOG = LogFactory.getLog(TabCidadesGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private CidadeControler cidadeControl;
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final JogadorFacade jogadorFacade = new JogadorFacade();
//    private SubTabConfigActor stRename = new SubTabConfigActor();
    private final SubTabPopup stResults = new SubTabPopup();
    private final SubTabBaseList stPersonagens = new SubTabBaseList();
    private final SubTabBaseList stProdutos = new SubTabBaseList();
    private SubTabOrdem stOrdens;
    private Cenario cenario;

    /**
     * Creates new form TabCidadesGui
     */
    public TabCidadesGui(String titulo, String dica, MapaControler mapaControl) {
        initComponents();
        //Basico
        setIcone("/images/cp_acampamento.gif");
        setTitle(titulo);
        setDica(dica);
        this.setMapaControler(mapaControl);
        this.setKeyFilterProperty("GuiFilterCity");

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
        qtCidades = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        detalhesCidade = new javax.swing.JTabbedPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();

        jLabel3.setText(labels.getString("LISTAR:")); // NOI18N

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Todos", "Próprios" }));
        comboFiltro.setName("comboFiltro"); // NOI18N

        jLabel5.setText(labels.getString("TOTAL:")); // NOI18N

        qtCidades.setText("999");

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(SysApoio.parseInt(SettingsManager.getInstance().getConfig("citySplitSize", "200")));
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jSplitPane1PropertyChange(evt);
            }
        });
        jSplitPane1.setRightComponent(detalhesCidade);

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

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        jLabel1.setText(bundle.getString("TAB.SEARCH.LABEL")); // NOI18N

        searchField.setToolTipText(bundle.getString("TAB.SEARCH.TOOLTIP")); // NOI18N
        searchField.setMinimumSize(new java.awt.Dimension(80, 20));
        searchField.setPreferredSize(new java.awt.Dimension(80, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addGap(2, 2, 2)
                .addComponent(qtCidades))
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(qtCidades)
                        .addComponent(jLabel5)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE))
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
            LOG.debug("Split city pane divisor modified to " + splitHeight + " px.");
            SettingsManager.getInstance().setConfig("citySplitSize", splitHeight);
        }
    }//GEN-LAST:event_jSplitPane1PropertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JTabbedPane detalhesCidade;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jtMainLista;
    private javax.swing.JLabel qtCidades;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables

    private void initConfig() {
        cenario = WorldFacadeCounselor.getInstance().getCenario();
        stOrdens = new SubTabOrdem(this, getMapaControler());
        jtMainLista.setAutoCreateColumnsFromModel(true);
        jtMainLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtMainLista.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        comboFiltro.setName("comboFiltro");
        comboFiltro.setModel(FiltroConverter.getFiltroComboModelByJogador(WorldManager.getInstance().getPartida().getJogadorAtivo(), 5));
        comboFiltro.setSelectedIndex(this.getFiltroDefault());

        //Cria o Controle da lista de cidades
        cidadeControl = new CidadeControler(this);

        //adiciona listeners
        addDocumentListener(searchField);
        comboFiltro.addActionListener(cidadeControl);
        jtMainLista.getSelectionModel().addListSelectionListener(cidadeControl);

        TableModel model = cidadeControl.getMainTableModel((GenericoComboObject) comboFiltro.getSelectedItem());
        this.setMainModel(model);
        stResults.setFontText(detalhesCidade.getFont());
        doAddTabs();
    }

    @Override
    public void setValueAt(ActorAction actorAction, int ordIndex, int openSlotsQt) {
        //set Col=1 at front, before skills so that it doesn't have to calculate where is the column.
        OpenSlotCounter openSlotCounter = (OpenSlotCounter) this.jtMainLista.getModel().getValueAt(cidadeControl.getModelRowIndex(), 1);
        openSlotCounter.setOpenSlotQt(openSlotsQt);
        this.jtMainLista.getModel().setValueAt(openSlotCounter, cidadeControl.getModelRowIndex(), 1);
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
        this.qtCidades.setText(getMainLista().getRowCount() + "");
    }

    private void doPrintTag(Cidade cidade) {
        try {
            getMapaControler().printTag(cidade.getLocal());
        } catch (NullPointerException ex) {
            this.doTagHide();
        }
    }

    private void doAddTabs() {
        //config tabs
        if (cenarioFacade.hasOrdensCidade(cenario)) {
            detalhesCidade.addTab(labels.getString("ACAO"),
                    new javax.swing.ImageIcon(getClass().getResource("/images/right.gif")),
                    stOrdens, labels.getString("ORDERNS.TOOLTIP"));
        }
        detalhesCidade.addTab(labels.getString("RESULTADOS"),
                new javax.swing.ImageIcon(getClass().getResource("/images/write-document-20x20.png")),
                stResults, labels.getString("RESULTADOS.TOOLTIP"));
        if (cenarioFacade.hasResourceManagement(cenario)) {
            detalhesCidade.addTab(labels.getString("ESTOQUES"),
                    new javax.swing.ImageIcon(getClass().getResource("/images/financas.gif")),
                    stProdutos, labels.getString("ESTOQUE.TOOLTIP"));
        }
        detalhesCidade.addTab(labels.getString("PRESENCAS"),
                new javax.swing.ImageIcon(getClass().getResource("/images/hex_personagem.gif")),
                stPersonagens, labels.getString("PRESENCA.TOOLTIP"));
        stResults.setGuiConfig("GuiCityResults");
    }

    public void doMudaCidadeClear() {
        this.doTagHide();
        stProdutos.setListModelClear();
        stPersonagens.setListModelClear();
        stResults.setText("", "");
    }

    public void doMudaCidade(Cidade cidade) {
        doPrintTag(cidade);
        stProdutos.setListModel(CidadeConverter.getProdutoModel(cidade));
        stPersonagens.setListModel(CidadeConverter.getPresencasModel(cidade));
        final String popupTitle = labels.getString("RESULTADOS.OF") + ": " + cidadeFacade.getNomeCoordenada(cidade);
        stResults.setText(popupTitle, cidadeControl.getResultados(cidade));
        stOrdens.doMudaActor(cidade);
        if (jogadorFacade.isMine(cidade, WorldFacadeCounselor.getInstance().getJogadorAtivo())
                && cidadeFacade.isAtivo(cidade)) {
            //can receive orders
            stOrdens.doMudaActor(cidade);
        } else {
            //refem ou morto, nao pode dar ordem
            //forca selecao para vazio, limpando quadro de parametros
            stOrdens.doOrdemClear();
        }
    }

    @Override
    protected int getComboFiltroSize() {
        return this.comboFiltro.getModel().getSize();
    }
}
