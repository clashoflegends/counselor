package control.services;

import control.support.DisplayPortraitsManager;
import gui.MainResultWindowGui;
import gui.services.Toast;
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

/**
 * Checks the public clashoflegends/portraits repo for a newer portrait pack and, if the player already
 * uses portraits, offers it via a clickable toast (mirrors UpdateChecker). Fire-and-forget: daemon
 * thread, short timeout, swallows everything. The pack is a GitHub Release; "newer" = the latest
 * release tag_name differs from the locally stored PortraitsVersion.
 */
public final class PortraitsChecker {

    private static final Log log = LogFactory.getLog(PortraitsChecker.class);

    /** Stable "latest" asset URL (also the default for the PortraitsZipUrl config). */
    public static final String LATEST_ZIP_URL = "https://github.com/clashoflegends/portraits/releases/latest/download/portraits.zip";
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/clashoflegends/portraits/releases/latest";
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final int TIMEOUT_MS = 5000;
    private static final int STARTUP_DELAY_MS = 25_000; // after the Counselor self-update check (20s)

    private PortraitsChecker() {
    }

    /** The newest portraits release tag (e.g. "portraits-2026.06.28"), or null on any failure. */
    public static String fetchLatestTag() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(LATEST_RELEASE_API).toURL().openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Counselor");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = TAG.matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
        } catch (Throwable ex) {
            log.warn("Portraits version check failed (ignored): " + ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /** If portraits are in use and a newer pack exists, notify with a clickable (download) toast. */
    public static void checkAsync(final MainResultWindowGui gui) {
        if (gui == null || "false".equalsIgnoreCase(SettingsManager.getInstance().getConfig("checkForUpdates", "true"))) {
            return;
        }
        // Only relevant if the player already has portraits downloaded (folder set + non-empty).
        if (!DisplayPortraitsManager.getInstance().isShowPortraitEnableable()) {
            log.info("Portraits update check skipped: portraits not in use (folder unset or empty).");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(STARTUP_DELAY_MS);
            } catch (InterruptedException e) {
                return; // app exiting
            }
            final String latest = fetchLatestTag();
            if (latest == null) {
                log.warn("Portraits update check: could not read the latest release tag.");
                return;
            }
            final String have = SettingsManager.getInstance().getConfig("PortraitsVersion", "");
            log.info("Portraits update check: latest=" + latest + " have='" + have + "'");
            if (latest.equals(have)) {
                return; // up to date
            }
            if (have.isEmpty()) {
                // Pre-versioning install that already has portraits: adopt the current tag SILENTLY, so we
                // only notify on the NEXT genuinely-new pack (no crying wolf with a byte-identical re-download).
                SettingsManager.getInstance().setConfigAndSaveToFile("PortraitsVersion", latest);
                log.info("Portraits present but unversioned; recorded tag " + latest + " (no re-download).");
                return;
            }
            log.info("Newer portraits pack available: " + latest + " (have '" + have + "')");
            final String msg = SettingsManager.getInstance().getBundleManager().getString("PORTRAITS.UPDATE.AVAILABLE");
            if ("true".equalsIgnoreCase(SettingsManager.getInstance().getConfig("autoDownloadPortraits", "false"))) {
                SwingUtilities.invokeLater(() -> DisplayPortraitsManager.getInstance().downloadLatest(gui, false));
            } else {
                SwingUtilities.invokeLater(() -> Toast.show(
                        SwingUtilities.getWindowAncestor(gui), msg, null,
                        () -> DisplayPortraitsManager.getInstance().downloadLatest(gui)));
            }
        }, "portraits-checker");
        t.setDaemon(true);
        t.start();
    }
}
