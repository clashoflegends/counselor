package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.NavyCombatResolver;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.LayerReport;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * The result of a run, in numbers. T-802.
 *
 * <h3>Numbers, not prose</h3>
 *
 * John, 2026-09-21: "Would be nice to be able to see the turn by turn results. And how many combat
 * rounds per layer. It could be the data points only, no narrative." So this is three things and
 * nothing else: a verdict line, a casualty summary, and ONE ROUNDS TABLE PER LAYER whose rows are
 * armies, columns are rounds, and cells are troops REMAINING. The narrative is T-803 and arrives
 * beside this, not instead of it.
 *
 * <h3>Remaining, not lost</h3>
 *
 * Losses answer "what did that cost"; remaining answers "was it enough", and the second is the
 * question the simulator is opened to ask. Reading down a column says who is still standing after
 * round 3; reading across a row says how fast an army is melting. The summary above gives the
 * differences for anyone who wants them.
 *
 * <h3>A layer that did not happen says why</h3>
 *
 * It does not vanish. "The city was never assaulted" and "the city held" are different answers to
 * the player's question, and today a third applies to two of the three layers: not simulated yet.
 * Leaving them out would let a land-only forecast read as a whole battle.
 *
 * <h3>Non-modal, like the diplomacy panel</h3>
 *
 * The player compares this against the platoon table and the roster while it is open, and re-runs
 * after changing a tactic. A modal dialog would make him close it to do either.
 */
public class BattleSimResultDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final BundleManager labels =
            SettingsManager.getInstance().getBundleManager();
    /** One table row, the unit the whole page is built out of. */
    private static final int ROW_SCROLL = 16;

    private final transient BattleSimResultPanel body;

    public BattleSimResultDialog(Frame owner, BattleSimControler controler) {
        super(owner, labels.getString("BATTLESIM.RESULTS.OPEN"), false);
        this.body = new BattleSimResultPanel(controler);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(body, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        setTitle(body.getResultTitle());
        pack();
        doSizeToContent();
        setLocationRelativeTo(owner);
        doRestoreBounds();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                doRememberBounds();
            }

            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                doRememberBounds();
            }
        });
    }

    /**
     * Opens at the size the result actually needs, bounded by the screen rather than by a constant.
     *
     * It used to be {@code min(760, ...)} wide. 760 fits a land-only battle and clips a three-layer
     * one, because each layer table carries a column per round: the first real sea-land-city result
     * opened with the Lost column off the edge, a horizontal scrollbar under the tables, and the
     * caveat line cut off mid-sentence. Every number was right and none of them could be read.
     *
     * Bounded at 90% of the screen so it cannot open wider than the display, and floored so a
     * one-line result is still a dialog rather than a slot.
     */
    private void doSizeToContent() {
        final Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        final int maxWidth = (int) (screen.width * 0.9);
        final int maxHeight = (int) (screen.height * 0.85);
        setSize(new Dimension(
                Math.min(maxWidth, Math.max(560, getWidth())),
                Math.min(maxHeight, Math.max(360, getHeight()))));
    }

    /** One key, four numbers, so a half-written value cannot leave the window half-placed. */
    private static final String BOUNDS_KEY = "battleSimResultBounds";

    /**
     * Puts the dialog back where it was left, if it is still somewhere a player can reach.
     *
     * Sizing to content is the right OPENING guess and it is only a guess: how wide a result wants
     * to be is a matter of how long the battle ran, and a player who has widened it once has said
     * what he wants. Remembered rather than recomputed from then on.
     *
     * VALIDATED against the current screen before it is used. A saved position is a fact about the
     * monitor that was attached when it was saved: reconnecting a laptop without its second display
     * would otherwise reopen this window at x=2400, off the edge of a screen that no longer exists,
     * with no way to drag it back.
     */
    private void doRestoreBounds() {
        final String saved = SettingsManager.getInstance().getConfig(BOUNDS_KEY, "");
        final String[] parts = saved.split(",");
        if (parts.length != 4) {
            return;
        }
        try {
            final java.awt.Rectangle want = new java.awt.Rectangle(
                    Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
            final Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            if (want.width < 360 || want.height < 240
                    || want.x + 120 > screen.width || want.y + 60 > screen.height
                    || want.x + want.width < 120 || want.y < 0) {
                return;     // off the edge, or too small to hold anything: keep the fresh size
            }
            setBounds(want);
        } catch (NumberFormatException ex) {
            // a hand-edited properties.config; the computed size is a perfectly good answer
        }
    }

    /**
     * Saves size and position on every move or resize.
     *
     * To the file, not just to memory, because the value of remembering is that it survives the
     * session where the player did the resizing.
     */
    private void doRememberBounds() {
        if (!isShowing()) {
            return;     // the bounds during construction and disposal are not the player's choice
        }
        SettingsManager.getInstance().setConfigAndSaveToFile(BOUNDS_KEY,
                getX() + "," + getY() + "," + getWidth() + "," + getHeight());
    }

    /** Rebuilt whole on every Run, because every number in it changes. */
    public void refresh() {
        body.refresh();
        setTitle(body.getResultTitle());
    }

    private JPanel buildButtons() {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 6));
        final JButton close = new JButton(labels.getString("BATTLESIM.DIPLOMACY.CLOSE"));
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        ret.add(close);
        return ret;
    }

    // ------------------------------------------------------------------ pieces
}
