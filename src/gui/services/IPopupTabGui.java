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

    public void doDetachTogglePopup();

    public void doDetachPopup();

    public void doAttachPopup();

    public String getGuiConfig();
}
