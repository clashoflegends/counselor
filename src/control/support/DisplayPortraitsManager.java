/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import business.BusinessException;
import business.DownloadPortraitsService;
import control.services.DownloadPortraitsHttpServiceImpl;
import gui.accessories.DownloadProgressWork;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.text.DecimalFormat;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Serguei
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
        SettingsManager settingsManager = SettingsManager.getInstance();
        String folderPath = settingsManager.getConfig("PortraitsFolder", "");

        final File portraitsFolder = new File(folderPath);
        boolean enableCheck = portraitsFolder.exists() && portraitsFolder.list().length > 0;

        return enableCheck;
    }

    public synchronized void downloadPortraits(JPanel panel, PropertyChangeListener propertyChangeListener) {
        DownloadPortraitsService downloadService = new DownloadPortraitsHttpServiceImpl();
        try {
            // First check network connection
            downloadService.checkNetworkConnection();

            // Select download folder
            JFileChooser folderChooser = null;
            String currentFolder = SettingsManager.getInstance().getConfig("PortraitsFolder", "");
            if (currentFolder.isEmpty()) {
                folderChooser = new JFileChooser();
            } else {
                folderChooser = new JFileChooser(currentFolder);
            }
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setDialogTitle(labels.getString("CONFIG.SELECT.FOLDER"));

            int jFileReturn = folderChooser.showOpenDialog(panel);
            if (jFileReturn == JFileChooser.APPROVE_OPTION) {
                File file = folderChooser.getSelectedFile();
                SettingsManager.getInstance().setConfig("PortraitsFolder", file.getPath());

            } else {
                throw new BusinessException();
            }

            // Read server properties file
            Properties propServer = downloadService.getServerPropertiesFile();

            String portraitsFileName = propServer.getProperty("portraistFileNameFull");
            int fileSizeByte = downloadService.getSize(portraitsFileName);
            float fileSize = fileSizeByte / (1024f * 1024f);
            DecimalFormat df = new DecimalFormat("####.##");

            int returnVal = JOptionPane.showConfirmDialog(panel, String.format(labels.getString("CONFIG.DOWNLOAD.SIZE.CONFIRM"), df.format(fileSize)),
                    labels.getString("CONFIG.READY.DOWNLOAD"), JOptionPane.OK_CANCEL_OPTION
            );

            if (returnVal == JOptionPane.OK_OPTION) {
                String portraitFolder = SettingsManager.getInstance().getConfig("PortraitsFolder");

                final DownloadProgressWork dpw = new DownloadProgressWork(portraitsFileName, portraitFolder, (int) fileSizeByte);
                dpw.addPropertyChangeListener(propertyChangeListener);

                dpw.execute();
            }

        } catch (FileNotFoundException ex) {
            String errorLabel = labels.getString("CONFIG.ERROR.ZIP");
            JOptionPane.showMessageDialog(panel, errorLabel, "Internal error", JOptionPane.ERROR_MESSAGE);

        } catch (ConnectException ex) {
            String errorLabel = labels.getString("CONFIG.ERROR.NETWORK");
            JOptionPane.showMessageDialog(panel, errorLabel, "downloadPortraits", JOptionPane.ERROR_MESSAGE);

        } catch (IOException ex) {
            String errorLabel = labels.getString("CONFIG.ERROR.PROPERTY");
            JOptionPane.showMessageDialog(panel, errorLabel, "Internal error", JOptionPane.ERROR_MESSAGE);
        } catch (BusinessException ex) {
            // Select folder canceled
        } finally {

        }
    }

}
