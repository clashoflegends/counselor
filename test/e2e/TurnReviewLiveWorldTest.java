package e2e;

import baseLib.BaseModel;
import business.facade.CenarioFacade;
import business.facade.OrdemFacade;
import control.facade.WorldFacadeCounselor;
import control.services.TurnReview;
import control.services.TurnReview.Finding;
import control.services.TurnReview.Kind;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The review against a REAL turn file, which the hand-built tests cannot stand in for.
 *
 * {@link TurnReviewTest} proves the rules on actors built to order. This proves the pass survives the
 * shapes a real EGF actually contains - cities and nations alongside characters, allied and NPC
 * actors the player may not act with, characters with no hex, orders with missing or malformed
 * parameters - none of which a synthetic fixture thinks to produce. The dialog runs this on the EDT,
 * so an exception escaping here is a crash in front of a player.
 *
 * It is a SHAPE test, not a content test: the fixture is one game at one turn, and asserting a
 * particular number of findings would pin the fixture rather than the rules.
 */
class TurnReviewLiveWorldTest {

    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();

    private static List<Finding> review(WorldFacadeCounselor wfc) {
        return TurnReview.compute(wfc.getActors(), wfc.getJogadorAtivo(), wfc.getLocais(),
                wfc.listPersonagens().values(), Collections.emptyMap(),
                actor -> cenarioFacade.hasOrdens(wfc.getPartida(), actor),
                wfc.getNationPackagesLimit());
    }

    /** The whole pass over a real world, with nothing entered: it must complete and say something. */
    @Test
    void reviewingARealTurnFileDoesNotThrow() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();

        final List<Finding> found = review(wfc);

        assertNotNull(found);
        assertFalse(found.isEmpty(), "a turn with nothing entered must flag something");

        //Nothing is entered on a freshly opened turn, so every actor that HAS orders to give is
        //unplanned. The point is that it found all of them: this is the loop that used to report one.
        //
        //Not every owned actor qualifies, and that is deliberate rather than a miss. CenarioFacade
        //.hasOrdens gives characters orders always, cities only where the scenario grants them, and
        //nations only at turn 0 with packages - so on this fixture (game 88, turn 20) 22 of the 39
        //owned actors have nothing to plan and must not be nagged about. That is exactly what
        //doSaveActorActions did before the predicate was shared, which is the behaviour being pinned.
        int expected = 0;
        for (BaseModel actor : TestWorld.ownedActors(wfc)) {
            if (cenarioFacade.hasOrdens(wfc.getPartida(), actor)) {
                expected++;
            }
        }
        assertTrue(expected > 1, "fixture is useless for this if it has fewer than two such actors");
        assertEquals(expected, count(found, Kind.OPEN_SLOT) + count(found, Kind.OPEN_PACKAGE),
                "one row per actor with something to plan, not one row in total");
    }

    /** Every finding must be renderable: the dialog indexes these fields with no guards of its own. */
    @Test
    void everyFindingIsWellFormed() throws Exception {
        final List<Finding> found = review(TestWorld.load());

        for (Finding f : found) {
            assertNotNull(f.getKind(), "a finding with no kind cannot be rendered");
            assertNotNull(f.getLevel());
            assertNotNull(f.getArgs());
            for (String arg : f.getArgs()) {
                assertNotNull(arg, "a null argument reads as \"null\" in the player's sentence: " + f);
            }
            if (f.getKind() != Kind.CONVERGE) {
                assertNotNull(f.getActor(), "only a converging-hex row belongs to nobody: " + f);
            }
        }
    }

    /**
     * Another player's actors must never appear. An allied turn file can be autoloaded into the same
     * world, and their orders are read-only here, so a row about one is noise the player cannot act
     * on - and the same {@code isAtivo} gate is what keeps their orders out of the submission.
     */
    @Test
    void nobodyElsesActorsAreReviewed() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();

        for (Finding f : review(wfc)) {
            if (f.getActor() == null) {
                continue;
            }
            assertTrue(ordemFacade.isAtivo(wfc.getJogadorAtivo(), (BaseModel) f.getActor()),
                    "reviewed an actor the player cannot act with: " + f);
        }
    }

    /** Running it twice must give the same answer: the pass must not mutate the world it reads. */
    @Test
    void theReviewIsReadOnly() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();

        final int first = review(wfc).size();
        final int second = review(wfc).size();

        assertEquals(first, second, "the review changed the world it was reviewing");
    }

    private static int count(List<Finding> found, Kind kind) {
        int n = 0;
        for (Finding f : found) {
            if (f.getKind() == kind) {
                n++;
            }
        }
        return n;
    }
}
