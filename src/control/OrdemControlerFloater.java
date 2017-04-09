/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import control.support.ControlBase;
import gui.services.IPopupTabGui;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import javax.swing.JDialog;
import javax.swing.JToggleButton;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author gurgel
 */
public class OrdemControlerFloater extends ControlBase implements Serializable, ComponentListener, ItemListener {

    private static final Log log = LogFactory.getLog(OrdemControlerFloater.class);
    private IPopupTabGui tabGui;

    public OrdemControlerFloater(IPopupTabGui tabOrdens) {
        setTabGui(tabOrdens);
    }

    private IPopupTabGui getTabGui() {
        return tabGui;
    }

    private void setTabGui(IPopupTabGui tabGui) {
        this.tabGui = tabGui;
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
            if ("dPopup".equals(cb.getName())) {
                //criar floating window para ordens
                this.getTabGui().doAttachPopup();
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() instanceof JToggleButton) {
            final JToggleButton cb = (JToggleButton) event.getSource();
            try {
                if ("jbDetach".equals(cb.getActionCommand())) {
                    //criar floating window para ordens
                    getTabGui().doDetachPopup();
                }
            } catch (ClassCastException ex) {
                log.debug("hum... suspicious");
            }
        }
    }
}
