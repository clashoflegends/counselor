/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.IBaseModel;
import business.facade.BattleSimFacade;
import business.facades.WorldFacade;
import control.services.AcaoConverter;
import control.services.ExercitoConverter;
import gui.BattleSimulator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import model.Exercito;
import model.ExercitoSim;
import model.Ordem;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class BattleSimulatorControler implements Serializable, ChangeListener, ListSelectionListener, ActionListener {

    private static final Log log = LogFactory.getLog(BattleSimulatorControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private BattleSimulator tabGui;
    private List<ExercitoSim> listaExibida = new ArrayList<ExercitoSim>();
    private ExercitoSim exercito;
    private BattleSimFacade combSim = new BattleSimFacade();
    private Terreno terreno;
    private int rowIndex = 0;

    public BattleSimulatorControler(BattleSimulator tabGui) {
        this.tabGui = tabGui;
    }

    public String getAjuda(Ordem ordem) {
        return AcaoConverter.getAjuda(ordem);
    }

    public BattleSimulator getTabGui() {
        return tabGui;
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() instanceof JSlider) {
            JSlider source = (JSlider) event.getSource();
            if (!source.getValueIsAdjusting()) {
                if ("jsLoyalty".equals(source.getName()) || "jsSize".equals(source.getName()) || "jsFortification".equals(source.getName())) {
                    tabGui.updateCityLabels();
                } else {
                    log.info(source.getName());
                }
            }
        } else if (event.getSource() instanceof JSpinner) {
            try {
                JSpinner source = (JSpinner) event.getSource();
                if ("jsCommander".equals(source.getName())) {
                    exercito.setComandante((Integer) source.getValue());
                } else if ("jsMorale".equals(source.getName())) {
                    exercito.setMoral((Integer) source.getValue());
                }
                this.getTabGui().setArmyModel(ExercitoConverter.getBattleModel(listaExibida), rowIndex);
            } catch (NullPointerException e) {
                //hex with no army
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        try {
            JTable table = this.getTabGui().getListaExercitos();
            ListSelectionModel lsm = (ListSelectionModel) event.getSource();
            if (!lsm.isSelectionEmpty()) {
                //testes
                rowIndex = lsm.getAnchorSelectionIndex();
                int modelIndex = table.convertRowIndexToModel(rowIndex);
                exercito = (ExercitoSim) listaExibida.get(modelIndex);
                getTabGui().updateArmy(exercito);
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JButton) {
            log.info(labels.getString("OPS.JBUTTON.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            actionOnTabGui(event);
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    private void actionOnTabGui(ActionEvent event) {
        JComboBox jcbTerrain = (JComboBox) event.getSource();
        if ("jcbTerrain".equals(jcbTerrain.getActionCommand())) {
            try {
                final GenericoComboObject obj = (GenericoComboObject) jcbTerrain.getModel().getSelectedItem();
                for (ExercitoSim army : listaExibida) {
                    army.setTerreno((Terreno) obj.getObject());
                }
                this.getTabGui().setArmyModel(ExercitoConverter.getBattleModel(listaExibida), rowIndex);
            } catch (NullPointerException ex) {
            }
        }
    }

    public TableModel getArmyListTableModel(Collection<Exercito> armies) {
        for (Exercito army : armies) {
            listaExibida.add(combSim.clone(army));
        }
        return ExercitoConverter.getBattleModel(listaExibida);
    }

    public GenericoComboBoxModel getTerrenoComboModel() {
        List<IBaseModel> lista = new ArrayList<IBaseModel>();
        for (IBaseModel elem : WorldFacade.getInstance().getCenario().getTerrenos().values()) {
            lista.add(elem);
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new IBaseModel[0]));
        return model;
    }

    public Terreno getTerreno() {
        return terreno;
    }

    public void setTerreno(Terreno terreno) {
        this.terreno = terreno;
    }
}
