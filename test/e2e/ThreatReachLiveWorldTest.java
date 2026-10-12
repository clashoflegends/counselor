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
import model.Nacao;
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
                wfc.getLocais(), wfc.getPartida(),
                wfc.getCenarioArmyMoveMaxPoints());
    }

    /**
     * The whole pass over a real world: it must complete, and it must not take a visible pause.
     *
     * <b>Measured on a production turn file</b>, {@code game_917_2.fabriziopaese.rr.egf} - a 13-player
     * Game of Thrones game, 3,071 hexes, 23 armies of which 9 are hostile and 2 unscouted. A cold
     * compute took <b>71ms</b>, warm runs 34 to 37ms, and it marked 143 hexes of which 73 are inside
     * one turn. That is cheap enough to sit on the EGF-open path, which is where it actually runs:
     * {@code TabCidadesGui} builds its table model in its constructor and the Threat column asks for
     * the picture there.
     * <p>
     * The ceiling below is deliberately far above that. It is not a performance budget, it is a
     * tripwire for an accidental return to the empty-troop cost of 1 a hex, which floods the WHOLE
     * map for every army and would blow straight through it.
     */
    @Test
    void computingThreatReachOverARealWorldDoesNotThrow() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();

        final long start = System.currentTimeMillis();
        final Coverage found = reach(wfc);
        final long millis = System.currentTimeMillis() - start;

        assertNotNull(found);
        //not just "it ran": a regression that marks NOTHING would otherwise sail through this, and
        //silence is the one failure mode a threat display must never have
        assertFalse(found.isEmpty(), "a world with hostile armies in it must mark something");
        assertFalse(found.getHexesWithin(1).isEmpty(), "and some of it within one turn");
        assertTrue(found.getHexesWithin(ThreatReach.MAX_TURNS).size()
                >= found.getHexesWithin(1).size(), "the two-turn band contains the one-turn band");
        assertTrue(millis < 3000, "threat reach took " + millis + "ms over the whole world");
    }

    /**
     * <b>The oracle.</b> For an army we can count, the turn-1 band must be exactly the hexes
     * {@code calculateArmyRangeHexes} returns at one budget.
     *
     * The comparison is built to match {@code ThreatReach.flood}: the Judge's worst-case budget, the
     * evasive flag ignored, mountain crossing allowed. Armies holding BOTH land troops and ships are
     * skipped, because {@code ThreatReach} unions both mediums for those and a single-medium oracle
     * cannot speak to the union. The skip is counted and reported, so this cannot quietly become a
     * test of nothing.
     * <p>
     * <b>What it does and does not prove.</b> It pins the WRAPPER - run selection, the two-flood
     * band classification, the origin hex - against the shared flood on real terrain. It does not
     * pin the flood's own inputs, because it sets the same ones; those are pinned by
     * {@link ThreatReachTest}, where each is verified by being broken.
     */
    @Test
    void aVisibleArmysTurnOneBandMatchesTheJudgesOwnFlood() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();
        final int budget = ThreatReach.budgetPerTurn(wfc.getCenarioArmyMoveMaxPoints());
        //A player's own EGF holds his own armies in full and everyone else's as fog, so the active
        //player has no VISIBLE hostile army to check the wrapper against. This observer owns one
        //nation that belongs to no team in this game, so every army in the file reads hostile to it
        //and the armies that CAN be counted go through the same code path. The oracle is about the
        //flood, not about whose side anyone is on.
        //
        //It owns a nation rather than none: hostility is "every own nation is friendly or better",
        //so an observer owning nothing has nothing to be unfriendly with and clears everyone.
        final Nacao noTeam = new Nacao();
        noTeam.setCodigo("ZZZZ");
        noTeam.setNome("Outsider");
        noTeam.setTeamFlag("ZZZZ");
        final Jogador outsider = new Jogador();
        outsider.addNacao(noTeam);

        int checked = 0;
        int skippedMixed = 0;
        for (Exercito army : listFactory.listExercitos().values()) {
            if (army.getPelotoes().isEmpty() || exercitoFacade.getLocal(army) == null) {
                continue;
            }
            if (!exercitoFacade.getTipoTropasTerra(army).isEmpty()
                    && !exercitoFacade.getTipoTropasAgua(army).isEmpty()) {
                skippedMixed++;
                continue;
            }
            assertTrue(ThreatReach.isHostile(army, outsider), "an outsider is hostile to everyone");
            final Coverage one = ThreatReach.compute(new ArrayList<>(List.of(army)),
                    outsider, wfc.getLocais(), wfc.getPartida(),
                    wfc.getCenarioArmyMoveMaxPoints());
            assertEquals(oracleHexes(wfc, army, budget), one.getHexesWithin(1),
                    army.getNome() + ": the turn-1 band must be the Judge's own flood");
            checked++;
        }
        //a vacuous pass is the failure mode this test exists to avoid: TurnReviewLiveWorldTest was
        //written against this same fixture and found zero planned orders in it
        assertTrue(checked > 0, "fixture holds no single-medium army we can count; this proved "
                + "nothing (" + skippedMixed + " skipped for holding both troops and ships)");
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
        //the same worst case ThreatReach.flood takes: the evasive flag is last turn's ORDER and is
        //never believed, and mountain crossing is allowed because the client cannot see the rule
        movEx.setEvasivo(false);
        movEx.setMoveMountain(true);
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

    /**
     * The assumed roster respects terrain COST, not merely terrain permission.
     *
     * {@link #noLandThreatReachesAcrossWater} alone proves less than it looks:
     * {@code getCustoMovimentacaoTerra} rejects a water destination independently of price, so that
     * test passes even if the whole cost model collapsed back to 1 point a hex. This is the
     * assertion that notices - an unscouted army costed from a real roster must reach materially
     * less ground than the same army costed with an empty troop list, which is the defect this
     * class exists to close.
     */
    @Test
    void anAssumedRosterReachesFarLessGroundThanAnEmptyTroopListWould() throws Exception {
        final WorldFacadeCounselor wfc = TestWorld.load();
        final int budget = ThreatReach.budgetPerTurn(wfc.getCenarioArmyMoveMaxPoints());

        int checked = 0;
        for (Exercito army : listFactory.listExercitos().values()) {
            if (!army.getPelotoes().isEmpty() || exercitoFacade.getLocal(army) == null
                    || army.getTamanhoEsquadra() > 0) {
                continue;
            }
            final int assumed = ThreatReach.rangeOf(army, wfc.getLocais(), wfc.getCenario(),
                    budget).size();

            //the old behaviour: hand the shared cost model nothing and every hex costs 1
            final MovimentoExercito empty = new MovimentoExercito();
            empty.setOrigem(exercitoFacade.getLocal(army));
            empty.setPorAgua(false);
            empty.setComida(true);
            empty.setLimiteMovimento(budget);
            final int circle = exercitoFacade.calculateArmyRangeHexes(wfc.getLocais(), empty).size();

            assertTrue(assumed > 0, army.getNome() + " must still have a reach");
            //A quarter smaller, not half: the margin depends entirely on the terrain around the
            //army and on how fast the assumed roster is. On this fixture the measured ratio is
            //150 hexes against 251, i.e. 0.60. A collapse back to one point a hex would put it at
            //1.0, which is the thing being watched for.
            assertTrue(assumed * 4 < circle * 3, String.format(
                    "%s: assumed roster reaches %d hexes, the cost-1 circle %d - the roster is not "
                    + "being costed", army.getNome(), assumed, circle));
            checked++;
        }
        assertTrue(checked > 0, "fixture holds no unscouted land army; this proved nothing");
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
