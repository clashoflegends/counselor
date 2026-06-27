package control.services;

import gui.services.BusyGlass;
import gui.services.Toast;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SysApoio;

/**
 * Downloads the right installer asset for THIS install's distribution and reveals it in the file
 * manager, so the player can run it to upgrade. We deliberately do NOT launch the installer: until
 * the binaries are code-signed (SignPath), auto-launching an unsigned installer re-triggers AV /
 * SmartScreen blocks. Reveal-only is safe today.
 *
 * Triggered from the clickable "update available" toast (see MainResultWindowGui.setUpdateAvailable).
 * For dev/jpackage/unknown distros, or on any failure, it falls back to opening the GitHub releases
 * page in the browser - the same destination the About box link uses.
 */
public final class UpdateDownloader {

    private static final Log log = LogFactory.getLog(UpdateDownloader.class);
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/clashoflegends/counselor/releases/latest";
    private static final Pattern ASSET_URL = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"(https://[^\"]+)\"");

    private UpdateDownloader() {
    }

    /** Begin the download+reveal flow. Call on the EDT (e.g. from the toast click). */
    public static void start(final Component parent, final BundleManager labels) {
        final String distro = SysApoio.getDistro();
        if (assetPredicateFor(distro) == null) {
            // dev / jpackage / unknown distro: no single installer asset applies - just open the page.
            openReleasesPage();
            return;
        }
        final BusyGlass busy = BusyGlass.show(parent, tx(labels, "UPDATE.DOWNLOAD.START", "Downloading update..."));
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                String url = pickAssetUrl(distro);
                if (url == null) {
                    return null; // no matching asset in the latest release
                }
                return downloadTo(url);
            }

            @Override
            protected void done() {
                if (busy != null) {
                    busy.dismiss();
                }
                File file = null;
                try {
                    file = get();
                } catch (Exception ex) {
                    log.warn("Update download failed: " + ex);
                }
                if (file != null) {
                    revealInFolder(file);
                    Toast.show(SwingUtilities.getWindowAncestor(parent),
                            String.format(tx(labels, "UPDATE.DOWNLOAD.OK", "Update downloaded to %s - open it to install."), file.getAbsolutePath()));
                } else {
                    // couldn't resolve/download the asset - fall back to the releases page
                    openReleasesPage();
                    Toast.show(SwingUtilities.getWindowAncestor(parent),
                            tx(labels, "UPDATE.DOWNLOAD.FAIL", "Could not download automatically - opening the downloads page."));
                }
            }
        }.execute();
    }

    /** Returns the matching asset's download URL from the latest release, or null if none matches. */
    private static String pickAssetUrl(String distro) throws Exception {
        String json = httpGet(LATEST_RELEASE_API);
        List<String> urls = new ArrayList<>();
        Matcher m = ASSET_URL.matcher(json);
        while (m.find()) {
            urls.add(m.group(1));
        }
        AssetMatch pred = assetPredicateFor(distro);
        for (String u : urls) {
            String name = u.substring(u.lastIndexOf('/') + 1);
            if (pred != null && pred.matches(name)) {
                return u;
            }
        }
        return null;
    }

    private interface AssetMatch {
        boolean matches(String fileName);
    }

    /** Maps the running distribution to a predicate over release-asset file names (version-agnostic). */
    private static AssetMatch assetPredicateFor(String distro) {
        if (distro == null) {
            return null;
        }
        switch (distro) {
            case "msi":
                return n -> n.endsWith(".msi");
            case "deb":
                return n -> n.endsWith(".deb");
            case "dmg":
                return n -> n.endsWith(".dmg");
            case "windows-portable":
                return n -> n.contains("windows-portable") && n.endsWith(".zip");
            case "portable-jar":
                // the cross-platform JAR zip is "Counselor-portable-<ver>.zip"; must NOT match the
                // "Counselor-windows-portable-<ver>.zip" app-image zip.
                return n -> n.startsWith("Counselor-portable-") && n.endsWith(".zip");
            default:
                return null; // dev / jpackage / anything else -> open the releases page
        }
    }

    private static File downloadTo(String url) throws Exception {
        File dir = new File(System.getProperty("user.home"), "Downloads");
        if (!dir.isDirectory()) {
            dir = new File(System.getProperty("user.home"));
        }
        String name = url.substring(url.lastIndexOf('/') + 1);
        File out = new File(dir, name);
        File part = new File(dir, name + ".part");
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setInstanceFollowRedirects(true); // release asset URLs redirect to a CDN
        conn.setRequestProperty("User-Agent", "Counselor");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);
        try (InputStream in = conn.getInputStream(); OutputStream os = new FileOutputStream(part)) {
            in.transferTo(os);
        } catch (Exception ex) {
            part.delete(); // never leave a truncated download behind
            throw ex;
        } finally {
            conn.disconnect();
        }
        // Publish only after a clean download, so a mid-transfer drop can never leave a
        // runnable-looking but truncated installer in Downloads.
        if (out.exists()) {
            out.delete();
        }
        return part.renameTo(out) ? out : part; // fall back to the .part path if rename fails (e.g. cross-device)
    }

    private static void revealInFolder(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception ex) {
            log.warn("Could not reveal download folder: " + ex);
        }
    }

    private static void openReleasesPage() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(UpdateChecker.getLatestReleaseUrl()));
            }
        } catch (Exception ex) {
            log.warn("Could not open releases page: " + ex);
        }
    }

    private static String httpGet(String apiUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent", "Counselor");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static String tx(BundleManager labels, String key, String fallback) {
        String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
