package gui.services;

import gui.MainResultWindowGui;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

public class EgfDropHandler extends TransferHandler {

    private final MainResultWindowGui gui;

    public EgfDropHandler(MainResultWindowGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            for (File f : files) {
                String name = f.getName();
                if (name.endsWith(".egf")) {
                    final File target = name.endsWith(".rc.egf")
                            ? new File(f.getParent(), name.substring(0, name.length() - 7) + ".rr.egf")
                            : f;
                    SwingUtilities.invokeLater(() -> gui.openEgfFile(target));
                    return true;
                }
            }
        } catch (Exception ex) {
            // drop silently failed
        }
        return false;
    }
}
