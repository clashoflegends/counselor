/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import java.awt.Color;
import java.awt.Dimension;
import java.io.Serializable;
import javax.swing.JFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class DialogHexInfo extends javax.swing.JDialog implements Serializable {
//make this follow hex selection
    private static final Log log = LogFactory.getLog(DialogHexInfo.class);
    // Variables declaration - do not modify
    private javax.swing.JScrollPane detContent;
    private javax.swing.JPanel jpContent;
    private javax.swing.JTextArea jtaTextArea;
    // End of variables declaration

    public DialogHexInfo(boolean modal) {
        super(new JFrame(), modal);
        this.setAlwaysOnTop(true);
        this.setPreferredSize(new Dimension(500, 400));
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
    }

    public void setText(String text) {
        this.jtaTextArea.setText(text);
        this.jtaTextArea.setCaretPosition(0);
    }

    public void setTextBackground(Color color) {
        this.jtaTextArea.setBackground(color);
    }
}
