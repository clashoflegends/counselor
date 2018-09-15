package control.support;

import java.awt.Image;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import business.combat.ArmySim;
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

    public void updateArmy(ArmySim exercito);

    public void setCasualtyBorder(ArmySim exercito, Terreno terrain);

    public void setIconImage(Image image);
}
