package gui.services;

import business.ImageManager;
import business.converter.ConverterFactory;
import business.facade.AcaoFacade;
import business.facade.JogadorFacade;
import java.awt.Point;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import model.Jogador;
import model.Local;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Finds hexes that two different PLAYERS are both sending a character into, where nothing is waiting
 * when they arrive - the "we each sent someone to found a camp there" mistake, which costs two actions
 * to achieve what one would have.
 * <p>
 * <b>Two players, not two nations - and "another player" has to be detected indirectly.</b> Grouping by
 * nation would be wrong: {@code Jogador} owns a MAP of nations, so one player walking two of his own
 * nations onto a hex would be flagged as converging with himself. But grouping by
 * {@code Nacao.getOwner()} does not work either, because in a player's results EGF only the player's
 * OWN nation carries an owner - measured across sample turns, every other nation has a null one. A
 * literal "two distinct owners" test would therefore never fire at all.
 * <p>
 * So the test is <b>at least one mover that is mine and at least one that is not</b>, which is exactly
 * "an ally and I" and is what the warning is for. It also handles the two-nations-one-player case for
 * free: both are mine, so nothing is flagged.
 * <p>
 * <b>Two different "mine" tests, deliberately.</b> Ownership for the DECISION uses
 * {@code getNacao().getOwner() == observer}, copying {@code WorldControler.isMine} - whose javadoc
 * records that {@code Jogador.isNacao} (what {@code JogadorFacade.isMine} calls) becomes unreliable
 * once allies' EGFs are autoloaded and their nations merge into the shared world. The marker COLOUR
 * uses {@code JogadorFacade.isMine} instead, because that is the exact test
 * {@code MapaManager.drawMovPathPc} uses to colour the line underneath - the marker must match its own
 * path, whatever that test decides.
 * <p>
 * <b>Why the hex has to be empty.</b> Converging on a city or an army is usually the whole point -
 * visiting, joining, reinforcing. Converging on bare ground is where the duplicated effort lives.
 * <p>
 * <b>What "empty" honestly means.</b> It is what the observer can see, this turn: fog of war can hide
 * an army or a city, and an army marching onto the hex is not counted at all, because army moves are
 * direction-parameterised and deliberately out of scope. So this is a hint, not a verdict - it never
 * blocks anything, and two players converging on purpose is a legitimate plan.
 */
public final class MoveConvergence {

    private static final Log log = LogFactory.getLog(MoveConvergence.class);

    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();

    /**
     * Last computed picture, read by the hex-info panel on every click. Refreshed on
     * ACTIONS_MAP_REDRAW and cleared when a new EGF is opened - a cached mover holds a Personagem and
     * a Local, and either reaches the whole previous World graph.
     */
    private static volatile Result current = new Result(Collections.<Local, List<Mover>>emptyMap());

    private MoveConvergence() {
    }

    public static void setCurrent(Result result) {
        current = (result == null) ? new Result(Collections.<Local, List<Mover>>emptyMap()) : result;
    }

    /** Never null; empty until the first refresh. */
    public static Result getCurrent() {
        return current;
    }

    /** One character walking into a contested hex, and the curve their order is drawn along. */
    public static final class Mover {

        private final String actorName;
        private final String nationName;
        private final Shape path;

        Mover(String actorName, String nationName, Shape path) {
            this.actorName = actorName;
            this.nationName = nationName;
            this.path = path;
        }

        public String getActorName() {
            return actorName;
        }

        public String getNationName() {
            return nationName;
        }

        /** The SAME curve the map already drew for this order - markers ride it, never a copy of it. */
        public Shape getPath() {
            return path;
        }
    }

    /** The contested hexes and who is walking into each. */
    public static final class Result {

        private final Map<Local, List<Mover>> byHex;

        Result(Map<Local, List<Mover>> byHex) {
            this.byHex = byHex;
        }

        public Map<Local, List<Mover>> getByHex() {
            return byHex;
        }

        /** Who is moving into this hex; empty when it is not contested. Never null. */
        public List<Mover> getMoversAt(Local hex) {
            final List<Mover> at = byHex.get(hex);
            return at == null ? Collections.<Mover>emptyList() : at;
        }

        public boolean isEmpty() {
            return byHex.isEmpty();
        }
    }

    /**
     * @param pcs      every character in the world
     * @param observer the active player
     * @param locais   the hex index, for resolving order parameters
     */
    public static Result compute(Collection<Personagem> pcs, Jogador observer, SortedMap<String, Local> locais) {
        final Map<Local, List<Mover>> candidates = new HashMap<>();
        final Map<Local, boolean[]> sides = new HashMap<>(); // {sawMine, sawOther} per hex
        if (pcs == null || locais == null) {
            return new Result(Collections.<Local, List<Mover>>emptyMap());
        }
        for (Personagem pers : pcs) {
            if (pers.getLocal() == null || pers.getNacao() == null) {
                continue;
            }
            // Colour test: exactly what drawMovPathPc uses, so the marker matches the line it rides.
            final boolean mine = jogadorFacade.isMine(pers, observer);
            if (!mine && !jogadorFacade.isAlly(pers, observer)) {
                continue;
            }
            // Decision test: owner identity, which survives allied EGFs being merged in.
            final boolean ownedByMe = pers.getNacao().getOwner() == observer;
            for (PersonagemOrdem po : pers.getAcoes().values()) {
                // One malformed action must never cost the whole overlay, and this runs inside the
                // order-save dispatch - an escaping exception would leave a half-applied save.
                try {
                    if (!acaoFacade.isMovimento(po) || acaoFacade.isMovimentoDirection(po)) {
                        continue;
                    }
                    final Local dest = acaoFacade.getLocalDestination(pers, po, locais);
                    // both skips are exactly what drawMovPathPc does, so a marker can never appear
                    // on a path the map did not draw
                    if (dest == null || pers.getLocal().equals(dest)) {
                        continue;
                    }
                    final Point ori1x = ConverterFactory.localToPoint(pers.getLocal());
                    final Point dest1x = ConverterFactory.localToPoint(dest);
                    // `mine` still picks the curve: the own and allied paths carry different pixel
                    // offsets so two lines to one hex stay legible as two. The MARKER no longer varies
                    // by it - it is magenta either way, and the line beneath says whose it is.
                    final Mover mover = new Mover(pers.getNome(), pers.getNacao().getNome(),
                            ImageManager.getPathPcShape(ori1x, dest1x, mine));
                    List<Mover> at = candidates.get(dest);
                    if (at == null) {
                        at = new ArrayList<>(2);
                        candidates.put(dest, at);
                        sides.put(dest, new boolean[2]);
                    }
                    at.add(mover);
                    final boolean[] side = sides.get(dest);
                    side[ownedByMe ? 0 : 1] = true;
                } catch (RuntimeException ex) {
                    log.warn(String.format("Skipping unreadable move for %s: %s", pers.getNome(), ex));
                }
            }
        }
        final Map<Local, List<Mover>> ret = new HashMap<>();
        for (Map.Entry<Local, List<Mover>> e : candidates.entrySet()) {
            final boolean[] side = sides.get(e.getKey());
            if (e.getValue().size() < 2 || !side[0] || !side[1]) {
                // one mover, or every mover on the same side - including one player sending two of his
                // own nations, and two of my own characters, neither of which is somebody else's plan
                continue;
            }
            if (!isOpenGround(e.getKey())) {
                continue;
            }
            ret.put(e.getKey(), e.getValue());
        }
        if (log.isDebugEnabled() && !ret.isEmpty()) {
            log.debug(String.format("CONVERGE: %d contested hex(es)", ret.size()));
        }
        return new Result(ret);
    }

    /**
     * Nothing is waiting on this hex as far as the player can see. A city, a character or an army there
     * makes converging on it a normal thing to do.
     */
    private static boolean isOpenGround(Local hex) {
        return hex.getCidade() == null
                && hex.getPersonagens().isEmpty()
                && hex.getExercitos().isEmpty();
    }

}
