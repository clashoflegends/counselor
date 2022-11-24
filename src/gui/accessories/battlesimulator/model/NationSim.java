/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import gui.accessories.battlesimulator.model.engine.INation;

/**
 *
 * @author serguei
 */
public class NationSim implements INation {
    
    
    private Set<TroopTypeSim>  troopTypes;
    
    private String name;

    private Image flag;

    public Image getFlag() {
        return flag;
    }

    public void setFlag(Image flag) {
        this.flag = flag;
    }

    public Set<TroopTypeSim> getTroopTypes() {
        return troopTypes;
    }

    public void setTroopTypes(Set<TroopTypeSim> troopTypes) {
        this.troopTypes = troopTypes;
    }
    
    @Override
    public String getCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    

    @Override
    public String getAlignment() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
