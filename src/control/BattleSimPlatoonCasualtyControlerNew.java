/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import business.BussinessException;
import business.combat.ArmySim;
import business.facade.BattleSimFacade;
import business.services.ComparatorFactory;
import control.services.CenarioConverter;
import control.services.ExercitoConverter;
import control.services.FiltroConverter;
import control.support.DispatchManager;
import gui.accessories.BattleCasualtySimulatorNew;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Local;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class BattleSimPlatoonCasualtyControlerNew implements Serializable, ListSelectionListener, ChangeListener, ActionListener {

    private static final Log log = LogFactory.getLog(BattleSimPlatoonCasualtyControlerNew.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private BattleCasualtySimulatorNew tabGui;
    private ArmySim exercito;
    private Pelotao platoon;
    private List<Pelotao> listaExibida = new ArrayList<Pelotao>();
    private final BattleSimFacade combSim = new BattleSimFacade();
    private int indexOfPlatoon = 0;

    public BattleSimPlatoonCasualtyControlerNew(BattleCasualtySimulatorNew tabGui, Local local) {
        setTabGui(tabGui);
    }

    private ArmySim getExercito() {
        return exercito;
    }

    private void setExercito(ArmySim exercito) {
        this.exercito = exercito;
    }

    private BattleCasualtySimulatorNew getTabGui() {
        return tabGui;
    }

    private void setTabGui(BattleCasualtySimulatorNew tabGui) {
        this.tabGui = tabGui;
    }

    private Pelotao getPlatoon() {
        return platoon;
    }

    private void setPlatoon(Pelotao platoon) {
        this.platoon = platoon;
    }

    public ComboBoxModel listTipoTropas() {
        return CenarioConverter.getInstance().getTropaTipoComboModel();
    }

    public void updateArmy(ArmySim exercito, Terreno terrain) {
        setExercito(exercito);
        getTabGui().setPlatoonModel(getPlatoonTableModel(getTabGui().getFiltroTypes(), terrain), indexOfPlatoon);
    }

    public GenericoTableModel getPlatoonTableModel(String filtro, Terreno terrain) {
        if (getExercito().getTatica() == 2) {
            listaExibida = new ArrayList<Pelotao>();
        } else if (getExercito() == null) {
            listaExibida = new ArrayList<Pelotao>();
        } else {
            listaExibida = FiltroConverter.listaByFiltroCasualty(filtro, getExercito().getPelotoes().values());
        }
        //sort array
//        ComparatorFactory.getComparatorCasualtiesPelotaoSorter(listaExibida, ConverterFactory.taticaToInt(tactic), terrain, WorldManager.getInstance().getPartida().getId());
        ComparatorFactory.getComparatorCasualtiesPelotaoSorter(listaExibida, getExercito().getTatica(), terrain);
        //find new platoon position so that the selected element doesn't change all the time
        if (getPlatoon() != null) {
            indexOfPlatoon = listaExibida.indexOf(getPlatoon());
        } else {
            indexOfPlatoon = Math.min(indexOfPlatoon, listaExibida.size() - 1);
        }
        //create model
        return ExercitoConverter.getPelotaoModel(listaExibida, getExercito());
    }

    private void doRemovePlatoon() {
        if (getPlatoon() == null) {
            return;
        }
        getExercito().getPelotoes().remove(getPlatoon().getCodigo());
        setPlatoon(null);
        this.getTabGui().doRefreshArmy();
    }

    private void doClonePlatoon() {
        if (getPlatoon() == null) {
            return;
        }
        try {
            final Pelotao platoonClone = combSim.clone(getPlatoon());
            changeTipoTropaToAvailable(platoonClone);
            getExercito().getPelotoes().put(platoonClone.getCodigo(), platoonClone);
            setPlatoon(platoonClone);
            this.getTabGui().doRefreshArmy();
        } catch (BussinessException be) {
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.STATUS_BAR_MSG, labels.getString("PLATOON.NEW.CANCELED"));
        }
    }

    private void doNewPlatoon() {
        try {
            final Pelotao platoonNew = new Pelotao();
            changeTipoTropaToAvailable(platoonNew);
            getExercito().getPelotoes().put(platoonNew.getCodigo(), platoonNew);
            setPlatoon(platoonNew);
            this.getTabGui().doRefreshArmy();
        } catch (BussinessException be) {
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.STATUS_BAR_MSG, labels.getString("PLATOON.NEW.CANCELED"));
        }
    }

    protected boolean doNoArmySelectedMsg() {
        if (getExercito() == null) {
            //no army is selected, can't have new
            DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.STATUS_BAR_MSG, labels.getString("PLATOON.NEW.CANCELED"));
            return true;
        }
        return false;
    }

    private void changeTipoTropaToAvailable(Pelotao aPlatoon) throws BussinessException {
        List<TipoTropa> tropaTipo = CenarioConverter.getInstance().getTropaTipo();
        for (Pelotao pelotao : getExercito().getPelotoes().values()) {
            tropaTipo.remove(pelotao.getTipoTropa());
        }
        if (!tropaTipo.isEmpty()) {
            aPlatoon.setTipoTropa(tropaTipo.get(0));
        } else {
            throw new BussinessException();
        }
    }

    protected void doActionButton(ActionEvent event) {
        JButton jbTemp = (JButton) event.getSource();
        //monta csv com as ordens
        if (doNoArmySelectedMsg()) {
            return;
        }
        if ("jbNewPlatoon".equals(jbTemp.getActionCommand())) {
            doNewPlatoon();
        } else if ("jbClonePlatoon".equals(jbTemp.getActionCommand())) {
            doClonePlatoon();
        } else if ("jbRemPlatoon".equals(jbTemp.getActionCommand())) {
            doRemovePlatoon();
        } else {
            log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
        }
    }

    private void actionOnTabGui(JComboBox jcbActive) {
        if ("cbTroopType".equals(jcbActive.getActionCommand())) {
            final GenericoComboObject troop = (GenericoComboObject) jcbActive.getModel().getSelectedItem();
            if (platoon != null) {
                platoon.setTipoTropa((TipoTropa) troop.getObject());
            }
            this.getTabGui().doRefreshArmy();
        } else {
            log.error(String.format("PlatoonActionOnTabGui %s %s", jcbActive.getActionCommand(), jcbActive.getName()));
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        try {
            ListSelectionModel lsm = (ListSelectionModel) event.getSource();
            if (lsm.isSelectionEmpty()) {
                return;
            }
            JTable table = this.getTabGui().getListaPlatoon();
            int indexRow = lsm.getAnchorSelectionIndex();
            int modelIndex = table.convertRowIndexToModel(indexRow);
            setPlatoon((Pelotao) listaExibida.get(modelIndex));
            //set short casualties list
            getTabGui().updatePlatoonPanel(getPlatoon());
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() instanceof JSpinner) {
            try {
                JSpinner source = (JSpinner) event.getSource();
                final Integer value = (Integer) source.getValue();
                if ("jsPlatoonQty".equals(source.getName())) {
                    platoon.setQtd(value);
                } else if ("jsPlatoonTraining".equals(source.getName())) {
                    platoon.setTreino(value);
                } else if ("jsPlatoonWeapon".equals(source.getName())) {
                    platoon.setModAtaque(value);
                } else if ("jsPlatoonArmor".equals(source.getName())) {
                    platoon.setModDefesa(value);
                }
                this.getTabGui().doRefreshArmy();
            } catch (NullPointerException e) {
                //hex with no army
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JButton) {
            doActionButton(event);
        } else if (event.getSource() instanceof JComboBox) {
            actionOnTabGui((JComboBox) event.getSource());
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT") + " " + event.getSource().getClass().toString());
        }
    }
}
