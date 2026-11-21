package gui.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import persistenceCommons.SettingsManager;

/** Tracks only edits made by one modeless Settings view; unrelated changes keep their latest values. */
public final class SettingsEditSession {
    private final Supplier<Map<String, String>> snapshot;
    private final BiConsumer<String, String> restore;
    private final Runnable persist;
    private final Map<String, String> original = new HashMap<>();
    private final Map<String, String> lastEdit = new HashMap<>();

    public SettingsEditSession() {
        this(() -> {
            SettingsManager sm = SettingsManager.getInstance();
            Map<String, String> values = new HashMap<>();
            var keys = sm.listConfigs();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                values.put(key, sm.getConfig(key));
            }
            return values;
        }, (key, value) -> {
            SettingsManager sm = SettingsManager.getInstance();
            if (value == null) sm.removeConfig(key);
            else sm.setConfig(key, value);
        }, () -> SettingsManager.getInstance().saveToFile());
    }

    SettingsEditSession(Supplier<Map<String, String>> snapshot, BiConsumer<String, String> restore, Runnable persist) {
        this.snapshot = snapshot;
        this.restore = restore;
        this.persist = persist;
    }

    public void edit(Runnable action) {
        Map<String, String> before = snapshot.get();
        try { action.run(); }
        finally {
            Map<String, String> after = snapshot.get();
            var keys = new HashSet<>(before.keySet());
            keys.addAll(after.keySet());
            for (String key : keys) {
                if (!Objects.equals(before.get(key), after.get(key))) {
                    // A later edit starts from the current value, including intervening external updates.
                    if (!lastEdit.containsKey(key) || !Objects.equals(before.get(key), lastEdit.get(key))) {
                        original.put(key, before.get(key));
                    }
                    lastEdit.put(key, after.get(key));
                }
            }
        }
    }

    public void cancel() {
        Map<String, String> now = snapshot.get();
        boolean changed = false;
        for (var entry : lastEdit.entrySet()) {
            if (Objects.equals(now.get(entry.getKey()), entry.getValue())) {
                restore.accept(entry.getKey(), original.get(entry.getKey()));
                changed = true;
            }
        }
        original.clear();
        lastEdit.clear();
        if (changed) persist.run(); // undo even if another operation flushed the draft to disk
    }
}
