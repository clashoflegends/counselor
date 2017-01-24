package control.support;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import model.ExercitoSim;
import model.Terreno;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author jmoura
 */
public interface IBattleSimulator {

    public void updateCityLabels();

    public void setArmyModel(TableModel model, int selected);

    public JTable getListaExercitos();

    public void updateArmy(ExercitoSim exercito);

    public void setCasualtyBorder(Terreno terrain);
}
