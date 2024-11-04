/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories.battlesimulator.model.engine;

/**
 *
 * @author serguei
 */
public class BasicCombatEngine implements ICombatEngine {
    
    private final IBattleField battleField;
    
    public BasicCombatEngine(IBattleField battleField) {
        this.battleField = battleField;
    }

    @Override
    public void combat() {
        System.out.println("In the warm climate of the " + battleField.getLandCode() + ", a conflict took place in the early afternoon under a clear sky.");
        battleField.getArmyList().forEach(army -> {
            System.out.println("At the head of a " + army.getArmyMoral() + " moral army rode " + army.getCommanderRank() + " rank " 
                    + army.getCommanderName() + " " + army.getNation().getName() );
            army.getPlatoons().forEach(platoon -> {
                System.out.println("\t- " + platoon.getTroops() + " " + platoon.getTroopType().getName() + "(" + platoon.getTroopType().getCode() +") with " 
                        + platoon.getWeapon() + " weapons and wearing a " + platoon.getArmor() + " armor arrayed in " + platoon.getTrainning() + " formation.");
                                    
                platoon.setTroopLeft(platoon.getTroops()/2);
            });
            System.out.println("Against the enemy forces, we tried to '" + army.getTacticsKey() + "' the enemy");
            System.out.println();
        });
        
    }
    
}
