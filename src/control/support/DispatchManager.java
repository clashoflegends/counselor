/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import gui.services.IDispatchReceiver;
import java.awt.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Local;
import model.Nacao;
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
    private final List<IDispatchReceiver> controlers = new ArrayList<>();
    private final SortedMap<Integer, HashSet<IDispatchReceiver>> lista = new TreeMap<>();
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
    public static final int LOCAL_MAP_REDRAW_RELOAD_TILES = 9;
    public static final int LOCAL_MAP_REDRAW_TAG = 10;
    public static final int ACTIONS_AUTOSAVE = 11;
    public static final int SWITCH_PORTRAIT_PANEL = 12;
    public static final int ACTIONS_MAP_REDRAW = 13;
    public static final int ACTIONS_COUNT = 14;
    public static final int STATUS_BAR_MSG = 15;
    public static final int GUI_STATUS_PERSIST = 16;
    public static final int WINDOWS_CLOSING = 17;
    public static final int WINDOWS_MAXIMIZING = 18;
    public static final int WINDOWS_MINIMIZING = 19;
    public static final int SPLIT_PANE_CHANGED = 20;

    private DispatchManager() {
    }

    public synchronized static DispatchManager getInstance() {
        if (DispatchManager.instance == null) {
            DispatchManager.instance = new DispatchManager();
        }
        return DispatchManager.instance;
    }

    public final void register(IDispatchReceiver controler) {
        controlers.add(controler);
    }

    public final void registerForMsg(int msgName, IDispatchReceiver controler) {
        if (lista.containsKey(msgName)) {
            lista.get(msgName).add(controler);
        } else {
            HashSet<IDispatchReceiver> set = new HashSet<>();
            set.add(controler);
            lista.put(msgName, set);
        }
    }

    public final void sendDispatchForMsg(int msgName) {
        if (lista.containsKey(msgName)) {
            for (IDispatchReceiver cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName);
            }
        }
    }

    public final void sendDispatchForMsg(int msgName, Component cmpnt) {
        if (lista.containsKey(msgName)) {
            for (IDispatchReceiver cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, cmpnt);
            }
        }
    }

    public final void sendDispatchForMsg(int msgName, String txt) {
        if (lista.containsKey(msgName)) {
            for (IDispatchReceiver cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, txt);
            }
        }
    }

    public final void sendDispatchForChar(Nacao nation, PersonagemOrdem before, PersonagemOrdem after) {
        for (IDispatchReceiver cb : controlers) {
            cb.receiveDispatch(nation, before, after);
        }
    }

    public final void sendDispatchForMsg(int msgName, Local local) {
        if (lista.containsKey(msgName)) {
            for (IDispatchReceiver cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, local);
            }
        }
    }

    public final void sendDispatchForMsg(int msgName, Local local, int range) {
        if (lista.containsKey(msgName)) {
            for (IDispatchReceiver cb : lista.get(msgName)) {
                cb.receiveDispatch(msgName, local, range);
            }
        }
    }
}
