package e2e;

import baseLib.BaseModel;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import model.Comando;
import model.ComandoDetail;
import model.Ordem;
import model.Partida;
import model.PersonagemOrdem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import persistenceCommons.XmlManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protects the happy path players depend on every turn: <b>open an EGF, enter an order, save the
 * orders file</b>. The upload leg is covered separately (it needs a network seam).
 * <p>
 * This runs entirely below the GUI, on the same classes the Swing layer drives:
 * {@link WorldFacadeCounselor#doStart} to open, {@link OrdemFacade#setOrdem} to enter an order (what
 * {@code SubTabOrdem} does on save), {@link Comando} to assemble the submission (what
 * {@code WorldControler.doSaveActorActions} does), {@link WorldFacadeCounselor#doSaveOrdens} to
 * write it, and {@link XmlManager} to read it back the way {@code WorldControler.setComando} does.
 * <p>
 * What it is meant to catch: an EGF that no longer opens, an order whose parameters do not survive
 * serialization, an actor/order identity that gets mangled on the way to the file, and a written
 * orders file that cannot be read back. All of those are silent-corruption failures that only
 * surface when the Judge rejects a turn.
 */
class OrderFileRoundTripTest {

    /** Costly single-parameter order in the fixture scenario: 1555 CriarAc, cost 2000, param "Nome". */
    private static final int ORDEM_CRIAR_AC = 1555;
    /** Costly single-parameter order in the fixture scenario: 1942 PsqFeit, cost 1000, param "Magia_Pre". */
    private static final int ORDEM_PSQ_FEIT = 1942;

    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static WorldFacadeCounselor wfc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
    }

    @Test
    void opensTheTurnFileWithAnActivePlayerAndItsScenario() {
        Partida partida = wfc.getPartida();
        assertNotNull(partida, "partida");
        assertNotNull(partida.getJogadorAtivo(), "no active player: every order path is gated on it");
        assertNotNull(partida.getCenario(), "cenario");
        assertFalse(partida.getCenario().getOrdens().isEmpty(), "scenario carries no orders");
        assertFalse(TestWorld.ownedActors(wfc).isEmpty(), "active player owns no actors");
    }

    @Test
    void enteringAnOrderPutsItOnTheActor() {
        BaseModel actor = TestWorld.freshPersonagem(wfc);
        Ordem ordem = TestWorld.ordem(wfc, ORDEM_CRIAR_AC);
        PersonagemOrdem po = TestWorld.buildOrder(actor, ordem, "Kingsguard");

        ordemFacade.setOrdem(actor, 0, po);

        assertEquals(1, actor.getAcaoSize());
        assertSame(po, actor.getAcao(0), "the actor must hold the very instance that was saved");
        assertEquals(ORDEM_CRIAR_AC, actor.getAcao(0).getOrdem().getNumero());
        assertEquals(List.of("Kingsguard"), actor.getAcao(0).getParametrosId());
    }

    @Test
    void savedOrderFileReloadsWithEveryOrderIntact() throws Exception {
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        Ordem criar = TestWorld.ordem(wfc, ORDEM_CRIAR_AC);
        Ordem pesquisa = TestWorld.ordem(wfc, ORDEM_PSQ_FEIT);
        ordemFacade.setOrdem(pc, 0, TestWorld.buildOrder(pc, criar, "Kingsguard"));
        ordemFacade.setOrdem(pc, 1, TestWorld.buildOrder(pc, pesquisa, "0101"));

        Comando saved = TestWorld.buildComando(wfc, pc);
        assertEquals(2, saved.size(), "both orders must reach the submission");

        File file = Files.createTempFile("orders_roundtrip", ".rc.egf").toFile();
        file.deleteOnExit();
        wfc.doSaveOrdens(saved, file);
        assertTrue(file.length() > 0, "orders file is empty");

        Object reread = XmlManager.getInstance().get(file);
        assertTrue(reread instanceof Comando, "orders file did not deserialize to a Comando");
        Comando loaded = (Comando) reread;

        assertEquals(saved.getPartidaId(), loaded.getPartidaId(), "game id");
        assertEquals(saved.getTurno(), loaded.getTurno(), "turn");
        assertEquals(saved.getJogadorId(), loaded.getJogadorId(), "player id");
        assertEquals(saved.size(), loaded.getOrdens().size(), "order count");

        ComandoDetail first = loaded.getOrdens().get(0);
        ComandoDetail second = loaded.getOrdens().get(1);
        assertEquals(pc.getCodigo(), first.getActorCodigo(), "actor code");
        assertEquals(pc.getNacao().getId() + "", first.getNacaoCodigo(), "nation code");
        assertEquals(criar.getCodigo(), first.getOrdemCodigo(), "order code");
        assertEquals(List.of("Kingsguard"), first.getParametroId(), "order parameters");
        assertEquals(pesquisa.getCodigo(), second.getOrdemCodigo(), "second order code");
        assertEquals(List.of("0101"), second.getParametroId(), "second order parameters");
    }

    @Test
    void orderParametersSurviveCharactersThatBreakDelimiters() throws Exception {
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        // Player-typed free text reaches the file verbatim; ';' and '|' are separators elsewhere in the
        // pipeline (habilidades, the deadline-sync wire), and accents exercise the encoding end to end.
        List<String> nasty = Arrays.asList("Ordem;com|pipe e acentuação");
        PersonagemOrdem po = new PersonagemOrdem();
        po.setNome(pc.getNome());
        po.setOrdem(TestWorld.ordem(wfc, ORDEM_CRIAR_AC));
        po.setParametrosId(nasty);
        po.setParametrosDisplay(nasty);
        po.setUpdateTime(Instant.now());
        ordemFacade.setOrdem(pc, 0, po);

        File file = Files.createTempFile("orders_encoding", ".rc.egf").toFile();
        file.deleteOnExit();
        wfc.doSaveOrdens(TestWorld.buildComando(wfc, pc), file);

        Comando loaded = (Comando) XmlManager.getInstance().get(file);
        assertEquals(nasty, loaded.getOrdens().get(0).getParametroId());
    }

    @Test
    void anActorWithNoOrdersContributesNothingToTheSubmission() {
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        assertEquals(0, TestWorld.buildComando(wfc, pc).size(),
                "an untouched actor must not produce an order line");
    }
}
