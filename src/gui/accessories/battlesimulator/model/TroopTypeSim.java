/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import gui.accessories.battlesimulator.model.engine.ITroopType;
import java.util.Objects;

/**
 *
 * @author serguei
 */
public class TroopTypeSim implements ITroopType {
    
    private String name;
    private String code;

    public void setCode(String code) {
        this.code = code;
    }

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
        return code;
    }
    
    @Override 
    public boolean equals(Object obj) {
        if (obj instanceof TroopTypeSim) {
            TroopTypeSim newType = (TroopTypeSim)obj;
            return this.getCode().equals(newType.getCode());
             
        }
        
        return false;
    } 

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(getCode());
        return hash;
    }
}
