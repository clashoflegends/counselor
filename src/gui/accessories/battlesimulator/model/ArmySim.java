/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import gui.accessories.battlesimulator.model.engine.IArmy;
import gui.accessories.battlesimulator.model.engine.INation;
import gui.accessories.battlesimulator.model.engine.IPlatoon;

/**
 *
 * @author serguei
 */
public class ArmySim implements IArmy {
    
    private String commanderName;
    private IntegerProperty moral;
    private IntegerProperty commanderRank;
    private INation nation;
    private final Set<PlatoonSim> platoons = new HashSet<>();
    
    private ObservableList avalaibleTroopTypes;
    private String tacticsKey;

    public Set<PlatoonSim> getPlatoonsSim() {
        return platoons;
    }

    public void addPlatoon(PlatoonSim platoon) {
        this.platoons.add(platoon);
    }

    @Override
    public INation getNation() {
        return nation;
    }

    public void setNation(INation nation) {
        this.nation = nation;
    }

    public IntegerProperty getCommanderRankProperty() {
        return commanderRank;
    }

    public void setCommanderRank(IntegerProperty commanderRank) {
        this.commanderRank = commanderRank;
    }
    
    public ArmySim() {
        
        commanderName = "UNKNOWN";
        ObjectProperty<Integer> objectProp = new SimpleObjectProperty<>(10);
        moral = IntegerProperty.integerProperty(objectProp);
        commanderRank = IntegerProperty.integerProperty(objectProp);
        this.nation = new NationSim();
        
    }
      

    public IntegerProperty getMoral() {
        return moral;
    }

    public void setMoral(IntegerProperty moral) {
        this.moral = moral;
    }
    
    public Image getNationFlag() {
        return this.nation.getFlag();        
    }
    
    @Override
    public String getCommanderName() {
        return commanderName;
    }

    public void setCommanderName(String commanderName) {
        this.commanderName = commanderName;
    }

    @Override
    public int getArmyMoral() {
        return this.moral.getValue();
    }

    @Override
    public int getCommanderRank() {
        return this.commanderRank.getValue();
    }
    
    public ObservableList getAvalaibleTroopTypes() {
        if (avalaibleTroopTypes == null) {
            avalaibleTroopTypes = FXCollections.observableArrayList(getNation().getTroopTypes());
        }
        return avalaibleTroopTypes;
    }

    @Override
    public Set<IPlatoon> getPlatoons() {
        Set<IPlatoon> set = new HashSet<>();        
        platoons.forEach(p -> set.add(p));
        return set;
    }
    
    @Override
    public String getTacticsKey() {
        return tacticsKey;
    }

    public void setTacticsKey(String tacticsKey) {
        this.tacticsKey = tacticsKey;
    }
    
}
