/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu;

import java.util.EnumSet;

/**
 *
 * @author jmoura
 */
public enum RadialActionsDirections {

    DIR_NW("NORTEOESTE.ABREVIADO", "no"), DIR_NE("NORTELESTE.ABREVIADO", "ne"), DIR_E("LESTE.ABREVIADO", "l"),
    DIR_SE("SULLESTE.ABREVIADO", "se"), DIR_SW("SULOESTE.ABREVIADO", "so"), DIR_W("OESTE.ABREVIADO", "o");
    public static final EnumSet<RadialActionsDirections> DirectionsMenu = EnumSet.allOf(RadialActionsDirections.class);
    private final String label;
    private final String actionCommand;

    RadialActionsDirections(String label, String actionCommand) {
        this.label = label;
        this.actionCommand = actionCommand;
    }

    public String getLabel() {
        return this.label;
    }

    public String getActionCommand() {
        return this.actionCommand;
    }
}
