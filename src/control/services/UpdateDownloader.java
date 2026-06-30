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
 * For dev/jpackage/unknown distros, for MSI (deferred until the installer is code-signed - an
 * auto-downloaded unsigned .msi trips SmartScreen), or on any failure, it falls back to opening the
 * GitHub releases page in the browser - the same destination the About box link uses. So auto-download
 * is live for deb / dmg / windows-portable / portable-jar, and MSI rejoins once SignPath is live.
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
        if ("msi".equals(distro)) {
            // MSI auto-download is deferred until the installer is code-signed: auto-downloading and
            // launching an UNSIGNED .msi trips Windows SmartScreen, a worse experience than a manual
            // download. Send MSI users to the releases page instead. Drop this branch once SignPath
            // signing is live (then MSI rejoins the download+reveal path via assetPredicateFor).
            openReleasesPage();
            return;
        }
        if (assetPredicateFor(distro) == null) {
            // dev / jpackage / unknown distro: no single installer asset applies - just open the page.
            openReleasesPage();
            return;
        }
        final BusyGlass busy = BusyGlass.show(parent, tx(labels, "UPDATE.DOWNLOAD.START", "Downloading update..."));
        new SwingWorker<Result, Void>() {
            @Override
            protected Result doInBackground() throws Exception {
                String url = pickAssetUrl(distro);
                if (url == null) {
                    return null; // no matching asset in the latest release
                }
                File file = downloadTo(url);
                // Heavy install/stage (unzip / hdiutil+ditto) runs HERE, off the EDT, so the UI never
                // freezes during the copy. done() only shows the result.
                boolean installed = installOrStage(distro, file);
                return new Result(file, installed);
            }

            @Override
            protected void done() {
                if (busy != null) {
                    busy.dismiss();
                }
                Result r = null;
                try {
                    r = get();
                } catch (Exception ex) {
                    log.warn("Update download failed: " + ex);
                }
                if (r == null) {
                    // couldn't resolve/download the asset - fall back to the releases page
                    openReleasesPage();
                    Toast.show(SwingUtilities.getWindowAncestor(parent),
                            tx(labels, "UPDATE.DOWNLOAD.FAIL", "Could not download automatically - opening the downloads page."));
                } else if (r.installed) {
                    Toast.show(SwingUtilities.getWindowAncestor(parent),
                            tx(labels, "UPDATE.INSTALLED.NEXTLAUNCH", "Update installed - it will be active next time you start Counselor."));
                } else {
                    // deb / windows-portable / any auto-install failure: reveal so the player runs it.
                    revealInFolder(r.file);
                    Toast.show(SwingUtilities.getWindowAncestor(parent),
                            String.format(tx(labels, "UPDATE.DOWNLOAD.OK", "Update downloaded to %s - open it to install."), r.file.getAbsolutePath()));
                }
            }
        }.execute();
    }

    private static final class Result {
        final File file;
        final boolean installed;
        Result(File file, boolean installed) {
            this.file = file;
            this.installed = installed;
        }
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

    /**
     * Install/stage a freshly-downloaded asset (runs OFF the EDT). portable-jar and dmg auto-install,
     * taking effect on the NEXT launch - never an auto-relaunch, so unsaved orders can't be lost.
     * Returns true if installed; false means the caller should reveal the download (deb /
     * windows-portable, or any install failure - the player then runs it manually).
     */
    private static boolean installOrStage(String distro, File file) {
        try {
            if ("portable-jar".equals(distro)) {
                stagePortableUpdate(file);
                return true;
            }
            if ("dmg".equals(distro)) {
                installDmg(file);
                return true;
            }
        } catch (Exception ex) {
            log.warn("Auto-install failed; falling back to reveal: " + ex);
        }
        return false;
    }

    /**
     * Stage the new portable bundle's dist/ at &lt;appRoot&gt;/.update/dist. run.bat/run.sh swap it in
     * before the JVM starts on the next launch (no locked-file fight; the old dist/ is kept until the
     * swap succeeds, so a failed swap rolls back rather than bricking the install).
     */
    private static void stagePortableUpdate(File zip) throws Exception {
        File appRoot = resolvePortableRoot();
        File staging = new File(appRoot, ".update");
        deleteRecursive(staging);
        File extract = new File(staging, "extract");
        if (!extract.mkdirs()) {
            throw new java.io.IOException("cannot create staging dir " + extract);
        }
        unzip(zip, extract);
        File newDist = findDistDir(extract);
        if (newDist == null) {
            throw new java.io.IOException("no dist/ found in portable zip");
        }
        java.nio.file.Files.move(newDist.toPath(), new File(staging, "dist").toPath());
        deleteRecursive(extract);
        zip.delete();
        log.info("Portable update staged at " + new File(staging, "dist") + " (applied on next launch)");
    }

    /** &lt;root&gt;/dist/PbmCounselor.jar -&gt; &lt;root&gt; (the portable launcher dir, regardless of CWD). */
    private static File resolvePortableRoot() throws Exception {
        File jar = new File(UpdateDownloader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File dist = jar.getParentFile();
        File root = (dist != null) ? dist.getParentFile() : null;
        if (root == null || !root.isDirectory()) {
            throw new java.io.IOException("cannot resolve portable root from " + jar);
        }
        return root;
    }

    private static File findDistDir(File extract) {
        File direct = new File(extract, "dist");
        if (direct.isDirectory()) {
            return direct;
        }
        File[] kids = extract.listFiles(File::isDirectory); // zip top-level is "Counselor/"
        if (kids != null) {
            for (File k : kids) {
                File d = new File(k, "dist");
                if (d.isDirectory()) {
                    return d;
                }
            }
        }
        return null;
    }

    private static void unzip(File zip, File dest) throws Exception {
        java.nio.file.Path destPath = dest.toPath().normalize();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zip))) {
            java.util.zip.ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File f = new File(dest, e.getName());
                if (!f.toPath().normalize().startsWith(destPath)) {
                    throw new java.io.IOException("zip entry escapes dest: " + e.getName()); // zip-slip guard
                }
                if (e.isDirectory()) {
                    f.mkdirs();
                } else {
                    f.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(f)) {
                        zis.transferTo(os);
                    }
                }
            }
        }
    }

    /**
     * Install a downloaded .dmg over the running app bundle (apple-silicon). Mounts the image, copies
     * Counselor.app into place via an atomic-ish temp+swap (old kept until the new is in, with rollback),
     * detaches. No relaunch - the running instance keeps the old code; the next launch picks up the new.
     */
    private static void installDmg(File dmg) throws Exception {
        String volume = null;
        try {
            String out = runProcess("hdiutil", "attach", "-nobrowse", "-quiet", dmg.getAbsolutePath());
            for (String line : out.split("\\R")) {
                int idx = line.indexOf("/Volumes/");
                if (idx >= 0) {
                    volume = line.substring(idx).trim();
                    break;
                }
            }
            if (volume == null) {
                throw new java.io.IOException("could not find the mounted volume");
            }
            File appInDmg = findAppBundle(new File(volume));
            if (appInDmg == null) {
                throw new java.io.IOException("no .app bundle in the dmg");
            }
            File targetApp = resolveMacApp();
            File tmp = new File(targetApp.getParentFile(), targetApp.getName() + ".new");
            File old = new File(targetApp.getParentFile(), targetApp.getName() + ".old");
            deleteRecursive(tmp);
            deleteRecursive(old);
            runProcess("ditto", appInDmg.getAbsolutePath(), tmp.getAbsolutePath());
            if (targetApp.exists() && !targetApp.renameTo(old)) {
                throw new java.io.IOException("cannot move current app aside");
            }
            if (!tmp.renameTo(targetApp)) {
                if (old.exists()) {
                    old.renameTo(targetApp); // rollback
                }
                throw new java.io.IOException("cannot move new app into place");
            }
            deleteRecursive(old);
            log.info("dmg update installed over " + targetApp + " (active on next launch)");
        } finally {
            if (volume != null) {
                try {
                    runProcess("hdiutil", "detach", volume, "-quiet");
                } catch (Exception ignore) {
                    // best-effort unmount
                }
            }
        }
    }

    private static File findAppBundle(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (k.getName().endsWith(".app")) {
                    return k;
                }
            }
        }
        return null;
    }

    /**
     * .../Counselor.app/Contents/MacOS/Counselor -&gt; .../Counselor.app. Uses lastIndexOf so an install
     * path that itself contains ".app" earlier (e.g. a "My.apps" folder) doesn't truncate wrongly.
     * Note: until the app is signed/notarized, Gatekeeper may run a Downloads-launched copy from a
     * randomized read-only translocated path - then this points there and ditto/rename fails, which is
     * caught upstream and falls back to reveal (the dmg auto-install simply no-ops for that case).
     */
    private static File resolveMacApp() throws Exception {
        String p = System.getProperty("jpackage.app-path");
        if (p == null) {
            throw new java.io.IOException("jpackage.app-path not set");
        }
        int idx = p.lastIndexOf(".app");
        if (idx < 0) {
            throw new java.io.IOException("not an .app path: " + p);
        }
        return new File(p.substring(0, idx + 4));
    }

    private static String runProcess(String... cmd) throws Exception {
        Process pr = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out;
        try (InputStream in = pr.getInputStream()) {
            out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!pr.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
            pr.destroyForcibly();
            throw new java.io.IOException("timed out running " + cmd[0]);
        }
        if (pr.exitValue() != 0) {
            throw new java.io.IOException(cmd[0] + " exited " + pr.exitValue() + ": " + out);
        }
        return out;
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteRecursive(k);
            }
        }
        f.delete();
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
