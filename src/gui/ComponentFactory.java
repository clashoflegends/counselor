/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import baseLib.GenericoComboBoxModel;
import baseLib.IBaseModel;
import baseLib.SysApoio;
import business.facade.ExercitoFacade;
import control.MapaControler;
import control.OrdemControler;
import control.support.ActorInterface;
import gui.subtabs.SubTabCoordenadas;
import gui.subtabs.SubTabDirecaoExercito;
import gui.subtabs.SubTabRelacionamento;
import gui.subtabs.SubTabTropas;
import java.awt.Component;
import java.io.Serializable;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import model.Exercito;
import model.Ordem;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class ComponentFactory implements Serializable {

    private static final Log log = LogFactory.getLog(ComponentFactory.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private ActorInterface actor;
    private boolean allSelected = false;
    private MapaControler mapaControler;
    private OrdemControler ordemControl;
    // Constantes para busca das chaves no banco.
    private static final String ALIANCA = "Alianca";
    private static final String ARTEFATO_CARRIED = "Artefato_Carried";
    private static final String ARTEFATO_CARRIED_SCRY = "Artefato_Carried_Scry";
    private static final String ARTEFATO_CARRIED_SUMMON = "Artefato_Carried_Summon";
    private static final String ARTEFATO_CARRIED_DRAGONEGG = "Artefato_Carried_Dragonegg";
    private static final String ARTEFATO_NACAO_DROP = "Artefato_Nacao_Drop";
    private static final String ARTEFATO_NONACAO = "Artefato_NoNacao";
    private static final String ARTEFATO_ALL = "Artefato_All";
    private static final String CIDADE_ANY = "Cidade_Any";
    private static final String CIDADE_NACAO = "Cidade_Nacao";
    private static final String CIDADE_NEWCAPITAL = "Cidade_NewCapital";
    private static final String COORDENADA = "Coordenada";
    private static final String COORDENADAW = "CoordenadaW";
    private static final String COORDENADA_12 = "Coordenada_12";
    private static final String COORDENADA_5 = "Coordenada_5";
    private static final String COORDENADA_8 = "Coordenada_8";
    private static final String COORDENADA_8W = "Coordenada_8W";
    private static final String COORDENADA_NAVIO = "Coordenada_Navio";
    private static final String DIRECAO = "Direcao";
    private static final String DIRECAO_EX = "Direcao_Ex";
    private static final String EVASIVO = "Evasivo";
    private static final String MALE_FEMALE = "Male_Female";
    private static final String MAGIA_ALL = "Magia_All";
    private static final String MAGIA_KNOWN = "Magia_Known";
    private static final String MAGIA_PRE = "Magia_Pre";
    private static final String NACAO = "Nacao";
    private static final String NACAO_RELACIONAMENTO = "Nacao_Relacionamento";
    private static final String NOME = "Nome";
    private static final String NOVACIDADE = "NovaCidade";
    private static final String NO_VALUE = "None";
    private static final String OURO = "Ouro";
    private static final String PERCENTAGE = "Percentage";
    private static final String PERSONAGEM_LOCAL_EX_COMANDANTE = "Personagem_Comandante_Ex";
    private static final String PERSONAGEM_LOCAL = "Personagem_Local";
    private static final String PERSONAGEM_LOCAL_SELF = "Personagem_Local_Self";
    private static final String PERSONAGEM_LOCAL_NACAO = "Personagem_Nacao_Local";
    private static final String PERSONAGEM_LOCAL_NONACAO = "Personagem_Local_NoNacao";
    private static final String PERSONAGEM_NONACAO = "Personagem_NoNacao";
    private static final String PERSONAGEM_NACAO = "Personagem_Nacao";
    private static final String PERSONAGEM_REFEM = "Personagem_Refem";
    private static final String PERSONAGEM_REFEM_NONACAO = "Personagem_Refem_NoNacao";
    private static final String PRODUTO = "Produto";
    private static final String PRODUTO_ALL = "Produto_All";
    private static final String PRODUTO_ARMADURA = "Produto_Armadura";
    private static final String PRODUTO_WEAPON = "Produto_Metal";
    private static final String QUANTIDADE = "Quantidade";
    private static final String RELACIONAMENTO_MUDA = "Relacionamento_Muda";
    private static final String REFEM = "Refem";
    private static final String SIM_NAO = "Sim_Nao";
    private static final String TATICA = "Tatica";
    private static final String TEXTO_20 = "Texto_20";
    private static final String TROPA_TIPO = "Tropa_Tipo";
    private static final String TROPA_TABLE = "Tropa_Table";
    private static final String TROPA_TABLE_TR = "Tropa_Table_Tr";
    private static final String TROPA_TABLE_GARRISON = "Tropa_Table_Garrison";
    private static final String VARIADO = "Variado";

    /**
     * Prepara o componente do parametro da ordem [JComboBox ou
     * JFormattedTextField]
     *
     * @param controle
     * @return componente
     */
//    private Component get ParametroComponent(String controle, String vlInicial,
//            Ordem ordemSelecionada, Ordem ordemGravada) {
    public Component getParametroComponent(String controle, String vlInicialDisplay, String vlDefaultId,
            Ordem ordemSelecionada) {

        Component cNovo = new JLabel(labels.getString("CONTROLE.NAO.IMPLEMENTADO"));
        if (controle.equals(ALIANCA)) {            //Alianca
            //FIXME: carregar do cenario, nao utilizada no grecia
            JComboBox cbTemp = new JComboBox(new GenericoComboBoxModel(
                    new String[][]{
                {labels.getString("ALIANCA.SERVOS.NEGROS"), "sn"},
                {labels.getString("ALIANCA.NEUTROS"), "ne"},
                {labels.getString("ALIANCA.POVOS.LIVRES"), "pl"}
            }));
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_ALL)) {            //Artefato-All
            //é um combo com TODOS os artefatos
            JComboBox cbTemp = new JComboBox(getActor().getArtefatoComboModel(0));
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_CARRIED)) {            //Artefato-Carried
            //é um combo com os artefatos que o actor possui
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(1));
            }
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_CARRIED_SCRY)) {            //Artefato-Carried
            //é um combo com os artefatos que o actor possui
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(4));
            }
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_CARRIED_SUMMON)) {            //Artefato-Carried
            //é um combo com os artefatos que o actor possui
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(5));
            }
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_CARRIED_DRAGONEGG)) {            //Artefato-Carried
            //é um combo com os artefatos que o actor possui
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(6));
            }
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_NACAO_DROP)) {            //Artefato-Nacao-Drop
            //é um combo com os artefatos que a nacao possui
            JComboBox cbTemp = new JComboBox(getActor().getArtefatoComboModel(2));
            cNovo = cbTemp;
        } else if (controle.equals(ARTEFATO_NONACAO)) {            //Artefato-NoNacao
            //é um combo com os artefatos que a nacao NAO possui
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getArtefatoComboModel(3));
            }
            cNovo = cbTemp;
        } else if (controle.equals(CIDADE_ANY)) {            //Cidade-ANY
            if (isAllSelected()) {
                //Inputbox
                JFormattedTextField jtTemp = new JFormattedTextField(
                        SysApoio.createFormatter("####"));
                jtTemp.setName("jtCidadeAny"); // NOI18N
                jtTemp.setColumns(4);
                cNovo = jtTemp;
            } else {
                //é um combo com o model com os personagens conhecidos no local da mesma nacao
                JComboBox cbTemp = new JComboBox(getActor().getCidadeComboModel(0));
                cNovo = cbTemp;
            }
        } else if (controle.equals(CIDADE_NACAO)) {            //Cidade-Nacao
            if (isAllSelected()) {
                //Inputbox
                JFormattedTextField jtTemp = new JFormattedTextField(
                        SysApoio.createFormatter("####"));
                jtTemp.setName("jtCidadeNacao"); // NOI18N
                jtTemp.setColumns(4);
                cNovo = jtTemp;
            } else {
                //é um combo com o model com os personagens conhecidos no local da mesma nacao
                JComboBox cbTemp = new JComboBox(getActor().getCidadeComboModel(1));
                cNovo = cbTemp;
            }
        } else if (controle.equals(CIDADE_NEWCAPITAL)) {            //Cidade-NewCapital
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(
                    SysApoio.createFormatter("####"));
            jtTemp.setName("jtCidadeNewCapital"); // NOI18N
            jtTemp.setColumns(4);
            cNovo = jtTemp;
        } else if (controle.equals(COORDENADA)) {            //Coordenada
            //Novo combobox com locais dentro do range
            final int range = 9999;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADAW)) {            //Coordenada
            //Novo combobox com locais dentro do range
            final int range = 9999;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), true, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_8)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 8;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_5)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 5;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_8W)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 8;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), true, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_12)) {
            //Coordenada-12
            //Novo combobox com locais dentro do range
            final int range = 12;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_NAVIO)) {            //Coordenada-Navio
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(
                    SysApoio.createFormatter("####"));
            jtTemp.setName("jtCoordenadaNavio"); // NOI18N
            jtTemp.setColumns(4);
            cNovo = jtTemp;
        } else if (controle.equals(DIRECAO)) {            //Direcao
            //é um combo com as direcoes validas
            JComboBox cbTemp = new JComboBox(new GenericoComboBoxModel(BaseMsgs.direcoes));
            cNovo = cbTemp;
        } else if (controle.equals(DIRECAO_EX)) {    //Direcao-Ex
            //PENDING: Fazer uma rosa dos ventos?
            SubTabDirecaoExercito jpTemp = new SubTabDirecaoExercito(
                    vlInicialDisplay, this.getActor(), this.getMapaControler(), ordemSelecionada, isAllSelected());
            cNovo = jpTemp;
        } else if (controle.equals(EVASIVO)) {            //Evasivo
            //é um combo male/female
            JComboBox cbTemp = new JComboBox(new GenericoComboBoxModel(
                    new String[][]{
                {labels.getString("NORMAL"), "nr"},
                {labels.getString("EVASIVO"), "EV"}
            }));
            cNovo = cbTemp;
        } else if (controle.equals(MALE_FEMALE)) {            //M/F
            //é um combo male/female
            JComboBox cbTemp = new JComboBox(new GenericoComboBoxModel(
                    new String[][]{
                {labels.getString("MASCULINO"), "m"},
                {labels.getString("FEMININO"), "f"}
            }));
            cNovo = cbTemp;
        } else if (controle.equals(MAGIA_ALL)) {//Magia-All
            //é um combo com o model magia, todas as magias que o actor nao tem
            JComboBox cbTemp = new JComboBox(getActor().getFeiticoComboModel(0));
            cNovo = cbTemp;
        } else if (controle.equals(MAGIA_PRE)) {//Magia-Pre
            //é um combo com o model magia, todas as magias que o actor pode aprender
            //PENDING: filtrar magias por pre-requisito, talvez livro.
            int filtro = 2;
            if (isAllSelected()) {
                filtro = 1;
            }
            JComboBox cbTemp = new JComboBox(getActor().getFeiticoComboModel(filtro));
            cNovo = cbTemp;
        } else if (controle.equals(MAGIA_KNOWN)) {//Magia-Known
            //é um combo com o model magia do actor para a ordem
            JComboBox cbTemp = new JComboBox(getActor().getFeiticoComboModelByOrdem(ordemSelecionada, isAllSelected()));
            cbTemp.setActionCommand("jcMagia");
            cbTemp.addActionListener(getOrdemControl());
            cNovo = cbTemp;
        } else if (controle.equals(NACAO)) {//Nacao
            //é um combo com o model nacao, menos a nacao do actor
            JComboBox cbNacao = new JComboBox(getActor().getNacaoComboModel());
            cNovo = cbNacao;
        } else if (controle.equals(NACAO_RELACIONAMENTO)) {//Nacao_RELACIONAMENTO
            SubTabRelacionamento jpTemp = new SubTabRelacionamento(vlDefaultId, getActor().getNacao(),
                    getActor().getNacaoComboModel(), isAllSelected());
            cNovo = jpTemp;
        } else if (controle.equals(NOME)) {//Nome
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(
                    SysApoio.createFormatterName("U*******************"));
            jtTemp.setColumns(20);
            cNovo = jtTemp;
        } else if (controle.equals(NOVACIDADE)) {//Nome
            //two in one (name + hex)
            CompDuplo jtTemp = new CompDuplo();
            String[] vlDisplay = new String[2];
            if (vlInicialDisplay.contains(";")) {
                vlDisplay = vlInicialDisplay.split(";");
            }
            String[] vlId = new String[2];
            if (vlDefaultId.contains(";")) {
                vlId = vlDefaultId.split(";");
            }
            final Component nome = this.getParametroComponent(NOME, vlDisplay[0], vlId[0], ordemSelecionada);
            final Component hex = this.getParametroComponent(COORDENADA_5, vlDisplay[1], vlId[1], ordemSelecionada);
            jtTemp.replaceA(nome);
            jtTemp.replaceB(hex);
            cNovo = jtTemp;
        } else if (controle.equals(NO_VALUE)) {         //None
            //Nao deveria ocorrer nunca para uma ordem. Ocorre para algumas magias.
            //Esconder o parametro 1
            throw new UnsupportedOperationException(labels.getString("NOT.IMPLEMENTED"));
        } else if (controle.equals(OURO)) {//Ouro
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(SysApoio.createFormatterInteger(0, 999999));
            jtTemp.setName("jtOuro"); // NOI18N
            jtTemp.setColumns(5);
            cNovo = jtTemp;
        } else if (controle.equals(PERCENTAGE)) {//Percentage
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(SysApoio.createFormatterInteger(1, 100));
            //jtTemp.setName("jtPercentage"); // NOI18N
            jtTemp.setColumns(3);
            jtTemp.setText("100");
            cNovo = jtTemp;
        } else if (controle.equals(PERSONAGEM_LOCAL_EX_COMANDANTE)) {//Personagem-Comandante-Ex
            //é um combo com o model com os personagens da mesma nacao conhecidos que comandam exercitos
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(1));
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_LOCAL)) {//Personagem-Local
            //é um combo com o model com os personagens conhecidos no local, independente da nacao.
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(2));
            cbTemp.setEditable(true);
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_LOCAL_NACAO)) {//Personagem-Nacao-Local
            //é um combo com o model com os personagens conhecidos no local da mesma nacao
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(3));
            cbTemp.setEditable(true);
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_LOCAL_NONACAO)) {//Personagem-Local-NoNacao
            //é um combo com o model com os personagens conhecidos no local e de outra nacao
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(4));
            cbTemp.setEditable(true);
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_LOCAL_SELF)) {//Personagem-Local
            //é um combo com o model com os personagens conhecidos no local, independente da nacao.
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(6));
            cbTemp.setEditable(true);
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_NONACAO)) {//Personagem-NoNacao
            //é um combo com o model com os personagens conhecidos de outras nacoes
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(0));
            cbTemp.setEditable(true);
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_NACAO)) { //Personagem-Nacao
            //é um combo com o model com os personagens conhecidos
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(5));
            cbTemp.setEditable(true);
            cNovo = cbTemp;
        } else if (controle.equals(PERSONAGEM_REFEM)) { //Personagem-Refem
            /*
             * FIXME: Refem nao ocorre no cenario 4. deixei pra depois
             */
        } else if (controle.equals(PERSONAGEM_REFEM_NONACAO)) { //Personagem-Refem-NoNacao
            /*
             * FIXME: Refem nao ocorre no cenario 4. deixei pra depois
             */
        } else if (controle.equals(PRODUTO)) {//Produto
            //é um combo com o model com as produtos do cenario, MENOS ouro
            JComboBox cbTemp = new JComboBox(getActor().getProdutoComboModel(1));
            cNovo = cbTemp;
        } else if (controle.equals(PRODUTO_ALL)) {//Produto-All
            //é um combo com o model com as produtos do cenario, inclusive ouro
            JComboBox cbTemp = new JComboBox(getActor().getProdutoComboModel(0));
            cNovo = cbTemp;
        } else if (controle.equals(PRODUTO_ARMADURA)) {//Produto-Armadura
            //é um combo com o model com as produtos do cenario
            JComboBox cbTemp = new JComboBox(getActor().getProdutoComboModel(2));
            cNovo = cbTemp;
        } else if (controle.equals(PRODUTO_WEAPON)) {//Produto-Metal
            //é um combo com o model com as produtos do cenario
            JComboBox cbTemp = new JComboBox(getActor().getProdutoComboModel(3));
            cNovo = cbTemp;
        } else if (controle.equals(QUANTIDADE)) {//Quantidade
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(SysApoio.createFormatterInteger(0, 999999));
            jtTemp.setColumns(5);
            cNovo = jtTemp;
        } else if (controle.equals(RELACIONAMENTO_MUDA)) {//Relacionamento_Muda
            //é um combo Melhora/Piora
            JComboBox cbTemp = new JComboBox(new GenericoComboBoxModel(
                    new String[][]{
                {labels.getString("MELHORAR"), "1"},
                {labels.getString("REBAIXAR"), "-1"}}));
            cNovo = cbTemp;
        } else if (controle.equals(REFEM)) { //Refem
            /*
             * FIXME: Refem nao ocorre no cenario 4. deixei pra depois
             */
        } else if (controle.equals(SIM_NAO)) {//Sim_Nao
            //é um combo sim/nao 
            JComboBox cbTemp = new JComboBox(new GenericoComboBoxModel(
                    new String[][]{
                {labels.getString("SIM"), "Sim"},
                {labels.getString("NAO"), "Nao"}
            }));
            cNovo = cbTemp;
        } else if (controle.equals(TATICA)) {//Tatica
            //é um combo com o model com as taticas do cenario
            JComboBox cbTemp = new JComboBox(getOrdemControl().getTaticasComboModel());
            cNovo = cbTemp;
        } else if (controle.equals(TEXTO_20)) {//Texto-20
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(
                    SysApoio.createFormatter("********************"));
            jtTemp.setColumns(20);
            cNovo = jtTemp;
        } else if (controle.equals(TROPA_TIPO)) {//Tropa-Tipo
            //é um combo com o model com os tipos de tropa que a nacao pode recrutar
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getTropaTipoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getTropaTipoComboModel(1));
            }
            cNovo = cbTemp;
        } else if (controle.equals(TROPA_TABLE)) {//Tropa-Table
            //é uma table com o model com os tipos de tropa do cenario + quantidade
            SubTabTropas jpTemp = new SubTabTropas(vlDefaultId, this.getActor().getExercito(), isAllSelected(), 1);
            cNovo = jpTemp;
        } else if (controle.equals(TROPA_TABLE_TR)) {//Tropa-Table
            //é uma table com o model com os tipos de tropa que podem transferir + quantidade
            SubTabTropas jpTemp = new SubTabTropas(vlDefaultId, this.getActor().getExercito(), isAllSelected(), 2);
            cNovo = jpTemp;
        } else if (controle.equals(TROPA_TABLE_GARRISON)) {//Tropa-Table
            //é uma table com o model com os tipos de tropa do cenario + quantidade
            ExercitoFacade ef = new ExercitoFacade();
            Exercito guarnicao = ef.getGuarnicao(getActor().getNacao(), this.getActor().getLocal());
            SubTabTropas jpTemp = new SubTabTropas(vlDefaultId, guarnicao, isAllSelected(), 1);
            cNovo = jpTemp;
        } else if (controle.equals(VARIADO)) {//Variado
            /*
             * nao gera aki. 
             * gera especifico na GUI
             */
            cNovo = new JLabel(labels.getString("CONTROLE.VARIADO"));
        }
        //seleciona item default
        if (cNovo instanceof JComboBox) {
            JComboBox cbTemp = (JComboBox) cNovo;
            try {
                GenericoComboBoxModel gcbm = (GenericoComboBoxModel) cbTemp.getModel();
                if (!vlInicialDisplay.equals("")) {
                    int index = gcbm.getIndexByDisplay(vlInicialDisplay);
                    if (index == 0 && cbTemp.isEditable()) {
                        cbTemp.insertItemAt(vlInicialDisplay, 0);
                    }
                    cbTemp.setSelectedIndex(index);
                } else if (controle.equals(CIDADE_NACAO)) {
                    String actorLocal = getActor().getLocalCoordenadas();
                    int index = gcbm.getIndexById(actorLocal);
                    if (index == 0 && cbTemp.isEditable()) {
                        cbTemp.insertItemAt(actorLocal, 0);
                    }
                    cbTemp.setSelectedIndex(index);
                }
            } catch (ClassCastException ex) {
                log.info(ex);
            } catch (IllegalArgumentException ex) {
            } catch (NullPointerException ex) {
                //PENDING: nao tem nada pra selecionar, provavelmente nao pode dar esta ordem agora
                //log.info("JG: " + ex);
            }
        } else if (cNovo instanceof JFormattedTextField) {
            //&& ordemSelecionada == actorOrdemGravada
            JFormattedTextField jtTemp = (JFormattedTextField) cNovo;
            jtTemp.setText(vlInicialDisplay);
        }
        if (cNovo instanceof JLabel && ((JLabel) cNovo).getText().equals(labels.getString("CONTROLE.NAO.IMPLEMENTADO"))) {
            log.fatal(labels.getString("CONTROLE.NAO.IMPLEMENTADO") + controle);
        }
        return cNovo;
    }

    public Component getBlank(boolean visible) {
        Component cNovo = new JLabel(labels.getString("CONTROLE.NAO.IMPLEMENTADO"));
        cNovo.setVisible(visible);
        return cNovo;
    }

    public void getParametros(Component comp, List<String> parId, List<String> parDisplay) {
        String tempId = " ", tempDisplay = " ";
        try {
            if (comp instanceof JComboBox) {
                JComboBox cb = (JComboBox) comp;
                try {
                    IBaseModel par = (IBaseModel) cb.getModel().getSelectedItem();
                    tempId = par.getComboId();
                    tempDisplay = par.getComboDisplay();
                } catch (ClassCastException ex) {
                    String par = (String) cb.getModel().getSelectedItem();
                    tempId = par;
                    tempDisplay = par;
                }
            } else if (comp instanceof JTextField) {
                JTextField par = (JTextField) comp;
                tempId = par.getText();
                tempDisplay = par.getText();
            } else if (comp instanceof SubTabDirecaoExercito) {
                SubTabDirecaoExercito par = (SubTabDirecaoExercito) comp;
                tempId = par.getDirecoesIdTipo();
                tempDisplay = par.getDirecaoDisplay();
            } else if (comp instanceof SubTabTropas) {
                SubTabTropas par = (SubTabTropas) comp;
                tempId = par.getTropasId();
                tempDisplay = par.getTropasDisplay();
            } else if (comp instanceof SubTabCoordenadas) {
                SubTabCoordenadas temp = (SubTabCoordenadas) comp;
                IBaseModel par = temp.getCoordenadaSelected();
                tempId = par.getComboId();
                tempDisplay = par.getComboDisplay();
            } else if (comp instanceof SubTabRelacionamento) {
                SubTabRelacionamento temp = (SubTabRelacionamento) comp;
                tempId = temp.getParametrosId();
                tempDisplay = temp.getParametrosDisplay();
            } else if (comp instanceof CompDuplo) {
                CompDuplo temp = (CompDuplo) comp;
                String[] parametros = temp.getParametros();
                tempId = parametros[0];
                tempDisplay = parametros[1];
            }
        } catch (NullPointerException ex) {
            //algum parametro nao foi informado.
            tempId = " ";
            tempDisplay = " ";
            log.info(labels.getString("FALTANDO.PARAMETRO") + ex);
        }
        parId.add(SysApoio.removeAcentos(tempId));
        parDisplay.add(tempDisplay);
    }

    private ActorInterface getActor() {
        return actor;
    }

    public void setActor(ActorInterface actor) {
        this.actor = actor;
    }

    private boolean isAllSelected() {
        return allSelected;
    }

    public void setAllSelected(boolean isAllSelected) {
        this.allSelected = isAllSelected;
    }

    private MapaControler getMapaControler() {
        return this.mapaControler;
    }

    public void setMapaControler(MapaControler mapaControl) {
        this.mapaControler = mapaControl;
    }

    private OrdemControler getOrdemControl() {
        return ordemControl;
    }

    public void setOrdemControl(OrdemControler ordemControl) {
        this.ordemControl = ordemControl;
    }
}
