/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import baseLib.SysApoio;
import baseLib.SysProperties;
import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.SettingsManager;

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
        log.info("Starting...");
        log.info("Counselor version: " + SysApoio.getVersion("version_counselor"));
        log.info("Commons version: " + SysApoio.getVersion("version_commons"));
        SettingsManager.getInstance().setConfigurationMode("Client");
        SettingsManager.getInstance().setLanguage(SysProperties.getProps("language", "en"));
        String autoload = SysProperties.getProps("autoLoad");
        SettingsManager.getInstance().setWorldBuilder(SysProperties.getProps("worldBuilder", "0").equalsIgnoreCase("1"));
        SettingsManager.getInstance().setRadialMenu(SysProperties.getProps("newUi", "1").equalsIgnoreCase("1"));
        //load application
        new PbmApplication(autoload).start(); //true to autoload results file
    }
}
