/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import gui.MainMapaGui;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.SwingUtilities;

/**
 *
 * @author jmoura
 */
public class MapaDragListener implements MouseMotionListener {

    private boolean beingDragged = false;
    private final boolean debugTile = false;
    private final MainMapaGui owner;

    public MapaDragListener(MainMapaGui aThis) {
        this.owner = aThis;
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        /*  Handles mouse dragging for the tile. */
//        if (debugTile) {
//            System.out.println("MouseDragged in tile: (" + event.getX() + "," + event.getY() + ")");
//        }
        if (!beingDragged) {
            beingDragged = true;
            owner.setLastMouseDragPoint(event.getX(), event.getY());
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
            //ignore right click drag...
            owner.handleMouseDragInBrowserPanel(event);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        /*  Handles mouse releases for the tile. */
//        if (debugTile) {
//            System.out.println("Caught a mouseReleased in tile...\n");
//        }
        if (beingDragged) {
            beingDragged = false;
        }
    }
}
