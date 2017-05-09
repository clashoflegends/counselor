/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import java.io.Serializable;
import model.Local;
import model.Nacao;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public abstract class ControlBase implements Serializable {

    private static final Log log = LogFactory.getLog(ControlBase.class);

    protected final void registerDispatchManager() {
        getDispatchManager().register(this);
    }

    protected final void registerDispatchManagerForMsg(int msgName) {
        getDispatchManager().registerForMsg(msgName, this);
    }

    /**
     * @return the dispatchManager
     */
    protected final DispatchManager getDispatchManager() {
        return DispatchManager.getInstance();
    }

    public void receiveDispatch(Nacao nation, PersonagemOrdem before, PersonagemOrdem after) {
    }

    public void receiveDispatch(int msgName) {
    }

    public void receiveDispatch(int msgName, String txt) {
    }

    public void receiveDispatch(int msgName, Local local) {
    }

    public void receiveDispatch(int msgName, Local local, int range) {
    }
}
