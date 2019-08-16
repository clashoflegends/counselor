/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.facade;

import baseLib.BaseModel;
import business.BussinessException;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import control.MapaControler;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Cenario;
import model.Cidade;
import model.Comando;
import model.Exercito;
import model.Habilidade;
import model.Jogador;
import model.Mercado;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.PersistenceException;
import persistenceCommons.XmlManager;

/**
 *
 * @author gurgel
 */
public class WorldFacadeCounselor implements Serializable {

    private static final Log log = LogFactory.getLog(WorldFacadeCounselor.class);
    private static WorldFacadeCounselor instance;
    private final CenarioFacade cf = new CenarioFacade();
    private MapaControler mapaControler;
    private final AcaoFacade acaoFacade = new AcaoFacade();
//    private final Map<Nacao, List<PersonagemOrdem>> mapPersonagemOrdens = new HashMap<Nacao, List<PersonagemOrdem>>();
    private final Map<Nacao, Set<PersonagemOrdem>> mapPersonagemOrdens = new HashMap<Nacao, Set<PersonagemOrdem>>();

    private WorldFacadeCounselor() {
    }

    public static synchronized WorldFacadeCounselor getInstance() {
        if (WorldFacadeCounselor.instance == null) {
            WorldFacadeCounselor.instance = new WorldFacadeCounselor();
        }
        return WorldFacadeCounselor.instance;
    }


    /*
     * Client Only
     */
    public synchronized void doStart(File file) throws BussinessException {
        try {
            WorldManager.getInstance().doStart(file);
        } catch (PersistenceException ex) {
            throw new BussinessException(ex.getMessage());
        }
    }

    public void doSaveOrdens(Comando com, File selectedFile) throws BussinessException {
        try {
            XmlManager.getInstance().save(com, selectedFile);
        } catch (PersistenceException ex) {
            throw new BussinessException(ex.getMessage());
        } catch (NullPointerException ex) {
            throw new BussinessException(ex.getMessage());
        }

    }

    public String getCenarioNome() {
        return WorldManager.getInstance().getPartida().getCenario().getNome();
    }

    public int getCenarioArmyMoveMaxPoints() {
        return cf.getArmyMoveMaxPoints(WorldManager.getInstance().getPartida().getCenario());
    }

    public Cenario getCenario() {
        return WorldManager.getInstance().getPartida().getCenario();
    }

    public Jogador getJogadorAtivo() {
        return WorldManager.getInstance().getPartida().getJogadorAtivo();
    }

    public String getJogadorAtivoNome() {
        return WorldManager.getInstance().getPartida().getJogadorAtivo().getNome();
    }

    public String getNacoesJogadorAtivoNome() {
        String ret = "";
        for (Nacao nacao : WorldManager.getInstance().getNacoes().values()) {
            if (WorldManager.getInstance().getPartida().getJogadorAtivo() == nacao.getOwner()) {
                ret += nacao.getNome() + " ";
            }
        }
        return ret;
    }

    public List<Nacao> getNacoesJogadorAtivo() {
        List<Nacao> ret = new ArrayList<Nacao>();
        for (Nacao nacao : WorldManager.getInstance().getNacoes().values()) {
            if (WorldManager.getInstance().getPartida().getJogadorAtivo() == nacao.getOwner()) {
                ret.add(nacao);
            }
        }
        return ret;
    }

    public SortedMap<String, Nacao> getNacoes() {
        return WorldManager.getInstance().getNacoes();
    }

    public boolean isJogadorAtivoEliminado(Jogador jogadorAtivo) {
        boolean ret = true;
        for (Nacao nacao : WorldManager.getInstance().getNacoes().values()) {
            if (jogadorAtivo == nacao.getOwner() && nacao.isAtiva()) {
                ret = false;
                break;
            }
        }
        return ret;
    }

    public Partida getPartida() {
        return WorldManager.getInstance().getPartida();
    }

    public String getPartidaNome() {
        return WorldManager.getInstance().getPartida().getNome();
    }

    public Iterator<Personagem> getPersonagens() {
        return WorldManager.getInstance().getPersonagens().values().iterator();
    }

    public Collection<Cidade> getCidades() {
        return WorldManager.getInstance().getCidades().values();
    }

    public Collection<Exercito> getExercitos() {
        return WorldManager.getInstance().getExercitos().values();
    }

    public SortedMap<String, Personagem> listPersonagens() {
        return WorldManager.getInstance().getPersonagens();
    }

    public List<BaseModel> getActors() {
        final int initialCapacity = 25 + WorldManager.getInstance().getPersonagens().size()
                + WorldManager.getInstance().getCidades().size();
        final List<BaseModel> ret = new ArrayList<BaseModel>(initialCapacity);
        ret.addAll(WorldManager.getInstance().getNacoes().values());
        ret.addAll(WorldManager.getInstance().getCidades().values());
        ret.addAll(WorldManager.getInstance().getPersonagens().values());
        return ret;
    }

    public SortedMap<String, BaseModel> getActorsAll() {
        SortedMap<String, BaseModel> ret = new TreeMap<String, BaseModel>();
        for (BaseModel actor : WorldFacadeCounselor.getInstance().getActors()) {
            if (actor.getCodigo() == null) {
                //FIXME: Why cod would be null? Happening in game 88.
                continue;
            }
            ret.put(actor.getCodigo(), actor);
        }
//        ret.putAll(WorldManager.getInstance().getNacoes());
//        ret.putAll(WorldManager.getInstance().getCidades());
//        ret.putAll(WorldManager.getInstance().getPersonagens());
        return ret;
    }

    public SortedMap<String, Ordem> getOrdens() {
        return WorldManager.getInstance().getPartida().getCenario().getOrdens();
    }

    public int getOrdensQt(Personagem personagem) {
        return getCenario().getNumOrdensPers() + personagem.getOrdensExtraQt();
    }

    public int getOrdensQtMax() {
        return getCenario().getNumMaxOrdens();
    }

    public int getTurno() {
        return WorldManager.getInstance().getTurno();
    }

    public int getTurnoMax() {
        return WorldManager.getInstance().getTurnoMax();
    }

    public Ordem getOrdem(String ordemCodigo) {
        return WorldManager.getInstance().getOrdens().get(ordemCodigo);
    }

    public Mercado getMercado() {
        return WorldManager.getInstance().getPartida().getMercado();
    }

    public boolean isGameOver() {
        return WorldManager.getInstance().getPartida().isGameOver();
    }

    public boolean isSpells() {
        return WorldManager.getInstance().getCenario().hasHabilidade(";SLS;");
    }

    public boolean isStartupPackages() {
        return WorldManager.getInstance().getPartida().isStartupPackages();
    }

    public boolean isNationPackages() {
        return getTurno() == 0 && WorldManager.getInstance().getPartida().isNationPackages();
    }

    public int getNationPackagesLimit() {
        return WorldManager.getInstance().getPartida().getNationPackagesLimit();
    }

    public boolean hasDiplomat() {
        return cf.hasDiplomat(getCenario());
    }

    public boolean hasRogue() {
        return cf.hasRogue(getCenario());
    }

    public boolean hasWizard() {
        return cf.hasWizard(getCenario());
    }

    public boolean hasCombatCasualtiesTactics() {
        return cf.hasCombatCasualtiesTactics(getCenario());
    }

    public boolean hasResourceManagement() {
        return cf.hasResourceManagement(getCenario());
    }

    public boolean hasOrdensCidade() {
        return cf.hasOrdensCidade(getCenario());
    }

    public boolean hasCapitals() {
        return getCenario().hasHabilidade(";SNC;");
    }

    public SortedMap<String, Habilidade> getHabilidades(String habilidades) {
        SortedMap<String, Habilidade> ret = new TreeMap<String, Habilidade>();
        String[] temp = habilidades.trim().split(";");
        for (String cdHab : temp) {
            cdHab = ";" + cdHab.trim() + ";";
            if (!cdHab.equals(";;")) {
                ret.put(cdHab, WorldManager.getInstance().getPackages().get(cdHab));
            }
        }
        return ret;
    }

    public MapaControler getMapaControler() {
        return mapaControler;
    }

    public void setMapaControler(MapaControler mapaControler) {
        this.mapaControler = mapaControler;
    }

    public Map<Nacao, Set<PersonagemOrdem>> getMapPersonagemOrdens() {
        return mapPersonagemOrdens;
    }

    public Set<PersonagemOrdem> getMapPersonagemOrdens(Nacao nation) {
        if (!getMapPersonagemOrdens().containsKey(nation)) {
            getMapPersonagemOrdens().put(nation, new HashSet<PersonagemOrdem>());
        }
        return getMapPersonagemOrdens().get(nation);
    }

    public boolean addNacaoPersonagemOrdens(Nacao nation, PersonagemOrdem order) {
        return getMapPersonagemOrdens(nation).add(order);
    }

    public boolean remNacaoPersonagemOrdens(Nacao nation, PersonagemOrdem order) {
        return getMapPersonagemOrdens(nation).remove(order);
    }

    public int getNacaoOrderCost(Nacao nacao) {
        int cost = 0;
        for (PersonagemOrdem po : getMapPersonagemOrdens(nacao)) {
            cost += getOrderCost(po, nacao);
        }
        return cost;
    }

    public int getOrderCost(PersonagemOrdem po, Nacao nacao) {
        return acaoFacade.getCusto(po, nacao, this.getCenario(), this.getMercado());
    }

    public Nacao getNacao(String idNacao) {
        return WorldManager.getInstance().getNacao(idNacao);
    }

}
