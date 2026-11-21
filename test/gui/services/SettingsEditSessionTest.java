package gui.services;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsEditSessionTest {
    private final Map<String, String> values = new HashMap<>();
    private Map<String, String> saved = Map.of();
    private final SettingsEditSession session = new SettingsEditSession(() -> new HashMap<>(values),
            (key, value) -> { if (value == null) values.remove(key); else values.put(key, value); },
            () -> saved = new HashMap<>(values));

    @Test void cancellationRevertsOwnEditButPreservesUnrelatedMainWindowChanges() {
        values.put("theme", "old");
        session.edit(() -> values.put("theme", "draft"));
        values.put("map", "new-main-window-choice");
        saved = new HashMap<>(values); // another operation saved all preferences
        session.cancel();
        assertEquals("old", values.get("theme"));
        assertEquals("new-main-window-choice", values.get("map"));
        assertEquals(values, saved);
    }

    @Test void cancellationDoesNotOverwriteANewerExternalEditOfTheSamePreference() {
        values.put("map", "old");
        session.edit(() -> values.put("map", "settings-draft"));
        values.put("map", "new-external-choice");
        session.cancel();
        assertEquals("new-external-choice", values.get("map"));
    }

    @Test void cancellingAnAddedOverrideRestoresItsAbsenceAndDefaultBehavior() {
        session.edit(() -> values.put("optional", "draft"));
        session.cancel();
        assertFalse(values.containsKey("optional"));
    }

    @Test void editingAgainAfterAnExternalUpdateUsesTheNewerBaseline() {
        values.put("map", "old");
        session.edit(() -> values.put("map", "draft1"));
        values.put("map", "external");
        session.edit(() -> values.put("map", "draft2"));
        session.edit(() -> values.put("map", "draft3"));
        session.cancel();
        assertEquals("external", values.get("map"));
    }
}
