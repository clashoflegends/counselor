package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.LayerReport;
import business.combat.NavyCombatResolver;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * The result as PLAIN TEXT, for an email to an ally or a paste into a chat. T-805, T-847.
 *
 * <h3>Why text and not a screenshot</h3>
 *
 * Because the ally is meant to argue with it. A picture of a battle can be admired; a table of
 * numbers can be checked against his own reading of the same hex, and the save file beside it lets
 * him change an assumption and re-run. The whole feature exists so two people can disagree
 * precisely.
 *
 * <h3>Kept honest against the panel</h3>
 *
 * This is a SECOND rendering of a result, which is exactly the thing that drifts - and a drift here
 * means the email says something the sender's screen does not. So it reads from the same sources as
 * {@link BattleSimResultPanel}, in the same order: {@code getVerdictLines}, {@code getArmyTotals},
 * {@code LayerReport.of} per layer, the same note prefixes, and that class's own
 * {@code NOT_MODELLED} list rather than a copy of it. Where the panel decides something - which
 * notes are suppressed because a layer already says them - this asks the panel rather than deciding
 * again.
 *
 * The one deliberate difference: no HTML, and tables are laid out with padding rather than columns,
 * because the destination is a mail client that will reflow anything cleverer.
 */
public final class BattleSimResultText {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    /** Wide enough for "Barristan Selmy" and the longest troop name, short enough to survive a mail client. */
    private static final int NAME_WIDTH = 24;
    private static final int CELL_WIDTH = 12;
    private static final String NL = System.lineSeparator();

    private BattleSimResultText() {
    }

    /** The whole result, ready to paste. Empty when nothing has been run. */
    public static String render(BattleSimControler controler) {
        final CombatResult result = controler == null ? null : controler.getLastResult();
        final CombatScenario scenario = controler == null ? null : controler.getScenario();
        if (result == null || scenario == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder();
        ret.append(title(scenario, result)).append(NL).append(NL);

        for (String line : BattleSimConverter.getVerdictLines(scenario, result)) {
            ret.append(line).append(NL);
        }

        ret.append(NL).append(labels.getString("BATTLESIM.RESULTS.CASUALTIES")).append(NL);
        ret.append(row(labels.getString("BATTLESIM.RESULTS.ARMY"),
                labels.getString("BATTLESIM.RESULTS.BEFORE"),
                labels.getString("BATTLESIM.RESULTS.AFTER"),
                labels.getString("BATTLESIM.RESULTS.LOST")));
        for (ArmySim army : scenario.getArmies()) {
            final int[] totals = BattleSimConverter.getArmyTotals(army, result);
            ret.append(totals == null
                    ? row(scenario.getDisplayName(army), "--", "--", "--")
                    : row(scenario.getDisplayName(army), number(totals[0]), number(totals[1]),
                            number(totals[2])));
        }

        for (CombatLayer layer : CombatLayer.values()) {
            ret.append(NL).append(layerHeading(layer)).append(NL);
            final LayerReport report = controler.getLayerReport(layer);
            if (!report.isFought()) {
                ret.append(labels.getString(report.getNotFoughtReason())).append(NL);
                continue;
            }
            // The same caption the panel prints, for the same reason: the number of rounds, and
            // WHAT the columns count - ships for the sea layer, bodies for the other two. Leaving
            // it out of the text version was the first drift between the two renderings and it
            // took one reading of the output to spot, which is why the output gets read.
            ret.append(String.format(labels.getString("BATTLESIM.RESULTS.ROUNDS"),
                    report.getRounds()))
                    .append("  -  ")
                    .append(labels.getString(layer == CombatLayer.NAVY
                            ? "BATTLESIM.RESULTS.REMAINING.SHIPS"
                            : "BATTLESIM.RESULTS.REMAINING"))
                    .append(NL);
            ret.append(roundsHeader(report, layer));
            for (ArmySim army : report.getArmies()) {
                final StringBuilder line = new StringBuilder(pad(scenario.getDisplayName(army)));
                for (int col = 0; col <= report.getRounds(); col++) {
                    final int value = report.getRemaining(army, col);
                    line.append(cell(value < 0 ? "--" : number(value)));
                }
                ret.append(line.toString().replaceAll("\\s+$", "")).append(NL);
            }
        }

        ret.append(footer(result));
        return ret.toString();
    }

    /** The three footer lists, asked of the panel so the two cannot disagree about them. */
    private static String footer(CombatResult result) {
        final StringBuilder ret = new StringBuilder();
        ret.append(notes(result, "BATTLESIM.RESULTS.NOTES", "BATTLESIM.RESULT."));
        ret.append(notes(result, "BATTLESIM.RESULTS.WITHHELD", "BATTLESIM.WITHHELD."));
        ret.append(NL).append(labels.getString("BATTLESIM.RESULTS.NOTMODELLED")).append(NL);
        ret.append("- ").append(labels.getString("BATTLESIM.NOTMODELLED.ENGINE")).append(NL);
        for (String key : BattleSimResultPanel.NOT_MODELLED) {
            ret.append("- ").append(labels.getString(key)).append(NL);
        }
        return ret.toString();
    }

    private static String notes(CombatResult result, String heading, String prefix) {
        final StringBuilder ret = new StringBuilder();
        for (String note : result.getNotes()) {
            if (!note.startsWith(prefix) || BattleSimResultPanel.isAlreadySaidPerLayer(note)) {
                continue;
            }
            if (ret.length() == 0) {
                ret.append(NL).append(labels.getString(heading)).append(NL);
            }
            final int count = result.getNoteCount(note);
            ret.append("- ").append(count > 0
                    ? String.format(labels.getString(note), count)
                    : labels.getString(note)).append(NL);
        }
        return ret.toString();
    }

    private static String title(CombatScenario scenario, CombatResult result) {
        return String.format(labels.getString("BATTLESIM.RESULTS.TITLE"),
                scenario.getLocal() == null ? "" : scenario.getLocal().getCoordenadas(),
                String.format(labels.getString("BATTLESIM.RESULTS.ROUNDS"), result.getRounds()));
    }

    private static String layerHeading(CombatLayer layer) {
        return String.format(labels.getString("BATTLESIM.RESULTS.LAYER"), layer.ordinal() + 1,
                labels.getString("BATTLESIM.LAYER." + layer.name()).toUpperCase());
    }

    /** Start, then one column per round, numbered the way that layer numbers its rounds. */
    private static String roundsHeader(LayerReport report, CombatLayer layer) {
        final StringBuilder ret = new StringBuilder(pad(labels.getString("BATTLESIM.RESULTS.ARMY")));
        ret.append(cell(labels.getString("BATTLESIM.RESULTS.START")));
        for (int col = 1; col <= report.getRounds(); col++) {
            if (layer == CombatLayer.CITY) {
                ret.append(cell(labels.getString("BATTLESIM.RESULTS.ROUNDASSAULT")));
            } else if (layer == CombatLayer.NAVY) {
                ret.append(cell(String.format(labels.getString("BATTLESIM.RESULTS.ROUND"),
                        col - 1 + NavyCombatResolver.FIRST_ROUND)));
            } else if (col == 1) {
                ret.append(cell(labels.getString("BATTLESIM.RESULTS.ROUNDFS")));
            } else {
                ret.append(cell(String.format(labels.getString("BATTLESIM.RESULTS.ROUND"),
                        col - 1)));
            }
        }
        return ret.toString().replaceAll("\\s+$", "") + NL;
    }

    private static String row(String name, String a, String b, String c) {
        return (pad(name) + cell(a) + cell(b) + cell(c)).replaceAll("\\s+$", "") + NL;
    }

    private static String number(int value) {
        return String.format("%,d", value);
    }

    private static String pad(String text) {
        final String one = text == null ? "" : text;
        return one.length() >= NAME_WIDTH ? one.substring(0, NAME_WIDTH - 1) + " "
                : one + spaces(NAME_WIDTH - one.length());
    }

    /** Right-aligned, so a column of figures can be compared by eye in a fixed-width mail. */
    private static String cell(String text) {
        final String one = text == null ? "" : text;
        return one.length() >= CELL_WIDTH ? one + " "
                : spaces(CELL_WIDTH - one.length()) + one;
    }

    private static String spaces(int count) {
        final StringBuilder ret = new StringBuilder();
        for (int ii = 0; ii < count; ii++) {
            ret.append(' ');
        }
        return ret.toString();
    }
}
