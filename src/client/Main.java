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

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        reconfigureLogForInstaller();
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
        } else {
            autoload = sm.getConfig("autoLoad");
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
            fa.setFile(dataDir + java.io.File.separator + "counselor.log");
            fa.activateOptions();
        }
    }
}
