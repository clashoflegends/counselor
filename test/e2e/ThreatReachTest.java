package e2e;

import business.ArmyPath;
import business.MovimentoExercito;
import business.facade.ExercitoFacade;
import control.services.ThreatReach;
import control.services.ThreatReach.Coverage;
import control.services.ThreatReach.Threat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Cenario;
import model.Exercito;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Raca;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules behind {@link ThreatReach}, on armies built to order.
 *
 * {@link ThreatReachLiveWorldTest} proves the wrapper reproduces the Judge's own flood on real
 * terrain. This proves the decisions the wrapper makes BEFORE the flood - what to assume about an
 * army nobody has scouted - which a real turn file cannot exercise, because it holds at most a
 * couple of enemy armies and no control over what they are made of.
 */
class ThreatReachTest {

    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();

    /** A 12x12 grid with a terrain per hex, plus the catalogue the flood resolves against. */
    private static final class Fixture {

        private final SortedMap<String, Local> locais = new TreeMap<>();
        private final Cenario cenario = new Cenario();
        private final Partida partida = new Partida();
        private final Terreno plain = terreno("P", "Plain", false);
        private final Terreno water = terreno("A", "Water", true);
        private final Terreno mountain = terreno("M", "Mountain", false);
        /** Shore. MovimentoExercito.isNavegavel only lets a fleet step ashore onto terrain "L"
         * (or an ancoravel hex), so a landing test needs one - a plain coast is not navigable. */
        private final Terreno shore = terreno("L", "Shore", false);
        private final Jogador me = new Jogador();
        private final Nacao mine = nacao("MINE");
        private final Nacao theirs = nacao("THEM");

        Fixture() {
            partida.setCenario(cenario);
            cenario.getTerrenos().put(plain.getCodigo(), plain);
            cenario.getTerrenos().put(water.getCodigo(), water);
            cenario.getTerrenos().put(mountain.getCodigo(), mountain);
            cenario.getTerrenos().put(shore.getCodigo(), shore);
            for (int col = 1; col <= 12; col++) {
                for (int row = 1; row <= 12; row++) {
                    hex(String.format("%02d%02d", col, row), plain);
                }
            }
            me.addNacao(mine);
        }

        Local hex(String codigo, Terreno terreno) {
            Local ret = locais.get(codigo);
            if (ret == null) {
                ret = new Local();
                ret.setCodigo(codigo);
                //model.Local declares "String estrada, rio, riacho, vau, ponte, rastro, landing = """
                //- only landing gets the empty string, the rest arrive NULL, and isRio() does
                //rio.contains(..). A real EGF always sets them; a fixture has to.
                ret.setEstrada("");
                ret.setRio("");
                ret.setRiacho("");
                ret.setVau("");
                ret.setPonte("");
                ret.setRastro("");
                locais.put(codigo, ret);
            }
            ret.setTerreno(terreno);
            return ret;
        }

        /** An army of {@code nacao} on a hex, with a visible composition. */
        Exercito army(String nome, String at, Nacao nacao, TipoTropa... troops) {
            final Exercito ret = new Exercito();
            ret.setNome(nome);
            ret.setCodigo(nome);
            ret.setNacao(nacao);
            ret.setLocal(locais.get(at));
            ret.setComida(9);
            int ii = 0;
            for (TipoTropa troop : troops) {
                final Pelotao pelotao = new Pelotao();
                pelotao.setTipoTropa(troop);
                pelotao.setQtd(100);
                ret.getPelotoes().put("P" + ii++, pelotao);
            }
            ret.setTamanhoExercito(troops.length);
            return ret;
        }

        /**
         * An army exactly as an unscouted one arrives: a name, a nation, a size band, and NOTHING
         * else. No platoons, no habilidades, comida 0.
         */
        Exercito unscouted(String nome, String at, Nacao nacao, int landBand, int seaBand) {
            final Exercito ret = new Exercito();
            ret.setNome(nome);
            ret.setCodigo(nome);
            ret.setNacao(nacao);
            ret.setLocal(locais.get(at));
            ret.setTamanhoExercito(landBand);
            ret.setTamanhoEsquadra(seaBand);
            return ret;
        }

        Coverage run(Exercito... armies) {
            return ThreatReach.compute(Arrays.asList(armies), me, locais, partida, 14);
        }
    }

    private static Terreno terreno(String codigo, String nome, boolean agua) {
        final Terreno ret = new Terreno();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        ret.setAgua(agua);
        return ret;
    }

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    /**
     * A troop with an explicit cost per terrain.
     *
     * {@code isBarcos()} is not a field: it is {@code hasHabilidade(";TTN;")}, the same token
     * {@code ExercitoFacade.isEsquadra} reads off the ARMY - which is why an unscouted army, whose
     * habilidades map arrives empty, cannot be identified as a fleet that way.
     */
    private static TipoTropa troop(String codigo, boolean boats, String habilidades,
            Terreno[] terrenos, int[] costs) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (boats) {
            ret.addHabilidade(habilidade(";TTN;"));
        }
        for (String code : habilidades.split(",")) {
            if (!code.trim().isEmpty()) {
                ret.addHabilidade(habilidade(code.trim()));
            }
        }
        final SortedMap<Terreno, Integer> mov = new TreeMap<>();
        for (int ii = 0; ii < terrenos.length; ii++) {
            mov.put(terrenos[ii], costs[ii]);
        }
        ret.setMovimentoTerreno(mov);
        return ret;
    }

    private static Habilidade habilidade(String codigo) {
        final Habilidade ret = new Habilidade();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static void giveRoster(Nacao nacao, TipoTropa... troops) {
        final Raca raca = new Raca();
        raca.setCodigo("R");
        raca.setNome("R");
        for (TipoTropa troop : troops) {
            raca.getTropas().put(troop, 0);
        }
        nacao.setRaca(raca);
    }

    // ----- the defect this class exists to close -----

    /**
     * <b>The shared cost model, characterised.</b> Hand {@code calculateArmyRangeHexes} an EMPTY
     * troop list - which is exactly what an unscouted army has - and it walks across the ocean.
     *
     * {@code MovimentoExercito.getCustoMovimentacaoTerra} opens with
     * {@code if (getTropas().isEmpty()) return 1;}, ABOVE the water, unbridged-river and
     * mountain-to-mountain checks in the same method. This is not a bug being fixed here: seven Judge
     * classes use that class and {@code Ordem851MoveNpcArmy} calls this flood, so it stays exactly as
     * it is. The test is here so that if it ever changes, whoever changes it finds out that
     * {@link ThreatReach}'s whole roster-substitution exists because of it.
     */
    @Test
    void theSharedCostModelTreatsAnEmptyArmyAsAbleToCrossOceans() {
        final Fixture f = new Fixture();
        f.hex("0605", f.water);
        f.hex("0705", f.water);
        final MovimentoExercito movEx = new MovimentoExercito();
        movEx.setOrigem(f.locais.get("0505"));
        movEx.setPorAgua(false);
        movEx.setComida(true);
        movEx.setLimiteMovimento(14);

        assertTrue(exercitoFacade.calculateArmyRangeHexes(f.locais, movEx)
                .containsKey(f.locais.get("0705")),
                "with no troops every hex costs 1 and nothing is impassable - the defect, pinned");
    }

    /** And the whole point: ThreatReach never hands it an empty list, so the ocean holds. */
    @Test
    void anUnscoutedLandArmyCannotWalkOnWater() {
        final Fixture f = new Fixture();
        f.hex("0605", f.water);
        f.hex("0705", f.water);
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);
        final Exercito enemy = f.unscouted("Desconhecido", "0505", f.theirs, 2, 0);

        final Coverage found = f.run(enemy);

        assertTrue(found.getThreats(f.locais.get("0705")).isEmpty(),
                "a land force has no business on the far side of the water");
        assertFalse(found.getThreats(f.locais.get("0506")).isEmpty(),
                "control: it still reaches the dry hex next door");
    }

    /** A fleet is a fleet: the size bands say which, because the habilidades map arrives empty. */
    @Test
    void anUnscoutedFleetIsReadFromItsSizeBandNotItsHabilidades() {
        final Fixture f = new Fixture();
        for (int row = 1; row <= 12; row++) {
            f.hex(String.format("%02d%02d", 6, row), f.water);
            f.hex(String.format("%02d%02d", 5, row), f.water);
        }
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        final TipoTropa ship = troop("SHIP", true, "", new Terreno[]{f.water}, new int[]{2});
        giveRoster(f.theirs, foot, ship);
        final Exercito fleet = f.unscouted("Desconhecido", "0505", f.theirs, 0, 3);

        assertFalse(f.run(fleet).getThreats(f.locais.get("0509")).isEmpty(),
                "ExercitoFacade.isEsquadra reads ;TTN; from a habilidades map that arrives empty, "
                + "so the fleet would otherwise vanish and every coastal city look safe");
    }

    /**
     * <b>A fleet one hex off a port threatens it NEXT turn, not the turn after.</b>
     *
     * The landing rule in {@code ExercitoFacade.calculateArmyRangeHexes} (lines 704-707) overwrites
     * the cost of stepping ashore with {@code max(1, limit - prevCost)}, so a landing total always
     * EQUALS the limit, whatever the limit happens to be. Classifying one flood run at twice the
     * budget by its cost therefore pushed every landing over the one-turn line, and every coastal
     * city came back amber however close the fleet was parked. Two floods, one per turn, is the fix.
     *
     * This is why {@link ThreatReachLiveWorldTest} cannot be the only check: the fixture turn file
     * holds one countable army and it is a land army, so no sea flood is ever compared against the
     * oracle.
     */
    @Test
    void aFleetOneHexOffThePortThreatensItNextTurn() {
        final Fixture f = new Fixture();
        for (int row = 1; row <= 12; row++) {
            f.hex(String.format("%02d%02d", 6, row), f.water);
        }
        f.hex("0505", f.shore);
        final TipoTropa ship = troop("SHIP", true, "", new Terreno[]{f.water, f.shore},
                new int[]{2, 2});
        giveRoster(f.theirs, ship);
        //anchored one water hex away from the shore at column 5
        final Exercito fleet = f.unscouted("Desconhecido", "0605", f.theirs, 0, 3);

        assertEquals(1, f.run(fleet).getSoonest(f.locais.get("0505")),
                "it is one step ashore; amber here is an under-warning on a coastal city");
    }

    // ----- what to assume about an army nobody has counted -----

    /**
     * The union over non-dominated troop types, not one "fastest" type.
     *
     * The two here are incomparable on purpose, and not only on price. The runner crosses plains at
     * 2 and mountains at 12; the climber is the other way round AND carries {@code ;TTM;}, without
     * which {@code getCustoMovimentacaoTerra} refuses mountain-to-mountain outright however many
     * points an army has. So the runner reaches plains the climber never will, and the climber walks
     * into the middle of a range the runner cannot enter at any price. Pick either one alone and the
     * reach is under-stated on the terrain the other is built for, which is the one direction a
     * threat display must never go.
     */
    @Test
    void theAssumedReachIsTheUnionOverTroopsNoOtherTroopBeatsEverywhere() {
        final Fixture f = new Fixture();
        for (int row = 1; row <= 12; row++) {
            f.hex(String.format("%02d%02d", 7, row), f.mountain);
            f.hex(String.format("%02d%02d", 8, row), f.mountain);
        }
        final TipoTropa runner = troop("RUNNER", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{2, 12});
        final TipoTropa climber = troop("CLIMBER", false, ";TTM;",
                new Terreno[]{f.plain, f.mountain}, new int[]{6, 4});
        giveRoster(f.theirs, runner, climber);

        final Set<Local> both = f.run(f.unscouted("Desconhecido", "0505", f.theirs, 2, 0))
                .getHexesWithin(1);

        giveRoster(f.theirs, runner);
        final Set<Local> runnerOnly = f.run(f.unscouted("R", "0505", f.theirs, 2, 0))
                .getHexesWithin(1);
        giveRoster(f.theirs, climber);
        final Set<Local> climberOnly = f.run(f.unscouted("C", "0505", f.theirs, 2, 0))
                .getHexesWithin(1);

        assertTrue(both.containsAll(runnerOnly) && both.containsAll(climberOnly),
                "the union must contain everything either type reaches on its own");
        assertTrue(climberOnly.contains(f.locais.get("0805")),
                "fixture check: only the ;TTM; troop gets into the second mountain column");
        assertFalse(runnerOnly.contains(f.locais.get("0805")),
                "fixture check: mountain to mountain without ;TTM; is refused at any price");
        assertTrue(both.size() > runnerOnly.size(), "the climber adds mountain hexes the runner cannot");
        assertTrue(both.size() > climberOnly.size(), "the runner adds plains the climber cannot");
    }

    /** A type another type beats on every terrain is not worth a flood. */
    @Test
    void aTroopBeatenEverywhereIsDroppedFromTheAssumption() {
        final Fixture f = new Fixture();
        final TipoTropa quick = troop("QUICK", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{2, 4});
        final TipoTropa slow = troop("SLOW", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{5, 12});
        giveRoster(f.theirs, quick, slow);

        final Coverage found = f.run(f.unscouted("Desconhecido", "0505", f.theirs, 2, 0));

        for (Threat threat : found.getThreats(f.locais.get("0507"))) {
            assertEquals("QUICK", threat.getAssumedTroop(),
                    "SLOW is beaten on every terrain, so it buys nothing and should not be flooded");
        }
    }

    /** What is assumed must be named, or the row reads as counted intelligence. */
    @Test
    void anAssumedThreatCarriesTheTroopItAssumedAndACountedOneDoesNot() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);

        final Threat assumed = f.run(f.unscouted("Desconhecido", "0505", f.theirs, 2, 0))
                .getThreats(f.locais.get("0506")).get(0);
        assertTrue(assumed.isAssumed());
        assertEquals("FOOT", assumed.getAssumedTroop());

        final Threat counted = f.run(f.army("Seen", "0505", f.theirs, foot))
                .getThreats(f.locais.get("0506")).get(0);
        assertFalse(counted.isAssumed(), "we counted this one; it must not be marked a guess");
    }

    /**
     * An unscouted army exports {@code comida} 0 - starving - and is nonetheless treated as fed.
     *
     * Being unfed costs a third of every hex ({@code ceil(cost * 4/3)}), so taking that 0 at face
     * value would quietly UNDER-state the reach of exactly the armies the player knows least about.
     * It does not, and the reason is an accident rather than a decision:
     * {@code ExercitoFacade.getComidaConsumo} sums upkeep over the PLATOONS, an unscouted army has
     * none, so consumption is 0, {@code 0 >= 0} holds, and {@code isComida} answers fed.
     * <p>
     * This test exists because that accident is load-bearing. Estimate consumption from the size
     * band instead - a perfectly reasonable future change - and every unscouted threat silently
     * shrinks by a quarter with nothing else to say so.
     */
    @Test
    void anUnscoutedArmyIsTreatedAsFedBecauseItHasNoPlatoonsToFeed() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);
        final Exercito enemy = f.unscouted("Desconhecido", "0505", f.theirs, 2, 0);

        assertEquals(0, enemy.getComida(), "this is how the server exports one");
        assertTrue(exercitoFacade.isComida(enemy),
                "consumption is summed over platoons it does not have, so starving reads as fed");

        //fed: 16 points at 3 a hex is 5 hexes. Unfed each hex costs ceil(3 * 4/3) = 4, so 4 hexes.
        //Asserted on the TURN, not on presence: the flood runs at two turns' budget, so an unfed
        //army still reaches this hex - just a turn later, which is the whole difference.
        assertEquals(1, f.run(enemy).getSoonest(f.locais.get("0510")),
                "the fifth hex is only one turn away if the army is treated as fed");
    }

    /**
     * Evasive movement doubles every hex, and the exported flag is a placeholder on an army nobody
     * has scouted. Unlike supply, nothing else makes that safe - a {@code true} taken at face value
     * would halve the reach of the armies we know least about.
     */
    @Test
    void anUnscoutedArmysEvasiveFlagIsNotBelieved() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);
        final Exercito enemy = f.unscouted("Desconhecido", "0505", f.theirs, 2, 0);
        enemy.setMovimentacaoEvasiva(true);

        assertEquals(1, f.run(enemy).getSoonest(f.locais.get("0510")),
                "believing the flag would double every hex and push this into the second turn");
    }

    // ----- the budget -----

    /**
     * The worst-case budget is the Judge's, not the plain-move one.
     *
     * {@code Ordem860ForcedMarch} and {@code Ordem830MovNavy} both override
     * {@code getMovPoints} with {@code getMovimentoExercito() + 2}, and between them they account for
     * 22,463 orders across games 850 to 907. The floor of 14 is {@code Ordem851MoveNpcArmy}, which
     * hardcodes its budget whatever the scenario says.
     */
    @Test
    void theBudgetCoversForcedMarchAndTheNpcFloor() {
        assertEquals(16, ThreatReach.budgetPerTurn(14), "14 + 2 for Forced March");
        assertEquals(14, ThreatReach.budgetPerTurn(8), "an ;AM08; game still faces NPCs moving 14");
        assertEquals(14, ThreatReach.budgetPerTurn(12), "12 + 2");
    }

    /** And it is really spent: a hex beyond a plain move is still in the turn-1 band. */
    @Test
    void aHexBeyondAPlainMoveIsStillReachedInOneTurn() {
        final Fixture f = new Fixture();
        //cost 5 a hex: a plain move of 14 buys 2 hexes, a forced march of 16 buys 3
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{5, 12});
        giveRoster(f.theirs, foot);

        final Coverage found = f.run(f.unscouted("Desconhecido", "0505", f.theirs, 2, 0));

        assertFalse(found.getThreats(f.locais.get("0508")).isEmpty(),
                "three hexes out costs 15, which only a forced march pays for");
        assertEquals(1, found.getSoonest(f.locais.get("0508")));
    }

    /** Turn 2 is a band of its own and is reached, not merged into turn 1. */
    @Test
    void whatIsOutOfReachThisTurnCanStillBeReachedNext() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{5, 12});
        giveRoster(f.theirs, foot);

        final Coverage found = f.run(f.unscouted("Desconhecido", "0505", f.theirs, 2, 0));

        assertEquals(2, found.getSoonest(f.locais.get("0510")),
                "five hexes out is 25 points: beyond one turn, inside two");
        assertTrue(found.getHexesWithin(2).containsAll(found.getHexesWithin(1)),
                "the two-turn band contains the one-turn band");
    }

    /** The hex the army is standing on is the one a player most needs flagged. */
    @Test
    void theHexAnArmyIsStandingOnIsThreatenedByIt() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);

        assertEquals(1, f.run(f.unscouted("Desconhecido", "0505", f.theirs, 2, 0))
                .getSoonest(f.locais.get("0505")));
    }

    // ----- the Armies tab shares this, and that is where the defect was visible -----

    /**
     * {@code rangeOf} is what {@code MapaControler.printTagArmyRange} draws, and it is the live bug.
     *
     * The Armies tab's filter combo offers All first and Enemies further down, so a player can click
     * an unscouted enemy army - and before the roster substitution the shaded range came back as a
     * perfect circle of the full point budget that crossed water. Sharing the call with the threat
     * picture is also what stops the two answering differently about the same army.
     */
    @Test
    void theArmiesTabRangeOfAnUnscoutedEnemyRespectsTerrain() {
        final Fixture f = new Fixture();
        for (int row = 1; row <= 12; row++) {
            f.hex(String.format("%02d%02d", 6, row), f.water);
        }
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);
        final Exercito enemy = f.unscouted("Desconhecido", "0505", f.theirs, 2, 0);

        final Map<Local, ArmyPath> range = ThreatReach.rangeOf(enemy, f.locais, f.cenario, 14);

        assertFalse(range.isEmpty(), "it still has a range");
        for (Local hex : range.keySet()) {
            assertFalse(hex.getTerreno().isAgua(), hex.getCodigo() + " is water");
        }
    }

    /** The union keeps the CHEAPEST path per hex, so the band a hex lands in is the soonest one. */
    @Test
    void rangeOfKeepsTheCheapestPathWhenTwoTroopsReachTheSameHex() {
        final Fixture f = new Fixture();
        final TipoTropa quick = troop("QUICK", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{2, 12});
        final TipoTropa slow = troop("SLOW", false, ";TTM;", new Terreno[]{f.plain, f.mountain},
                new int[]{5, 4});
        giveRoster(f.theirs, quick, slow);

        final Map<Local, ArmyPath> range = ThreatReach.rangeOf(
                f.unscouted("Desconhecido", "0505", f.theirs, 2, 0), f.locais, f.cenario, 14);

        assertEquals(2, range.get(f.locais.get("0506")).getCost(),
                "both reach the next hex; the cheap one has to win");
    }

    /** Nothing to ask about is not an exception. */
    @Test
    void rangeOfAnArmyWithNoHexIsEmpty() {
        final Fixture f = new Fixture();
        final Exercito nowhere = new Exercito();
        nowhere.setNome("Nowhere");
        nowhere.setCodigo("Nowhere");

        assertTrue(ThreatReach.rangeOf(nowhere, f.locais, f.cenario, 14).isEmpty());
        assertTrue(ThreatReach.rangeOf(null, f.locais, f.cenario, 14).isEmpty());
    }

    // ----- the cache -----

    /**
     * The picture is kept for the whole planning session - enemy positions do not move while the
     * player writes orders - and dropped when a new game is opened.
     *
     * {@code MapaControler.initialize} is what drops it, beside the ScoutFootprint and
     * MoveConvergence snapshots and for the same reason: a kept Threat holds an Exercito and the map
     * keys on Local, so either one pins the whole previous World graph.
     */
    @Test
    void theSnapshotIsKeptUntilANewGameIsOpened() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);
        final Exercito enemy = f.unscouted("Desconhecido", "0505", f.theirs, 2, 0);
        ThreatReach.setCurrent(null);

        final Coverage first = ThreatReach.getOrCompute(Arrays.asList(enemy), f.me, f.locais,
                f.partida, 14);
        assertSame(first, ThreatReach.getOrCompute(Arrays.asList(enemy), f.me, f.locais,
                f.partida, 14), "the second ask must not pay for the flood again");

        ThreatReach.setCurrent(null);
        assertNotSame(first, ThreatReach.getOrCompute(Arrays.asList(enemy), f.me, f.locais,
                f.partida, 14), "and a new game must not inherit the old one's picture");
        ThreatReach.setCurrent(null);
    }

    // ----- whose armies count -----

    /**
     * An army with NO nation counts.
     *
     * This is the one place this parts company with the Armies tab's own "enemies" filter, which
     * wraps the whole test in {@code catch (NullPointerException)} and drops such an army in silence.
     * An unscouted army can arrive exactly that way, and dropping the ones we know least about is
     * backwards for a threat display.
     */
    @Test
    void anArmyWithNoNationIsTreatedAsHostile() {
        final Fixture f = new Fixture();
        final Exercito orphan = f.unscouted("Desconhecido", "0505", null, 2, 0);

        assertTrue(ThreatReach.isHostile(orphan, f.me));
    }

    @Test
    void myOwnArmiesAreNotAThreatToMe() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});

        assertFalse(ThreatReach.isHostile(f.army("Mine", "0505", f.mine, foot), f.me));
        assertTrue(f.run(f.army("Mine", "0505", f.mine, foot)).isEmpty());
    }

    @Test
    void nothingIsComputedWithoutAWorld() {
        assertTrue(ThreatReach.compute(null, null, null, null, 14).isEmpty());
        assertTrue(ThreatReach.compute(new ArrayList<>(), new Jogador(), new TreeMap<>(),
                new Partida(), 14).isEmpty(), "a Partida with no scenario answers nothing");
    }

    /** One unreadable army must not cost the picture of all the others. */
    @Test
    void oneBrokenArmyDoesNotTakeDownTheRest() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{3, 12});
        giveRoster(f.theirs, foot);
        //a troop with no terrain costs at all: the flood throws on it
        final TipoTropa corrupt = new TipoTropa();
        corrupt.setCodigo("X");
        corrupt.setNome("X");
        corrupt.setMovimentoTerreno(null);
        final Nacao other = nacao("OTHER");
        giveRoster(other, corrupt);
        final Exercito broken = f.unscouted("Broken", "0505", other, 2, 0);
        final Exercito good = f.unscouted("Good", "0909", f.theirs, 2, 0);

        assertFalse(f.run(broken, good).getThreats(f.locais.get("0910")).isEmpty());
    }

    /** Threats on one hex come back soonest first, so a row can lead with the worst news. */
    @Test
    void threatsAreOrderedSoonestFirst() {
        final Fixture f = new Fixture();
        final TipoTropa foot = troop("FOOT", false, "", new Terreno[]{f.plain, f.mountain},
                new int[]{5, 12});
        giveRoster(f.theirs, foot);
        final Exercito near = f.unscouted("Near", "0505", f.theirs, 2, 0);
        final Exercito far = f.unscouted("Far", "0501", f.theirs, 2, 0);

        final List<Threat> at = f.run(near, far).getThreats(f.locais.get("0506"));
        assertTrue(at.size() >= 2, "both must reach it, or this orders nothing");
        assertEquals(1, at.get(0).getTurns());
    }
}
