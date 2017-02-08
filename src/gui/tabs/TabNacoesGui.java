/*
 * TabNacoesGui.java
 *
 * Created on April 23, 2008, 11:37 AM
 */
package gui.tabs;

import baseLib.GenericoComboObject;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.JogadorFacade;
import business.facade.NacaoFacade;
import business.facades.WorldFacadeCounselor;
import control.MapaControler;
import control.NacaoControler;
import control.services.FiltroConverter;
import control.services.NacaoConverter;
import gui.TabBase;
import gui.services.IAcaoGui;
import gui.services.LimitTableCellRenderer;
import gui.subtabs.SubTabBaseList;
import gui.subtabs.SubTabOrdem;
import gui.subtabs.SubTabTextArea;
import java.io.Serializable;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.Habilidade;
import model.HabilidadeNacao;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class TabNacoesGui extends TabBase implements Serializable, IAcaoGui {

    private static final Log log = LogFactory.getLog(TabNacoesGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private NacaoControler nacaoControl;
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final JogadorFacade jogadorFacade = new JogadorFacade();
    private final SubTabTextArea stResults = new SubTabTextArea();
    private final SubTabTextArea stCombats = new SubTabTextArea();
    private final SubTabBaseList stDiplomacy = new SubTabBaseList();
    private final SubTabBaseList stTroops = new SubTabBaseList();
    private SubTabOrdem stOrdens;
    private LimitTableCellRenderer ltcr;

    public TabNacoesGui(String titulo, String dica, MapaControler mapaControl) {
        initComponents();
        //Basico
        setIcone("/images/nation-icon.png");
        setTitle(titulo);
        setDica(dica);
        this.setMapaControler(mapaControl);
        initConfig();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        comboFiltro = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        qtNacoes = new javax.swing.JLabel();
        detalhesNacao = new javax.swing.JTabbedPane();

        jScrollPane3.setBorder(null);

        jtMainLista.setAutoCreateRowSorter(true);
        jtMainLista.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
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

        jLabel2.setText(labels.getString("TOTAL:")); // NOI18N

        comboFiltro.setModel(getDefaultComboBoxModelTodosProprio());

        jLabel1.setText(labels.getString("LISTAR:")); // NOI18N

        qtNacoes.setText("66666"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(qtNacoes)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(qtNacoes))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
            .addComponent(detalhesNacao)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(detalhesNacao, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE))
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
    private javax.swing.JTabbedPane detalhesNacao;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jtMainLista;
    private javax.swing.JLabel qtNacoes;
    // End of variables declaration//GEN-END:variables

    public JTable getMainLista() {
        return jtMainLista;
    }

    public final void setMainModel(TableModel model) {
        //clear stuff
        stResults.setText("");
        stCombats.setText("");
        stDiplomacy.setListModelClear();
        stTroops.setListModelClear();
        //set model
        this.jtMainLista.setModel(model);
        //confid red background
        jtMainLista.getColumnModel().getColumn(NacaoConverter.ORDEM_COL_INDEX_START).setCellRenderer(ltcr);
        //auto adjust columns
        this.doConfigTableColumns(jtMainLista);
        this.updateGui();
        this.doTagHide();
        //trigger change
        this.jtMainLista.getSelectionModel().setSelectionInterval(0, 0);
    }

    public GenericoComboObject getFiltro() {
        return (GenericoComboObject) comboFiltro.getSelectedItem();
    }

    private void updateGui() {
        this.qtNacoes.setText(getMainLista().getRowCount() + "");
    }

    private void initConfig() {
        stOrdens = new SubTabOrdem(this, getMapaControler());
        //configura grid
        jtMainLista.setAutoCreateColumnsFromModel(true);
        jtMainLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtMainLista.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jtMainLista.setAutoCreateRowSorter(true);
        comboFiltro.setName("comboFiltro");
        comboFiltro.setActionCommand("comboFiltro");
        comboFiltro.setModel(FiltroConverter.getFiltroComboModelByJogador(WorldManager.getInstance().getPartida().getJogadorAtivo(), 6));
        comboFiltro.setSelectedIndex(this.getFiltroDefault());

        //Cria o Controle da lista 
        nacaoControl = new NacaoControler(this);
        stResults.setFontText(detalhesNacao.getFont());
        stCombats.setFontText(detalhesNacao.getFont());
        addTabs();
        //adiciona listeners
        comboFiltro.addActionListener(nacaoControl);
        jtMainLista.getSelectionModel().addListSelectionListener(nacaoControl);
        //rendered
        if (WorldFacadeCounselor.getInstance().isNationPackages()) {
            ltcr = new LimitTableCellRenderer(WorldFacadeCounselor.getInstance().getNationPackagesLimit());
        }
        final TableModel model = nacaoControl.getMainTableModel(this.getFiltro());
        this.setMainModel(model);
    }

    private void addTabs() {
        if (cenarioFacade.hasOrdensNacao(WorldFacadeCounselor.getInstance().getPartida())) {
            detalhesNacao.addTab(labels.getString("STARTUP"),
                    new javax.swing.ImageIcon(getClass().getResource("/images/package_icon.gif")),
                    stOrdens, labels.getString("STARTUP.TOOLTIP"));
        }
        detalhesNacao.addTab(labels.getString("RESULTADOS"),
                new javax.swing.ImageIcon(getClass().getResource("/images/write-document-20x20.png")),
                stResults, labels.getString("RESULTADOS.TOOLTIP"));
        detalhesNacao.addTab(labels.getString("RESULTADOS.COMBAT"),
                new javax.swing.ImageIcon(getClass().getResource("/images/combat.png")),
                stCombats, labels.getString("RESULTADOS.COMBAT.TOOLTIP"));
        detalhesNacao.addTab(labels.getString("DIPLOMACY"),
                new javax.swing.ImageIcon(getClass().getResource("/images/diplomacy.gif")),
                stDiplomacy, labels.getString("DIPLOMACY.TOOLTIP"));
        detalhesNacao.addTab(labels.getString("TROPAS"),
                new javax.swing.ImageIcon(getClass().getResource("/images/hex_exercito.gif")),
                stTroops, labels.getString("TROPAS.DISPONIVEL"));
    }

    @Override
    public void setValueAt(String[] ordemDisplay, int ordIndex) {
        //set how many points were selected
        final int points = acaoFacade.getPointsSetup(stOrdens.getActor().getNacao());
        this.jtMainLista.getModel().setValueAt(points,
                nacaoControl.getModelRowIndex(),
                NacaoConverter.ORDEM_COL_INDEX_START);
    }

    private void setResults(Nacao nacao) {
        String hab = labels.getString("HABILIDADES.ESPECIAIS") + "\n";
        for (HabilidadeNacao elem : nacaoFacade.getHabilidadesNacao(nacao)) {
            hab += String.format("- %s\n", elem.getNome());
        }
        for (Habilidade elem : nacaoFacade.getHabilidades(nacao)) {
            try {
                if (!elem.isHidden()) {
                    hab += String.format("- %s\n", elem.getNome());
                }
            } catch (NullPointerException ex) {
            }
        }
        try {
            for (String msg : nacaoFacade.getMensagensResultsRumoresEncontros(nacao)) {
//                hab += "\n\n\n" + msg.replace(',', '\n');
                hab += "\n\n\n" + msg;
            }
        } catch (NullPointerException ex) {
            //just skip
            hab += labels.getString("?");
        }
        stResults.setText(hab);
    }

    private void setCombats(Nacao nacao) {
        String hab = "\n";
        try {
            for (String msg : nacaoFacade.getMensagensCombatesDuelos(nacao)) {
//                hab += "\n\n\n" + msg.replace(',', '\n');
                hab += "\n\n\n" + msg;
            }
        } catch (NullPointerException ex) {
            //just skip
            hab += labels.getString("?");
        }
        stCombats.setText(hab);
    }

    public void doMudaNacao(Nacao nacao) {
        try {
            getMapaControler().printTag(nacaoFacade.getLocal(nacao));
        } catch (NullPointerException ex) {
            this.doTagHide();
        }
        stDiplomacy.setListModel(nacaoControl.getRelacionamentoTableModel(nacao));
        stTroops.setListModel(nacaoControl.getTropaTableModel(nacao));
        setResults(nacao);
        setCombats(nacao);
        if (jogadorFacade.isMine(nacao, WorldFacadeCounselor.getInstance().getJogadorAtivo())
                && nacaoFacade.isAtiva(nacao)) {
            //can receive orders
            stOrdens.doMudaActor(nacao);
        } else {
            //refem ou morto, nao pode dar ordem
            //forca selecao para vazio, limpando quadro de parametros
            stOrdens.doOrdemClear();
        }
    }
}
