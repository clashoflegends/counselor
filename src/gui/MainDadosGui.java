/*
 * MainDadosGui.java
 *
 * Created on April 23, 2008, 11:12 AM
 */
package gui;

import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import gui.tabs.TabAcoesGui;
import gui.tabs.TabArtefatosGui;
import gui.tabs.TabCidadesGui;
import gui.tabs.TabExercitosGui;
import gui.tabs.TabFeiticosGui;
import gui.tabs.TabFinancesGui;
import gui.tabs.TabHexagonosGui;
import gui.tabs.TabNacoesGui;
import gui.tabs.TabOrdensGui;
import gui.tabs.TabPackagesGui;
import gui.tabs.TabPartidaGui;
import gui.tabs.TabPersonagensGui;
import gui.tabs.TabTipoTropasGui;
import java.io.Serializable;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class MainDadosGui extends javax.swing.JPanel implements Serializable {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final TabPersonagensGui tabPersonagem;

    /**
     * Creates new form MainDadosGui
     */
    public MainDadosGui() {
        initComponents();
        MapaControler mapaControl = WorldFacadeCounselor.getInstance().getMapaControler();
        // prepara as tabs
        if (!SettingsManager.getInstance().isConfig("SetTabPosition", "1", "1")) {
            this.jTabbedPane1.setTabPlacement(SettingsManager.getInstance().getConfigAsInt("SetTabPosition"));
        }
        if (SettingsManager.getInstance().isWorldBuilder()) {
//            this.addTabBase(new TabWorldBuilderGui(labels.getString("WB.TAB.TITLE"), labels.getString("WB.TAB.HINT"), mapaControl));
        }
        this.tabPersonagem = new TabPersonagensGui(labels.getString("PERSONAGENS"), labels.getString("PERSONAGENS.DISPONIVEL"), mapaControl);
        this.addTabBase(this.tabPersonagem);
        this.addTabBase(new TabCidadesGui(labels.getString("CIDADES"), labels.getString("CIDADES.DISPONIVEL"), mapaControl));
        this.addTabBase(new TabExercitosGui(labels.getString("EXERCITOS"), labels.getString("EXERCITOS.DISPONIVEL"), mapaControl));
        this.addTabBase(new TabArtefatosGui(labels.getString("ARTEFATOS"), labels.getString("ARTEFATOS.DISPONIVEL"), mapaControl));
        if (WorldFacadeCounselor.getInstance().isBattleRoyal()) {
            this.addTabBase(new TabHexagonosGui(labels.getString("LOCALS"), labels.getString("LOCAL.LIST"), mapaControl));
        }
        this.addTabBase(new TabNacoesGui(labels.getString("NACOES"), labels.getString("NACOES"), mapaControl));
        this.addTabBase(new TabFinancesGui(labels.getString("FINANCAS"), labels.getString("FINANCAS"), mapaControl));
        if (WorldFacadeCounselor.getInstance().isStartupPackages() && WorldFacadeCounselor.getInstance().getTurno() == 0) {
            this.addTabBase(new TabPackagesGui(labels.getString("STARTUP"), labels.getString("STARTUP.DISPONIVEL"), mapaControl));
        }
        this.addTabBase(new TabPartidaGui(labels.getString("PARTIDA"), labels.getString("PARTIDA.TOOLTIP")));
        this.addTabBase(new TabAcoesGui(labels.getString("ACOES"), labels.getString("ACOES.DISPONIVEL")));
        this.addTabBase(new TabTipoTropasGui(labels.getString("TROPAS"), labels.getString("TROPAS.DISPONIVEL")));
        if (WorldFacadeCounselor.getInstance().isSpells()) {
            this.addTabBase(new TabFeiticosGui(labels.getString("FEITICOS"), labels.getString("FEITICOS.DISPONIVEL")));
        }
        //this.addTabBase(new TabOrdensGui("Ordens", "Ordens a enviar"));

    }

    //** Adiciona uma Tab ao painel principal de exibição */
    private void addTabBase(TabBase panel) {
        this.jTabbedPane1.addTab(panel.getTitle(), panel.getIcone(), panel, panel.getDica());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();

        jScrollPane1.setBorder(null);
        jScrollPane1.setViewportView(jTabbedPane1);
        jTabbedPane1.getAccessibleContext().setAccessibleName("Personagem"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables

    public TabPersonagensGui getTabPersonagem() {
        return this.tabPersonagem;
    }
}
