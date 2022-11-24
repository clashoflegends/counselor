/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model;

/**
 *
 * @author serguei
 */
public class Bandols {
    
    private static Bandols bandols;
    
    
    private Bandols() {
        
    }
    
    public static Bandols getInstance() {
        if (bandols == null) {
            bandols = new Bandols();
        }
        return bandols;
    }
    
}
