package control.services;

import business.DownloadPortraitsService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Downloads the portraits zip over HTTPS. As of 2026-06 the pack is hosted as a GitHub Release asset
 * (clashoflegends/portraits, public) at a stable "latest" URL - no more GoDaddy /portraits/ folder or
 * portraits.config manifest. The zip URL is supplied by the caller (a config value, see PortraitsChecker).
 */
public class DownloadPortraitsHttpServiceImpl implements DownloadPortraitsService {

    private static final Log LOG = LogFactory.getLog(DownloadPortraitsService.class);
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 120_000; // the pack is ~10MB+

    @Override
    public void checkNetworkConnection() throws ConnectException {
        try {
            URLConnection conn = URI.create("https://github.com").toURL().openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(CONNECT_TIMEOUT_MS);
            conn.connect();
        } catch (IOException e) {
            LOG.error("No network for portraits download: " + e);
            throw new ConnectException();
        }
    }

    @Override
    public File downloadPortraitsZip(String url, String destFolder) throws FileNotFoundException {
        File file = new File(destFolder, "portraits.zip");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setInstanceFollowRedirects(true); // GitHub latest-asset URL 302s to the CDN
            conn.setRequestProperty("User-Agent", "Counselor");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            try (InputStream in = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(file)) {
                in.transferTo(fos);
            }
        } catch (IOException ex) {
            LOG.error("Portraits download failed from " + url + ": " + ex);
            throw new FileNotFoundException();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return file;
    }

    @Override
    public int getSize(String url) throws FileNotFoundException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Counselor");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(CONNECT_TIMEOUT_MS);
            conn.setRequestMethod("HEAD");
            conn.connect();
            return conn.getContentLength(); // 0 if a redirect hop doesn't report it - only used for a size hint
        } catch (IOException ex) {
            LOG.error("Portraits size check failed: " + ex);
            throw new FileNotFoundException(ex.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
