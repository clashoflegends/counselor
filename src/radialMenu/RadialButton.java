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
        log.warn("\n" + getLocal().getCoordenadas() + " - " + position);

        log.warn("x = " + position.x);
        log.warn("Width = " + this.mapSize.x);
        log.warn("Width - hexsize = " + (this.mapSize.x - ImageManager.HEX_SIZE));
        log.warn(position.x >= this.mapSize.x - ImageManager.HEX_SIZE);
        log.warn(this.mapSize.x - ImageManager.HEX_SIZE - position.x);

        log.warn("y = " + position.y);
        log.warn("Height = " + this.mapSize.y);
        log.warn("Height - hexsize = " + (this.mapSize.y - ImageManager.HEX_SIZE));
        log.warn(position.y >= this.mapSize.y - ImageManager.HEX_SIZE);
        log.warn(this.mapSize.y - ImageManager.HEX_SIZE - position.y);

        //use max, not min(), for a nice spacing for at least 16 items
        double angularSpacing = (double) 360 / (double) Math.max(16, subMenuItems.size());
        double count = 0;
        double baseAngle = getInitAngle();
        for (RadialButton menu : subMenuItems) {
            final double angle = Math.toRadians(baseAngle);
            // Get current angles (in radians)
            double currentXAngle = Math.cos(angle);
            double currentYAngle = Math.sin(angle);
            // Get current offset coordinates
            double currentXCoordinate = ImageManager.HEX_SIZE * currentXAngle * 2;
            double currentYCoordinate = ImageManager.HEX_SIZE * currentYAngle * 2;
            // Position buttons around circle
            menu.setBounds(position.x + (int) currentXCoordinate,
                    position.y - (int) currentYCoordinate,
                    ImageManager.HEX_SIZE, ImageManager.HEX_SIZE);
            menu.setPosition(position);
//            menu.setInitAngle(angle);
            menu.setVisible(true);
            baseAngle += angularSpacing;
        }
    }

    private double[] calcPosicaoAngle() {
        double startAngle = 0;
        final int totalArc;
        //check for map boundaries
        if (position.y <= 0) {
            //upper border
            totalArc = 180;
            startAngle = 90;
        } else if (position.y >= this.mapSize.y - ImageManager.HEX_SIZE * 2) {
            //lower border
            totalArc = 180;
            startAngle = 270;
        } else if (position.x >= this.mapSize.x - ImageManager.HEX_SIZE * 2) {
            //right border
            totalArc = 360;
            startAngle = 180;
        } else if (position.x <= ImageManager.HEX_SIZE) {
            //left border
            totalArc = 360;
            startAngle = 0;
        } else if (position.x <= 0 && position.y <= 0) {
            //upper left corner
            totalArc = 180;
            startAngle = 0;
        } else if (position.x >= this.mapSize.x - ImageManager.HEX_SIZE * 2 && position.y <= 0) {
            //upper right corner
            totalArc = 120;
            startAngle = 270;
        } else if (position.x <= 0 && position.y >= this.mapSize.y - ImageManager.HEX_SIZE * 2) {
            //bottom left corner
            totalArc = 120;
            startAngle = 90;
        } else if (position.x >= this.mapSize.x - ImageManager.HEX_SIZE * 2 && position.y >= this.mapSize.y - ImageManager.HEX_SIZE * 2) {
            //bottom right corner
            totalArc = 120;
            startAngle = 180;
        } else {
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
        log.warn("set initAngle = " + initAngle);
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
