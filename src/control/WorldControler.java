/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import gui.services.ClipboardHelper;

import baseLib.BaseModel;
import business.BusinessException;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.CidadeFacade;
import business.facade.NacaoFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import business.facade.PointsFacade;
import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import control.services.NacaoConverter;
import control.services.OrdersHashService;
import control.support.ControlBase;
import control.support.DispatchManager;
import control.support.DisplayPortraitsManager;
import gui.MainResultWindowGui;
import gui.services.Toast;
import gui.accessories.MainAboutBox;
import gui.accessories.MainSettingsGui;
import gui.charts.DataSetForChart;
import gui.services.AppIcon;
import gui.services.BusyGlass;
import gui.services.ComponentFactory;
import gui.services.TokenSetupDialog;
import gui.services.VictoryDashboardDialog;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ProgressMonitor;
import model.Cidade;
import model.Comando;
import model.ComandoDetail;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.Personagem;
import model.PersonagemOrdem;
import model.VictoryPointsGame;
import model.World;
import modelWeb.PartidaJogadorWebInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.PersistFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.PersistenceException;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
import persistenceCommons.WebCounselorManager;
import persistenceCommons.XmlManager;
import persistenceLocal.PathFactory;
import utils.CounterStringInt;

/**
 *
 * @author gurgel
 */
public class WorldControler extends ControlBase implements Serializable, ActionListener, PropertyChangeListener {

    private static final Log log = LogFactory.getLog(WorldControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final JFileChooser fcResults;
    private final JFileChooser fcOrders;
    private final JFileChooser fcWorld;
    private final JFileChooser fcMapImage;
    private boolean saved = false;
    private boolean savedWorld = false;
    private boolean msgSubmitReady = false;
    private boolean loadingEgf = false; // EDT-confined guard: blocks a concurrent open while one is in progress
    private int actionsSlots = 0;
    // Session-scoped memory of the stand-by/normal choice per owner login (lowercased), populated when the
    // player ticks "don't ask again this session" in the on-behalf submit dialog. Cleared on app restart.
    private final java.util.Map<String, Boolean> shadowChoiceRemembered = new java.util.HashMap<>();
    private int actionsCount = 0;
    private File currentResultsFile = null; // the EGF currently open (both manual-open and autoload paths); source for "set autoload to current"
    private MainResultWindowGui gui = null;
    private final AcaoFacade acaoFacade = new AcaoFacade();
    private final OrdemFacade ordemFacade = new OrdemFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final WorldFacadeCounselor WFC = WorldFacadeCounselor.getInstance();

    private ProgressMonitor progressMonitor;

    public WorldControler(MainResultWindowGui aGui) {
        //initialize file choosers
        this.fcResults = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
        fcResults.resetChoosableFileFilters();
        fcResults.setFileFilter(PathFactory.getFilterResults());
        this.fcOrders = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
        fcOrders.resetChoosableFileFilters();
        fcOrders.setFileFilter(PathFactory.getFilterAcoes());
        this.fcWorld = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
        fcWorld.resetChoosableFileFilters();
        fcWorld.setFileFilter(PathFactory.getFilterWorld());
        this.fcMapImage = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
        fcMapImage.resetChoosableFileFilters();
        fcMapImage.setFileFilter(PathFactory.getFilterImages());

        setGui(aGui);

        registerDispatchManager();
        registerDispatchManagerForMsg(DispatchManager.SET_LABEL_MONEY);
        registerDispatchManagerForMsg(DispatchManager.SAVE_WORLDBUILDER_FILE);
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_AUTOSAVE);
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_COUNT);
        registerDispatchManagerForMsg(DispatchManager.STATUS_BAR_MSG);
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
        registerDispatchManagerForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL);
        registerDispatchManagerForMsg(DispatchManager.SPLIT_PANE_CHANGED);

    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JButton) {
            JButton jbTemp = (JButton) actionEvent.getSource();
            doActionPerformendButtom(jbTemp);
        } else if (actionEvent.getSource() instanceof JToggleButton) {
            JToggleButton jbTemp = (JToggleButton) actionEvent.getSource();
            doActionPerformedToggle(jbTemp);
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    private void doActionPerformedToggle(JToggleButton jbTemp) {
        if (null == jbTemp.getActionCommand()) {
            log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
            return;
        }
        switch (jbTemp.getActionCommand()) {
            case "pcPathDraw":
                doDrawPjPaths();
                break;
            case "drawPathArmy":
                doDrawArmyPaths(jbTemp);
                break;
            case "drawPathResources":
                doDrawResourcePaths(jbTemp);
                break;
            case "drawScoutTargets":
                doScoutTargets(jbTemp);
                break;
            case "drawFogWar":
                doDrawFogOfWar(jbTemp);
                break;
            case "drawShowCityCap":
                doDrawShowCityCap(jbTemp);
                break;
            case "drawDisplayPortraits":
                doPortraits(jbTemp);
                break;
            default:
                log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
                break;
        }
    }

    private void doActionPerformendButtom(JButton jbTemp) throws HeadlessException {
        if (null == jbTemp.getActionCommand()) {
            log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
        }
        //monta csv com as ordens
        switch (jbTemp.getActionCommand()) {
            case "jbOpen":
                doOpen(jbTemp);
                break;
            case "jbSave":
                doSave(jbTemp, false);
                break;
            case "jbSaveWorld":
                doSaveWorld(jbTemp);
                break;
            case "jbExportMap":
                doMapSave(jbTemp);
                break;
            case "jbCopy":
                doCopy();
                break;
            case "jbEmailList":
                doEmailList();
                break;
            case "jbSend":
                doSend(jbTemp);
                break;
            case "jbAbout":
                doAbout();
                break;
            case "jbHexview":
                doHexview();
                break;
            case "jbConfig":
                doConfig();
                break;
            case "jbLoad":
                doLoad(jbTemp);
                break;
            case "joao":
                break;
            case "jbScoreGraph":
                doDataVictoryPointsPerNation();
                break;
            case "jbVictoryDashboard":
                VictoryDashboardDialog.show(this.gui, labels, this);
                break;
            case "jbGraphSingleTurn":
                //disabled: doDataVictoryPointsPerTeam();
                break;
            case "jbGraphAllTurns":
                doDataVictoryPointHistoryAllTurns();
                break;
            case "jbGraphKeyCityPerTeam":
                //disable Consolidated: doDataKeyCityPerTeam();
                break;
            case "jbGraphKeyCityPerNation":
                doDataKeyCityPerNation();
                break;
            case "jbGraphDomination":
                doGraphBattleRoyale();
                break;
            default:
                log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
                break;
        }
    }

    /**
     * Carrega o arquivo de ordens gerado pelo client.
     *
     * @param jbTemp
     * @throws HeadlessException
     */
    private void doLoad(JButton jbTemp) throws HeadlessException {
        String nomeArquivo = getCommandFileName();
        //salva o arquivo
        fcOrders.setSelectedFile(new File(nomeArquivo));
        //Create a file chooser
        //In response to a button click:
        int returnVal = fcOrders.showOpenDialog(jbTemp);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fcOrders.getSelectedFile();
            log.info(labels.getString("LOADING: ") + file.getName());
            setComando(file);
            this.saved = false;
            this.savedWorld = false;
        } else {
            this.getGui().setStatusMsg(labels.getString("LOAD.CANCELLED"));
        }
    }

    private String getCommandFileName() {
        Partida partida = WorldManager.getInstance().getPartida();
        String nomeArquivo = String.format(labels.getString("FILENAME.ORDERS"),
                partida.getId(), partida.getTurno() + 1,
                partida.getJogadorAtivo().getLogin());
        return nomeArquivo;
    }

    private String getCommandFileMask() {
        Partida partida = WorldManager.getInstance().getPartida();
        String nomeArquivo = String.format(labels.getString("FILENAME.ORDERS.MASK"),
                partida.getId(), partida.getTurno() + 1);
        return nomeArquivo;
    }

    /**
     * Salva ordens para o arquivo a ser enviado para o Engine
     *
     * @param jbTemp
     * @throws HeadlessException
     */
    private File doSave(Component jbTemp, boolean forceMissingPopup) throws HeadlessException {
        Partida partida = WFC.getPartida();
        Jogador jogadorAtivo = partida.getJogadorAtivo();
        Comando comando = new Comando();
        comando.setInfos(partida);
        // Stamp the SENDER (this machine's configured player login) so the server can show a "Sender:"
        // line on the results email when submitting on-behalf. Owner stays jogadorAtivo (setInfos).
        // Empty when unknown (paste-token user who hasn't been backfilled yet) -> server shows no line.
        comando.setSenderLogin(SettingsManager.getInstance().getConfig("playerLogin", ""));
        String missingActionMsg = doSaveActorActions(jogadorAtivo, comando);
        if (comando.size() == 0) {
            Toast.show(javax.swing.SwingUtilities.getWindowAncestor(this.getGui()),
                    labels.getString("NONE.ORDERS"),
                    javax.swing.UIManager.getIcon("OptionPane.informationIcon"));
            this.getGui().setStatusMsg(labels.getString("NONE.ORDERS"));
            //we're done here, nothing to save
            return null;
        }
        if (!missingActionMsg.equalsIgnoreCase("") && SettingsManager.getInstance().isConfig("ActionsMissingPopup", "1", "0")) {
            if (forceMissingPopup || !SettingsManager.getInstance().isAutoSaveActions()) {
                Toast.show(javax.swing.SwingUtilities.getWindowAncestor(this.getGui()),
                        missingActionMsg,
                        javax.swing.UIManager.getIcon("OptionPane.warningIcon"));
            }
        }
        if (missingActionMsg.equalsIgnoreCase("") && !this.msgSubmitReady) {
            Toast.show(javax.swing.SwingUtilities.getWindowAncestor(this.getGui()),
                    labels.getString("ORDERS.READY.SUBMIT"),
                    javax.swing.UIManager.getIcon("OptionPane.warningIcon"));
            this.getGui().blinkSubmitReady(); // pulse the submit button in sync with the ready toast
            this.msgSubmitReady = true;
        }

        //define nome default
        String fileName = String.format(labels.getString("FILENAME.ORDERS"), partida.getId(), partida.getTurno() + 1, partida.getJogadorAtivo().getLogin());

        //salva o arquivo
        File ret = null;
        if (!this.saved) {
            //monta o dialogo
            //define default
            fcOrders.setSelectedFile(new File(fileName));
            if (SettingsManager.getInstance().isConfig("SaveOrdersNoPrompt", "1", "0")) {
                //accept the default name + folder (same folder as the rr.egf) without prompting
                fcOrders.setSelectedFile(new File(fcOrders.getCurrentDirectory(), fileName));
                ret = doFileSave(comando, missingActionMsg);
                if (ret != null) {
                    log.info(String.format("Auto-saved Server file[%s]: %s", SysApoio.getPidOs(), ret.getAbsolutePath()));
                }
            } else if (fcOrders.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
                //exibe dialogo
                ret = doFileSave(comando, missingActionMsg);
                log.info(String.format("Saved Server file[%s]: %s", SysApoio.getPidOs(), ret.getAbsolutePath()));
            } else {
                this.getGui().setStatusMsg(labels.getString("SAVE.CANCELLED"));
            }
        } else {
            //salva com o nome anterior.
            ret = doFileSave(comando, missingActionMsg);
        }
        return ret;
    }

    private void doSaveWorld(JButton jbTemp) {
        World world = WorldManager.getInstance().getWorld();
        if (!this.savedWorld) {
            //monta o dialogo
            fcWorld.setSelectedFile(new File(PathFactory.getWorldFileName(world)));
            //exibe dialogo
            if (fcWorld.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
                saveWorldFile(world);
            } else {
                this.getGui().setStatusMsg(labels.getString("SAVE.CANCELLED"));
            }
        } else {
            saveWorldFile(world);
        }
    }

    private File doFileSave(Comando comando, String missingActionMsg) {
        File ret = null;
        try {
            ret = fcOrders.getSelectedFile();
            WFC.doSaveOrdens(comando, ret);
            this.getGui().setStatusMsg(missingActionMsg + " " + String.format(labels.getString("ORDENS.SALVAS"), comando.size(), fcOrders.getSelectedFile().getName()));
            this.saved = true;
        } catch (BusinessException ex) {
            log.error(ex.getMessage());
            SysApoio.showDialogError(ex.getMessage(), this.getGui());
            this.getGui().setStatusMsg(ex.getMessage());
        }
        return ret;
    }

    private void doMapSave(JButton jbTemp) {
        //define nome default
        Partida partida = WorldManager.getInstance().getPartida();
        String nomeArquivo = String.format(labels.getString("FILENAME.MAP"),
                partida.getId(), partida.getTurno(), partida.getJogadorAtivo().getLogin());

        fcMapImage.setSelectedFile(new File(nomeArquivo));
        //exibe dialogo

        if (fcMapImage.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
            saveMapFile();
        } else {
            this.getGui().setStatusMsg(labels.getString("SAVE.CANCELLED"));
        }
    }

    private void doAbout() throws HeadlessException {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this.getGui());
        JDialog dAbout = new JDialog(owner, java.awt.Dialog.ModalityType.MODELESS);
        AppIcon.applyTo(dAbout);
        MainAboutBox stAbout = new MainAboutBox();
        dAbout.add(stAbout);
        dAbout.setTitle(labels.getString("MENU.ABOUT"));
        dAbout.setLocationRelativeTo(this.getGui());
        dAbout.pack();
        dAbout.setVisible(true);
    }

    private void doHexview() {
        WFC.getMapaControler().doHexViewToggle();
    }

    private void doConfig() throws HeadlessException {

        MainSettingsGui settingPanel = new MainSettingsGui(this.currentResultsFile);

        int option = JOptionPane.showOptionDialog(getGui(), settingPanel, labels.getString("MENU.CONFIG"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
                new javax.swing.ImageIcon(getClass().getResource("/images/icon_customize.gif")), null, null);

        if (option == JOptionPane.OK_OPTION) {
            log.debug("Saving settings from option window.");

            Enumeration keys = SettingsManager.getInstance().listConfigs();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                //FIXME Comparation is not working. All settings are saving to file.
                if (SettingsManager.getInstance().isConfig(key, SettingsManager.getInstance().getConfig(key), "")) {
                    SettingsManager.getInstance().doConfigSave(key);
                }
            }

        } else {
            SettingsManager.getInstance().listConfigs();
            log.debug("Cancel settings option window.");

            Enumeration keys = SettingsManager.getInstance().listConfigs();
            while (keys.hasMoreElements()) {
                SettingsManager.getInstance().doConfigRestore((String) keys.nextElement());
            }
        }
    }

    /**
     * Transitional method for development purpose. Must be deleted after new methods in SettingsManager would be implemented.
     *
     * @param props
     * @return * private Map<String, String> getMapProperties(SettingsManager settingsManager) { Map<String, String> mapProperties = new
     * HashMap<String, String>(); mapProperties.put("loadDir", settingsManager.getProperties("loadDir")); mapProperties.put("saveDir",
     * settingsManager.getProperties("saveDir")); return mapProperties; }
     */
    private void doCopy() throws HeadlessException {
        //config text Area
        JTextArea jtaResultado = new javax.swing.JTextArea(80, 20);
        jtaResultado.setLineWrap(false);
        jtaResultado.setWrapStyleWord(false);
        jtaResultado.setEditable(false);
        //carrega o texto
        jtaResultado.setText(listaOrdens());
        //copy para o clipboard
        jtaResultado.selectAll();
        jtaResultado.copy();
        this.getGui().setStatusMsg(labels.getString("COPIAR.ACOES.STATUS"));
        jtaResultado.select(0, 0);
        if (SettingsManager.getInstance().getConfig("CopyActionsPopUp", "1").equals("1")) {
            //scroll pane
            JScrollPane jsp = new javax.swing.JScrollPane(jtaResultado);
            //configura jDialog
            JDialog dAbout = new JDialog(new JFrame(), true);
            AppIcon.applyTo(dAbout);
            dAbout.setTitle(labels.getString("COPIAR.ACOES"));
            dAbout.setAlwaysOnTop(true);
            dAbout.setPreferredSize(new Dimension(600, 400));
            dAbout.add(jsp);
            dAbout.setLocationRelativeTo(this.getGui());
            dAbout.pack();
            dAbout.setVisible(true);
        }
    }

    private void doEmailList() throws HeadlessException {
        //config text Area
        JTextArea jtaResultado = new javax.swing.JTextArea(80, 20);
        jtaResultado.setLineWrap(false);
        jtaResultado.setWrapStyleWord(false);
        jtaResultado.setEditable(false);
        //carrega o texto
        jtaResultado.setText(listaEmails());
        //copy para o clipboard
        jtaResultado.selectAll();
        jtaResultado.copy();
        this.getGui().setStatusMsg(labels.getString("COPIAR.EMAILS.STATUS"));
        jtaResultado.select(0, 0);
        if (SettingsManager.getInstance().getConfig("CopyEmailListPopUp", "1").equals("1")) {
            //scroll pane
            JScrollPane jsp = new javax.swing.JScrollPane(jtaResultado);
            //configura jDialog
            JDialog dAbout = new JDialog(new JFrame(), true);
            AppIcon.applyTo(dAbout);
            dAbout.setTitle(labels.getString("COPIAR.EMAILS"));
            dAbout.setAlwaysOnTop(true);
            dAbout.setPreferredSize(new Dimension(600, 400));
            dAbout.add(jsp);
            dAbout.setLocationRelativeTo(this.getGui());
            dAbout.pack();
            dAbout.setVisible(true);
        }
    }

    /**
     * Salva ordens para o arquivo a ser enviado para o Engine
     *
     * @param jbTemp
     * @throws HeadlessException
     */
    private void doSend(JButton jbTemp) throws HeadlessException {
        /*
         * checa se ja salvou nesta sessao
         * se sim, salva com o mesmo nome sem exibir o dialogo 
         * se nao, pede para salvar pela primeira vez (exibe o dialogo)
         */
        File attachment = doSave(jbTemp, true);
        if (attachment == null) {
            this.getGui().setStatusMsg(labels.getString("ENVIAR.FALTOU.ARQUIVO"));
            SysApoio.showDialogError(labels.getString("ENVIAR.FALTOU.ARQUIVO"), this.getGui());
            return;
        }
        /*
         * Try to post. The HTTP upload can take several seconds, so run it on a SwingWorker with a
         * busy overlay instead of blocking (freezing) the EDT. doPrepPost reads the model, so keep it
         * on the EDT; the worker only does the network call.
         */
        this.getGui().setStatusMsg(labels.getString("ENVIAR.POST.JUDGE"));
        if (!ensurePlayerToken()) {
            this.getGui().setStatusMsg(labels.getString("TOKEN.REQUIRED.STATUS"));
            return; // no token set - cannot upload (no shared-secret fallback anymore)
        }
        // Stand-by / SHADOW option (Phase-2 #4): only OFFER it when a stand-by submit could actually succeed,
        // so the player is never shown a choice that is guaranteed to be rejected. Pre-reqs the client can
        // check: (1) on-behalf — the configured sender login differs from the nation owner (jogadorAtivo);
        // (2) the loaded nation carries a per-EGF token (cdToken > 0) — the Site's shadow gate mandates
        // pEgfToken, so without it a stand-by is a certain 401. The per-player token is already guaranteed by
        // ensurePlayerToken() above. (fl_accept_shadow / on-behalf policy are Site-side and can't be
        // pre-checked; if those reject, the Site's message is surfaced.) When stand-by isn't offered we just
        // submit a normal on-behalf set with no prompt.
        boolean shadow = false;
        final String senderLogin = SettingsManager.getInstance().getConfig("playerLogin", "").trim();
        final String ownerLogin = WFC.getPartida().getJogadorAtivo().getLogin();
        final List<Nacao> nacoesAtivo = WFC.getNacoesJogadorAtivo();
        final boolean egfTokenPresent = !nacoesAtivo.isEmpty() && nacoesAtivo.get(0).getCdToken() > 0;
        final boolean onBehalf = !senderLogin.isEmpty() && !senderLogin.equalsIgnoreCase(ownerLogin);
        if (onBehalf && egfTokenPresent) {
            final String ownerKey = ownerLogin.toLowerCase();
            if (shadowChoiceRemembered.containsKey(ownerKey)) {
                // Player ticked "don't ask again this session" for this owner earlier — reuse that choice.
                shadow = shadowChoiceRemembered.get(ownerKey);
            } else {
                final javax.swing.JCheckBox dontAsk = new javax.swing.JCheckBox(
                        String.format(labels.getString("SHADOW.PROMPT.REMEMBER"), ownerLogin));
                final Object[] message = {
                    String.format(labels.getString("SHADOW.PROMPT.MSG"), ownerLogin), dontAsk};
                final Object[] options = {
                    labels.getString("SHADOW.OPT.NORMAL"),
                    labels.getString("SHADOW.OPT.STANDBY"),
                    labels.getString("TOKEN.SETUP.CANCEL")};
                final int choice = JOptionPane.showOptionDialog(this.getGui(), message,
                        labels.getString("SHADOW.PROMPT.TITLE"),
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]); // default = normal on-behalf
                if (choice != 0 && choice != 1) { // Cancel / closed
                    this.getGui().setStatusMsg(labels.getString("SAVE.CANCELLED"));
                    return;
                }
                shadow = (choice == 1);
                if (dontAsk.isSelected()) {
                    // Remember the DECISION (normal or stand-by) for this owner for the rest of the session.
                    shadowChoiceRemembered.put(ownerKey, shadow);
                }
            }
        }
        final PartidaJogadorWebInfo info = doPrepPost(attachment, shadow);
        info.setOnBehalf(onBehalf); // so a gate rejection surfaces the site's message (not the generic error)
        final BusyGlass busy = BusyGlass.show(this.getGui(), labels.getString("ENVIAR.POST.JUDGE"));
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return WebCounselorManager.getInstance().doSendViaPost(info);
            }

            @Override
            protected void done() {
                if (busy != null) {
                    busy.dismiss();
                }
                boolean ok;
                try {
                    ok = handleSendResult(get(), attachment, info);
                } catch (Exception ex) {
                    // network failure / PersistenceException
                    WorldControler.this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                    ok = false;
                }
                if (!ok) {
                    WorldControler.this.getGui().setStatusMsg(labels.getString("ENVIAR.ERRO"));
                    SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.INSTRUCTIONS"), labels.getString("ENVIAR.ERRO"), WorldControler.this.getGui());
                }
            }
        }.execute();
    }

    /**
     * Ensures a per-player token is set before an order upload. The token is mandatory from this
     * version on (the shared pToken is no longer sent): if none is stored, prompt for it. Returns
     * true if a token is available (proceed with the upload), false if the player cancelled without
     * setting one - the caller then aborts the upload (the player can still read/compose; they just
     * can't submit until a token is set). Runs on the EDT before the post.
     */
    private boolean ensurePlayerToken() {
        if (!SettingsManager.getInstance().getConfig("playerToken", "").trim().isEmpty()) {
            return true; // already set - WebCounselorManager will send it
        }
        try {
            String tok = TokenSetupDialog.show(this.getGui(), labels);
            return (tok != null && !tok.trim().isEmpty());
        } catch (Throwable t) {
            log.warn("Player token prompt failed: " + t);
            return false; // no token -> abort the upload (posting without it would just 401)
        }
    }

    /**
     * Abre o turno com os resultados enviados pelo engine
     *
     * @param jbTemp
     * @throws HeadlessException
     */
    public void doOpenFile(final File resultsFile) {
        if (loadingEgf) {
            return; // an open is already in progress; ignore re-entrant menu clicks / drops
        }
        // Show a static dim overlay (no spinner: the EDT-bound rebuild below would freeze any animation)
        // so the player sees the app is busy during the ~1-3s open. Then run the actual open on the NEXT
        // EDT cycle via invokeLater, so the overlay gets painted before this same EDT thread is blocked
        // by the parse + GUI rebuild. The work stays entirely on the EDT (no threading) - the overlay is
        // pure visual feedback, removed when done. doAutoLoad (startup) is unaffected; the splash covers it.
        final BusyGlass busy = BusyGlass.show(this.getGui(), labels.getString("OPENING: ") + resultsFile.getName(), false);
        loadingEgf = true; // set after show() so a show() failure can't wedge opening for the session
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                log.info(labels.getString("OPENING: ") + resultsFile.getName() + String.format(" [%s]", SysApoio.getPidOs()));
                WFC.doStart(resultsFile);
                // A new game/turn is loaded: close stale graph/dashboard popups from the previous game.
                ComponentFactory.disposeSecondaryWindows();
                this.setActionsSlots(doCountActorActions(WFC.getJogadorAtivo()));
                log.info(labels.getString("INICIALIZANDO.GUI"));
                getGui().iniciaConfig();
                this.getGui().setStatusMsg(labels.getString("OPENING: ") + resultsFile.getName());
                this.getGui().setOpenFileName(resultsFile.getName());
                this.currentResultsFile = resultsFile;
                this.msgSubmitReady = false;
                this.getGui().stopSubmitBlink(); // kill any running blink from the previous turn
                this.saved = false;
                this.savedWorld = false;
                doAutoLoadCommands(resultsFile);
                File dir = resultsFile.getParentFile();
                fcResults.setCurrentDirectory(dir);
                fcOrders.setCurrentDirectory(dir);
                fcWorld.setCurrentDirectory(dir);
                fcMapImage.setCurrentDirectory(dir);
                client.RecentFiles.add(resultsFile); // record only successful opens (covers menu, drag-drop, recent-menu)
                refreshOrderSyncFromServer(); // order-sync indicator: compare loaded orders to what the server holds
            } catch (BusinessException ex) {
                //parse-time failure: file not found, corrupted, or incompatible version (already a player-readable message)
                log.error("Failed to open results file: " + resultsFile.getName(), ex);
                SysApoio.showDialogError(ex.getMessage(), labels.getString("OPEN.ERRO.TITULO"), this.getGui());
                this.getGui().setStatusMsg(labels.getString("OPEN.ERRO.STATUS") + resultsFile.getName());
            } catch (RuntimeException ex) {
                //GUI-build or otherwise unexpected failure: keep the app alive, show a friendly message, log the stack trace
                log.error("Unexpected error opening results file: " + resultsFile.getName(), ex);
                persistenceCommons.CrashReporter.report(ex, "egf-open:" + resultsFile.getName());
                SysApoio.showDialogError(labels.getString("OPEN.ERRO.INESPERADO"), labels.getString("OPEN.ERRO.TITULO"), this.getGui());
                this.getGui().setStatusMsg(labels.getString("OPEN.ERRO.STATUS") + resultsFile.getName());
            } finally {
                loadingEgf = false;
                if (busy != null) {
                    busy.dismiss();
                }
            }
        });
    }

    private void doOpen(JButton jbTemp) throws HeadlessException {
        //Create a file chooser
        if (SettingsManager.getInstance().isWorldBuilder()) {
            fcResults.addChoosableFileFilter(PathFactory.getFilterWorld());
        }
        int returnVal = fcResults.showOpenDialog(jbTemp);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            SysApoio.setLoadMode("menu");
            doOpenFile(fcResults.getSelectedFile());
        } else {
            log.info(labels.getString("OPEN.CANCELLED") + String.format(" [%s]", SysApoio.getPidOs()));
        }
    }

    private void doAutoLoadCommands(final File resultsFile) {
        if (isLoadTeamOrders()) {
            //check for the file on the same folder with same name
            List<File> commandFiles = PathFactory.getInstance().listCommandFile(resultsFile.getParentFile(), getCommandFileMask());
            for (File ordersFile : commandFiles) {
                if (ordersFile.exists()) {
                    setComando(ordersFile);
                }
            }
        } else {
            final String ordersFile = String.format("%s%s%s", resultsFile.getParent(), File.separator, getCommandFileName());
            //check for the file on the same folder with same name
            final File loadActions = new File(ordersFile);
            if (loadActions.exists()) {
                setComando(loadActions);
            }
        }
    }

    /**
     * Verifica e carrega a partida default definida no properties
     *
     * @param autoLoad
     */
    public void doAutoLoad(String autoLoad) {
        if (autoLoad == null || autoLoad.isEmpty()) {
            return;
        }
        try {
            log.info(labels.getString("AUTOLOADING.OPENING") + autoLoad + String.format(" [%s]", SysApoio.getPidOs()));
            this.getGui().setStatusMsg(labels.getString("AUTOLOADING.OPENING") + autoLoad);
            final File resultsFile = new File(autoLoad);
            WFC.doStart(resultsFile);
            this.setActionsSlots(doCountActorActions(WFC.getJogadorAtivo()));
            getGui().iniciaConfig();
            this.getGui().setOpenFileName(resultsFile.getName());
            this.currentResultsFile = resultsFile;
            String autoLoadActions = SettingsManager.getInstance().getConfig("autoLoadActions", "none");
            if (!autoLoadActions.equals("none") && !autoLoadActions.isEmpty()) {
                //check if there's a defined orders file to be loaded
                final File loadActions = new File(autoLoadActions);
                setComando(loadActions);
            } else {
                doAutoLoadCommands(resultsFile);
            }
            // Autoload parity with doOpenFile: fetch the server's stored order hash so the order-sync
            // indicator (and the submit button's at-rest tint) resolve on startup instead of staying grey.
            refreshOrderSyncFromServer();
            fcOrders.setSelectedFile(resultsFile);
            this.saved = false;
        } catch (BusinessException ex) {
            log.error("Failed to autoload results file: " + autoLoad, ex);
            SysApoio.showDialogError(ex.getMessage(), labels.getString("OPEN.ERRO.TITULO"), this.getGui());
            this.getGui().setStatusMsg(labels.getString("OPEN.ERRO.STATUS") + autoLoad);
        } catch (RuntimeException ex) {
            log.error("Unexpected error autoloading results file: " + autoLoad, ex);
            persistenceCommons.CrashReporter.report(ex, "egf-autoload");
            SysApoio.showDialogError(labels.getString("OPEN.ERRO.INESPERADO"), labels.getString("OPEN.ERRO.TITULO"), this.getGui());
            this.getGui().setStatusMsg(labels.getString("OPEN.ERRO.STATUS") + autoLoad);
        }
    }

    private String listaEmails() {
        Set<String> emailList = new TreeSet<>();
        //find allied nations
        List<Nacao> nationList = NacaoConverter.listaByFiltro("team");
        for (Nacao nation : nationList) {
            if (nation.getOwner().isNpc()) {
                continue;
            }
            emailList.add(nation.getOwner().getEmail());
        }
        //format return String
        String ret = "";
        for (String address : emailList) {
            ret += String.format("%s\n", address);
        }
        return ret;
    }

    private String listaOrdens() {
        String ret = "";
        if (WFC.isStartupPackages() && WFC.getTurno() == 0) {
            ret += listaPackages() + "\n\n";
        }
        ret += listaOrdensBySequence() + "\n\n";
        if (cenarioFacade.hasOrdensNacao(WFC.getPartida())) {
            ret += listaOrdensByNation() + "\n\n";
        }
        if (WFC.hasOrdensCidade()) {
            ret += listaOrdensByCity() + "\n\n";
        }
        if (SettingsManager.getInstance().getConfig("CopyActionsOrder", "1").equals("1")) {
            ret += listaOrdensByPers() + "\n\n";
        }
        return ret;
    }

    private String listaOrdensByPers() {
        String ret = labels.getString("TITLE.LIST.BYCHAR") + ":\n";
        Jogador jogadorAtivo = WFC.getJogadorAtivo();
        //lista todos os personagens
        for (Iterator<Personagem> iter = WFC.getPersonagens(); iter.hasNext();) {
            Personagem personagem = iter.next();
            if (!jogadorAtivo.isNacao(personagem.getNacao())) {
                continue;
            }
            ret += personagemFacade.getResultadoLocal(personagem);
            ret += "\n\t" + String.format(labels.getString("PERSONAGEM.HAS.SKILLS"), personagem.getNome()) + "\n";
            List<String[]> pericias = personagemFacade.getPericias(personagem, WFC.getCenario());
            int aTipo = 0, aTitulo = 1, aNatural = 2, aFinal = 3;
            for (String[] sPericias : pericias) {
                if (sPericias[aTitulo].equals("") && sPericias[aNatural].equals(sPericias[aFinal])) {
                    ret += String.format("\t. %s: %s \n", sPericias[aTipo], sPericias[aNatural]);
                } else if (sPericias[aTitulo].equals("") && !sPericias[aNatural].equals(sPericias[aFinal])) {
                    ret += String.format("\t. %s: %s (%s)\n", sPericias[aTipo], sPericias[aNatural], sPericias[aFinal]);
                } else if (sPericias[aNatural].equals(sPericias[aFinal])) {
                    ret += String.format("\t. %s: %s - %s\n", sPericias[aTipo], sPericias[aNatural], sPericias[aTitulo]);
                } else {
                    ret += String.format("\t. %s: %s (%s) - %s\n", sPericias[aTipo], sPericias[aNatural], sPericias[aFinal], sPericias[aTitulo]);
                }
            }
            if (personagemFacade.hasExtraOrdem(personagem)) {
                ret += String.format("\t. %s: %s \n", labels.getString("EPIC.HERO"), labels.getString("EPIC.HERO.DESCRIPTION"));
            }
            ret += getActorOrdersString(personagem);
            ret += "\n";
        }
        return ret;
    }

    private String listaOrdensByCity() {
        String ret = labels.getString("TITLE.LIST.BYCITY") + ":\n";
        final Jogador jogadorAtivo = WFC.getJogadorAtivo();
        //lista todos as cidades
        for (Cidade cidade : WFC.getCidades()) {
            if (jogadorAtivo.isNacao(cidade.getNacao())) {
                for (String msg : cidadeFacade.getInfoTitle(cidade)) {
                    ret += msg;
                }
                ret += "\n";
                ret += getActorOrdersString(cidade);
                ret += "\n";
            }
        }
        return ret;
    }

    private String listaOrdensByNation() {
        String ret = labels.getString("TITLE.LIST.BYNATION") + ":\n";
        //lista todos as cidades
        for (Nacao nacao : WFC.getNacoesJogadorAtivo()) {
            for (String msg : nacaoFacade.getInfoTitle(nacao)) {
                ret += msg;
            }
            ret += "\n";
            ret += getActorOrdersString(nacao);
            ret += "\n";
        }
        return ret;
    }

    private String getActorOrdersString(BaseModel actor) {
        String ret = "";
        List<String> par;
        for (PersonagemOrdem po : actor.getAcoes().values()) {
            if (po != null) {
                ret += "\t- ";
                ret += po.getOrdem().getDescricao();
                par = po.getParametrosDisplay();
                boolean first = true;
                for (String elem : par) {
                    if (first) {
                        ret += ": ";
                        first = false;
                    } else {
                        ret += ", ";
                    }
                    ret += elem;
                }
                ret += "\n";
            }
        }
        return ret;
    }

    private String listaPackages() {
        String ret = "";
        for (Nacao nacao : WorldManager.getInstance().getNacoesJogadorAtivo()) {
            List<Habilidade> packages = getPackages(nacao);
            if (!packages.isEmpty()) {
                ret += String.format(labels.getString("STARTUP.NATION.TITLE") + "\n", nacao.getNome());
                for (Habilidade elem : packages) {
                    ret += String.format("%s (%s %s)\n", elem.getNome(), elem.getCost(), labels.getString("STARTUP.POINTS"));
                }
            }
        }
        if (ret.equals("")) {
            return labels.getString("MISSING.PACKAGE");
        } else {
            return ret;
        }
    }

    private String listaOrdensBySequence() {
        String ret = labels.getString("TITLE.LIST.BYSEQ") + ":\n";
        Jogador jogadorAtivo = WFC.getJogadorAtivo();
        SortedMap<Integer, List<PersonagemOrdem>> ordens = new TreeMap<>();
        //list all actions from all actors
        for (BaseModel actor : WFC.getActors()) {
            if (jogadorAtivo.isNacao(actor.getNacao())) {
                for (PersonagemOrdem po : actor.getAcoes().values()) {
                    if (po != null) {
                        List<PersonagemOrdem> lista = ordens.get(po.getOrdem().getNumero());
                        if (lista == null) {
                            lista = new ArrayList<>();
                        }
                        lista.add(po);
                        ordens.put(po.getOrdem().getNumero(), lista);
                    }
                }
            }
        }
        //now format output
        List<String> par;
        for (List<PersonagemOrdem> lista : ordens.values()) {
            for (PersonagemOrdem po : lista) {
                String temp = "";
                par = po.getParametrosDisplay();
                boolean first = true;
                for (String elem : par) {
                    if (first) {
                        temp += ": ";
                        first = false;
                    } else {
                        temp += ", ";
                    }
                    temp += elem;
                }
                ret += String.format("%s - %s %s\n",
                        po.getNome(),
                        po.getOrdem().getDescricao(),
                        temp);
            }
        }
        return ret;
    }

    public String getCenarioNome() {
        return WFC.getCenarioNome();
    }

    public String getJogadorAtivoNome() {
        return WFC.getJogadorAtivoNome();
    }

    public String getNacoesJogadorAtivoNome() {
        return WFC.getNacoesJogadorAtivoNome();
    }

    public int getNacoesJogadorAtivoQtd() {
        return WFC.getJogadorAtivo().getNacoes().size();
    }

    public String getPartidaNome() {
        return WFC.getPartidaNome();
    }

    public int getTurno() {
        return WFC.getTurno();
    }

    public int getTurnoMax() {
        return WFC.getTurnoMax();
    }

    public String getDeadline() {
        try {
            return WFC.getPartida().getDeadline().toDateTimeString();
        } catch (NullPointerException e) {
            return "?";
        }
    }

    public int getDeadlineDaysRemaining() {
        try {
            return WFC.getPartida().getDeadline().getDaysDiffToNow();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Carrega o arquivo verifica integridade do arquivo verifica se o turno/nacao/jogador eh correto limpa as ordens atuais existentes carrega as ordens
     * personagem por personagem atualiza GUI indica quantas ordens foram carregadas/descartadas
     */
    private void setComando(File file) {
        try {
            Comando comando = (Comando) XmlManager.getInstance().get(file);
            //verificar serial violado
            if (!comando.isSerial()) {
                throw new IllegalStateException(labels.getString("SERIAL.VIOLATION") + file.getName());
            }
            //verificar se o turno 'e correto
            Partida partida = WFC.getPartida();
            if (comando.getTurno() != partida.getTurno()) {
                throw new IllegalStateException(labels.getString("TURNO.ERRADO") + file.getName());
            }
            final int qtPackageCarregadas = this.setPackage(comando.getPackages());
            List<String> errorMsgs = new ArrayList<>();
            int qtOrdensCarregadas = this.setOrdens(comando, errorMsgs);
            this.getGui().setStatusMsg(String.format("%d %s %s", qtOrdensCarregadas, labels.getString("ORDENS.CARREGADAS"), file.getName()));
            doCountActions();
            if (!errorMsgs.isEmpty()) {
                String msg = "";
                for (String line : errorMsgs) {
                    msg += line + "\n";
                }
                if (SettingsManager.getInstance().isConfig("LoadActionsFailToLoadBehavior", "LogOnly", "Display")) {
                    log.error(String.format("LoadActionsFailToLoadBehavior=LogOnly. Set 'Display' for popup. %d %s %s", errorMsgs.size(), labels.getString("ORDENS.CARREGADAS.FAIL"), file.getName()));
                } else {
                    SysApoio.showDialogError(String.format("%d %s %s\n%s",
                            errorMsgs.size(), labels.getString("ORDENS.CARREGADAS.FAIL"), file.getName(), msg), this.getGui());
                }
            }
            getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
            getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_COUNT);
        } catch (IllegalStateException | PersistenceException ex) {
            SysApoio.showDialogError(ex.getMessage(), this.getGui());
            this.getGui().setStatusMsg(ex.getMessage());
            log.info(ex.getMessage());
        } catch (ClassCastException ex) {
            SysApoio.showDialogError(labels.getString("ARQUIVO.CORROMPIDO.ACOES") + file.getName(), this.getGui());
            this.getGui().setStatusMsg(labels.getString("ARQUIVO.CORROMPIDO.ACOES") + file.getName());
            log.error(ex.getMessage());
        }
    }

    /**
     * Coloca as ordens nos personagens
     *
     * @param comando
     * @return
     */
    private int setOrdens(Comando comando, List<String> errorMsgs) {
        int ret = 0;
        final SortedMap<String, BaseModel> actors = WFC.getActorsAll();
        if (SettingsManager.getInstance().isConfig("LoadActionsBehavior", "Clean", "Clean")) {
            //limpa todas as ordens
            for (BaseModel actor : actors.values()) {
                actor.remAcoes();
            }
            //limpa financas.
            getDispatchManager().sendDispatchForMsg(DispatchManager.CLEAR_FINANCES_FORECAST, "");
        }

        for (ComandoDetail comandoDetail : comando.getOrdens()) {
            BaseModel actor = actors.get(comandoDetail.getActorCodigo());
            try {
                actor.remAcoes();
            } catch (NullPointerException ex) {
                //nao faz nada, ordens nao disponiveis...
                log.fatal("problems loading actor: " + comandoDetail.getActorCodigo());
            }
        }
        try {
            //carrega as ordens personagem por personagem
            for (ComandoDetail comandoDetail : comando.getOrdens()) {
                BaseModel actor = actors.get(comandoDetail.getActorCodigo());
                Ordem ordem = WFC.getOrdem(comandoDetail.getOrdemCodigo());
                try {
                    //just to catch the NullPointerException
                    ordem.getNome();
                    int indexOrdem = actor.getAcaoSize();
                    //recupera os parametros da ordem
                    PersonagemOrdem po = new PersonagemOrdem();
                    po.setNome(actor.getNome());
                    po.setOrdem(ordem);
                    po.setParametrosDisplay(comandoDetail.getParametroDisplay());
                    po.setParametrosId(comandoDetail.getParametroId());
                    po.setUpdateTime(comandoDetail.getUpdateTime());
//                    log.fatal(comandoDetail.getUpdateTime().toString());
                    final Nacao nation = WFC.getNacao(comandoDetail.getNacaoCodigo());
                    //atualiza financas e outras dependencias
                    getDispatchManager().sendDispatchForChar(nation, null, po);
                    ordemFacade.setOrdem(actor, indexOrdem, po);
                    //atualiza GUI
                    ret++;
                } catch (NullPointerException ex) {
                    errorMsgs.add(comandoDetail.getOrdemDisplay());
                    //nao faz nada, ordens nao disponiveis...
                    log.fatal("problems loading actions: " + comandoDetail.getOrdemDisplay());
                }
            }
        } catch (Exception e) {
            log.info(e);
        }
        SettingsManager.getInstance().setTableColumnAdjust(SettingsManager.getInstance().getConfig("TableColumnAdjust", "0").equalsIgnoreCase("0"));
        getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_RELOAD, "");
        SettingsManager.getInstance().setTableColumnAdjust(true);
        return ret;
    }

    public boolean isGameOver() {
        return WFC.isGameOver();
    }

    public boolean isJogadorAtivoEliminado() {
        return WFC.isJogadorAtivoEliminado(WorldManager.getInstance().getPartida().getJogadorAtivo());
    }

    /**
     * @return the gui
     */
    public MainResultWindowGui getGui() {
        return gui;
    }

    /** The EGF currently open (manual-open or autoload), or null if none opened this session. */
    public File getCurrentResultsFile() {
        return currentResultsFile;
    }

    /**
     * @param gui the gui to set
     */
    public final void setGui(MainResultWindowGui gui) {
        this.gui = gui;
    }

    @Override
    public void receiveDispatch(Nacao nation, PersonagemOrdem before, PersonagemOrdem after) {
        if (after != null) {
            //replacing with something
            this.getGui().setStatusMsg(
                    String.format("%s: %s [$%s]",
                            after.getNome(),
                            after.getOrdem().getDescricao(),
                            WFC.getOrderCost(after, nation)));
        } else if (before != null) {
            //Clear before
            this.getGui().setStatusMsg(
                    String.format("%s: %s [$%s]",
                            before.getNome(),
                            before.getOrdem().getDescricao(),
                            WFC.getOrderCost(before, nation)));
        }
    }

    @Override
    public void receiveDispatch(int msgName) {
        switch (msgName) {
            case DispatchManager.ACTIONS_COUNT:
                doCountActions();
                break;
            case DispatchManager.ACTIONS_MAP_REDRAW:
                gui.getPcPath().setSelected(gui.isPcPathSelected());
                gui.getPcPathFuture().setSelected(gui.isPcPathFutureSelected());
                gui.getFogOfWar().setSelected(gui.isFogOfWarSelected());
                gui.getShowCityCap().setSelected(gui.isShowCityCapSelected());
                break;
            default:
                break;
        }
    }

    @Override
    public void receiveDispatch(int msgName, String msg) {
        switch (msgName) {
            case DispatchManager.SET_LABEL_MONEY:
                final Nacao nacao = WFC.getNacao(msg);
                int actionCost = WFC.getNacaoOrderCost(nacao);
                final String labelActionsCost = String.format(labels.getString("MENU.ACTION.COST"), nacaoFacade.getNome(nacao), actionCost);
                getGui().setLabelMoney(labelActionsCost);
                break;
            case DispatchManager.STATUS_BAR_MSG:
                getGui().setStatusMsg(msg);
                break;
            case DispatchManager.SWITCH_PORTRAIT_PANEL:
                gui.getDisplayPortraits().setSelected(msg.equals("1"));
                break;
            case DispatchManager.SPLIT_PANE_CHANGED:
                gui.setSplitPaneValue(Integer.parseInt(msg));
                break;
            default:
                break;
        }
    }

    @Override
    public void receiveDispatch(int msgName, Local locao) {
        if (msgName == DispatchManager.SAVE_WORLDBUILDER_FILE) {
            doSaveWorld(null);
        }
    }

    @Override
    public void receiveDispatch(int msgName, Component cmpnt) {
        if (msgName == DispatchManager.ACTIONS_AUTOSAVE && SettingsManager.getInstance().isAutoSaveActions()) {
            doSave(cmpnt, false);
        }
    }

    public void saveWorldFile(World world) {
        //salva o arquivo
        try {
            String filename = PersistFactory.getWorldDao().save(world, fcWorld.getSelectedFile());
            this.getGui().setStatusMsg(String.format(labels.getString("WORLD.SALVAS"), world.getLocais().size(), filename));
            this.savedWorld = true;
            log.info("Saved World file:" + fcWorld.getSelectedFile().getAbsolutePath());
        } catch (PersistenceException ex) {
            log.fatal("Can't save???", ex);
        }
    }

    public void saveMapFile() {
        try {
            // Save image
            BufferedImage buffered = WFC.getMapaControler().getMap();
            ImageIO.write(buffered, "png", fcMapImage.getSelectedFile());
            this.getGui().setStatusMsg(String.format(labels.getString("MAPA.SALVAS"), fcMapImage.getSelectedFile().getName()));
        } catch (IOException ex) {
            log.fatal("IOException Problem", ex);
            this.getGui().setStatusMsg(labels.getString("IO.ERROR"));
        }
    }

    private String doSaveActorActions(Jogador jogadorAtivo, Comando comando) {
        //lista todos os actors, carregando para o xml
        final int nationPackagesLimit = WFC.getNationPackagesLimit();
        String ret = "";
        for (BaseModel actor : WFC.getActors()) {
            if (!ordemFacade.isAtivo(jogadorAtivo, actor)) {
                continue;
            }
            if (actor.getAcaoSize() > 0) {
                for (int index = 0; index < ordemFacade.getOrdemMax(actor); index++) {
                    if (ordemFacade.getOrdem(actor, index) != null) {
                        comando.addComando(actor, ordemFacade.getOrdem(actor, index),
                                ordemFacade.getParametrosId(actor, index),
                                ordemFacade.getParametrosDisplay(actor, index),
                                ordemFacade.getTimeLastChange(actor, index));
                    } else if (actor.isNacaoClass()) {
                        //count points, not open slots
                        if (acaoFacade.isPointsSetupUnderLimit(actor, nationPackagesLimit)) {
                            ret = String.format(labels.getString("MISSING.PACKAGE.NATION"), actor.getNome());
                        }
                    } else {
                        ret = String.format(labels.getString("MISSING.ORDERS"), actor.getNome());
                    }
                }
            } else if (cenarioFacade.hasOrdens(WFC.getPartida(), actor)) {
                ret = String.format(labels.getString("MISSING.ORDERS"), actor.getNome());
            }
        }
        return ret;
    }

    private int doCountActorActions(Jogador jogadorAtivo) {
        //lista todos os actors, count actions
        Partida partida = WFC.getPartida();
        int ret = 0;
        for (BaseModel actor : WFC.getActors()) {
            if (!ordemFacade.isAtivo(jogadorAtivo, actor)) {
                continue;
            }
            //count actions
            ret += ordemFacade.getOrdemMax(actor, partida);
        }
        return ret;
    }

    private List<Habilidade> getPackages(Nacao nacao) {
        List<Habilidade> ret = new ArrayList<>();
        for (Habilidade habilidade : nacao.getHabilidades().values()) {
            try {
                if (habilidade.isPackage()) {
                    ret.add(habilidade);
                }
            } catch (NullPointerException e) {
            }
        }
        return ret;
    }

    private int setPackage(SortedMap<Integer, String> packages) {
        Nacao nacao;
        for (Integer idNation : packages.keySet()) {
            try {
                nacao = WorldManager.getInstance().getNacao(idNation);
                clearPackages(nacao);
                nacao.addHabilidades(WFC.getHabilidades(packages.get(idNation)));
            } catch (NullPointerException ex) {
                log.fatal("Something wrong loading packages.");
            }
        }
        getDispatchManager().sendDispatchForMsg(DispatchManager.PACKAGE_RELOAD, "reload");
        return packages.size();
    }

    private void clearPackages(Nacao nacao) {
        final List<Habilidade> list = new ArrayList<>();
        list.addAll(nacao.getHabilidades().values());
        for (Habilidade habilidade : list) {
            if (habilidade.isPackage()) {
                nacao.remHabilidade(habilidade);
            }
        }
    }

    /**
     * Handles the upload result on the EDT (status messages + dialogs). The network call itself runs
     * on a SwingWorker in doSend(); this is invoked from its done(). Returns true when no further
     * error handling is needed, false to trigger the generic "ENVIAR.ERRO.INSTRUCTIONS" dialog.
     */
    private boolean handleSendResult(int ret, File attachment, PartidaJogadorWebInfo info) {
        switch (ret) {
            case WebCounselorManager.OK:
                // Tailor the confirmation to how the orders were sent: a stand-by set, an on-behalf submit
                // (name whose orders they are), or a plain self-submit.
                final String msg;
                if (info.isShadow()) {
                    msg = String.format(labels.getString("POST.DONE.STANDBY"), attachment.getName());
                } else if (info.isOnBehalf()) {
                    msg = String.format(labels.getString("POST.DONE.BEHALF"), info.getPlayerLogin(), attachment.getName());
                } else {
                    msg = String.format(labels.getString("POST.DONE"), attachment.getName());
                }
                log.info(msg);
                if (SettingsManager.getInstance().isConfig("DebugWebpostTime", "1", "0")) {
                    log.info(WebCounselorManager.getInstance().getLastResponseString());
                }
                this.getGui().setStatusMsg(msg);
                if (SettingsManager.getInstance().getConfig("SendOrderConfirmationPopUp", "1").equals("1")) {
                    // Non-modal toast instead of a blocking dialog: confirms the post without interrupting work.
                    Toast.show(javax.swing.SwingUtilities.getWindowAncestor(this.getGui()), msg);
                }
                // The orders just uploaded are now what the server holds -> flip the indicator to SENT
                // without a round-trip (the hash sent == the current local hash).
                OrdersHashService.getInstance().markSent(WFC.getTurno());
                updateOrderSyncIndicator();
                return true;
            case WebCounselorManager.ERROR_BADPLAYERTOKEN:
                // Stored player token is invalid/stale (e.g. regenerated on the site). Clear it and
                // reopen the setup dialog so the player can set a fresh one, then resubmit.
                SettingsManager.getInstance().setConfigAndSaveToFile("playerToken", "");
                this.getGui().setStatusMsg(labels.getString("TOKEN.INVALID.STATUS"));
                SysApoio.showDialogError(labels.getString("TOKEN.INVALID.MSG"), labels.getString("ENVIAR.ERRO"), this.getGui());
                ensurePlayerToken(); // prompt for a fresh token; the player resubmits after
                return true; // handled - suppress the generic error dialog and don't email
            case WebCounselorManager.ERROR_GAMECLOSED:
                //display alert!
                SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.GAMECLOSED"), labels.getString("ENVIAR.ERRO"), this.getGui());
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return true; //dont try email
            case WebCounselorManager.ERROR_SERVERMSG:
                // On-behalf / stand-by gate rejection: show the site's own message (owner doesn't accept
                // on-behalf orders, not a teammate, doesn't accept stand-by, bad EGF token, ...) — the client
                // can't pre-validate it, so surface the server's exact reason instead of a generic error.
                final String serverMsg = WebCounselorManager.getInstance().getLastResponseString();
                SysApoio.showDialogError(
                        (serverMsg == null || serverMsg.trim().isEmpty()) ? labels.getString("ERROR") : serverMsg,
                        labels.getString("ENVIAR.ERRO"), this.getGui());
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return true; //dont try email
            case WebCounselorManager.ERROR_TURN:
                final String expectedTurn = String.format(labels.getString("ENVIAR.ERRO.WRONGTURN"), WebCounselorManager.getInstance().getLastResponseString());
                //display alert!
                SysApoio.showDialogError(expectedTurn, labels.getString("ENVIAR.ERRO"), this.getGui());
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return true; //dont try email
            default:
                SysApoio.showDialogError(labels.getString("ERROR"), labels.getString("ENVIAR.ERRO"), this.getGui());
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return false;
        }
    }

    private PartidaJogadorWebInfo doPrepPost(File attachment, boolean shadow) {
        Partida partida = WFC.getPartida();
        Jogador jogador = partida.getJogadorAtivo();

        PartidaJogadorWebInfo info = new PartidaJogadorWebInfo();
        info.setShadow(shadow);
        info.setAttachment(attachment);
        info.setOrders(listaOrdens());
        info.setGameId(partida.getId());
        info.setGameTurn(partida.getTurno());
        info.setGameNm(partida.getNome());
        info.setPlayerId(jogador.getId());
        info.setPlayerLogin(jogador.getLogin());
        info.setPlayerEmail(jogador.getEmail());
        List<Nacao> nacoes = WFC.getNacoesJogadorAtivo();
        if (!nacoes.isEmpty()) {
            info.setCdToken(nacoes.get(0).getCdToken());
        }
        // Order-sync indicator: attach the canonical hash of the order set being sent, so the site
        // can store it and later answer "are these the orders you hold?".
        OrdersHashService.getInstance().refreshLocal(WFC);
        info.setOrdersHash(OrdersHashService.getInstance().getLocalHash());
        return info;
    }

    private boolean isLoadTeamOrders() {
        return SettingsManager.getInstance().isConfig("LoadActionsBehavior", "append", "0") && SettingsManager.getInstance().isConfig("LoadActionsOtherNations", "allow", "0");
    }

    /**
     * @return the actionsSlots
     */
    private int getActionsSlots() {
        return actionsSlots;
    }

    /**
     * @param slots the actionsSlots to set
     */
    private void setActionsSlots(int slots) {
        this.actionsSlots = slots;
        doCountActions();//and refresh the UI
    }

    private void doCountActions() {
        int ret = 0;
        for (BaseModel actor : WFC.getActors()) {
            if (!ordemFacade.isAtivo(WFC.getJogadorAtivo(), actor)) {
                continue;
            }
            ret += ordemFacade.getActionCount(actor);
        }
        this.actionsCount = ret;
        doUpdateGuiActionCount();
    }

    private void doUpdateGuiActionCount() {
        getGui().setActionsCount(this.actionsCount, this.actionsSlots);
        updateOrderSyncIndicator();
    }

    /**
     * Recompute the local order hash and refresh the status-bar order-sync indicator, comparing
     * against the server hash already cached from the last fetch / send (no network). Runs on every
     * action-count change, so any order edit flips the indicator to PENDING.
     */
    private void updateOrderSyncIndicator() {
        OrdersHashService svc = OrdersHashService.getInstance();
        svc.refreshLocal(WFC);
        getGui().setOrderSyncState(svc.resolve(WFC.getTurno()));
    }

    /**
     * Fetch (background) the orders hash the server currently holds for the loaded game/turn/nation
     * and refresh the indicator. Called on order-file load. Degrades to UNKNOWN/NO_TOKEN gracefully
     * when offline or no token is set (fetchOrdersHash returns null).
     */
    private void refreshOrderSyncFromServer() {
        final int gameId = WFC.getPartida().getId();
        final int turn = WFC.getTurno();
        final List<Nacao> nacoes = WFC.getNacoesJogadorAtivo();
        final int egfToken = nacoes.isEmpty() ? 0 : nacoes.get(0).getCdToken();
        OrdersHashService.getInstance().refreshLocal(WFC);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return WebCounselorManager.getInstance().fetchOrdersHash(gameId, turn, egfToken);
                } catch (Exception ex) {
                    log.warn("Orders-hash fetch failed: " + ex);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    OrdersHashService.getInstance().setServerResponse(get());
                } catch (Exception ignore) {
                    // keep whatever was cached; indicator resolves to UNKNOWN
                }
                updateOrderSyncIndicator();
            }
        }.execute();
    }

    private void doDrawPjPaths() {
        int pcPathValue = gui.getPcPath().isSelected() ? 1 : 0;
        int pcPathFutureValue = gui.getPcPathFuture().isSelected() ? 1 : 0;
        int settingValue = calculateDrawPcPathValue(pcPathValue, pcPathFutureValue);
        SettingsManager.getInstance().setConfig("drawPcPath", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("drawPcPath");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }

    private void doDrawResourcePaths(JToggleButton jbTemp) {
        int settingValue = jbTemp.isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("drawResourcePath", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("drawResourcePath");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }

    private void doDrawArmyPaths(JToggleButton jbTemp) {
        int settingValue = jbTemp.isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("drawArmyMovPath", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("drawArmyMovPath");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }

    private void doScoutTargets(JToggleButton jbTemp) {
        int settingValue = jbTemp.isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("drawScoutOnMap", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("drawScoutOnMap");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }

    private void doDrawFogOfWar(JToggleButton button) {
        int settingValue = button.isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("fogOfWarType", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("fogOfWarType");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }

    private void doDrawShowCityCap(JToggleButton button) {
        int settingValue = button.isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("showCityCap", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("showCityCap");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
    }

    private void doPortraits(JToggleButton button) {
        DisplayPortraitsManager displayPortraitsManager = DisplayPortraitsManager.getInstance();
        if (!displayPortraitsManager.isShowPortraitEnableable()) {
            progressMonitor = new ProgressMonitor(gui, "Downloading file...", "", 0, 100);
            progressMonitor.setProgress(0);
            displayPortraitsManager.downloadPortraits(gui, this);

        } else {
            doDisplayPortraits(button);
        }
    }

    private int calculateDrawPcPathValue(int pcPath, int pcPathFuture) {
        int settingValue = 0;
        String valueBin = String.valueOf(pcPath).concat(String.valueOf(pcPathFuture));
        int tempValue = Integer.parseInt(valueBin, 2);
        switch (tempValue) {
            case 0:
                settingValue = tempValue;
                break;
            case 1:
                settingValue = 3;
                break;
            case 2:
                settingValue = tempValue;
                break;
            case 3:
                settingValue = 1;
                break;
        }
        return settingValue;
    }

    private void doDisplayPortraits(JToggleButton jbTemp) {
        int selected = (jbTemp.isSelected()) ? 1 : 0;
        SettingsManager.getInstance().setConfig("ShowCharacterPortraits", String.valueOf(selected));
        SettingsManager.getInstance().doConfigSave("ShowCharacterPortraits");
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL, String.valueOf(selected));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);

            String message = String.format("Completed %d%%.\n", progress);
            progressMonitor.setNote(message);

            if (progress == 100) {
                //      settingsGui.checkDisplayPortraitCheckBox();
            }
        }
    }

    public boolean isVictoryPointsExists() {
        return !(WFC.getVictoryPoints() == null || WFC.getVictoryPoints().isEmpty());
    }

    public boolean isBattleRoyal() {
        return WFC.isBattleRoyal();
    }

    private void doDataKeyCityPerTeam() {
        List<DataSetForChart> dataSet = new ArrayList<>();
        final PointsFacade pf = new PointsFacade();
        final CounterStringInt cityCount = pf.doVictoryDominationTeam(
                WorldFacadeCounselor.getInstance().getLocais().values(),
                WorldFacadeCounselor.getInstance().getNacaoNeutra());
        final double total = cityCount.getTotal();
        //data header
        String dataBody = String.format("%s\t%s\t%s\n", labels.getString("TEAM"), labels.getString("PONTOS.KEYCITY.TEAM"), labels.getString("PERCENTAGE"));

        //data body
        for (String nmTeam : cityCount.getKeys()) {
            final double value = cityCount.getValue(nmTeam);
            dataBody += String.format("%.0f\t%.1f%%\t%s\n", value, (value / total * 100d), nmTeam);
            dataSet.add(new DataSetForChart(nmTeam, value, "", SysApoio.getColorFromName(nmTeam)));
        }
        //copy para o clipboard
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));

        //create and display chart
        ComponentFactory.showChartPie(labels.getString("PONTOS.KEYCITY.TEAM"), dataSet, getPartidaTagName(), this.gui);
        ComponentFactory.showChartBar(labels.getString("PONTOS.KEYCITY.TEAM"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.KEYCITY"), this.gui);
    }

    private void doDataKeyCityPerNation() {
        List<DataSetForChart> dataSet = new ArrayList<>();
        final PointsFacade pf = new PointsFacade();
        final Set<String> teams = new TreeSet<>();
        final SortedMap<String, Nacao> mapNations = new TreeMap<>();
        List<Nacao> nations = doPrepNations(mapNations, teams);
        final CounterStringInt pointsCount = pf.doVictoryDomination(
                WorldFacadeCounselor.getInstance().getLocais().values(),
                WorldFacadeCounselor.getInstance().getNacaoNeutra());

        //data header
        String dataBody = String.format("%s\t%s\n", labels.getString("PONTOS.KEYCITY.NATION"), labels.getString("TEAM"));

        //data body
        for (String teamName : teams) {
            for (Nacao nation : nations) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                final String nmNation = nation.getNome();
                dataBody += String.format("%s\t%s\t%s\n", pointsCount.getValue(nmNation), nmNation, mapNations.get(nmNation).getTeamFlag());
                dataSet.add(new DataSetForChart(nation.getNome(), pointsCount.getValue(nmNation), nation.getTeamFlag(), nation.getFillColor()));
            }
        }

        //copy para o clipboard
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));

        //create and display chart
        ComponentFactory.showChartPie(labels.getString("PONTOS.KEYCITY.NATION"), dataSet, getPartidaTagName(), this.gui);
        ComponentFactory.showChartBar(labels.getString("PONTOS.KEYCITY.NATION"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.KEYCITY"), this.gui);
        ComponentFactory.showChartStackedBar(labels.getString("PONTOS.KEYCITY.TEAM"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.KEYCITY"), this.gui);
        doDataKeyCityPerTeam();
    }

    private void doGraphBattleRoyale() {
        List<DataSetForChart> dataSet = new ArrayList<>();
        final PointsFacade pf = new PointsFacade();
        final Set<String> teams = new TreeSet<>();
        final SortedMap<String, Nacao> mapNations = new TreeMap<>();
        List<Nacao> nations = doPrepNations(mapNations, teams);
        final CounterStringInt pointsCount = pf.doDominationBattleRoyale(
                WorldFacadeCounselor.getInstance().getLocais().values(),
                WorldFacadeCounselor.getInstance().getNacaoNeutra());

        //data header
        String dataBody = String.format("%s\t%s\t%s\t%s\n", labels.getString("PONTOS.BATTLEROYAL"), labels.getString("PERCENTAGE"), labels.getString("NACAO"), labels.getString("TEAM"));

        //data body
        for (String teamName : teams) {
            for (Nacao nation : nations) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                final String nmNation = nation.getNome();
                dataBody += String.format("%s\t%s\t%s\n", pointsCount.getValue(nmNation), nmNation, mapNations.get(nmNation).getTeamFlag());
                dataSet.add(new DataSetForChart(nation.getNome(), pointsCount.getValue(nmNation), nation.getTeamFlag(), nation.getFillColor()));
            }
        }

        //copy para o clipboard
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));

        //create and display chart
        ComponentFactory.showChartPie(labels.getString("PONTOS.BATTLEROYAL.TEAM"), dataSet, getPartidaTagName(), this.gui);
        ComponentFactory.showChartBar(labels.getString("PONTOS.BATTLEROYAL.NATION"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.BATTLEROYAL"), this.gui);
        ComponentFactory.showChartStackedBar(labels.getString("PONTOS.BATTLEROYAL.TEAM"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.BATTLEROYAL"), this.gui);
        doGraphBattleRoyalePerTeam();
    }

    private void doGraphBattleRoyalePerTeam() {
        List<DataSetForChart> dataSet = new ArrayList<>();
        final PointsFacade pf = new PointsFacade();
        CounterStringInt pointsCount = pf.doDominationBattleRoyaleUsThem(
                WorldFacadeCounselor.getInstance().getLocais().values(),
                WorldFacadeCounselor.getInstance().getJogadorAtivo());
        final double total = pointsCount.getTotal();
        //data header
        String dataBody = String.format("%s\t%s\t%s\n", labels.getString("TEAM"), labels.getString("PONTOS.KEYCITY.TEAM"), labels.getString("PERCENTAGE"));

        //data body ("Us"/"Them" are internal codes - show plain You / Opponents to the player)
        for (String nmTeam : pointsCount.getKeys()) {
            final double value = pointsCount.getValue(nmTeam);
            final String display = usThemLabel(nmTeam);
            dataBody += String.format("%.0f\t%.1f%%\t%s\n", value, (value / total * 100d), display);
            dataSet.add(new DataSetForChart(display, value, "", getColorForTeam(nmTeam)));
        }
        //copy para o clipboard
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));

        //create and display chart
        ComponentFactory.showChartPie(labels.getString("PONTOS.BATTLEROYAL.TEAM"), dataSet, getPartidaTagName(), this.gui);
    }

    private void doDataVictoryPointsPerNation() {
        List<DataSetForChart> dataSet = new ArrayList<>();
        final Set<String> teams = new TreeSet<>();
        final SortedMap<String, Nacao> mapNations = new TreeMap<>();
        List<Nacao> nations = doPrepNations(mapNations, teams);

        //data header
        String dataBody = String.format("%s\t%s\t%s\n", labels.getString("PONTOS.VITORIA"), labels.getString("NACAO"), labels.getString("TEAM"));

        //data body
        for (String teamName : teams) {
            for (Nacao nation : nations) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                dataBody += String.format("%s\t%s\t%s\n", nation.getPontosVitoria(), nation.getNome(), nation.getTeamFlag());
                dataSet.add(new DataSetForChart(nation.getNome(), nation.getPontosVitoria(), nation.getTeamFlag(), nation.getFillColor()));
            }
        }

        //copy para o clipboard
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));
        ComponentFactory.showChartPie(labels.getString("PONTOS.VITORIA.NATION"), dataSet, getPartidaTagName(), this.gui);

        ComponentFactory.showChartBar(labels.getString("PONTOS.VITORIA.NATION"), dataSet, getPartidaTagName(), labels.getString("NACAO") + " / " + labels.getString("TEAM"), labels.getString("PONTOS.VITORIA"), this.gui);
        //ComponentFactory.showChartBar(labels.getString("PONTOS.VITORIA.NATION"), dataSet, getPartidaTagName(), labels.getString("NACAO") + " / " + labels.getString("TEAM"), labels.getString("PONTOS.VITORIA"), false, this.gui);
        //ComponentFactory.showChartStackedBar(labels.getString("PONTOS.VITORIA.TEAM"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.VITORIA"), true, this.gui);
        ComponentFactory.showChartStackedBar(labels.getString("PONTOS.VITORIA.TEAM"), dataSet, getPartidaTagName(), labels.getString("TEAM"), labels.getString("PONTOS.VITORIA"), this.gui);
    }

    // The Victory Overview chart storm (bar + stacked + US gauge) was retired in the Pass-2 graphs rework:
    // the Victory Dashboard now carries that assessment as text rows plus small side-by-side gauges (see
    // gui.services.VictoryDashboardDialog + control.VictoryStatus). The other detailed charts moved into the
    // dashboard's own toolbar via the public launchers below.

    /** Launches the "VP per nation" chart set (moved from the main toolbar into the dashboard). */
    public void showVpPerNationChart() {
        doDataVictoryPointsPerNation();
    }

    /** Launches the "key cities per nation" chart (moved from the main toolbar into the dashboard). */
    public void showKeyCityChart() {
        doDataKeyCityPerNation();
    }

    /** Launches the "VP history (all turns)" chart (moved from the main toolbar into the dashboard). */
    public void showVpHistoryChart() {
        doDataVictoryPointHistoryAllTurns();
    }

    /** Launches the Battle Royale (domination) chart; only meaningful when {@code isBattleRoyal()}. */
    public void showBattleRoyaleChart() {
        doGraphBattleRoyale();
    }

    private void doDataVictoryPointHistoryAllTurns() {
        List<DataSetForChart> dataSet = new ArrayList<>();
        final Set<String> teams = new TreeSet<>();
        final SortedMap<String, Nacao> mapNations = new TreeMap<>();
        List<Nacao> nations = doPrepNations(mapNations, teams);
        final VictoryPointsGame victoryPoints = WFC.getVictoryPoints();
        // For line styling: my own nation is emphasised, and nations not on my side are drawn dashed.
        // isTeam mirrors PartidaControl.isTeam() (GLA/GSL); in solo/FFA "my side" is just my nation.
        final Nacao myNation = WFC.getJogadorAtivo().getNacoes().get(WFC.getJogadorAtivo().getNacoes().firstKey());
        final boolean isTeam = WFC.getPartida().isTeamLocked() || WFC.getPartida().isTeamWithLord();

        //data header with cariable columns as a functions of how many turns so far
        String dataBody = String.format("%s / %s of ", labels.getString("NACAO"), labels.getString("PONTOS.VITORIA"), labels.getString("TURN"));
        for (Integer turn : victoryPoints.getTurnList()) {
            dataBody += String.format("\t%s", turn);
        }
        dataBody += "\n";

        //data body
//        for (Nacao nation : nationsList) {
//            dataBody += nation.getNome();
//            SortedMap<Integer, Integer> nationPoints = victoryPoints.getNationPoints(nation);
//            for (Integer turn : victoryPoints.getTurnList()) {
//                dataBody += String.format("\t%s", nationPoints.get(turn));
//            }
//            dataBody += "\n";
//        }
        for (String teamName : teams) {
            for (Nacao nation : nations) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                dataBody += nation.getNome();
                SortedMap<Integer, Integer> nationPoints = victoryPoints.getNationPoints(nation);
                final boolean emphasis = nation == myNation;
                final boolean dashed = isTeam
                        ? !myNation.getTeamFlag().equals(nation.getTeamFlag())
                        : nation != myNation;
                for (Integer turn : victoryPoints.getTurnList()) {
                    dataBody += String.format("\t%s", nationPoints.get(turn));
                    dataSet.add(new DataSetForChart(nation.getNome(), nationPoints.get(turn),
                            String.format("T %s", turn), nation.getFillColor())
                            .setEmphasis(emphasis).setDashed(dashed));
                }
                dataBody += "\n";
            }
        }

        //copy para o clipboard
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));

        ComponentFactory.showChartLine(labels.getString("PONTOS.VITORIA.HISTORY"), dataSet, getPartidaTagName(), labels.getString("TURNO"), labels.getString("PONTOS.VITORIA"), true, this.gui);
    }

    /** Launcher for the dashboard hub: Nation Power Comparison (X1) radar. */
    public void showNationPowerChart() {
        doNationPowerComparison();
    }

    /** Launcher for the dashboard hub: Your momentum (X2) - rank per turn. */
    public void showMomentumChart() {
        doDataMomentum();
    }

    /**
     * "Your momentum" (X2): a bump chart of every nation's RANK (1 = leader) per turn, derived from the VP
     * history ({@code VictoryPointsGame}). Your own line is emphasised (thick + markers), your side solid /
     * enemies dashed - so a climb toward the top or a slide is obvious at a glance. Rank each turn uses
     * standard competition ranking (1 + the number of nations strictly ahead that turn).
     */
    private void doDataMomentum() {
        final VictoryPointsGame victoryPoints = WFC.getVictoryPoints();
        final Set<String> teams = new TreeSet<>();
        final SortedMap<String, Nacao> mapNations = new TreeMap<>();
        final List<Nacao> nations = doPrepNations(mapNations, teams);
        final Nacao myNation = WFC.getJogadorAtivo().getNacoes().get(WFC.getJogadorAtivo().getNacoes().firstKey());
        final boolean isTeam = WFC.getPartida().isTeamLocked() || WFC.getPartida().isTeamWithLord();

        // per-nation VP-by-turn map, fetched once.
        final java.util.Map<Nacao, SortedMap<Integer, Integer>> ptsByNation = new java.util.LinkedHashMap<>();
        for (Nacao n : nations) {
            ptsByNation.put(n, victoryPoints.getNationPoints(n));
        }

        final List<DataSetForChart> dataSet = new ArrayList<>();
        String dataBody = labels.getString("NACAO") + " / " + labels.getString("PONTOS.MOMENTUM.RANK");
        for (Integer turn : victoryPoints.getTurnList()) {
            dataBody += String.format("\t%s", turn);
        }
        dataBody += "\n";

        for (String teamName : teams) {
            for (Nacao nation : nations) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                dataBody += nation.getNome();
                final boolean emphasis = nation == myNation;
                final boolean dashed = isTeam
                        ? !myNation.getTeamFlag().equals(nation.getTeamFlag())
                        : nation != myNation;
                for (Integer turn : victoryPoints.getTurnList()) {
                    final Integer myPts = ptsByNation.get(nation).get(turn);
                    if (myPts == null) {
                        dataBody += "\t";
                        continue;   // nation not in play that turn -> break the line
                    }
                    int rank = 1;
                    for (Nacao other : nations) {
                        if (other == nation) {
                            continue;
                        }
                        final Integer op = ptsByNation.get(other).get(turn);
                        if (op != null && op > myPts) {
                            rank++;
                        }
                    }
                    dataSet.add(new DataSetForChart(nation.getNome(), rank, String.format("T %s", turn),
                            nation.getFillColor()).setEmphasis(emphasis).setDashed(dashed));
                    dataBody += String.format("\t%s", rank);
                }
                dataBody += "\n";
            }
        }

        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));
        ComponentFactory.showChartRank(labels.getString("PONTOS.MOMENTUM.TITLE"), dataSet, getPartidaTagName(),
                labels.getString("TURNO"), labels.getString("PONTOS.MOMENTUM.AXIS"), nations.size(), this.gui);
    }

    /**
     * Radar of YOUR nation vs a few key others across normalised power metrics (gold, income, cities, big
     * cities, key cities, troops, characters). Team games show you + your strongest ally + the 2 strongest
     * rivals; solo/FFA shows you + the 3 strongest rivals (all by victory points). Each metric is normalised
     * to a percent of the leader among the shown nations. Opponent figures are fog-limited.
     */
    private void doNationPowerComparison() {
        final NacaoFacade nf = new NacaoFacade();
        final CidadeFacade cf = new CidadeFacade();
        final Jogador player = WFC.getJogadorAtivo();
        final Nacao myNation = player.getNacoes().get(player.getNacoes().firstKey());
        final boolean isTeam = WFC.getPartida().isTeamLocked() || WFC.getPartida().isTeamWithLord();
        final String myTeam = myNation.getTeamFlag();

        final List<Nacao> allies = new ArrayList<>();
        final List<Nacao> rivals = new ArrayList<>();
        for (Nacao n : WFC.getNacoes().values()) {
            if (!nf.isAtivaPC(n) || n == myNation) {
                continue;   // active player nations only (excludes barbarians / NPCs)
            }
            if (isTeam && myTeam.equals(n.getTeamFlag())) {
                allies.add(n);
            } else {
                rivals.add(n);
            }
        }
        final java.util.Comparator<Nacao> byVpDesc = (a, b) -> Integer.compare(nf.getPointVictory(b), nf.getPointVictory(a));
        allies.sort(byVpDesc);
        rivals.sort(byVpDesc);

        final List<Nacao> shown = new ArrayList<>();
        shown.add(myNation);
        if (isTeam) {
            if (!allies.isEmpty()) {
                shown.add(allies.get(0));                       // your strongest ally
            }
            for (int k = 0; k < Math.min(2, rivals.size()); k++) {
                shown.add(rivals.get(k));                       // 2 strongest rivals
            }
        } else {
            for (int k = 0; k < Math.min(3, rivals.size()); k++) {
                shown.add(rivals.get(k));                       // solo/FFA: 3 strongest rivals
            }
        }

        final String[] axes = {
            labels.getString("CHART.POWER.GOLD"), labels.getString("CHART.POWER.INCOME"),
            labels.getString("CHART.POWER.CITIES"), labels.getString("CHART.POWER.BIGCITIES"),
            labels.getString("CHART.POWER.KEYCITIES"), labels.getString("CHART.POWER.TROOPS"),
            labels.getString("CHART.POWER.CHARS")
        };
        final java.util.Map<Nacao, int[]> raw = new java.util.LinkedHashMap<>();
        for (Nacao n : shown) {
            int big = 0;
            for (Cidade c : n.getCidades()) {
                if (cf.isBigCity(c)) {
                    big++;
                }
            }
            final Integer keyCities = nf.getPointsKeyCity(n);
            raw.put(n, new int[]{
                nf.getMoneySaldo(n), nf.getArrecadacao(n), n.getCidades().size(), big,
                keyCities == null ? 0 : keyCities, nf.getTropasQt(n, WFC.getExercitos()), nf.getPersonagens(n)
            });
        }
        final int[] max = new int[axes.length];
        for (int[] v : raw.values()) {
            for (int m = 0; m < axes.length; m++) {
                max[m] = Math.max(max[m], v[m]);
            }
        }

        final List<DataSetForChart> dataSet = new ArrayList<>();
        String dataBody = labels.getString("NACAO") + "\t" + String.join("\t", axes) + "\n";
        for (Nacao n : shown) {
            final int[] v = raw.get(n);
            dataBody += n.getNome();
            for (int m = 0; m < axes.length; m++) {
                final double pct = max[m] > 0 ? 100.0 * v[m] / max[m] : 0.0;
                dataSet.add(new DataSetForChart(n.getNome(), pct, axes[m], n.getFillColor()).setEmphasis(n == myNation));
                dataBody += "\t" + v[m];
            }
            dataBody += "\n";
        }
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));
        ComponentFactory.showChartRadar(labels.getString("CHART.POWER.TITLE"), dataSet, getPartidaTagName(), labels.getString("CHART.POWER.FOGNOTE"), this.gui);
    }

    /** Launcher for the dashboard hub: "What should I grow?" (N2). */
    public void showGrowthChart() {
        doGrowthAdvice();
    }

    /**
     * "What should I grow?" (N2, newbie-oriented). Your nation on each power lever expressed as a percent of
     * the GAME AVERAGE (over all active player nations), so a bar below the 100% baseline is a lever you are
     * behind on. Levers that feed THIS game's active victory conditions are highlighted (strong colour), the
     * rest muted - so a new player learns which stat actually wins the game they are in. Opponent economy is
     * fog-limited, so the average understates gold/income (caveat).
     */
    private void doGrowthAdvice() {
        final NacaoFacade nf = new NacaoFacade();
        final CidadeFacade cf = new CidadeFacade();
        final Jogador player = WFC.getJogadorAtivo();
        final Nacao myNation = player.getNacoes().get(player.getNacoes().firstKey());

        final String[] axes = {
            labels.getString("CHART.POWER.GOLD"), labels.getString("CHART.POWER.INCOME"),
            labels.getString("CHART.POWER.CITIES"), labels.getString("CHART.POWER.BIGCITIES"),
            labels.getString("CHART.POWER.KEYCITIES"), labels.getString("CHART.POWER.TROOPS"),
            labels.getString("CHART.POWER.CHARS")
        };

        final long[] sum = new long[axes.length];
        int count = 0;
        int[] mine = null;
        for (Nacao n : WFC.getNacoes().values()) {
            if (!nf.isAtivaPC(n)) {
                continue;   // active player nations only (excludes barbarians / NPCs)
            }
            int big = 0;
            for (Cidade c : n.getCidades()) {
                if (cf.isBigCity(c)) {
                    big++;
                }
            }
            final Integer keyCities = nf.getPointsKeyCity(n);
            final int[] v = new int[]{
                nf.getMoneySaldo(n), nf.getArrecadacao(n), n.getCidades().size(), big,
                keyCities == null ? 0 : keyCities, nf.getTropasQt(n, WFC.getExercitos()), nf.getPersonagens(n)
            };
            for (int m = 0; m < axes.length; m++) {
                sum[m] += v[m];
            }
            count++;
            if (n == myNation) {
                mine = v;
            }
        }
        if (mine == null || count == 0) {
            return;
        }

        // Which levers feed an active victory condition -> highlight them.
        final boolean[] priority = new boolean[axes.length];
        for (VictoryStatus.Row r : new VictoryStatus().evaluate().rows) {
            for (int idx : growthLeversFor(r.code)) {
                priority[idx] = true;
            }
        }
        boolean anyPriority = false;
        for (boolean b : priority) {
            anyPriority |= b;
        }
        if (!anyPriority) {
            java.util.Arrays.fill(priority, true);   // unknown/legacy goal set: don't grey everything out
        }

        final List<DataSetForChart> dataSet = new ArrayList<>();
        String dataBody = labels.getString("CHART.POWER.TITLE") + "\t" + labels.getString("PONTOS.YOU")
                + "\t" + labels.getString("CHART.GROW.BASELINE") + "\t%\n";
        for (int m = 0; m < axes.length; m++) {
            final double avg = (double) sum[m] / count;
            final double pct = avg > 0 ? 100.0 * mine[m] / avg : 0.0;
            dataSet.add(new DataSetForChart("you", pct, axes[m], myNation.getFillColor()).setEmphasis(priority[m]));
            dataBody += axes[m] + "\t" + mine[m] + "\t" + Math.round(avg) + "\t" + Math.round(pct) + "\n";
        }
        ClipboardHelper.copy(dataBody);
        this.getGui().setStatusMsg(labels.getString("COPIAR.DATASET.STATUS"));
        ComponentFactory.showChartGrowth(labels.getString("CHART.GROW.TITLE"), dataSet, getPartidaTagName(),
                labels.getString("CHART.GROW.CAVEAT"), labels.getString("CHART.GROW.AXIS"),
                labels.getString("CHART.GROW.BASELINE"), this.gui);
    }

    /** Power levers (index into the N2 axes) that feed each victory-condition code. */
    private static int[] growthLeversFor(String code) {
        switch (code) {
            case ";VSP;":                 // Score (victory points): economy + territory + leaders
                return new int[]{1, 2, 3, 6};
            case ";VSC;":                 // Conquest (big cities)
            case ";VCP;":                 // Battle Royale (city domination points)
                return new int[]{2, 3};
            case ";VSK;":                 // Domination (key cities)
            case ";VKC;":                 // your team's capital (key-city) losses
                return new int[]{4};
            case ";VSS;":                 // Supremacy (elimination): military
                return new int[]{5, 6};
            case ";VDL;":                 // Dragonlord: characters (dragons are NPC chars)
                return new int[]{6};
            default:
                return new int[0];
        }
    }

    private List<Nacao> doPrepNations(SortedMap<String, Nacao> mapNations, Set<String> teams) {
        List<Nacao> nations = new ArrayList<>(WorldFacadeCounselor.getInstance().getNacoes().values());
        //sort by points
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        for (Nacao nation : nations) {
            //collect information
            teams.add(nation.getTeamFlag());
            mapNations.put(nation.getNome(), nation);
        }
        return nations;
    }

    private String getPartidaTagName() {
        //create and display chart
        final Partida partida = WorldManager.getInstance().getPartida();
        final String subtitle = String.format(labels.getString("GAME.TURN"), partida.getId(), partida.getTurno());
        return subtitle;
    }

    private Color getColorForTeam(String nmTeam) {
        Color colorTeam;
        colorTeam = SysApoio.getColorFromNameNoDefault(nmTeam);
        if (colorTeam == null && nmTeam.equalsIgnoreCase("Us")) {
            try {
                colorTeam = SysApoio.getColorFromName(WorldFacadeCounselor.getInstance().getNacoesJogadorAtivo().get(0).getTeamFlag());
            } catch (NullPointerException e) {
                //Player has no active nations? Eliminated this turn? Just carry on.
            }
        }
        return colorTeam;
    }

    /** Map the internal "Us"/"Them" counter keys to plain player-facing labels; pass other keys through. */
    private String usThemLabel(String key) {
        if ("Us".equals(key)) {
            return labels.getString("PONTOS.YOU");
        }
        if ("Them".equals(key)) {
            return labels.getString("PONTOS.OPPONENTS");
        }
        return key;
    }
}
