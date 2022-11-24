/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import control.support.ControlBase;
import control.support.DispatchManager;
import control.support.DisplayPortraitsManager;
import gui.accessories.MainSettingsGui;
import gui.accessories.MainSettingsGui.ComboItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.Serializable;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceLocal.PathFactory;

/**
 *
 * @author serguei
 */
public class SettingsControler extends ControlBase implements Serializable, ActionListener, ChangeListener, PropertyChangeListener {

    private final MainSettingsGui settingsGui;
    private ProgressMonitor progressMonitor;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public SettingsControler(MainSettingsGui jpanel) {
        this.settingsGui = jpanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        switch (actionCommand) {
            case "fSaves": {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    SettingsManager.getInstance().setConfig("saveDir", file.getPath());
                    settingsGui.getSaveDirTextField().setText(file.getPath());
                }
                break;
            }
            case "languageCombo": {
                JComboBox languageCombo = (JComboBox) e.getSource();
                String value = ((ComboItem) languageCombo.getSelectedItem()).getValue();
                SettingsManager.getInstance().setConfig("language", value);
                break;
            }
            case "fLoad": {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    SettingsManager.getInstance().setConfig("loadDir", file.getPath());
                    settingsGui.getLoadDirTextField().setText(file.getPath());
                }
                break;
            }
            case "fLoadAuto": {
                JFileChooser fc = new JFileChooser(SettingsManager.getInstance().getConfig("loadDir"));
                fc.setFileFilter(PathFactory.getFilterResults());
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    SettingsManager.getInstance().setConfig("autoLoad", file.getPath());
                    settingsGui.getAutoLoadTextField().setText(file.getPath());
                }
                break;
            }
            case "fLoadAutoAction": {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(PathFactory.getFilterAcoes());
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    SettingsManager.getInstance().setConfig("autoLoadActions", file.getPath());
                    settingsGui.getAutoLoadActionTextField().setText(file.getPath());
                }
                break;
            }
            case "overrideAction": {
                JCheckBox overCheck = (JCheckBox) e.getSource();
                int selected = (overCheck.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("OverrideElimination", String.valueOf(selected));
                break;
            }
            case "autoSaveOrders": {
                JCheckBox overCheck = (JCheckBox) e.getSource();
                int selected = (overCheck.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("AutoSaveActions", String.valueOf(selected));
                break;
            }
            case "playerEmail":
                String playerEmail = ((JTextField) e.getSource()).getText();
                if (playerEmail != null && !playerEmail.isEmpty()) {
                    SettingsManager.getInstance().setConfig("MyEmail", playerEmail);
                }
                break;
            case "showPopUp": {
                JCheckBox popUpCheck = (JCheckBox) e.getSource();
                int selected = (popUpCheck.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("SendOrderConfirmationPopUp", String.valueOf(selected));
                break;
            }
            case "recieveConfirm": {
                JCheckBox showCheck = (JCheckBox) e.getSource();
                int selected = (showCheck.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("SendOrderReceiptRequest", String.valueOf(selected));
                break;
            }
            case "allFilter": {
                JRadioButton allCheck = (JRadioButton) e.getSource();
                int selected = (allCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    SettingsManager.getInstance().setConfig("filtro.default", String.valueOf(0));
                }
                break;
            }
            case "ownFilter": {
                JRadioButton ownCheck = (JRadioButton) e.getSource();
                int selected = (ownCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    SettingsManager.getInstance().setConfig("filtro.default", String.valueOf(1));
                }
                break;
            }
            case "alphaSort": {
                JRadioButton alphaCheck = (JRadioButton) e.getSource();
                int selected = (alphaCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    SettingsManager.getInstance().setConfig("SortAllCombos", String.valueOf(1));
                }
                break;
            }
            case "seqSort": {
                JRadioButton seqCheck = (JRadioButton) e.getSource();
                int selected = (seqCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    SettingsManager.getInstance().setConfig("SortAllCombos", String.valueOf(0));
                }
                break;
            }
            case "maxWindow": {
                JCheckBox maxWindow = (JCheckBox) e.getSource();
                int selected = (maxWindow.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("maximizeWindowOnStart", String.valueOf(selected));
                break;
            }
            case "columnAdjust": {
                JCheckBox columnAdjust = (JCheckBox) e.getSource();
                int selected = (columnAdjust.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("TableColumnAdjust", String.valueOf(selected));
                break;
            }
            case "copyActions": {
                JCheckBox copyActions = (JCheckBox) e.getSource();
                int selected = (copyActions.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("CopyActionsPopUp", String.valueOf(selected));
                break;
            }
            case "copyOrders": {
                JCheckBox copyOrders = (JCheckBox) e.getSource();
                int selected = (copyOrders.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("CopyActionsOrder", String.valueOf(selected));
                break;
            }
            case "keepPopUp": {
                JCheckBox keepPopUp = (JCheckBox) e.getSource();
                int selected = (keepPopUp.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("KeepPopupOpen", String.valueOf(selected));
                break;
            }
            case "FogOfWar": {
                JCheckBox fogOfWar = (JCheckBox) e.getSource();
                int selected = (fogOfWar.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("fogOfWarType", String.valueOf(selected));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            case "LoadActionsOtherNations": {
                JCheckBox loadActionsOtherNations = (JCheckBox) e.getSource();
                int selected = (loadActionsOtherNations.isSelected()) ? 1 : 0;
                if (selected == 0) {
                    SettingsManager.getInstance().setConfig("LoadActionsOtherNations", "deny");
                } else {
                    SettingsManager.getInstance().setConfig("LoadActionsOtherNations", "allow");
                }
                break;
            }
            case "LoadActionsBehavior": {
                JCheckBox loadActionsBehavior = (JCheckBox) e.getSource();
                int selected = (loadActionsBehavior.isSelected()) ? 1 : 0;
                if (selected == 0) {
                    SettingsManager.getInstance().setConfig("LoadActionsBehavior", "clean");
                } else {
                    SettingsManager.getInstance().setConfig("LoadActionsBehavior", "append");
                }
                break;
            }
            case "autoMoveAction": {
                JCheckBox autoMoveAction = (JCheckBox) e.getSource();
                int selected = (autoMoveAction.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("AutoMoveNextAction", String.valueOf(selected));
                break;
            }
            case "mapTiles": {
                JComboBox mapTiles = (JComboBox) e.getSource();
                String value = (String) mapTiles.getSelectedItem();
                SettingsManager.getInstance().setConfig("MapTiles", value);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            case "tagStyle": {
                JComboBox tagStyle = (JComboBox) e.getSource();
                String value = (String) tagStyle.getSelectedItem();
                SettingsManager.getInstance().setConfig("HexTagStyle", value);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
                break;
            }
            case "hexTagFrame": {
                JCheckBox hexTagFrame = (JCheckBox) e.getSource();
                int selected = (hexTagFrame.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("HexTagFrame", String.valueOf(selected));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
                break;
            }
            case "armyPath": {
                JComboBox tagStyle = (JComboBox) e.getSource();
                int index = tagStyle.getSelectedIndex();
                SettingsManager.getInstance().setConfig("ShowArmyMovPath", String.valueOf(index));
                break;
            }
            case "pcPath": {
                JComboBox tagStyle = (JComboBox) e.getSource();
                int index = tagStyle.getSelectedIndex();
                SettingsManager.getInstance().setConfig("drawPcPath", String.valueOf(index));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
                break;
            }
            case "autoLoadCheck":
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
                break;
            case "autoLoadActionCheck":
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
                break;
            case "showPortraits": {
                JCheckBox showPortraits = (JCheckBox) e.getSource();
                int selected = (showPortraits.isSelected()) ? 1 : 0;
                SettingsManager.getInstance().setConfig("ShowCharacterPortraits", String.valueOf(selected));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL, String.valueOf(selected));
                settingsGui.checkDisplayPortraitCheckBox();
                break;
            }
            case "downloadPortraits":
                DisplayPortraitsManager displayPortraitsManager = DisplayPortraitsManager.getInstance();
                if (!displayPortraitsManager.isShowPortraitEnableable()) {
                    progressMonitor = new ProgressMonitor(settingsGui, labels.getString("CONFIG.DOWNLOAD.FILE"), "", 0, 100);
                    progressMonitor.setProgress(0);
                    displayPortraitsManager.downloadPortraits(settingsGui, this);
                }
                settingsGui.checkDisplayPortraitCheckBox();
                break;
            default:
                break;
        }
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        JSpinner jspiner = (JSpinner) ce.getSource();
        int sizeValue = (Integer) jspiner.getValue();
        SettingsManager.getInstance().setConfig("splitSize", String.valueOf(sizeValue));
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SPLIT_PANE_CHANGED, String.valueOf(sizeValue));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);

            final String message = String.format(labels.getString("CONFIG.DOWNLOAD.FILE"), progress);
            progressMonitor.setNote(message);

            if (progress == 100) {
                settingsGui.checkDisplayPortraitCheckBox();

            }

        }
    }
}
