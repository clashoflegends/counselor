/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import control.support.ControlBase;
import gui.subtabs.SubTabOrdem;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Serializable;
import javax.swing.JDialog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author gurgel
 */
public class OrdemControlerFloater extends ControlBase implements Serializable, ComponentListener {

    private static final Log log = LogFactory.getLog(OrdemControlerFloater.class);
    private SubTabOrdem tabGui;

    public OrdemControlerFloater(SubTabOrdem tabOrdens) {
        setTabGui(tabOrdens);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        //listener do jDialog
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        //listener do jDialog
    }

    @Override
    public void componentShown(ComponentEvent e) {
        //listener do jDialog
    }

    @Override
    public void componentHidden(ComponentEvent event) {
        //listener do jDialog
        if (event.getSource() instanceof JDialog) {
            JDialog cb = (JDialog) event.getSource();
            //Now that we know which button was pushed, find out
            //whether it was selected or deselected.
            if ("dOrdem".equals(cb.getName())) {
                //criar floating window para ordens
                this.getTabGui().doAttachOrders();
            }
        }
    }

    private SubTabOrdem getTabGui() {
        return tabGui;
    }

    private void setTabGui(SubTabOrdem tabGui) {
        this.tabGui = tabGui;
    }
}
