/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class Main implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(Main.class);

    /** Wall-clock at main() entry; PbmApplication uses it to log total startup time. */
    static long launchMs;

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        launchMs = System.currentTimeMillis();
        reconfigureLogForInstaller();
        pruneOldDailyLogs();
        // Invokes Gui to display turn results
        log.info(String.format("Starting... %s ...", SysApoio.getPidOs()));
        log.info("Counselor version: " + SysApoio.getVersionClash("version_counselor"));
        log.info("Commons version: " + SysApoio.getVersionClash("version_commons"));
        log.info("Java Specification Version: " + SysApoio.getVersionJavaSpecification());
        log.info("Java Runtime Version: " + SysApoio.getVersionJava());
        log.info("Java Runtime Name: " + SysApoio.getVersionJavaRuntime());
        log.info("Java VM Version: " + SysApoio.getVersionJavaVm());
        log.info("Java VM Name: " + SysApoio.getVersionJavaVmName());
        log.info("OS version: " + SysApoio.getVersionOs());
        log.info("Screen Size: " + SysApoio.getScreenSize());

        final SettingsManager sm = SettingsManager.getInstance();
        sm.setConfigurationMode("Client");
        sm.setLanguage(sm.getConfig("language", "en"));
        String autoload;
        if (args.length == 1) {
            autoload = args[0];
            // launched with a file argument = OS file-association / double-click / command line
            SysApoio.setLoadMode("association");
        } else {
            autoload = sm.getConfig("autoLoad");
            // no argument = the autoLoad game from properties.config opened at startup
            SysApoio.setLoadMode("autostart");
        }
        if (autoload != null && autoload.endsWith(".rc.egf")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(.*[/\\\\])?orders_(\\d+)_(\\d+)\\.(.+)\\.rc\\.egf$")
                    .matcher(autoload);
            if (m.matches()) {
                int turn = Integer.parseInt(m.group(3)) - 1;
                autoload = (m.group(1) != null ? m.group(1) : "") + "game_" + m.group(2) + "_" + turn + "." + m.group(4) + ".rr.egf";
            } else {
                autoload = autoload.substring(0, autoload.length() - 7) + ".rr.egf";
            }
        }
        sm.setWorldBuilder(sm.getConfig("worldBuilder", "0").equalsIgnoreCase("1"));
        sm.setRadialMenu(sm.getConfig("newUi", "1").equalsIgnoreCase("1"));
        //load application
        new PbmApplication(autoload).start(); //filename to autoload results file
    }

    private static void reconfigureLogForInstaller() {
        String dataDir = SysApoio.getInstallerDataDir();
        if (dataDir == null) return;
        new java.io.File(dataDir).mkdirs();
        org.apache.log4j.Appender a = org.apache.log4j.Logger.getRootLogger().getAppender("logfile");
        if (a instanceof org.apache.log4j.FileAppender) {
            org.apache.log4j.FileAppender fa = (org.apache.log4j.FileAppender) a;
            // log4j already opened the relative "counselor.log" in the launch CWD (= the EGF's folder
            // on a file-association launch) at static init, before main() ran. Capture that stray path,
            // redirect the appender to the data dir, then remove the orphaned file if it's still empty
            // (nothing is logged before this runs). Guarded so we never delete the real log on
            // portable/dev (dataDir == null returns above) or a file that actually has content.
            java.io.File stray = (fa.getFile() != null) ? new java.io.File(fa.getFile()).getAbsoluteFile() : null;
            String target = dataDir + java.io.File.separator + "counselor.log";
            fa.setFile(target);
            fa.activateOptions();
            if (stray != null && !stray.equals(new java.io.File(target).getAbsoluteFile())
                    && stray.isFile() && stray.length() == 0) {
                stray.delete();
            }
        }
    }

    /**
     * Bound the daily logs. The log uses DailyRollingFileAppender (one counselor.log per day; all of
     * a day's launches share it), but log4j 1.x ignores MaxBackupIndex on it, so old daily files
     * would pile up forever - which is why a prior change had switched away from it. Prune rotated
     * daily logs older than KEEP_DAYS at startup to keep retention bounded. Runs after
     * reconfigureLogForInstaller() so it prunes in the active log directory (the installer's data
     * dir when running from the MSI). Never touches the active counselor.log itself.
     */
    private static void pruneOldDailyLogs() {
        final int keepDays = 30;
        org.apache.log4j.Appender a = org.apache.log4j.Logger.getRootLogger().getAppender("logfile");
        if (!(a instanceof org.apache.log4j.FileAppender)) {
            return;
        }
        String active = ((org.apache.log4j.FileAppender) a).getFile();
        if (active == null) {
            return;
        }
        java.io.File logFile = new java.io.File(active);
        java.io.File dir = logFile.getParentFile();
        if (dir == null) {
            return;
        }
        // Rotated daily files are "counselor.log.<yyyy-MM-dd>"; the trailing dot excludes the active log.
        final String rotatedPrefix = logFile.getName() + ".";
        final long cutoff = System.currentTimeMillis() - keepDays * 24L * 60 * 60 * 1000;
        java.io.File[] rotated = dir.listFiles((d, name) -> name.startsWith(rotatedPrefix));
        if (rotated == null) {
            return;
        }
        for (java.io.File f : rotated) {
            if (f.lastModified() < cutoff) {
                f.delete();
            }
        }
    }
}
