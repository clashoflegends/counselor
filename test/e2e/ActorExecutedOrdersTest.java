package e2e;

import baseLib.BaseModel;
import control.facade.WorldFacadeCounselor;
import control.support.ActorInterface;
import control.support.ActorInterfaceFactory;
import control.services.LastTurnOrders;
import java.util.List;
import model.Cidade;
import model.Nacao;
import model.Personagem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Do it again" reads last turn's orders through the actor interface, so every actor type that can
 * carry them must surface them - characters, cities, hexes and nations alike. Which types actually
 * arrive populated depends on the scenario and on what the server recorded (GoT has no city orders at
 * all; WDO does), so the client must simply read whatever the turn file holds and disable the action
 * when a given actor has nothing.
 */
class ActorExecutedOrdersTest {

    private static final int ORDEM_A = 1555;

    private static WorldFacadeCounselor wfc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
    }

    @Test
    void aCharacterSurfacesItsExecutedOrders() {
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        pc.getAcaoExecutadas().clear();
        pc.addAcaoExecutada(TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));

        ActorInterface actor = ActorInterfaceFactory.getInstance().getActorInterface((Personagem) pc);

        assertEquals(1, actor.getOrdensExecutadas().size());
        assertTrue(LastTurnOrders.hasAny(actor.getOrdensExecutadas()));
    }

    @Test
    void aCitySurfacesItsExecutedOrders() {
        Cidade cidade = someCity();
        cidade.getAcaoExecutadas().clear();
        cidade.addAcaoExecutada(TestWorld.buildOrder(cidade, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));

        ActorInterface actor = ActorInterfaceFactory.getInstance().getActorInterface(cidade);

        assertEquals(1, actor.getOrdensExecutadas().size(),
                "a city's last-turn orders did not reach the actor interface");
        List<PersonagemOrdem> repeatable =
                LastTurnOrders.listRepeatable(actor.getOrdensExecutadas(), cidade.getNome(), List.of(ORDEM_A));
        assertEquals(1, repeatable.size());
        assertEquals(ORDEM_A, repeatable.get(0).getOrdem().getNumero());
    }

    @Test
    void aNationSurfacesItsExecutedOrders() {
        Nacao nacao = wfc.getJogadorAtivo().getNacoes().values().iterator().next();
        nacao.getAcaoExecutadas().clear();
        nacao.addAcaoExecutada(TestWorld.buildOrder(nacao, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));

        ActorInterface actor = ActorInterfaceFactory.getInstance().getActorInterface(nacao);

        assertEquals(1, actor.getOrdensExecutadas().size());
    }

    @Test
    void anActorWithNothingRecordedOffersNothingToRepeat() {
        Cidade cidade = someCity();
        cidade.getAcaoExecutadas().clear();

        ActorInterface actor = ActorInterfaceFactory.getInstance().getActorInterface(cidade);

        assertNotNull(actor.getOrdensExecutadas(), "must be empty, never null");
        assertFalse(LastTurnOrders.hasAny(actor.getOrdensExecutadas()),
                "the action must be unavailable rather than repeating nothing");
    }

    private static Cidade someCity() {
        for (BaseModel actor : TestWorld.ownedActors(wfc)) {
            if (actor instanceof Cidade) {
                return (Cidade) actor;
            }
        }
        throw new IllegalStateException("fixture has no city owned by the active player");
    }
}
