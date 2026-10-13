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
 *
 * Two entry points, ONE check: {@link #checkAsync} is the startup one (delayed, opt-out-able,
 * fires the toast), and {@link #checkNow} is the synchronous one behind the About box button, for
 * the player who leaves the Counselor open for days and would otherwise have to restart once just
 * to LEARN an update exists and a second time to apply it.
 */
public final class UpdateChecker {

    private static final Log log = LogFactory.getLog(UpdateChecker.class);
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/clashoflegends/counselor/releases/latest";
    private static final String LATEST_RELEASE_URL = "https://github.com/clashoflegends/counselor/releases/latest";
    private static final Pattern RELEASE_BUILD = Pattern.compile("Counselor 2\\.(\\d+)");
    private static final int TIMEOUT_MS = 5000;

    /** Newer version found by the last check (e.g. "2.882"), or null if none / not yet checked.
     *  Set on the checker thread, read on the EDT (About box) - hence volatile. */
    private static volatile String availableVersion = null;

    /** Newer Counselor version available ("2.NNN"), or null if up to date / the check hasn't completed. */
    public static String getAvailableVersion() {
        return availableVersion;
    }

    /** GitHub "latest release" page (redirects to the newest tag) - where to send the player to update. */
    public static String getLatestReleaseUrl() {
        return LATEST_RELEASE_URL;
    }

    private UpdateChecker() {
    }

    /** Wait this long after startup before the network check, so it doesn't compete with the EGF
     *  autoload + first render for CPU/network while the app is settling. */
    private static final int STARTUP_DELAY_MS = 20_000;

    public static void checkAsync(final MainResultWindowGui gui) {
        if (gui == null || "false".equalsIgnoreCase(SettingsManager.getInstance().getConfig("checkForUpdates", "true"))) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(STARTUP_DELAY_MS);
            } catch (InterruptedException e) {
                return; // app exiting - abandon the check
            }
            check(gui);
        }, "update-checker");
        t.setDaemon(true);
        t.start();
    }

    private static void check(MainResultWindowGui gui) {
        final Result r = checkNow();
        if (r.isNewer()) {
            SwingUtilities.invokeLater(() -> gui.setUpdateAvailable(r.getLatestVersion()));
        }
    }

    /** What a check found. FAILED covers both "could not reach GitHub" and "could not read the
     *  release name" - never report either of those as up to date, which is the silent plausible
     *  default that makes a reassuring button worse than no button. */
    public enum Status {
        NEWER, CURRENT, FAILED
    }

    /** Outcome of one check: the status plus both build numbers, for the message shown to the player. */
    public static final class Result {

        private final Status status;
        private final int localBuild;
        private final int latestBuild;

        private Result(Status status, int localBuild, int latestBuild) {
            this.status = status;
            this.localBuild = localBuild;
            this.latestBuild = latestBuild;
        }

        public Status getStatus() {
            return status;
        }

        public boolean isNewer() {
            return status == Status.NEWER;
        }

        /** The running version, "2.NNN" - meaningless when the local build could not be read. */
        public String getLocalVersion() {
            return "2." + localBuild;
        }

        /** The newer version, "2.NNN", or null unless the status is NEWER. */
        public String getLatestVersion() {
            return status == Status.NEWER ? "2." + latestBuild : null;
        }
    }

    /**
     * The whole decision, with no I/O, so it can be tested without a network. Either build coming
     * back negative means the comparison is meaningless: {@link #fetchLatestBuild} returns -1 when
     * the API response carries no "Counselor 2.NNN" release name, and an unreadable local build
     * parses to -1 as well. Both are FAILED, never CURRENT.
     */
    public static Result classify(int localBuild, int latestBuild) {
        if (localBuild < 0 || latestBuild < 0) {
            return new Result(Status.FAILED, localBuild, latestBuild);
        }
        return new Result(latestBuild > localBuild ? Status.NEWER : Status.CURRENT, localBuild, latestBuild);
    }

    /**
     * Run the check on THIS thread and report what it found. Never throws and never blocks longer
     * than the timeouts, but it does hit the network - call it off the EDT.
     *
     * Deliberately ignores checkForUpdates=false: that setting governs the unprompted startup
     * check, and a player who just clicked the button has asked for this one.
     */
    public static Result checkNow() {
        final int localBuild = parseInt(SysApoio.getVersionClash("version_counselor"));
        int latestBuild;
        try {
            latestBuild = fetchLatestBuild();
        } catch (Throwable ex) {
            // WARN (not debug) so a failed check is visible in counselor.log instead of silently swallowed.
            log.warn("Update check failed (ignored): " + ex);
            latestBuild = -1;
        }
        // Always log the outcome so the check is diagnosable (a latest of 2.-1 means the API
        // response did not match the expected "Counselor 2.NNN" release name).
        log.info(String.format("Update check: current 2.%d, latest on GitHub 2.%d", localBuild, latestBuild));
        final Result r = classify(localBuild, latestBuild);
        if (r.isNewer()) {
            availableVersion = r.getLatestVersion(); // exposed to the About box (getAvailableVersion)
            log.warn(String.format("A newer Counselor is available: %s (you are on 2.%d) - update from the releases page.",
                    r.getLatestVersion(), localBuild));
        }
        return r;
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
