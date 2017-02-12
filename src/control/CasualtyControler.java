/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.converter.ConverterFactory;
import business.services.ComparatorFactory;
import control.services.CenarioConverter;
import control.services.ExercitoConverter;
import control.services.FiltroConverter;
import control.services.TipoTropaConverter;
import gui.subtabs.SubTabCasualtyGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import model.ExercitoSim;
import model.Local;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class CasualtyControler implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(CasualtyControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private SubTabCasualtyGui tabGui;
    private final Local local;
    private ExercitoSim exercito;

    public CasualtyControler(SubTabCasualtyGui tabGui, Local local) {
        this.tabGui = tabGui;
        this.local = local;
    }

    /**
     * Make sure TabGui is set before usage.
     *
     * @param local
     */
    public CasualtyControler(Local local) {
        this.local = local;
    }

    private SubTabCasualtyGui getTabGui() {
        return tabGui;
    }

    public void setTabGui(SubTabCasualtyGui tabGui) {
        this.tabGui = tabGui;
    }

    public GenericoTableModel getMainTableModel(String filtro, String tactic) {
        return getMainTableModel(filtro, tactic, local.getTerreno());
    }

    public GenericoTableModel getMainTableModel(String filtro, String tactic, Terreno terrain) {
        if (getExercito() == null) {
            return getMainTableModelTipoTropa(filtro, tactic, terrain);
        } else {
            return getMainTableModelPelotao(filtro, tactic, terrain);
        }
    }

    private GenericoTableModel getMainTableModelTipoTropa(String filtro, String tactic, Terreno terrain) {
        final List<TipoTropa> listaExibida;
        if (tactic.equalsIgnoreCase("pa")) {
            listaExibida = new ArrayList<TipoTropa>();
        } else {
            listaExibida = FiltroConverter.listaByFiltroCasualty(filtro);
        }
        //sort array
        ComparatorFactory.getComparatorCasualtiesTipoTropaSorter(listaExibida, ConverterFactory.taticaToInt(tactic), terrain, WorldManager.getInstance().getPartida().getId());
        this.mainTableModel = TipoTropaConverter.getCasualtyModel(listaExibida, terrain);
        return this.mainTableModel;
    }

    private GenericoTableModel getMainTableModelPelotao(String filtro, String tactic, Terreno terrain) {
        final List<Pelotao> listaExibida;
        if (tactic.equalsIgnoreCase("pa")) {
            listaExibida = new ArrayList<Pelotao>();
        } else if (getExercito() == null) {
            listaExibida = new ArrayList<Pelotao>();
        } else {
            listaExibida = FiltroConverter.listaByFiltroCasualty(filtro, exercito.getPelotoes().values());
        }
        //sort array
        ComparatorFactory.getComparatorCasualtiesPelotaoSorter(listaExibida, ConverterFactory.taticaToInt(tactic), terrain, WorldManager.getInstance().getPartida().getId());
        this.mainTableModel = ExercitoConverter.getPelotaoModel(listaExibida, getExercito());
        return this.mainTableModel;
    }

    public ComboBoxModel listFiltro() {
        return new GenericoComboBoxModel(FiltroConverter.listFiltroLW());
    }

    public ComboBoxModel listFiltroTactic() {
        return CenarioConverter.getInstance().getTaticaComboModel();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) event.getSource();
            if ("comboFiltro".equals(cb.getActionCommand())) {
                getTabGui().setMainModel(getMainTableModel(getTabGui().getFiltro(), getTabGui().getFiltroTactic()));
            }
        }
    }

    public ExercitoSim getExercito() {
        return exercito;
    }

    public void updateArmy(ExercitoSim exercito, Terreno terrain) {
        this.setExercito(exercito);
        getTabGui().setMainModel(getMainTableModel(getTabGui().getFiltro(), getTabGui().getFiltroTactic(), terrain));
    }

    public void setExercito(ExercitoSim exercito) {
        this.exercito = exercito;
    }

}
