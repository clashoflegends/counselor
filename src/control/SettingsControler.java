/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import control.support.ControlBase;
import gui.MainSettingsGui;
import gui.MainSettingsGui.ComboItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.Serializable;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import persistenceCommons.SettingsManager;
import persistenceLocal.PathFactory;

/**
 *
 * @author serguei
 */
public class SettingsControler extends ControlBase implements Serializable, ActionListener, ChangeListener  {
    
    
    private final MainSettingsGui settingsGui;
    
    public SettingsControler(MainSettingsGui jpanel) {
        this.settingsGui = jpanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("fSaves")) {
           
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);       
            
            int returnVal = fc.showOpenDialog(settingsGui);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                SettingsManager.getInstance().setConfig("saveDir", file.getPath());
                settingsGui.getSaveDirTextField().setText(file.getPath());              
            }
        } else if (actionCommand.equals("languageCombo")) {
            JComboBox languageCombo = (JComboBox)e.getSource();
            String value = ((ComboItem)languageCombo.getSelectedItem()).getValue();
            SettingsManager.getInstance().setConfig("language", value);
        } else if (actionCommand.equals("fLoad")) {
           
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);       
            
            int returnVal = fc.showOpenDialog(settingsGui);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                SettingsManager.getInstance().setConfig("loadDir", file.getPath());
                settingsGui.getLoadDirTextField().setText(file.getPath());              
            }
        } else if (actionCommand.equals("fLoadAuto")) {
           
            JFileChooser fc = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
            fc.setFileFilter(PathFactory.getFilterResults());
            
            int returnVal = fc.showOpenDialog(settingsGui);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                SettingsManager.getInstance().setConfig("autoLoad", file.getPath());
                settingsGui.getAutoLoadTextField().setText(file.getPath());              
            }
        } else if (actionCommand.equals("fLoadAutoAction")) {
           
            JFileChooser fc = new JFileChooser();            
            fc.setFileFilter(PathFactory.getFilterAcoes());
            
            int returnVal = fc.showOpenDialog(settingsGui);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                SettingsManager.getInstance().setConfig("autoLoadActions", file.getPath());
                settingsGui.getAutoLoadActionTextField().setText(file.getPath());              
            }
        } else if (actionCommand.equals("overrideAction")) {
           JCheckBox overCheck = (JCheckBox)e.getSource();
           int selected = (overCheck.isSelected()) ? 1 : 0;
           SettingsManager.getInstance().setConfig("OverrideElimination", String.valueOf(selected));
           
        } else if (actionCommand.equals("playerEmail")) {
           
            String playerEmail = ((JTextField)e.getSource()).getText();
            
            if (playerEmail != null && !playerEmail.isEmpty()) {
                 SettingsManager.getInstance().setConfig("MyEmail", playerEmail);
            }
        } else if (actionCommand.equals("showPopUp")) {           
            JCheckBox popUpCheck = (JCheckBox)e.getSource();
            int selected = (popUpCheck.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("SendOrderConfirmationPopUp", String.valueOf(selected));
            
        } else if (actionCommand.equals("recieveConfirm")) {           
            JCheckBox showCheck = (JCheckBox)e.getSource();
            int selected = (showCheck.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("SendOrderReceiptRequest", String.valueOf(selected));
        } else if (actionCommand.equals("allFilter")) {           
            JRadioButton allCheck = (JRadioButton)e.getSource();
            int selected = (allCheck.isSelected()) ? 1 : 0;
             if (selected == 1) {
                 SettingsManager.getInstance().setConfig("filtro.default", String.valueOf(0));
             }
        } else if (actionCommand.equals("ownFilter")) {           
            JRadioButton ownCheck = (JRadioButton)e.getSource();
            int selected = (ownCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("filtro.default", String.valueOf(1));
            }
        } else if (actionCommand.equals("alphaSort")) {           
            JRadioButton alphaCheck = (JRadioButton)e.getSource();
            int selected = (alphaCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("SortAllCombos", String.valueOf(1));
            }
        } else if (actionCommand.equals("seqSort")) {           
            JRadioButton seqCheck = (JRadioButton)e.getSource();
            int selected = (seqCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("SortAllCombos", String.valueOf(0));
            }
        }
        
        
        
        
        
        
        
     //   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
       
        JSpinner jspiner = (JSpinner)ce.getSource();        
        int sizeValue = (Integer)jspiner.getValue();
        SettingsManager.getInstance().setConfig("splitSize", String.valueOf(sizeValue));
        
       }
    
    
    
    
}
