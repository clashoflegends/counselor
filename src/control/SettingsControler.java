/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import business.converter.ColorFactory;
import control.support.ControlBase;
import control.support.DispatchManager;
import control.support.DisplayPortraitsManager;
import gui.accessories.MainSettingsGui;
import gui.accessories.MainSettingsGui.ComboItem;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.Serializable;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;

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
    private gui.services.Feedback.Progress progressMonitor;
    private final gui.services.SettingsEditSession edits = new gui.services.SettingsEditSession();

    public void cancelEdits() { edits.cancel(); }
    private void setConfig(String key, String value) {
        edits.edit(() -> SettingsManager.getInstance().setConfig(key, value));
    }

    private void setConfigAndSaveToFile(String key, String value) {
        edits.edit(() -> SettingsManager.getInstance().setConfigAndSaveToFile(key, value));
    }
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public SettingsControler(MainSettingsGui jpanel) {
        this.settingsGui = jpanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        applyAction(e);
    }

    private void applyAction(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        switch (actionCommand) {
            case "fSaves": {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    setConfig("saveDir", file.getPath());
                    settingsGui.getSaveDirTextField().setText(file.getPath());
                }
                break;
            }
            case "languageCombo": {
                JComboBox languageCombo = (JComboBox) e.getSource();
                String value = ((ComboItem) languageCombo.getSelectedItem()).getValue();
                setConfig("language", value);
                break;
            }
            case "themeCombo": {
                JComboBox themeCombo = (JComboBox) e.getSource();
                // Saved now; the look-and-feel is installed at startup, so it takes effect on next launch.
                setConfig("LookAndFeelTheme", ((ComboItem) themeCombo.getSelectedItem()).getValue());
                break;
            }
            case "fLoad": {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    setConfig("loadDir", file.getPath());
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
                    setConfig("autoLoad", file.getPath());
                    settingsGui.getAutoLoadTextField().setText(file.getPath());
                }
                break;
            }
            case "setAutoLoadCurrent": {
                // Pin the currently open EGF as the autoload file, skipping the browser. If nothing is open
                // this session, tell the player to open a results file first (never silently pin a stale path).
                File current = settingsGui.getCurrentResultsFile();
                if (current == null) {
                    if (e.getSource() instanceof javax.swing.JComponent control) {
                        control.putClientProperty("JComponent.outline", "error");
                        control.setToolTipText(labels.getString("SETTINGS.GAME.AUTOLOAD.SETCURRENT.NOFILE"));
                    }
                    gui.services.Feedback.report(settingsGui, labels.getString("MENU.CONFIG"),
                            labels.getString("SETTINGS.GAME.AUTOLOAD.SETCURRENT.NOFILE"));
                    break;
                }
                String path = current.getAbsolutePath();
                setConfig("autoLoad", path);
                // Reflect + enable in the UI (setSelected does not fire the checkbox listener, so mirror its work).
                settingsGui.getAutoLoadCheck().setSelected(true);
                settingsGui.getAutoLoadTextField().setEnabled(true);
                settingsGui.getAutoLoadTextField().setText(path);
                settingsGui.getAutoLoadButton().setEnabled(true);
                break;
            }
            case "fLoadAutoAction": {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(PathFactory.getFilterAcoes());
                int returnVal = fc.showOpenDialog(settingsGui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    setConfig("autoLoadActions", file.getPath());
                    settingsGui.getAutoLoadActionTextField().setText(file.getPath());
                }
                break;
            }
            case "overrideAction": {
                JCheckBox overCheck = (JCheckBox) e.getSource();
                int selected = (overCheck.isSelected()) ? 1 : 0;
                setConfig("OverrideElimination", String.valueOf(selected));
                break;
            }
            case "autoSaveOrders": {
                JCheckBox overCheck = (JCheckBox) e.getSource();
                int selected = (overCheck.isSelected()) ? 1 : 0;
                setConfig("AutoSaveActions", String.valueOf(selected));
                break;
            }
            case "saveOrdersNoPrompt": {
                JCheckBox noPromptCheck = (JCheckBox) e.getSource();
                int selected = (noPromptCheck.isSelected()) ? 1 : 0;
                setConfig("SaveOrdersNoPrompt", String.valueOf(selected));
                break;
            }
            case "autoSaveOnMapPick": {
                JCheckBox mapPickCheck = (JCheckBox) e.getSource();
                int selected = (mapPickCheck.isSelected()) ? 1 : 0;
                setConfig("AutoSaveOnMapPick", String.valueOf(selected));
                break;
            }
            case "showPopUp": {
                JCheckBox popUpCheck = (JCheckBox) e.getSource();
                int selected = (popUpCheck.isSelected()) ? 1 : 0;
                setConfig("SendOrderConfirmationPopUp", String.valueOf(selected));
                break;
            }
            case "recieveConfirm": {
                JCheckBox showCheck = (JCheckBox) e.getSource();
                int selected = (showCheck.isSelected()) ? 1 : 0;
                setConfig("SendOrderReceiptRequest", String.valueOf(selected));
                break;
            }
            case "allFilter": {
                JRadioButton allCheck = (JRadioButton) e.getSource();
                int selected = (allCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    setConfig("filtro.default", String.valueOf(0));
                }
                break;
            }
            case "ownFilter": {
                JRadioButton ownCheck = (JRadioButton) e.getSource();
                int selected = (ownCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    setConfig("filtro.default", String.valueOf(1));
                }
                break;
            }
            case "alphaSort": {
                JRadioButton alphaCheck = (JRadioButton) e.getSource();
                int selected = (alphaCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    setConfig("SortAllCombos", String.valueOf(1));
                }
                break;
            }
            case "seqSort": {
                JRadioButton seqCheck = (JRadioButton) e.getSource();
                int selected = (seqCheck.isSelected()) ? 1 : 0;
                if (selected == 1) {
                    setConfig("SortAllCombos", String.valueOf(0));
                }
                break;
            }
            case "maxWindow": {
                JCheckBox maxWindow = (JCheckBox) e.getSource();
                int selected = (maxWindow.isSelected()) ? 1 : 0;
                setConfig("maximizeWindowOnStart", String.valueOf(selected));
                break;
            }
            case "columnAdjust": {
                JCheckBox columnAdjust = (JCheckBox) e.getSource();
                int selected = (columnAdjust.isSelected()) ? 1 : 0;
                setConfig("TableColumnAdjust", String.valueOf(selected));
                break;
            }
            case "colorDifficulty": {
                JCheckBox colorDiff = (JCheckBox) e.getSource();
                int selected = (colorDiff.isSelected()) ? 1 : 0;
                setConfig("ColorDifficulty", String.valueOf(selected));
                break;
            }
            case "copyActions": {
                JCheckBox copyActions = (JCheckBox) e.getSource();
                int selected = (copyActions.isSelected()) ? 1 : 0;
                setConfig("CopyActionsPopUp", String.valueOf(selected));
                break;
            }
            case "copyOrders": {
                JCheckBox copyOrders = (JCheckBox) e.getSource();
                int selected = (copyOrders.isSelected()) ? 1 : 0;
                setConfig("CopyActionsOrder", String.valueOf(selected));
                break;
            }
            case "keepPopUp": {
                JCheckBox keepPopUp = (JCheckBox) e.getSource();
                int selected = (keepPopUp.isSelected()) ? 1 : 0;
                setConfig("KeepPopupOpen", String.valueOf(selected));
                break;
            }
            case "FogOfWar": {
                JCheckBox fogOfWar = (JCheckBox) e.getSource();
                int selected = (fogOfWar.isSelected()) ? 1 : 0;
                setConfig("fogOfWarType", String.valueOf(selected));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            case "LoadActionsOtherNations": {
                JCheckBox loadActionsOtherNations = (JCheckBox) e.getSource();
                int selected = (loadActionsOtherNations.isSelected()) ? 1 : 0;
                if (selected == 0) {
                    setConfig("LoadActionsOtherNations", "deny");
                } else {
                    setConfig("LoadActionsOtherNations", "allow");
                }
                break;
            }
            case "LoadActionsBehavior": {
                JCheckBox loadActionsBehavior = (JCheckBox) e.getSource();
                int selected = (loadActionsBehavior.isSelected()) ? 1 : 0;
                if (selected == 0) {
                    setConfig("LoadActionsBehavior", "clean");
                } else {
                    setConfig("LoadActionsBehavior", "append");
                }
                break;
            }
            case "autoMoveAction": {
                JCheckBox autoMoveAction = (JCheckBox) e.getSource();
                int selected = (autoMoveAction.isSelected()) ? 1 : 0;
                setConfig("AutoMoveNextAction", String.valueOf(selected));
                break;
            }
            case "mapTiles": {
                JComboBox mapTiles = (JComboBox) e.getSource();
                String value = (String) mapTiles.getSelectedItem();
                setConfig("MapTiles", value);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            case "tagStyle": {
                JComboBox tagStyle = (JComboBox) e.getSource();
                String value = (String) tagStyle.getSelectedItem();
                setConfig("HexTagStyle", value);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
                break;
            }
            case "hexTagFrame": {
                JCheckBox hexTagFrame = (JCheckBox) e.getSource();
                int selected = (hexTagFrame.isSelected()) ? 1 : 0;
                setConfig("HexTagFrame", String.valueOf(selected));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_TAG);
                break;
            }
            case "armyPath": {
                JComboBox tagStyle = (JComboBox) e.getSource();
                int index = tagStyle.getSelectedIndex();
                setConfig("ShowArmyMovPath", String.valueOf(index));
                break;
            }
            case "pcPath": {
                JComboBox tagStyle = (JComboBox) e.getSource();
                int index = tagStyle.getSelectedIndex();
                setConfig("drawPcPath", String.valueOf(index));
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
                    setConfig("autoLoad", "");

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
                    setConfig("autoLoadActions", "");

                }
                break;
            case "showPortraits": {
                JCheckBox showPortraits = (JCheckBox) e.getSource();
                int selected = (showPortraits.isSelected()) ? 1 : 0;
                setConfig("ShowCharacterPortraits", String.valueOf(selected));
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL, String.valueOf(selected));
                settingsGui.checkDisplayPortraitCheckBox();
                break;
            }
            case "downloadPortraits":
                DisplayPortraitsManager displayPortraitsManager = DisplayPortraitsManager.getInstance();
                if (displayPortraitsManager.isShowPortraitEnableable()) {
                    // Already have a portraits folder -> re-download the latest pack into it (update/refresh).
                    displayPortraitsManager.downloadLatest(settingsGui);
                } else {
                    // First time -> choose a folder, then download, with the progress bar.
                    progressMonitor = new gui.services.Feedback.Progress(settingsGui, labels.getString("CONFIG.DOWNLOAD.FILE"));
                    progressMonitor.setProgress(0);
                    displayPortraitsManager.downloadPortraits(settingsGui, this);
                }
                settingsGui.checkDisplayPortraitCheckBox();
                break;
            case "autoDownloadPortraits": {
                JCheckBox autoDl = (JCheckBox) e.getSource();
                setConfigAndSaveToFile("autoDownloadPortraits",
                        autoDl.isSelected() ? "true" : "false");
                break;
            }
            case "MountainColorButton":
                Color colorBefore = ColorFactory.getColorBd(SettingsManager.getInstance().getConfig("MapColorCoordinate", "efefef"));
                Color c = JColorChooser.showDialog(this.settingsGui, "Choose", colorBefore);
                if (c == null) {
                    break;
                }
                String colorAfter = ColorFactory.getColorBd(c);
                setConfigAndSaveToFile("MapColorCoordinate", colorAfter);
                JButton source = (JButton) e.getSource();
                source.setBackground(c);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            case "BarbarianColorButton": {
                Color before = ColorFactory.getColorBd(SettingsManager.getInstance().getConfig("ColorBarbarian", "FFFFFF"));
                Color picked = JColorChooser.showDialog(this.settingsGui, "Choose", before);
                if (picked == null) {
                    break;
                }
                setConfigAndSaveToFile("ColorBarbarian", ColorFactory.getColorBd(picked));
                ((JButton) e.getSource()).setBackground(picked);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            case "RangeColorButton": {
                //border drawn round the hexes an order parameter may target (see MapaControler.showRangeOutline).
                //Resolve through MapaControler so the dialog and the map agree on a malformed config value.
                Color picked = JColorChooser.showDialog(this.settingsGui, "Choose",
                        MapaControler.getRangeOutlineColor());
                if (picked == null) {
                    break;
                }
                setConfigAndSaveToFile("ColorHexRange", ColorFactory.getColorBd(picked));
                ((JButton) e.getSource()).setBackground(picked);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            case "UnknownColorButton": {
                Color before = ColorFactory.getColorBd(SettingsManager.getInstance().getConfig("ColorUnknown", "AAAAAA"));
                Color picked = JColorChooser.showDialog(this.settingsGui, "Choose", before);
                if (picked == null) {
                    break;
                }
                setConfigAndSaveToFile("ColorUnknown", ColorFactory.getColorBd(picked));
                ((JButton) e.getSource()).setBackground(picked);
                DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW_RELOAD_TILES);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        applySpinnerChange(ce);
    }

    private void applySpinnerChange(ChangeEvent ce) {
        JSpinner jspiner = (JSpinner) ce.getSource();
        int sizeValue = (Integer) jspiner.getValue();
        setConfig("splitSize", String.valueOf(sizeValue));
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SPLIT_PANE_CHANGED, String.valueOf(sizeValue));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);

            // Its OWN label, with the percentage in it. This read getString("CONFIG.DOWNLOAD.FILE")
            // and passed progress to a string with no specifier, which String.format
            // silently discards - so the note said "Downloading file..." from 0 to 100 and
            // the number never appeared. A second key rather than a specifier on the first,
            // because that one is also used as the monitor's static message above.
            final String message = String.format(
                    labels.getString("CONFIG.DOWNLOAD.FILE.PROGRESS"), progress);
            progressMonitor.setNote(message);

            if (progress == 100) {
                settingsGui.checkDisplayPortraitCheckBox();

            }

        }
    }
}
