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
import business.facade.NacaoFacade;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import control.services.ArtefatoConverter;
import control.services.CenarioConverter;
import control.services.FeiticoConverter;
import control.services.PersonagemConverter;
import javax.swing.ComboBoxModel;
import model.ActorAction;
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
public class ActorInterfaceNacao extends ActorInterface {

    private static final Log log = LogFactory.getLog(ActorInterfaceNacao.class);
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final OrdemFacade ordemFacade = new OrdemFacade();
    private Nacao nacao;

    @Override
    public Nacao getNacao() {
        return nacao;
    }

    public void setNacao(Nacao nacao) {
        this.nacao = nacao;
    }

    @Override
    public Local getLocal() {
        return nacaoFacade.getLocal(getNacao());
    }

    @Override
    public boolean isAtivo() {
        return nacaoFacade.isAtiva(getNacao());
    }

    @Override
    protected BaseModel getActor() {
        return getNacao();
    }

    @Override
    public String getNome() {
        return nacaoFacade.getNome(getNacao());
    }

    @Override
    public String getLocalCoordenadas() {
        return nacaoFacade.getCoordenadasCapital(getNacao());
    }

    @Override
    public ComboBoxModel getTropaTipoComboModel(int tipo) {
        //tipo=0 then ALL; =1 then by city=char race; =2 then basic troops that can be recruited at capital
        switch (tipo) {
            case 0:
                return CenarioConverter.getInstance().getTropaTipoComboModel();
            case 1:
            {
                Raca racaNacao = nacaoFacade.getRaca(getNacao());
                Raca racaCidade = cidadeFacade.getRaca(nacaoFacade.getCapital(getNacao()));
                return CenarioConverter.getInstance().getTropaTipoComboModel(racaCidade, racaNacao);
            }
            default:
            {
                Raca racaNacao = nacaoFacade.getRaca(getNacao());
                Raca racaCidade = cidadeFacade.getRaca(nacaoFacade.getCapital(getNacao()));
                return CenarioConverter.getInstance().getTropaTipoComboBasicModel(racaCidade, racaNacao);
            }
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
        if (ordemSelecionada == null || getNacao() == null || ordemSelecionada.getComboId().equals("0")) {
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
        return ef.getGuarnicao(getNacao(), nacaoFacade.getLocal(nacao));
    }

    @Override
    public GenericoComboBoxModel getOrdemComboModel(int ordemAtiva, boolean allOrders) {
        if (getNacao() == null) {
            return (null);
        } else {
            Ordem[] items = ordemFacade.getOrdensDisponiveis(WorldFacadeCounselor.getInstance().getOrdens(), getNacao(), ordemAtiva, allOrders, WorldFacadeCounselor.getInstance().isNationPackages());
            GenericoComboBoxModel model = new GenericoComboBoxModel(items);
            return model;
        }
    }

    @Override
    public ActorAction doOrderClear(int indexModelOrdem) {
        getDispatchManager().sendDispatchForChar(getNacao(), getNacao().getAcao(indexModelOrdem), null);
        setOrdem(indexModelOrdem, null);
        return ordemFacade.getActorActionBlank();
    }

    @Override
    public String[] doOrderSave(int indexModelOrdem, PersonagemOrdem po) {
        //recupera os parametros da ordem
        //{Ordem, List parametroId, List ParametroDisplay}
        po.setNome(getNacao().getNome());
        getDispatchManager().sendDispatchForChar(getNacao(), getNacao().getAcao(indexModelOrdem), po);
        return setOrdem(indexModelOrdem, po);
    }

    private String[] setOrdem(int index, PersonagemOrdem pOrdem) {
        ordemFacade.setOrdem(getNacao(), index, pOrdem);
        return ordemFacade.getOrdemDisplay(
                getNacao(), index,
                WorldFacadeCounselor.getInstance().getCenario(),
                WorldFacadeCounselor.getInstance().getJogadorAtivo());
    }

    @Override
    public PersonagemOrdem getPersonagemOrdem(int indexOrdem) {
        return ordemFacade.getPersonagemOrdem(getNacao(), indexOrdem);
    }

    @Override
    public String getParametroDisplay(int indexOrdem, int indexParametro) {
        return ordemFacade.getParametroDisplay(getNacao(), indexOrdem, indexParametro);
    }

    @Override
    public GenericoTableModel getOrdemTableModel() {
        if (getNacao() == null) {
            return (null);
        } else {
            GenericoTableModel ordemModel = getOrdemModel();
            return (ordemModel);
        }
    }

    @Override
    public int getAcaoMax() {
        return ordemFacade.getOrdemMax(getNacao(), getCenario());
    }

    @Override
    protected String[] getOrdemDisplay(int index) {
        return ordemFacade.getOrdemDisplay(getNacao(), index, getCenario(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
    }
}
