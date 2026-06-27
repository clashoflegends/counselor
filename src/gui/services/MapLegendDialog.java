package gui.services;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;

/**
 * Map legend: the key map symbols (terrain, settlements + sizes, fortifications, sites, routes/water
 * features, units/markers) each shown with its actual map icon and a brief explanation. Icons are
 * loaded straight from the classpath (/images/...), the same assets MapaManager draws, so the legend
 * always matches the rendered map. Opened from the main toolbar's Legend button.
 */
public final class MapLegendDialog extends JDialog {

    private static final Log log = LogFactory.getLog(MapLegendDialog.class);
    private static final int ICON_MAX = 32; // uniform icon box; hex tiles are scaled down to this

    private final BundleManager labels;

    // {category-key, icon-resource (or "" for none), label-key, fallback-label, desc-key, fallback-desc}
    // Categories and rows are deliberately generic (variant-independent); army icons are per-nation so
    // they are summarised in one row rather than enumerated.
    private static final String[][] ROWS = {
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_mar.png", "MAPLEGEND.SEA", "Sea", "MAPLEGEND.SEA.D", "Deep water - only ships cross"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_costa.png", "MAPLEGEND.COAST", "Coast", "MAPLEGEND.COAST.D", "Coastal water"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_litoral.png", "MAPLEGEND.LITTORAL", "Littoral", "MAPLEGEND.LITTORAL.D", "Shallows along the shore"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_planicie.png", "MAPLEGEND.PLAINS", "Plains", "MAPLEGEND.PLAINS.D", "Open land, easy movement"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_floresta.png", "MAPLEGEND.FOREST", "Forest", "MAPLEGEND.FOREST.D", "Wooded terrain"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_colinas.png", "MAPLEGEND.HILLS", "Hills", "MAPLEGEND.HILLS.D", "Rough, elevated terrain"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_montanha.png", "MAPLEGEND.MOUNTAINS", "Mountains", "MAPLEGEND.MOUNTAINS.D", "Hard to cross"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_pantano.png", "MAPLEGEND.SWAMP", "Swamp", "MAPLEGEND.SWAMP.D", "Marshland, slow going"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_deserto.png", "MAPLEGEND.DESERT", "Desert", "MAPLEGEND.DESERT.D", "Arid wasteland"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_lago.png", "MAPLEGEND.LAKE", "Lake", "MAPLEGEND.LAKE.D", "Inland water"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/hex_wasteland.png", "MAPLEGEND.WASTELAND", "Wasteland", "MAPLEGEND.WASTELAND.D", "Barren, ruined land"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_2b_vazio.png", "MAPLEGEND.UNEXPLORED", "Unexplored", "MAPLEGEND.UNEXPLORED.D", "Not yet scouted"},
        {"MAPLEGEND.CAT.TERRAIN", "/images/mapa/hex_fogofwar.png", "MAPLEGEND.FOG", "Fog of war", "MAPLEGEND.FOG.D", "Last-known view, may be stale"},

        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_acampamento.gif", "MAPLEGEND.CAMP", "Camp", "MAPLEGEND.CAMP.D", "Smallest settlement (size 1)"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_aldeia.gif", "MAPLEGEND.VILLAGE", "Village", "MAPLEGEND.VILLAGE.D", "Size 2"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_vila.gif", "MAPLEGEND.TOWN", "Town", "MAPLEGEND.TOWN.D", "Size 3"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_burgo.gif", "MAPLEGEND.BURG", "Burg", "MAPLEGEND.BURG.D", "Size 4"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_cidade.gif", "MAPLEGEND.CITY", "City", "MAPLEGEND.CITY.D", "Largest settlement (size 5)"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_capital.gif", "MAPLEGEND.CAPITAL", "Capital", "MAPLEGEND.CAPITAL.D", "A nation's seat of power"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_porto.gif", "MAPLEGEND.PORT", "Port", "MAPLEGEND.PORT.D", "Coastal city - harbors and builds ships"},
        {"MAPLEGEND.CAT.SETTLE", "/images/mapa/cp_docas.gif", "MAPLEGEND.DOCKS", "Docks", "MAPLEGEND.DOCKS.D", "Naval facility"},

        {"MAPLEGEND.CAT.FORT", "/images/mapa/cp_torre.gif", "MAPLEGEND.TOWER", "Tower", "MAPLEGEND.TOWER.D", "Light fortification"},
        {"MAPLEGEND.CAT.FORT", "/images/mapa/cp_forte.gif", "MAPLEGEND.FORT", "Fort", "MAPLEGEND.FORT.D", ""},
        {"MAPLEGEND.CAT.FORT", "/images/mapa/cp_castelo.gif", "MAPLEGEND.CASTLE", "Castle", "MAPLEGEND.CASTLE.D", ""},
        {"MAPLEGEND.CAT.FORT", "/images/mapa/cp_fortaleza.gif", "MAPLEGEND.FORTRESS", "Fortress", "MAPLEGEND.FORTRESS.D", ""},
        {"MAPLEGEND.CAT.FORT", "/images/mapa/cp_cidadela.gif", "MAPLEGEND.CITADEL", "Citadel", "MAPLEGEND.CITADEL.D", "Strongest fortification"},

        {"MAPLEGEND.CAT.ROUTE", "/images/mapa/hex_road_l.gif", "MAPLEGEND.ROAD", "Road", "MAPLEGEND.ROAD.D", "Speeds movement"},
        {"MAPLEGEND.CAT.ROUTE", "/images/mapa/hex_rio_l.gif", "MAPLEGEND.RIVER", "River", "MAPLEGEND.RIVER.D", "Hex-edge water - limits crossing"},
        {"MAPLEGEND.CAT.ROUTE", "/images/mapa/hex_riacho_l.gif", "MAPLEGEND.STREAM", "Stream", "MAPLEGEND.STREAM.D", "Minor watercourse"},
        {"MAPLEGEND.CAT.ROUTE", "/images/mapa/hex_ponte_l.gif", "MAPLEGEND.BRIDGE", "Bridge", "MAPLEGEND.BRIDGE.D", "Crossing over a river"},
        {"MAPLEGEND.CAT.ROUTE", "/images/mapa/hex_vau_l.gif", "MAPLEGEND.FORD", "Ford", "MAPLEGEND.FORD.D", "Shallow river crossing"},
        {"MAPLEGEND.CAT.ROUTE", "/images/mapa/hex_water_E.gif", "MAPLEGEND.LANDING", "Landing", "MAPLEGEND.LANDING.D", "Where ships can land troops"},

        {"MAPLEGEND.CAT.UNITS", "/images/armies/neutral.png", "MAPLEGEND.ARMY", "Army", "MAPLEGEND.ARMY.D", "Military unit - tinted by its nation's color"},
        {"MAPLEGEND.CAT.UNITS", "/images/mapa/hex_navio.gif", "MAPLEGEND.FLEET", "Ship / Fleet", "MAPLEGEND.FLEET.D", "Naval unit"},
        {"MAPLEGEND.CAT.UNITS", "/images/mapa/hex_personagem.gif", "MAPLEGEND.CHAR", "Character", "MAPLEGEND.CHAR.D", "A character on the map"},
        {"MAPLEGEND.CAT.UNITS", "/images/mapa/hex_npc.gif", "MAPLEGEND.NPC", "NPC", "MAPLEGEND.NPC.D", "Non-player character"},
        {"MAPLEGEND.CAT.UNITS", "/images/mapa/hex_artefato.gif", "MAPLEGEND.ITEM", "Magic item", "MAPLEGEND.ITEM.D", "An artifact lying on the map"},
        {"MAPLEGEND.CAT.UNITS", "/images/mapa/hex_goldmine.gif", "MAPLEGEND.GOLDMINE", "Gold mine", "MAPLEGEND.GOLDMINE.D", "A resource site"},
        {"MAPLEGEND.CAT.UNITS", "/images/combat.png", "MAPLEGEND.COMBAT", "Combat", "MAPLEGEND.COMBAT.D", "A battle took place here"},
        {"MAPLEGEND.CAT.UNITS", "/images/explosion.png", "MAPLEGEND.OVERRUN", "Overrun", "MAPLEGEND.OVERRUN.D", "A position was destroyed / overrun"},
        {"MAPLEGEND.CAT.UNITS", "/images/hex_path_army.png", "MAPLEGEND.ARMYPATH", "Army path", "MAPLEGEND.ARMYPATH.D", "A planned army movement"},
        {"MAPLEGEND.CAT.UNITS", "/images/hex_path_pj.png", "MAPLEGEND.CHARPATH", "Character path", "MAPLEGEND.CHARPATH.D", "A planned character movement"},

        {"MAPLEGEND.CAT.COLORS", "", "MAPLEGEND.CITYCOLORS", "City colors", "MAPLEGEND.CITYCOLORS.D",
            "Cities are tinted by owner. The map-colors option switches between Regular (owner), Alliance (allies share a color), My enemies (hostiles highlighted) and Border (ownership outlines)."},
    };

    private MapLegendDialog(Window parent, BundleManager labels) {
        super(parent, ModalityType.MODELESS);
        this.labels = labels;
        AppIcon.applyTo(this);
        setTitle(tx("MAPLEGEND.TITLE", "Map legend"));
        buildContent();
        setSize(new Dimension(440, 560));
        setLocationRelativeTo(parent);
    }

    /** Show the legend (non-modal, so the player can keep it open beside the map). */
    public static void show(Component parent, BundleManager labels) {
        Window w = (parent instanceof Window) ? (Window) parent
                : javax.swing.SwingUtilities.getWindowAncestor(parent);
        new MapLegendDialog(w, labels).setVisible(true);
    }

    private void buildContent() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        String lastCat = null;
        for (String[] row : ROWS) {
            if (!row[0].equals(lastCat)) {
                lastCat = row[0];
                JLabel header = new JLabel(tx(row[0], row[0]));
                header.setFont(header.getFont().deriveFont(Font.BOLD));
                header.setBorder(BorderFactory.createEmptyBorder(lastCat == null ? 0 : 10, 0, 4, 0));
                c.gridwidth = 2;
                c.insets = new Insets(6, 0, 2, 0);
                p.add(header, c);
                c.gridy++;
            }
            // icon
            c.gridwidth = 1;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(1, 4, 1, 8);
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(ICON_MAX + 4, ICON_MAX + 4));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            ImageIcon icon = loadIcon(row[1]);
            if (icon != null) {
                iconLabel.setIcon(icon);
            }
            p.add(iconLabel, c);
            // label + description
            c.gridx = 1;
            c.weightx = 1;
            String label = tx(row[2], row[3]);
            String desc = tx(row[4], row[5]);
            String text = desc.isEmpty()
                    ? String.format("<html><b>%s</b></html>", label)
                    : String.format("<html><b>%s</b> &ndash; %s</html>", label, desc);
            p.add(new JLabel(text), c);
            c.gridy++;
        }

        JScrollPane scroll = new JScrollPane(p,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton close = new JButton(tx("MAPLEGEND.CLOSE", "Close"));
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.add(close);

        setLayout(new java.awt.BorderLayout());
        add(scroll, java.awt.BorderLayout.CENTER);
        add(south, java.awt.BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);
    }

    /** Loads a classpath image and scales it to the uniform icon box; null if the resource is absent. */
    private ImageIcon loadIcon(String resource) {
        if (resource == null || resource.isEmpty()) {
            return null;
        }
        try {
            URL url = getClass().getResource(resource);
            if (url == null) {
                log.debug("Legend icon not found: " + resource);
                return null;
            }
            ImageIcon raw = new ImageIcon(url);
            int w = raw.getIconWidth();
            int h = raw.getIconHeight();
            if (w <= 0 || h <= 0) {
                return null;
            }
            if (w <= ICON_MAX && h <= ICON_MAX) {
                return raw;
            }
            double s = Math.min((double) ICON_MAX / w, (double) ICON_MAX / h);
            Image scaled = raw.getImage().getScaledInstance((int) (w * s), (int) (h * s), Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            log.debug("Legend icon load failed: " + resource + " - " + ex);
            return null;
        }
    }

    private String tx(String key, String fallback) {
        String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }
}
