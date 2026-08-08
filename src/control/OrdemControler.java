/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import business.facade.OrdemFacade;
import control.services.AcaoConverter;
import control.services.CenarioConverter;
import control.services.LastTurnOrders;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.services.Toast;
import gui.subtabs.SubTabOrdem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.ActorAction;
import model.Ordem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class OrdemControler extends ControlBase implements Serializable, ActionListener, ListSelectionListener, ItemListener {

    private static final Log log = LogFactory.getLog(OrdemControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private int indexModelOrdem;
    private final SubTabOrdem tabGui;
    private final OrdemFacade ordemFacade = new OrdemFacade();

    public OrdemControler(SubTabOrdem tabOrdens) {
        this.tabGui = tabOrdens;
        //registerDispatchManager();
    }

    public String getParametroDisplay(int indexParametro) {
        return getTabGui().getActor().getParametroDisplay(indexModelOrdem, indexParametro);
    }

    public PersonagemOrdem getPersonagemOrdem() {
        return getTabGui().getActor().getPersonagemOrdem(indexModelOrdem);
    }

    private void doRemoveAction() {
        if (getTabGui().isReadOnly()) {
            //segregation: never mutate a non-mine (allied/NPC) actor's orders, whatever the UI state
            return;
        }
        getTabGui().setValueAt(getTabGui().getActor().doOrderClear(indexModelOrdem), indexModelOrdem);
        if (SettingsManager.getInstance().isAutoSaveActions()) {
            getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_AUTOSAVE, this.getTabGui());
        }
        getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
        getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_COUNT);
    }

    private void doSalvaAction() {
        try {
            doSalvaAction(indexModelOrdem);
            final int repeats = ordemFacade.getRequirementsMultiLevel(getTabGui().getOrdemQuadro().getOrdem());
            for (int ii = 0; ii < repeats; ii++) {
                doSalvaAction(getTabGui().getNextActionSlot());
            }
            getTabGui().doFindNextActionSlot();
        } catch (NullPointerException ex) {
            //nao fax nada, ordem nao existe
        }
    }

    /**
     * Save the current order and advance to the next slot, exactly as pressing the save (jbOk) button /
     * ENTER does. Used by the optional "auto-confirm on map hex-pick" feature (AutoSaveOnMapPick) for
     * single-parameter orders. Guarded by isReadOnly (never persists onto a non-mine actor).
     */
    public void doSaveFromMapPick() {
        if (getTabGui().isReadOnly()) {
            return;
        }
        doSalvaAction();
        getTabGui().resetOrdersAllOnSave();
    }

    private void doSalvaAction(int index) {
        doSaveOrder(index, index < 0 ? null : getTabGui().getOrdemQuadro());
    }

    /**
     * Saves an order into a slot: the one chokepoint every order goes through, whether it was composed
     * in the panel or repeated from last turn. Returns false when nothing was saved.
     */
    private boolean doSaveOrder(int index, PersonagemOrdem po) {
        if (index < 0 || po == null) {
            return false;
        }
        if (getTabGui().isReadOnly()) {
            //segregation: never persist orders onto a non-mine (allied/NPC) actor, whatever the UI state
            return false;
        }
        if (hasBlankTarget(po)) {
            // KI-017: don't serialize an order whose required hex/city target is blank (ticking ALL swaps the
            // target picker for an empty 4-digit box; a blank one produced a malformed action that later
            // NPE-crashed the results-EGF render). Tell the player and abort this save.
            Toast.showError(labels.getString("ORDEM.ALVO.AUSENTE"));
            return false;
        }
        getTabGui().getActor().doOrderSave(index, po);
        if (SettingsManager.getInstance().isAutoSaveActions()) {
            getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_AUTOSAVE, this.getTabGui());
        }
        final ActorAction actorAction = ordemFacade.getActorAction(po);
        getTabGui().setValueAt(actorAction, index);
        //draw orders on map
        getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_MAP_REDRAW);
        getDispatchManager().sendDispatchForMsg(DispatchManager.ACTIONS_COUNT);
        return true;
    }

    /**
     * "Do it again": refills this actor's EMPTY order slots with the orders it ran last turn, read from
     * the turn file itself (see {@link LastTurnOrders}). Orders the actor can no longer choose are
     * skipped, existing entries are never touched, and the player reviews every repeated order in the
     * panel before submitting - the parameters are last turn's, so a target that has since died or
     * moved is theirs to correct.
     */
    private void doDoAgainAction() {
        if (getTabGui().isReadOnly()) {
            return;
        }
        final java.util.Collection<PersonagemOrdem> executed = getTabGui().getActor().getOrdensExecutadas();
        if (!LastTurnOrders.hasAny(executed)) {
            Toast.show(labels.getString("DOAGAIN.NONE"), null);
            return;
        }
        int slot = getTabGui().getNextActionSlot();
        if (slot < 0) {
            Toast.show(labels.getString("DOAGAIN.NOSLOT"), null);
            return;
        }
        // Availability is per slot, and follows the ALL checkbox exactly as the order combo does.
        final List<Integer> availableNow = LastTurnOrders.availableNumbers(getTabGui().getOrdemComboModel(slot));
        final List<PersonagemOrdem> repeatable =
                LastTurnOrders.listRepeatable(executed, getTabGui().getActor().getNome(), availableNow);
        final int unavailable = LastTurnOrders.countUnavailable(executed, availableNow);

        int saved = 0;
        for (PersonagemOrdem po : repeatable) {
            if (slot < 0) {
                break; //out of empty slots: what is already in the turn stays as the player left it
            }
            if (doSaveOrder(slot, po)) {
                saved++;
            }
            slot = getTabGui().getNextActionSlot();
        }
        final int notPlaced = repeatable.size() - saved;
        if (saved == 0 && notPlaced == 0 && unavailable > 0) {
            Toast.show(String.format(labels.getString("DOAGAIN.NONEVALID"), unavailable), null);
        } else if (unavailable + notPlaced > 0) {
            Toast.show(String.format(labels.getString("DOAGAIN.DONE.PARTIAL"), saved, unavailable + notPlaced), null);
        } else {
            Toast.show(String.format(labels.getString("DOAGAIN.DONE"), saved), null);
        }
        getTabGui().doFindNextActionSlot();
    }

    /** True when the actor has orders from last turn to repeat (drives the button's enabled state). */
    public boolean hasLastTurnOrders() {
        return LastTurnOrders.hasAny(getTabGui().getActor().getOrdensExecutadas());
    }

    /**
     * KI-017: true when a required city/hex TARGET parameter is blank. Scoped to the Cidade_ and Coordenada_
     * token families (the transfer/transport orders where ticking ALL replaces the never-empty target picker
     * with an empty box). Only checks when the collected param ids are index-aligned with the order's params
     * (no param was hidden), so it never false-rejects an order that legitimately omits a parameter. Treats
     * both an empty string and a single space (the null-combo sentinel from ComponentFactory) as blank.
     */
    private boolean hasBlankTarget(PersonagemOrdem po) {
        if (po == null || po.getOrdem() == null || po.getParametrosId() == null) {
            return false;
        }
        final Ordem ord = po.getOrdem();
        final List<String> ids = po.getParametrosId();
        if (ids.size() != ord.getParametrosIdeQtd()) {
            return false; // a param was hidden -> indices don't align; don't risk a false reject
        }
        for (int nn = 0; nn < ids.size(); nn++) {
            final String controle = ord.getParametroIde(nn);
            if (controle != null && (controle.startsWith("Cidade") || controle.startsWith("Coordenada"))) {
                final String id = ids.get(nn);
                if (id == null || id.trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void doRepeatAction() {
        try {
            doSalvaAction(indexModelOrdem);
            //find open slot
            int nextActionSlot = getTabGui().getNextActionSlot();
            while (nextActionSlot >= 0) {
                doSalvaAction(nextActionSlot);
                //update open slot
                nextActionSlot = getTabGui().getNextActionSlot();
            }
        } catch (NullPointerException ex) {
            //nao fax nada, ordem nao existe
        }
    }

    public ComboBoxModel getTaticasComboModel() {
        return CenarioConverter.getInstance().getTaticaComboModel();
    }

    public ComboBoxModel getTerrainComboModel() {
        return CenarioConverter.getInstance().getTerrainComboModel();
    }

    private SubTabOrdem getTabGui() {
        return this.tabGui;
    }

    public String getOrdemAjuda(Ordem ordemSelecionada) {
        return AcaoConverter.getAjuda(ordemSelecionada);
    }

    /**
     * Alguem acionou o botao para gravar ordens(JButton)
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (actionEvent.getSource() instanceof JButton) {
            JButton button = (JButton) actionEvent.getSource();
            if ("jbOk".equals(button.getActionCommand())) {
                doSalvaAction();
                getTabGui().resetOrdersAllOnSave();
            } else if ("jbRepeat".equals(button.getActionCommand())) {
                doRepeatAction();
            } else if ("jbDoagain".equals(button.getActionCommand())) {
                doDoAgainAction();
            } else if ("jbClear".equals(button.getActionCommand())) {
                doRemoveAction();
            } else if ("jbHelp".equals(button.getActionCommand())) {
                //exibir ajuda.
                getTabGui().doDisplayAjuda();
            }
        } else if (actionEvent.getSource() instanceof JComboBox) {
            JComboBox jcMagia = (JComboBox) actionEvent.getSource();
            if ("jcMagia".equals(jcMagia.getActionCommand())) {
                try {
                    getTabGui().setOrdemParametrosMagia((GenericoComboObject) jcMagia.getModel().getSelectedItem());
                } catch (NullPointerException ex) {
                    //nao faz nada, ordens nao disponiveis...
                }
            }
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    /**
     * Listener para a troca na combo de ordem...
     *
     * @param event
     */
    @Override
    public void itemStateChanged(ItemEvent event) {
        //Object source = event.getItemSelectable();
        if (event.getSource() instanceof JCheckBox) {
            final JCheckBox cb = (JCheckBox) event.getSource();
            //Now that we know which button was pushed, find out
            //whether it was selected or deselected.
            if ("cbOrdersAll".equals(cb.getActionCommand())) {
                //nao faz diferenca se esta selecionado ou nao.
                //&& event.getStateChange() == ItemEvent.DESELECTED
                // eh apenas o refresh da combo.
                getTabGui().doMudaOrdem(this.indexModelOrdem);
            }
        }
        if (event.getSource() instanceof JComboBox && event.getStateChange() == ItemEvent.SELECTED) {
            final JComboBox cb = (JComboBox) event.getSource();
            try {
                if ("cbOrdem".equals(cb.getActionCommand())) {
                    getTabGui().setOrdemParametrosQuadro((GenericoComboObject) cb.getModel().getSelectedItem());
                }
            } catch (ClassCastException ex) {
                log.debug("hum... suspicious");
            }
        }
    }

    /**
     * listener para a tabela de ordens.
     *
     * @param event
     */
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel lsm = (ListSelectionModel) event.getSource();
        if (!lsm.isSelectionEmpty()) {
            JTable table = this.getTabGui().getOrdemLista();
            //pega o index da table
            int rowIndex = lsm.getAnchorSelectionIndex();
            //convete o index da table para o index do model
            this.indexModelOrdem = table.convertRowIndexToModel(rowIndex);
            getTabGui().doMudaOrdem(this.indexModelOrdem);
            //segue daki
            //agora pega a ordem e carrega o quadro de ordens.
        }
    }
}
