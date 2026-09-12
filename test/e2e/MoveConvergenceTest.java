package e2e;

import gui.services.MoveConvergence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rule that decides whether this warning is worth showing, driven by hand-built actors.
 * <p>
 * <b>Why synthetic and not the shared fixture.</b> {@code TestWorld}'s EGF is a ONE-PLAYER world, so
 * every character in it is the observer's: no hex can ever have a mover on both sides, and a test that
 * loops over the result would iterate zero times and assert nothing while still passing. The
 * interesting cases - two players, and the trap of ONE player owning TWO nations - only exist if they
 * are constructed. {@code compute} takes its world as parameters precisely so this is possible, and
 * building the hexes too keeps {@code setLocal}'s registration side effect out of the shared world.
 */
class MoveConvergenceTest {

    /** An ally and I both walking into the same empty hex: the case the feature exists for. */
    @Test
    void twoPlayersConvergingOnEmptyGroundIsFlagged() {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final Jogador ally = f.player("ally");
        final List<Personagem> pcs = Arrays.asList(
                f.mover("Ned", f.nation("Stark", me), "0101", "0505"),
                f.mover("Davos", f.nation("Baratheon", ally), "0110", "0505"));
        f.allAllied();

        final MoveConvergence.Result r = MoveConvergence.compute(pcs, me, f.locais);

        assertEquals(1, r.getByHex().size(), "one contested hex");
        final List<MoveConvergence.Mover> movers = r.getMoversAt(f.hex("0505"));
        assertEquals(2, movers.size());
        for (MoveConvergence.Mover m : movers) {
            assertNotNull(m.getPath(), "a mover must carry the curve its marker rides");
        }
    }

    /**
     * THE trap. A Jogador owns a MAP of nations, so one player can walk two of his own nations onto a
     * hex. Grouping by nation would call that a convergence and warn him about himself - and both
     * markers would sit on "mine"-coloured paths, which is the tell that the rule was wrong.
     */
    @Test
    void onePlayerWithTwoNationsIsNotAConvergence() {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final List<Personagem> pcs = Arrays.asList(
                f.mover("Ned", f.nation("Stark", me), "0101", "0505"),
                f.mover("Robb", f.nation("Winterfell", me), "0110", "0505"));

        assertTrue(MoveConvergence.compute(pcs, me, f.locais).isEmpty(),
                "two nations of the SAME player converging is one player's own plan");
    }

    /** Two of my own characters is my own business, and I can already see both orders. */
    @Test
    void twoOfMyOwnCharactersIsNotAConvergence() {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final Nacao stark = f.nation("Stark", me);
        final List<Personagem> pcs = Arrays.asList(
                f.mover("Ned", stark, "0101", "0505"),
                f.mover("Robb", stark, "0110", "0505"));

        assertTrue(MoveConvergence.compute(pcs, me, f.locais).isEmpty());
    }

    /** One character heading somewhere is not a convergence, however inviting the hex. */
    @Test
    void aSingleMoverIsNotAConvergence() {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final List<Personagem> pcs = Arrays.asList(
                f.mover("Ned", f.nation("Stark", me), "0101", "0505"));

        assertTrue(MoveConvergence.compute(pcs, me, f.locais).isEmpty());
    }

    /** A city, a character or an army on the hex makes converging on it a normal thing to do. */
    @Test
    void occupiedGroundIsNotFlagged() {
        assertTrue(convergeOnto(Occupancy.CITY).isEmpty(), "a city there is a reason to both go");
        assertTrue(convergeOnto(Occupancy.CHARACTER).isEmpty(), "somebody there is a reason to both go");
        assertTrue(convergeOnto(Occupancy.ARMY).isEmpty(), "an army there is a reason to both go");
        assertEquals(1, convergeOnto(Occupancy.NONE).getByHex().size(), "control: empty ground IS flagged");
    }

    /** A character already standing on the destination is not "moving into" it. */
    @Test
    void aMoveToWhereYouAlreadyStandIsNotAMove() {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final Jogador ally = f.player("ally");
        final List<Personagem> pcs = Arrays.asList(
                f.mover("Ned", f.nation("Stark", me), "0505", "0505"),   // already there
                f.mover("Davos", f.nation("Baratheon", ally), "0110", "0505"));
        f.allAllied();

        assertTrue(MoveConvergence.compute(pcs, me, f.locais).isEmpty(),
                "parity with drawMovPathPc, which draws no line for a move to the current hex");
    }

    /** A direction-parameterised move is an army move and is deliberately out of scope. */
    @Test
    void directionMovesAreIgnored() {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final Jogador ally = f.player("ally");
        final Personagem mine = f.mover("Ned", f.nation("Stark", me), "0101", "0505");
        final Personagem theirs = f.mover("Davos", f.nation("Baratheon", ally), "0110", "0505");
        f.allAllied();
        theirs.getAcoes().get(0).getOrdem().setParametrosIde(new String[]{"Direcao_Ex"});

        assertTrue(MoveConvergence.compute(pcs(mine, theirs), me, f.locais).isEmpty());
    }

    /** No input, no warning - and never a null for a caller to guard. */
    @Test
    void emptyInputYieldsEmptyResult() {
        final MoveConvergence.Result r = MoveConvergence.compute(null, null, null);
        assertTrue(r.isEmpty());
        assertTrue(r.getByHex().isEmpty());
        assertTrue(r.getMoversAt(new Local()).isEmpty(), "getMoversAt must never return null");
    }

    /** The cached picture the hex-info panel reads is never null, even before the first refresh. */
    @Test
    void currentResultIsNeverNull() {
        final MoveConvergence.Result saved = MoveConvergence.getCurrent();
        try {
            MoveConvergence.setCurrent(null);
            assertNotNull(MoveConvergence.getCurrent());
            assertTrue(MoveConvergence.getCurrent().isEmpty());
        } finally {
            MoveConvergence.setCurrent(saved); // static: do not bleed into later tests
        }
    }

    // ----- helpers -----

    private enum Occupancy { NONE, CITY, CHARACTER, ARMY }

    private static MoveConvergence.Result convergeOnto(Occupancy what) {
        final Fixture f = new Fixture();
        final Jogador me = f.player("me");
        final Jogador ally = f.player("ally");
        final List<Personagem> pcs = pcs(
                f.mover("Ned", f.nation("Stark", me), "0101", "0505"),
                f.mover("Davos", f.nation("Baratheon", ally), "0110", "0505"));
        final Local dest = f.hex("0505");
        f.allAllied();
        switch (what) {
            case CITY:
                dest.setCidade(new Cidade());
                break;
            case CHARACTER:
                // a bystander, not one of the movers
                f.plain("Bystander", f.nation("Tully", ally), "0505");
                break;
            case ARMY:
                final model.Exercito host = new model.Exercito();
                host.setCodigo("host");   // the hex indexes armies by codigo; a null key NPEs the TreeMap
                dest.addExercito(host);
                break;
            default:
                break;
        }
        return MoveConvergence.compute(pcs, me, f.locais);
    }

    private static List<Personagem> pcs(Personagem... list) {
        return new ArrayList<>(Arrays.asList(list));
    }

    /** Builds a throwaway world: its own hexes, so nothing registers into the shared fixture. */
    private static final class Fixture {

        private final SortedMap<String, Local> locais = new TreeMap<>();
        private final List<Nacao> nations = new ArrayList<>();

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

        Jogador player(String login) {
            final Jogador j = new Jogador();
            j.setNome(login);
            j.setCodigo(login);
            return j;
        }

        Nacao nation(String name, Jogador owner) {
            final Nacao n = new Nacao();
            n.setNome(name);
            n.setCodigo(name);
            n.setOwner(owner);
            owner.addNacao(n);
            nations.add(n);
            return n;
        }

        /**
         * Make every nation friendly with every other, so allied characters survive the
         * {@code isMine || isAlly} filter the production code inherits from drawMovPathPc.
         * <p>
         * Direction matters and is easy to get backwards: {@code Jogador.isJogadorAliado} reads
         * {@code targetNation.getRelacionamento(myNation) > 1}, i.e. the relationship is read off the
         * OTHER nation's map, not mine.
         */
        void allAllied() {
            for (Nacao a : nations) {
                for (Nacao b : nations) {
                    if (a != b) {
                        a.addRelacionamento(b, 2);
                    }
                }
            }
        }

        /** A character standing somewhere, with no orders. */
        Personagem plain(String name, Nacao nation, String at) {
            final Personagem p = new Personagem();
            p.setNome(name);
            p.setCodigo(name);
            p.setNacao(nation);
            p.setLocal(hex(at));
            return p;
        }

        /** A character with a queued move to {@code to}, exactly as the order pickers would store it. */
        Personagem mover(String name, Nacao nation, String from, String to) {
            final Personagem p = plain(name, nation, from);
            final Ordem move = new Ordem();
            move.setNome("MovPers");
            move.setCodigo("1810");
            move.setTipo("Mov");
            move.setParametrosIde(new String[]{"Coordenada_12"});
            final PersonagemOrdem po = new PersonagemOrdem();
            po.setOrdem(move);
            po.setParametrosId(new ArrayList<>(Arrays.asList(to)));
            p.setAcao(0, po);
            hex(to); // the destination must exist in the index for getLocalDestination to resolve it
            return p;
        }
    }
}
