/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import control.services.ExercitoConverter;
import gui.subtabs.SubTabDirecaoExercito;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import model.Exercito;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class DirecaoExercitoControler implements Serializable, ActionListener {

    private static final Log log = LogFactory.getLog(DirecaoExercitoControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final SubTabDirecaoExercito tabGui;

    public DirecaoExercitoControler(SubTabDirecaoExercito aThis) {
        tabGui = aThis;
    }

    public GenericoComboBoxModel getTropaTipoComboModel(Exercito exercito, boolean water) {
        return ExercitoConverter.getTropaTipoComboModel(exercito, water);
    }

    public GenericoComboBoxModel getTropaTipoComboModel(boolean water) {
        return ExercitoConverter.getTropaTipoComboModelAll(water);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JButton) {
            JButton jbDir = (JButton) actionEvent.getSource();
            if ("apaga".equals(jbDir.getActionCommand())) {
                //apaga todas as direcoes.
                tabGui.doApaga();
            } else {
                //executa o movimento
                tabGui.doMovementTagAdd(jbDir.getActionCommand());
            }
        } else if (actionEvent.getSource() instanceof JRadioButton) {
            JRadioButton jbDir = (JRadioButton) actionEvent.getSource();
            if ("normal".equals(jbDir.getActionCommand())) {
                tabGui.setDirecaoTipo("nr");
            } else {
                tabGui.setDirecaoTipo("ev");
            }
            tabGui.doMovementTagsRepaint();
        } else if (actionEvent.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) actionEvent.getSource();
            if ("tipoTropaChanged".equals(cb.getActionCommand())) {
                //re-calc the movement with new troop costs
                tabGui.doMovementTagsRepaint();
            }
        } else if (actionEvent.getSource() instanceof JCheckBox) {
            JCheckBox jbDir = (JCheckBox) actionEvent.getSource();
            if ("cavalarias".equals(jbDir.getActionCommand())) {
                //cavalarias
                tabGui.doMovementTagsRepaint();
            } else if ("comida".equals(jbDir.getActionCommand())) {
                //sem comida
                tabGui.doMovementTagsRepaint();
            } else {
                //nao devia ocorrer.
                log.error(jbDir.getActionCommand());
            }
        } else {
            log.info(labels.getString("EVENTO.NAO.MAPEADO"));
        }

    }
}
