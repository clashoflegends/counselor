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
                    final File target;
                    if (name.endsWith(".rc.egf")) {
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("^orders_(\\d+)_(\\d+)\\.(.+)\\.rc\\.egf$")
                                .matcher(name);
                        if (m.matches()) {
                            int turn = Integer.parseInt(m.group(2)) - 1;
                            target = new File(f.getParent(), "game_" + m.group(1) + "_" + turn + "." + m.group(3) + ".rr.egf");
                        } else {
                            target = new File(f.getParent(), name.substring(0, name.length() - 7) + ".rr.egf");
                        }
                    } else {
                        target = f;
                    }
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
