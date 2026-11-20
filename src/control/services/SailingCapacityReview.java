package control.services;

import baseLib.BaseModel;
import business.facade.CenarioFacade;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import business.facade.OrdemFacade;
import business.services.RecruitmentRules;
import control.services.TurnReview.Finding;
import control.services.TurnReview.Kind;
import control.services.TurnReview.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import model.Cidade;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.Pelotao;
import model.Personagem;
import model.PersonagemOrdem;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SysApoio;
import persistenceCommons.XmlManager;

/** A read-only capacity audit, not a simulation or promise of successful order execution. */
public final class SailingCapacityReview {
    private static final OrdemFacade ORDERS = new OrdemFacade();
    private static final ExercitoFacade ARMIES = new ExercitoFacade();
    private static final CenarioFacade SCENARIO = new CenarioFacade();
    private static final LocalFacade LOCALS = new LocalFacade();
    private static final Log log = LogFactory.getLog(SailingCapacityReview.class);
    private static final Set<String> RECRUIT = Set.of("411", "413");
    private static final Set<String> OTHER_CHANGES = Set.of("401", "414", "425", "428",
            "728", "729", "741", "743", "745", "747", "782", "798");

    private SailingCapacityReview() { }

    private record Change(BaseModel actor, PersonagemOrdem order) {
        String cause() {
            return actor.getNome() + " / " + order.getOrdem().getNome();
        }
    }

    private record Load(float burden, long ownCapacity, long garrisonCapacity) {
        boolean overloaded() {
            return burden > ownCapacity + garrisonCapacity;
        }
    }

    public static List<Finding> compute(Collection<BaseModel> actors, Jogador observer,
            Partida game, Collection<Exercito> armies, AtomicInteger skipped) {
        List<Finding> out = new ArrayList<>();
        if (actors == null) return out;
        // Resolve identity in the whole visible set before applying ownership/life prerequisites.
        List<Personagem> visible = actors.stream().filter(a -> a instanceof Personagem)
                .map(a -> (Personagem) a).distinct().toList();
        List<BaseModel> owned = new ArrayList<>();
        for (BaseModel actor : actors) {
            try {
                if (actor == null) throw new IllegalArgumentException("Missing review actor");
                if (ORDERS.isAtivo(observer, actor)) owned.add(actor);
            } catch (RuntimeException ex) {
                unreadable(skipped, ex);
            }
        }
        for (BaseModel base : owned) {
            if (!(base instanceof Personagem actor)) continue;
            for (Map.Entry<Integer, PersonagemOrdem> entry : actor.getAcoes().entrySet()) {
                if (!sailing(entry.getValue())) continue;
                try {
                    audit(out, actor, entry.getKey(), entry.getValue(), owned, visible, game, armies);
                } catch (RuntimeException ex) {
                    unreadable(skipped, ex);
                }
            }
        }
        return out;
    }

    private static void unreadable(AtomicInteger skipped, RuntimeException ex) {
        skipped.incrementAndGet();
        log.warn("Unreadable sailing-capacity review input", ex);
    }

    private static void audit(List<Finding> out, Personagem actor, int slot,
            PersonagemOrdem sail, List<BaseModel> owned, List<Personagem> visible,
            Partida game, Collection<Exercito> armies) {
        if (game == null || game.getCenario() == null || actor.getLocal() == null
                || actor.getNacao() == null || armies == null) throw new IllegalArgumentException();
        Exercito army = actor.getExercito();
        if (army != null && army.getComandante() != actor) return;
        SortedMap<String, Pelotao> roster = copy(army);
        Exercito garrison = garrison(actor, armies);
        SortedMap<String, Pelotao> port = copy(garrison);
        Load current = load(roster, port, game);
        List<Change> changes = changes(actor, sail, owned, visible);
        Change cause = changes.isEmpty() ? null : changes.get(0);
        boolean certain = changes.size() <= 1 && number(sail.getOrdem()) > 0;
        if (!certain && cause == null) cause = new Change(actor, sail);
        if (cause != null) {
            certain &= number(cause.order().getOrdem()) > 0
                    && number(cause.order().getOrdem()) < number(sail.getOrdem());
            if (certain) {
                try {
                    String code = code(cause.order());
                    if (RECRUIT.contains(code) && cause.actor() == actor) {
                        certain = recruit(actor, cause.order(), sail, owned, game, roster);
                    } else if ("424".equals(code) && cause.actor() instanceof Personagem source) {
                        certain = transfer(source, actor, cause.order(), owned, visible, roster, port);
                    } else {
                        certain = false;
                    }
                } catch (RuntimeException ex) {
                    // A malformed order is visible uncertainty, not a silently assumed successful change.
                    certain = false;
                }
            }
        }
        Load projected = certain ? load(roster, port, game) : current;
        if (projected.burden() > projected.ownCapacity() && projected.garrisonCapacity() > 0) {
            Change competitor = competingSail(actor, sail, owned, visible, game);
            if (competitor != null) {
                certain = false;
                cause = competitor;
            }
        }
        if (!certain) {
            add(out, current.overloaded() ? Level.WARN : Level.INFO,
                    Kind.SAIL_CAPACITY_UNCERTAIN, actor, slot, current, cause.cause());
        } else if (projected.overloaded()) {
            add(out, Level.WARN, cause == null ? Kind.SAIL_CAPACITY : Kind.SAIL_CAPACITY_PLANNED,
                    actor, slot, projected, cause == null ? null : cause.cause());
        }
    }

    private static List<Change> changes(Personagem sailor, PersonagemOrdem sail, List<BaseModel> owned,
            List<Personagem> visible) {
        List<Change> out = new ArrayList<>();
        for (BaseModel actor : owned) {
            for (PersonagemOrdem order : actor.getAcoes().values()) {
                if (order == null || order == sail || order.getOrdem() == null) continue;
                String code = code(order);
                if (!RECRUIT.contains(code) && !OTHER_CHANGES.contains(code)
                        && !"424".equals(code) && !sailing(order)) continue;
                boolean ownArmy = actor == sailor || actor instanceof Personagem p
                        && sailor.getExercito() != null && p.getExercito() == sailor.getExercito();
                boolean samePort = actor.getNacao() == sailor.getNacao()
                        && sameHex(local(actor), sailor.getLocal());
                boolean relevant = ownArmy && (RECRUIT.contains(code) || OTHER_CHANGES.contains(code)
                        || sailing(order) || "424".equals(code));
                if ("424".equals(code) || "428".equals(code)) {
                    String target = targetParameter(order);
                    relevant |= resolveTarget(visible, target) == sailor;
                    relevant |= samePort && garrisonTarget(target);
                }
                // A city's recruitment changes its garrison, not the character's existing army.
                relevant |= samePort && actor instanceof Cidade
                        && (RECRUIT.contains(code) || OTHER_CHANGES.contains(code));
                relevant |= actor.getNacao() == sailor.getNacao() && "401".equals(code)
                        && sailor.getLocal().getCidade() != null;
                if (relevant && (number(order.getOrdem()) <= 0
                        || number(order.getOrdem()) <= number(sail.getOrdem()))) out.add(new Change(actor, order));
            }
        }
        return out;
    }

    private static boolean recruit(Personagem actor, PersonagemOrdem order, PersonagemOrdem sail,
            List<BaseModel> owned, Partida game, SortedMap<String, Pelotao> roster) {
        Cidade city = actor.getLocal().getCidade();
        Nacao nation = actor.getNacao();
        if (!SCENARIO.hasResourceManagement(game.getCenario()) || city == null
                || city.getNacao() != nation || city.getRaca() == null || nation.getRaca() == null) return false;
        // Shared city availability is not independently reusable by several recruitment orders.
        int requests = 0;
        for (BaseModel candidate : owned) {
            if (candidate.getNacao() != nation) continue;
            for (PersonagemOrdem plan : candidate.getAcoes().values()) {
                if (plan == null || plan.getOrdem() == null
                        || number(plan.getOrdem()) > number(sail.getOrdem())) continue;
                if ("401".equals(code(plan)) || RECRUIT.contains(code(plan))
                        && sameHex(local(candidate), actor.getLocal())) requests++;
            }
        }
        if (requests != 1) return false;
        TipoTropa troop = game.getCenario().getTipoTropas().values().stream()
                .filter(t -> parameter(order, 1).equals(t.getCodigo())).findFirst().orElse(null);
        if (troop == null || !SCENARIO.isTropaRecruitable(game.getCenario(), city.getRaca(),
                nation.getRaca(), troop, city)) return false;
        float factor = troop.getFactorRecruits(game.isNewRules());
        if (!(factor > 0) || !Float.isFinite(factor)) return false;
        int citySlots = RecruitmentRules.citySlots(city.getTamanho(), city.getRaca() == nation.getRaca(), nation);
        int slots = Math.min(citySlots, RecruitmentRules.orderSlots(citySlots, game, nation));
        int maximum = Math.max(0, (int) (slots / factor));
        int requested = SysApoio.parseInt(parameter(order, 0));
        int quantity = requested <= 0 ? maximum : Math.min(requested, maximum);
        addTroops(roster, troop, quantity);
        return true;
    }

    private static boolean transfer(Personagem source, Personagem sailor, PersonagemOrdem order,
            List<BaseModel> owned, List<Personagem> visible, SortedMap<String, Pelotao> roster,
            SortedMap<String, Pelotao> port) {
        if (source.getExercito() == null || source.getExercito().getComandante() != source
                || !sameHex(source.getLocal(), sailor.getLocal()) || source.getNacao() != sailor.getNacao()
                || !LOCALS.isAncoravel(source.getLocal())) return false;
        String targetId = targetParameter(order);
        boolean toPort = garrisonTarget(targetId);
        Personagem target = toPort ? null : resolveTarget(visible, targetId);
        if (!toPort && (target == null || target.getVida() <= 0 || target.getNacao() != source.getNacao()
                || !target.isComandante()
                || !sameHex(target.getLocal(), source.getLocal()) || target == source
                || target.getExercito() == source.getExercito())) return false;
        if (source != sailor && target != sailor && !toPort) return false;
        // A preceding change on either endpoint makes a one-transfer projection ambiguous.
        for (BaseModel actor : owned) {
            for (PersonagemOrdem earlier : actor.getAcoes().values()) {
                if (earlier == null || earlier == order || earlier.getOrdem() == null) continue;
                String code = code(earlier);
                boolean changesRoster = RECRUIT.contains(code) || OTHER_CHANGES.contains(code)
                        || "424".equals(code) || sailing(earlier);
                if (!changesRoster) continue;
                boolean endpoint = sameArmy(actor, source) || target != null && sameArmy(actor, target);
                Personagem recipient = "424".equals(code) || "428".equals(code)
                        ? resolveTarget(visible, targetParameter(earlier)) : null;
                boolean incoming = recipient == source || target != null && recipient == target;
                if ((endpoint || incoming) && number(earlier.getOrdem()) <= number(order.getOrdem())) return false;
            }
        }
        Object parsed = XmlManager.getInstance().fromXml(parameter(order, 1));
        if (!(parsed instanceof Map<?, ?> requested)) return false;
        SortedMap<String, Pelotao> from = source == sailor ? copy(roster) : copy(source.getExercito());
        SortedMap<String, Pelotao> to = toPort ? copy(port)
                : target == sailor ? copy(roster) : copy(target.getExercito());
        for (Map.Entry<?, ?> entry : requested.entrySet()) {
            if (!(entry.getKey() instanceof String code) || !(entry.getValue() instanceof Integer quantity)
                    || quantity < 0) return false;
            Pelotao platoon = from.get(code);
            if (platoon == null) continue;
            int moved = Math.min(quantity, platoon.getQtd());
            platoon.setQtd(platoon.getQtd() - moved);
            addTroops(to, platoon.getTipoTropa(), moved);
        }
        // Commit both endpoint copies only after the entire request has validated.
        if (source == sailor) { roster.clear(); roster.putAll(from); }
        if (target == sailor) { roster.clear(); roster.putAll(to); }
        if (toPort) { port.clear(); port.putAll(to); }
        return true;
    }

    private static Change competingSail(Personagem actor, PersonagemOrdem sail,
            List<BaseModel> owned, List<Personagem> visible, Partida game) {
        for (BaseModel base : owned) {
            if (!(base instanceof Personagem other) || other == actor || other.getNacao() != actor.getNacao()
                    || !sameHex(other.getLocal(), actor.getLocal()) || other.getExercito() == null
                    || other.getExercito().getComandante() != other) continue;
            SortedMap<String, Pelotao> roster = copy(other.getExercito());
            boolean currentlyNeedsShips = ARMIES.getTransportesBurden(roster) > capacity(roster, game, false);
            for (PersonagemOrdem order : other.getAcoes().values()) {
                if (sailing(order) && number(order.getOrdem()) <= number(sail.getOrdem())
                        && (currentlyNeedsShips || !changes(other, order, owned, visible).isEmpty())) return new Change(other, order);
            }
        }
        return null;
    }

    private static Load load(SortedMap<String, Pelotao> roster, SortedMap<String, Pelotao> port, Partida game) {
        float burden = ARMIES.getTransportesBurden(roster);
        if (!Float.isFinite(burden) || burden < 0) throw new IllegalArgumentException();
        return new Load(burden, capacity(roster, game, false), capacity(port, game, true));
    }

    private static long capacity(SortedMap<String, Pelotao> roster, Partida game, boolean shipsOnly) {
        boolean variable = game.getCenario().isFirstAge() || game.isDeathMatch();
        long capacity = 0;
        for (Pelotao p : roster.values()) {
            TipoTropa type = p.getTipoTropa();
            if (shipsOnly && !type.isBarcos()) continue;
            if (variable) capacity += ARMIES.getTransportesCapacity(p);
            else if (type.hasHabilidade(";TTT;") || type.hasHabilidade(";TTT5;")) {
                capacity += (long) p.getQtd() * ExercitoFacade.SHIP_CAPACITY;
            }
        }
        if (capacity < 0) throw new IllegalArgumentException();
        return capacity;
    }

    private static SortedMap<String, Pelotao> copy(Exercito army) {
        return copy(army == null ? new TreeMap<>() : army.getPelotoes());
    }

    private static SortedMap<String, Pelotao> copy(SortedMap<String, Pelotao> original) {
        SortedMap<String, Pelotao> copy = new TreeMap<>();
        for (Pelotao p : original.values()) {
            if (p == null || p.getTipoTropa() == null || p.getQtd() < 0) throw new IllegalArgumentException();
            addTroops(copy, p.getTipoTropa(), p.getQtd());
        }
        return copy;
    }

    private static void addTroops(SortedMap<String, Pelotao> roster, TipoTropa type, int quantity) {
        if (quantity == 0) return;
        Pelotao p = roster.computeIfAbsent(Objects.requireNonNull(type.getCodigo()), key -> {
            Pelotao created = new Pelotao(); created.setTipoTropa(type); return created;
        });
        p.setQtd(Math.addExact(p.getQtd(), quantity));
    }

    private static Exercito garrison(Personagem actor, Collection<Exercito> armies) {
        Exercito found = null;
        for (Exercito army : armies) {
            if (army == null || !army.isGarrison() || army.getNacao() != actor.getNacao()
                    || !sameHex(army.getLocal(), actor.getLocal())) continue;
            if (found != null && found != army) throw new IllegalArgumentException("Ambiguous garrison");
            found = army;
        }
        return found;
    }

    private static boolean sameHex(Local a, Local b) {
        return a != null && b != null && a.getCodigo() != null && a.getCodigo().equals(b.getCodigo());
    }

    private static boolean sameArmy(BaseModel candidate, Personagem endpoint) {
        return candidate == endpoint || candidate instanceof Personagem p
                && endpoint.getExercito() != null && p.getExercito() == endpoint.getExercito();
    }

    private static Local local(BaseModel actor) {
        return actor instanceof Personagem p ? p.getLocal() : actor instanceof Cidade c ? c.getLocal() : null;
    }

    private static String parameter(PersonagemOrdem order, int index) {
        return rawParameter(order, index).trim();
    }

    private static String rawParameter(PersonagemOrdem order, int index) {
        if (order.getParametrosId() == null || index >= order.getParametrosId().size()) return "";
        String value = order.getParametrosId().get(index);
        return value == null ? "" : value;
    }

    private static String targetParameter(PersonagemOrdem order) {
        String raw = rawParameter(order, 0).stripLeading();
        if (garrisonTarget(raw.stripTrailing())) return raw.stripTrailing();
        // The fifth character can be a significant space, including in an editable name prefix.
        return ident(raw.length() > 5 ? raw.substring(0, 5) : raw);
    }

    private static String ident(String stored) {
        return stored == null ? null : SysApoio.removeAcentos(stored).toLowerCase(Locale.ROOT);
    }

    private static Personagem resolveTarget(List<Personagem> visible, String wanted) {
        if (garrisonTarget(wanted)) return null;
        List<Personagem> exact = visible.stream().filter(p -> wanted.equals(ident(p.getCodigo()))).toList();
        if (!exact.isEmpty()) return exact.size() == 1 ? exact.get(0) : null;
        if (wanted.length() >= 5) return null;
        String padded = wanted + " ".repeat(5 - wanted.length());
        List<Personagem> fallback = visible.stream().filter(p -> padded.equals(ident(p.getCodigo()))).toList();
        return fallback.size() == 1 ? fallback.get(0) : null;
    }

    private static boolean garrisonTarget(String value) {
        return value.isEmpty() || "Guarnicao".equalsIgnoreCase(value);
    }

    private static boolean sailing(PersonagemOrdem order) {
        return order != null && order.getOrdem() != null && order.getOrdem().hasHabilidade(";AMW;");
    }

    private static String code(PersonagemOrdem order) {
        return Objects.toString(order.getOrdem().getCodigo(), "");
    }

    private static int number(Ordem order) {
        if (order.getNumero() > 0) return order.getNumero();
        try {
            int number = Integer.parseInt(order.getCodigo());
            return number > 0 ? number : -1;
        } catch (NumberFormatException ex) {
            return -1; // Unknown precedence qualifies a relevant plan; it never hides the snapshot.
        }
    }

    private static void add(List<Finding> out, Level level, Kind kind, Personagem actor,
            int slot, Load load, String cause) {
        List<String> args = new ArrayList<>(List.of(format(load.burden()), Long.toString(load.ownCapacity()),
                Long.toString(load.garrisonCapacity())));
        if (cause != null) args.add(cause);
        out.add(new Finding(level, kind, actor, slot, args.toArray(String[]::new)));
    }

    private static String format(float value) {
        return value == (long) value ? Long.toString((long) value) : Float.toString(value);
    }
}
