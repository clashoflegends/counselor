package gui.services;

import baseLib.BaseModel;
import java.util.List;
import javax.swing.JTable;
import gui.subtabs.SubTabOrdem;

/** Identity-based navigation, unaffected by duplicate actor names or table sorting. */
public final class OrderNavigation {
    private OrderNavigation() { }
    public static boolean select(JTable table, List<?> actors, BaseModel actor, int slot, SubTabOrdem orders) {
        int index = actors.indexOf(actor);
        if (index < 0) return false;
        int view = table.convertRowIndexToView(index);
        if (view < 0) return false;
        table.clearSelection();
        table.setRowSelectionInterval(view, view);
        table.scrollRectToVisible(table.getCellRect(view, 0, true));
        if (actor instanceof model.Personagem pc) orders.doMudaActor(pc);
        else if (actor instanceof model.Cidade city) orders.doMudaActor(city);
        else if (actor instanceof model.Nacao nation) orders.doMudaActor(nation);
        if (!control.services.OrdersList.ownedBy(actor.getNacao(),
                control.facade.WorldFacadeCounselor.getInstance().getJogadorAtivo())) {
            orders.requireReadOnly();
        }
        JTable actions = orders.getOrdemLista();
        if (slot >= 0 && slot < actions.getModel().getRowCount()) {
            int actionView = actions.convertRowIndexToView(slot);
            if (actionView >= 0) {
                actions.clearSelection();
                actions.setRowSelectionInterval(actionView, actionView);
            }
        }
        return true;
    }
}
