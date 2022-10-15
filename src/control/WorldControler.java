/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.BaseModel;
import business.BussinessException;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.CidadeFacade;
import business.facade.NacaoFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import control.facade.WorldFacadeCounselor;
import control.services.NacaoConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import control.support.DisplayPortraitsManager;
import gui.MainResultWindowGui;
import gui.accessories.GraphPopupScoreByNation;
import gui.accessories.GraphPopupVpPerTeam;
import gui.accessories.GraphPopupVpPerTurn;
import gui.accessories.MainAboutBox;
import gui.accessories.MainSettingsGui;
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

/**
 *
 * @author gurgel
 */
public class WorldControler extends ControlBase implements Serializable, ActionListener, PropertyChangeListener {

    private static final Log log = LogFactory.getLog(WorldControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final JFileChooser fc = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
    private boolean saved = false;
    private boolean savedWorld = false;
    private boolean msgSubmitReady = false;
    private int actionsSlots = 0;
    private int actionsCount = 0;
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
            //monta csv com as ordens
            if ("jbOpen".equals(jbTemp.getActionCommand())) {
                doOpen(jbTemp);
            } else if ("jbSave".equals(jbTemp.getActionCommand())) {
                doSave(jbTemp, false);
            } else if ("jbSaveWorld".equals(jbTemp.getActionCommand())) {
                doSaveWorld(jbTemp);
            } else if ("jbExportMap".equals(jbTemp.getActionCommand())) {
                doMapSave(jbTemp);
            } else if ("jbCopy".equals(jbTemp.getActionCommand())) {
                doCopy();
            } else if ("jbEmailList".equals(jbTemp.getActionCommand())) {
                doEmailList();
            } else if ("jbSend".equals(jbTemp.getActionCommand())) {
                doSend(jbTemp);
            } else if ("jbScoreGraph".equals(jbTemp.getActionCommand())) {
                doGraphScore();
            } else if ("jbGraphSingleTurn".equals(jbTemp.getActionCommand())) {
                doGraphSingleTurn();
            } else if ("jbGraphAllTurns".equals(jbTemp.getActionCommand())) {
                doGraphAllTurns();
            } else if ("jbAbout".equals(jbTemp.getActionCommand())) {
                doAbout();
            } else if ("jbHexview".equals(jbTemp.getActionCommand())) {
                doHexview();
            } else if ("jbConfig".equals(jbTemp.getActionCommand())) {
                doConfig();
            } else if ("jbLoad".equals(jbTemp.getActionCommand())) {
                doLoad(jbTemp);
            } else {
                log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
            }
        } else if (actionEvent.getSource() instanceof JToggleButton) {
            JToggleButton jbTemp = (JToggleButton) actionEvent.getSource();
            if (("pcPathDraw").equals(jbTemp.getActionCommand())) {
                doDrawPjPaths();
            } else if ("drawPathArmy".equals(jbTemp.getActionCommand())) {
                doDrawArmyPaths();
            } else if ("drawFogWar".equals(jbTemp.getActionCommand())) {
                doDrawFogOfWar(jbTemp);
            } else if ("drawDisplayPortraits".equals(jbTemp.getActionCommand())) {
                DisplayPortraitsManager displayPortraitsManager = DisplayPortraitsManager.getInstance();
                if (!displayPortraitsManager.isShowPortraitEnableable()) {
                    progressMonitor = new ProgressMonitor(gui, "Downloading file...", "", 0, 100);
                    progressMonitor.setProgress(0);
                    displayPortraitsManager.downloadPortraits(gui, this);
                }
                doDisplayPortraits(jbTemp);
            } else {
                log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
            }
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
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
        fc.setSelectedFile(new File(nomeArquivo));
        //Create a file chooser
        //In response to a button click:
        fc.resetChoosableFileFilters();
        fc.setFileFilter(PathFactory.getFilterAcoes());
        int returnVal = fc.showOpenDialog(jbTemp);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
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
        String missingActionMsg = doSaveActorActions(jogadorAtivo, comando);
        if (comando.size() == 0) {
            SysApoio.showDialogAlert(labels.getString("NONE.ORDERS"), this.getGui());
            this.getGui().setStatusMsg(labels.getString("NONE.ORDERS"));
            //we're done here, nothing to save
            return null;
        }
        if (!missingActionMsg.equalsIgnoreCase("") && SettingsManager.getInstance().isConfig("ActionsMissingPopup", "1", "0")) {
            if (forceMissingPopup || !SettingsManager.getInstance().isAutoSaveActions()) {
                SysApoio.showDialogAlert(missingActionMsg, this.getGui());
            }
        }
        if (missingActionMsg.equalsIgnoreCase("") && !this.msgSubmitReady) {
            SysApoio.showDialogAlert(labels.getString("ORDERS.READY.SUBMIT"), this.getGui());
            this.msgSubmitReady = true;
        }

        //define nome default
        String fileName = String.format(labels.getString("FILENAME.ORDERS"), partida.getId(), partida.getTurno() + 1, partida.getJogadorAtivo().getLogin());

        //salva o arquivo
        File ret = null;
        if (!this.saved) {
            //monta o dialogo
            //define default
            fc.setSelectedFile(new File(fileName));
            //seta filters
            fc.resetChoosableFileFilters();
            fc.setFileFilter(PathFactory.getFilterAcoes());
            //exibe dialogo
            if (fc.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
                ret = doFileSave(comando, missingActionMsg);
                log.info("Saved Server file:" + ret.getAbsolutePath());
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
            fc.setSelectedFile(new File(PathFactory.getWorldFileName(world)));
            //seta filters
            fc.resetChoosableFileFilters();
            fc.setFileFilter(PathFactory.getFilterWorld());
            //exibe dialogo
            if (fc.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
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
            ret = fc.getSelectedFile();
            WFC.doSaveOrdens(comando, ret);
            this.getGui().setStatusMsg(missingActionMsg + " " + String.format(labels.getString("ORDENS.SALVAS"), comando.size(), fc.getSelectedFile().getName()));
            this.saved = true;
        } catch (BussinessException ex) {
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

        fc.setSelectedFile(new File(nomeArquivo));
        //seta filters
        fc.resetChoosableFileFilters();
        fc.setFileFilter(PathFactory.getFilterImages());
        //exibe dialogo

        if (fc.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
            saveMapFile();
        } else {
            this.getGui().setStatusMsg(labels.getString("SAVE.CANCELLED"));
        }
    }

    private void doAbout() throws HeadlessException {
        JDialog dAbout = new JDialog(new JFrame(), true);
        MainAboutBox stAbout = new MainAboutBox();
        //configura jDialog
        dAbout.add(stAbout);
        dAbout.setTitle(labels.getString("MENU.ABOUT"));
        dAbout.setAlwaysOnTop(true);
        //dAbout.setPreferredSize(new Dimension(400, 400));
        dAbout.setLocationRelativeTo(this.getGui());
        dAbout.pack();
        dAbout.setVisible(true);
    }

    private void doGraphScore() throws HeadlessException {
        GraphPopupScoreByNation graph = new GraphPopupScoreByNation();
        graph.start();
    }

    private void doGraphSingleTurn() throws HeadlessException {
        GraphPopupVpPerTeam graph = new GraphPopupVpPerTeam();
        graph.start();
    }

    private void doGraphAllTurns() throws HeadlessException {
        GraphPopupVpPerTurn graph = new GraphPopupVpPerTurn(WorldFacadeCounselor.getInstance().getNacoes().values());
        graph.start(WFC.getVictoryPoints());
    }

    private void doHexview() {
        WFC.getMapaControler().doHexViewToggle();
    }

    private void doConfig() throws HeadlessException {

        MainSettingsGui settingPanel = new MainSettingsGui();

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
         * Try to post
         * if fail, then alternate message
         */
        this.getGui().setStatusMsg(labels.getString("ENVIAR.POST.JUDGE"));
        if (!doSendPost(attachment)) {
            //nao deu post 
            this.getGui().setStatusMsg(labels.getString("ENVIAR.ERRO"));
            SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.INSTRUCTIONS"), labels.getString("ENVIAR.ERRO"), this.getGui());
        }
    }

    /**
     * Abre o turno com os resultados enviados pelo engine
     *
     * @param jbTemp
     * @throws HeadlessException
     */
    private void doOpen(JButton jbTemp) throws HeadlessException {
        //Create a file chooser
        //In response to a button click:
        fc.resetChoosableFileFilters();
        fc.setFileFilter(PathFactory.getFilterResults());
        if (SettingsManager.getInstance().isWorldBuilder()) {
            fc.addChoosableFileFilter(PathFactory.getFilterWorld());
        }
        int returnVal = fc.showOpenDialog(jbTemp);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                final File resultsFile = fc.getSelectedFile();
                log.info(labels.getString("OPENING: ") + resultsFile.getName());
                WFC.doStart(resultsFile);
                this.setActionsSlots(doCountActorActions(WFC.getJogadorAtivo()));
                log.info(labels.getString("INICIALIZANDO.GUI"));
                getGui().iniciaConfig();
                this.getGui().setStatusMsg(labels.getString("OPENING: ") + resultsFile.getName());
                this.msgSubmitReady = false;
                this.saved = false;
                this.savedWorld = false;
                doAutoLoadCommands(resultsFile);
            } catch (BussinessException ex) {
                SysApoio.showDialogError(ex.getMessage(), this.getGui());
                this.getGui().setStatusMsg(ex.getMessage());
                log.error(ex);
            }
        } else {
            log.info(labels.getString("OPEN.CANCELLED"));
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
            log.info(labels.getString("AUTOLOADING.OPENING") + autoLoad);
            this.getGui().setStatusMsg(labels.getString("AUTOLOADING.OPENING") + autoLoad);
            final File resultsFile = new File(autoLoad);
            WFC.doStart(resultsFile);
            this.setActionsSlots(doCountActorActions(WFC.getJogadorAtivo()));
            getGui().iniciaConfig();
            String autoLoadActions = SettingsManager.getInstance().getConfig("autoLoadActions", "none");
            if (!autoLoadActions.equals("none") && !autoLoadActions.isEmpty()) {
                //check if there's a defined orders file to be loaded
                final File loadActions = new File(autoLoadActions);
                setComando(loadActions);
            } else {
                doAutoLoadCommands(resultsFile);
            }
            fc.setSelectedFile(resultsFile);
            this.saved = false;
        } catch (BussinessException ex) {
            SysApoio.showDialogError(ex.getMessage(), this.getGui());
            this.getGui().setStatusMsg(ex.getMessage());
            log.error(ex);
        }
    }

    private String listaOrdensEmailBody(String msg) {
        String ret = String.format("%s\n%s: \n%s\n\n",
                labels.getString("ENVIAR.FAILREASON.HINT"),
                labels.getString("ENVIAR.FAILREASON.TITLE"),
                msg);
        ret += listaOrdens();
        return ret;
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
                SysApoio.showDialogError(String.format("%d %s %s\n%s",
                        errorMsgs.size(), labels.getString("ORDENS.CARREGADAS.FAIL"), file.getName(), msg), this.getGui());
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
            String filename = PersistFactory.getWorldDao().save(world, fc.getSelectedFile());
            this.getGui().setStatusMsg(String.format(labels.getString("WORLD.SALVAS"), world.getLocais().size(), filename));
            this.savedWorld = true;
            log.info("Saved World file:" + fc.getSelectedFile().getAbsolutePath());
        } catch (PersistenceException ex) {
            log.fatal("Can't save???", ex);
        }
    }

    public void saveMapFile() {
        try {
            // Save image
            BufferedImage buffered = WFC.getMapaControler().getMap();
            ImageIO.write(buffered, "png", fc.getSelectedFile());
            this.getGui().setStatusMsg(String.format(labels.getString("MAPA.SALVAS"), fc.getSelectedFile().getName()));
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
                                ordemFacade.getParametrosDisplay(actor, index));
                    } else if (actor.isNacao()) {
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

    private boolean doSendPost(File attachment) {
        try {
            //int ret = WebCounselorManager.getInstance().doSendViaPost(attachment, partida, listaOrdens());
            PartidaJogadorWebInfo info = doPrepPost(attachment);
            int ret = WebCounselorManager.getInstance().doSendViaPost(info);
            switch (ret) {
                case WebCounselorManager.OK:
                    final String msg = String.format(labels.getString("POST.DONE"), attachment.getName());
                    log.info(msg);
                    this.getGui().setStatusMsg(msg);
                    if (SettingsManager.getInstance().getConfig("SendOrderConfirmationPopUp", "1").equals("1")) {
                        SysApoio.showDialogInfo(labels.getString("POST.DONE.TITLE"), labels.getString("POST.DONE.TITLE"), this.getGui());
                    }
                    return true;
                case WebCounselorManager.ERROR_GAMECLOSED:
                    //display alert!
                    SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.GAMECLOSED"), labels.getString("ENVIAR.ERRO"), this.getGui());
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
        } catch (PersistenceException ex) {
            this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
            return false;
        }
    }

    private PartidaJogadorWebInfo doPrepPost(File attachment) {
        Partida partida = WFC.getPartida();
        Jogador jogador = partida.getJogadorAtivo();

        PartidaJogadorWebInfo info = new PartidaJogadorWebInfo();
        info.setAttachment(attachment);
        info.setOrders(listaOrdens());
        info.setGameId(partida.getId());
        info.setGameTurn(partida.getTurno());
        info.setGameNm(partida.getNome());
        info.setPlayerId(jogador.getId());
        info.setPlayerLogin(jogador.getLogin());
        info.setPlayerEmail(jogador.getEmail());
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

    private void doDrawArmyPaths() {
        int settingValue = gui.getArmyPath().isSelected() ? 1 : 0;
        SettingsManager.getInstance().setConfig("drawArmyMovPath", String.valueOf(settingValue));
        SettingsManager.getInstance().doConfigSave("drawArmyMovPath");
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
}
