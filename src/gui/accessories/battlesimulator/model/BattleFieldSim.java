/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import gui.accessories.battlesimulator.model.engine.IArmy;
import gui.accessories.battlesimulator.model.engine.IBattleField;

/**
 *
 * @author serguei
 */
public class BattleFieldSim implements IBattleField {
    
  
    private String hexType;
    
    private List<IArmy> armyList = new ArrayList<>();

    public String getHexType() {
        return hexType;
    }

    public void setHexType(String hexType) {
        this.hexType = hexType;
    }

    @Override
    public List<IArmy> getArmyList() {
        return armyList;
    }

    public void setArmyList(List<IArmy> armyList) {
        this.armyList = armyList;
    }
    
    
    public void addArmySim(ArmySim army) throws IOException {
        getArmyList().add(army);
    }
    
    @Override
    public String getLandCode() {
        return this.hexType;
    }
}
