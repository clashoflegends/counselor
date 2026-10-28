package gui.services;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import modelWeb.PartidaJogadorWebInfo;
import persistenceCommons.WebCounselorManager;
import persistenceCommons.WebCounselorManager.UploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrowserUploadFallbackTest {
    private static final UploadResult CHALLENGE = new UploadResult(WebCounselorManager.ERROR_BROWSER_VERIFICATION,
            "Please wait while your request is being verified...", true);
    private final ResourceBundle labels = ResourceBundle.getBundle("labels", Locale.ROOT);

    @Test
    void genericHtmlMessageIsPlainTextAndLocalizedWithoutClaimingRejection(@TempDir Path directory) throws Exception {
        String websiteMessage = "<html><img src='http://example.invalid/'> Access unavailable";
        SwingUtilities.invokeAndWait(() -> {
            for (String lang : new String[]{"", "pt", "es", "it", "ca"}) {
                ResourceBundle bundle = ResourceBundle.getBundle("labels", lang.isEmpty() ? Locale.ROOT : Locale.of(lang));
                var result = new UploadResult(WebCounselorManager.ERROR_HTML_RESPONSE, websiteMessage, true);
                JPanel panel = BrowserUploadFallback.createPanel(bundle::getString, directory.resolve("orders.rc.egf").toFile(),
                        info(0), result, new FakeActions());
                assertTrue(text(panel).contains(websiteMessage));
                assertTrue(text(panel).contains(bundle.getString("UPLOAD.HTML.MESSAGE")));
                assertFalse(text(panel).contains(bundle.getString("UPLOAD.BROWSER.MESSAGE")));
                assertTrue(WebsiteResponseMessages.format(bundle::getString, "").contains(bundle.getString("WEBSITE.HTML.EMPTY")));
                var content = WebsiteResponseMessages.content(websiteMessage);
                var editor = components(content, javax.swing.JTextArea.class).get(0);
                assertEquals(websiteMessage, editor.getText());
                assertInstanceOf(javax.swing.text.PlainDocument.class, editor.getDocument());
            }
        });
    }

    @Test
    void guidanceAndButtonsPreserveSubmissionModeInEveryLanguage(@TempDir Path directory) throws Exception {
        File file = directory.resolve("orders with spaces.rc.egf").toFile();
        SwingUtilities.invokeAndWait(() -> {
            for (String lang : new String[]{"", "pt", "es", "it", "ca"}) {
                ResourceBundle bundle = ResourceBundle.getBundle("labels", lang.isEmpty() ? Locale.ROOT : Locale.of(lang));
                for (int mode = 0; mode < 3; mode++) {
                    PartidaJogadorWebInfo info = info(mode);
                    JPanel panel = BrowserUploadFallback.createPanel(bundle::getString, file, info, CHALLENGE, new FakeActions());
                    List<JButton> buttons = components(panel, JButton.class).stream()
                            .filter(b -> b.getText() != null && !b.getText().isEmpty()).toList();
                    assertEquals(mode == 0 ? 2 : 1, buttons.size());
                    String text = text(panel);
                    assertTrue(text.contains(file.getAbsolutePath()));
                    assertFalse(text.contains("%s"));
                    assertFalse(text.contains("\uFFFD"));
                    assertTrue(text.contains(bundle.getString("UPLOAD.BROWSER.MESSAGE")));
                    assertTrue(text.contains(CHALLENGE.message()));
                    if (mode == 0) {
                        assertTrue(text.contains("sample.owner"));
                        assertTrue(text.contains("321"));
                        assertTrue(text.contains(BrowserUploadFallback.MY_GAMES.toString()));
                    } else {
                        assertFalse(text.contains(BrowserUploadFallback.MY_GAMES.toString()));
                        assertTrue(text.contains(bundle.getString(mode == 1 ? "UPLOAD.BROWSER.BEHALF" : "UPLOAD.BROWSER.SHADOW")));
                    }
                }
            }
        });
    }

    @Test
    void bothActionsStayAvailableAndOnlyOpenTheFixedSiteAndSavedParent(@TempDir Path directory) throws Exception {
        File saved = Files.writeString(directory.resolve("orders test.rc.egf"), "test").toFile();
        FakeActions actions = new FakeActions();
        JPanel panel = panel(saved, actions);
        clickAndWait(panel, labels.getString("UPLOAD.BROWSER.OPEN"));
        assertEquals(URI.create("http://clashlegends.com/PbmSite/p_mygames.php"), actions.uri);
        clickAndWait(panel, labels.getString("UPLOAD.BROWSER.FOLDER"));
        assertEquals(saved.getAbsoluteFile().getParentFile(), actions.folder);
        assertEquals("test", Files.readString(saved.toPath()));
    }

    @Test
    void failedDesktopLaunchKeepsCopyableRecoveryDetails(@TempDir Path directory) throws Exception {
        File saved = Files.writeString(directory.resolve("orders.rc.egf"), "test").toFile();
        FakeActions actions = new FakeActions();
        actions.fail = true;
        JPanel panel = panel(saved, actions);
        clickAndWait(panel, labels.getString("UPLOAD.BROWSER.OPEN"));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(text(panel).contains(labels.getString("UPLOAD.BROWSER.OPEN.FAILED")));
            assertTrue(text(panel).contains(saved.getAbsolutePath()));
        });
        actions.fail = false;
        Files.delete(saved.toPath());
        clickAndWait(panel, labels.getString("UPLOAD.BROWSER.FOLDER"));
        SwingUtilities.invokeAndWait(() -> assertTrue(text(panel).contains(labels.getString("UPLOAD.BROWSER.FOLDER.FAILED"))));
        assertNull(actions.folder, "Missing file must not open an unrelated folder");
    }

    private JPanel panel(File saved, FakeActions actions) throws Exception {
        AtomicReference<JPanel> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(BrowserUploadFallback.createPanel(labels::getString, saved, info(0), CHALLENGE, actions)));
        return result.get();
    }

    private static PartidaJogadorWebInfo info(int mode) {
        PartidaJogadorWebInfo info = new PartidaJogadorWebInfo();
        info.setPlayerLogin("sample.owner");
        info.setGameId(321);
        info.setOnBehalf(mode != 0);
        info.setShadow(mode == 2);
        return info;
    }

    private static void clickAndWait(JPanel panel, String caption) throws Exception {
        AtomicReference<JButton> target = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JButton button = components(panel, JButton.class).stream().filter(b -> b.getText().equals(caption)).findFirst().orElseThrow();
            target.set(button);
            button.doClick();
            assertFalse(button.isEnabled());
        });
        for (int i = 0; i < 100; i++) {
            AtomicReference<Boolean> enabled = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> enabled.set(target.get().isEnabled()));
            if (enabled.get()) { return; }
            Thread.sleep(30);
        }
        fail("Desktop action did not finish");
    }

    private static String text(Container panel) {
        return components(panel, JTextComponent.class).stream().map(JTextComponent::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static <T> List<T> components(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) { result.add(type.cast(child)); }
            if (child instanceof Container nested) { result.addAll(components(nested, type)); }
        }
        return result;
    }

    private static class FakeActions implements BrowserUploadFallback.Actions {
        volatile URI uri;
        volatile File folder;
        volatile boolean fail;
        @Override public void browse(URI value) throws IOException {
            if (fail) { throw new IOException("Synthetic launch failure"); }
            uri = value;
        }
        @Override public void openFolder(File value) { folder = value; }
    }
}
