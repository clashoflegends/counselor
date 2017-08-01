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
import business.facades.WorldFacadeCounselor;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.MainResultWindowGui;
import gui.accessories.MainAboutBox;
import gui.accessories.MainSettingsGui;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.mail.internet.AddressException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
import persistenceCommons.SmtpManager;
import persistenceCommons.SysApoio;
import persistenceCommons.WebCounselorManager;
import persistenceCommons.XmlManager;
import persistenceLocal.PathFactory;

/**
 *
 * @author gurgel
 */
public class WorldControler extends ControlBase implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(WorldControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final JFileChooser fc = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
    private boolean saved = false;
    private boolean savedWorld = false;
    private MainResultWindowGui gui = null;
    private final AcaoFacade acaoFacade = new AcaoFacade();
    private final OrdemFacade ordemFacade = new OrdemFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final WorldFacadeCounselor WFC = WorldFacadeCounselor.getInstance();

    public WorldControler(MainResultWindowGui aGui) {
        this.gui = aGui;
        registerDispatchManager();
        registerDispatchManagerForMsg(DispatchManager.SET_LABEL_MONEY);
        registerDispatchManagerForMsg(DispatchManager.SAVE_WORLDBUILDER_FILE);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JButton) {
            JButton jbTemp = (JButton) actionEvent.getSource();
            //monta csv com as ordens
            if ("jbOpen".equals(jbTemp.getActionCommand())) {
                doOpen(jbTemp);
            } else if ("jbSave".equals(jbTemp.getActionCommand())) {
                doSave(jbTemp);
            } else if ("jbSaveWorld".equals(jbTemp.getActionCommand())) {
                doSaveWorld(jbTemp);
            } else if ("jbExportMap".equals(jbTemp.getActionCommand())) {
                doMapSave(jbTemp);
            } else if ("jbCopy".equals(jbTemp.getActionCommand())) {
                doCopy();
            } else if ("jbSend".equals(jbTemp.getActionCommand())) {
                doSend(jbTemp);
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
    private File doSave(JButton jbTemp) throws HeadlessException {
        File ret = null;
        Partida partida = WFC.getPartida();
        Jogador jogadorAtivo = partida.getJogadorAtivo();
        Comando comando = new Comando();
        comando.setInfos(partida);
        BaseModel missingAction = doSaveActorActions(jogadorAtivo, comando);
        boolean missingPackage = doSavePackages(comando);
        if (comando.size() == 0) {
            SysApoio.showDialogAlert(labels.getString("NONE.ORDERS"));
            this.getGui().setStatusMsg(labels.getString("NONE.ORDERS"));
        } else {
            if (missingAction != null) {
                final String msg = String.format(labels.getString("MISSING.ORDERS"), missingAction.getNome());
                SysApoio.showDialogAlert(msg);
                this.getGui().setStatusMsg(msg);
            }
            if (missingPackage) {
                SysApoio.showDialogAlert(labels.getString("MISSING.PACKAGE"));
                this.getGui().setStatusMsg(labels.getString("MISSING.PACKAGE"));
            }

            //define nome default
            String nomeArquivo = String.format(labels.getString("FILENAME.ORDERS"), partida.getId(), partida.getTurno() + 1, partida.getJogadorAtivo().getLogin());

            //salva o arquivo
            if (!this.saved) {
                //monta o dialogo
                //define default
                fc.setSelectedFile(new File(nomeArquivo));
                //seta filters
                fc.resetChoosableFileFilters();
                fc.setFileFilter(PathFactory.getFilterAcoes());
                //exibe dialogo
                if (fc.showSaveDialog(jbTemp) == JFileChooser.APPROVE_OPTION) {
                    ret = doFileSave(comando);
                } else {
                    this.getGui().setStatusMsg(labels.getString("SAVE.CANCELLED"));
                }
            } else {
                //salva com o nome anterior.
                ret = doFileSave(comando);
            }
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

    private File doFileSave(Comando comando) {
        File ret = null;
        try {
            ret = fc.getSelectedFile();
            WFC.doSaveOrdens(comando, ret);
            this.getGui().setStatusMsg(String.format(labels.getString("ORDENS.SALVAS"), comando.size(), fc.getSelectedFile().getName()));
            this.saved = true;
        } catch (BussinessException ex) {
            log.error(ex.getMessage());
            SysApoio.showDialogError(ex.getMessage());
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

    private void doHexview() {
        WFC.getMapaControler().doHexViewToggle();
    }

    private void doConfig() throws HeadlessException {

        MainSettingsGui settingPanel = new MainSettingsGui();

        int option = JOptionPane.showOptionDialog(null, settingPanel, labels.getString("MENU.CONFIG"),
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
     * Transictional method for development porpuse. Must be deleted after new methods in SettingsManager would be implemented.
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
            dAbout.setTitle(labels.getString("MENU.ABOUT"));
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
        File attachment = doSave(jbTemp);
        if (attachment == null) {
            this.getGui().setStatusMsg(labels.getString("ENVIAR.FALTOU.ARQUIVO"));
            SysApoio.showDialogError(labels.getString("ENVIAR.FALTOU.ARQUIVO"));
            return;
        }
        /*
         * Try to post
         * if fail, hen try to email
         * if fail, then alternate message
         */
        this.getGui().setStatusMsg(labels.getString("ENVIAR.POST.JUDGE"));
        if (!SettingsManager.getInstance().getConfig("SendOrderWebPopUp", "1").equals("1")) {
            doSendViaEmail(attachment, labels.getString("ENVIAR.FAILREASON.PROPERTYSET"));
        } else if (!doSendPost(attachment)) {
            //try to send via email
            final String lastResponse = WebCounselorManager.getInstance().getLastResponseString();
            doSendViaEmail(attachment, lastResponse);
        } else {
            //fail msg displayed by doSendViaEmail
            //nao deu post nem email
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
                log.info(labels.getString("INICIALIZANDO.GUI"));
                getGui().iniciaConfig();
                this.getGui().setStatusMsg(labels.getString("OPENING: ") + resultsFile.getName());
                this.saved = false;
                this.savedWorld = false;
                doAutoLoadCommands(resultsFile);
            } catch (BussinessException ex) {
                SysApoio.showDialogError(ex.getMessage());
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
            SysApoio.showDialogError(ex.getMessage());
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

    private String listaOrdens() {
        String ret = "";
        if (WFC.isStartupPackages() && WFC.getTurno() == 0) {
            ret += listaPackages() + "\n\n";
        }
        ret += listaOrdensBySequence() + "\n\n";
        if (cenarioFacade.hasOrdensNacao(WFC.getPartida())) {
            ret += listaOrdensByNation() + "\n\n";
        }
        if (cenarioFacade.hasOrdensCidade(WFC.getCenario())) {
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
            if (jogadorAtivo.isNacao(personagem.getNacao())) {
                //ret += personagemFacade.getNome(personagem);
                //ret += "\t@" + personagemFacade.getCoordenadas(personagem) + "\n";
                ret += personagemFacade.getResultadoLocal(personagem);
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

                //Hero
                if (personagemFacade.hasExtraOrdem(personagem)) {
                    ret += String.format("\t. %s: %s \n", labels.getString("EPIC.HERO"), labels.getString("EPIC.HERO.DESCRIPTION"));
                }
                ret += getActorOrdersString(personagem);
                ret += "\n";
            }
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
        SortedMap<Integer, List<PersonagemOrdem>> ordens = new TreeMap<Integer, List<PersonagemOrdem>>();
        //list all actions from all actors
        for (BaseModel actor : WFC.getActors()) {
            if (jogadorAtivo.isNacao(actor.getNacao())) {
                for (PersonagemOrdem po : actor.getAcoes().values()) {
                    if (po != null) {
                        List<PersonagemOrdem> lista = ordens.get(po.getOrdem().getNumero());
                        if (lista == null) {
                            lista = new ArrayList<PersonagemOrdem>();
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
     * Carrega o arquivo verifica integridade do arquivo verifica se o turno/nacao/jogador eh correto limpa as ordens atuais existentes carrega as
     * ordens personagem por personagem atualiza GUI indica quantas ordens foram carregadas/descartadas
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
            int qtPackageCarregadas = this.setPackage(comando.getPackages());
            List<String> errorMsgs = new ArrayList<String>();
            int qtOrdensCarregadas = this.setOrdens(comando, errorMsgs);
            this.getGui().setStatusMsg(String.format("%d %s %s", qtOrdensCarregadas, labels.getString("ORDENS.CARREGADAS"), file.getName()));
            if (!errorMsgs.isEmpty()) {
                String msg = "";
                for (String line : errorMsgs) {
                    msg += line + "\n";
                }
                SysApoio.showDialogError(String.format("%d %s %s\n%s",
                        errorMsgs.size(), labels.getString("ORDENS.CARREGADAS.FAIL"), file.getName(), msg));
            }
        } catch (IllegalStateException ex) {
            SysApoio.showDialogError(ex.getMessage());
            this.getGui().setStatusMsg(ex.getMessage());
            log.info(ex.getMessage());
        } catch (ClassCastException ex) {
            SysApoio.showDialogError(labels.getString("ARQUIVO.CORROMPIDO.ACOES") + file.getName());
            this.getGui().setStatusMsg(labels.getString("ARQUIVO.CORROMPIDO.ACOES") + file.getName());
            log.error(ex.getMessage());
        } catch (PersistenceException ex) {
            SysApoio.showDialogError(ex.getMessage());
            this.getGui().setStatusMsg(ex.getMessage());
            log.info(ex.getMessage());
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
//                    this.getGui().getTabPersonagem().setValueFor(ordemDisplay, personagem.getNome(), indexOrdem);
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
    public void setGui(MainResultWindowGui gui) {
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
                            acaoFacade.getCusto(after)));
        } else if (before != null) {
            //Clear before
            this.getGui().setStatusMsg(
                    String.format("%s: %s [$%s]",
                            before.getNome(),
                            before.getOrdem().getDescricao(),
                            acaoFacade.getCusto(before)));
        }
    }

    @Override
    public void receiveDispatch(int msgName, String idNacao) {
        if (msgName == DispatchManager.SET_LABEL_MONEY) {
            final Nacao nacao = WFC.getNacao(idNacao);
            int actionCost = WFC.getNacaoOrderCost(nacao);
            final String labelActionsCost = String.format(labels.getString("MENU.ACTION.COST"), nacaoFacade.getNome(nacao), actionCost);
            getGui().setLabelMoney(labelActionsCost);
        }
    }

    @Override
    public void receiveDispatch(int msgName, Local locao) {
        if (msgName == DispatchManager.SAVE_WORLDBUILDER_FILE) {
            doSaveWorld(null);
        }
    }

    public void saveWorldFile(World world) {
        //salva o arquivo
        try {
            String filename = PersistFactory.getWorldDao().save(world, fc.getSelectedFile());
            this.getGui().setStatusMsg(String.format(labels.getString("WORLD.SALVAS"), world.getLocais().size(), filename));
            this.savedWorld = true;
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

    private BaseModel doSaveActorActions(Jogador jogadorAtivo, Comando comando) {
        BaseModel ret = null;
        //lista todos os personagens, carregando para o xml
        final int nationPackagesLimit = WFC.getNationPackagesLimit();
        for (BaseModel actor : WFC.getActors()) {
            if (!ordemFacade.isAtivo(jogadorAtivo, actor)) {
                continue;
            }
            if (actor.getAcaoSize() > 0) {
                for (int index = 0; index < actor.getOrdensQt(); index++) {
                    if (ordemFacade.getOrdem(actor, index) != null) {
                        comando.addComando(actor, ordemFacade.getOrdem(actor, index),
                                ordemFacade.getParametrosId(actor, index),
                                ordemFacade.getParametrosDisplay(actor, index));
                    } else if (actor.isNacao()) {
                        //count points, not open slots
                        if (acaoFacade.isPointsSetupUnderLimit(actor, nationPackagesLimit)) {
                            ret = actor;
                        }
                    } else {
                        ret = actor;
                    }
                }
            } else if (cenarioFacade.hasOrdens(WFC.getPartida(), actor)) {
                ret = actor;
            }
        }
        return ret;
    }

    private boolean doSaveActorActionsOLD(Jogador jogadorAtivo, Comando comando) {
        boolean missing = false;
        //lista todos os personagens, carregando para o xml
        for (BaseModel actor : WFC.getActors()) {
            if (!ordemFacade.isAtivo(jogadorAtivo, actor)) {
                continue;
            }
            if (actor.getAcaoSize() > 0) {
                for (int index = 0; index < actor.getOrdensQt(); index++) {
                    if (ordemFacade.getOrdem(actor, index) != null) {
                        comando.addComando(actor, ordemFacade.getOrdem(actor, index),
                                ordemFacade.getParametrosId(actor, index),
                                ordemFacade.getParametrosDisplay(actor, index));
                    } else {
                        missing = true;
                    }
                }
            } else if (cenarioFacade.hasOrdens(WFC.getPartida(), actor)) {
                missing = true;
            }
        }
        return missing;
    }

    private boolean doSavePackages(Comando comando) {
        if (WFC.isStartupPackages() && WFC.getTurno() == 0) {
            return getPackagesAll(comando);
        } else {
            return false;
        }
    }

    private boolean getPackagesAll(Comando comando) {
        boolean missing = true;
        for (Nacao nacao : WorldManager.getInstance().getNacoesJogadorAtivo()) {
            comando.addPackage(nacao, getPackages(nacao));
            missing = false;
            log.info(nacao.getNome() + " + " + getPackages(nacao));
        }
        return missing;
    }

    private List<Habilidade> getPackages(Nacao nacao) {
        List<Habilidade> ret = new ArrayList<Habilidade>();
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
        final List<Habilidade> list = new ArrayList<Habilidade>();
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
                        SysApoio.showDialogInfo(labels.getString("POST.DONE.TITLE"), labels.getString("POST.DONE.TITLE"));
                    }
                    return true;
                case WebCounselorManager.ERROR_GAMECLOSED:
                    //display alert!
                    SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.GAMECLOSED"), labels.getString("ENVIAR.ERRO"));
                    this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                    return true; //dont try email
                case WebCounselorManager.ERROR_TURN:
                    final String expectedTurn = String.format(labels.getString("ENVIAR.ERRO.WRONGTURN"), WebCounselorManager.getInstance().getLastResponseString());
                    //display alert!
                    SysApoio.showDialogError(expectedTurn, labels.getString("ENVIAR.ERRO"));
                    this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                    return true; //dont try email
                default:
                    SysApoio.showDialogError(labels.getString("ERROR"), labels.getString("ENVIAR.ERRO"));
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

    private boolean doSendViaEmail(File attachment, String msg) {
        /* prepara o email, pede informacoes se properties
         * nao estao preenchidas salva novas informacoes no properties !
         * pergunta se quer receber uma copia? ! 
         * envia o email e avisa do recibo.
         */
        this.getGui().setStatusMsg(labels.getString("ENVIAR.JUDGE"));
        final String from = getEmail();
        if (from.equals("none")) {
            return false;
        }
        try {
            SmtpManager email = new SmtpManager();
            email.addToCc(from);
            email.setFrom(from);
            email.setBody(listaOrdensEmailBody(msg));
            String subject;
            try {
                subject = String.format("[Orders] %s - %s (%s) [%s]",
                        WorldManager.getInstance().getPartida().getCodigo(),
                        WorldManager.getInstance().getPartida().getJogadorAtivo().getLogin(),
                        attachment.getName(), SysApoio.nowTimestamp());
            } catch (NullPointerException e) {
                subject = String.format("[Orders] NULL (%s) [%s]",
                        attachment.getName(), SysApoio.nowTimestamp());
            }
            email.setSubject(subject);
            email.addAttachment(attachment);
            if (email.sendCounselor()) {
                this.getGui().setStatusMsg(String.format(labels.getString("ENVIAR.DONE"), attachment.getName()));
                return true;
            } else {
                this.getGui().setStatusMsg(labels.getString("ENVIAR.ERRO"));
                SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.INSTRUCTIONS"), labels.getString("ENVIAR.ERRO"));
                return false;
            }
        } catch (PersistenceException ex) {
            this.getGui().setStatusMsg(labels.getString("ENVIAR.ERRO") + " => " + ex.getMessage());
            SysApoio.showDialogError(ex.getMessage() + "\n\n" + labels.getString("ENVIAR.ERRO.INSTRUCTIONS"), labels.getString("ENVIAR.ERRO"));
            return false;
        } catch (AddressException ex) {
            this.getGui().setStatusMsg(labels.getString("ENVIAR.ERRO.MALFORMED") + " => " + ex.getMessage());
            SysApoio.showDialogError(ex.getMessage() + "\n\n" + labels.getString("ENVIAR.ERRO.INSTRUCTIONS"), labels.getString("ENVIAR.ERRO.MALFORMED"));
            return false;
        }
    }

    private String getEmail() {
        String from = SettingsManager.getInstance().getConfig("MyEmail", "none");
        if (from.equals("none")) {
            from = JOptionPane.showInputDialog(labels.getString("ENVIAR.INPUT.EMAIL"), from);
            if (from == null || from.equals("none")) {
                this.getGui().setStatusMsg(labels.getString("ENVIAR.FALTOU.FROM"));
                SysApoio.showDialogError(labels.getString("ENVIAR.FALTOU.FROM"));
                return "none";
            }
            //salva novas informacoes no properties
            SettingsManager.getInstance().setConfigAndSaveToFile("MyEmail", from);
        }
        return from;
    }

    private boolean isLoadTeamOrders() {
        return SettingsManager.getInstance().isConfig("LoadActionsBehavior", "append", "0") && SettingsManager.getInstance().isConfig("LoadActionsOtherNations", "allow", "0");
    }

}
