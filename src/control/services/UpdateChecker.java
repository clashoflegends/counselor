package control.services;

import gui.MainResultWindowGui;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 * Checks GitHub for a newer Counselor release and notifies the player (window title + status bar)
 * if one is available. Fire-and-forget: runs on a daemon thread with a short timeout and swallows
 * everything, so it never blocks startup or breaks anything when offline.
 *
 * Compares the local build number (version_counselor BUILD) against the latest release's name
 * ("Counselor 2.NNN", set by the jpackage release workflow). Opt out with checkForUpdates=false.
 */
public final class UpdateChecker {

    private static final Log log = LogFactory.getLog(UpdateChecker.class);
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/clashoflegends/counselor/releases/latest";
    private static final Pattern RELEASE_BUILD = Pattern.compile("Counselor 2\\.(\\d+)");
    private static final int TIMEOUT_MS = 5000;

    private UpdateChecker() {
    }

    public static void checkAsync(final MainResultWindowGui gui) {
        if (gui == null || "false".equalsIgnoreCase(SettingsManager.getInstance().getConfig("checkForUpdates", "true"))) {
            return;
        }
        Thread t = new Thread(() -> check(gui), "update-checker");
        t.setDaemon(true);
        t.start();
    }

    private static void check(MainResultWindowGui gui) {
        try {
            int localBuild = parseInt(SysApoio.getVersionClash("version_counselor"));
            int latestBuild = fetchLatestBuild();
            // Always log the outcome so the check is diagnosable (a latest of 2.-1 means the API
            // response did not match the expected "Counselor 2.NNN" release name).
            log.info(String.format("Update check: current 2.%d, latest on GitHub 2.%d", localBuild, latestBuild));
            if (latestBuild > localBuild) {
                final String version = "2." + latestBuild;
                log.warn(String.format("A newer Counselor is available: %s (you are on 2.%d) - update from the releases page.", version, localBuild));
                SwingUtilities.invokeLater(() -> gui.setUpdateAvailable(version));
            }
        } catch (Throwable ex) {
            // WARN (not debug) so a failed check is visible in counselor.log instead of silently swallowed.
            log.warn("Update check failed (ignored): " + ex);
        }
    }

    private static int fetchLatestBuild() throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(LATEST_RELEASE_API).toURL().openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Counselor"); // GitHub API requires a User-Agent
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = RELEASE_BUILD.matcher(line);
                    if (m.find()) {
                        return Integer.parseInt(m.group(1));
                    }
                }
            }
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
