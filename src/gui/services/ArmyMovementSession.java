package gui.services;

import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import model.Local;
import model.TipoTropa;

/** Client-only route state. Swing and map rendering belong to the editor adapters. */
public final class ArmyMovementSession implements ArmyMovementParameters {

    public static final int AUTOMATIC_PLANNING_LIMIT = 24;

    /** Snapshot of the current calculation inputs, retaining the order's own allowance. */
    public record Context(List<TipoTropa> troops, boolean food, boolean evasive,
            int allowance, int moveType, boolean enabled) {
        public Context {
            troops = List.copyOf(troops);
        }

        private MovimentoExercito movement() {
            MovimentoExercito movement = new MovimentoExercito();
            movement.addTropasAll(troops);
            movement.setComida(food);
            movement.setEvasivo(evasive);
            movement.setLimiteMovimento(allowance);
            movement.setMoveType(moveType);
            return movement;
        }
    }

    /** Movement is supplied to the existing map renderer; callers must not mutate it. */
    public record Step(String direction, MovimentoExercito movement, int cost, long cumulativeCost) {
    }

    private final Map<String, Local> locals;
    private final Local origin;
    private final LocalFacade localFacade = new LocalFacade();
    private final List<Step> steps = new ArrayList<>();
    private final List<String> directions = new ArrayList<>();
    private Context context;

    public ArmyMovementSession(Map<String, Local> locals, Local origin, Context context) {
        this.locals = Objects.requireNonNull(locals);
        this.origin = Objects.requireNonNull(origin);
        this.context = Objects.requireNonNull(context);
    }

    public Context context() {
        return context;
    }

    public Local origin() {
        return origin;
    }

    public Local destination() {
        return steps.isEmpty() ? origin : steps.get(steps.size() - 1).movement().getDestino();
    }

    public List<Step> steps() {
        return List.copyOf(steps);
    }

    public long cost() {
        return steps.isEmpty() ? 0 : steps.get(steps.size() - 1).cumulativeCost();
    }

    /** Recalculate the chosen route; never replace deliberate holds or detours with a new path. */
    public void setContext(Context context) {
        Objects.requireNonNull(context);
        if (this.context.equals(context)) {
            return;
        }
        ArmyMovementSession recalculated = new ArmyMovementSession(locals, origin, context);
        for (String direction : directions) {
            // Preserve entered directions even if the new assumptions make their cost infeasible.
            recalculated.appendStep(direction);
        }
        this.context = context;
        steps.clear();
        steps.addAll(recalculated.steps);
    }

    public boolean append(String direction) {
        return context.enabled() && appendStep(direction);
    }

    private boolean appendStep(String direction) {
        if (direction == null || !ConverterFactory.isDirecaoValid(direction)) {
            return false;
        }
        if (isLandingStopped()) {
            directions.add(direction);
            return true;
        }
        Local from = destination();
        int directionNumber = ConverterFactory.direcaoToInt(direction);
        Local to = directionNumber == 0 ? from
                : locals.get(localFacade.getIdentificacaoVizinho(from, direction));
        if (to == null) {
            return false;
        }
        MovimentoExercito movement = context.movement();
        movement.setOrigem(from);
        movement.setDestino(to);
        movement.setDirecao(directionNumber);
        for (int index = steps.size() - 1; index >= 0; index--) {
            String previous = steps.get(index).direction();
            if (ConverterFactory.direcaoToInt(previous) != 0) {
                movement.setDirecaoAnterior(ConverterFactory.direcaoToInt(previous) + 3);
                break;
            }
        }
        int cost = movement.getCustoMovimento();
        steps.add(new Step(direction, movement, cost, cost() + cost));
        directions.add(direction);
        return true;
    }

    public boolean isLandingStopped() {
        return !steps.isEmpty() && steps.get(steps.size() - 1).movement().isLandingStop();
    }

    public void clear() {
        steps.clear();
        directions.clear();
    }

    public void removeLast() {
        if (!directions.isEmpty()) {
            var remaining = new ArrayList<>(directions.subList(0, directions.size() - 1));
            clear();
            for (String direction : remaining) appendStep(direction);
        }
    }

    /** Searches before changing state. Failure leaves every entered step intact. */
    public boolean routeTo(Local target) {
        if (!context.enabled()) {
            return false;
        }
        var route = ArmyRouteFinder.find(locals, origin, target, context.movement(),
                AUTOMATIC_PLANNING_LIMIT);
        if (route.isEmpty()) {
            return false;
        }
        ArmyMovementSession replacement = new ArmyMovementSession(locals, origin, context);
        for (String direction : route.get().directions()) {
            if (!replacement.append(direction)) {
                return false;
            }
        }
        steps.clear();
        steps.addAll(replacement.steps);
        directions.clear();
        directions.addAll(replacement.directions);
        return true;
    }

    @Override
    public String getDirecoesIdTipo() {
        StringBuilder canonical = new StringBuilder();
        for (String direction : directions) {
            canonical.append(direction).append(';');
        }
        return canonical.append(context.evasive() ? "ev" : "nr").toString();
    }

    @Override
    public String getDirecaoDisplay() {
        // Saved display is also parsed when reopening an order. Keep it canonical.
        return getDirecoesIdTipo();
    }
}
