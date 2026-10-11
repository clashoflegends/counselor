package e2e;

import baseLib.BaseModel;
import control.services.TurnReview;
import control.services.TurnReview.Finding;
import control.services.TurnReview.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Cidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Personagem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules behind "Review turn", driven by hand-built actors.
 *
 * <b>The rendezvous check is the one with teeth, and the one most at risk of being "simplified"</b>
 * back into something that looks equivalent and is not. MovUnir means "move to hex X, then join
 * character Y", and the Judge refuses the join unless the two are on the same hex when the order runs.
 * Measured across games 850 to 907, the destination equalled the target's END-of-turn hex in 15,448 of
 * 15,461 successes but in only 317 of 1,549 could-not-attempt failures. Comparing against the target's
 * CURRENT hex instead would have flagged about 6,400 successful orders, because walking to a
 * rendezvous is ordinary play - {@link #noFindingWhenTheTargetIsWalkingToTheDestination} is the test
 * that fails if anyone tries it, and it is written around an ARMY commander with a direction move
 * because that is 98% of the real population.
 *
 * Synthetic rather than the shared {@code TestWorld} fixture, and for the same reason
 * {@code MoveConvergenceTest} is: the interesting states (a target walking somewhere, a passenger with
 * no orders) only exist if they are constructed. {@code compute} takes its world as parameters
 * precisely so that is possible.
 */
class TurnReviewTest {

    // ----- the rendezvous check -----

    /** The reported waste: you walk to a hex the person you meant to join is not going to. */
    @Test
    void rendezvousIsFlaggedWhenTheTargetWillBeSomewhereElse() {
        final Fixture f = new Fixture();
        final Personagem target = f.done(f.plain("Jon", "0707"));
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        final List<Finding> found = f.run(actor, target);

        assertEquals(1, count(found, Kind.RENDEZVOUS), "the two never meet, so the join cannot happen");
        final Finding f1 = first(found, Kind.RENDEZVOUS);
        assertEquals(actor, f1.getActor(), "the finding belongs to the character who spent the order");
        assertEquals(TurnReview.Level.WARN, f1.getLevel());
        assertEquals(0, f1.getSlot(), "and names the slot it came from");
    }

    /** The target is standing on the destination and not moving: the commonest good case. */
    @Test
    void noFindingWhenTheTargetAlreadyStandsOnTheDestination() {
        final Fixture f = new Fixture();
        final Personagem target = f.done(f.plain("Jon", "0505"));
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        assertEquals(0, count(f.run(actor, target), Kind.RENDEZVOUS));
    }

    /**
     * THE regression test. The target is an army commander walking to the destination by DIRECTIONS,
     * which is what 98% of real MovUnir targets are. It is not there now and it will be there then, so
     * there is nothing to warn about - and any version of this check that compares against the
     * target's current hex fails here.
     */
    @Test
    void noFindingWhenTheTargetIsWalkingToTheDestination() {
        final Fixture f = new Fixture();
        final Personagem target = f.armyMover("Jon", "0405", "E;E"); // 0405 -> 0505 -> 0605
        final Personagem actor = f.joiner("Tyrion", "0101", "0605", target);

        assertEquals(0, count(f.run(actor, target), Kind.RENDEZVOUS),
                "walking to the rendezvous is ordinary play, not a mistake");
    }

    /** ...and the same walk ending one hex short IS a mistake, which is the control for the above. */
    @Test
    void aTargetWhoseWalkEndsShortOfTheDestinationIsFlagged() {
        final Fixture f = new Fixture();
        final Personagem target = f.armyMover("Jon", "0405", "E"); // stops at 0505
        final Personagem actor = f.joiner("Tyrion", "0101", "0605", target);

        assertEquals(1, count(f.run(actor, target), Kind.RENDEZVOUS));
    }

    /** A target riding with someone else goes where the leader goes, as Ordem755JoinLeader resolves it. */
    @Test
    void aTargetTravellingWithALeaderResolvesThroughTheLeader() {
        final Fixture f = new Fixture();
        final Personagem leader = f.armyMover("Jeor", "0405", "E"); // ends 0505
        final Personagem target = f.plain("Jon", "0405");
        target.setLider(leader);
        f.give(target, f.idleOrder());                              // an order, but not a move
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        assertEquals(0, count(f.run(actor, target, leader), Kind.RENDEZVOUS),
                "the passenger ends where the leader ends, which is the destination");
    }

    /**
     * The passenger's OWN move wins over the leader's, and it must be found in whatever slot it sits
     * in.
     *
     * {@code PersonagemFacade.getLocalDestination} keeps its leader branch inside its loop over
     * orders, so it answers with the leader's destination the moment it meets any non-movement order
     * and never reads a move in a later slot. {@code executa810} has an explicit
     * {@code isViajandoCom} branch that takes a passenger out of the group, so the leader's
     * destination is the wrong answer here.
     */
    @Test
    void aPassengersOwnMoveInALaterSlotBeatsTheLeader() {
        final Fixture f = new Fixture();
        final Personagem leader = f.armyMover("Jeor", "0405", "E");  // leader ends 0505
        final Personagem target = f.plain("Jon", "0405");
        target.setLider(leader);
        f.give(target, f.idleOrder());                                // slot 0: not a move
        f.giveMove(target, "0707");                                   // slot 1: their own move
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        assertEquals(1, count(f.run(actor, target, leader), Kind.RENDEZVOUS),
                "they leave the group and walk to 0707; the leader's 0505 is not where they land");
    }

    /**
     * A passenger holding NO orders is skipped rather than guessed at.
     *
     * {@code PersonagemFacade.getLocalDestination} keeps its leader branch inside the loop over the
     * character's orders, so with no orders it answers with the passenger's own hex. That would read
     * as a mismatch when the truth is simply unknown here, and a plausible-looking wrong answer is
     * worse than no row at all.
     */
    @Test
    void aTargetWithNoOrdersOfItsOwnStillFollowsItsLeader() {
        final Fixture f = new Fixture();
        final Personagem leader = f.armyMover("Jeor", "0405", "E");  // ends 0505
        final Personagem target = f.plain("Jon", "0909");
        target.setLider(leader);

        assertEquals(0, count(f.run(actor(f, target), target, leader), Kind.RENDEZVOUS),
                "a passenger goes where it is carried, wherever it is standing now");

        final Fixture g = new Fixture();
        final Personagem elsewhere = g.armyMover("Jeor", "0405", "SE"); // ends somewhere else
        final Personagem rider = g.plain("Jon", "0909");
        rider.setLider(elsewhere);
        assertEquals(1, count(g.run(actor(g, rider), rider, elsewhere), Kind.RENDEZVOUS),
                "and if the leader is going elsewhere, so is the passenger");
    }

    private static Personagem actor(Fixture f, Personagem target) {
        return f.joiner("Tyrion", "0101", "0505", target);
    }

    // ----- the two shapes that produced confident false warnings -----

    /**
     * A target still being PLANNED is not a target standing still.
     *
     * The commonest way to meet a target with no orders is that the player has not got to them yet -
     * plan the escort before the leader and the old rule warned about a rendezvous the player was in
     * the middle of arranging, in the same list that said the leader had an open slot. In a SUBMITTED
     * turn "no orders" really does mean "not moving" (42 of 45 successes, 1 of 161 failures), which
     * is why the current hex is trusted the moment the target has nothing left to plan.
     */
    @Test
    void aTargetThePlayerHasNotPlannedYetIsNotJudged() {
        final Fixture f = new Fixture();
        final Personagem target = f.plain("Jon", "0707");   // no orders, every slot still open
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        assertEquals(0, count(f.run(actor, target), Kind.RENDEZVOUS),
                "they have not been given their orders yet, so there is nothing to disagree with");

        f.done(target);
        assertEquals(1, count(f.run(actor, target), Kind.RENDEZVOUS),
                "control: once they are fully planned and staying put, the mismatch is real");
    }

    /**
     * {@code Bloq} is typed {@code Mov} and takes a {@code Direcao}, but {@code executa840} only sets
     * a blockade facing - the character does not move. Reading it as a move puts a blockading
     * commander one hex from where they are standing, and a blockading commander is exactly who a
     * player marches out to join.
     */
    @Test
    void aBlockadingTargetHasNotMovedAnywhere() {
        final Fixture f = new Fixture();
        final Personagem target = f.blockader("Jon", "0505", "E");
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        assertEquals(0, count(f.run(actor, target), Kind.RENDEZVOUS),
                "Bloq sets a facing, not a destination: the join at 0505 succeeds");
    }

    /**
     * The check is gated on parameter SHAPE, not on an order code. A coordinate followed by an
     * own-nation character is unique to MovUnir across the whole catalogue, which also means the
     * direction-parameterised variant excludes itself with no code list to maintain.
     */
    @Test
    void aDirectionParameterisedVariantIsSkipped() {
        final Fixture f = new Fixture();
        final Personagem target = f.plain("Jon", "0707");
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);
        actor.getAcoes().get(0).getOrdem()
                .setParametrosIde(new String[]{"Direcao_Ex", "Personagem_Nacao"});

        assertEquals(0, count(f.run(actor, target), Kind.RENDEZVOUS));
    }

    /** The character combo is editable, so the parameter can name nobody. Say nothing, do not throw. */
    @Test
    void anUnresolvableTargetIsSkipped() {
        final Fixture f = new Fixture();
        final Personagem target = f.plain("Jon", "0707");
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);
        actor.getAcoes().get(0).getParametrosId().set(1, "zzzzz");

        assertEquals(0, count(f.run(actor, target), Kind.RENDEZVOUS));
    }

    /**
     * An ident is RELEASED when its holder dies and reused inside the same game (KI-051), so a
     * world-wide search can answer with a corpse. The living holder is the one the player picked.
     */
    @Test
    void aDeadHolderOfAReusedIdentIsNotMistakenForTheTarget() {
        final Fixture f = new Fixture();
        final Personagem dead = f.plain("Jon", "0909");   // registered FIRST, so order cannot save us
        dead.setVida(0);
        final Personagem target = f.done(f.plain("Jon", "0505")); // same ident, alive, where we go
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", target);

        assertEquals(0, count(f.run(actor, target, dead), Kind.RENDEZVOUS),
                "the dead holder of a reused ident must not decide where the living one is");
    }

    /**
     * Joining an ALLY is legal (Ordem755JoinLeader allows it) and the picker's combo is editable, so
     * the parameter can name a character of another nation. That is 161 successful and 208 failed
     * MovUnir orders in the measured corpus, so the check follows them rather than going quiet.
     */
    @Test
    void anAlliedCharacterIsResolvedWhenTheIdentIsUnambiguous() {
        final Fixture f = new Fixture();
        final Personagem ally = f.done(f.foreign("Davos", "0909"));
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", ally);

        assertEquals(1, count(f.run(actor, ally), Kind.RENDEZVOUS),
                "an ally standing somewhere else is still a rendezvous that will not happen");
    }

    /**
     * ...but the widened search refuses to guess. Two living holders of one ident and the check says
     * nothing, because naming the wrong character is worse than naming none.
     */
    @Test
    void anAmbiguousIdentOutsideMyNationResolvesToNobody() {
        final Fixture f = new Fixture();
        final Personagem ally = f.done(f.foreign("Davos", "0909"));
        final Personagem twin = f.done(f.foreign("Davos", "0808"));
        final Personagem actor = f.joiner("Tyrion", "0101", "0505", ally);

        assertEquals(0, count(f.run(actor, ally, twin), Kind.RENDEZVOUS),
                "two living holders of one ident must not be resolved to either");
    }

    // ----- the ;SCC; growth cap -----

    /**
     * A city that cannot grow is flagged - but ONLY where the rule exists.
     *
     * {@code CidadeFacade.isCheckCitySizeCapToUpgrade} is pure geometry: it measures the distance to
     * the nearest big city and never looks at whether {@code ;SCC;} is switched on. Every other
     * caller gates it on {@code WorldFacadeCounselor.hasCitySizeCap} for that reason, and this check
     * shipped without the gate, so in any game WITHOUT the rule a perfectly legal upgrade was told
     * "the Judge will refuse this order". A false warning in the one row style a player acts on.
     */
    @Test
    void theGrowthCapIsOnlyCheckedWhereTheRuleIsSwitchedOn() {
        final Fixture f = new Fixture();
        final Cidade city = f.city("Winterfell", "0505", 3, false);
        f.city("White Harbor", "0605", 4, false);      // a size-4 neighbour one hex away caps it
        final Cidade actor = f.improveCity(city);

        f.sizeCapOn = true;
        assertEquals(1, count(f.runActors(actor), Kind.SIZE_CAP), "control: with ;SCC; it IS capped");

        f.sizeCapOn = false;
        assertEquals(0, count(f.runActors(actor), Kind.SIZE_CAP),
                "without ;SCC; nothing caps anything, and saying otherwise is a lie");
    }

    /** With the rule on but nothing in range, there is nothing to say. */
    @Test
    void aCityWithNoBigNeighbourIsNotFlagged() {
        final Fixture f = new Fixture();
        final Cidade city = f.city("Winterfell", "0505", 3, false);
        f.city("White Harbor", "0101", 4, false);      // far away
        final Cidade actor = f.improveCity(city);

        assertEquals(0, count(f.runActors(actor), Kind.SIZE_CAP));
    }

    // ----- the predicate the SAVE PATH shares -----

    /**
     * {@code hasOpenSlot} is the one definition of "still unplanned", and {@code doSaveActorActions}
     * calls it, so these four branches are the submit path's behaviour as well as the review's.
     * They are pinned here because that equivalence was a refactor, not a rewrite, and a change to
     * any of them changes what a player is told before sending a turn.
     */
    @Test
    void theSharedOpenSlotPredicatePinsItsFourBranches() {
        final Fixture f = new Fixture();

        final Personagem half = f.plain("Ned", "0101");
        f.give(half, f.idleOrder());                       // 1 of a character's 2 slots
        assertTrue(TurnReview.hasOpenSlot(half, a -> true, 0), "a character with a free slot");

        final Personagem full = f.plain("Robb", "0102");
        f.give(full, f.idleOrder());
        f.give(full, f.idleOrder());
        assertFalse(TurnReview.hasOpenSlot(full, a -> true, 0), "a character with every slot used");

        final Personagem idle = f.plain("Arya", "0103");
        assertTrue(TurnReview.hasOpenSlot(idle, a -> true, 0), "no orders, and the scenario offers some");
        assertFalse(TurnReview.hasOpenSlot(idle, a -> false, 0), "no orders, and none to give it");

        //A nation counts POINTS, not slots: an empty slot with the budget already spent is finished.
        final Nacao nation = new Nacao();
        nation.setNome("Stark");
        nation.setCodigo("Stark");
        nation.setAcao(0, f.idleOrder());                  // a nation has 3 slots, so 2 are free
        assertTrue(TurnReview.hasOpenSlot(nation, a -> true, 1), "budget left over");
        assertFalse(TurnReview.hasOpenSlot(nation, a -> true, 0), "budget spent, so nothing to plan");
    }

    // ----- completeness -----

    /**
     * The reason the dialog exists at all: {@code doSaveActorActions} overwrites its message every
     * iteration, so with three actors left half-planned the player was told about one of them.
     */
    @Test
    void everyActorWithAnOpenSlotIsListed() {
        final Fixture f = new Fixture();
        final Personagem a = f.plain("Ned", "0101");
        final Personagem b = f.plain("Robb", "0102");
        final Personagem c = f.plain("Arya", "0103");
        f.give(a, f.idleOrder());   // 1 of 2 slots used
        f.give(b, f.idleOrder());
        f.give(c, f.idleOrder());

        assertEquals(3, count(f.run(a, b, c), Kind.OPEN_SLOT), "all three, not just the last one");
    }

    /** An actor the scenario offers no orders to is not "unplanned"; it has nothing to plan. */
    @Test
    void anActorWithNoOrdersAvailableIsNotFlagged() {
        final Fixture f = new Fixture();
        final Personagem idle = f.plain("Hodor", "0101");

        f.offersOrders = false;
        assertEquals(0, count(f.run(idle), Kind.OPEN_SLOT));
        f.offersOrders = true;
        assertEquals(1, count(f.run(idle), Kind.OPEN_SLOT), "control: it IS flagged when orders exist");
    }

    // ----- convergence and robustness -----

    /** Converging hexes arrive from the map overlay's cached picture and become plain rows. */
    @Test
    void convergingHexesBecomeRows() {
        final Fixture f = new Fixture();
        f.converging.put(f.hex("0505"), Arrays.asList("Ned", "Davos"));

        final List<Finding> found = f.run();
        assertEquals(1, count(found, Kind.CONVERGE));
        assertEquals(TurnReview.Level.INFO, first(found, Kind.CONVERGE).getLevel(),
                "two players converging on purpose is legitimate, so this is a nudge not a warning");
    }

    /** No input, no findings, and never a null for the dialog to guard. */
    @Test
    void nullInputsYieldAnEmptyList() {
        assertTrue(TurnReview.compute(null, null, null, null, null, null, 0, true).isEmpty());
    }

    // ----- helpers -----

    private static int count(List<Finding> found, Kind kind) {
        int n = 0;
        for (Finding f : found) {
            if (f.getKind() == kind) {
                n++;
            }
        }
        return n;
    }

    private static Finding first(List<Finding> found, Kind kind) {
        for (Finding f : found) {
            if (f.getKind() == kind) {
                return f;
            }
        }
        throw new AssertionError("no finding of kind " + kind);
    }

    /** A throwaway world with its own hexes, so nothing registers into the shared fixture. */
    private static final class Fixture {

        private final SortedMap<String, Local> locais = new TreeMap<>();
        private final Map<Local, List<String>> converging = new LinkedHashMap<>();
        private final Jogador me = new Jogador();
        private final Nacao nation = new Nacao();
        private boolean offersOrders = true;
        private int limit;
        private boolean sizeCapOn = true;

        Fixture() {
            me.setNome("me");
            me.setCodigo("me");
            nation.setNome("Stark");
            nation.setCodigo("Stark");
            nation.setOwner(me);
            me.addNacao(nation);
            //A real grid, not just the hexes the test names. A direction path is walked hex by hex
            //through locais, so a missing INTERMEDIATE hex makes the walk resolve to null and the
            //check skips - which reads as "no finding" and passes a test that proved nothing.
            for (int col = 1; col <= 10; col++) {
                for (int row = 1; row <= 10; row++) {
                    hex(String.format("%02d%02d", col, row));
                }
            }
        }

        Local hex(String coord) {
            Local l = locais.get(coord);
            if (l == null) {
                l = new Local();
                l.setCodigo(coord);
                l.setNome(coord);
                locais.put(coord, l);
            }
            return l;
        }

        /** Like {@link #run}, for actors that are not characters. */
        List<Finding> runActors(BaseModel... actors) {
            final List<BaseModel> list = new ArrayList<>(Arrays.asList(actors));
            return TurnReview.compute(list, me, locais, Collections.<Personagem>emptyList(),
                    converging, actor -> offersOrders, limit, sizeCapOn);
        }

        /** A city of my nation on a hex, which is what the size-cap rule measures between. */
        Cidade city(String name, String at, int size, boolean capital) {
            final Cidade c = new Cidade();
            c.setNome(name);
            c.setCodigo(at);
            c.setNacao(nation);
            c.setTamanho(size);
            c.setCapital(capital);
            c.setLocal(hex(at));
            hex(at).setCidade(c);
            return c;
        }

        /** The city, carrying an Improve City Size order in slot 0. */
        Cidade improveCity(Cidade city) {
            final Ordem o = new Ordem();
            o.setNome("AmplCP");
            o.setCodigo("550");          // one of OrdemJudgeFactory's improve-city codes
            o.setTipo("Cid");
            o.setParametrosIde(new String[]{"None"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(o);
            po.setParametrosId(new ArrayList<>(Collections.singletonList(" ")));
            city.setAcao(0, po);
            return city;
        }

        List<Finding> run(Personagem... actors) {
            final List<BaseModel> list = new ArrayList<>(Arrays.asList(actors));
            final List<Personagem> world = new ArrayList<>(Arrays.asList(actors));
            return TurnReview.compute(list, me, locais, world, converging,
                    actor -> offersOrders, limit, sizeCapOn);
        }

        /**
         * Fill every slot, so the review stops treating the character as still being planned.
         * A character has 2 slots ({@code Personagem.getOrdensQt}).
         */
        Personagem done(Personagem p) {
            while (p.getAcaoSize() < 2) {
                give(p, idleOrder());
            }
            return p;
        }

        /**
         * Bloq: typed {@code Mov} and parameterised by a {@code Direcao}, but {@code executa840} only
         * sets a blockade facing. The character does not go anywhere.
         */
        Personagem blockader(String name, String at, String facing) {
            final Personagem p = plain(name, at);
            final Ordem o = new Ordem();
            o.setNome("Bloq");
            o.setCodigo("840");
            o.setTipo("Mov");
            o.setParametrosIde(new String[]{"Direcao"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(o);
            po.setParametrosId(new ArrayList<>(Collections.singletonList(facing)));
            p.setAcao(0, po);
            give(p, idleOrder());   // slot 1, so it is done planning
            return p;
        }

        /** A character of SOMEBODY ELSE'S nation, which the order picker never offered. */
        Personagem foreign(String name, String at) {
            final Nacao theirs = new Nacao();
            theirs.setNome("Baratheon");
            theirs.setCodigo("Baratheon");
            final Personagem p = new Personagem();
            p.setNome(name);
            p.setCodigo(name.substring(0, Math.min(5, name.length())));
            p.setNacao(theirs);
            p.setLocal(hex(at));
            return p;
        }

        Personagem plain(String name, String at) {
            final Personagem p = new Personagem();
            p.setNome(name);
            p.setCodigo(name.substring(0, Math.min(5, name.length())));
            p.setNacao(nation);
            p.setLocal(hex(at));
            return p;
        }

        /** A plain coordinate move into the character's next free slot. */
        void giveMove(Personagem p, String to) {
            final Ordem o = new Ordem();
            o.setNome("MovPers");
            o.setCodigo("810");
            o.setTipo("Mov");
            o.setParametrosIde(new String[]{"Coordenada_12"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(o);
            po.setParametrosId(new ArrayList<>(Collections.singletonList(to)));
            give(p, po);
            hex(to);
        }

        void give(Personagem p, PersonagemOrdem po) {
            p.setAcao(p.getAcaoSize(), po);
        }

        /** An order that is not a movement and carries no coordinate: pure slot filler. */
        PersonagemOrdem idleOrder() {
            final Ordem o = new Ordem();
            o.setNome("Idle");
            o.setCodigo("1001");
            o.setTipo("Esp");
            o.setParametrosIde(new String[]{"None"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(o);
            po.setParametrosId(new ArrayList<>(Collections.singletonList(" ")));
            return po;
        }

        /** A character with a MovUnir: move to {@code to} and join {@code target} there. */
        Personagem joiner(String name, String from, String to, Personagem target) {
            final Personagem p = plain(name, from);
            final Ordem o = new Ordem();
            o.setNome("MovUnir");
            o.setCodigo("870");
            o.setTipo("Mov");
            o.setParametrosIde(new String[]{"Coordenada_12", "Personagem_Nacao"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(o);
            po.setParametrosId(new ArrayList<>(Arrays.asList(to, target.getCodigo())));
            p.setAcao(0, po);
            hex(to);
            return p;
        }

        /** An army commander moving by DIRECTIONS, which is how real armies move. */
        Personagem armyMover(String name, String from, String path) {
            final Personagem p = plain(name, from);
            final Ordem o = new Ordem();
            o.setNome("MovEsq");
            o.setCodigo("830");
            o.setTipo("Mov");
            o.setParametrosIde(new String[]{"Direcao_Ex"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(o);
            po.setParametrosId(new ArrayList<>(Collections.singletonList(path)));
            p.setAcao(0, po);
            return p;
        }
    }
}
