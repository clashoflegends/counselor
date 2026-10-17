package e2e;

import gui.subtabs.SubTabMagicItems;
import gui.tabs.TabArtefatosGui;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.lang.reflect.Field;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicItemCopyTest {
    @Test void bothDetailPanelsAllowCopyingWithoutEditing() throws Exception {
        TestWorld.load();
        SwingUtilities.invokeAndWait(() -> {
            try {
                checkCopy(new TabArtefatosGui("Items", "Items", null), "listaHistoria");
                checkCopy(new SubTabMagicItems(), "detail");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static void checkCopy(Object tab, String fieldName) throws Exception {
        final Field field = tab.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        final JTextComponent detail = (JTextComponent) field.get(tab);
        assertTrue(detail.isFocusable(), "Keyboard selection and Copy need focus");
        assertFalse(detail.isEditable(), "Descriptions remain read-only");
        detail.setText("Red Rain\n100% chance to activate");
        detail.select(9, detail.getDocument().getLength());
        final Clipboard clipboard = new Clipboard("magic-item-test");
        detail.getTransferHandler().exportToClipboard(detail, clipboard, TransferHandler.COPY);
        assertEquals("100% chance to activate", clipboard.getData(DataFlavor.stringFlavor));
    }
}
