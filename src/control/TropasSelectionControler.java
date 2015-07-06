/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import gui.subtabs.SubTabTropas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.JButton;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class TropasSelectionControler implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(TropasSelectionControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    ;
    private final SubTabTropas tabGui;

    public TropasSelectionControler(SubTabTropas aThis) {
        tabGui = aThis;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JButton) {
            JButton jbDir = (JButton) actionEvent.getSource();
            if ("apaga".equals(jbDir.getActionCommand())) {
                //apaga todas as direcoes.
//                tabGui.doApaga();
            } else {
                //executa o movimento
//                tabGui.doMovementTagAdd(jbDir.getActionCommand());
            }
        } else {
            log.info(labels.getString("EVENTO.NAO.MAPEADO"));
        }

    }
}
