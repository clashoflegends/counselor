/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import gui.accessories.battlesimulator.model.engine.ITroopType;

/**
 *
 * @author serguei
 */
public class TroopTypeSim implements ITroopType {
    
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getCode() {
        return name;
    }
}
