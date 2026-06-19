/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu;

import baseLib.BaseModel;
import business.ImageManager;
import control.MapaControler;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class RadialButton extends JButton {

    private static final Log log = LogFactory.getLog(RadialButton.class);
    private MapaControler hierarchyAncestor;
    private Local local;
    private BaseModel baseModel;
    private Point position;
    private double initAngle = 0;
    private double zoom = 1.0; // map zoom; scales this button's submenu ring/size like the root menu
    private final List<RadialButton> subMenuItems = new ArrayList<RadialButton>();
    private boolean subMenu = false;
    private Enum radialAction;
    private Point mapSize;

    public RadialButton(Enum action) {
        this.radialAction = action;
        this.setModel(new DefaultButtonModel());
        this.setFocusPainted(false);
        this.setContentAreaFilled(false);
        this.setBorderPainted(false);
        this.setAlignmentX(LEFT_ALIGNMENT);
        this.setAlignmentY(CENTER_ALIGNMENT);
        this.setHorizontalTextPosition(JButton.CENTER);
        this.setVerticalTextPosition(JButton.CENTER);
        this.setBorder(null);
    }

    public RadialButton(Enum currentAction, Point canvasSize) {
        this(currentAction);
        this.mapSize = canvasSize;
    }

    /** Update the map-extent the button uses for edge/border arc detection. Set to the SCALED map
     *  size when zoomed so the menu's boundary math matches the displayed coordinate space. */
    public void setMapSize(Point mapSize) {
        this.mapSize = mapSize;
    }

    private javax.swing.Icon baseIcon; // native bubble/arrow icon, captured once; scaled per zoom
    private java.awt.Font baseFont;    // native button font, captured once; bolded + scaled per zoom

    /** Scale the bubble icon AND label font to the map zoom so the bubble draws at hex scale and its
     *  text stays proportional. 1.0 = native size (text is still bolded for legibility at any zoom). */
    public void applyZoom(double zoom) {
        this.zoom = zoom; // remembered so this button scales its own submenu (KI-003)
        if (baseIcon == null) {
            baseIcon = getIcon(); // capture the native icon set by the menu manager (blue ball / arrow)
        }
        if (baseIcon instanceof javax.swing.ImageIcon) {
            if (zoom == 1.0) {
                setIcon(baseIcon);
            } else {
                int w = Math.max(1, (int) Math.round(baseIcon.getIconWidth() * zoom));
                int h = Math.max(1, (int) Math.round(baseIcon.getIconHeight() * zoom));
                setIcon(new javax.swing.ImageIcon(
                        ((javax.swing.ImageIcon) baseIcon).getImage().getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH)));
            }
        }
        if (baseFont == null) {
            baseFont = getFont();
        }
        if (baseFont != null) {
            setFont(baseFont.deriveFont(java.awt.Font.BOLD, (float) (baseFont.getSize() * Math.max(1.0, zoom))));
        }
    }

    /**
     * Render the bubble: icon centered, then the label as a dark drop-shadow under white text so it
     * reads against the light-blue gradient ball. super is bypassed (the button has no background or
     * border - contentAreaFilled/borderPainted are off), so this fully controls the bubble's look.
     */
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        try {
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            final int w = getWidth(), h = getHeight();
            final javax.swing.Icon ic = getIcon();
            if (ic != null) {
                ic.paintIcon(this, g2, (w - ic.getIconWidth()) / 2, (h - ic.getIconHeight()) / 2);
            }
            final String txt = getText();
            if (txt != null && !txt.isEmpty()) {
                g2.setFont(getFont());
                final java.awt.FontMetrics fm = g2.getFontMetrics();
                final int tx = (w - fm.stringWidth(txt)) / 2;
                final int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new java.awt.Color(0, 0, 0, 180));
                g2.drawString(txt, tx + 1, ty + 1); // shadow
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(txt, tx, ty);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * @return the hierarchyAncestor
     */
    public MapaControler getHierarchyAncestor() {
        return hierarchyAncestor;
    }

    /**
     * @param hierarchyAncestor the hierarchyAncestor to set
     */
    public void setHierarchyAncestor(MapaControler hierarchyAncestor) {
        this.hierarchyAncestor = hierarchyAncestor;
    }

    /**
     * @return the local
     */
    public Local getLocal() {
        return local;
    }

    /**
     * @param local the local to set
     */
    public void setLocal(Local local) {
        this.local = local;
        for (RadialButton menu : subMenuItems) {
            menu.setLocal(local);
        }
    }

    /**
     * @return the subMenu
     */
    public boolean hasSubMenu() {
        return !this.subMenuItems.isEmpty();
    }

    public void setBaseItem(BaseModel baseModel) {
        this.baseModel = baseModel;
    }

    public void addSubMenuItem(RadialButton subMenu) {
        this.subMenuItems.add(subMenu);
    }

    /**
     * @return the baseModel
     */
    public BaseModel getBaseModel() {
        return baseModel;
    }

    public void updateButtons() {
        //remember that Rads 0 degrees = 3oclock. 12 oclock = 90 degrees. 
        //when clock = 12,3,6,9 then rad = 90, 0, 270, 180

//        log.warn("\n" + getLocal().getCoordenadas() + " - " + position);
//
//        log.warn("x = " + position.x);
//        log.warn("Width = " + this.mapSize.x);
//        log.warn("Width - hexsize = " + (this.mapSize.x - ImageManager.HEX_SIZE));
//        log.warn(position.x >= this.mapSize.x - ImageManager.HEX_SIZE);
//        log.warn(this.mapSize.x - ImageManager.HEX_SIZE - position.x);
//
//        log.warn("y = " + position.y);
//        log.warn("Height = " + this.mapSize.y);
//        log.warn("Height - hexsize = " + (this.mapSize.y - ImageManager.HEX_SIZE));
//        log.warn(position.y >= this.mapSize.y - ImageManager.HEX_SIZE);
//        log.warn(this.mapSize.y - ImageManager.HEX_SIZE - position.y);
        double[] posAngle = calcPosicaoAngle();
        //use max, not min(), for a nice spacing for at least 16 items
        double angularSpacing;
        if (posAngle[0] >= 360) {
            angularSpacing = (double) posAngle[0] / (double) Math.max(16, subMenuItems.size());
        } else if (posAngle[0] >= 180) {
            angularSpacing = (double) posAngle[0] / (double) Math.max(9, subMenuItems.size() - 1);
        } else {
            //less than full circle, then do not space as much
            angularSpacing = (double) posAngle[0] / (double) Math.max(5, subMenuItems.size() - 1);
        }
        double baseAngle = posAngle[1];
        // Sub-bubble ring radius and size scale with the map zoom, like the root menu (KI-003).
        final int hexZ = Math.max(1, (int) Math.round(ImageManager.HEX_SIZE * zoom));
        //set each's menu position clockwise
        for (RadialButton menu : subMenuItems) {
            final double angle = Math.toRadians(baseAngle);
            // Get current angles (in radians)
            double currentXAngle = Math.cos(angle);
            double currentYAngle = Math.sin(angle);
            // Get current offset coordinates (scaled by zoom)
            double currentXCoordinate = ImageManager.HEX_SIZE * zoom * currentXAngle * 2;
            double currentYCoordinate = ImageManager.HEX_SIZE * zoom * currentYAngle * 2;
            // Position buttons around circle
            menu.setBounds(position.x + (int) currentXCoordinate,
                    position.y - (int) currentYCoordinate,
                    hexZ, hexZ);
            menu.setPosition(position);
            menu.setMapSize(this.mapSize); // inherit the (scaled) extent for the sub-button's edge math
            menu.applyZoom(zoom);          // scale the sub-bubble icon + font to match
//            menu.setInitAngle(angle);
            menu.setVisible(true);
            baseAngle -= angularSpacing;
        }
    }

    private double[] calcPosicaoAngle() {
        double startAngle;
        final int totalArc;
        //check for map boundaries (margins scaled to the map zoom so edge arcs trigger at the right distance)
        final int hexZ = Math.max(1, (int) Math.round(ImageManager.HEX_SIZE * zoom));
        if (position.x <= 0 && position.y <= 0) {
            //upper left corner
            totalArc = 90;
            startAngle = 0;
        } else if (position.x >= this.mapSize.x - hexZ * 2 && position.y <= 0) {
            //upper right corner
            totalArc = 90;
            startAngle = 270;
        } else if (position.x <= 0 && position.y >= this.mapSize.y - hexZ * 2) {
            //bottom left corner
            totalArc = 90;
            startAngle = 90;
        } else if (position.x >= this.mapSize.x - hexZ * 2 && position.y >= this.mapSize.y - hexZ * 2) {
            //bottom right corner
            totalArc = 90;
            startAngle = 180;
        } else if (position.y <= 0) {
            //upper border
            totalArc = 180;
            startAngle = 0;
        } else if (position.y >= this.mapSize.y - hexZ * 2) {
            //lower border
            totalArc = 180;
            startAngle = 180;
        } else if (position.x >= this.mapSize.x - hexZ * 2) {
            //right border
            totalArc = 180;
            startAngle = 270;
        } else if (position.x <= hexZ) {
            //left border
            totalArc = 180;
            startAngle = 90;
        } else {
            //not an edge of the map
            totalArc = 360;
            startAngle = getInitAngle();
        }

        //setInitAngle(startAngle);
        final double[] ret = {totalArc, startAngle};
        return ret;
    }

    private double getInitAngle() {
        return this.initAngle;
    }

    /**
     * @return the position
     */
    public Point getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(Point position) {
        this.position = position;
    }

    /**
     * @param initAngle the initAngle to set
     */
    public void setInitAngle(double initAngle) {
        //log.warn("initAngle from " + this.initAngle + " to " + initAngle);
        this.initAngle = initAngle;
    }

    public void closeSubMenu() {
        for (RadialButton menu : subMenuItems) {
            menu.setVisible(false);
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag) {
            for (RadialButton rb : subMenuItems) {
                rb.setVisible(false);
            }
        }
    }

    public boolean isSubMenu() {
        return this.subMenu;
    }

    /**
     * @param subMenu the subMenu to set
     */
    public void setSubMenu(boolean subMenu) {
        this.subMenu = subMenu;
    }

    /**
     * @return the radialAction
     */
    public Enum getRadialAction() {
        return radialAction;
    }

    /**
     * @param radialAction the radialAction to set
     */
    public void setRadialAction(Enum radialAction) {
        this.radialAction = radialAction;
    }

    @Override
    public String toString() {
        return this.radialAction.toString();
    }
}
