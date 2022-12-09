/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import gui.accessories.battlesimulator.model.engine.IArmyTactic;

/**
 *
 * @author serguei
 */
public class ArmyTacticSim implements IArmyTactic {
    
    private String name;
    
    private final String code;
    
    public ArmyTacticSim(String code, String name) {
        this.name = name;
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
   
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
