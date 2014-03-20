/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Local;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author gurgel
 */
public class DispatchManager implements Serializable {

    private static final Log log = LogFactory.getLog(DispatchManager.class);
    private static DispatchManager instance;
    private List<ControlBase> controlers = new ArrayList<ControlBase>();
    private SortedMap<Integer, HashSet<ControlBase>> lista = new TreeMap<Integer, HashSet<ControlBase>>();
    //public MSGS
    public static final int SET_LABEL_MONEY = 0;
    public static final int CLEAR_FINANCES_FORECAST = 1;
    public static final int LOCAL_MAP_CLICK = 2;
    public static final int LOCAL_MAP_REDRAW = 3;
    public static final int SAVE_WORLDBUILDER_FILE = 4;
    public static final int LOCAL_CITY_REDRAW = 5;
    public static final int LOCAL_RANGE_CLICK = 6;
    public static final int PACKAGE_RELOAD = 7;
    public static final int ACTIONS_RELOAD = 8;

    private DispatchManager() {
    }

    public synchronized static DispatchManager getInstance() {
        if (DispatchManager.instance == null) {
            DispatchManager.instance = new DispatchManager();
        }
        return DispatchManager.instance;
    }

    public void register(ControlBase controler) {
        controlers.add(controler);
    }

    public void registerForMsg(int msgName, ControlBase controler) {
        if (lista.containsKey(msgName)) {
            lista.get(msgName).add(controler);
        } else {
            HashSet<ControlBase> set = new HashSet<ControlBase>();
            set.add(controler);
            lista.put(msgName, set);
        }
    }

    public void sendDispatchForMsg(int msgName, String txt) {
        if (lista.containsKey(msgName)) {
            for (ControlBase cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, txt);
            }
        }
    }

    public void sendDispatchForChar(PersonagemOrdem antes, PersonagemOrdem depois) {
        for (ControlBase cb : controlers) {
            cb.receiveDispatch(antes, depois);
        }
    }

    public void sendDispatchForMsg(int msgName, Local local) {
        if (lista.containsKey(msgName)) {
            for (ControlBase cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, local);
            }
        }
    }

    public void sendDispatchForMsg(int msgName, Local local, int range) {
        if (lista.containsKey(msgName)) {
            for (ControlBase cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, local, range);
            }
        }
    }
}
