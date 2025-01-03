/*
 * MainMapaGui.java
 *
 * Created on April 23, 2008, 11:04 AM
 */
package gui;

import business.ArmyPath;
import business.ImageManager;
import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.services.TagManager;
import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import gui.services.MapaDragListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JViewport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import radialMenu.RadialMenu;

/**
 *
 * @author gurgel
 */
public final class MainMapaGui extends javax.swing.JPanel implements Serializable {

    private static final Log log = LogFactory.getLog(MainMapaGui.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
//    private final ImageIcon tagImage;
    private JLabel jlTag, jlActionsOnMap;
    private int lastDragPointy = 0;
    private int lastDragPointx = 0;
    private final List<JLabel> movTags = new ArrayList();
    private int counter = 0;
    private int dx = 0, dy = 0;
    private RadialMenu radialMenu;

    /**
     * Creates new form MainMapaGui
     */
    public MainMapaGui() {
        initComponents();
        MapaControler mapaControl = WorldFacadeCounselor.getInstance().getMapaControler();
        mapaControl.setTabGui(this);
        doMapa(mapaControl.printMapaGeral());
        this.mapaLabel.addMouseMotionListener(new MapaDragListener(this));
        this.mapaLabel.addMouseListener(mapaControl);
        setTag();
        getJlTag().setVisible(false);

        jLayeredPane1.add(getJlActionsOnMap(), new Integer(20));
        jLayeredPane1.add(getJlTag(), new Integer(100));
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        mapaLabel = new javax.swing.JLabel();

        setAutoscrolls(true);

        jScrollPane1.setBorder(null);

        mapaLabel.setMaximumSize(new java.awt.Dimension(32767, 32767));
        jLayeredPane1.add(mapaLabel);
        mapaLabel.setBounds(0, 0, 400, 400);

        jScrollPane1.setViewportView(jLayeredPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel mapaLabel;
    // End of variables declaration//GEN-END:variables

    public void setLastMouseDragPoint(int x, int y) {
        lastDragPointx = x;
        lastDragPointy = y;
    }

    public void handleMouseDragInBrowserPanel(MouseEvent event) {
        JViewport viewPort = jScrollPane1.getViewport();
        Point scrollPosition = viewPort.getViewPosition();

        int xx = event.getX() - lastDragPointx;
        int yy = event.getY() - lastDragPointy;

        scrollPosition.x -= xx;
        scrollPosition.y -= yy;

        viewPort.setViewPosition(scrollPosition);
    }

    /*
     * Display hex tag on map and bring it into viewport
     */
    public void setFocusTag(int x, int y) {
        final Rectangle tagRectangle = new Rectangle(x - dx, y - dy, ImageManager.HEX_SIZE + 2 * dx, ImageManager.HEX_SIZE + 2 * dy);
        getJlTag().setBounds(tagRectangle);
        getJlTag().setVisible(true);
        final JViewport vp = jScrollPane1.getViewport();
        final Rectangle viewRect = vp.getViewRect();
        final boolean contains = viewRect.contains(tagRectangle);
        if (!contains) {
            vp.setViewPosition(new Point(0, 0));
            vp.scrollRectToVisible(tagRectangle);
        }
    }

    public void hidefocusTag() {
        getJlTag().setVisible(false);
    }

    /*
     * keep track of cumulative movement points.
     */
    public void addMovementTagCumulative(int x, int y, int custoMov, int limitMov) {
        addMovementTag(x, y, custoMov, limitMov, null, true);
    }

    public void addMovementTagNonCumulative(int x, int y, int custoMov, int limitMov) {
        addMovementTag(x, y, custoMov, limitMov, null, false);
    }

    /**
     * plot each value as sent.
     */
    public void addMovementTagNonCumulative(int x, int y, ArmyPath movEx, int limitMov) {
        addMovementTag(x, y, movEx.getCost(), limitMov, movEx.getLastMove(), false);
    }

    private void addMovementTag(int x, int y, int custoMov, int limitMov, MovimentoExercito move, boolean cumulativeCounter) {
        //prepara tag, chamar depois do printMapaGeral, pois ele carrega todas as imagens.
        JLabel tagMovement = new JLabel(this.getMovementTagIcon(custoMov, limitMov, move, cumulativeCounter));
        tagMovement.setOpaque(false);
        final Rectangle rectangle = new Rectangle(x, y, ImageManager.HEX_SIZE, ImageManager.HEX_SIZE);
        tagMovement.setBounds(rectangle);
        tagMovement.setVisible(true);
        movTags.add(tagMovement);
        jLayeredPane1.add(tagMovement, new Integer(99 + counter));
        JViewport vp = jScrollPane1.getViewport();
        Rectangle viewRect = vp.getViewRect();
        boolean contains = viewRect.contains(rectangle);
        if (!contains) {
            vp.setViewPosition(new Point(0, 0));
            vp.scrollRectToVisible(rectangle);
        }
    }

    public void clearMovementTags() {
        for (JLabel tag : movTags) {
            tag.setVisible(false);
            jLayeredPane1.remove(tag);
        }
        movTags.clear();
        this.counter = 0;
    }

    private ImageIcon getMovementTagIcon(int custoMov, int limitMov, MovimentoExercito move, boolean cumulativeCounter) {
        int points;
        if (cumulativeCounter) {
            counter += custoMov;
            points = counter;
        } else {
            counter++;
            points = custoMov;
        }
        BufferedImage img = new BufferedImage(60, 60, BufferedImage.TRANSLUCENT);
        Graphics2D g = img.createGraphics();

        if (move != null) {
            ImageManager.getInstance().doDrawRastro(g, ConverterFactory.getDirecao(move.getDirecao() + 3), Color.BLUE);
        }

        /**
         * Draw a slightly larger black circle first to give the impression of a dark border
         */
//        g.setColor(Color.BLACK);
//        g.fillOval(21, 21, 19, 19);
        ImageManager.getInstance().doDrawCircle(g, 21, 19, Color.BLACK);

        //Draw a circle
        final Composite compositeBefore = g.getComposite();
        final float tagTransparency = SettingsManager.getInstance().getConfigAsInt("MoveTagTransparency");
        if (tagTransparency > 0) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (10f - tagTransparency) / 10));
        }
        if (limitMov >= 9999) {
            //high value. marca como amarelo
            g.setColor(Color.yellow);
        } else if (points > limitMov) {
            g.setColor(Color.red);
        } else {
            g.setColor(Color.cyan);
        }
        g.fillOval(22, 22, 17, 17);
        g.setComposite(compositeBefore);

        // Create white background
        //g.setColor(Color.WHITE);
        //g.fillRect(0, 0, 31, 31);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Verdana", Font.PLAIN, 10));
        if (points < 10) {
            g.drawString(points + "", 28, 35);
        } else if (custoMov > 100 || points > 100) {
            g.drawString("X", 28, 35);
        } else {
            g.drawString(points + "", 24, 35);
        }
        // Create imageIcon from BufferedImage
        final ImageIcon ret = new ImageIcon(img);
        return (ret);
    }

    /**
     * Define jlTag style.
     *
     * @param mapaControl
     */
    public void setTag() {
        final String tagStyle = SettingsManager.getInstance().getConfig("HexTagStyle", "0");
        //prepara tag, chamar depois do printMapaGeral, pois ele carrega todas as imagens.
        switch (tagStyle) {
            case "1":
                setTagLabel(drawTagStyle0());
                break;
            case "2":
                setTagLabel(drawTagStyle2());
                break;
            case "3":
                setTagLabel(drawTagStyle3());
                break;
            default:
                setTagLabel(drawTagStyleDefault());
                break;
        }
        getJlTag().setOpaque(false);
        if (SettingsManager.getInstance().getConfig("HexTagFrame", "0").equals("1")) {
            getJlTag().setBorder(javax.swing.BorderFactory.createEtchedBorder());
        } else {
            getJlTag().setBorder(javax.swing.BorderFactory.createEmptyBorder());
        }
    }

    private ImageIcon drawTagStyle0() {
        return TagManager.getInstance().getTagImage();
    }

    private ImageIcon drawTagStyle2() {
        dx = 4;
        dy = 4;
        return TagManager.getInstance().drawTagStyle2(dx, dy);
    }

    private ImageIcon drawTagStyleDefault() {
        dx = 10;
        dy = 10;
        return TagManager.getInstance().drawTagStyle3(dx, dy);
    }

    private ImageIcon drawTagStyle3() {
        return TagManager.getInstance().drawTagStyle3(dx, dy);
    }

    public void doMapa(ImageIcon mapa) {
        //Gera o mapa
        Dimension tamanho = new Dimension(mapa.getIconWidth(), mapa.getIconHeight());
        this.jLayeredPane1.setPreferredSize(tamanho);
        this.mapaLabel.setSize(tamanho);
        this.mapaLabel.setIcon(mapa);
    }

    public void doActionsOnMap(ImageIcon actionsMap) {
        getJlActionsOnMap().setIcon(actionsMap);
        final Rectangle tagRectangle = new Rectangle(0, 0, actionsMap.getIconWidth(), actionsMap.getIconHeight());
        getJlActionsOnMap().setBounds(tagRectangle);
        getJlActionsOnMap().setVisible(true);
    }

    public void doActionsOnMapHide() {
        getJlActionsOnMap().setVisible(false);
    }

    public void addRadialMenu(RadialMenu aRadialMenu) {
        if (this.radialMenu != null) {
            jLayeredPane1.remove(this.radialMenu);
        }
        jLayeredPane1.add(aRadialMenu, new Integer(400));
    }

    private JLabel getJlTag() {
        return jlTag;
    }

    private void setTagLabel(ImageIcon tagIcon) {
        if (this.jlTag == null) {
            this.jlTag = new JLabel(tagIcon);
        } else {
            this.jlTag.setIcon(tagIcon);
        }
    }

    private JLabel getJlActionsOnMap() {
        if (this.jlActionsOnMap == null) {
            this.jlActionsOnMap = new JLabel();
            this.jlActionsOnMap.setOpaque(false);
            this.jlActionsOnMap.setVisible(false);
        }
        return jlActionsOnMap;
    }

}
