/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model.engine;

import java.util.Set;

/**
 *
 * @author serguei
 */
public interface IArmy {
    
    public int getArmyMoral();
    public int getCommanderRank();
    public String getCommanderName();
    public INation getNation();
    public Set<IPlatoon> getPlatoons();
    
}
