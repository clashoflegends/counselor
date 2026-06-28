/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import business.DownloadPortraitsService;
import control.services.DownloadPortraitsHttpServiceImpl;
import control.support.DispatchManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Serguei
 */
public class DownloadProgressWork extends SwingWorker<Void, Void> {

    private static final Log LOG = LogFactory.getLog(DownloadProgressWork.class);

    private final DownloadPortraitsService portraitsService;
    private final String zipUrl;
    private final String portraitsFolder;
    private File downloadFile = null;
    private int filesCount = 0;
    private boolean succeeded = false;

    @Override
    protected Void doInBackground() {
        // Download then uncompress, both on this SwingWorker's own background thread. The previous
        // version spawned a nested Thread and busy-polled downloadFile.length() in a no-sleep
        // while-loop, which raced on downloadFile/filesCount, pinned a CPU core, and could throw from
        // setProgress(>100). Sequential work + milestone progress (0/70/100) is deterministic and safe.
        setProgress(0);
        // Set the path up front so done() can clean up a partial file even if the download throws.
        downloadFile = new File(new File(portraitsFolder), "portraits.zip");
        try {
            downloadFile = portraitsService.downloadPortraitsZip(zipUrl, portraitsFolder);
            setProgress(70); // downloaded; uncompressing next
            filesCount = doUncompressZip(downloadFile);
            succeeded = true;
        } catch (Throwable ex) {
            // Catch broadly: done() does not call get(), so an uncaught throwable would be swallowed.
            LOG.error("Portrait download/uncompress failed: " + ex, ex);
        }
        return null;
    }

    @Override
    protected void done() {
        setProgress(100);
        if (downloadFile != null) {
            downloadFile.delete(); // remove the temp zip (fully downloaded, or a partial on failure)
        }
        if (succeeded) {
            // Record the pack version just installed, so PortraitsChecker stops re-notifying for it.
            String tag = control.services.PortraitsChecker.fetchLatestTag();
            if (tag != null) {
                persistenceCommons.SettingsManager.getInstance().setConfigAndSaveToFile("PortraitsVersion", tag);
            }
            LOG.info("Portraits pack installed: " + filesCount + " images extracted into " + portraitsFolder
                    + (tag != null ? " (version " + tag + ")" : ""));
            String successLabel = "Successful download and uncompress process. A total of " + filesCount + " portraits have been obtained.";
            gui.services.Toast.show(successLabel, javax.swing.UIManager.getIcon("OptionPane.informationIcon"));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL, String.valueOf(1));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_RELOAD, "");
        } else {
            JOptionPane.showMessageDialog(null,
                    persistenceCommons.SettingsManager.getInstance().getBundleManager().getString("CONFIG.ERROR.ZIP"),
                    "Portraits", JOptionPane.ERROR_MESSAGE);
        }
    }

    public DownloadProgressWork(String zipUrl, String portraitsFolder) {
        portraitsService = new DownloadPortraitsHttpServiceImpl();
        this.zipUrl = zipUrl;
        this.portraitsFolder = portraitsFolder;
    }

    /**
     * Uncompress all the files contained in the compressed file and its folders in the same folder where the zip file is placed. Doesn't respects the
     * directory tree of the zip file. This method seems a clear candidate to ZipManager.
     *
     * @param file Zip file.
     * @return Count of files uncopressed.
     * @throws ZipException Exception.
     */
    private int doUncompressZip(File file) throws ZipException {
        int fileCount = 0;
        try {
            byte[] buf = new byte[1024];
            ZipFile zipFile = new ZipFile(file);

            Enumeration zipFileEntries = zipFile.entries();

            while (zipFileEntries.hasMoreElements()) {
                ZipEntry zipentry = (ZipEntry) zipFileEntries.nextElement();
                if (zipentry.isDirectory()) {
                    continue;
                }
                File entryZipFile = new File(zipentry.getName());
                File outputFile = new File(file.getParentFile(), entryZipFile.getName());
                FileOutputStream fileoutputstream = new FileOutputStream(outputFile);
                InputStream is = zipFile.getInputStream(zipentry);
                int n;
                while ((n = is.read(buf, 0, 1024)) > -1) {
                    fileoutputstream.write(buf, 0, n);
                }
                fileoutputstream.close();
                is.close();
                fileoutputstream.close();
                fileCount++;
            }
            zipFile.close();

        } catch (IOException ex) {
            throw new ZipException(ex.getMessage());
        }

        return fileCount;
    }

}
