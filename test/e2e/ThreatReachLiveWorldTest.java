package e2e;

import business.ArmyPath;
import business.MovimentoExercito;
import business.facade.ExercitoFacade;
import control.facade.WorldFacadeCounselor;
import control.services.ThreatReach;
import control.services.ThreatReach.Coverage;
import control.services.ThreatReach.Threat;
import persistence.local.ListFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ThreatReach} against a REAL turn file.
 *
 * The hand-built tests prove the rules on armies made to order. This proves two things they cannot.
 *
 * <b>The oracle.</b> {@code ExercitoFacade.calculateArmyRangeHexes} is the Judge's own flood
 * ({@code Ordem851MoveNpcArmy} calls it) and this class wraps it rather than replacing it, so for an
 * army whose composition is VISIBLE the two must agree hex for hex. Any difference is a bug in the
 * wrapper, and a synthetic fixture would only prove the wrapper agrees with itself.
 *
 * <b>The shapes a real EGF contains.</b> Armies with no hex, with no nation, with an empty troop map,
 * garrisons, fleets - none of which a fixture thinks to build.
 */
class ThreatReachLiveWorldTest {

    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final ListFactory listFactory = new ListFactory();

    private static Coverage reach(WorldFacadeCounselor wfc) {
        return ThreatReach.compute(listFactory.listExercitos().values(), wfc.getJogadorAtivo(),
                wfc.getLocais(), wfc.getCenario(),
                wfc.getCenarioArmyMoveMaxPoints());
    }

    /** The whole pass over a real world: it must complete, and it must not take a visible pause. */
    @Test
    void computingThreatReachOverARealWorldDoesNotThrow() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();

        final long start = System.currentTimeMillis();
        final Coverage found = reach(wfc);
        final long millis = System.currentTimeMillis() - start;

        assertNotNull(found);
        //the picture is built once per EGF load and cached, so this is generous on purpose: it is a
        //tripwire for an accidental return to the cost-1 circle, which floods the whole map per army
        assertTrue(millis < 20000, "threat reach took " + millis + "ms over the whole world");
    }

    /**
     * <b>The oracle.</b> For an army we can count, the turn-1 band must be exactly the hexes
     * {@code calculateArmyRangeHexes} returns at one budget, called the way
     * {@code MapaControler.printTagArmyRange} calls it - except for the budget, which is the Judge's
     * worst case rather than its plain-move value.
     */
    @Test
    void aVisibleArmysTurnOneBandMatchesTheJudgesOwnFlood() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();
        final int budget = ThreatReach.budgetPerTurn(wfc.getCenarioArmyMoveMaxPoints());
        //A player's own EGF holds his own armies in full and everyone else's as fog, so the active
        //player has no VISIBLE hostile army to check the wrapper against. An observer who owns
        //nothing is hostile to everything, which puts the one army in this file that can be counted
        //through the same code path. The oracle is about the flood, not about whose side it is on.
        final Jogador outsider = new Jogador();

        int checked = 0;
        for (Exercito army : listFactory.listExercitos().values()) {
            if (army.getPelotoes().isEmpty() || exercitoFacade.getLocal(army) == null) {
                continue;
            }
            assertTrue(ThreatReach.isHostile(army, outsider), "an outsider is hostile to everyone");
            final Coverage one = ThreatReach.compute(new ArrayList<>(List.of(army)),
                    outsider, wfc.getLocais(), wfc.getCenario(),
                    wfc.getCenarioArmyMoveMaxPoints());
            assertEquals(oracleHexes(wfc, army, budget), one.getHexesWithin(1),
                    army.getNome() + ": the turn-1 band must be the Judge's own flood");
            checked++;
        }
        //a vacuous pass is the failure mode this test exists to avoid: TurnReviewLiveWorldTest was
        //written against this same fixture and found zero planned orders in it
        assertTrue(checked > 0, "fixture holds no army we can count; this proved nothing");
    }

    /** {@code printTagArmyRange}'s own call, with the worst-case budget substituted. */
    private static Set<Local> oracleHexes(WorldFacadeCounselor wfc, Exercito army, int budget) {
        final Local local = exercitoFacade.getLocal(army);
        final boolean water = new business.facade.LocalFacade().isAgua(local);
        final MovimentoExercito movEx = new MovimentoExercito();
        final List<TipoTropa> troops = water
                ? exercitoFacade.getTipoTropasAgua(army) : exercitoFacade.getTipoTropasTerra(army);
        movEx.addTropasAll(troops);
        movEx.setComida(exercitoFacade.isComida(army));
        movEx.setEvasivo(army.isMovimentacaoEvasiva());
        movEx.setOrigem(local);
        movEx.setPorAgua(water);
        movEx.setLimiteMovimento(budget);
        final HashMap<Local, ArmyPath> range
                = exercitoFacade.calculateArmyRangeHexes(wfc.getLocais(), movEx);
        final Set<Local> ret = new HashSet<>(range.keySet());
        //ThreatReach marks the hex the army stands on; the raw flood drops it
        ret.add(local);
        return ret;
    }

    /**
     * <b>The fog fix, where it actually matters.</b> No unscouted army may reach a water hex on a
     * land flood.
     *
     * This is the defect the whole class exists to close: the shared cost model returns 1 for an
     * empty troop list BEFORE it checks whether the destination is water, so an unscouted land army
     * used to come back able to walk across the ocean at one point a hex.
     */
    @Test
    void noLandThreatReachesAcrossWater() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();
        final business.facade.LocalFacade localFacade = new business.facade.LocalFacade();

        final Coverage found = reach(wfc);
        final List<String> wet = new ArrayList<>();
        for (Local hex : wfc.getLocais().values()) {
            if (!localFacade.isAgua(hex)) {
                continue;
            }
            for (Threat threat : found.getThreats(hex)) {
                if (threat.getArmy().getTamanhoEsquadra() <= 0) {
                    wet.add(threat.getArmyName() + " -> " + hex.getCodigo());
                }
            }
        }
        assertTrue(wet.isEmpty(), "land-only forces reaching water: " + wet);
    }

    /** An assumed reach must name the troop it assumed, or the UI cannot say it is a guess. */
    @Test
    void everyAssumedThreatNamesItsAssumption() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();

        final Coverage found = reach(wfc);
        int assumed = 0;
        for (Local hex : wfc.getLocais().values()) {
            for (Threat threat : found.getThreats(hex)) {
                if (!threat.isAssumed()) {
                    continue;
                }
                assertFalse(threat.getAssumedTroop().isEmpty(),
                        "an assumed threat with no named troop reads as counted intelligence");
                assertTrue(threat.getArmy().getPelotoes().isEmpty(),
                        "only an army we cannot count may carry an assumption");
                assumed++;
            }
        }
        assertTrue(assumed > 0, "fixture holds no unscouted hostile army; this proved nothing");
    }
}
