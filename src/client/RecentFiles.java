package client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import persistenceCommons.SettingsManager;

/**
 * Recently-opened results files, persisted in properties.config under key "recentFiles" as a
 * pipe-delimited list of absolute paths, most-recent first. Pipe is illegal in Windows filenames
 * and vanishingly rare on *nix, so it is a safe delimiter (the same Properties escaping already
 * round-trips full UNC paths via the autoLoad key). Best-effort: every method swallows its own
 * errors so recent-files tracking can never disrupt opening a file.
 */
public final class RecentFiles {

    private static final String KEY = "recentFiles";
    private static final String SEP = "|";
    private static final int MAX = 8;

    private RecentFiles() {
    }

    /** Record a just-opened file at the top of the list (deduped, capped at MAX). */
    public static synchronized void add(File f) {
        try {
            if (f == null) {
                return;
            }
            String path = f.getAbsolutePath();
            List<String> paths = rawPaths();
            paths.removeIf(p -> p.equalsIgnoreCase(path));
            paths.add(0, path);
            while (paths.size() > MAX) {
                paths.remove(paths.size() - 1);
            }
            SettingsManager.getInstance().setConfigAndSaveToFile(KEY, String.join(SEP, paths));
        } catch (Exception ignore) {
            // recent-files tracking must never break opening a file
        }
    }

    /** Existing recent files, most-recent first; missing/moved files are skipped. */
    public static synchronized List<File> list() {
        List<File> out = new ArrayList<>();
        try {
            for (String p : rawPaths()) {
                File f = new File(p);
                if (f.isFile()) {
                    out.add(f);
                }
            }
        } catch (Exception ignore) {
        }
        return out;
    }

    /** Forget all recent files. */
    public static synchronized void clear() {
        try {
            SettingsManager.getInstance().setConfigAndSaveToFile(KEY, "");
        } catch (Exception ignore) {
        }
    }

    private static List<String> rawPaths() {
        List<String> paths = new ArrayList<>();
        String stored = SettingsManager.getInstance().getConfig(KEY, "");
        if (stored != null && !stored.isEmpty()) {
            for (String p : stored.split("\\|", -1)) {
                if (!p.trim().isEmpty()) {
                    paths.add(p);
                }
            }
        }
        return paths;
    }
}
