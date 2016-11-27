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
import java.io.File;
import java.io.Serializable;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import persistenceCommons.SettingsManager;
import persistenceLocal.PathFactory;

/**
 *
 * @author serguei
 */
public class SettingsControler extends ControlBase implements Serializable, ActionListener {
    
    
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
           
        }
        
        
     //   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
