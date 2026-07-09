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
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.net.URI;
import java.util.MissingResourceException;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
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

        // App-wide right-click Cut/Copy/Paste/Select All + Ctrl+C on every text component (one global
        // hook, covers all windows/dialogs without per-form changes).
        gui.services.TextContextMenu.install();

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
        // Expose cold-start time for upload telemetry (player_stats.vl_startup_ms).
        SysApoio.setStartupTimeMs(tShown - client.Main.launchMs);
        //check GitHub for a newer release (async, daemon thread; notifies title + status bar if found)
        control.services.UpdateChecker.checkAsync(mainWin);
        //check GitHub for a newer portrait pack (async; toast to download if the player uses portraits)
        control.services.PortraitsChecker.checkAsync(mainWin);
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

    /**
     * Crash-exit dialog (the base {@link Application} builds + shows it modally, then calls System.exit).
     * The crash report was already POSTed by {@code Application.uncaughtException}. Here we additionally
     * nudge OUTDATED clients to update: {@link control.services.UpdateChecker} runs an async check ~20s
     * after startup and caches whether a newer build exists, so {@code getAvailableVersion()} is a
     * non-blocking "are we behind" signal - no network on the crash path. When we KNOW a newer build
     * exists (and this was not an OOM, which we can't reliably download our way out of), we tell the
     * player the issue is likely already fixed and offer a one-click "Update now" (reuses the normal
     * downloader: self-install for portable-jar/dmg, releases page otherwise) plus a clickable link.
     * Otherwise we show the plain message, matching the base behavior.
     */
    @Override
    protected JDialog getUncaughtExceptionDialog() {
        final BundleManager labels = SettingsManager.getInstance().getBundleManager();
        final boolean oom = getUncaughtThrowable() instanceof OutOfMemoryError;
        final String newer = control.services.UpdateChecker.getAvailableVersion();
        final String title = tx(labels, "CRASH.TITLE", "Error");
        final String baseMsg = oom
                ? tx(labels, "CRASH.MSG.OOM", "Java ran out of memory. " + getName() + " will now exit. Just relaunch it and it will be fine.")
                : tx(labels, "CRASH.MSG.GENERIC", "An unrecoverable error has occurred. " + getName() + " will now exit. You can send the counselor.log file to the GM for investigation.");

        // Only offer the update path when we KNOW a newer build exists and this was not an OOM.
        if (newer == null || oom) {
            JOptionPane pane = new JOptionPane(baseMsg, JOptionPane.ERROR_MESSAGE);
            JDialog dialog = pane.createDialog(null, title);
            dialog.setAlwaysOnTop(true);
            dialog.toFront();
            dialog.setVisible(true);
            return dialog;
        }

        final String url = control.services.UpdateChecker.getLatestReleaseUrl();
        final String outdatedTmpl = tx(labels, "CRASH.OUTDATED",
                "Your %s is outdated (%s available). There is a good chance this issue is already fixed in the new version.");
        String outdatedMsg;
        try {
            outdatedMsg = String.format(outdatedTmpl, getName(), newer);
        } catch (RuntimeException badFormat) {
            // a mis-typed % in some translation must not turn the crash dialog into another crash
            outdatedMsg = String.format("Your %s is outdated (%s available). There is a good chance this issue is already fixed in the new version.", getName(), newer);
        }
        final String outdated = outdatedMsg;
        JEditorPane body = new JEditorPane("text/html",
                "<html><body style='width:380px; font-family:sans-serif'>"
                + "<p>" + baseMsg + "</p>"
                + "<p><b>" + outdated + "</b></p>"
                + "<p>" + tx(labels, "CRASH.UPDATE.LINK", "Update here:")
                + " <a href='" + url + "'>" + url + "</a></p></body></html>");
        body.setEditable(false);
        body.setOpaque(false);
        body.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                browse(url);
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(body, BorderLayout.CENTER);

        Object[] options = {
            tx(labels, "CRASH.BTN.UPDATE", "Update now"),
            tx(labels, "CRASH.BTN.EXIT", "Exit")
        };
        JOptionPane pane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, options[1]);
        JDialog dialog = pane.createDialog(null, title);
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        dialog.setVisible(true); // modal - blocks until the player picks
        if (options[0].equals(pane.getValue())) {
            runUpdateBeforeExit(labels);
        }
        return dialog;
    }

    /**
     * Run the update synchronously while a small modal progress dialog is up, then return so the base
     * class exits. The download/stage runs off the EDT in a worker; the modal dialog pumps its own event
     * loop so {@code done()} can dispose it - this blocks the crash-exit until staging finishes, without
     * freezing. Never relaunches (unsaved orders are safe); the staged update is active on the next start.
     */
    private void runUpdateBeforeExit(BundleManager labels) {
        final JDialog progress = new JDialog((Frame) null, getName(), true);
        progress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        p.add(new JLabel(tx(labels, "CRASH.DOWNLOADING", "Getting the update... " + getName() + " will close when it finishes.")), BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(bar, BorderLayout.CENTER);
        progress.setContentPane(p);
        progress.pack();
        progress.setLocationRelativeTo(null);
        progress.setAlwaysOnTop(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    control.services.UpdateDownloader.stageLatestForCrashExit();
                } catch (Throwable ignore) {
                    // best-effort; base class exits regardless
                }
                return null;
            }

            @Override
            protected void done() {
                progress.dispose();
            }
        }.execute();
        progress.setVisible(true); // blocks (modal) until done() disposes it
    }

    private void browse(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            log.warn("Could not open the releases page: " + ex);
        }
    }

    /** Localized label with an English fallback (matches UpdateDownloader.tx); never throws. */
    private static String tx(BundleManager labels, String key, String fallback) {
        try {
            String s = labels.getString(key);
            return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
        } catch (Exception e) {
            return fallback;
        }
    }
}
