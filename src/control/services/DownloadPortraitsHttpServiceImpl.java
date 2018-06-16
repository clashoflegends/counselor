/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import business.DownloadPortraitsService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Serguei
 */
public class DownloadPortraitsHttpServiceImpl implements DownloadPortraitsService {

    private static final Log LOG = LogFactory.getLog(DownloadPortraitsService.class);

    private static final String PROTOCOL_HOST = "http://";
    private static final String CLASH_HOST = "clashlegends.com";
    private static final String PORTRAITS_PATH = "/portraits/";
    private static final String PROPERTIES_FILENAME = "portraits.config";

    @Override
    public void checkNetworkConnection() throws ConnectException {
        boolean networkAvalaible = false;
        int timeout = 2000;
        
        
        
        try {
            final URL url = new URL(PROTOCOL_HOST + CLASH_HOST);
            final URLConnection conn = url.openConnection();
            conn.connect();
            networkAvalaible = true;
           
            /*
            InetAddress[] addresses = InetAddress.getAllByName(CLASH_HOST);
            for (InetAddress address : addresses) {
                if (address.isReachable(timeout)) {
                    networkAvalaible = true;
                }
            }
*/
        } catch (UnknownHostException e1) {
            LOG.error(e1);
            networkAvalaible = false;
        } catch (IOException e2) {
            LOG.error(e2);
            networkAvalaible = false;
        }

        if (!networkAvalaible) {
            throw new ConnectException();
        }
    }

    @Override
    public Properties getServerPropertiesFile() throws IOException {
        Properties propFile = null;

        try {
            propFile = new Properties();
            URL website = new URL(PROTOCOL_HOST + CLASH_HOST + PORTRAITS_PATH + PROPERTIES_FILENAME);

            InputStream is = website.openStream();
            propFile.load(is);
            is.close();
        } catch (MalformedURLException ex) {
            LOG.error(ex);
            throw new IOException();
        }
        return propFile;
    }

    @Override
    public File downloadPortraisFile(String portraitsFileName, String portraitsFolderName) throws FileNotFoundException {
        File file = null;
        try {
            URL website = new URL(PROTOCOL_HOST + CLASH_HOST + PORTRAITS_PATH + portraitsFileName);
            //https://www.colorado.edu/conflict/peace/download/peace.zip
            // speedtest.ftp.otenet.gr/files/test10Mb.db
          //  URL website = new URL("https://www.colorado.edu/conflict/peace/download/peace.zip");
            file = new File(portraitsFolderName + File.separator + portraitsFileName);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());

            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();

        } catch (IOException ex) {
            LOG.error(ex);
            throw new FileNotFoundException();
        }
        return file;
    }

    @Override
    public int getSize(String portraitsFileName) throws FileNotFoundException {

        int size = 0;
        URL website;
        URLConnection conn = null;
        try {            
            website = new URL(PROTOCOL_HOST + CLASH_HOST + PORTRAITS_PATH + portraitsFileName);
         //   website = new URL("https://www.colorado.edu/conflict/peace/download/peace.zip");
            
            conn = website.openConnection();
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            size = conn.getContentLength();
        } catch (IOException ex) {
            LOG.error(ex);
            throw new FileNotFoundException(ex.getMessage());
        } finally {
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }
        return size;
    }
}
