package e2e;

import business.combat.ArmySim;
import control.BattleSimControler;
import control.facade.WorldFacadeCounselor;
import gui.accessories.BattleSimWindow;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import model.Local;
import persistenceCommons.SettingsManager;

/**
 * Screenshots of {@link BattleSimWindow} at several UI scales, without a human opening the app.
 *
 * <h3>Why this is not a test</h3>
 *
 * It asserts nothing. It renders the window and writes PNGs plus a measured layout audit, so a
 * layout regression can be LOOKED at rather than described. A class ending in {@code Test} is
 * picked up by the JUnit runner (see {@code reference-counselor-test-suite}), and this must not be:
 * it needs a real display, a real EGF and one JVM per scale.
 *
 * <h3>One JVM per scale, and why the picture is bigger than the window</h3>
 *
 * {@code sun.java2d.uiScale} is read once, before the first AWT class initialises, so a scale
 * cannot be changed inside a running JVM - hence the driver loop outside. What that knob does on
 * Java 9+ is change the DEVICE resolution, not the layout: a Swing window keeps its logical size
 * and Java2D paints it at {@code scale} times as many pixels. So a 1366x768 PHYSICAL laptop screen
 * is only {@code 1366/scale x 768/scale} LOGICAL pixels, and that shrinking viewport - not any
 * change in the window - is what a high UI scale does to a layout.
 *
 * That is why the images are allocated at {@code ceil(w*scale) x ceil(h*scale)} with the Graphics
 * scaled to match. A plain BufferedImage of {@code frame.getSize()} would come out byte-identical
 * at 100%, 150% and 200%, which would look like a passing result and mean nothing.
 *
 * <h3>Not headless</h3>
 *
 * {@code java.awt.headless=true} cannot be used: {@code BattleSimWindow} is a {@code JFrame} and
 * {@code Window}'s constructor throws {@code HeadlessException}. The frame is {@code pack()}ed,
 * which creates the peer and lays everything out, but never made visible; the render is
 * {@code rootPane.printAll(g)} into a BufferedImage. Nothing appears on the desktop.
 *
 * <h3>Running it</h3>
 *
 * Run it from a SCRATCH directory. {@code SettingsManager} writes {@code properties.config} and
 * log4j writes {@code counselor.log} into the working directory, so running it from
 * {@code PbmCounselor/} would read - and rewrite - the real local config, theme and font size
 * included. The five {@code --add-opens} are the same ones {@code run.bat} carries; without them
 * XStream cannot reach {@code TreeMap.comparator} and the EGF never loads.
 *
 * <pre>
 * javac -cp "PbmCounselor/build/classes;PbmCounselor/lib/*" -d &lt;tmp&gt; \
 *       PbmCounselor/test/e2e/BattleSimShots.java
 * cd &lt;scratch dir&gt;
 * java -Dsun.java2d.uiScale=1.5 -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true \
 *      --add-opens java.base/java.util=ALL-UNNAMED \
 *      --add-opens java.base/java.lang=ALL-UNNAMED \
 *      --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
 *      --add-opens java.base/java.text=ALL-UNNAMED \
 *      --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
 *      -cp "&lt;tmp&gt;;PbmCounselor/build/classes;PbmCounselor/lib/*" \
 *      e2e.BattleSimShots &lt;file.egf&gt; &lt;hex&gt; &lt;outDir&gt; 1.5 [screenW screenH]
 * </pre>
 *
 * A crowded hex is worth the trouble of finding: a real turn file with several armies, a city and
 * at least one army the player cannot see into exercises the layout in ways a hand-built fixture
 * does not. {@code business.combat.WorldProbe} in PbmCommons lists the candidates in any EGF.
 *
 * Every path is an argument on purpose. PbmCounselor is a PUBLIC repo and a hardcoded network
 * share or local user directory in a committed file would leak exactly what hard constraint 6
 * forbids.
 */
public final class BattleSimShots {

    private static final List<String> audit = new ArrayList<>();

    private BattleSimShots() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("usage: BattleSimShots <file.egf> <hex> <outDir> <scale> "
                    + "[screenW screenH]");
            return;
        }
        final File egf = new File(args[0]);
        final String hex = args[1];
        final File out = new File(args[2]);
        final double scale = Double.parseDouble(args[3]);
        final int screenW = args.length > 4 ? Integer.parseInt(args[4]) : 1366;
        final int screenH = args.length > 5 ? Integer.parseInt(args[5]) : 768;
        out.mkdirs();

        installLookAndFeel();
        say("ENV|headless=" + GraphicsEnvironment.isHeadless()
                + "|uiScale=" + System.getProperty("sun.java2d.uiScale")
                + "|deviceTx=" + GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform()
                        .getScaleX()
                + "|laf=" + UIManager.getLookAndFeel().getName()
                + "|labelFont=" + UIManager.getFont("Label.font")
                + "|flatUserScale=" + com.formdev.flatlaf.util.UIScale.getUserScaleFactor());

        final WorldFacadeCounselor wfc = WorldFacadeCounselor.getInstance();
        wfc.doStart(egf);
        final Local local = findHex(wfc, hex);
        if (local == null) {
            say("FATAL|hex " + hex + " not in this EGF");
            System.exit(2);
        }
        say("HEX|" + local.getCoordenadas()
                + "|armies=" + (local.getExercitos() == null ? 0 : local.getExercitos().size())
                + "|city=" + (local.getCidade() == null ? "-" : local.getCidade().getNome())
                + "|terrain=" + (local.getTerreno() == null ? "-" : local.getTerreno().getNome()));

        final String tag = String.valueOf((int) Math.round(scale * 100)) + "pct";
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    shootAll(local, out, tag, scale, screenW, screenH);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        // FlatLaf and AWT keep non-daemon threads alive; without this the per-scale loop hangs.
        System.exit(0);
    }

    private static void shootAll(Local local, File out, String tag, double scale,
            int screenW, int screenH) throws Exception {
        final BattleSimWindow frame = new BattleSimWindow(local);
        // The constructor already packs and then floors the size at 980x640 LOGICAL px. Record what
        // it chose, and what that costs on the target screen.
        final int lw = frame.getWidth(), lh = frame.getHeight();
        final int pw = (int) Math.ceil(lw * scale), ph = (int) Math.ceil(lh * scale);
        say("SIZE|" + tag + "|logical=" + lw + "x" + lh + "|physical=" + pw + "x" + ph
                + "|screen=" + screenW + "x" + screenH
                + "|logicalViewport=" + (int) (screenW / scale) + "x" + (int) (screenH / scale)
                + "|fits=" + (pw <= screenW && ph <= screenH)
                + "|overflowX=" + Math.max(0, pw - screenW)
                + "|overflowY=" + Math.max(0, ph - screenH)
                + "|minimumSize=" + frame.getMinimumSize().width + "x"
                + frame.getMinimumSize().height);

        final JTree roster = find(frame, JTree.class);
        final JTable platoons = find(frame, JTable.class);

        // 1) the window as it opens, at the army the controller picked
        frame.validate();
        statusBar(frame, tag + "/natural");
        auditTree(frame, tag + "/natural", platoons);
        write(frame, out, "bsim_" + tag + "_natural.png", scale);

        // 2) one shot per army, BEFORE anything is resized. Order matters and it is not cosmetic:
        // the right-hand split has resizeWeight 0, so a divider pushed by a squeeze is not put back
        // when the window is restored - shooting the armies after the laptop shot would show a
        // layout no player ever sees, and would hide the first-open caption defect. A fleet carries
        // three extra transport columns (T-428); an unscouted army shows the morale-unknown label.
        eachArmy(frame, roster, platoons, out, tag, "", scale);
        frame.dispose();

        // 3) a SECOND window for the squeezed states, because the divider does not come back. The
        // two sizes are different questions: the minimum is the smallest the player can actually
        // drag to, and the forced size is below that - only a stand-in for whatever the window
        // manager does when the minimum will not fit on the screen, which this harness cannot
        // observe because it never shows the window.
        final BattleSimWindow small = new BattleSimWindow(local);
        final JTable smallPlatoons = find(small, JTable.class);
        final Dimension floor = small.getMinimumSize();
        small.setSize(floor.width, floor.height);
        small.validate();
        say("MINIMUM|" + tag + "|logical=" + small.getWidth() + "x" + small.getHeight()
                + "|physical=" + (int) Math.ceil(small.getWidth() * scale) + "x"
                + (int) Math.ceil(small.getHeight() * scale)
                + "|fitsScreen=" + (small.getWidth() * scale <= screenW
                        && small.getHeight() * scale <= screenH));
        final JTree smallRoster = find(small, JTree.class);
        auditTree(small, tag + "/minimum", smallPlatoons);
        write(small, out, "bsim_" + tag + "_minimum.png", scale);
        // Every army at the squeezed sizes too, and this is where it matters most: the two
        // unscouted armies carry the LONGEST text in the window ("To include it: use Add platoon to
        // say what you think it holds..."), so a pane that holds army0's four short lines can still
        // cut theirs. Selecting a row resizes nothing, so the loop stays inside this one state.
        eachArmy(small, smallRoster, smallPlatoons, out, tag, "min", scale);

        final int fitW = (int) Math.floor(screenW / scale), fitH = (int) Math.floor(screenH / scale);
        small.setMinimumSize(new Dimension(1, 1));
        small.setSize(fitW, fitH);
        small.validate();
        say("FORCED|" + tag + "|asked=" + fitW + "x" + fitH
                + "|got=" + small.getWidth() + "x" + small.getHeight()
                + "|belowFloor=" + (fitW < floor.width || fitH < floor.height));
        auditTree(small, tag + "/laptop", smallPlatoons);
        write(small, out, "bsim_" + tag + "_laptop" + screenW + "x" + screenH + ".png", scale);
        eachArmy(small, smallRoster, smallPlatoons, out, tag, "laptop", scale);
        small.dispose();
    }

    /** One shot per army in the roster, at whatever size the window currently is. */
    private static void eachArmy(java.awt.Window frame, JTree roster, JTable platoons, File out,
            String tag, String state, double scale) throws Exception {
        int shot = 0;
        for (int row = 0; row < roster.getRowCount() && shot < 4; row++) {
            final Object node = roster.getPathForRow(row).getLastPathComponent();
            if (!(node instanceof DefaultMutableTreeNode)) {
                continue;
            }
            final Object user = ((DefaultMutableTreeNode) node).getUserObject();
            if (!(user instanceof BattleSimControler.ArmyNode)) {
                continue;
            }
            final ArmySim army = ((BattleSimControler.ArmyNode) user).getArmy();
            roster.setSelectionRow(row);
            frame.validate();
            shot++;
            final String name = safe(army.getNome());
            final String where = tag + "/" + (state.isEmpty() ? "army" : state + "-army") + ":"
                    + name;
            say("ARMY|" + tag + "|" + (state.isEmpty() ? "natural" : state) + "|" + name
                    + "|platoonCols=" + platoons.getColumnCount()
                    + "|platoonRows=" + platoons.getRowCount());
            auditTree((Container) frame, where, platoons);
            write(frame, out, "bsim_" + tag + "_" + (state.isEmpty() ? "" : state + "_")
                    + "army" + shot + "_" + name + ".png", scale);
        }
    }

    // ------------------------------------------------------------------ rendering

    /**
     * Paints the root pane, not the frame: a packed-but-never-shown JFrame carries native decoration
     * insets that would leave a band of nothing down two edges of every image.
     */
    private static void write(java.awt.Window frame, File dir, String name, double scale)
            throws Exception {
        final Component root = ((javax.swing.RootPaneContainer) frame).getRootPane();
        final int w = Math.max(1, (int) Math.ceil(root.getWidth() * scale));
        final int h = Math.max(1, (int) Math.ceil(root.getHeight() * scale));
        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(scale, scale);
        root.printAll(g);
        g.dispose();
        final File file = new File(dir, name);
        ImageIO.write(img, "png", file);
        say("SHOT|" + file.getAbsolutePath() + "|" + w + "x" + h);
    }

    // ------------------------------------------------------------------ measured audit

    /**
     * What the layout actually did, measured rather than eyeballed.
     *
     * A truncated label is {@code width < preferredSize.width} and nothing else; reading it off a
     * PNG is guesswork once the ellipsis is one pixel wide. The images are for the things a
     * measurement cannot name - overlap, crowding, a pane that reads badly.
     */
    private static void auditTree(Container root, String where, JTable table) {
        walk(root, where);
        panes(root, where);
        if (table != null && table.getColumnCount() > 0) {
            int wanted = 0;
            for (int ii = 0; ii < table.getColumnCount(); ii++) {
                wanted += table.getColumnModel().getColumn(ii).getPreferredWidth();
            }
            final Container viewport = table.getParent();
            final int have = viewport == null ? 0 : viewport.getWidth();
            if (wanted > have) {
                say("ISSUE|" + where + "|TABLE-OVERFLOW|platoon columns want " + wanted
                        + "px, viewport is " + have + "px");
            }
            final javax.swing.table.JTableHeader header = table.getTableHeader();
            if (header != null) {
                for (int ii = 0; ii < table.getColumnCount(); ii++) {
                    final javax.swing.table.TableColumn col = table.getColumnModel().getColumn(ii);
                    final Component cell = header.getDefaultRenderer()
                            .getTableCellRendererComponent(table, col.getHeaderValue(),
                                    false, false, -1, ii);
                    final int need = cell.getPreferredSize().width;
                    if (need > col.getWidth()) {
                        say("ISSUE|" + where + "|HEADER-CLIPPED|col " + ii + " \""
                                + col.getHeaderValue() + "\" needs " + need + "px, has "
                                + col.getWidth() + "px");
                    }
                }
            }
        }
    }

    /**
     * Every scrolling pane, measured against what it is scrolling.
     *
     * The label walk cannot see the worst failure this window has. When the split panes run out of
     * height the platoon table does not truncate anything - the VIEWPORT shrinks until neither the
     * column header nor one row of data fits, and the pane still reports a perfectly healthy
     * layout. Nothing is clipped; there is simply nowhere left to put the table. So it is measured
     * here in rows: a viewport that cannot show the header plus one row shows nothing at all.
     */
    private static void panes(Container parent, String where) {
        for (Component one : parent.getComponents()) {
            if (one instanceof JScrollPane) {
                final JScrollPane scroll = (JScrollPane) one;
                final Component view = scroll.getViewport().getView();
                final Dimension port = scroll.getViewport().getSize();
                // A JTable's column header lives in the scroll pane's COLUMN HEADER, not in the
                // viewport, so the viewport height is already rows only - subtracting the header
                // from it would report one row fewer than the player sees.
                final int unit = view instanceof JTable ? ((JTable) view).getRowHeight()
                        : view instanceof JTree ? Math.max(16, ((JTree) view).getRowHeight()) : 16;
                final int rows = Math.max(0, port.height / Math.max(1, unit));
                say("PANE|" + where + "|" + viewName(view)
                        + "|viewport=" + port.width + "x" + port.height
                        + "|rowHeight=" + unit + "|visibleRows=" + rows
                        + (view instanceof JTable ? "|dataRows=" + ((JTable) view).getRowCount()
                                : ""));
                if (rows < 1) {
                    say("ISSUE|" + where + "|PANE-COLLAPSED|"
                            + viewName(view)
                            + " viewport is " + port.width + "x" + port.height
                            + " - room for " + rows + " rows, so the pane shows nothing");
                }
            }
            if (one instanceof Container) {
                panes((Container) one, where);
            }
        }
    }

    /**
     * The two status-bar labels, printed WITH their text.
     *
     * The label walk skips blank labels on purpose - a form is full of them - but that makes an
     * empty status bar unreadable from the audit alone: a label with no text and a label starved to
     * zero width look the same. Printed here, they can be told apart.
     */
    private static void statusBar(javax.swing.RootPaneContainer frame, String where) {
        final Container content = (Container) frame.getContentPane();
        if (!(content.getLayout() instanceof java.awt.BorderLayout)) {
            return;
        }
        final Component south = ((java.awt.BorderLayout) content.getLayout())
                .getLayoutComponent(java.awt.BorderLayout.SOUTH);
        if (!(south instanceof Container)) {
            return;
        }
        for (Component one : all((Container) south)) {
            if (one instanceof JLabel) {
                say("STATUS|" + where + "|width=" + one.getWidth()
                        + "|needs=" + one.getPreferredSize().width
                        + "|text=\"" + clip(((JLabel) one).getText()) + "\"");
            }
        }
    }

    private static List<Component> all(Container parent) {
        final List<Component> ret = new ArrayList<>();
        for (Component one : parent.getComponents()) {
            ret.add(one);
            if (one instanceof Container) {
                ret.addAll(all((Container) one));
            }
        }
        return ret;
    }

    /** The platoon table and the roster are anonymous subclasses, so getSimpleName() is "". */
    private static String viewName(Component view) {
        if (view == null) {
            return "?";
        }
        Class<?> type = view.getClass();
        while (type != null && type.getSimpleName().isEmpty()) {
            type = type.getSuperclass();
        }
        return type == null ? "?" : type.getSimpleName();
    }

    /**
     * Two siblings on the same spot, which is how the toolbar loses its Run button.
     *
     * BorderLayout hands LINE_START its full preferred width and then places LINE_END against the
     * right edge regardless, so an overfull button row does not push Run off the window - it lays
     * itself straight over the top of it. Both panels measure correctly, both are inside the
     * window, and one is invisible because the other is painted after it. Only an intersection
     * test finds this.
     */
    private static void overlaps(Container parent, String where) {
        // The layered pane is SUPPOSED to stack: FlatLaf's window title bar sits on a layer over
        // the content pane by design, and reporting that would bury the real one.
        if (parent instanceof javax.swing.JLayeredPane || parent instanceof javax.swing.JRootPane) {
            return;
        }
        final Component[] kids = parent.getComponents();
        for (int ii = 0; ii < kids.length; ii++) {
            for (int jj = ii + 1; jj < kids.length; jj++) {
                if (!kids[ii].isVisible() || !kids[jj].isVisible()) {
                    continue;
                }
                final java.awt.Rectangle hit = kids[ii].getBounds()
                        .intersection(kids[jj].getBounds());
                if (hit.width > 1 && hit.height > 1) {
                    say("ISSUE|" + where + "|OVERLAP|" + describe(kids[ii]) + " and "
                            + describe(kids[jj]) + " share " + hit.width + "x" + hit.height
                            + "px; the one painted last hides the other");
                }
            }
        }
    }

    private static void walk(Container parent, String where) {
        final Component root = SwingUtilities.getRootPane(parent);
        overlaps(parent, where);
        for (Component one : parent.getComponents()) {
            if (!skip(one)) {
                final Dimension want = one.getPreferredSize();
                // Pushed off the window entirely, which no size comparison can see: an overfull
                // FlowLayout gives every button its full preferred width and simply lays the last
                // ones out past the edge. They measure perfectly and are not on screen.
                final java.awt.Rectangle box = SwingUtilities.convertRectangle(one.getParent(),
                        one.getBounds(), root);
                if (root != null && (box.x + box.width > root.getWidth() + 1
                        || box.y + box.height > root.getHeight() + 1
                        || box.x < -1 || box.y < -1)) {
                    say("ISSUE|" + where + "|OFF-EDGE|" + describe(one) + " sits at "
                            + box.x + "," + box.y + " " + box.width + "x" + box.height
                            + " in a window of " + root.getWidth() + "x" + root.getHeight());
                } else if (one.getWidth() == 0 || one.getHeight() == 0) {
                    say("ISSUE|" + where + "|COLLAPSED|" + describe(one)
                            + " is " + one.getWidth() + "x" + one.getHeight()
                            + ", wants " + want.width + "x" + want.height);
                } else if (isHtml(one)) {
                    htmlFit((JLabel) one, where);
                } else if (want.width > one.getWidth() + 1) {
                    say("ISSUE|" + where + "|TRUNCATED|" + describe(one)
                            + " has " + one.getWidth() + "px, needs " + want.width + "px");
                } else if (want.height > one.getHeight() + 1) {
                    say("ISSUE|" + where + "|CLIPPED-HEIGHT|" + describe(one)
                            + " has " + one.getHeight() + "px, needs " + want.height + "px");
                }
            }
            if (one instanceof Container && !(one instanceof JScrollPane
                    && ((JScrollPane) one).getViewport().getView() instanceof JTable)) {
                walk((Container) one, where);
            }
        }
    }

    private static boolean isHtml(Component one) {
        return one instanceof JLabel && ((JLabel) one).getText() != null
                && ((JLabel) one).getText().toLowerCase().startsWith("<html>");
    }

    /**
     * An HTML label measured the way it is actually PAINTED, which its preferred size does not say.
     *
     * {@code getPreferredSize()} on a wrapping label reports the width the text would need on ONE
     * line - 751px for the casualty caption - and comparing that to the label's 623px is how a
     * perfectly fine two-line label gets reported as truncated. The question that matters is the
     * other one: given the width the layout actually gave it, how TALL does the text want to be,
     * and did it get that much? A fresh {@code View} is built rather than the label's own, so this
     * cannot disturb what is on screen.
     */
    private static void htmlFit(JLabel label, String where) {
        final javax.swing.text.View view =
                javax.swing.plaf.basic.BasicHTML.createHTMLView(label, label.getText());
        final java.awt.Insets in = label.getInsets();
        final int innerW = label.getWidth() - in.left - in.right;
        final int innerH = label.getHeight() - in.top - in.bottom;
        if (innerW <= 0) {
            return;
        }
        view.setSize(innerW, 0);
        final int needH = (int) Math.ceil(
                view.getPreferredSpan(javax.swing.text.View.Y_AXIS));
        if (needH > innerH + 1) {
            say("ISSUE|" + where + "|HTML-CLIPPED|" + describe(label)
                    + " wraps to " + needH + "px at its " + innerW + "px width, but has "
                    + innerH + "px - the tail is cut");
        }
    }

    /**
     * Only widgets that carry WORDS are judged, and only ones the layout owns.
     *
     * The look-and-feel's own furniture - scrollbar arrows, split-pane one-touch triangles, the
     * tree's shared renderer stamp - is reported "too small" by construction: a FlatLaf scrollbar
     * button is deliberately 0x0 and a one-touch triangle is deliberately 5px of a 16px icon. Left
     * in, sixty lines of that buried the four findings that are real.
     */
    private static boolean skip(Component one) {
        if (!one.isVisible() || one instanceof javax.swing.CellRendererPane) {
            return true;
        }
        if (!(one instanceof JLabel || one instanceof AbstractButton
                || one instanceof javax.swing.JComboBox)) {
            return true;
        }
        final String name = one.getClass().getName();
        if (name.startsWith("com.formdev.flatlaf") || name.contains("BasicArrowButton")
                || name.contains("TreeCellRenderer") || name.contains("OneTouch")) {
            return true;
        }
        return one instanceof JLabel && isBlank(((JLabel) one).getText());
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static String describe(Component one) {
        final String text;
        if (one instanceof JLabel) {
            text = ((JLabel) one).getText();
        } else if (one instanceof AbstractButton) {
            text = ((AbstractButton) one).getText();
        } else if (one instanceof javax.swing.JComboBox) {
            // getNome, not toString: BaseModel.toString answers "Tyrell-model.Nacao@1a2b3c", which
            // would report a 40-character name the player never sees and make every nation combo
            // look far too narrow.
            final Object item = ((javax.swing.JComboBox<?>) one).getSelectedItem();
            text = "combo:" + (item instanceof baseLib.BaseModel
                    ? ((baseLib.BaseModel) item).getNome() : String.valueOf(item));
        } else {
            text = "";
        }
        return one.getClass().getSimpleName() + "[" + clip(text) + "]";
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        final String flat = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return flat.length() > 60 ? flat.substring(0, 57) + "..." : flat;
    }

    // ------------------------------------------------------------------ plumbing

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component one : parent.getComponents()) {
            if (type.isInstance(one)) {
                return (T) one;
            }
            if (one instanceof Container) {
                final T deeper = find((Container) one, type);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return null;
    }

    /** Hex by map key, falling back to a scan on the printed coordinates. */
    private static Local findHex(WorldFacadeCounselor wfc, String hex) {
        final Local direct = wfc.getLocais().get(hex);
        if (direct != null) {
            return direct;
        }
        for (Local one : wfc.getLocais().values()) {
            if (one != null && hex.equals(one.getCoordenadas())) {
                return one;
            }
        }
        return null;
    }

    /**
     * What {@code Main} plus {@code Application.installLookAndFeel} do, in that order.
     *
     * {@code WindowsTitleBar.installSystemTitleBarTracking} is deliberately left out: it hangs an
     * AWT listener that recolours the native title bar on {@code WINDOW_OPENED}, which never fires
     * for a window that is packed and painted but never shown, and which changes a colour rather
     * than a layout. So the title bar in these images is FlatLaf's own, in light mode.
     */
    private static void installLookAndFeel() {
        final SettingsManager sm = SettingsManager.getInstance();
        sm.setConfigurationMode("Client");
        sm.setLanguage(sm.getConfig("language", "en"));
        com.formdev.flatlaf.FlatLaf.registerCustomDefaultsSource("themes");
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            say("WARN|FlatLightLaf failed: " + ex);
        }
        // Application.setUIFont, after the L&F as there: setLookAndFeel replaces the defaults and
        // would discard an earlier bump. This is the ONE setting that changes the LOGICAL layout -
        // sun.java2d.uiScale does not - so a run with it unset is the best case, not the only one.
        final float fontSize = sm.getConfigAsInt("LookAndFeelFontSize", "0");
        if (fontSize > 10) {
            for (Object key : java.util.Collections.list(UIManager.getDefaults().keys())) {
                final Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key,
                            ((javax.swing.plaf.FontUIResource) value).deriveFont(fontSize));
                }
            }
            say("FONT|LookAndFeelFontSize=" + (int) fontSize + " applied");
        }
    }

    private static String safe(String name) {
        return name == null ? "?" : name.replaceAll("[^A-Za-z0-9]+", "-");
    }

    private static void say(String line) {
        audit.add(line);
        System.out.println(line);
    }
}
