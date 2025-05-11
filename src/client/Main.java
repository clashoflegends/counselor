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
        // Invokes Gui to display turn results
        if (!SysApoio.getVersionJavaSpecification().equals("1.8")) {
            log.fatal("Counselor only works with Java Community version 1.8. Dounload it from java.com");
        }
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
        final String autoload;
        if (args.length == 1) {
            autoload = args[0];
        } else {
            autoload = sm.getConfig("autoLoad");
        }
        sm.setWorldBuilder(sm.getConfig("worldBuilder", "0").equalsIgnoreCase("1"));
        sm.setRadialMenu(sm.getConfig("newUi", "1").equalsIgnoreCase("1"));
        //load application
        new PbmApplication(autoload).start(); //filename to autoload results file
    }
}
