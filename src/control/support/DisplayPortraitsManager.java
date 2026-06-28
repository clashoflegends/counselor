/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import business.BusinessException;
import business.DownloadPortraitsService;
import control.services.DownloadPortraitsHttpServiceImpl;
import control.services.PortraitsChecker;
import gui.accessories.DownloadProgressWork;
import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.text.DecimalFormat;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * Drives portrait-pack download. The pack is a GitHub Release asset (clashoflegends/portraits) at a
 * stable "latest" URL (PortraitsZipUrl config, default PortraitsChecker.LATEST_ZIP_URL) - no GoDaddy
 * manifest. Counselor downloads + extracts it flat into the player's PortraitsFolder.
 */
public class DisplayPortraitsManager {

    private static DisplayPortraitsManager instance;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private DisplayPortraitsManager() {
    }

    public static synchronized DisplayPortraitsManager getInstance() {
        if (instance == null) {
            instance = new DisplayPortraitsManager();
        }
        return instance;
    }

    public boolean isShowPortraitEnableable() {
        String folderPath = SettingsManager.getInstance().getConfig("PortraitsFolder", "");
        final File portraitsFolder = new File(folderPath);
        return portraitsFolder.exists() && portraitsFolder.list() != null && portraitsFolder.list().length > 0;
    }

    /** Choose a folder (first time / change) then download the latest pack into it. */
    public synchronized void downloadPortraits(JPanel panel, PropertyChangeListener propertyChangeListener) {
        DownloadPortraitsService downloadService = new DownloadPortraitsHttpServiceImpl();
        try {
            downloadService.checkNetworkConnection();

            String currentFolder = SettingsManager.getInstance().getConfig("PortraitsFolder", "");
            JFileChooser folderChooser = currentFolder.isEmpty() ? new JFileChooser() : new JFileChooser(currentFolder);
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setDialogTitle(labels.getString("CONFIG.SELECT.FOLDER"));
            if (folderChooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION) {
                throw new BusinessException(); // selection canceled
            }
            File file = folderChooser.getSelectedFile();
            SettingsManager.getInstance().setConfig("PortraitsFolder", file.getPath());
            SettingsManager.getInstance().setConfig("ShowCharacterPortraits", "1");
            SettingsManager.getInstance().doConfigSave("ShowCharacterPortraits");

            confirmAndDownload(panel, propertyChangeListener, downloadService, file.getPath());
        } catch (ConnectException ex) {
            JOptionPane.showMessageDialog(panel, labels.getString("CONFIG.ERROR.NETWORK"), "downloadPortraits", JOptionPane.ERROR_MESSAGE);
        } catch (BusinessException ex) {
            // folder selection canceled - nothing to do
        }
    }

    /**
     * Re-download the latest pack into the already-configured folder (no folder chooser), showing the
     * size confirm dialog. Used by the "updated portraits available" toast (the player clicked it, but
     * the size prompt still gives context before the transfer).
     */
    public synchronized void downloadLatest(Component parent) {
        downloadLatest(parent, true);
    }

    /**
     * Re-download the latest pack into the already-configured folder (no folder chooser).
     *
     * @param confirm when false, skip the size confirm dialog and download silently (the
     *                autoDownloadPortraits path - the player already opted in via the setting, an
     *                out-of-the-blue "Ready to download" prompt has no context for them).
     */
    public synchronized void downloadLatest(Component parent, boolean confirm) {
        DownloadPortraitsService downloadService = new DownloadPortraitsHttpServiceImpl();
        try {
            downloadService.checkNetworkConnection();
            String folder = SettingsManager.getInstance().getConfig("PortraitsFolder", "");
            if (folder.isEmpty()) {
                return; // no folder set yet; the update check only fires when portraits are already in use
            }
            confirmAndDownload(parent, null, downloadService, folder, confirm);
        } catch (ConnectException ex) {
            JOptionPane.showMessageDialog(parent, labels.getString("CONFIG.ERROR.NETWORK"), "downloadPortraits", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmAndDownload(Component parent, PropertyChangeListener listener,
            DownloadPortraitsService downloadService, String folder) {
        confirmAndDownload(parent, listener, downloadService, folder, true);
    }

    private void confirmAndDownload(Component parent, PropertyChangeListener listener,
            DownloadPortraitsService downloadService, String folder, boolean confirm) {
        String url = SettingsManager.getInstance().getConfig("PortraitsZipUrl", PortraitsChecker.LATEST_ZIP_URL);
        if (confirm) {
            int sizeBytes = 0;
            try {
                sizeBytes = downloadService.getSize(url); // size is only a hint for the confirm dialog
            } catch (FileNotFoundException ex) {
                // ignore - proceed; a real failure surfaces during the download
            }
            float fileSize = sizeBytes / (1024f * 1024f);
            DecimalFormat df = new DecimalFormat("####.##");
            int ret = JOptionPane.showConfirmDialog(parent,
                    String.format(labels.getString("CONFIG.DOWNLOAD.SIZE.CONFIRM"), df.format(fileSize)),
                    labels.getString("CONFIG.READY.DOWNLOAD"), JOptionPane.OK_CANCEL_OPTION);
            if (ret != JOptionPane.OK_OPTION) {
                return;
            }
        }
        DownloadProgressWork dpw = new DownloadProgressWork(url, folder);
        if (listener != null) {
            dpw.addPropertyChangeListener(listener);
        }
        dpw.execute();
    }
}
