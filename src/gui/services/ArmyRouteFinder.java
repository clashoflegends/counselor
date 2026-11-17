package gui.services;

import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import model.Local;

/** Lowest-cost routes under the same movement-cost rules as manual direction entry. */
public final class ArmyRouteFinder {

    private static final String[] DIRECTIONS = {"nw", "ne", "e", "se", "sw", "w"};

    private ArmyRouteFinder() {
    }

    public record Route(List<String> directions, int cost) {
        public Route {
            directions = List.copyOf(directions);
        }
    }

    // River navigation depends on the side from which the previous step entered this hex.
    private record State(Local local, int arrivalDirection) {
    }

    private record Node(State state, int cost, int steps, long sequence, Node previous,
            String direction) {
    }

    /**
     * Uses only the supplied map and movement settings; neither is mutated. The movement's troop
     * list is shared by its clones and is read-only throughout the search. Holds cannot improve a
     * route: they cost points and leave the last non-hold direction unchanged.
     */
    public static Optional<Route> find(Map<String, Local> locals, Local origin, Local destination,
            MovimentoExercito movement) {
        Objects.requireNonNull(movement);
        return find(locals, origin, destination, movement, movement.getLimiteMovimento());
    }

    /** A planning budget may exceed the order's per-turn allowance; movement settings stay intact. */
    public static Optional<Route> find(Map<String, Local> locals, Local origin, Local destination,
            MovimentoExercito movement, int maxCost) {
        Objects.requireNonNull(locals);
        Objects.requireNonNull(movement);
        if (origin == null || destination == null || maxCost < 0) {
            return Optional.empty();
        }
        final Local start = locals.get(origin.getCodigo());
        final Local target = locals.get(destination.getCodigo());
        if (start == null || target == null) {
            return Optional.empty();
        }

        final LocalFacade facade = new LocalFacade();
        final var queue = new java.util.PriorityQueue<Node>(Comparator.comparingInt(Node::cost)
                .thenComparingInt(Node::steps).thenComparingLong(Node::sequence));
        final Map<State, Node> best = new HashMap<>();
        long sequence = 0;
        final Node first = new Node(new State(start, 0), 0, 0, sequence++, null, null);
        best.put(first.state(), first);
        queue.add(first);
        while (!queue.isEmpty()) {
            final Node current = queue.remove();
            if (best.get(current.state()) != current) {
                continue;
            }
            if (current.state().local() == target) {
                final List<String> directions = new ArrayList<>();
                for (Node node = current; node.previous() != null; node = node.previous()) {
                    directions.add(node.direction());
                }
                Collections.reverse(directions);
                return Optional.of(new Route(directions, current.cost()));
            }
            for (int direction = 1; direction <= 6; direction++) {
                final Local next = locals.get(facade.getIdentificacaoVizinho(
                        current.state().local(), direction));
                if (next == null) {
                    continue;
                }
                final MovimentoExercito step = copy(movement);
                step.setOrigem(current.state().local());
                step.setDestino(next);
                step.setDirecao(direction);
                step.setDirecaoAnterior(current.state().arrivalDirection());
                final int cost = step.getCustoMovimento();
                if (cost <= 0 || cost >= 9999
                        || cost > maxCost - current.cost()) {
                    continue;
                }
                final int total = current.cost() + cost;
                final State state = new State(next, ConverterFactory.getDirecao(direction + 3));
                final Node old = best.get(state);
                final int steps = current.steps() + 1;
                if (old != null && (old.cost() < total
                        || (old.cost() == total && old.steps() <= steps))) {
                    continue;
                }
                final Node node = new Node(state, total, steps, sequence++, current,
                        DIRECTIONS[direction - 1]);
                best.put(state, node);
                queue.add(node);
            }
        }
        return Optional.empty();
    }

    private static MovimentoExercito copy(MovimentoExercito movement) {
        try {
            return movement.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalStateException("Movement settings cannot be copied", ex);
        }
    }
}
