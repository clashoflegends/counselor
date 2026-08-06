package e2e;

import baseLib.BaseModel;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Comando;
import model.Ordem;
import model.PersonagemOrdem;

/**
 * Shared fixture plumbing for the happy-path tests.
 * <p>
 * The fixture is {@code test/resources/egf/game_88_20.rr.egf} - game 88, turn 20, active player
 * "alexandre", 40 owned actors, a 69-order scenario. It is deliberately an OLD file: it also keeps
 * the XStream backward-compatibility machinery ({@code readResolve}) under test, and it contains the
 * null-{@code getCodigo()} actors that {@code WorldFacadeCounselor.getActorsAll} guards against.
 * <p>
 * {@link WorldFacadeCounselor} is a singleton over a static world, so the world is loaded once and
 * each test takes a clean actor via {@link #freshPersonagem}.
 * <p>
 * <b>Shared-world hazard:</b> that world is shared by every test class in the JVM, and JUnit gives no
 * cross-class ordering guarantee - {@code ComandoAssemblyTest} wipes the orders of EVERY actor before
 * each of its methods. Nothing currently holds order state across methods within a class, which is why
 * this is safe today; a test that sets orders up in {@code @BeforeAll} and asserts on them later would
 * break here, so set orders up per method.
 */
public final class TestWorld {

    static final String FIXTURE = "/egf/game_88_20.rr.egf";

    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static boolean loaded = false;

    private TestWorld() {
    }

    /** Opens the fixture turn file once (what the Open dialog does) and returns the loaded facade. */
    public static synchronized WorldFacadeCounselor load() throws Exception {
        WorldFacadeCounselor wfc = WorldFacadeCounselor.getInstance();
        if (!loaded) {
            URL url = TestWorld.class.getResource(FIXTURE);
            if (url == null) {
                throw new IllegalStateException("Test fixture missing from test/resources" + FIXTURE);
            }
            wfc.doStart(new File(url.toURI()));
            loaded = true;
        }
        return wfc;
    }

    /** Every actor the active player may act with, in a stable order. */
    public static List<BaseModel> ownedActors(WorldFacadeCounselor wfc) {
        List<BaseModel> ret = new ArrayList<>();
        for (BaseModel actor : wfc.getActors()) {
            if (actor.getCodigo() != null && ordemFacade.isAtivo(wfc.getJogadorAtivo(), actor)) {
                ret.add(actor);
            }
        }
        ret.sort((a, b) -> a.getCodigo().compareTo(b.getCodigo()));
        return ret;
    }

    /**
     * A character of the active player with no orders on it. The world is shared across tests, so the
     * actor is wiped before it is handed out; callers must not assume which character they get.
     */
    public static BaseModel freshPersonagem(WorldFacadeCounselor wfc) {
        for (BaseModel actor : ownedActors(wfc)) {
            if (actor.isPersonagem()) {
                actor.remAcoes();
                return actor;
            }
        }
        throw new IllegalStateException("fixture has no character owned by the active player");
    }

    /**
     * An order from the loaded scenario, by its order NUMBER (the number players and the Judge know it
     * by, e.g. 1555). Note the scenario map is keyed by the per-scenario {@code getCodigo()} ("555"),
     * which is also what a saved orders file carries, so tests assert against {@code getCodigo()}.
     */
    public static Ordem ordem(WorldFacadeCounselor wfc, int numero) {
        for (Ordem ordem : wfc.getPartida().getCenario().getOrdens().values()) {
            if (ordem.getNumero() == numero) {
                return ordem;
            }
        }
        throw new IllegalStateException("fixture scenario has no order " + numero);
    }

    /** Builds a saved order the way {@code SubTabOrdem.getOrdemQuadro} does on the confirm button. */
    public static PersonagemOrdem buildOrder(BaseModel actor, Ordem ordem, String... parametros) {
        PersonagemOrdem po = new PersonagemOrdem();
        po.setNome(actor.getNome());
        po.setOrdem(ordem);
        po.setParametrosId(new ArrayList<>(Arrays.asList(parametros)));
        po.setParametrosDisplay(new ArrayList<>(Arrays.asList(parametros)));
        po.setUpdateTime(Instant.now());
        return po;
    }

    /**
     * Assembles the submission for a SINGLE actor, so a test can assert on one actor's orders without
     * depending on what other tests left on the shared world. The full multi-actor assembly that the
     * client actually runs is production code and is driven directly by
     * {@code control.ComandoAssemblyTest}.
     */
    public static Comando buildComando(WorldFacadeCounselor wfc, BaseModel actor) {
        Comando comando = new Comando();
        comando.setInfos(wfc.getPartida());
        for (int index = 0; index < ordemFacade.getOrdemMax(actor); index++) {
            if (ordemFacade.getOrdem(actor, index) != null) {
                comando.addComando(actor, ordemFacade.getOrdem(actor, index),
                        ordemFacade.getParametrosId(actor, index),
                        ordemFacade.getParametrosDisplay(actor, index),
                        ordemFacade.getTimeLastChange(actor, index));
            }
        }
        return comando;
    }
}
