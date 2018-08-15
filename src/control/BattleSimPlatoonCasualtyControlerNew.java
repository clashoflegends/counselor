/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoTableModel;
import business.converter.ConverterFactory;
import business.services.ComparatorFactory;
import control.services.ExercitoConverter;
import control.services.FiltroConverter;
import gui.accessories.BattleCasualtySimulatorNew;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import model.ExercitoSim;
import model.Local;
import model.Pelotao;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class BattleSimPlatoonCasualtyControlerNew implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(BattleSimPlatoonCasualtyControlerNew.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private BattleCasualtySimulatorNew tabGui;
    private Terreno terrain;
    private ExercitoSim exercito;

    public BattleSimPlatoonCasualtyControlerNew(BattleCasualtySimulatorNew tabGui, Local local) {
        setTabGui(tabGui);
        setTerrain(local.getTerreno());
    }

    private Terreno getTerrain() {
        return terrain;
    }

    private void setTerrain(Terreno terrain) {
        this.terrain = terrain;
    }

    private ExercitoSim getExercito() {
        return exercito;
    }

    private void setExercito(ExercitoSim exercito) {
        this.exercito = exercito;
    }

    private BattleCasualtySimulatorNew getTabGui() {
        return tabGui;
    }

    private void setTabGui(BattleCasualtySimulatorNew tabGui) {
        this.tabGui = tabGui;
    }

    public void updateArmy(ExercitoSim exercito, Terreno terrain) {
        setExercito(exercito);
        setTerrain(terrain);
        getTabGui().setPlatoonModel(getMainTableModel(getTabGui().getFiltro(), getTabGui().getFiltroTactic().getComboId(), terrain));
    }

    public GenericoTableModel getMainTableModel(String filtro, String tactic, Terreno terrain) {
        final List<Pelotao> listaExibida;
        if (tactic.equalsIgnoreCase("pa")) {
            listaExibida = new ArrayList<Pelotao>();
        } else if (getExercito() == null) {
            listaExibida = new ArrayList<Pelotao>();
        } else {
            listaExibida = FiltroConverter.listaByFiltroCasualty(filtro, getExercito().getPelotoes().values());
        }
        //sort array
        ComparatorFactory.getComparatorCasualtiesPelotaoSorter(listaExibida, ConverterFactory.taticaToInt(tactic), terrain, WorldManager.getInstance().getPartida().getId());
        this.mainTableModel = ExercitoConverter.getPelotaoModel(listaExibida, getExercito());
        return this.mainTableModel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        //FIXME: move into BattleSimulatorControlerNew
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) event.getSource();
            if ("comboFiltro".equals(cb.getActionCommand())) {
                getTabGui().setPlatoonModel(
                        getMainTableModel(getTabGui().getFiltro(), getTabGui().getFiltroTactic().getComboId(), getTerrain())
                );
            }
        }
    }
}
