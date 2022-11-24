/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model.engine;

import java.util.Set;
import gui.accessories.battlesimulator.model.TroopTypeSim;
import javafx.scene.image.Image;

/**
 *
 * @author serguei
 */
public interface INation {
    
    public String getCode();
    public String getName();
    public String getAlignment();
    public Set<TroopTypeSim> getTroopTypes();

    public Image getFlag();
}
