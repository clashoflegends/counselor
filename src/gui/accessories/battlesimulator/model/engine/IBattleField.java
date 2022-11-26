/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model.engine;

import java.util.List;

/**
 *
 * @author serguei
 */
public interface IBattleField {
    
    public String getLandCode();
    
    public List<IArmy> getArmyList();
    
}
