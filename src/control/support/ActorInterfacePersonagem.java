/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import baseLib.BaseModel;
import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.CidadeFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
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
import model.Personagem;
import model.PersonagemOrdem;
import model.Raca;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ActorInterfacePersonagem extends ActorInterface {

    private static final Log log = LogFactory.getLog(ActorInterfacePersonagem.class);
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private Personagem personagem;
    private final CidadeFacade cityFacade = new CidadeFacade();

    @Override
    public Nacao getNacao() {
        return personagemFacade.getNacao(getPersonagem());
    }

    @Override
    public Local getLocal() {
        return personagemFacade.getLocal(this.getPersonagem());
    }

    @Override
    public boolean isAtivo() {
        return personagemFacade.isAtivo(personagem);
    }

    @Override
    protected Personagem getPersonagem() {
        return personagem;
    }

    @Override
    protected BaseModel getActor() {
        return personagem;
    }

    public void setPersonagem(Personagem personagem) {
        this.personagem = personagem;
    }

    @Override
    public String getNome() {
        return personagemFacade.getNome(getPersonagem());
    }

    @Override
    public String getLocalCoordenadas() {
        return personagemFacade.getCoordenadas(getPersonagem());
    }

    @Override
    public ComboBoxModel getTropaTipoComboModel(int tipo) {
        //tipo=0 then ALL; =1 then by city=char race
        if (tipo == 0) {
            return CenarioConverter.getInstance().getTropaTipoComboModel();
        } else {
            Raca racaNacao = personagemFacade.getNacaoRaca(getPersonagem());
            Cidade city = personagemFacade.getCidade(getPersonagem());
            Raca racaCidade = cityFacade.getRaca(city);
            return CenarioConverter.getInstance().getTropaTipoComboModel(racaCidade, racaNacao, city);
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
        if (ordemSelecionada == null || getPersonagem() == null || ordemSelecionada.getComboId().equals("0")) {
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
        return personagemFacade.getExercitoViajando(getPersonagem());
    }

    @Override
    public GenericoComboBoxModel getOrdemComboModel(int ordemAtiva, boolean allOrders) {
        if (getPersonagem() == null) {
            return (null);
        } else {
            Ordem[] items = ordemFacade.getOrdensDisponiveis(WorldFacadeCounselor.getInstance().getOrdens(), getPersonagem(), ordemAtiva, allOrders, WorldFacadeCounselor.getInstance().isNationPackages());
            GenericoComboBoxModel model = new GenericoComboBoxModel(items);
            return model;
        }
    }

    @Override
    public ActorAction doOrderClear(int indexModelOrdem) {
        getDispatchManager().sendDispatchForChar(getNacao(), getPersonagem().getAcao(indexModelOrdem), null);
        setOrdem(indexModelOrdem, null);
        return ordemFacade.getActorActionBlank();
    }

    @Override
    public String[] doOrderSave(int indexModelOrdem, PersonagemOrdem po) {
        getDispatchManager().sendDispatchForChar(getNacao(), getPersonagem().getAcao(indexModelOrdem), po);
        //recupera os parametros da ordem
        //{Ordem, List parametroId, List ParametroDisplay}
        po.setNome(getPersonagem().getNome());
        return setOrdem(indexModelOrdem, po);
    }

    private String[] setOrdem(int index, PersonagemOrdem pOrdem) {
        ordemFacade.setOrdem(getPersonagem(), index, pOrdem);
        return ordemFacade.getOrdemDisplay(
                getPersonagem(), index,
                getCenario(),
                getJogadorAtivo());
    }

    @Override
    public PersonagemOrdem getPersonagemOrdem(int indexOrdem) {
        return ordemFacade.getPersonagemOrdem(getPersonagem(), indexOrdem);
    }

    @Override
    public String getParametroDisplay(int indexOrdem, int indexParametro) {
        return ordemFacade.getParametroDisplay(getPersonagem(), indexOrdem, indexParametro);
    }

    @Override
    public GenericoTableModel getOrdemTableModel() {
        if (getPersonagem() == null) {
            return (null);
        } else {
            GenericoTableModel ordemModel = getOrdemModel();
            return (ordemModel);
        }
    }

    @Override
    public int getAcaoMax() {
        return ordemFacade.getOrdemMax(getPersonagem(), getCenario());
    }

    @Override
    protected String[] getOrdemDisplay(int index) {
        return ordemFacade.getOrdemDisplay(getPersonagem(), index, getCenario(),
                WorldFacadeCounselor.getInstance().getJogadorAtivo());
    }
}
