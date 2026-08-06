package control;

import baseLib.BaseModel;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import e2e.TestWorld;
import java.util.ArrayList;
import java.util.List;
import model.Comando;
import model.ComandoDetail;
import model.Jogador;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the REAL assembly step of a submission - {@link WorldControler#doSaveActorActions} - rather
 * than a copy of it. Whatever this produces is what gets written to the orders file and handed to the
 * Judge, so the two properties that matter are: every order the player entered is in it, and nothing
 * that is not theirs ever is.
 * <p>
 * Lives in package {@code control} to reach the package-private assembly method; it needs no GUI,
 * which is why that method is static.
 */
class ComandoAssemblyTest {

    private static final int ORDEM_A = 1555;
    private static final int ORDEM_B = 1942;

    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static WorldFacadeCounselor wfc;
    private static Jogador jogadorAtivo;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
        jogadorAtivo = wfc.getJogadorAtivo();
    }

    @BeforeEach
    void clearEveryActorsOrders() {
        for (BaseModel actor : wfc.getActors()) {
            actor.remAcoes();
        }
    }

    @Test
    void carriesEveryOrderThePlayerEntered() {
        List<BaseModel> actors = twoOwnedCharacters();
        BaseModel first = actors.get(0);
        BaseModel second = actors.get(1);
        ordemFacade.setOrdem(first, 0, TestWorld.buildOrder(first, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        ordemFacade.setOrdem(second, 0, TestWorld.buildOrder(second, TestWorld.ordem(wfc, ORDEM_B), "0101"));

        Comando comando = assemble();

        assertEquals(2, comando.size(), "an entered order went missing from the submission");
        assertTrue(actorCodes(comando).contains(first.getCodigo()));
        assertTrue(actorCodes(comando).contains(second.getCodigo()));
    }

    @Test
    void keepsEveryOrderOfAnActorWithSeveral() {
        BaseModel pc = twoOwnedCharacters().get(0);
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
        ordemFacade.setOrdem(pc, 1, TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_B), "0101"));

        Comando comando = assemble();

        assertEquals(2, comando.size());
        assertEquals(2, actorCodes(comando).size());
    }

    /**
     * The fog-of-war guarantee: a submission must never carry orders for an actor the active player
     * does not own, however the order got onto that actor (an ally's turn file opened read-only, a
     * team order set loaded for reference).
     */
    @Test
    void neverCarriesAnotherPlayersOrders() {
        BaseModel foreign = foreignActor();
        ordemFacade.setOrdem(foreign, 0, TestWorld.buildOrder(foreign, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));

        Comando comando = assemble();

        assertFalse(actorCodes(comando).contains(foreign.getCodigo()),
                "assembly leaked an order for an actor the active player does not own");
        assertEquals(0, comando.size());
    }

    @Test
    void producesNothingWhenNoOrdersWereEntered() {
        assertEquals(0, assemble().size());
    }

    @Test
    void warnsWhenAnActorWasLeftWithoutOrders() {
        // Nothing entered anywhere, and the fixture's actors do have orders available to them, so the
        // caller must get a non-empty warning to show before the player submits a half-empty turn.
        assertFalse(WorldControler.doSaveActorActions(jogadorAtivo, new Comando()).isEmpty(),
                "no missing-orders warning for a turn with nothing entered");
    }

    @Test
    void doesNotWarnWhenEveryActorHasOrders() {
        for (BaseModel actor : wfc.getActors()) {
            if (!ordemFacade.isAtivo(jogadorAtivo, actor)) {
                continue;
            }
            for (int i = 0; i < ordemFacade.getOrdemMax(actor); i++) {
                ordemFacade.setOrdem(actor, i, TestWorld.buildOrder(actor, TestWorld.ordem(wfc, ORDEM_A), "Kingsguard"));
            }
        }

        Comando comando = new Comando();
        comando.setInfos(wfc.getPartida());
        String warning = WorldControler.doSaveActorActions(jogadorAtivo, comando);

        assertTrue(comando.size() > 0, "the fully-filled turn produced no orders");
        assertEquals("", warning, "warned about missing orders on a fully-filled turn: " + warning);
    }

    private static Comando assemble() {
        Comando comando = new Comando();
        comando.setInfos(wfc.getPartida());
        WorldControler.doSaveActorActions(jogadorAtivo, comando);
        return comando;
    }

    private static List<String> actorCodes(Comando comando) {
        List<String> ret = new ArrayList<>();
        for (ComandoDetail detail : comando.getOrdens()) {
            ret.add(detail.getActorCodigo());
        }
        return ret;
    }

    private static List<BaseModel> twoOwnedCharacters() {
        List<BaseModel> ret = new ArrayList<>();
        for (BaseModel actor : TestWorld.ownedActors(wfc)) {
            if (actor.isPersonagem() && ordemFacade.getOrdemMax(actor) > 1) {
                ret.add(actor);
            }
            if (ret.size() == 2) {
                return ret;
            }
        }
        throw new IllegalStateException("fixture has fewer than two usable characters");
    }

    /** An actor the active player does NOT own. */
    private static BaseModel foreignActor() {
        for (BaseModel actor : wfc.getActors()) {
            if (actor.getCodigo() != null && actor.getNacao() != null
                    && !ordemFacade.isAtivo(jogadorAtivo, actor)) {
                actor.remAcoes();
                return actor;
            }
        }
        throw new IllegalStateException("fixture has no actor outside the active player's nations");
    }
}
