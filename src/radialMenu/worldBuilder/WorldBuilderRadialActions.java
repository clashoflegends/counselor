/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu.worldBuilder;

import java.util.EnumSet;

/**
 *
 * @author jmoura
 */
public enum WorldBuilderRadialActions {

    //MENU BUILD
    BUILD_ROADS("MENU.BUILD.ROADS"), BUILD_BRIDGES("MENU.BUILD.BRIDGES"), BUILD_LANDING("MENU.BUILD.LANDING"), BUILD_SHALLOW("MENU.BUILD.SHALLOW"),
    BUILD_RIVER("MENU.BUILD.RIVER"), BUILD_STREAM("MENU.BUILD.STREAM"),
    CHANGE_TERRAIN("MENU.CHANGE.TERRAIN"), CHANGE_CLIMATE("MENU.CHANGE.CLIMATE"),
    CHANGE_CITY_NATION("MENU.CHANGE.CITY.FACTION"),
    MAIN_LOCK("MENU.MAIN.LOCK"), MAIN_CLIMATE("MENU.MAIN.CLIMATE"),
    //MENU MAIN
    MAIN_TERRAIN("MENU.MAIN.TERRAIN"), MAIN_CITY("MENU.MAIN.CITY"),
    MAIN_ROAD("MENU.MAIN.ROAD"), MAIN_RIVER("MENU.MAIN.RIVER"), MAIN_STREAM("MENU.MAIN.STREAM"),
    MAIN_BRIDGE("MENU.MAIN.BRIDGE"), MAIN_LANDING("MENU.MAIN.LANDING"), MAIN_SHALLOW("MENU.MAIN.SHALLOW");
    //internal fields
    private final String label;
    //external Sets
    public static final EnumSet<WorldBuilderRadialActions> WorldBuilderMainMenu = EnumSet.range(MAIN_ROAD, MAIN_SHALLOW);

    WorldBuilderRadialActions(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
