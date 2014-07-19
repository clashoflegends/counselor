/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.BaseModel;
import baseLib.SysApoio;
import baseLib.SysProperties;
import business.BussinessException;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.CidadeFacade;
import business.facade.NacaoFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import business.facades.WorldFacade;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.MainAboutBox;
import gui.MainResultWindowGui;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import javax.swing.*;
import model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.*;
import persistence.local.PersistFactory;
import persistence.local.WorldManager;

/**
 *
 * @author gurgel
 */
public class WorldControler extends ControlBase implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(WorldControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final JFileChooser fc = new JFileChooser(SysProperties.getProps("loadDir"));
    private boolean saved = false;
    private boolean savedWorld = false;
    private MainResultWindowGui gui = null;
    private final AcaoFacade acaoFacade = new AcaoFacade();
    private final OrdemFacade ordemFacade = new OrdemFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final PersonagemFacade personagemFacade = new PersonagemFacade();

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
            } else if ("jbCopy".equals(jbTemp.getActionCommand())) {
                doCopy();
            } else if ("jbSend".equals(jbTemp.getActionCommand())) {
                doSend(jbTemp);
            } else if ("jbAbout".equals(jbTemp.getActionCommand())) {
                doAbout();
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
        Partida partida = WorldManager.getInstance().getPartida();
        String nomeArquivo = String.format(labels.getString("FILENAME.ORDERS"),
                partida.getId(), partida.getTurno() + 1,
                partida.getJogadorAtivo().getLogin());
        //salva o arquivo
        fc.setSelectedFile(new File(nomeArquivo));
        //Create a file chooser
        //In response to a button click:
        fc.resetChoosableFileFilters();
        fc.setFileFilter(PathFactory.getFilterAcoes());
        int returnVal = fc.showOpenDialog(jbTemp);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            log.info(labels.getString("LOADING: ") + file.getName());
            setComando(file);
            this.saved = false;
            this.savedWorld = false;
        } else {
            this.getGui().setStatusMsg(labels.getString("LOAD.CANCELLED"));
        }
    }

    /**
     * Salva ordens para o arquivo a ser enviado para o Engine
     *
     * @param jbTemp
     * @throws HeadlessException
     */
    private File doSave(JButton jbTemp) throws HeadlessException {
        File ret = null;
        Partida partida = WorldFacade.getInstance().getPartida();
        Jogador jogadorAtivo = partida.getJogadorAtivo();
        Comando comando = new Comando();
        comando.setInfos(partida);
        boolean missingAction = doSaveActorActions(jogadorAtivo, comando);
        boolean missingPackage = doSavePackages(comando);
        if (comando.size() == 0) {
            SysApoio.showDialogAlert(labels.getString("NONE.ORDERS"));
            this.getGui().setStatusMsg(labels.getString("NONE.ORDERS"));
        } else {
            if (missingAction) {
                SysApoio.showDialogAlert(labels.getString("MISSING.ORDERS"));
                this.getGui().setStatusMsg(labels.getString("MISSING.ORDERS"));
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
            WorldFacade.getInstance().doSaveOrdens(comando, ret);
            this.getGui().setStatusMsg(String.format(labels.getString("ORDENS.SALVAS"), comando.size(), fc.getSelectedFile().getName()));
            this.saved = true;
        } catch (BussinessException ex) {
            log.error(ex.getMessage());
            SysApoio.showDialogError(ex.getMessage());
            this.getGui().setStatusMsg(ex.getMessage());
        }
        return ret;
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
        if (SysProperties.getProps("CopyActionsPopUp", "1").equals("1")) {
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
        if (!SysProperties.getProps("SendOrderWebPopUp", "1").equals("1")) {
            doSendViaEmail(attachment, labels.getString("ENVIAR.FAILREASON.PROPERTYSET"));
        } else if (!doSendViaPost(attachment)) {
            //success or wrong turn
            String lastResponse = WebCounselorManager.getInstance().getLastResponseString();
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
                File file = fc.getSelectedFile();
                log.info(labels.getString("OPENING: ") + file.getName());
                WorldFacade.getInstance().doStart(file);
                log.info(labels.getString("INICIALIZANDO.GUI"));
                getGui().iniciaConfig();
                this.getGui().setStatusMsg(labels.getString("OPENING: ") + file.getName());
                this.saved = false;
                this.savedWorld = false;
            } catch (BussinessException ex) {
                SysApoio.showDialogError(ex.getMessage());
                this.getGui().setStatusMsg(ex.getMessage());
                log.error(ex);
            }
        } else {
            log.info(labels.getString("OPEN.CANCELLED"));
        }
    }

    /**
     * Verifica e carrega a partida default definida no properties
     *
     * @param autoLoad
     */
    public void doAutoLoad(String autoLoad) {
        if (autoLoad != null) {
//            autoLoad = SysProperties.getProps("loadDir") + autoLoad;
            try {
                log.info(labels.getString("AUTOLOADING.OPENING") + autoLoad);
                WorldFacade.getInstance().doStart(new File(autoLoad));
                getGui().iniciaConfig();
                String autoLoadActions = SysProperties.getProps("autoLoadActions", "none");
                if (!autoLoadActions.equals("none")) {
                    final File loadActions = new File(autoLoadActions);
                    setComando(loadActions);
                }
                this.getGui().setStatusMsg(labels.getString("AUTOLOADING.OPENING") + autoLoad);
                this.saved = false;
            } catch (BussinessException ex) {
                SysApoio.showDialogError(ex.getMessage());
                this.getGui().setStatusMsg(ex.getMessage());
                log.error(ex);
            }
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
        String ret = listaOrdensBySequence() + "\n\n";
        if (cenarioFacade.hasOrdensNacao(WorldFacade.getInstance().getPartida())) {
            ret += listaOrdensByNation() + "\n\n";
        }
        if (cenarioFacade.hasOrdensCidade(WorldFacade.getInstance().getCenario())) {
            ret += listaOrdensByCity() + "\n\n";
        }
        if (SysProperties.getProps("CopyActionsOrder", "1").equals("1")) {
            ret += listaOrdensByPers() + "\n\n";
        }
        return ret;
    }

    private String listaOrdensByPers() {
        String ret = labels.getString("TITLE.LIST.BYCHAR") + ":\n";
        Jogador jogadorAtivo = WorldFacade.getInstance().getJogadorAtivo();
        //lista todos os personagens
        for (Iterator<Personagem> iter = WorldFacade.getInstance().getPersonagens(); iter.hasNext();) {
            Personagem personagem = iter.next();
            if (jogadorAtivo.isNacao(personagem.getNacao())) {
                //ret += personagemFacade.getNome(personagem);
                //ret += "\t@" + personagemFacade.getCoordenadas(personagem) + "\n";
                ret += personagemFacade.getResultadoLocal(personagem);
                List<String[]> pericias = personagemFacade.getPericias(personagem, WorldFacade.getInstance().getCenario());
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
        final Jogador jogadorAtivo = WorldFacade.getInstance().getJogadorAtivo();
        //lista todos as cidades
        for (Cidade cidade : WorldFacade.getInstance().getCidades()) {
            if (jogadorAtivo.isNacao(cidade.getNacao())) {
                for (String msg : cidadeFacade.getInfoTitle(cidade)) {
                    ret += msg;
                }
                ret += "\n";
                ret += ordemFacade.getResultado(cidade);
                ret += getActorOrdersString(cidade);
                ret += "\n";
            }
        }
        return ret;
    }

    private String listaOrdensByNation() {
        String ret = labels.getString("TITLE.LIST.BYNATION") + ":\n";
        //lista todos as cidades
        for (Nacao nacao : WorldFacade.getInstance().getNacoesJogadorAtivo()) {
            for (String msg : nacaoFacade.getInfoTitle(nacao)) {
                ret += msg;
            }
            ret += "\n";
            ret += ordemFacade.getResultado(nacao);
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

    private String listaOrdensBySequence() {
        String ret = labels.getString("TITLE.LIST.BYSEQ") + ":\n";
        Jogador jogadorAtivo = WorldFacade.getInstance().getJogadorAtivo();
        SortedMap<Integer, List<PersonagemOrdem>> ordens = new TreeMap<Integer, List<PersonagemOrdem>>();
        //list all actions from all actors
        for (BaseModel actor : WorldFacade.getInstance().getActors()) {
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
        return WorldFacade.getInstance().getCenarioNome();
    }

    public String getJogadorAtivoNome() {
        return WorldFacade.getInstance().getJogadorAtivoNome();
    }

    public String getNacoesJogadorAtivoNome() {
        return WorldFacade.getInstance().getNacoesJogadorAtivoNome();
    }

    public int getNacoesJogadorAtivoQtd() {
        return WorldFacade.getInstance().getJogadorAtivo().getNacoes().size();
    }

    public String getPartidaNome() {
        return WorldFacade.getInstance().getPartidaNome();
    }

    public int getTurno() {
        return WorldFacade.getInstance().getTurno();
    }

    public String getDeadline() {
        try {
            return WorldFacade.getInstance().getPartida().getDeadline().toDateTimeString();
        } catch (NullPointerException e) {
            return "?";
        }
    }

    public int getDeadlineDaysRemaining() {
        try {
            return WorldFacade.getInstance().getPartida().getDeadline().getDaysDiffToNow();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Carrega o arquivo verifica integridade do arquivo verifica se o
     * turno/nacao/jogador eh correto limpa as ordens atuais existentes carrega
     * as ordens personagem por personagem atualiza GUI indica quantas ordens
     * foram carregadas/descartadas
     */
    private void setComando(File file) {
        try {
            Comando comando = (Comando) XmlManager.getInstance().get(file);
            //verificar serial violado
            if (!comando.isSerial()) {
                throw new IllegalStateException(labels.getString("SERIAL.VIOLATION") + file.getName());
            }
            //verificar se o turno 'e correto
            Partida partida = WorldFacade.getInstance().getPartida();
            if (comando.getTurno() != partida.getTurno()) {
                throw new IllegalStateException(labels.getString("TURNO.ERRADO") + file.getName());
            }
            int qtPackageCarregadas = this.setPackage(comando.getPackages());
            int qtOrdensCarregadas = this.setOrdens(comando);
            this.getGui().setStatusMsg(String.format("%d %s %s", qtOrdensCarregadas, labels.getString("ORDENS.CARREGADAS"), file.getName()));
            //SysApoio.showDialogError(String.format("%d %s %s", qtOrdensCarregadas, labels.getString("ORDENS.CARREGADAS"), file.getName()));
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
    private int setOrdens(Comando comando) {
        int ret = 0;
        SortedMap<String, BaseModel> listPers = new TreeMap<String, BaseModel>();
        //limpa todas as ordens
        for (BaseModel actor : WorldFacade.getInstance().getActors()) {
            actor.remAcoes();
            listPers.put(actor.getCodigo(), actor);
        }
        //limpa financas.
        getDispatchManager().sendDispatchForMsg(DispatchManager.CLEAR_FINANCES_FORECAST, "");

        try {
            //carrega as ordens personagem por personagem
            for (ComandoDetail comandoDetail : comando.getOrdens()) {
                BaseModel actor = listPers.get(comandoDetail.getActorCodigo());
                Ordem ordem = WorldFacade.getInstance().getOrdem(comandoDetail.getOrdemCodigo());
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
                    //atualiza financas e outras dependencias
                    getDispatchManager().sendDispatchForChar(null, po);
                    ordemFacade.setOrdem(actor, indexOrdem, po);
                    //atualiza GUI
//                    this.getGui().getTabPersonagem().setValueFor(ordemDisplay, personagem.getNome(), indexOrdem);
                    ret++;
                } catch (NullPointerException ex) {
                    //nao faz nada, ordens nao disponiveis...
                    log.fatal("problems loading actions: " + comandoDetail.getOrdemCodigo());
                }
            }
        } catch (Exception e) {
            log.info(e);
        }
        SettingsManager.getInstance().setTableColumnAdjust(SysProperties.getProps("TableColumnAdjust", "0").equalsIgnoreCase("0"));
        getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_RELOAD, "");
        SettingsManager.getInstance().setTableColumnAdjust(true);
        return ret;
    }

    public boolean isGameOver() {
        return WorldFacade.getInstance().isGameOver();
    }

    public boolean isJogadorAtivoEliminado() {
        return WorldFacade.getInstance().isJogadorAtivoEliminado(WorldManager.getInstance().getPartida().getJogadorAtivo());
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
    public void receiveDispatch(PersonagemOrdem antes, PersonagemOrdem depois) {
        if (depois != null) {
            this.getGui().setStatusMsg(
                    String.format("%s: %s [$%s]",
                    depois.getNome(),
                    depois.getOrdem().getDescricao(),
                    acaoFacade.getCusto(depois)));
        }
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.SET_LABEL_MONEY) {
            getGui().setLabelMoney(txt);
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

    private boolean doSaveActorActions(Jogador jogadorAtivo, Comando comando) {
        boolean missing = false;
        //lista todos os personagens, carregando para o xml
        for (BaseModel actor : WorldFacade.getInstance().getActors()) {
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
            } else if (cenarioFacade.hasOrdens(WorldFacade.getInstance().getPartida(), actor)) {
                missing = true;
            }
        }
        return missing;
    }

    private boolean doSavePackages(Comando comando) {
        boolean missing = true;
        if (WorldFacade.getInstance().isStartupPackages() && WorldFacade.getInstance().getTurno() == 0) {
            for (Nacao nacao : WorldManager.getInstance().getNacoesJogadorAtivo()) {
                comando.addPackage(nacao, getPackages(nacao));
                missing = false;
                log.info(nacao.getNome() + " + " + getPackages(nacao));
            }
        } else {
            return false;
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
        Nacao nacao = null;
        for (Integer idNacao : packages.keySet()) {
            try {
                nacao = findNacao(idNacao);
                clearPackages(nacao);
                nacao.addHabilidades(WorldFacade.getInstance().getHabilidades(packages.get(idNacao)));
            } catch (NullPointerException ex) {
                log.fatal("Something wrong loading packages.");
            }
        }
        getDispatchManager().sendDispatchForMsg(DispatchManager.PACKAGE_RELOAD, "reload");
        return packages.size();
    }

    private Nacao findNacao(int idNacao) {
        for (Nacao nacao : WorldManager.getInstance().getNacoes().values()) {
            if (nacao.getId() == idNacao) {
                return nacao;
            }
        }
        return null;
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

    private boolean doSendViaPost(File attachment) {
        try {
            Partida partida = WorldFacade.getInstance().getPartida();
            int ret = WebCounselorManager.getInstance().doSendViaPost(attachment, partida, listaOrdens());

            if (ret == WebCounselorManager.OK) {
                final String msg = String.format(labels.getString("POST.DONE"), attachment.getName());
                log.info(msg);
                this.getGui().setStatusMsg(msg);
                if (SysProperties.getProps("SendOrderConfirmationPopUp", "1").equals("1")) {
                    SysApoio.showDialogInfo(labels.getString("POST.DONE.TITLE"), labels.getString("POST.DONE.TITLE"));
                }
                return true;
            } else if (ret == WebCounselorManager.ERROR_GAMECLOSED) {
                //display alert!
                SysApoio.showDialogError(labels.getString("ENVIAR.ERRO.GAMECLOSED"), labels.getString("ENVIAR.ERRO"));
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return true; //dont try email
            } else if (ret == WebCounselorManager.ERROR_TURN) {
                final String expectedTurn = String.format(labels.getString("ENVIAR.ERRO.WRONGTURN"), WebCounselorManager.getInstance().getLastResponseString());
                //display alert!
                SysApoio.showDialogError(expectedTurn, labels.getString("ENVIAR.ERRO"));
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return true; //dont try email
            } else {
                SysApoio.showDialogError(labels.getString("ERROR"), labels.getString("ENVIAR.ERRO"));
                this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
                return false;
            }
        } catch (PersistenceException ex) {
            this.getGui().setStatusMsg(String.format(labels.getString("POST.DONE.NOT"), attachment.getName()));
            return false;
        }
    }

    public boolean doSendViaEmail(File attachment, String msg) {
        /* prepara o email, pede informacoes se properties
         * nao estao preenchidas salva novas informacoes no properties !
         * pergunta se quer receber uma copia? ! 
         * envia o email e avisa do recibo.
         */
        this.getGui().setStatusMsg(labels.getString("ENVIAR.JUDGE"));
        String from = getEmail();
        if (from.equals("none")) {
            return false;
        }
        try {
            SmtpManager email = new SmtpManager();
            email.setToCc(from);
            email.setFrom(from);
            email.setBody(listaOrdensEmailBody(msg));
            String subject = null;
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
        }
    }

    private String getEmail() {
        String from = SysProperties.getProps("MyEmail", "none");
        if (from.equals("none")) {
            from = JOptionPane.showInputDialog(labels.getString("ENVIAR.INPUT.EMAIL"), from);
            if (from == null || from.equals("none")) {
                this.getGui().setStatusMsg(labels.getString("ENVIAR.FALTOU.FROM"));
                SysApoio.showDialogError(labels.getString("ENVIAR.FALTOU.FROM"));
                return "none";
            }
            //salva novas informacoes no properties
            SysProperties.getInstance().setProp("MyEmail", from);
        }
        return from;
    }
}
