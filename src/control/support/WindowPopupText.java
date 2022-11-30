/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import baseLib.GenericoTableModel;
import gui.subtabs.SubTabBaseList;
import gui.subtabs.SubTabTextArea;
import java.awt.Component;
import java.io.Serializable;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 *
 * @author jmoura
 */
public class WindowPopupText implements Serializable {

    public static void showWindowText(String message, String title, Component relativeTo) {
        JDialog dPopup = new JDialog(new JFrame(), false);
        SubTabTextArea stContent = new SubTabTextArea();
        stContent.setText(message);
        //monta a floating window 
        dPopup.setTitle(title);
        dPopup.add(stContent);
        dPopup.pack();
        dPopup.setLocationRelativeTo(relativeTo);
        dPopup.setVisible(true);
    }

    public static void showWindowTable(GenericoTableModel model, String title, Component relativeTo) {
        JDialog dPopup = new JDialog(new JFrame(), false);
        SubTabBaseList stContent = new SubTabBaseList();
        stContent.setListModel(model);
        //monta a floating window 
        dPopup.setTitle(title);
        dPopup.add(stContent);
        dPopup.pack();
        dPopup.setLocationRelativeTo(relativeTo);
        dPopup.setVisible(true);
    }
}
