/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import baseLib.BaseModel;
import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import control.services.ArtefatoConverter;
import control.services.CenarioConverter;
import control.services.FeiticoConverter;
import control.services.PersonagemConverter;
import javax.swing.ComboBoxModel;
import model.ActorAction;
import model.Cidade;
import model.Exercito;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.PersonagemOrdem;
import model.Raca;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ActorInterfaceCidade extends ActorInterface {

    private static final Log log = LogFactory.getLog(ActorInterfaceCidade.class);
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final OrdemFacade ordemFacade = new OrdemFacade();
    private Cidade cidade;

    @Override
    public Nacao getNacao() {
        return cidadeFacade.getNacao(getCidade());
    }

    @Override
    public Local getLocal() {
        return cidadeFacade.getLocal(getCidade());
    }

    @Override
    public boolean isAtivo() {
        return cidadeFacade.isAtivo(getCidade());
    }

    private Cidade getCidade() {
        return cidade;
    }

    @Override
    protected BaseModel getActor() {
        return cidade;
    }

    public void setCidade(Cidade cidade) {
        this.cidade = cidade;
    }

    @Override
    public String getNome() {
        return cidadeFacade.getNome(getCidade());
    }

    @Override
    public String getLocalCoordenadas() {
        return cidadeFacade.getCoordenadas(getCidade());
    }

    @Override
    public ComboBoxModel getTropaTipoComboModel(int tipo) {
        //tipo=0 then ALL; =1 then by city=char race
        if (tipo == 0) {
            return CenarioConverter.getInstance().getTropaTipoComboModel();
        } else {
            Raca racaNacao = cidadeFacade.getNacaoRaca(getCidade());
            Raca racaCidade = cidadeFacade.getRaca(getCidade());
            return CenarioConverter.getInstance().getTropaTipoComboModel(racaCidade, racaNacao);
        }
    }

    @Override
    public ComboBoxModel getArtefatoComboModel(int tipo) {
        return ArtefatoConverter.getArtefatoComboModel(tipo, getPersonagem());
    }

    /*
     * tipo = 0, todas as magias tipo = 1, todas que ele nao tem tipo = 2, todas
     * que ele nao tem, mas pode ter pq tem o pre-requisito para aprender
     */
    @Override
    public ComboBoxModel getFeiticoComboModel(int filtro) {
        return FeiticoConverter.getFeiticoComboModel(filtro, getPersonagem());
    }

    @Override
    public ComboBoxModel getFeiticoComboModelByOrdem(Ordem ordemSelecionada, boolean cbAll) {
        GenericoComboBoxModel model;
        if (ordemSelecionada == null || getCidade() == null || ordemSelecionada.getComboId().equals("0")) {
            model = PersonagemConverter.getFeiticoComboModel(null, null);
        } else if (cbAll) {
            model = PersonagemConverter.getFeiticoComboModel(ordemSelecionada, null);
        } else {
            model = PersonagemConverter.getFeiticoComboModel(ordemSelecionada, getPersonagem());
        }
        return model;
    }

    @Override
    public Exercito getExercito() {
        final ExercitoFacade ef = new ExercitoFacade();
        return ef.getGuarnicao(getCidade().getNacao(), getCidade().getLocal());
    }

    @Override
    public GenericoComboBoxModel getOrdemComboModel(int ordemAtiva, boolean allOrders) {
        if (getCidade() == null) {
            return (null);
        } else {
            Ordem[] items = ordemFacade.getOrdensDisponiveis(WorldFacadeCounselor.getInstance().getOrdens(), getCidade(), ordemAtiva, allOrders, WorldFacadeCounselor.getInstance().isNationPackages());
            GenericoComboBoxModel model = new GenericoComboBoxModel(items);
            return model;
        }
    }

    @Override
    public ActorAction doOrderClear(int indexModelOrdem) {
        getDispatchManager().sendDispatchForChar(getNacao(), getCidade().getAcao(indexModelOrdem), null);
        setOrdem(indexModelOrdem, null);
        return ordemFacade.getActorActionBlank();
    }

    @Override
    public String[] doOrderSave(int indexModelOrdem, PersonagemOrdem po) {
        //recupera os parametros da ordem
        //{Ordem, List parametroId, List ParametroDisplay}
        po.setNome(getCidade().getNome());
        getDispatchManager().sendDispatchForChar(getNacao(), getCidade().getAcao(indexModelOrdem), po);
        return setOrdem(indexModelOrdem, po);
    }

    private String[] setOrdem(int index, PersonagemOrdem pOrdem) {
        ordemFacade.setOrdem(getCidade(), index, pOrdem);
        return ordemFacade.getOrdemDisplay(
                getCidade(), index,
                WorldFacadeCounselor.getInstance().getCenario(),
                WorldFacadeCounselor.getInstance().getJogadorAtivo());
    }

    @Override
    public PersonagemOrdem getPersonagemOrdem(int indexOrdem) {
        return ordemFacade.getPersonagemOrdem(getCidade(), indexOrdem);
    }

    @Override
    public String getParametroDisplay(int indexOrdem, int indexParametro) {
        return ordemFacade.getParametroDisplay(getCidade(), indexOrdem, indexParametro);
    }

    @Override
    public GenericoTableModel getOrdemTableModel() {
        if (getCidade() == null) {
            return (null);
        } else {
            GenericoTableModel ordemModel = getOrdemModel();
            return (ordemModel);
        }
    }

    @Override
    public int getAcaoMax() {
        return ordemFacade.getOrdemMax(getCidade(), getCenario());
    }

    @Override
    protected String[] getOrdemDisplay(int index) {
        return ordemFacade.getOrdemDisplay(getCidade(), index, getCenario(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
    }
}
