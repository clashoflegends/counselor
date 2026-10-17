package control.services;

import baseLib.BaseModel;
import business.converter.ConverterFactory;
import business.facade.CenarioFacade;
import business.facade.OrdemFacade;
import business.services.RecruitmentRules;
import control.services.TurnReview.Finding;
import control.services.TurnReview.Kind;
import control.services.TurnReview.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Cidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Personagem;
import model.PersonagemOrdem;
import model.Produto;
import model.TipoTropa;

/** A demand audit against visible stocks, never a simulation of turn execution. */
public final class RecruitmentReview {
    private RecruitmentReview() { }
    private static final CenarioFacade SCENARIO = new CenarioFacade();
    private static final OrdemFacade ORDERS = new OrdemFacade();

    private static final class Budget {
        final Cidade city;
        final BaseModel actor;
        final int slot;
        final int capacity;
        long slots;
        long provision;
        final long[] recruit = new long[7];
        final long[] weapons = new long[7];
        final long[] armour = new long[7];
        Budget(Cidade city, BaseModel actor, int slot) {
            this.city = city;
            this.actor = actor;
            this.slot = slot;
            capacity = RecruitmentRules.citySlots(city.getTamanho(),
                    city.getRaca() == city.getNacao().getRaca(), city.getNacao());
        }
    }

    /** Findings hold actor identities for navigation; all arithmetic uses separate local ledgers. */
    public static List<Finding> compute(Collection<BaseModel> actors, Jogador observer, Partida game) {
        List<Finding> out = new ArrayList<>();
        Map<Cidade, Budget> cities = new LinkedHashMap<>();
        Map<Nacao, Long> gold = new IdentityHashMap<>();
        Map<Nacao, BaseModel> representatives = new IdentityHashMap<>();
        BaseModel first = null;
        if (actors == null) return out;
        for (BaseModel actor : actors) {
            try {
                if (actor == null) throw new IllegalArgumentException();
                if (!ORDERS.isAtivo(observer, actor)) continue;
                for (Map.Entry<Integer, PersonagemOrdem> entry : actor.getAcoes().entrySet()) {
                    PersonagemOrdem order = entry.getValue();
                    if (order == null || order.getOrdem() == null) continue;
                    String code = order.getOrdem().getCodigo();
                    if (!"411".equals(code) && !"413".equals(code) && !"401".equals(code)) continue;
                    if (first == null) first = actor;
                    try {
                        if (game == null || game.getCenario() == null) throw new IllegalArgumentException();
                        Nacao nation = actor.getNacao();
                        if (nation == null) throw new IllegalArgumentException();
                        boolean muster = "401".equals(code);
                        TipoTropa troop = troop(game, parameter(order, muster ? 0 : 1));
                        if (troop == null) throw new IllegalArgumentException();
                        if (muster) {
                            add(out, Level.INFO, Kind.RECRUIT_CONDITIONAL, actor, entry.getKey());
                            int minimum = nation.hasHabilidade(";PMT;") ? Math.min(4, nation.getHabilidadeValor(";PMT;"))
                                    : nation.hasHabilidadeNacao("0048") ? Math.min(4, nation.getHabilidadeNacaoValor("0048")) : 4;
                            for (Cidade city : nation.getCidades()) {
                                if (city.getTamanho() >= minimum && !city.isSitiado()) {
                                    audit(out, cities, gold, representatives, actor, entry.getKey(), order,
                                            city, nation, troop, game, true);
                                }
                            }
                        } else {
                            Local local = actor instanceof Cidade ? ((Cidade) actor).getLocal()
                                    : actor instanceof Personagem ? ((Personagem) actor).getLocal() : null;
                            if (local == null || local.getCidade() == null) throw new IllegalArgumentException();
                            audit(out, cities, gold, representatives, actor, entry.getKey(), order,
                                    local.getCidade(), nation, troop, game, false);
                        }
                    } catch (RuntimeException ex) {
                        // Do not expose raw input/exception text, or turn a partial pass into an all-clear.
                        add(out, Level.WARN, Kind.RECRUIT_INCOMPLETE, actor, entry.getKey());
                    }
                }
            } catch (RuntimeException ex) {
                add(out, Level.WARN, Kind.RECRUIT_INCOMPLETE, actor, -1);
            }
        }
        if (first != null) add(out, Level.INFO, Kind.RECRUIT_SCOPE, first, -1);
        for (Budget b : cities.values()) {
            try {
                finish(out, b, game);
            } catch (RuntimeException ex) {
                add(out, Level.WARN, Kind.RECRUIT_INCOMPLETE, b.actor, b.slot);
            }
        }
        for (Map.Entry<Nacao, Long> e : gold.entrySet()) {
            if (e.getValue() > e.getKey().getMoney()) {
                add(out, Level.WARN, Kind.RECRUIT_GOLD, representatives.get(e.getKey()), -1,
                        Long.toString(e.getValue()), Integer.toString(e.getKey().getMoney()));
            }
        }
        return out;
    }

    private static void audit(List<Finding> out, Map<Cidade, Budget> cities, Map<Nacao, Long> gold,
            Map<Nacao, BaseModel> representatives, BaseModel actor, int slot, PersonagemOrdem order,
            Cidade city, Nacao nation, TipoTropa troop, Partida game, boolean muster) {
        // Allied/hidden city stores are not an owned-city budget and must not be invented.
        if (city.getNacao() != nation || city.getRaca() == null || nation.getRaca() == null) {
            throw new IllegalArgumentException();
        }
        if (!SCENARIO.isTropaRecruitable(game.getCenario(), city.getRaca(), nation.getRaca(), troop, city)) {
            add(out, Level.WARN, Kind.RECRUIT_ELIGIBILITY, actor, slot, city.getNome(), troop.getNome());
            // Food may still be transferred to an existing army after recruitment fails.
            if (!muster && SCENARIO.hasResourceManagement(game.getCenario())) {
                Budget b = cities.computeIfAbsent(city, c -> new Budget(c, actor, slot));
                b.provision += Math.max(0, Integer.parseInt(parameter(order, 4).trim()));
            }
            return;
        }
        Budget b = cities.computeIfAbsent(city, c -> new Budget(c, actor, slot));
        float factor = troop.getFactorRecruits(game.isNewRules());
        if (!(factor > 0) || b.capacity < 0) throw new IllegalArgumentException();
        int limit = muster ? b.capacity : Math.min(b.capacity, RecruitmentRules.orderSlots(b.capacity, game, nation));
        int maximum = Math.max(0, (int) (limit / factor));
        int requested = muster ? 0 : Integer.parseInt(parameter(order, 0).trim());
        int quantity = requested <= 0 ? maximum : Math.min(requested, maximum);
        if (requested > maximum) {
            add(out, Level.WARN, Kind.RECRUIT_LIMIT, actor, slot, Integer.toString(requested),
                    Integer.toString(maximum), city.getNome());
        }
        // Preserve ordinary truncation vs Muster's rounding. These are slot units, not troop counts.
        long slots = muster ? Math.round(quantity * factor) : (int) (quantity * factor);
        boolean resources = SCENARIO.hasResourceManagement(game.getCenario());
        if (!resources && (muster || "413".equals(order.getOrdem().getCodigo()))) {
            throw new IllegalArgumentException(); // distinct nonstandard adapters, never assume free equipment
        }
        long[] bill = new long[7];
        int weapon = 0;
        int armour = 0;
        int provision = 0;
        if (resources) {
            if (city.getLocal() == null || city.getLocal().getTerreno() == null) throw new IllegalArgumentException();
            RecruitmentRules.Materials materials = RecruitmentRules.materials(troop, nation, game.isNewRules(),
                    city.isCapital() && game.getCenario().hasHabilidade(";SNC;"),
                    city.getTamanho() >= 4, city.getLocal().getTerreno().isDeserto(),
                    nation.hasHabilidadeNacao("0017"), nation.hasHabilidadeNacao("0018"));
            bill = materials.demand(quantity);
            if (muster) {
                provision = Math.multiplyExact(quantity, 4);
            } else {
                weapon = ConverterFactory.produtoToInt(parameter(order, 2));
                armour = ConverterFactory.produtoToInt(parameter(order, 3));
                provision = Math.max(0, Integer.parseInt(parameter(order, 4).trim()));
            }
            if (troop.getFactorWeapon(game.isNewRules()) < 0 || troop.getFactorArmor(game.isNewRules()) < 0)
                throw new IllegalArgumentException();
            for (long amount : bill) if (amount < 0) throw new IllegalArgumentException();
        }
        long cost = muster ? 0 : (long) quantity * troop.getRecruitCostMoney();
        if (!muster && nation.hasHabilidade(";NDH;")) cost = cost * (100 - nation.getHabilidadeValor(";NDH;")) / 100;
        if (cost < 0) throw new IllegalArgumentException();
        // Commit a complete quote atomically, so malformed input cannot leave a half-reserved order.
        b.slots += slots;
        b.provision += provision;
        for (int i = 0; i < 7; i++) b.recruit[i] += bill[i];
        if (weapon > 0) b.weapons[weapon - 1] += (long) quantity * troop.getFactorWeapon(game.isNewRules());
        if (armour > 0) b.armour[armour - 1] += (long) quantity * troop.getFactorArmor(game.isNewRules());
        if (!muster) {
            gold.merge(nation, cost, Long::sum);
            representatives.putIfAbsent(nation, actor);
        }
    }

    private static void finish(List<Finding> out, Budget b, Partida game) {
        if (b.slots > b.capacity) add(out, Level.WARN, Kind.RECRUIT_CAPACITY, b.actor, b.slot,
                b.city.getNome(), Long.toString(b.slots), Integer.toString(b.capacity));
        if (!SCENARIO.hasResourceManagement(game.getCenario())) return;
        for (int i = 0; i < 7; i++) {
            Produto product = product(game, i);
            long need = b.recruit[i] + b.weapons[i] + b.armour[i] + (i == 4 ? b.provision : 0);
            if (need == 0) continue;
            if (product == null || !b.city.getEstoques().containsKey(product)
                    || b.city.getEstoques().get(product) == null) {
                add(out, Level.WARN, Kind.RECRUIT_INCOMPLETE, b.actor, b.slot);
                continue;
            }
            int stock = b.city.getEstoque(product);
            if (b.recruit[i] > stock) add(out, Level.WARN, Kind.RECRUIT_MATERIAL, b.actor, b.slot,
                    b.city.getNome(), product.getNome(), Long.toString(b.recruit[i]), Integer.toString(stock));
            long afterRecruit = Math.max(0, stock - b.recruit[i]);
            if (b.weapons[i] + b.armour[i] > afterRecruit) {
                add(out, Level.WARN, Kind.RECRUIT_EQUIPMENT, b.actor, b.slot,
                        b.city.getNome(), product.getNome(), Long.toString(b.weapons[i]),
                        Long.toString(b.armour[i]), Long.toString(afterRecruit));
            }
            if (i == 4 && b.provision > afterRecruit) add(out, Level.WARN, Kind.RECRUIT_PROVISION,
                    b.actor, b.slot, b.city.getNome(), Long.toString(b.provision), Long.toString(afterRecruit));
        }
    }

    private static TipoTropa troop(Partida game, String code) {
        // Lists may be indexed by names. Resolve the object's stable code rather than the map key.
        for (TipoTropa troop : game.getCenario().getTipoTropas().values()) {
            if (code.equalsIgnoreCase(troop.getCodigo())) return troop;
        }
        return null; // do not pretend the runtime's fallback type was explicitly requested
    }

    private static Produto product(Partida game, int index) {
        for (Produto product : game.getCenario().getProdutos().values()) {
            if (ConverterFactory.estoqueToInt(product.getCodigo()) == index) return product;
        }
        return null;
    }

    private static String parameter(PersonagemOrdem order, int index) {
        String value = order.getParametrosId().get(index);
        if (value == null) throw new IllegalArgumentException();
        return value;
    }

    private static void add(List<Finding> out, Level level, Kind kind, BaseModel actor, int slot, String... args) {
        out.add(new Finding(level, kind, actor, slot, args));
    }
}
