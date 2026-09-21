package e2e;

import gui.services.ComponentFactory;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Windows that show ONE game must not survive the opening of another.
 *
 * John, 2026-09-20, with a screenshot of three BattleSim windows from two different games open at
 * once: "these are from different games. As I opened new EGFs to switch, the BattleSim persisted."
 *
 * <h3>Why this is worse than a stale chart</h3>
 *
 * A chart left over from the previous game shows old numbers and nothing else. A BattleSim window
 * is live: its {@code CombatScenario} holds armies, nations and a Partida from the world that built
 * it, while its nation and troop-type combos read the CURRENT world every time they are opened. So
 * after a switch it offers new-game nations and new-game troop types for armies that belong to the
 * old one, and the diplomacy matrix derives against a Partida those armies were never in. Two games
 * mixed inside one scenario, with nothing on screen saying so.
 *
 * <h3>Why the list is asserted rather than the behaviour</h3>
 *
 * {@code disposeSecondaryWindows} needs real {@code java.awt.Window} instances to do anything, which
 * a headless build cannot make. What actually goes wrong is not the loop - it is someone adding a
 * new snapshot window and never registering it, after which it silently outlives its game. So the
 * TYPE LIST is the thing worth pinning.
 */
class ComponentFactoryStaleWindowTest {

    private static List<Class<?>> staleTypes() {
        return Arrays.asList(ComponentFactory.getStaleOnNewGameTypes());
    }

    /** The window this test was written for. */
    @Test
    void theBattleSimWindowIsClosedWhenAnotherGameIsOpened() {
        assertTrue(staleTypes().contains(gui.accessories.BattleSimWindow.class),
                "a BattleSim holds armies from the game that built it; it cannot outlive it");
    }

    /** The old window has the same problem and dies the same way, until it is retired (T-438). */
    @Test
    void theOldBattleSimWindowIsAlsoClosed() {
        assertTrue(staleTypes().contains(gui.accessories.BattleCasualtySimulatorNew.class));
    }

    /** It is built from a Terreno of the scenario being replaced. */
    @Test
    void theCasualtiesListIsAlsoClosed() {
        assertTrue(staleTypes().contains(gui.accessories.TroopsCasualtiesList.class));
    }

    /** The charts and dashboards that were already registered stay registered. */
    @Test
    void theOriginalSnapshotWindowsAreStillListed() {
        final List<Class<?>> types = staleTypes();
        for (Class<?> expected : new Class<?>[]{gui.charts.ChartBar.class, gui.charts.ChartLine.class,
            gui.charts.ChartPie.class, gui.charts.ChartGauge.class, gui.charts.ChartRadar.class,
            gui.charts.ChartGrowth.class}) {
            assertTrue(types.contains(expected), "lost " + expected.getSimpleName());
        }
    }

    /** A defensive copy: nobody can empty the list by accident from outside. */
    @Test
    void theListIsNotSharedWithCallers() {
        final Class<?>[] one = ComponentFactory.getStaleOnNewGameTypes();
        Arrays.fill(one, null);

        assertTrue(ComponentFactory.getStaleOnNewGameTypes()[0] != null,
                "getStaleOnNewGameTypes must hand out a copy");
    }
}
