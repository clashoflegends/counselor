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
import gui.tabs.TabLocationsGui;
import gui.tabs.TabNacoesGui;
import gui.tabs.TabOrdensGui;
import gui.tabs.TabPackagesGui;
import gui.tabs.TabPartidaGui;
import gui.tabs.TabPersonagensGui;
import gui.tabs.TabTipoTropasGui;
import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class MainDadosGui extends javax.swing.JPanel implements Serializable {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final TabPersonagensGui tabPersonagem;
    private static final Log log = LogFactory.getLog(MainDadosGui.class);

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
        this.addTabBase(new TabLocationsGui(labels.getString("LOCALS"), labels.getString("LOCAL.LIST"), mapaControl));
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
        if (SettingsManager.getInstance().isConfig("GuiShowLocationsTab", "1", "0")) {
            this.addTabBase(new TabOrdensGui("Ordens", labels.getString("ORDENS.SAVED")));
        }
        restoreAndTrackSelectedTab();
    }

    /**
     * Reopen on the tab the player last used, then remember the selection going forward. Keyed by tab
     * TITLE (not index) because the tab set varies per game/turn (Spells, Startup packages, Locations
     * tab are conditional); an unmatched title (e.g. after a language switch) just falls back to the
     * first tab. Listener is attached after the restore so construction-time tab adds don't fire it.
     */
    private void restoreAndTrackSelectedTab() {
        final String lastTab = SettingsManager.getInstance().getConfig("LastDataTab", "");
        if (!lastTab.isEmpty()) {
            for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
                if (lastTab.equals(jTabbedPane1.getTitleAt(i))) {
                    jTabbedPane1.setSelectedIndex(i);
                    break;
                }
            }
        }
        jTabbedPane1.addChangeListener(e -> {
            int sel = jTabbedPane1.getSelectedIndex();
            if (sel >= 0) {
                SettingsManager.getInstance().setConfig("LastDataTab", jTabbedPane1.getTitleAt(sel));
            }
        });
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
