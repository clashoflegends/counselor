/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model.engine;

/**
 *
 * @author serguei
 */
public interface IPlatoon {
    
    public int getTroops();
    public ITroopType getTroopType();
    public int getWeapon();
    public int getArmor();
    public int getTrainning();

    /**
     * Set the troops at the end of the combat
     * @param i Number of troops.
     */
    public void setTroopLeft(int i);
}
