/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import business.ImageManager;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.accessories.MainSettingsGui;
import gui.accessories.MainSettingsGui.ComboItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class SettingsControler extends ControlBase implements Serializable, ActionListener, ChangeListener {

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
            JComboBox languageCombo = (JComboBox) e.getSource();
            String value = ((ComboItem) languageCombo.getSelectedItem()).getValue();
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
            JCheckBox overCheck = (JCheckBox) e.getSource();
            int selected = (overCheck.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("OverrideElimination", String.valueOf(selected));

        } else if (actionCommand.equals("playerEmail")) {

            String playerEmail = ((JTextField) e.getSource()).getText();

            if (playerEmail != null && !playerEmail.isEmpty()) {
                SettingsManager.getInstance().setConfig("MyEmail", playerEmail);
            }
        } else if (actionCommand.equals("showPopUp")) {
            JCheckBox popUpCheck = (JCheckBox) e.getSource();
            int selected = (popUpCheck.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("SendOrderConfirmationPopUp", String.valueOf(selected));

        } else if (actionCommand.equals("recieveConfirm")) {
            JCheckBox showCheck = (JCheckBox) e.getSource();
            int selected = (showCheck.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("SendOrderReceiptRequest", String.valueOf(selected));
        } else if (actionCommand.equals("allFilter")) {
            JRadioButton allCheck = (JRadioButton) e.getSource();
            int selected = (allCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("filtro.default", String.valueOf(0));
            }
        } else if (actionCommand.equals("ownFilter")) {
            JRadioButton ownCheck = (JRadioButton) e.getSource();
            int selected = (ownCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("filtro.default", String.valueOf(1));
            }
        } else if (actionCommand.equals("alphaSort")) {
            JRadioButton alphaCheck = (JRadioButton) e.getSource();
            int selected = (alphaCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("SortAllCombos", String.valueOf(1));
            }
        } else if (actionCommand.equals("seqSort")) {
            JRadioButton seqCheck = (JRadioButton) e.getSource();
            int selected = (seqCheck.isSelected()) ? 1 : 0;
            if (selected == 1) {
                SettingsManager.getInstance().setConfig("SortAllCombos", String.valueOf(0));
            }
        } else if (actionCommand.equals("maxWindow")) {
            JCheckBox maxWindow = (JCheckBox) e.getSource();
            int selected = (maxWindow.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("maximizeWindowOnStart", String.valueOf(selected));
        } else if (actionCommand.equals("columnAdjust")) {
            JCheckBox columnAdjust = (JCheckBox) e.getSource();
            int selected = (columnAdjust.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("TableColumnAdjust", String.valueOf(selected));
        } else if (actionCommand.equals("copyActions")) {
            JCheckBox copyActions = (JCheckBox) e.getSource();
            int selected = (copyActions.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("CopyActionsPopUp", String.valueOf(selected));
        } else if (actionCommand.equals("copyOrders")) {
            JCheckBox copyOrders = (JCheckBox) e.getSource();
            int selected = (copyOrders.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("CopyActionsOrder", String.valueOf(selected));
        } else if (actionCommand.equals("keepPopUp")) {
            JCheckBox keepPopUp = (JCheckBox) e.getSource();
            int selected = (keepPopUp.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("KeepPopupOpen", String.valueOf(selected));
        } else if (actionCommand.equals("FogOfWar")) {
            JCheckBox fogOfWar = (JCheckBox) e.getSource();
            int selected = (fogOfWar.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("FogOfWarType", String.valueOf(selected));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        } else if (actionCommand.equals("LoadActionsOtherNations")) {
            JCheckBox loadActionsOtherNations = (JCheckBox) e.getSource();
            int selected = (loadActionsOtherNations.isSelected()) ? 1 : 0;
            if (selected == 0) {
                SettingsManager.getInstance().setConfig("LoadActionsOtherNations", "deny");
            } else {
                SettingsManager.getInstance().setConfig("LoadActionsOtherNations", "allow");
            }
        } else if (actionCommand.equals("LoadActionsBehavior")) {
            JCheckBox loadActionsBehavior = (JCheckBox) e.getSource();
            int selected = (loadActionsBehavior.isSelected()) ? 1 : 0;
            if (selected == 0) {
                SettingsManager.getInstance().setConfig("LoadActionsBehavior", "clean");
            } else {
                SettingsManager.getInstance().setConfig("LoadActionsBehavior", "append");
            }
        } else if (actionCommand.equals("autoMoveAction")) {
            JCheckBox autoMoveAction = (JCheckBox) e.getSource();
            int selected = (autoMoveAction.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("AutoMoveNextAction", String.valueOf(selected));
        } else if (actionCommand.equals("mapTiles")) {
            JComboBox mapTiles = (JComboBox) e.getSource();
            String value = (String) mapTiles.getSelectedItem();
            SettingsManager.getInstance().setConfig("MapTiles", value);
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        } else if (actionCommand.equals("tagStyle")) {
            JComboBox tagStyle = (JComboBox) e.getSource();
            String value = (String) tagStyle.getSelectedItem();
            SettingsManager.getInstance().setConfig("HexTagStyle", value);
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
        } else if (actionCommand.equals("hexTagFrame")) {
            JCheckBox hexTagFrame = (JCheckBox) e.getSource();
            int selected = (hexTagFrame.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("HexTagFrame", String.valueOf(selected));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
        } else if (actionCommand.equals("armyPath")) {
            JComboBox tagStyle = (JComboBox) e.getSource();
            int index = tagStyle.getSelectedIndex();
            SettingsManager.getInstance().setConfig("ShowArmyMovPath", String.valueOf(index));
        } else if (actionCommand.equals("pcPath")) {
            JCheckBox pcPath = (JCheckBox) e.getSource();
            int selected = (pcPath.isSelected()) ? 1 : 0;
            SettingsManager.getInstance().setConfig("drawPcPath", String.valueOf(selected));
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
        } else if (actionCommand.equals("autoLoadCheck")) {
            JCheckBox autoLoad = (JCheckBox) e.getSource();

            if (autoLoad.isSelected()) {
                this.settingsGui.getAutoLoadTextField().setEnabled(true);
                this.settingsGui.getAutoLoadButton().setEnabled(true);
                this.settingsGui.getAutoLoadTextField().setText(SettingsManager.getInstance().getConfig("autoLoad"));

            } else {
                this.settingsGui.getAutoLoadTextField().setEnabled(false);
                this.settingsGui.getAutoLoadButton().setEnabled(false);
                this.settingsGui.getAutoLoadTextField().setText("");
                SettingsManager.getInstance().setConfig("autoLoad", "");

            }

        } else if (actionCommand.equals("autoLoadActionCheck")) {
            JCheckBox autoLoadActions = (JCheckBox) e.getSource();

            if (autoLoadActions.isSelected()) {
                this.settingsGui.getAutoLoadActionTextField().setEnabled(true);
                this.settingsGui.getAutoLoadActionButton().setEnabled(true);
                this.settingsGui.getAutoLoadActionTextField().setText(SettingsManager.getInstance().getConfig("autoLoadActions"));

            } else {
                this.settingsGui.getAutoLoadActionTextField().setEnabled(false);
                this.settingsGui.getAutoLoadActionButton().setEnabled(false);
                this.settingsGui.getAutoLoadActionTextField().setText("");
                SettingsManager.getInstance().setConfig("autoLoadActions", "");

            }
        } else if (actionCommand.equals("showPortraits")) {
            JCheckBox showPortraits = (JCheckBox) e.getSource();
            int selected = (showPortraits.isSelected()) ? 1 : 0;
            
            SettingsManager.getInstance().setConfig("ShowCharacterPortraits", String.valueOf(selected));
            ImageManager.getInstance().doLoadPortraits();
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL, String.valueOf(selected));
            
            
        }
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        JSpinner jspiner = (JSpinner) ce.getSource();
        int sizeValue = (Integer) jspiner.getValue();
        SettingsManager.getInstance().setConfig("splitSize", String.valueOf(sizeValue));
    }
}
