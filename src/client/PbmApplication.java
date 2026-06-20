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
import gui.services.EgfDropHandler;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.util.MissingResourceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class PbmApplication extends Application implements Serializable {

    private static final Log log = LogFactory.getLog(PbmApplication.class);
    private final String autoStart;
    private final JgFrame frame = new JgFrame(getName());
    private final String configName = "GuiMainWindow";
    private boolean isMaximized = false;

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
        final long tStart = System.currentTimeMillis();
        //load configs
        int width = SettingsManager.getInstance().getConfigAsInt(configName + "SizeWidth", "-1");
        int height = SettingsManager.getInstance().getConfigAsInt(configName + "SizeHeight", "-1");
        int posX = SettingsManager.getInstance().getConfigAsInt(configName + "PositionX", "-1");
        int posY = SettingsManager.getInstance().getConfigAsInt(configName + "PositionY", "-1");
        final long tConfig = System.currentTimeMillis();
        // Pre-warm the shared image cache (idempotent singleton) so its eager load
        // is timed separately from the GUI build below instead of hiding inside it.
        ImageManager.getInstance();
        final long tImages = System.currentTimeMillis();

        MainResultWindowGui mainWin = null;
        try {
            //Create and set up the main content pane.
            //contentMainPane.setOpaque(true); //content panes must be opaque
            mainWin = new MainResultWindowGui(this.autoStart);
            frame.setContentPane(mainWin);
            frame.setTransferHandler(new EgfDropHandler(mainWin));
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
        final long tGui = System.currentTimeMillis();
        //default
        frame.pack();
        if (SettingsManager.getInstance().getConfig("maximizeWindowOnStart", "0").equals("1")) {
            isMaximized = true;
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        } else {
            isMaximized = false;
            frame.setExtendedState(Frame.NORMAL);
        }
        //send event to load GUI configs in other windows. 
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.GUI_STATUS_PERSIST);
        //centerWindow(frame);
        frame.setVisible(true);
        if (mainWin != null) {
            // Re-apply the title now that the frame is realized + visible. addNotify() during pack()
            // is unreliable for the autoload/double-click path, leaving the title as bare "Counselor".
            mainWin.applyWindowTitle();
        }
        setListeners(frame);
        final long tShown = System.currentTimeMillis();
        //check GitHub for a newer release (async, daemon thread; notifies title + status bar if found)
        control.services.UpdateChecker.checkAsync(mainWin);
        log.info(String.format(
                "STARTUP TIMING: jvm+preInit=%dms config=%dms images=%dms gui+autoload=%dms pack+show=%dms | total-since-launch=%dms",
                tStart - client.Main.launchMs, tConfig - tStart, tImages - tConfig,
                tGui - tImages, tShown - tGui, tShown - client.Main.launchMs));
        log.info("Interface carregada and exibida.");
    }

    private void saveFrameConfigs() {
        if (isMaximized) {
            //save maximize windows config
            SettingsManager.getInstance().setConfig("maximizeWindowOnStart", "1");
        } else {
            //save minimized windows config
            SettingsManager.getInstance().setConfig(configName + "SizeWidth", frame.getSize().width + "");
            SettingsManager.getInstance().setConfig(configName + "SizeHeight", frame.getSize().height + "");
            SettingsManager.getInstance().setConfig(configName + "PositionX", frame.getLocation().x + "");
            SettingsManager.getInstance().setConfig(configName + "PositionY", frame.getLocation().y + "");
            SettingsManager.getInstance().setConfig("maximizeWindowOnStart", "0");
        }
        SettingsManager.getInstance().saveToFile();
        log.info(String.format("Closing... %s ...", SysApoio.getPidOs()));
    }

    private void setListeners(JgFrame frame) {
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.WINDOWS_CLOSING);
                saveFrameConfigs();
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
                    isMaximized = true;
                    saveFrameConfigs();
                } else if ((oldState & Frame.MAXIMIZED_BOTH) != 0 && (newState & Frame.MAXIMIZED_BOTH) == 0) {
                    //Frame was de-maximized
                    isMaximized = false;
                    saveFrameConfigs();
                }
            }
        });
    }
}
