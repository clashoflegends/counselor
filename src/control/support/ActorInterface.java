/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import baseLib.BaseModel;
import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import business.facade.LocalFacade;
import business.facade.NacaoFacade;
import business.facade.OrdemFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import control.services.CenarioConverter;
import control.services.CidadeConverter;
import control.services.NacaoConverter;
import java.io.Serializable;
import javax.swing.ComboBoxModel;
import model.ActorAction;
import model.Cenario;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public abstract class ActorInterface extends ControlBase implements Serializable {

    private static final Log log = LogFactory.getLog(ActorInterface.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final LocalFacade localFacade = new LocalFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final OrdemFacade ordemFacade = new OrdemFacade();

    public boolean isPersonagem() {
        return false;
    }

    public boolean isCidade() {
        return false;
    }

    public ComboBoxModel getCidadeComboModel(int tipo) {
        return CidadeConverter.getCidadeComboModel(tipo, getNacao());
    }

    public GenericoComboBoxModel getNacaoComboModel() {
        return NacaoConverter.getNacaoComboModel(getNacao());
    }

    public GenericoComboBoxModel getNacaoAllyComboModel() {
        return NacaoConverter.getNacaoAllyComboModel(getNacao());
    }

    public GenericoComboBoxModel getNacaoNoEnemySwornComboModel() {
        return NacaoConverter.getNacaoNoEnemySwornComboModel(getNacao());
    }

    /**
     * 0 = Personagens 1 = Personagem-Comandante-Ex 2 = Personagem-Local 3 = Personagem-Nacao-Local 4 = Personagem-Local-NoNacao 5 = Personagem-Nacao
     * 5 = Personagem-local_self
     *
     * @param tipo
     * @param personagem
     * @return
     */
    public GenericoComboBoxModel getPersonagensComboModel(int tipo) {
        Personagem[] items = null;
        if (tipo == 0) {
            ListFactory listFactory = new ListFactory();
            items = nacaoFacade.listPersonagemNaoNacao(getNacao(), listFactory.listPersonagens());
        } else if (tipo == 1) {
            items = localFacade.listPersonagemLocal(getLocal(), getPersonagem(), 3);
        } else if (tipo == 2) {
            items = localFacade.listPersonagemLocal(getLocal(), getPersonagem(), 0);
        } else if (tipo == 3) {
            items = localFacade.listPersonagemLocal(getLocal(), getPersonagem(), 1);
        } else if (tipo == 4) {
            items = localFacade.listPersonagemLocal(getLocal(), getPersonagem(), 2);
        } else if (tipo == 5) {
            items = nacaoFacade.listPersonagemNacao(getNacao(), getPersonagem());
        } else if (tipo == 6) {
            items = localFacade.listPersonagemLocal(getLocal(), getPersonagem(), 4);
        } else if (tipo == 7) {
            items = localFacade.listPersonagemLocal(getLocal(), getNacao());
        } else {
            items = localFacade.listPersonagemLocal(getLocal(), getPersonagem(), 4);
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        if (tipo == 1) {
            //Add null as optional, meaning garrison
            GenericoComboObject comboObj = new GenericoComboObject(labels.getString("GUARNICAO"), "Guarnicao");
            model.addElement(comboObj);
        }
        return model;
    }

    public ComboBoxModel getProdutoComboModel(int tipo) {
        return CenarioConverter.getInstance().getProdutoComboModel(tipo);
    }

    protected Personagem getPersonagem() {
        return null;
    }

    protected Jogador getJogadorAtivo() {
        return WorldFacadeCounselor.getInstance().getJogadorAtivo();
    }

    protected Cenario getCenario() {
        return WorldFacadeCounselor.getInstance().getCenario();
    }

    public GenericoTableModel getOrdemModel() {
        GenericoTableModel model = new GenericoTableModel(
                new String[]{labels.getString("ACAO"), labels.getString("PARAMETRO"), labels.getString("TIPO")},
                getOrdemAsArray(),
                new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class
                });
        return model;
    }

    private String[][] getOrdemAsArray() {
        int ii = 0;
        String[][] ret = new String[getAcaoMax()][3];
        for (ii = 0; ii < ret.length; ii++) {
            ret[ii] = getOrdemDisplay(ii);
            ret[ii][1] = ret[ii][1].replace('[', ' ').replace(']', ' ').trim();
        }
        return (ret);
    }

    public int getOrdemComboIndex(int index, GenericoComboBoxModel ordemComboModel) {
        try {
            Ordem ordem = ordemFacade.getOrdem(getActor(), index);
            return ordemComboModel.getIndexByDisplay(ordem.getComboDisplay());
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public abstract Nacao getNacao();

    public abstract boolean isAtivo();

    public abstract int getAcaoMax();

    public abstract Local getLocal();

    public abstract String getNome();

    public abstract String getLocalCoordenadas();

    public abstract ComboBoxModel getTropaTipoComboModel(int tipo);

    public abstract ComboBoxModel getArtefatoComboModel(int tipo);

    public abstract ComboBoxModel getFeiticoComboModelByOrdem(Ordem ordemSelecionada, boolean allSelected);

    public abstract ComboBoxModel getFeiticoComboModel(int filtro);

    public abstract Exercito getExercito();

    public abstract GenericoComboBoxModel getOrdemComboModel(int ordemAtiva, boolean allOrders);

    public abstract ActorAction doOrderClear(int indexModelOrdem);

    public abstract String[] doOrderSave(int indexModelOrdem, PersonagemOrdem po);

    public abstract PersonagemOrdem getPersonagemOrdem(int indexOrdem);

    public abstract String getParametroDisplay(int indexOrdem, int indexParametro);

    public abstract GenericoTableModel getOrdemTableModel();

    protected abstract BaseModel getActor();

    protected abstract String[] getOrdemDisplay(int index);
}
