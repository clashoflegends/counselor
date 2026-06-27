package gui.services;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.WebCounselorManager;

/**
 * First-upload prompt that lets the player set their per-player Counselor token (jogador.cd_token).
 * Two ways to obtain it: fetch automatically (site login + password, never stored) or open the
 * website token page and paste it. Dismissible: [Skip] proceeds without a token, so this NEVER
 * blocks an order upload (the server side is log-only until Phase B). Shown only when no playerToken
 * is stored yet. Returns the saved token, or null if the player skipped.
 */
public final class TokenSetupDialog extends JDialog {

    private static final Log log = LogFactory.getLog(TokenSetupDialog.class);

    private final BundleManager labels;
    private final JTextField loginField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JTextField tokenField = new JTextField(30);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton fetchButton;
    private String result = null;

    private TokenSetupDialog(Window parent, BundleManager labels) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.labels = labels;
        this.fetchButton = new JButton(tx("TOKEN.SETUP.FETCH.BUTTON", "Fetch"));
        AppIcon.applyTo(this);
        setTitle(tx("TOKEN.SETUP.TITLE", "Set up your Counselor token"));
        setResizable(false);
        buildContent();
        pack();
        setLocationRelativeTo(parent);
    }

    /** Show the dialog modally. Returns the saved token, or null if skipped. */
    public static String show(java.awt.Component parent, BundleManager labels) {
        Window w = (parent instanceof Window) ? (Window) parent
                : javax.swing.SwingUtilities.getWindowAncestor(parent);
        TokenSetupDialog d = new TokenSetupDialog(w, labels);
        d.setVisible(true);
        return d.result;
    }

    private void buildContent() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);

        JLabel intro = new JLabel(tx("TOKEN.SETUP.INTRO",
                "<html><body style='width:420px'>Counselor can tag your order uploads with a personal token "
                + "so the game master knows they came from you. Fetch it with your website login, or open the "
                + "website token page and paste it here. You can skip this and set it later.</body></html>"));
        p.add(intro, c);

        // --- Option A: fetch by login + password ---
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(6, 0, 2, 0);
        p.add(sectionLabel(tx("TOKEN.SETUP.FETCH.HEADER", "Fetch automatically")), c);

        c.gridwidth = 1;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 0, 2, 8);
        p.add(new JLabel(tx("TOKEN.SETUP.LOGIN", "Website login or email:")), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        p.add(loginField, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 0, 2, 8);
        p.add(new JLabel(tx("TOKEN.SETUP.PASSWORD", "Password:")), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        p.add(passwordField, c);

        c.gridx = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(4, 0, 8, 0);
        fetchButton.addActionListener(e -> doFetch());
        p.add(fetchButton, c);

        // --- separator ---
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        p.add(new JSeparator(), c);

        // --- Option B: open page + paste ---
        c.gridy++;
        c.insets = new Insets(4, 0, 2, 0);
        p.add(sectionLabel(tx("TOKEN.SETUP.MANUAL.HEADER", "Or paste it from the website")), c);

        c.gridy++;
        c.insets = new Insets(2, 0, 4, 0);
        JButton openPage = new JButton(tx("TOKEN.SETUP.OPENPAGE", "Open the website token page"));
        openPage.addActionListener(e -> doOpenPage());
        p.add(openPage, c);

        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 0, 2, 8);
        p.add(new JLabel(tx("TOKEN.SETUP.TOKEN", "Token:")), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        p.add(tokenField, c);

        // --- status line ---
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(8, 0, 4, 0);
        statusLabel.setForeground(new java.awt.Color(0x99, 0x66, 0x00));
        p.add(statusLabel, c);

        // --- buttons ---
        JPanel buttons = new JPanel();
        JButton save = new JButton(tx("TOKEN.SETUP.SAVE", "Save"));
        JButton skip = new JButton(tx("TOKEN.SETUP.SKIP", "Skip for now"));
        save.addActionListener(e -> doSave());
        skip.addActionListener(e -> {
            result = null;
            dispose();
        });
        buttons.add(save);
        buttons.add(skip);
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(6, 0, 0, 0);
        p.add(buttons, c);

        getRootPane().setDefaultButton(save);
        setContentPane(p);
        setMinimumSize(new Dimension(480, 0));
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
        return l;
    }

    private void doFetch() {
        final String login = loginField.getText().trim();
        final String password = new String(passwordField.getPassword());
        if (login.isEmpty() || password.isEmpty()) {
            setStatus(tx("TOKEN.SETUP.NEEDCREDS", "Enter your website login and password first."));
            return;
        }
        fetchButton.setEnabled(false);
        setStatus(tx("TOKEN.SETUP.FETCHING", "Fetching..."));
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return WebCounselorManager.getInstance().fetchPlayerToken(login, password);
            }

            @Override
            protected void done() {
                fetchButton.setEnabled(true);
                try {
                    String tok = get();
                    if (tok != null && !tok.isEmpty()) {
                        tokenField.setText(tok);
                        setStatus(tx("TOKEN.SETUP.FETCH.OK", "Token fetched. Click Save to store it."));
                    } else {
                        setStatus(tx("TOKEN.SETUP.FETCH.BAD", "Login failed or too many attempts. Check your credentials."));
                    }
                } catch (Exception ex) {
                    log.warn("Token fetch failed: " + ex);
                    setStatus(tx("TOKEN.SETUP.FETCH.NET", "Could not reach the website. Try again or paste the token manually."));
                }
            }
        }.execute();
    }

    private void doOpenPage() {
        final String url = SettingsManager.getInstance().getConfig("TokenPageUrl", "http://clashlegends.com/PbmSite/p_token.php");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                setStatus(url);
            }
        } catch (Exception ex) {
            log.warn("Could not open token page: " + ex);
            setStatus(url);
        }
    }

    private void doSave() {
        String tok = tokenField.getText().trim();
        if (tok.isEmpty()) {
            setStatus(tx("TOKEN.SETUP.NEEDTOKEN", "Fetch or paste a token first, or click Skip."));
            return;
        }
        if (!tok.matches("[0-9a-fA-F]{20,}")) {
            // Site tokens are 40 hex chars (random_bytes(20)); reject a mis-paste (password, truncated
            // copy) rather than silently storing junk that would only log TOKINV on every upload.
            setStatus(tx("TOKEN.SETUP.BADFORMAT", "That does not look like a valid token. Fetch it, or copy it exactly from the website."));
            return;
        }
        SettingsManager.getInstance().setConfigAndSaveToFile("playerToken", tok);
        result = tok;
        dispose();
    }

    private void setStatus(String text) {
        statusLabel.setText(text == null || text.isEmpty() ? " " : text);
    }

    private String tx(String key, String fallback) {
        // BundleManager.getString never throws; it returns an "N/A (Missing Translation: ...)"
        // sentinel for unknown keys. Fall back to clean English in that case so the dialog reads
        // well even before the keys are localized.
        String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
