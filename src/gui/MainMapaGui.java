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
    private String hexTagStyle = "0"; // selected HexTagStyle, so the focus tag can be re-drawn at zoom
    private int tagX1x = -1, tagY1x = -1; // 1x coords of the current focus tag, so it can be re-placed when zoom changes
    private static final String ZOOM_KEY = "MapZoom";
    private static final String ZOOM_MIGRATED_KEY = "MapZoomPerScreen"; // one-time per-screen migration flag
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

    /** Zoom is per-screen (KI-004): it's resolved in addNotify(), where getGraphicsConfiguration()
     *  reliably reports the current monitor's LOGICAL, post-OS-scale bounds. Deferred from the
     *  constructor because the GC is not reliable there (Toolkit.getScreenSize() can report native px
     *  on a scaled display -> double-scale), and so a docked/undocked/resized screen each gets its own
     *  remembered zoom on launch. */
    private void initZoom() {
        autoZoomPending = true;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (autoZoomPending) {
            autoZoomPending = false;
            resolveScreenZoom();
            applyMapZoom();
            // The startup auto-select places the focus tag during the EGF load, before zoom is resolved
            // here (so it lands at 1x). Re-place it at the resolved zoom once layout settles.
            javax.swing.SwingUtilities.invokeLater(this::refreshFocusTag);
        }
    }

    /** Re-place the current focus tag at the active zoom (no-op if none is shown). Used after the
     *  startup zoom resolves, since the initial tag was placed before the screen zoom was known. */
    private void refreshFocusTag() {
        if (tagX1x >= 0 && getJlTag().isVisible()) {
            setFocusTag(tagX1x, tagY1x);
        }
    }

    /** Per-screen config key so each monitor/resolution remembers its own zoom (survives docking,
     *  resolution changes, and copied configs). E.g. "MapZoom.2560x1440". */
    private String zoomKeyForScreen() {
        final java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        final java.awt.Rectangle b = (gc != null) ? gc.getBounds()
                : new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        return ZOOM_KEY + "." + b.width + "x" + b.height;
    }

    /** Resolve the zoom for the current screen: its remembered value if present, else this screen's
     *  derived default. A pre-2.882 single MapZoom value is migrated onto the first screen seen, once. */
    private void resolveScreenZoom() {
        final SettingsManager sm = SettingsManager.getInstance();
        final java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        final int width = (gc != null) ? gc.getBounds().width
                : java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        String saved = sm.getConfig(zoomKeyForScreen(), "");
        if (saved.isEmpty() && !sm.isConfig(ZOOM_MIGRATED_KEY, "1", "0")) {
            // one-time carry-over of the old single value onto the first screen; other screens default
            saved = sm.getConfig(ZOOM_KEY, "");
        }
        double z;
        try {
            z = saved.isEmpty() ? clampAutoDefault(width / 1920.0) : clampZoom(Double.parseDouble(saved));
        } catch (NumberFormatException ignore) {
            z = clampAutoDefault(width / 1920.0);
        }
        zoom = z;
        persistZoom(z);
    }

    /** Persist the active zoom under the per-screen key (for restore) AND the legacy ZOOM_KEY (kept as
     *  the "current zoom" mirror that upload telemetry / pMapZoom reads), and mark migration done. */
    private void persistZoom(double z) {
        final SettingsManager sm = SettingsManager.getInstance();
        sm.setConfig(zoomKeyForScreen(), String.valueOf(z));
        sm.setConfig(ZOOM_KEY, String.valueOf(z));
        sm.setConfig(ZOOM_MIGRATED_KEY, "1");
    }

    // Manual/persisted zoom range. The 0.5 floor lets a player on a small screen zoom OUT to fit a
    // whole large map (KI-004); the screen-derived auto-default still never drops below 1.0
    // (clampAutoDefault), so an existing small-screen user's default map is not silently shrunk.
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.5;
    private static final double AUTO_MIN_ZOOM = 1.0;

    private static double clampZoom(double z) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
    }

    /** Clamp for the screen-derived default only: enlarge on hi-res/low-scale panels but never
     *  auto-shrink below 1.0. Manual zoom-out below 1.0 stays available via clampZoom. */
    private static double clampAutoDefault(double z) {
        return Math.max(AUTO_MIN_ZOOM, Math.min(MAX_ZOOM, z));
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
        persistZoom(z);
        clearMovementTags();
        hidefocusTag();
        if (this.radialMenu != null) {
            this.radialMenu.doHide(); // dismiss the bubbles too, else they persist mislaid after rescale
        }
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

    /** Re-draw the focus tag at the given scale: the drawn-hexagon styles render vector-crisp at the
     *  zoom (no pixelation); the legacy raster gif style (HexTagStyle=1) falls back to a smoothed upscale. */
    private ImageIcon buildScaledTag(double scale) {
        switch (hexTagStyle) {
            case "1":
                return scaleGlyph(baseTagIcon); // legacy raster gif - can only be upscaled
            case "2":
                return TagManager.getInstance().drawTagStyle2(dx, dy, scale);
            default: // "3" and the unset/default style both use the drawn style-3 hexagon
                return TagManager.getInstance().drawTagStyle3(dx, dy, scale);
        }
    }

    /** Ctrl+mouse-wheel zooms; Ctrl+0 resets to 100%. A plain wheel still
     *  scrolls - the event is re-dispatched to the scroll pane (a child listener otherwise swallows it). */
    private void installZoomControls() {
        mapaLabel.addMouseWheelListener((java.awt.event.MouseWheelEvent e) -> {
            if (e.isControlDown()) {
                // Use precise rotation: getWheelRotation() is an int that is often 0 on precision
                // touchpads / hi-res wheels (so its sign would wrongly read as zoom-out). The double
                // getPreciseWheelRotation() carries a reliable sign there.
                double rot = e.getPreciseWheelRotation();
                if (rot != 0) {
                    setZoom(zoom * (rot < 0 ? 1.1 : 1.0 / 1.1));
                    showZoomOverlay();
                }
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
                setZoom(1.0); // Ctrl+0 = predictable reset to 100%; first launch still auto-sizes to the screen
                showZoomOverlay();
            }
        });
        // Ctrl + / Ctrl - keyboard zoom (same 1.1 step as the Ctrl+wheel), for laptops/trackpads
        // without an easy Ctrl+wheel. VK_EQUALS covers the unshifted "+/=" key; VK_ADD/VK_SUBTRACT the numpad.
        getActionMap().put("mapZoomIn", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setZoom(zoom * 1.1);
                showZoomOverlay();
            }
        });
        getActionMap().put("mapZoomOut", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setZoom(zoom / 1.1);
                showZoomOverlay();
            }
        });
        final int ctrl = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        final javax.swing.InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        for (int k : new int[]{java.awt.event.KeyEvent.VK_PLUS, java.awt.event.KeyEvent.VK_EQUALS, java.awt.event.KeyEvent.VK_ADD}) {
            im.put(javax.swing.KeyStroke.getKeyStroke(k, ctrl), "mapZoomIn");
        }
        for (int k : new int[]{java.awt.event.KeyEvent.VK_MINUS, java.awt.event.KeyEvent.VK_SUBTRACT}) {
            im.put(javax.swing.KeyStroke.getKeyStroke(k, ctrl), "mapZoomOut");
        }
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
        tagX1x = x; // remember 1x coords so the tag can be re-placed when zoom resolves/changes
        tagY1x = y;
        final Rectangle tagRectangle = new Rectangle(
                (int) Math.round((x - dx) * zoom), (int) Math.round((y - dy) * zoom),
                (int) Math.round((ImageManager.HEX_SIZE + 2 * dx) * zoom),
                (int) Math.round((ImageManager.HEX_SIZE + 2 * dy) * zoom));
        getJlTag().setIcon(buildScaledTag(zoom));
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
        tagX1x = -1; // no tag to re-place on the next zoom change
        tagY1x = -1;
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
        final Rectangle rectangle = new Rectangle((int) Math.round(x * zoom), (int) Math.round(y * zoom),
                (int) Math.round(ImageManager.HEX_SIZE * zoom), (int) Math.round(ImageManager.HEX_SIZE * zoom));
        tagMovement.setBounds(rectangle);
        tagMovement.setVisible(true);
        movTags.add(tagMovement);
        jLayeredPane1.add(tagMovement, Integer.valueOf(99 + counter));
        // Mark just this tag's area dirty (NOT scrollRectToVisible, which previously scrolled +
        // repainted the whole zoomed map once per hex - slow at high zoom and made the view jump).
        // repaint() is coalesced by Swing, so a whole range/sim batch collapses to a single paint.
        jLayeredPane1.repaint(rectangle);
    }

    public void clearMovementTags() {
        boolean had = !movTags.isEmpty();
        for (JLabel tag : movTags) {
            tag.setVisible(false);
            jLayeredPane1.remove(tag);
        }
        movTags.clear();
        this.counter = 0;
        if (had) {
            jLayeredPane1.repaint(); // clear the removed tags (coalesced with any subsequent re-adds)
        }
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
        // Render the bubble at the current map zoom (was a fixed 60px image upscaled -> pixelated).
        // Antialiasing on so the circle/number are smooth even at 100%. The 60-unit geometry below is
        // unchanged - g.scale draws it crisp at any zoom; the font scales with the transform too.
        final int sz = Math.max(1, (int) Math.round(60 * zoom));
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(zoom, zoom);

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
        this.hexTagStyle = tagStyle; // remembered so setFocusTag can re-draw the tag vector-crisp at zoom
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
        // The menu positions its buttons in the SCALED map space (showActiveRadialMenu passes pos*zoom).
        // Size the menu panel to the scaled map so buttons past the original 1x extent aren't clipped,
        // and update each button's mapSize so the edge/border arc detection matches the scaled space.
        // (Bug: at zoom > 100% the bubbles clipped away / mislaid toward the south-east.)
        int w = mapIcon.getIconWidth(), h = mapIcon.getIconHeight();
        aRadialMenu.setBounds(0, 0, w, h);
        aRadialMenu.setZoom(zoom); // scale the bubble ring radius + bubble size to the map zoom
        for (radialMenu.RadialButton rb : aRadialMenu.getRootMenu()) {
            rb.setMapSize(new Point(w, h));
        }
        jLayeredPane1.add(aRadialMenu, Integer.valueOf(400));
        this.radialMenu = aRadialMenu; // keep the handle so zoom (and future dismissals) can hide it
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
