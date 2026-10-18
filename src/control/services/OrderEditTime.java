package control.services;

import java.time.Instant;
import java.util.Objects;
import model.PersonagemOrdem;

/** Timestamp policy for a successful client edit, independent of display language. */
public final class OrderEditTime {
    private OrderEditTime() { }

    public static boolean sameContent(PersonagemOrdem before, PersonagemOrdem after) {
        return before != null && after != null && before.getOrdem() != null && after.getOrdem() != null
                && Objects.equals(before.getOrdem().getCodigo(), after.getOrdem().getCodigo())
                && Objects.equals(before.getParametrosId(), after.getParametrosId());
    }

    public static void stamp(PersonagemOrdem before, PersonagemOrdem after, Instant now) {
        after.setRecordedUpdateTime(sameContent(before, after) ? before.getRecordedUpdateTime() : now);
    }
}
