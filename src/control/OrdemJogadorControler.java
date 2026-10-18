package control;

import control.support.ControlBase;
import control.support.DispatchManager;
import gui.services.OrdersPanel;
import javax.swing.SwingUtilities;

/** Refresh after mutations, not from the pre-mutation finance notifications. */
public final class OrdemJogadorControler extends ControlBase {
    private final OrdersPanel panel;
    private boolean queued;

    public OrdemJogadorControler(OrdersPanel panel) {
        this.panel = panel;
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_COUNT);
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_RELOAD);
    }

    @Override public void receiveDispatch(int msg) { queueRefresh(); }
    @Override public void receiveDispatch(int msg, String text) { queueRefresh(); }

    private void queueRefresh() {
        if (queued || !panel.currentWorld()) return;
        queued = true;
        SwingUtilities.invokeLater(() -> {
            queued = false;
            if (panel.currentWorld()) panel.refresh();
        });
    }
}
