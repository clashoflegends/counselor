/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.services;

import java.awt.Component;
import model.Local;
import model.Nacao;
import model.PersonagemOrdem;

/**
 *
 * @author jmoura
 */
public interface IDispatchReceiver {

    public void receiveDispatch(int msgName);

    public void receiveDispatch(int msgName, String txt);

    public void receiveDispatch(int msgName, Local local);

    public void receiveDispatch(int msgName, Local local, int range);

    public void receiveDispatch(int msgName, Component cmpnt);

    public void receiveDispatch(Nacao nation, PersonagemOrdem before, PersonagemOrdem after);
}
