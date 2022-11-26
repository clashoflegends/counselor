/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import gui.accessories.battlesimulator.model.engine.IPlatoon;
import gui.accessories.battlesimulator.model.engine.ITroopType;

/**
 *
 * @author serguei
 */
public class PlatoonSim implements IPlatoon {
    
    private ObjectProperty<TroopTypeSim> troopType = new SimpleObjectProperty<>();
    private IntegerProperty trainning;
    private IntegerProperty weapon;
    private IntegerProperty armor;
    private SimpleIntegerProperty troops = new SimpleIntegerProperty(0);

    public SimpleIntegerProperty getTroopsProperty() {
        return troops;
    }

    public void setTroops(int troops) {
        this.troops.set(troops);
    }
    private IntegerProperty troopsLeft;

    public IntegerProperty getWeaponProperty() {
        return weapon;
    }

    public void setWeapon(IntegerProperty weapon) {
        this.weapon = weapon;
    }

    public IntegerProperty getArmorProperty() {
        return armor;
    }

    public void setArmor(IntegerProperty armor) {
        this.armor = armor;
    }


  
    public IntegerProperty getTroopsLeft() {
        return troopsLeft;
    }

    public void setTroopsLeft(IntegerProperty troopsLeft) {
        this.troopsLeft = troopsLeft;
    }

    public IntegerProperty getTrainningProperty() {
        return trainning;
    }

    public void setTrainning(IntegerProperty trainning) {
        this.trainning = trainning;
    }
    
    public PlatoonSim() {
        ObjectProperty<Integer> objectProp = new SimpleObjectProperty<>(10);
        trainning = IntegerProperty.integerProperty(objectProp);
        
        objectProp = new SimpleObjectProperty<>(10);
        weapon = IntegerProperty.integerProperty(objectProp);
        
        objectProp = new SimpleObjectProperty<>(0);
        armor = IntegerProperty.integerProperty(objectProp);
             
        objectProp = new SimpleObjectProperty<>(0);
        troopsLeft = IntegerProperty.integerProperty(objectProp);
    }

    public ObjectProperty<TroopTypeSim> getTroopTypeProperty() {
        return troopType;
    }

    public void setTroopType(ObjectProperty<TroopTypeSim> troopType) {
        this.troopType = troopType;
    }
  
    @Override
    public int getTroops() {
        return troops.getValue();
    }

    @Override
    public ITroopType getTroopType() {
        return this.troopType.getValue();
    }

    @Override
    public int getWeapon() {
        return this.weapon.getValue();
    }

    @Override
    public int getArmor() {
        return this.armor.getValue();
    }

    @Override
    public int getTrainning() {
        return this.trainning.intValue();
    }

    @Override
    public void setTroopLeft(int i) {
        getTroopsLeft().set(i);
    }
    
    
    
}
