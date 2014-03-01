/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import java.io.Serializable;
import model.Local;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public abstract class ControlBase implements Serializable {

    private static final Log log = LogFactory.getLog(ControlBase.class);

    protected void registerDispatchManager() {
        getDispatchManager().register(this);
    }

    protected void registerDispatchManagerForMsg(int msgName) {
        getDispatchManager().registerForMsg(msgName, this);
    }

    /**
     * @return the dispatchManager
     */
    protected DispatchManager getDispatchManager() {
        return DispatchManager.getInstance();
    }

    public void receiveDispatch(PersonagemOrdem antes, PersonagemOrdem depois) {
    }

    public void receiveDispatch(int msgName, String txt) {
    }

    public void receiveDispatch(int msgName, Local local) {
    }

    public void receiveDispatch(int msgName, Local local, int range) {
    }
}
