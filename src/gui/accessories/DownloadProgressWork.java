/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import business.DownloadPortraitsService;
import control.services.DownloadPortraitsHttpServiceImpl;
import java.io.File;
import java.io.FileNotFoundException;
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
    private final String portraitsFileName;
    private final String portraitsFolder;
    private File downloadFile = null;
    private int filesCount = 0;
    private final int fileSize;

    @Override
    public Void doInBackground() throws FileNotFoundException {
        setProgress(0);
        try {
            Thread.sleep(1000);
            downloadFile = new File(new File(portraitsFolder), portraitsFileName);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        downloadFile = portraitsService.downloadPortraisFile(portraitsFileName, portraitsFolder);
                        filesCount = doUncompressZip(downloadFile);
                    } catch (FileNotFoundException ex) {
                        LOG.error("Qué mal todo. " + ex.getMessage());
                    } catch (ZipException ex) {
                        LOG.error("Qué mal todo descomprimiendo. " + ex.getMessage());
                    }
                }
            });
            t.start();
            while (t.isAlive()) {
                Float progreso = downloadFile.length() / (float) fileSize;
                progreso *= 100;
                int progress = progreso.intValue();

                setProgress(progress);
            }

        } catch (InterruptedException ignore) {
        }
        return null;
    }

    @Override
    public void done() {
        setProgress(100);
        downloadFile.delete();
        String successLabel = "Successful download and uncompress process. A total of " + filesCount + " portraits have been obtained.";
        JOptionPane.showMessageDialog(null, successLabel);

    }

    public DownloadProgressWork(String portraitsFileName, String portraitsFolder, int fileSize) {
        portraitsService = new DownloadPortraitsHttpServiceImpl();
        this.portraitsFileName = portraitsFileName;
        this.portraitsFolder = portraitsFolder;
        this.fileSize = fileSize;

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
