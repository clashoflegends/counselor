/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import gui.services.IDispatchReceiver;
import java.awt.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
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
    // Receivers are held via WEAK references (Collections.newSetFromMap(WeakHashMap)) so a GUI tree
    // discarded on a turn-reload - each turn-open rebuilds MainDadosGui, whose Tab*Gui panels each new
    // their own ControlBase controller that registers here - is NOT pinned alive by this singleton and
    // gets GC'd. Long-lived receivers stay strongly reachable elsewhere so their weak entries survive:
    // WorldControler (the MainResultWindowGui.wc field) and MapaControler (cached in WorldFacadeCounselor).
    // This fixes the reload memory leak (many turn-opens in one window -> heap OOM). transient because the
    // backing WeakHashMap is not Serializable; this singleton is never actually serialized (getInstance()).
    private final transient Set<IDispatchReceiver> controlers = Collections.newSetFromMap(new WeakHashMap<>());
    private final transient SortedMap<Integer, Set<IDispatchReceiver>> lista = new TreeMap<>();
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
        lista.computeIfAbsent(msgName, k -> Collections.newSetFromMap(new WeakHashMap<>())).add(controler);
    }

    // Dispatch snapshots the receiver set into an ArrayList first: (1) the copy strong-refs the live
    // receivers for the duration so a mid-dispatch GC can't clear one out from under us; (2) it avoids a
    // ConcurrentModificationException if a receiver (re-)registers during dispatch or a WeakHashMap expunge
    // structurally changes the set. A GC'd receiver simply isn't in the set anymore -> not dispatched.
    public final void sendDispatchForMsg(int msgName) {
        final Set<IDispatchReceiver> set = lista.get(msgName);
        if (set != null) {
            for (IDispatchReceiver cb : new ArrayList<>(set)) {
                cb.receiveDispatch(msgName);
            }
        }
    }

    public final void sendDispatchForMsg(int msgName, Component cmpnt) {
        final Set<IDispatchReceiver> set = lista.get(msgName);
        if (set != null) {
            for (IDispatchReceiver cb : new ArrayList<>(set)) {
                cb.receiveDispatch(msgName, cmpnt);
            }
        }
    }

    public final void sendDispatchForMsg(int msgName, String txt) {
        final Set<IDispatchReceiver> set = lista.get(msgName);
        if (set != null) {
            for (IDispatchReceiver cb : new ArrayList<>(set)) {
                cb.receiveDispatch(msgName, txt);
            }
        }
    }

    public final void sendDispatchForChar(Nacao nation, PersonagemOrdem before, PersonagemOrdem after) {
        for (IDispatchReceiver cb : new ArrayList<>(controlers)) {
            cb.receiveDispatch(nation, before, after);
        }
    }

    public final void sendDispatchForMsg(int msgName, Local local) {
        final Set<IDispatchReceiver> set = lista.get(msgName);
        if (set != null) {
            for (IDispatchReceiver cb : new ArrayList<>(set)) {
                cb.receiveDispatch(msgName, local);
            }
        }
    }

    public final void sendDispatchForMsg(int msgName, Local local, int range) {
        final Set<IDispatchReceiver> set = lista.get(msgName);
        if (set != null) {
            for (IDispatchReceiver cb : new ArrayList<>(set)) {
                cb.receiveDispatch(msgName, local, range);
            }
        }
    }
}
