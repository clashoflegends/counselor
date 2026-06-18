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

    // --- High-DPI map zoom (Phase 10) ---
    // The shared MapaManager renders the map and reports hex pixel positions at 1x; Counselor scales
    // the map image and overlay placement at the display layer by `zoom`. Default derives from the
    // monitor's logical width (getScreenSize is post-OS-scale: a 4K@200% panel reports 1920 -> 1.0,
    // while 4K@100% reports 3840 -> 2.0 - the "map tiny on a hi-res screen" case). Persisted on change.
    private double zoom = 1.0;
    private boolean autoZoomPending = false; // compute the screen-derived default in addNotify (logical GC)
    private final gui.services.ScaledMapIcon mapIcon = new gui.services.ScaledMapIcon();
    private ImageIcon baseTagIcon; // 1x focus-tag glyph, scaled to `zoom` when placed
    private static final String ZOOM_KEY = "MapZoom";
    private final gui.services.ZoomOverlay zoomOverlay = new gui.services.ZoomOverlay(); // transient "150%" badge

    /**
     * Creates new form MainMapaGui
     */
    public MainMapaGui() {
        initComponents();
        // Render the map on a transparent base so the themed canvas behind it (set below) shows in the
        // jagged margins instead of white. Counselor-only; Judge/Distiler keep the white base.
        business.MapaManager.setMapaBaseColor(new Color(0, 0, 0, 0));
        MapaControler mapaControl = WorldFacadeCounselor.getInstance().getMapaControler();
        mapaControl.setTabGui(this);
        initZoom();
        doMapa(mapaControl.printMapaGeral());
        this.mapaLabel.addMouseMotionListener(new MapaDragListener(this));
        this.mapaLabel.addMouseListener(mapaControl);
        installZoomControls();
        setTag();
        getJlTag().setVisible(false);

        jLayeredPane1.add(getJlActionsOnMap(), Integer.valueOf(20));
        jLayeredPane1.add(getJlTag(), Integer.valueOf(100));
        jLayeredPane1.add(zoomOverlay, Integer.valueOf(500)); // above the radial menu (400)
        zoomOverlay.setVisible(false);

        // Match the Swing theme so the canvas behind/around the map reads as the app background
        // instead of a white slab (before the map paints, or in the margins when it is smaller than
        // the viewport). Counselor-only - the shared map image rendering in PbmCommons is untouched.
        final java.awt.Color panelBg = javax.swing.UIManager.getColor("Panel.background");
        if (panelBg != null) {
            setBackground(panelBg);
            jScrollPane1.getViewport().setBackground(panelBg);
            jLayeredPane1.setOpaque(true);
            jLayeredPane1.setBackground(panelBg);
        }
    }

    // ----- High-DPI map zoom -----

    /** Pick the starting zoom: a persisted user choice if present, else the screen-derived default
     *  (which we persist so player_stats telemetry and the config reflect what is actually shown). */
    private void initZoom() {
        String saved = SettingsManager.getInstance().getConfig(ZOOM_KEY, "");
        if (!saved.isEmpty()) {
            try {
                zoom = clampZoom(Double.parseDouble(saved));
                return;
            } catch (NumberFormatException ignore) {
                // fall through to the deferred default
            }
        }
        // No saved choice: defer the screen-derived default to addNotify(), where the panel is
        // displayable and getGraphicsConfiguration() reliably reports the monitor's LOGICAL width
        // (Toolkit.getScreenSize() at construction can report native px on a scaled display -> double-scale).
        autoZoomPending = true;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (autoZoomPending) {
            autoZoomPending = false;
            java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc != null) {
                zoom = clampZoom(gc.getBounds().width / 1920.0); // logical width, post-OS-scale
                SettingsManager.getInstance().setConfig(ZOOM_KEY, String.valueOf(zoom));
                applyMapZoom();
            }
        }
    }

    /** Screen-derived default for the Ctrl+0 reset (window is shown, so the GC is available). */
    private double computeDefaultZoom() {
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        int width = (gc != null) ? gc.getBounds().width
                : java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        // Logical width is post-OS-scale, so this only enlarges on hi-res / low-scale panels.
        return clampZoom(width / 1920.0);
    }

    private static double clampZoom(double z) {
        return Math.max(1.0, Math.min(2.5, z));
    }

    public double getZoom() {
        return zoom;
    }

    /** Apply a new zoom: rescale the map, drop now-stale overlays, log, persist, relayout. */
    public void setZoom(double newZoom) {
        double z = clampZoom(newZoom);
        if (z == zoom) {
            return;
        }
        log.info(String.format("Map zoom %.2f -> %.2f", zoom, z));
        zoom = z;
        SettingsManager.getInstance().setConfig(ZOOM_KEY, String.valueOf(zoom));
        clearMovementTags();
        hidefocusTag();
        applyMapZoom();
    }

    /** Resize the map view to the current zoom (the JLabel already shares mapIcon). */
    private void applyMapZoom() {
        mapIcon.setZoom(zoom);
        int w = mapIcon.getIconWidth();
        int h = mapIcon.getIconHeight();
        this.jLayeredPane1.setPreferredSize(new Dimension(w, h));
        this.mapaLabel.setBounds(0, 0, w, h);
        this.jLayeredPane1.revalidate();
        this.mapaLabel.repaint();
    }

    /** Scale a small overlay glyph to the current zoom (cheap; glyphs are hex-sized). */
    private ImageIcon scaleGlyph(ImageIcon icon) {
        if (icon == null || zoom == 1.0) {
            return icon;
        }
        int w = Math.max(1, (int) Math.round(icon.getIconWidth() * zoom));
        int h = Math.max(1, (int) Math.round(icon.getIconHeight() * zoom));
        return new ImageIcon(icon.getImage().getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
    }

    /** Ctrl+mouse-wheel zooms; Ctrl+0 resets to the screen-derived default. A plain wheel still
     *  scrolls - the event is re-dispatched to the scroll pane (a child listener otherwise swallows it). */
    private void installZoomControls() {
        mapaLabel.addMouseWheelListener((java.awt.event.MouseWheelEvent e) -> {
            if (e.isControlDown()) {
                setZoom(zoom * (e.getWheelRotation() < 0 ? 1.1 : 1.0 / 1.1));
                showZoomOverlay();
                e.consume();
            } else {
                jScrollPane1.dispatchEvent(new java.awt.event.MouseWheelEvent(
                        jScrollPane1, e.getID(), e.getWhen(), e.getModifiersEx(),
                        e.getX(), e.getY(), e.getClickCount(), e.isPopupTrigger(),
                        e.getScrollType(), e.getScrollAmount(), e.getWheelRotation()));
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_DOWN_MASK), "mapZoomReset");
        getActionMap().put("mapZoomReset", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setZoom(computeDefaultZoom());
                showZoomOverlay();
            }
        });
    }

    /** Flash the transient "150%" badge centered near the top of the visible map and (re)start its
     *  5s hold-then-fade. Positioned in viewport coordinates so it sits over the visible area. */
    private void showZoomOverlay() {
        zoomOverlay.show(Math.round(zoom * 100) + "%");
        Dimension bs = zoomOverlay.badgeSize();
        Rectangle vr = jScrollPane1.getViewport().getViewRect();
        int x = vr.x + Math.max(0, (vr.width - bs.width) / 2);
        int y = vr.y + 16;
        zoomOverlay.setBounds(x, y, bs.width, bs.height);
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
        // x,y are 1x hex pixel coords from MapaManager.doCoordToPosition - scale position + glyph by zoom.
        final Rectangle tagRectangle = new Rectangle(
                (int) Math.round((x - dx) * zoom), (int) Math.round((y - dy) * zoom),
                (int) Math.round((ImageManager.HEX_SIZE + 2 * dx) * zoom),
                (int) Math.round((ImageManager.HEX_SIZE + 2 * dy) * zoom));
        getJlTag().setIcon(scaleGlyph(baseTagIcon));
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
        JLabel tagMovement = new JLabel(scaleGlyph(this.getMovementTagIcon(custoMov, limitMov, move, cumulativeCounter)));
        tagMovement.setOpaque(false);
        final Rectangle rectangle = new Rectangle((int) Math.round(x * zoom), (int) Math.round(y * zoom),
                (int) Math.round(ImageManager.HEX_SIZE * zoom), (int) Math.round(ImageManager.HEX_SIZE * zoom));
        tagMovement.setBounds(rectangle);
        tagMovement.setVisible(true);
        movTags.add(tagMovement);
        jLayeredPane1.add(tagMovement, Integer.valueOf(99 + counter));
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
        //Gera o mapa - rendered at 1x by MapaManager; ScaledMapIcon paints it at the current zoom.
        mapIcon.setBase(mapa.getImage());
        this.mapaLabel.setIcon(mapIcon);
        applyMapZoom();
    }

    public void doActionsOnMap(ImageIcon actionsMap) {
        // Full-map overlay - folded into the scaled map icon so it zooms with the map (and we don't
        // hold a second pre-scaled full-map bitmap). Drawn above the map, below the tag/radial layers.
        mapIcon.setActions(actionsMap.getImage());
        this.mapaLabel.repaint();
    }

    public void doActionsOnMapHide() {
        mapIcon.setActions(null);
        this.mapaLabel.repaint();
    }

    public void addRadialMenu(RadialMenu aRadialMenu) {
        if (this.radialMenu != null) {
            jLayeredPane1.remove(this.radialMenu);
        }
        jLayeredPane1.add(aRadialMenu, Integer.valueOf(400));
    }

    private JLabel getJlTag() {
        return jlTag;
    }

    private void setTagLabel(ImageIcon tagIcon) {
        this.baseTagIcon = tagIcon; // keep the 1x glyph; setFocusTag scales it to the current zoom
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
