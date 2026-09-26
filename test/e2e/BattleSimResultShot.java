package e2e;

import business.combat.ArmySim;
import business.combat.CombatLevel;
import business.combat.CombatScenario;
import control.BattleSimControler;
import gui.accessories.BattleSimResultDialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import model.Local;

/**
 * A picture of the RESULT DIALOG, which {@link BattleSimShots} never takes.
 *
 * The shots harness photographs the setup window and stops there: it never presses Run, so
 * everything the run produces - the verdict lines, the title's round count, one table per layer -
 * had shipped without anyone looking at it. The defect that prompted this class is exactly the kind
 * that only a picture shows: a battle that razed a city was captioned "No land battle took place on
 * this hex", and every automated check passed, because the checks were on geometry.
 *
 * It uses the same audit as the shots harness, so an off-edge label or a table wider than its
 * viewport is reported the same way, and writes a PNG beside it for the human half.
 *
 * Run by hand, like {@link BattleSimShots}, because it wants a real EGF and a real screen:
 *
 * <pre>
 *   java -cp &lt;build/classes;build/test/classes;lib/*&gt; e2e.BattleSimResultShot &lt;file.egf&gt; 1660
 * </pre>
 *
 * It is deliberately NOT a JUnit test. Neither build system's include pattern would run it - the
 * same trap that left the two known-answer regressions in {@code FidelityHarness} dormant - and a
 * test that never runs is worse than a tool that says it is one.
 */
class BattleSimResultShot {

    /** The laptop that 10-12% of the player base is on, and the size everything has to survive. */
    private static final int WIDTH = 1366;
    private static final int HEIGHT = 768;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: BattleSimResultShot <file.egf> <hex>");
            return;
        }
        run(new File(args[0]), args[1]);
        // FlatLaf and AWT keep non-daemon threads alive, as they do for BattleSimShots.
        System.exit(0);
    }

    private static boolean run(File egf, String hex) throws Exception {
        BattleSimShots.installLookAndFeel();
        final Local local = BattleSimShots.loadHex(egf, hex);
        if (local == null) {
            return false;
        }
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                shoot(local, hex);
            }
        });
        return true;
    }

    private static void shoot(Local local, String hex) {
        // The SETUP WINDOW after a run, so the Result tab is photographed in place - that is where
        // a player reads it now, and the pop-out is the second case rather than the first.
        final gui.accessories.BattleSimWindow window = new gui.accessories.BattleSimWindow(local);
        window.pack();
        window.setSize(WIDTH - 60, HEIGHT - 40);
        // LAY IT OUT before measuring. Sizing a window does not run its layout, so auditing here
        // without this reports every label as 0x0 - a fault in the tool that reads exactly like a
        // fault in the window.
        window.validate();
        try {
            BattleSimShots.auditTree(window, "window/" + hex, null);
            BattleSimShots.write(window, new File("target/shots"),
                    "bsim_window_" + hex + ".png", 1.0);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        final BattleSimControler controler = new BattleSimControler(local);
        final CombatScenario scenario = controler.getScenario();
        // Ordered to assault, because a hex nobody attacked has no city layer to photograph and
        // the combat level of a foreign army does not ride the EGF.
        for (ArmySim army : scenario.getArmies()) {
            army.setCombatLevel(CombatLevel.ATTACK_CITY);
        }
        controler.doRun();

        // The dialog's OWN size, not one imposed here. It sizes itself to the result and clamps to
        // the screen, and forcing a size was hiding whether that works - which is the one thing
        // worth photographing. Reported below so a too-wide result is visible as a number as well
        // as a picture.
        final BattleSimResultDialog dialog = new BattleSimResultDialog((Frame) null, controler);
        dialog.setVisible(true);
        System.out.println("DIALOG|" + dialog.getWidth() + "x" + dialog.getHeight()
                + "|laptop=" + WIDTH + "x" + HEIGHT
                + "|fits=" + (dialog.getWidth() <= WIDTH && dialog.getHeight() <= HEIGHT));
        try {
            BattleSimShots.auditTree(dialog, "result/" + hex, null);
            final File out = new File("target/shots");
            out.mkdirs();
            BattleSimShots.write(dialog, out, "bsim_result_" + hex + ".png", 1.0);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            dialog.dispose();
        }
    }
}
