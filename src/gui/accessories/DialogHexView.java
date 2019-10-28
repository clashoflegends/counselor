/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import control.support.DispatchManager;
import gui.services.IDispatchReceiver;
import gui.services.IPopupTabGui;
import java.awt.Color;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JFrame;
import model.Local;
import model.Nacao;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class DialogHexView extends javax.swing.JDialog implements IDispatchReceiver, Serializable {
//make this follow hex selection

    private static final Log log = LogFactory.getLog(DialogHexView.class);
    // Variables declaration - do not modify
    private javax.swing.JScrollPane detContent;
    private javax.swing.JPanel jpContent;
    private javax.swing.JTextArea jtaTextArea;
    // End of variables declaration

    public DialogHexView(boolean modal) {
        super(new JFrame(), modal);
        this.setAlwaysOnTop(true);
        initComponents();
    }

    private void initComponents() {

        detContent = new javax.swing.JScrollPane();
        jpContent = new javax.swing.JPanel();
        jtaTextArea = new javax.swing.JTextArea();

        detContent.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jtaTextArea.setLineWrap(true);
        jtaTextArea.setRows(80);
        jtaTextArea.setWrapStyleWord(true);

        javax.swing.GroupLayout jpContentLayout = new javax.swing.GroupLayout(jpContent);
        jpContent.setLayout(jpContentLayout);
        jpContentLayout.setHorizontalGroup(
                jpContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jtaTextArea));
        jpContentLayout.setVerticalGroup(
                jpContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jtaTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE));

        detContent.setViewportView(jpContent);
        this.add(detContent);
        //events
        DispatchManager.getInstance().registerForMsg(DispatchManager.WINDOWS_CLOSING, this);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                SettingsManager.getInstance().setConfigAndSaveToFile("GuiHexViewDetachedStatus", IPopupTabGui.POPUP_HIDDEN);
            }
        });
    }

    public void setText(String text) {
        this.jtaTextArea.setText(text);
        this.jtaTextArea.setCaretPosition(0);
    }

    public void setTextBackground(Color color) {
        this.jtaTextArea.setBackground(color);
    }

    @Override
    public void receiveDispatch(int msgName) {
        log.info(String.format("size (%s); coord: (%s);", this.getSize().toString(), this.getLocation().toString()));
    }

    @Override
    public void receiveDispatch(Nacao nation, PersonagemOrdem before, PersonagemOrdem after) {
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
    }

    @Override
    public void receiveDispatch(int msgName, Local local) {
    }

    @Override
    public void receiveDispatch(int msgName, Local local, int range) {
    }

    @Override
    public void receiveDispatch(int msgName, Component cmpnt) {
    }
}
