package control.services;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import model.Partida;

/** Session-only lineage. Nothing here is serialized into a game or battle file. */
public final class BattleSimVariants implements AutoCloseable {
    public static final int MAX_PINS = 6;

    public static final class Family {
        private final List<BattleSimRunSnapshot> pins = new ArrayList<>();
        private final List<Runnable> listeners = new ArrayList<>();
        private int windows;
        public List<BattleSimRunSnapshot> pins() { return List.copyOf(pins); }
        public void pin(BattleSimRunSnapshot snapshot, int replace) {
            if (!snapshot.familyId().equals(id)) throw new IllegalArgumentException("Different variant family");
            if (windows == 0) throw new IllegalStateException("Closed variant family");
            if (replace >= 0) pins.set(replace, snapshot);
            else {
                if (pins.size() >= MAX_PINS) throw new IllegalStateException("Choose a pin to replace");
                pins.add(snapshot);
            }
            changed();
        }
        public void remove(int index) { pins.remove(index); changed(); }
        public void listen(Runnable listener) { listeners.add(listener); }
        public void unlisten(Runnable listener) { listeners.remove(listener); }
        private void changed() { List.copyOf(listeners).forEach(Runnable::run); }
        private final UUID id = UUID.randomUUID();
    }

    private final Family family;
    private final CombatScenario scenario;
    private final Partida game;
    private final int turn;
    private final String gameCode;
    private final Map<ArmySim, UUID> ids = new IdentityHashMap<>();
    private String name;
    private boolean closed;

    public BattleSimVariants(CombatScenario scenario, String name) {
        this(scenario, name, new Family());
    }

    private BattleSimVariants(CombatScenario scenario, String name, Family family) {
        this.scenario = scenario;
        this.name = name;
        this.family = family;
        game = scenario.getPartida();
        turn = game == null ? 0 : game.getTurno();
        gameCode = game == null ? "" : String.valueOf(game.getCodigo());
        family.windows++;
        synchronize();
    }

    /** Only this owned copy operation establishes lineage; names and army codes never do. */
    public BattleSimVariants fork(String name) {
        requireOpen();
        synchronize();
        final CombatScenario copy = scenario.copy();
        final BattleSimVariants ret = new BattleSimVariants(copy, name, family);
        // CombatScenario.copy iterates its list once, appending exactly one detached ArmySim per entry.
        if (copy.getArmies().size() != scenario.getArmies().size()) {
            ret.close();
            throw new IllegalStateException("Incomplete variant copy");
        }
        for (int i = 0; i < scenario.getArmies().size(); i++) {
            ret.ids.put(copy.getArmies().get(i), ids.get(scenario.getArmies().get(i)));
        }
        return ret;
    }

    /** Remove identity entries promptly; a later destination import gets a new identity. */
    public void synchronize() {
        requireOpen();
        ids.keySet().removeIf(army -> scenario.getArmies().stream().noneMatch(current -> current == army));
        for (ArmySim army : scenario.getArmies()) ids.computeIfAbsent(army, unused -> UUID.randomUUID());
    }

    public UUID armyId(ArmySim army) {
        synchronize();
        final UUID id = ids.get(army);
        if (id == null) throw new IllegalArgumentException("Army is outside this variant");
        return id;
    }
    public boolean matches(Partida current) {
        return !closed && current == game && (current == null || current.getTurno() == turn);
    }
    public CombatScenario scenario() { return scenario; }
    public Family family() { return family; }
    public UUID familyId() { return family.id; }
    public String gameCode() { return gameCode; }
    public int turn() { return turn; }
    public String name() { return name; }
    public void rename(String name) {
        requireOpen();
        if (name != null && !name.isBlank()) this.name = name.strip().substring(0, Math.min(80, name.strip().length()));
    }
    private void requireOpen() { if (closed) throw new IllegalStateException("Closed variant"); }
    @Override public void close() {
        if (closed) return;
        closed = true;
        ids.clear();
        if (--family.windows == 0) {
            family.pins.clear();
            family.changed();
            family.listeners.clear();
        }
    }
}
