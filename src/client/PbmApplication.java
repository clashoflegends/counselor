/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import baseLib.Application;
import baseLib.JgFrame;
import business.ImageManager;
import gui.MainResultWindowGui;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.MissingResourceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class PbmApplication extends Application {

    private static final Log log = LogFactory.getLog(PbmApplication.class);
    String autoStart;

    public PbmApplication() {
        super();
        this.autoStart = null;
    }

    public PbmApplication(String autoStart) {
        super();
        this.autoStart = autoStart;
    }

    @Override
    public String getName() {
        return "Counselor";
    }

    @Override
    protected void init() {
        this.createAndShowGUI();
    }

    private void createAndShowGUI() {
        JgFrame frame = new JgFrame(getName());
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        try {
            //Create and set up the main content pane.
            //contentMainPane.setOpaque(true); //content panes must be opaque
            MainResultWindowGui mainWin = new MainResultWindowGui(this.autoStart);
            frame.setContentPane(mainWin);
            //set icon
            frame.setIconImage(ImageManager.getInstance().getIconApp());
        } catch (MissingResourceException e) {
            log.fatal(e);
        }
        //default
        frame.pack();
        //frame.setLocationRelativeTo(null);
        if (SettingsManager.getInstance().getConfig("maximizeWindowOnStart", "0").equals("1")) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        } else {
            frame.setExtendedState(Frame.NORMAL);
        }
        //centerWindow(frame);
        frame.setVisible(true);
        log.info("Interface carregada and exibida.");
    }
}
