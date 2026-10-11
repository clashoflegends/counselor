/*
 * TurnReview.java
 */
package control.services;

import baseLib.BaseModel;
import business.facade.AcaoFacade;
import business.facade.CidadeFacade;
import business.facade.JogadorFacade;
import business.facade.LocalFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import model.Cidade;
import model.Jogador;
import model.Local;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * One pass over the WHOLE planned turn, producing a list of things worth a second look before the
 * player submits.
 *
 * <b>Why this exists at all.</b> The client already warns about several of these, but each warning
 * fires once, at a different moment, and then is gone: the missing-slot toast at save time, the
 * {@code ;SCC;} growth-cap question at the moment one order is saved, the convergence markers on the
 * map. A player with twenty actors has no single place that answers "is my turn ready to send". The
 * worst of it is {@code WorldControler.doSaveActorActions}, whose loop OVERWRITES its message every
 * iteration, so with five actors left empty the player is told about one of them. {@link
 * #hasOpenSlot} is now the one definition of that test and both callers use it.
 *
 * <b>What this deliberately does NOT do.</b> It is not an oracle for the Judge. The blank-target and
 * blank-sole-param guards in {@code OrdemControler.doSaveOrder} REFUSE at save time, so such an order
 * can never reach the saved set and re-checking it here would be dead code. Everything here is a
 * warning and nothing blocks a save.
 * <p>
 * <b>One method here IS on the save path:</b> {@link #hasOpenSlot}, which {@code doSaveActorActions}
 * calls so the save-time toast and this list cannot drift apart. {@link #compute} and the dialog are
 * not, so a bug in a check can cost a confusing row but never an hour of orders.
 *
 * <b>Findings are data, not text.</b> A {@link Finding} carries a {@link Kind} and its arguments; the
 * dialog renders them from the bundle. That keeps the checks unit-testable without a label bundle and
 * keeps the wording in one place.
 *
 * @author jmoura
 */
public final class TurnReview implements Serializable {

    private static final Log log = LogFactory.getLog(TurnReview.class);

    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();
    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();

    /** Amber for something that will probably cost the player an action; blue for a nudge. */
    public enum Level {
        WARN, INFO
    }

    public enum Kind {
        /** An active actor still has an order slot free. */
        OPEN_SLOT,
        /** A nation has not spent its whole setup package budget. */
        OPEN_PACKAGE,
        /** An Improve City Size order whose city is capped by a neighbour right now. */
        SIZE_CAP,
        /** A MovUnir whose destination is not where the target will end the turn. */
        RENDEZVOUS,
        /** Two players walking into the same empty hex. */
        CONVERGE
    }

    public static final class Finding implements Serializable {

        private final Level level;
        private final Kind kind;
        /** The actor OBJECT, never its name: names are reused inside a game (KI-051). */
        private final transient BaseModel actor;
        /** The order slot this is about, or -1 when the finding is about the actor as a whole. */
        private final int slot;
        private final String[] args;

        Finding(Level level, Kind kind, BaseModel actor, int slot, String... args) {
            this.level = level;
            this.kind = kind;
            this.actor = actor;
            this.slot = slot;
            this.args = args == null ? new String[0] : args.clone();
        }

        public Level getLevel() {
            return level;
        }

        public Kind getKind() {
            return kind;
        }

        public BaseModel getActor() {
            return actor;
        }

        public int getSlot() {
            return slot;
        }

        public String[] getArgs() {
            return args.clone();
        }

        @Override
        public String toString() {
            return kind + "/" + (actor == null ? "-" : actor.getNome()) + "@" + slot;
        }
    }

    private TurnReview() {
    }

    /**
     * @param actors         every actor in the world, as {@code WorldFacadeCounselor.getActors()} gives
     *                       them; inactive ones and other players' are skipped here
     * @param observer       the active player
     * @param locais         the hex index, for resolving order parameters
     * @param world          every character the turn file carries, for the ident fallback in
     *                       {@link #byIdent}; the actor's own nation is always searched first
     * @param converging     hex to the names walking into it, from {@code MoveConvergence}; may be
     *                       empty, which is also what the player gets after silencing that overlay
     * @param ordensQtCheck  true when the scenario offers orders at all for an actor class; passing
     *                       the predicate in keeps this free of the Partida lookup and testable
     * @param hasCitySizeCap whether {@code ;SCC;} is switched on for this game at all. Passed in for
     *                       the same reason as the predicate above, and REQUIRED: see
     *                       {@link #addSizeCap}, where omitting it warned in every game.
     */
    public static List<Finding> compute(Collection<BaseModel> actors, Jogador observer,
            SortedMap<String, Local> locais, Collection<Personagem> world,
            Map<Local, List<String>> converging, ActorOffersOrders ordensQtCheck,
            int nationPackagesLimit, boolean hasCitySizeCap) {
        final List<Finding> ret = new ArrayList<>();
        if (actors != null) {
            for (BaseModel actor : actors) {
                if (!ordemFacade.isAtivo(observer, actor)) {
                    continue;
                }
                // One unreadable actor must never cost the whole review.
                try {
                    addOpenSlot(ret, actor, ordensQtCheck, nationPackagesLimit);
                    addOrderFindings(ret, actor, observer, locais, world, hasCitySizeCap,
                            ordensQtCheck, nationPackagesLimit);
                } catch (RuntimeException ex) {
                    //A review is a convenience; never let one bad actor take the list down. LOGGED,
                    //not swallowed: a silent catch here is what hid a broken test fixture, and silent
                    //catches in Swing handlers are this codebase's most repeated bug.
                    log.warn(String.format("Skipping unreadable actor %s in turn review: %s",
                            actor.getNome(), ex));
                }
            }
        }
        addConverging(ret, converging);
        return ret;
    }

    /** True when the scenario offers this actor class any order at all. */
    public interface ActorOffersOrders {

        boolean test(BaseModel actor);
    }

    /**
     * The ONE definition of "this actor still has something unplanned", shared with {@code
     * WorldControler.doSaveActorActions} so the review and the save-time toast cannot disagree about
     * WHICH actors are unplanned. (The wording can still differ: see {@link #addOpenSlot}.)
     *
     * A nation counts points rather than slots: its setup is a package budget, not a list of orders.
     */
    public static boolean hasOpenSlot(BaseModel actor, ActorOffersOrders offersOrders,
            int nationPackagesLimit) {
        if (actor.getAcaoSize() <= 0) {
            return offersOrders != null && offersOrders.test(actor);
        }
        for (int index = 0; index < ordemFacade.getOrdemMax(actor); index++) {
            //getOrdem, not getAcao: it also answers null for a slot holding an action with no order,
            //which is the exact test doSaveActorActions has always used
            if (ordemFacade.getOrdem(actor, index) != null) {
                continue;
            }
            //An empty slot. For a NATION that is not enough on its own: its setup is a package budget,
            //so an empty slot with the budget already spent is finished, not unplanned. Keeping the
            //points test gated on an empty slot is also exactly what the save-time toast did before
            //this became shared, so nothing on the save path changed meaning.
            return !actor.isNacaoClass()
                    || acaoFacade.isPointsSetupUnderLimit(actor, nationPackagesLimit);
        }
        return false;
    }

    /**
     * The {@code && getAcaoSize() > 0} mirrors the save toast's key choice exactly: a nation that has
     * entered NOTHING is told it has orders to give, not that it has points to spend, because at that
     * stage the two are the same sentence and the shorter one is already the one it hears on save.
     */
    private static void addOpenSlot(List<Finding> out, BaseModel actor,
            ActorOffersOrders offersOrders, int nationPackagesLimit) {
        if (hasOpenSlot(actor, offersOrders, nationPackagesLimit)) {
            out.add(new Finding(Level.INFO,
                    actor.isNacaoClass() && actor.getAcaoSize() > 0
                            ? Kind.OPEN_PACKAGE : Kind.OPEN_SLOT,
                    actor, -1, actor.getNome()));
        }
    }

    private static void addOrderFindings(List<Finding> out, BaseModel actor, Jogador observer,
            SortedMap<String, Local> locais, Collection<Personagem> world, boolean hasCitySizeCap,
            ActorOffersOrders offersOrders, int nationPackagesLimit) {
        for (Map.Entry<Integer, PersonagemOrdem> entry : actor.getAcoes().entrySet()) {
            final PersonagemOrdem po = entry.getValue();
            if (po == null || po.getOrdem() == null) {
                continue;
            }
            addSizeCap(out, actor, observer, entry.getKey(), po, locais, hasCitySizeCap);
            addRendezvous(out, actor, entry.getKey(), po, locais, world, offersOrders,
                    nationPackagesLimit);
        }
    }

    /**
     * The {@code ;SCC;} proximity cap, re-asked at review time.
     *
     * {@code OrdemControler.confirmSizeCapOverride} asks ONCE per save gesture and takes yes for an
     * answer, and a city can become capped after its order was saved (a neighbour grows, or the player
     * plans the two in the other order). So this is not a duplicate of that question: it is the state
     * as it stands now, which is the only state worth reviewing.
     * <p>
     * <b>{@code hasCitySizeCap} is not optional.</b> {@code CidadeFacade.isCheckCitySizeCapToUpgrade}
     * is pure geometry - it measures the distance to the nearest big city and never looks at whether
     * the RULE is switched on - so without this gate it answers for every game, and this row tells the
     * player "the Judge will refuse this order" in games where the Judge will do no such thing. Every
     * other caller gates it the same way: {@code OrdemControler.getSizeCapBlocker},
     * {@code CidadeConverter}, {@code FiltroConverter}.
     */
    private static void addSizeCap(List<Finding> out, BaseModel actor, Jogador observer, int slot,
            PersonagemOrdem po, SortedMap<String, Local> locais, boolean hasCitySizeCap) {
        if (!hasCitySizeCap || !acaoFacade.isImproveCitySize(po)) {
            return;
        }
        final Local hex = localOf(actor);
        final Cidade city = hex == null ? null : hex.getCidade();
        if (city == null || !jogadorFacade.isMine(city, observer)) {
            return;
        }
        final Local blocker = cidadeFacade.isCheckCitySizeCapToUpgrade(city, locais);
        if (blocker == null) {
            return;
        }
        final Cidade blockerCity = blocker.getCidade();
        out.add(new Finding(Level.WARN, Kind.SIZE_CAP, actor, slot,
                cidadeFacade.getNome(city),
                blockerCity == null ? "?" : cidadeFacade.getNome(blockerCity),
                LocalFacade.getCoordenadas(blocker)));
    }

    /**
     * MovUnir: "move to hex X, then join character Y". The join fails unless the two are on the SAME
     * hex when the order runs, so the destination has to be where the target will END the turn.
     *
     * <b>Measured.</b> Across games 850 to 907, comparing the order's destination against
     * {@code personagem_turno.id_hex_final} - the hex the server actually left the target on - they
     * agreed in <b>15,448 of 15,461 successes (99.92%)</b> and in only <b>317 of 1,549
     * could-not-attempt failures (20.5%)</b>. Over the full population that rule would flag 1,232 of
     * the 1,628 MovUnir failures (76%) and 13 of the 15,888 successes (0.08%). MovUnir is the largest
     * source of could-not-attempt in the catalogue that is not deliberate (1,628 of 17,710 issued),
     * behind only PegArt, whose 76% failure rate IS the mechanic - a blind search.
     * <p>
     * <b>Read those figures as the ceiling, not as this check's error rate.</b> They score the RULE
     * against the server's own answer. The client has to ESTIMATE that answer from orders the player
     * has not submitted yet - {@code PersonagemFacade.getLocalDestination} with a still-planning
     * test, which this work had to correct first - and every case the estimate cannot reach is
     * handled by staying silent, which costs recall rather than precision. Two further reasons not to
     * read the separation too literally: {@code Ordem.executa870} runs {@code executa810} and then
     * builds a SEPARATE {@code Ordem755JoinLeader} whose outcome is never written back, so a row can
     * be stored SIM with the move done and the join refused; and {@code executa810} writes PRE for
     * its own failures, so some of the 1,628 are movement failures with no rendezvous in them.
     * Neither changes the direction of the signal, and both make it conservative here.
     *
     * <b>Do NOT "simplify" this to the target's CURRENT hex.</b> That was measured too and it is
     * wrong: 42% of SUCCESSFUL MovUnir orders have the target somewhere else at turn start, because
     * walking to a rendezvous is ordinary play. It would flag roughly 6,400 good orders.
     *
     * <b>Gated on parameter SHAPE, not order code.</b> A {@code Coordenada} followed by exactly
     * {@code Personagem_Nacao} is unique to MovUnir across the whole catalogue, and it covers its
     * codes (870 and 871, which the Judge routes to the same {@code executa870}; its direction-
     * parameterised variant excludes itself) without naming any of them. The client could not gate on
     * the code even if it wanted to: {@code model.Ordem} carries {@code nu_ordem}, never the
     * {@code cd_ordem} the Judge dispatches on.
     */
    private static void addRendezvous(List<Finding> out, BaseModel actor, int slot,
            PersonagemOrdem po, SortedMap<String, Local> locais, Collection<Personagem> world,
            ActorOffersOrders offersOrders, int nationPackagesLimit) {
        if (!(actor instanceof Personagem) || !isJoinLeaderShape(po)) {
            return;
        }
        final Local dest = acaoFacade.getLocalDestination((Personagem) actor, po, locais);
        if (dest == null) {
            return;
        }
        final Personagem target = byIdent(po.getParametrosId().get(1), (Personagem) actor, world);
        if (target == null) {
            //the combo is editable, so the parameter can hold text that names nobody. Saying nothing
            //beats inventing a verdict about a character we cannot find.
            return;
        }
        final Local plannedEnd = personagemFacade.getLocalDestination(target, locais,
                pc -> hasOpenSlot(pc, offersOrders, nationPackagesLimit));
        if (plannedEnd == null || dest.equals(plannedEnd)) {
            return;
        }
        out.add(new Finding(Level.WARN, Kind.RENDEZVOUS, actor, slot,
                actor.getNome(),
                LocalFacade.getCoordenadas(dest),
                target.getNome(),
                LocalFacade.getCoordenadas(plannedEnd)));
    }

    /**
     * A destination coordinate followed by an own-nation character, on a movement order: MovUnir.
     *
     * The second parameter is matched EXACTLY, not by {@code contains}. {@code Personagem_Nacao_Local}
     * and {@code Personagem_Nacao_Nonhero_Local} are still live deprecated aliases until 2027-07-01
     * (KI-026) and both still ship in real scenarios, so {@code contains} would sweep in any future
     * order shaped "march to a hex and do something to a character there" with no code change and no
     * test. {@code trim()} first, because the catalogue really does carry leading-space parameter
     * tokens. The {@code Mov} type is free to check and nails the gate down further.
     */
    static boolean isJoinLeaderShape(PersonagemOrdem po) {
        if (po.getOrdem().getParametrosIdeQtd() < 2 || po.getParametrosId() == null
                || po.getParametrosId().size() < 2 || !acaoFacade.isMovimento(po)) {
            return false;
        }
        return po.getOrdem().getParametroIde(0).trim().contains("Coordenada")
                && po.getOrdem().getParametroIde(1).trim().equals("Personagem_Nacao");
    }

    /**
     * The living character whose 5-character ident the parameter holds: the actor's OWN NATION first,
     * then the wider world but only on a single unambiguous match.
     *
     * Own nation first because that is what the picker offered ({@code
     * NacaoFacade.listPersonagemNacao}), so a same-nation match is certainly the character chosen.
     * The wider fallback exists because the combo is EDITABLE and joining an ALLY is legal, which
     * {@code Ordem755JoinLeader.criticaRequisitos} spells out - and it is not rare: 161 successful and
     * 208 could-not-attempt MovUnir orders across games 850 to 907 name a character of another
     * nation. Dropping the fallback would blind the check to about one in eight of the cases it
     * exists for.
     *
     * The fallback REFUSES to guess, which is what keeps it honest. An ident is released when its
     * holder dies and reused inside the same game (KI-051), and an autoloaded ally's turn file merges
     * their characters into the same world, so "first match wins" can answer with a corpse or with
     * the wrong character and pin the planned destination on the wrong body. A second living holder
     * therefore yields null and the check stays silent. Measured, that ambiguity is real but small:
     * 54 of 1,546 could-not-attempt rows and 253 of 15,453 successes.
     */
    private static Personagem byIdent(String ident, Personagem actor, Collection<Personagem> world) {
        if (ident == null || actor == null) {
            return null;
        }
        final String wanted = ident.trim();
        if (wanted.isEmpty()) {
            return null;
        }
        if (actor.getNacao() != null) {
            final Personagem mine = matchIn(actor.getNacao().getPersonagens(), wanted, actor);
            if (mine != null) {
                return mine;
            }
        }
        // Not in my nation. The parameter can still be legitimate: the picker's combo is EDITABLE,
        // and Ordem755JoinLeader explicitly allows joining a character of an ALLIED nation
        // (its NacaoAmigavelNao branch). Across games 850 to 907 that is 161 successful MovUnir
        // orders and 208 that could not be attempted, in 28 and 20 games - too much to ignore.
        //
        // Widening the search is only safe while the answer is UNambiguous, so a second holder of
        // the ident makes this give up rather than pick one. That is the whole risk being managed:
        // an ident is released when its holder dies and reused in the same game (KI-051), so a
        // "first match wins" search can answer with a corpse or with somebody else's character and
        // pin the planned destination on the wrong body.
        return uniqueMatchIn(world, wanted, actor);
    }

    private static Personagem matchIn(Collection<Personagem> candidates, String ident, Personagem actor) {
        if (candidates == null) {
            return null;
        }
        for (Personagem pers : candidates) {
            if (isLivingMatch(pers, ident, actor)) {
                return pers;
            }
        }
        return null;
    }

    /** The one living holder of this ident, or null when there is none or more than one. */
    private static Personagem uniqueMatchIn(Collection<Personagem> candidates, String ident,
            Personagem actor) {
        if (candidates == null) {
            return null;
        }
        Personagem found = null;
        for (Personagem pers : candidates) {
            if (!isLivingMatch(pers, ident, actor)) {
                continue;
            }
            if (found != null) {
                return null; // ambiguous: say nothing rather than name the wrong character
            }
            found = pers;
        }
        return found;
    }

    private static boolean isLivingMatch(Personagem pers, String ident, Personagem actor) {
        return pers != null && pers != actor && !personagemFacade.isMorto(pers)
                && pers.getCodigo() != null && pers.getCodigo().trim().equals(ident);
    }

    private static void addConverging(List<Finding> out, Map<Local, List<String>> converging) {
        if (converging == null) {
            return;
        }
        for (Map.Entry<Local, List<String>> hex : converging.entrySet()) {
            //Guarded per entry, not per pass: this runs OUTSIDE the per-actor loop, so without it one
            //unreadable hex would escape to the dialog's own catch and discard every finding computed
            //above it - the player would be told the turn is clean when it is not.
            try {
                out.add(new Finding(Level.INFO, Kind.CONVERGE, null, -1,
                        LocalFacade.getCoordenadas(hex.getKey()),
                        String.join(", ", hex.getValue())));
            } catch (RuntimeException ex) {
                log.warn(String.format("Skipping unreadable converging hex in turn review: %s", ex));
            }
        }
    }

    private static Local localOf(BaseModel actor) {
        if (actor instanceof Personagem) {
            return ((Personagem) actor).getLocal();
        }
        if (actor instanceof Cidade) {
            return ((Cidade) actor).getLocal();
        }
        return null;
    }
}
