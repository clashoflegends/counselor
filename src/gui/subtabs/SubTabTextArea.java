/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * SubTabTextArea.java
 *
 * Created on 17/Abr/2011, 9:35:23
 */
package gui.subtabs;

import gui.TabBase;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.Serializable;
import javax.swing.GroupLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class SubTabTextArea extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabTextArea.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    /**
     * Creates new form SubTabTextArea
     */
    public SubTabTextArea() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        detAjuda = new javax.swing.JScrollPane();
        jpAjuda = new javax.swing.JPanel();
        jtaTextArea = new javax.swing.JTextArea();

        detAjuda.setBorder(null);

        jtaTextArea.setFont(jtaTextArea.getFont().deriveFont(jtaTextArea.getFont().getSize()-1f));
        jtaTextArea.setLineWrap(true);
        jtaTextArea.setRows(80);
        jtaTextArea.setWrapStyleWord(true);
        jtaTextArea.setBorder(null);

        javax.swing.GroupLayout jpAjudaLayout = new javax.swing.GroupLayout(jpAjuda);
        jpAjuda.setLayout(jpAjudaLayout);
        jpAjudaLayout.setHorizontalGroup(
            jpAjudaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jtaTextArea)
        );
        jpAjudaLayout.setVerticalGroup(
            jpAjudaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jtaTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
        );

        detAjuda.setViewportView(jpAjuda);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(detAjuda))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 281, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(detAjuda))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane detAjuda;
    private javax.swing.JPanel jpAjuda;
    private javax.swing.JTextArea jtaTextArea;
    // End of variables declaration//GEN-END:variables

    public void setText(String text) {
        this.jtaTextArea.setText(text);
        this.jtaTextArea.setCaretPosition(0);
    }

    public void setTextBackground(Color color) {
        this.jtaTextArea.setBackground(color);
    }

    public void setFontMonospaced() {
        jtaTextArea.setFont(new java.awt.Font("Monospaced", 0, 12));
    }

    public void setFontText(Font f) {
        jtaTextArea.setFont(f);
    }

    public void replace(Component oldComponent) {
        GroupLayout parLayout = (GroupLayout) oldComponent.getParent().getLayout();
        parLayout.replace(oldComponent, this);
    }
}
