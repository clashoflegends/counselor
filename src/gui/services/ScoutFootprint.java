package gui.services;

import business.facade.AcaoFacade;
import business.facade.JogadorFacade;
import business.facade.LocalFacade;
import business.facade.PersonagemFacade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import model.Jogador;
import model.Local;
import model.Personagem;
import model.PersonagemOrdem;

/**
 * Works out which hexes each queued scout/recon/scry order will actually uncover, and where those
 * footprints tread on each other, so the player can spread them out instead of paying twice for the
 * same ground.
 * <p>
 * <b>Why a footprint and not the picker's range ring.</b> Map Area's hex picker is limited to range 8,
 * so the border the map used to draw was the diplomat's <i>reach</i> - 217 hexes - while the order
 * only ever uncovers <b>7</b>: the target and its six neighbours. Recon Area and Scout Area take no
 * hex parameter at all and uncover the same 7 around wherever the character ends the turn. Drawing
 * reach instead of effect made overlap impossible to judge by eye.
 * <p>
 * <b>The 7 comes from the Judge</b> - {@code NacaoControl.addLocalVisivel} sets the centre plus
 * directions 1..6 visible, and the army-detection loop in {@code Ordem.executa910_925} walks the same
 * {@code ii = 0..6}. Skill rank changes how much detail the report carries, never the radius, so the
 * footprint is a fixed radius 1 for every order that carries {@code ;ASR;}. If the water-extension
 * branch in {@code addLocalVisivel} is ever repaired (today it re-reads the same neighbour instead of
 * stepping outward, so it extends nothing), the <i>visibility</i> footprint would grow over water
 * while the intel footprint stayed at 7, and {@link #SCOUT_RADIUS} would no longer tell the whole
 * story.
 * <p>
 * <b>Overlap is a pure function of the distance between two centres</b>, identical for every parity of
 * the offset grid: distance 0 shares all 7, distance 1 shares 4, distance 2 shares 1 or 2, and
 * <b>distance 3 or more shares nothing</b>. "Keep scout centres at least 3 hexes apart" is the whole
 * rule, and it is what the overlap shading teaches without saying it.
 * <p>
 * Overlapping is not always a mistake, so nothing here blocks or filters: army detection is rolled per
 * order and per nation with a bonus on the centre hex, which makes a deliberate second look at one
 * area a real tactic. This only makes the cost visible.
 * <p>
 * <b>What counts as a scout is the scenario's call, not this class's.</b> The filter is
 * {@code AcaoFacade.isScout}, i.e. the {@code ;ASR;} ability, which the data puts on Scout Area, Recon
 * Area, Map Area and the scry-area spell. The scrying ARTIFACT order is deliberately NOT in that set:
 * what the item does depends on the item - it may map an area, it may hunt for characters, it may do
 * something else entirely - so it cannot be assumed to uncover a 7-hex disc and must not be drawn as
 * one. Do not "fix" that by adding {@code ;ASR;} to it.
 */
public final class ScoutFootprint {

    private static final Log log = LogFactory.getLog(ScoutFootprint.class);

    /** Hexes uncovered around the centre. Fixed by the Judge at 1 (centre + 6 neighbours = 7 hexes). */
    public static final int SCOUT_RADIUS = 1;

    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();
    private static final LocalFacade localFacade = new LocalFacade();

    /**
     * Last computed picture, so the hex-info panel can ask "who is scouting this hex?" on every click
     * without recomputing. Refreshed by {@code MapaControler.refreshScoutOverlay} on ACTIONS_MAP_REDRAW,
     * which is fired on every order save, clear and EGF load - the only things that can change it.
     * <p>
     * Deliberately kept even when the map overlay is switched off: the toggle hides the drawing, not
     * the advice.
     */
    private static volatile Coverage current = new Coverage(
            Collections.<Ring>emptyList(), Collections.<Local, List<Ring>>emptyMap());

    private ScoutFootprint() {
    }

    public static void setCurrent(Coverage coverage) {
        current = (coverage == null)
                ? new Coverage(Collections.<Ring>emptyList(), Collections.<Local, List<Ring>>emptyMap())
                : coverage;
    }

    /** Never null; empty until the first refresh. */
    public static Coverage getCurrent() {
        return current;
    }

    /** One queued scout order: where it lands and what it will uncover. */
    public static final class Ring {

        private final Local centre;
        private final Set<Local> hexes;
        private final boolean mine;
        private final String actorName;

        Ring(Local centre, Set<Local> hexes, boolean mine, String actorName) {
            this.centre = centre;
            this.hexes = hexes;
            this.mine = mine;
            this.actorName = actorName;
        }

        public Local getCentre() {
            return centre;
        }

        public Set<Local> getHexes() {
            return hexes;
        }

        /** True for the player's own characters, false for an ally's (drawn in a different colour). */
        public boolean isMine() {
            return mine;
        }

        public String getActorName() {
            return actorName;
        }
    }

    /** Every queued scout footprint, plus which hexes more than one of them covers. */
    public static final class Coverage {

        private final List<Ring> rings;
        private final Map<Local, List<Ring>> byHex;

        Coverage(List<Ring> rings, Map<Local, List<Ring>> byHex) {
            this.rings = rings;
            this.byHex = byHex;
        }

        public List<Ring> getRings() {
            return rings;
        }

        /** The scouts covering one hex; empty when none do. Never null. */
        public List<Ring> getRingsAt(Local hex) {
            final List<Ring> at = byHex.get(hex);
            return at == null ? Collections.<Ring>emptyList() : at;
        }

        /** Hexes two or more queued scouts both uncover - the wasted ground. */
        public List<Local> getOverlappedHexes() {
            final List<Local> ret = new ArrayList<>();
            for (Map.Entry<Local, List<Ring>> e : byHex.entrySet()) {
                if (e.getValue().size() > 1) {
                    ret.add(e.getKey());
                }
            }
            return ret;
        }

        public boolean isEmpty() {
            return rings.isEmpty();
        }
    }

    /**
     * Build the coverage picture for every scout order queued by anyone the player can see.
     * <p>
     * Allied characters are only in {@code pcs} once team orders have been loaded, so a player who has
     * not loaded them sees own-nation overlap only. That is a smaller picture, never a wrong one.
     *
     * @param pcs      every character in the world (own, allied, and others)
     * @param observer the active player, used to split own from allied
     * @param locais   the hex index, for resolving order parameters and neighbours
     */
    public static Coverage compute(Collection<Personagem> pcs, Jogador observer, SortedMap<String, Local> locais) {
        final List<Ring> rings = new ArrayList<>();
        final Map<Local, List<Ring>> byHex = new HashMap<>();
        if (pcs == null || locais == null) {
            return new Coverage(rings, byHex);
        }
        for (Personagem pers : pcs) {
            if (pers.getLocal() == null) {
                continue;
            }
            final boolean mine = jogadorFacade.isMine(pers, observer);
            if (!mine && !jogadorFacade.isAlly(pers, observer)) {
                // an enemy's orders are never in our EGF; this only guards the merged-world case
                continue;
            }
            // entrySet, not values(): the slot number is the MAP KEY. PersonagemOrdem.index is dead
            // weight - nothing in the codebase ever writes it, so it is always 0.
            for (Map.Entry<Integer, PersonagemOrdem> slot : pers.getAcoes().entrySet()) {
                final PersonagemOrdem po = slot.getValue();
                // One unresolvable action must never cost the whole overlay - same contract the map
                // render keeps, where a single bad order used to be able to break opening an EGF.
                try {
                    if (!acaoFacade.isScout(po)) {
                        continue;
                    }
                    final Local centre = resolveCentre(pers, po, locais);
                    if (centre == null) {
                        continue;
                    }
                    final Ring ring = new Ring(centre, footprint(centre, locais), mine, pers.getNome());
                    rings.add(ring);
                    if (log.isDebugEnabled()) {
                        // one line per drawn footprint: the fast way to answer "why is there a ring there?"
                        log.debug(String.format("SCOUT ring: %s slot=%d order=%s centre=%s %s",
                                pers.getNome(), slot.getKey(),
                                po.getOrdem() == null ? "?" : po.getOrdem().getNome(),
                                centre.getCodigo(), mine ? "mine" : "ally"));
                    }
                    for (Local hex : ring.getHexes()) {
                        List<Ring> at = byHex.get(hex);
                        if (at == null) {
                            at = new ArrayList<>(2);
                            byHex.put(hex, at);
                        }
                        at.add(ring);
                    }
                } catch (RuntimeException ex) {
                    // skip this one order, keep the rest of the overlay
                }
            }
        }
        final Coverage ret = new Coverage(rings, byHex);
        if (log.isDebugEnabled() && !rings.isEmpty()) {
            int mineCount = 0;
            for (Ring r : rings) {
                if (r.isMine()) {
                    mineCount++;
                }
            }
            log.debug(String.format("SCOUT overlay: %d rings (%d mine, %d ally), %d hexes overlapped",
                    rings.size(), mineCount, rings.size() - mineCount, ret.getOverlappedHexes().size()));
        }
        return ret;
    }

    /**
     * Where the order will actually go off.
     * <p>
     * Map Area carries a hex parameter. Recon Area and Scout Area carry none, and go off wherever the
     * character ends the turn - which is why the fallback is the movement destination rather than the
     * current hex. That ordering is not a guess: the Judge queues actions by order
     * number ({@code PartidaControl.addOrdensEnviadas} keys on {@code getNuOrdem() * 100000 + seq}),
     * and every scout order sits above both Move Character and the movement milestone, so movement has
     * always resolved by the time a scout fires.
     */
    private static Local resolveCentre(Personagem pers, PersonagemOrdem po, SortedMap<String, Local> locais) {
        final Local target = acaoFacade.getLocalDestination(pers, po, locais);
        if (target != null) {
            return target;
        }
        return personagemFacade.getLocalDestination(pers, locais);
    }

    /**
     * The centre and its six neighbours. Uses the same range function the hex pickers filter with, so
     * the shading can never disagree with the distances the rest of the client quotes, and hexes off
     * the edge of the map simply are not in the index and drop out with no special case.
     */
    private static Set<Local> footprint(Local centre, SortedMap<String, Local> locais) {
        return new HashSet<>(localFacade.getLocalRange(centre, SCOUT_RADIUS, false, locais).keySet());
    }
}
