package control.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Ordem;
import model.PersonagemOrdem;

/**
 * Source of the "do it again" feature: the orders this actor actually ran LAST turn.
 * <p>
 * They arrive inside the turn file itself - the server writes each character's submitted orders back
 * into {@code BaseModel.acaoExecutadas} ({@code ServerPersonagemDao.getPersonagemOrdem}), so nothing
 * depends on the player still having last turn's {@code .rc.egf} on this machine, and what comes back
 * is what the Judge ran rather than what was last saved locally.
 * <p>
 * Two limits worth knowing:
 * <ul>
 *   <li>the server only fills this in for <b>characters</b>, so cities and nations have nothing to
 *       repeat and the feature is simply unavailable there;</li>
 *   <li>parameters are copied <b>verbatim</b>. An order still offered to the actor is repeatable, but
 *       a target that has since died or moved is not detected here - the player reviews each repeated
 *       order in the panel before submitting, which is why the feature fills empty slots only.</li>
 * </ul>
 */
public final class LastTurnOrders {

    private LastTurnOrders() {
    }

    /** True when this actor ran anything last turn that could be repeated (drives the button state). */
    public static boolean hasAny(Collection<PersonagemOrdem> executed) {
        return executed != null && !executed.isEmpty();
    }

    /**
     * Last turn's orders for this actor, as fresh copies safe to save into a slot, keeping only those
     * still offered to the actor this turn.
     *
     * @param executed       last turn's orders for the actor
     * @param actorName      the actor's current name, stamped on each repeated order
     * @param availableNow   the orders currently selectable for the actor (an order dropped from the
     *                       scenario, or no longer legal for this actor, must not be repeated)
     */
    public static List<PersonagemOrdem> listRepeatable(Collection<PersonagemOrdem> executed, String actorName,
            Collection<Integer> availableNow) {
        List<PersonagemOrdem> ret = new ArrayList<>();
        if (!hasAny(executed)) {
            return ret;
        }
        for (PersonagemOrdem po : executed) {
            if (po == null || po.getOrdem() == null) {
                continue;
            }
            if (availableNow != null && !availableNow.contains(po.getOrdem().getNumero())) {
                continue;
            }
            ret.add(copyOf(actorName, po));
        }
        return ret;
    }

    /** How many of last turn's orders are no longer offered to the actor (the "skipped" count). */
    public static int countUnavailable(Collection<PersonagemOrdem> executed, Collection<Integer> availableNow) {
        if (!hasAny(executed)) {
            return 0;
        }
        int ret = 0;
        for (PersonagemOrdem po : executed) {
            if (po == null || po.getOrdem() == null) {
                continue;
            }
            if (availableNow != null && !availableNow.contains(po.getOrdem().getNumero())) {
                ret++;
            }
        }
        return ret;
    }

    /**
     * A fresh order carrying the same choice as the executed one.
     * <p>
     * Deep-copied on purpose. The executed order is a live object inside the loaded turn file, and a
     * saved order is held by identity in three places (the actor's slots, the cost forecast set, and
     * the submission), so handing the same instance to two slots would make the forecast count one of
     * them for free. The parameter lists are copied for the same reason: {@code ComandoDetail} keeps
     * the list reference it is given, so a shared list would alias into the saved orders file.
     */
    public static PersonagemOrdem copyOf(String actorName, PersonagemOrdem source) {
        final PersonagemOrdem ret = new PersonagemOrdem();
        ret.setNome(actorName == null ? source.getNome() : actorName);
        ret.setOrdem(source.getOrdem());
        ret.setParametrosId(copyList(source.getParametrosId()));
        ret.setParametrosDisplay(copyList(source.getParametrosDisplay()));
        // Entered now, not when the Judge ran it: this is a new order in this turn's submission.
        ret.setUpdateTime(Instant.now());
        return ret;
    }

    private static List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    /** The order numbers currently selectable for an actor, read from the order combo the panel builds. */
    public static List<Integer> availableNumbers(baseLib.GenericoComboBoxModel ordemComboModel) {
        List<Integer> ret = new ArrayList<>();
        if (ordemComboModel == null) {
            return ret;
        }
        for (baseLib.GenericoComboObject elem : ordemComboModel.getElementAll()) {
            if (elem != null && elem.getObject() instanceof Ordem) {
                ret.add(((Ordem) elem.getObject()).getNumero());
            }
        }
        return ret;
    }
}
