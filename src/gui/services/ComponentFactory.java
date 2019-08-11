/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import baseLib.GenericoComboBoxModel;
import baseLib.IBaseModel;
import business.facade.ExercitoFacade;
import control.MapaControler;
import control.OrdemControler;
import control.support.ActorInterface;
import gui.accessories.DialogHexView;
import gui.components.DialogTextArea;
import gui.components.JLabelGradient;
import gui.subtabs.SubTabCoordenadas;
import gui.subtabs.SubTabDirecao;
import gui.subtabs.SubTabDirecaoExercito;
import gui.subtabs.SubTabRelacionamento;
import gui.subtabs.SubTabTropas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
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
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

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
    private static final String COORDENADAC = "Coordenadac";
    private static final String COORDENADAX = "Coordenadax";
    private static final String COORDENADA_3 = "Coordenada_3";
    private static final String COORDENADA_3C = "Coordenada_3c";
    private static final String COORDENADA_3X = "Coordenada_3x";
    private static final String COORDENADA_3W = "Coordenada_3w";
    private static final String COORDENADA_5 = "Coordenada_5";
    private static final String COORDENADA_5C = "Coordenada_5c";
    private static final String COORDENADA_5X = "Coordenada_5x";
    private static final String COORDENADA_5W = "Coordenada_5w";
    private static final String COORDENADA_8 = "Coordenada_8";
    private static final String COORDENADA_8C = "Coordenada_8c";
    private static final String COORDENADA_8X = "Coordenada_8x";
    private static final String COORDENADA_8W = "Coordenada_8W";
    private static final String COORDENADA_12 = "Coordenada_12";
    private static final String COORDENADA_12C = "Coordenada_12c";
    private static final String COORDENADA_12X = "Coordenada_12x";
    private static final String COORDENADA_12W = "Coordenada_12w";
    private static final String COORDENADA_NAVIO = "Coordenada_Navio";
    private static final String DIRECAO = "Direcao";
    private static final String DIRECAO_EX = "Direcao_Ex";
    private static final String DIRECAO_3 = "Direcao_3";
    private static final String EVASIVO = "Evasivo";
    private static final String MALE_FEMALE = "Male_Female";
    private static final String MAGIA_ALL = "Magia_All";
    private static final String MAGIA_KNOWN = "Magia_Known";
    private static final String MAGIA_PRE = "Magia_Pre";
    private static final String NACAO = "Nacao";
    private static final String NACAO_ALLY = "Nacao_Ally";
    private static final String NACAO_RELACIONAMENTO = "Nacao_Relacionamento";
    private static final String NOME = "Nome";
    private static final String NOVACIDADE5 = "NovaCidade";
    private static final String NOVACIDADE8 = "NovaCidade_8";
    private static final String NOMEGENDER = "NomeGender";
    private static final String NO_VALUE = "None";
    private static final String OURO = "Ouro";
    private static final String PERCENTAGE = "Percentage";
    private static final String PERSONAGEM_LOCAL_EX_COMANDANTE = "Personagem_Comandante_Ex";
    private static final String PERSONAGEM_LOCAL = "Personagem_Local";
    private static final String PERSONAGEM_LOCAL_SELF = "Personagem_Local_Self";
    private static final String PERSONAGEM_LOCAL_NACAO = "Personagem_Nacao_Local";
    private static final String PERSONAGEM_LOCAL_NACAO_NONHERO = "Personagem_Nacao_Nonhero_Local";
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
    private static final String TERRENO = "Terreno";
    private static final String TEXTO_20 = "Texto_20";
    private static final String TROPA_TIPO = "Tropa_Tipo";
    private static final String TROPA_TIPO_BASIC = "Tropa_Tipo_Basic";
    private static final String TROPA_TABLE = "Tropa_Table";
    private static final String TROPA_TABLE_TR = "Tropa_Table_Tr";
    private static final String TROPA_TABLE_GARRISON = "Tropa_Table_Garrison";
    private static final String VARIADO = "Variado";

    /**
     * Prepara o componente do parametro da ordem [JComboBox ou JFormattedTextField]
     *
     * @param controle
     * @return componente
     */
//    private Component get ParametroComponent(String controle, String vlInicial,
//            Ordem ordemSelecionada, Ordem ordemGravada) {
    public Component getParametroComponent(String controle, String vlInicialDisplay, String vlDefaultId,
            Ordem ordemSelecionada) {

        Component cNovo = null;
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
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADAC)) {            //Coordenada, cities only
            //Novo combobox com locais dentro do range
            final int range = 9999;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 1, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADAX)) {            //Coordenada, hide cities
            //Novo combobox com locais dentro do range
            final int range = 9999;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), true, 2, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADAW)) {            //Coordenada
            //Novo combobox com locais dentro do range
            final int range = 9999;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), true, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_3)) {            //Coordenada-3
            //combobox com locais dentro do range
            final int range = 3;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_3C)) {            //Coordenada-3, cities only
            //combobox com locais dentro do range, exclude cities
            final int range = 3;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 1, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_3X)) {            //Coordenada-3, hide cities
            //combobox com locais dentro do range, exclude cities
            final int range = 3;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 2, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_3W)) {            //Coordenada-3, hide cities
            //combobox com locais dentro do range, exclude cities
            final int range = 3;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_5)) {            //Coordenada-5
            //Novo combobox com locais dentro do range
            final int range = 5;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_5C)) {            //Coordenada-5
            //Novo combobox com locais dentro do range
            final int range = 5;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 1, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_5X)) {            //Coordenada-5
            //Novo combobox com locais dentro do range
            final int range = 5;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 2, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_5W)) {            //Coordenada-5
            //Novo combobox com locais dentro do range
            final int range = 5;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_8)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 8;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_8C)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 8;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 1, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_8X)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 8;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 2, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_8W)) {            //Coordenada-8
            //Novo combobox com locais dentro do range
            final int range = 8;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), true, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_12)) {
            //Coordenada-12
            //Novo combobox com locais dentro do range
            final int range = 12;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_12C)) {
            //Coordenada-12
            //Novo combobox com locais dentro do range
            final int range = 12;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 1, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_12X)) {
            //Coordenada-12
            //Novo combobox com locais dentro do range
            final int range = 12;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 2, this.getMapaControler());
            cNovo = jpTemp;
        } else if (controle.equals(COORDENADA_12W)) {
            //Coordenada-12
            //Novo combobox com locais dentro do range
            final int range = 12;
            SubTabCoordenadas jpTemp = new SubTabCoordenadas(vlDefaultId, getActor().getLocal(),
                    range, isAllSelected(), false, 0, this.getMapaControler());
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
        } else if (controle.equals(DIRECAO_3)) {    //Direcao-Ex
            //PENDING: Fazer uma rosa dos ventos?
            SubTabDirecao jpTemp = new SubTabDirecao(
                    vlInicialDisplay, this.getActor().getLocal(), 3, false, this.getMapaControler(), isAllSelected());
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
        } else if (controle.equals(NACAO_ALLY)) {//Nacao
            //é um combo com o model nacao, menos a nacao do actor
            JComboBox cbNacao = new JComboBox(getActor().getNacaoAllyComboModel());
            cNovo = cbNacao;
        } else if (controle.equals(NACAO_RELACIONAMENTO)) {//Nacao_RELACIONAMENTO
            SubTabRelacionamento jpTemp = new SubTabRelacionamento(vlDefaultId, getActor().getNacao(),
                    getActor().getNacaoNoEnemySwornComboModel(), isAllSelected());
            cNovo = jpTemp;
        } else if (controle.equals(NOME)) {//Nome
            //Inputbox
            JFormattedTextField jtTemp = new JFormattedTextField(
                    SysApoio.createFormatterName("U*******************"));
            jtTemp.setColumns(20);
            cNovo = jtTemp;
        } else if (controle.equals(NOMEGENDER)) {//Nome, Gender
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
            final Component hex = this.getParametroComponent(MALE_FEMALE, vlDisplay[1], vlId[1], ordemSelecionada);
            jtTemp.replaceA(nome);
            jtTemp.replaceB(hex);
            cNovo = jtTemp;
        } else if (controle.equals(NOVACIDADE5)) {//Nome, Hex
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
            final Component hex = this.getParametroComponent(COORDENADA_5X, vlDisplay[1], vlId[1], ordemSelecionada);
            jtTemp.replaceA(nome);
            jtTemp.replaceB(hex);
            cNovo = jtTemp;
        } else if (controle.equals(NOVACIDADE8)) {//Nome, Hex
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
            final Component hex = this.getParametroComponent(COORDENADA_8X, vlDisplay[1], vlId[1], ordemSelecionada);
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
            if (isAllSelected()) {
                cbTemp.setEditable(true);
            }
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
        } else if (controle.equals(PERSONAGEM_LOCAL_NACAO_NONHERO)) {
            //é um combo com o model com os personagens conhecidos no local da mesma nacao nao heros
            JComboBox cbTemp = new JComboBox(getActor().getPersonagensComboModel(7));
//            cbTemp.setEditable(true);
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
        } else if (controle.equals(TERRENO)) {//terrain
            //é um combo com o model com as terrenos do cenario
            JComboBox cbTemp = new JComboBox(getOrdemControl().getTerrainComboModel());
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
        } else if (controle.equals(TROPA_TIPO_BASIC)) {//Tropa-Tipo-vanilla
            //é um combo com o model com os tipos de tropa BASICOS que a nacao pode recrutar
            JComboBox cbTemp;
            if (isAllSelected()) {
                cbTemp = new JComboBox(getActor().getTropaTipoComboModel(0));
            } else {
                cbTemp = new JComboBox(getActor().getTropaTipoComboModel(2));
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
        } else {
            cNovo = new JLabel(labels.getString("CONTROLE.NAO.IMPLEMENTADO"));
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
            } else if (comp instanceof SubTabDirecao) {
                SubTabDirecao par = (SubTabDirecao) comp;
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

    public static DialogTextArea showDialogPopup(String title, String text, Component relativeTo) {
        DialogTextArea localTextArea = new DialogTextArea(false);
        localTextArea.setText(text);
        localTextArea.setTitle(title);
        //configura jDialog
        if (relativeTo != null) {
            localTextArea.setLocationRelativeTo(relativeTo);
        }
        localTextArea.pack();
        localTextArea.setVisible(true);
        return localTextArea;
    }

    public static DialogHexView showDialogHexView(Component relativeTo) {
        DialogHexView hexViewDialog = new DialogHexView(false);
        //configura jDialog
        if (relativeTo != null) {
            hexViewDialog.setLocationRelativeTo(relativeTo);
        }
        hexViewDialog.pack();
        hexViewDialog.setVisible(true);
        return hexViewDialog;
    }

    public JLabelGradient getLabelGradient() {

        JLabel testLabel = new JLabel(labels.getString("STATUS.MESSAGES")) {
            @Override
            protected void paintComponent(Graphics g) {
                Color colorFinal = Color.RED;
                Color colorStart = Color.BLUE;

//                Point2D start = new Point2D.Float(0, 0);
//                Point2D end = new Point2D.Float(50, 50);
//                float[] dist = {0.0f, 0.2f, 1.0f};
//                Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
//                LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
                LinearGradientPaint linearGradientPaintOriginal = new LinearGradientPaint(
                        new Point(0, 25), new Point(0, getHeight()),
                        new float[]{0.240f, 0.250f}, new Color[]{
                            getBackground(), Color.RED});

                Point2D start = new Point2D.Float(0, 25);
                Point2D end = new Point2D.Float(0, getHeight());
                float[] dist = {0.0f, 0.2f, 1.0f};
                Color[] colors = {Color.RED, Color.YELLOW, Color.BLUE};
                LinearGradientPaint linearGradientPaint = new LinearGradientPaint(start, end, dist, colors);

                Graphics2D graphics2d = (Graphics2D) g.create();
                graphics2d.setPaint(linearGradientPaintOriginal);
                graphics2d.fill(new Rectangle(0, 0, getWidth(), getHeight()));
                graphics2d.dispose();

                super.paintComponent(g);
            }
        };
        JLabel labelGradient2 = new JLabel(labels.getString("STATUS.MESSAGES")) {
            @Override
            protected void paintComponent(Graphics g) {
                Color colorFinal = new Color(255, 1, 1);

                Graphics2D graphics2d = (Graphics2D) g.create();
                LinearGradientPaint lpg = new LinearGradientPaint(0, 0, 0, getHeight() / 2,
                        new float[]{0f, 0.3f, 1f},
                        new Color[]{new Color(0.8f, 0.8f, 1f), new Color(0.7f, 0.7f, 1f), new Color(0.6f, 0.6f, 1f)});
                graphics2d.setPaint(lpg);
                graphics2d.fillRect(getWidth() / 2, 0, getWidth(), getHeight());
                graphics2d.dispose();

                super.paintComponent(g);
            }
        };
        return new JLabelGradient();
//        return labelGradient2;
    }
}
