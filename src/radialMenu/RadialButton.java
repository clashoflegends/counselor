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

    private static final Log log = LogFactory.getLog(RadialMenu.class);
    private MapaControler hierarchyAncestor;
    private Local local;
    private BaseModel baseModel;
    private Point position;
    private double initAngle = 0;
    private final List<RadialButton> subMenuItems = new ArrayList<RadialButton>();
    private boolean subMenu = false;
    private Enum radialAction;

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
        double angularSpacing = (double) 360 / (double) Math.max(16, subMenuItems.size());
        double count = 0;
        final double baseAngle = getInitAngle();
        for (RadialButton menu : subMenuItems) {
            final double angle = Math.toRadians(baseAngle + count++ * angularSpacing);
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
            menu.setInitAngle(angle);
            menu.setVisible(true);
        }
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
