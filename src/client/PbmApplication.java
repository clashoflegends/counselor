/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import baseLib.Application;
import baseLib.JgFrame;
import business.ImageManager;
import control.support.DispatchManager;
import gui.MainResultWindowGui;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.util.MissingResourceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class PbmApplication extends Application implements Serializable {

    private static final Log log = LogFactory.getLog(PbmApplication.class);
    private final String autoStart;
    private final JgFrame frame = new JgFrame(getName());
    private final String configName = "MainWindow";

    public PbmApplication() {
        super();
        this.autoStart = null;
    }

    public PbmApplication(String autoStartLoading) {
        super();
        this.autoStart = autoStartLoading;
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
        //load configs
        int width = SettingsManager.getInstance().getConfigAsInt(configName + "SizeWidth", "-1");
        int height = SettingsManager.getInstance().getConfigAsInt(configName + "SizeHeight", "-1");
        int posX = SettingsManager.getInstance().getConfigAsInt(configName + "PositionX", "-1");
        int posY = SettingsManager.getInstance().getConfigAsInt(configName + "PositionY", "-1");

        try {
            //Create and set up the main content pane.
            //contentMainPane.setOpaque(true); //content panes must be opaque
            MainResultWindowGui mainWin = new MainResultWindowGui(this.autoStart);
            frame.setContentPane(mainWin);
            //set icon
            frame.setIconImage(ImageManager.getInstance().getIconApp());
            if (posX != -1) {
                frame.setLocation(posX, posY);
            }
            if (width != -1) {
                frame.setPreferredSize(new Dimension(width, height));
            }
        } catch (MissingResourceException e) {
            log.fatal(e);
        }
        //default
        frame.pack();
        if (SettingsManager.getInstance().getConfig("maximizeWindowOnStart", "0").equals("1")) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        } else {
            frame.setExtendedState(Frame.NORMAL);

        }
        //send event to load GUI configs in other windows. 
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.GUI_STATUS_PERSIST);
        //centerWindow(frame);
        frame.setVisible(true);
        setListeners(frame);
        log.info("Interface carregada and exibida.");
    }

    private void saveFrameConfigs(int msgName) {
        if (DispatchManager.WINDOWS_MAXIMIZING == msgName) {
            //save windows config
            SettingsManager.getInstance().setConfig("maximizeWindowOnStart", "1");
        } else if (DispatchManager.WINDOWS_MINIMIZING == msgName) {
            //save windows config
            SettingsManager.getInstance().setConfig("maximizeWindowOnStart", "0");
        }
        SettingsManager.getInstance().setConfig(configName + "SizeWidth", frame.getSize().width + "");
        SettingsManager.getInstance().setConfig(configName + "SizeHeight", frame.getSize().height + "");
        SettingsManager.getInstance().setConfig(configName + "PositionX", frame.getLocation().x + "");
        SettingsManager.getInstance().setConfigAndSaveToFile(configName + "PositionY", frame.getLocation().y + "");
    }

    private void setListeners(JgFrame frame) {
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.WINDOWS_CLOSING);
                saveFrameConfigs(DispatchManager.WINDOWS_CLOSING);
                exit();
            }

        });
        frame.addWindowStateListener(new WindowAdapter() {

            @Override
            public void windowStateChanged(WindowEvent evt) {
                int oldState = evt.getOldState();
                int newState = evt.getNewState();

                if ((oldState & Frame.ICONIFIED) == 0 && (newState & Frame.ICONIFIED) != 0) {
                    //Frame was iconized
                } else if ((oldState & Frame.ICONIFIED) != 0 && (newState & Frame.ICONIFIED) == 0) {
                    //Frame was deiconized
                }

                if ((oldState & Frame.MAXIMIZED_BOTH) == 0 && (newState & Frame.MAXIMIZED_BOTH) != 0) {
                    //Frame was maximized
                    saveFrameConfigs(DispatchManager.WINDOWS_MAXIMIZING);
                } else if ((oldState & Frame.MAXIMIZED_BOTH) != 0 && (newState & Frame.MAXIMIZED_BOTH) == 0) {
                    //Frame was de-maximized
                    saveFrameConfigs(DispatchManager.WINDOWS_MINIMIZING);
                }
            }
        });
    }
}
