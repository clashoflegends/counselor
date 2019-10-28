/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

/**
 *
 * @author jmoura
 */
public interface IPopupTabGui {

    public static final String POPUP_FLOATING = "floating";
    public static final String POPUP_DOCKED = "docked";
    public static final String POPUP_HIDDEN = "hidden";

    public void doDetachTogglePopup();

    public void doDetachPopup();

    public void doAttachPopup();

    public String getGuiConfig();

    public String getGuiConfigDetachedStatus();
}
