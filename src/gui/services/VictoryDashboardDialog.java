package gui.services;

import control.VictoryStatus;
import control.VictoryStatus.Assessment;
import control.VictoryStatus.Row;
import control.VictoryStatus.State;
import control.WorldControler;
import gui.charts.ChartGauge;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * "Lay of the land" victory dashboard AND graphs hub: reads {@link VictoryStatus} and shows, per active
 * victory condition, whether you are winning, close, contested, or at risk - with a one-line action hint -
 * plus the always-on turn-limit standing. Pass 2 turned it into the graphs hub: a toolbar (NORTH) launches
 * the detailed charts (VP per nation, key cities, VP history, Battle Royale) that used to live on the main
 * window, and a grid of small gauges under the assessment shows how close each active condition is to a win
 * (and, for authoritative conditions with a known rival, to defeat) so they can be compared side by side
 * without a window storm. Read-only, modeless.
 */
public final class VictoryDashboardDialog extends JDialog {

    private static final String GREEN = "#2e7d32";
    private static final String ORANGE = "#e65100";
    private static final String RED = "#c62828";
    private static final String GRAY = "#757575";
    private static final String BLUE = "#1565c0";
    private static final String MUTED = "#9e9e9e";

    private static final int GAUGE_ICON = 18;
    private static final int GAUGE_W = 170;
    private static final int GAUGE_H = 130;

    private final BundleManager labels;
    private final transient WorldControler wc;
    private java.util.List<String> vpFlags = java.util.Collections.emptyList();
    private boolean isTeam;

    private VictoryDashboardDialog(Window parent, BundleManager labels, WorldControler wc, Assessment a) {
        // Modeless: the player can keep the dashboard open while inspecting the data tabs and the map.
        super(parent, ModalityType.MODELESS);
        this.labels = labels;
        this.wc = wc;
        AppIcon.applyTo(this);
        setTitle(titleFor(a.gameId));

        // NORTH: the graphs-hub toolbar - launchers for the detailed charts that used to sit on the main window.
        final JToolBar toolbar = buildToolbar(a);

        // CENTER: the HTML assessment above a grid of small gauges, all inside one scroll pane so a tiny
        // window degrades to a scrollbar rather than clipping.
        final JEditorPane pane = new JEditorPane("text/html", buildHtml(a));
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        final JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        pane.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(pane);
        final JPanel gauges = buildGaugePanel(a);
        if (gauges != null) {
            gauges.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(gauges);
        }

        final JScrollPane scroll = new JScrollPane(center);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(580, 560));

        final JPanel buttons = new JPanel();
        final JButton copy = new JButton(tx("VDASH.COPY", "Copy"));
        copy.addActionListener(e -> ClipboardHelper.copy(buildPlainText(a)));
        final JButton close = new JButton(tx("VDASH.CLOSE", "Close"));
        close.addActionListener(e -> dispose());
        buttons.add(copy);
        buttons.add(close);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);
        pack();
        setLocationRelativeTo(parent);
        pane.setCaretPosition(0);
    }

    public static void show(Component parent, BundleManager labels, WorldControler wc) {
        final Window w = (parent instanceof Window) ? (Window) parent
                : javax.swing.SwingUtilities.getWindowAncestor(parent);
        new VictoryDashboardDialog(w, labels, wc, new VictoryStatus().evaluate()).setVisible(true);
    }

    // ---- toolbar (graphs hub) -------------------------------------------------------------------

    /** The dashboard's own toolbar: the detailed-chart launchers moved off the main window. */
    private JToolBar buildToolbar(Assessment a) {
        final JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setRollover(true);
        if (wc != null) {
            bar.add(toolButton("chart-bar", tx("VDASH.CHART.VP", "Victory points per nation"),
                    e -> wc.showVpPerNationChart()));
            bar.add(toolButton("chart-bar", withHint(tx("VDASH.CHART.KEYCITY", "Key cities per nation"),
                    tx("VDASH.HINT.KEYCITY", "Key cities: a nation's original capital locations.")),
                    e -> wc.showKeyCityChart()));
            bar.add(toolButton("chart-area", tx("VDASH.CHART.HISTORY", "Victory point history (all turns)"),
                    e -> wc.showVpHistoryChart()));
            bar.add(toolButton("chart-radar", tx("VDASH.CHART.POWER", "Nation power comparison (you vs key rivals)"),
                    e -> wc.showNationPowerChart()));
            if (a.battleRoyale) {
                bar.add(toolButton("chart-pie", withHint(tx("VDASH.CHART.BATTLEROYALE", "Battle Royale (domination points)"),
                        tx("VDASH.HINT.DOMINATION", "Domination points: points from a city's size and importance.")),
                        e -> wc.showBattleRoyaleChart()));
            }
        }
        return bar;
    }

    private JButton toolButton(String icon, String tooltip, java.awt.event.ActionListener onClick) {
        final JButton b = new JButton(SvgIcon.themed(icon, GAUGE_ICON));
        b.setToolTipText(tooltip);
        b.setFocusable(false);
        b.addActionListener(onClick);
        return b;
    }

    /** Appends a de-jargon hint to a tooltip so hovering the button explains the game term. */
    private String withHint(String label, String hint) {
        return "<html>" + esc(label) + "<br><i>" + esc(hint) + "</i></html>";
    }

    // ---- gauges ---------------------------------------------------------------------------------

    /**
     * A grid of small gauges, one card per ACTIVE PROGRESS/SURVIVAL/CAPITALS condition. Each card holds a
     * "toward victory" gauge and, where a single rival is known, a "toward defeat" gauge, so conditions can
     * be compared side by side. Returns null when no condition has a meaningful gauge (degrades gracefully).
     */
    private JPanel buildGaugePanel(Assessment a) {
        final List<JPanel> cards = new ArrayList<>();
        for (Row r : a.rows) {
            final JPanel card = gaugeCard(r, a.isTeam);
            if (card != null) {
                cards.add(card);
            }
        }
        if (cards.isEmpty()) {
            return null;
        }
        final JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createEmptyBorder(4, 12, 10, 12));
        final JLabel heading = new JLabel(tx("VDASH.GAUGES", "How close each goal is"));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        wrap.add(heading, BorderLayout.NORTH);
        final int cols = Math.min(2, cards.size());
        final JPanel grid = new JPanel(new GridLayout(0, cols, 6, 6));
        for (JPanel card : cards) {
            grid.add(card);
        }
        wrap.add(grid, BorderLayout.CENTER);
        return wrap;
    }

    /** One condition's gauge card, or null if it has no meaningful gauge (not active / info / turn-limit). */
    private JPanel gaugeCard(Row r, boolean isTeam) {
        if (r.state == State.NOT_ACTIVE
                || r.kind == VictoryStatus.Kind.TURNLIMIT
                || r.kind == VictoryStatus.Kind.INFO) {
            return null;
        }
        final GaugeSpec g = gaugeSpec(r, isTeam);
        if (g == null) {
            return null;
        }
        final JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(java.awt.Color.decode(MUTED)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        final JLabel name = new JLabel(conditionName(r.code), SwingConstants.CENTER);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setToolTipText(conditionHint(r.code));
        card.add(name);

        // One combined dial: needle = my share; amber "defeat" band at the bottom, blue "win" band at top.
        final JPanel p = ChartGauge.buildPanel(String.format("%.0f%%", g.needle), g.needle,
                g.defeatTo, g.winFrom, GAUGE_W, GAUGE_H);
        p.setPreferredSize(new Dimension(GAUGE_W, GAUGE_H));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(p);

        // Readout: the raw gap(s) at a glance.
        final JLabel nums = new JLabel(g.detail, SwingConstants.CENTER);
        nums.setAlignmentX(Component.CENTER_ALIGNMENT);
        nums.setForeground(java.awt.Color.decode(GRAY));
        nums.setFont(nums.getFont().deriveFont(java.awt.Font.PLAIN, 9f));
        card.add(nums);
        return card;
    }

    /** A combined gauge: needle (my share), amber defeat band [0,defeatTo], blue win band [winFrom,100]. */
    private static final class GaugeSpec {
        final double needle;
        final double defeatTo;   // amber upper bound, or -1 for none
        final double winFrom;    // blue lower bound, or -1 for none
        final String detail;     // gap readout

        GaugeSpec(double needle, double defeatTo, double winFrom, String detail) {
            this.needle = needle;
            this.defeatTo = defeatTo;
            this.winFrom = winFrom;
            this.detail = detail;
        }
    }

    /**
     * Needle = my share (matches the text %); win band starts at the threshold's share of the total; defeat
     * band ends at the complementary share (both discount out-of-race barbarians via threshold vs total).
     * The readout carries the raw gap to the win line. "Up" is always good for me (more share / nations /
     * safe capitals). Returns null when the row has no meaningful gauge.
     */
    private GaugeSpec gaugeSpec(Row r, boolean isTeam) {
        final double f = isTeam ? 0.75 : 0.50;
        switch (r.kind) {
            case PROGRESS: {
                if (r.threshold <= 0 || r.total <= 0) {
                    return null;
                }
                final double winFrom = clampPct(100.0 * r.threshold / r.total);   // Score 72, territory 75
                final double defeatTo = clampPct(winFrom * (1 - f) / f);           // Score 24, territory 25
                final int winGap = Math.max(0, r.threshold - r.myValue);
                final String detail = r.rivalKnown
                        ? String.format("you %d (+%d win) · rival +%d", r.myValue, winGap,
                                Math.max(0, r.threshold - r.rivalToWin))
                        : String.format("you %d of %d (+%d win)", r.myValue, r.total, winGap);
                return new GaugeSpec(clampPct(100.0 * r.myValue / r.total), defeatTo, winFrom, detail);
            }
            case SURVIVAL: {
                if (r.total <= 0) {
                    return null;   // nations, no barbarians -> clean 75/25 bands
                }
                final String detail = r.threshold == 0
                        ? String.format("%d of %d nations - supremacy", r.myValue, r.total)
                        : String.format("%d of %d nations · kill %d to win", r.myValue, r.total, r.threshold);
                return new GaugeSpec(clampPct(100.0 * r.myValue / r.total),
                        clampPct((1 - f) * 100.0), clampPct(f * 100.0), detail);
            }
            case CAPITALS: {
                if (r.threshold <= 0) {
                    return null;   // defeat-only: needle = capitals still safe (up = good), no win band
                }
                return new GaugeSpec(clampPct(100.0 * (r.threshold - r.myValue) / r.threshold),
                        clampPct(100.0 / r.threshold), -1.0,
                        String.format("lost %d of %d capitals", r.myValue, r.threshold));
            }
            case DRAGON: {
                if (r.total <= 0) {
                    return null;   // hold ALL to win: needle = your share, win band = the last dragon
                }
                final double winFrom = r.total <= 1 ? 100.0 : clampPct(100.0 * (r.total - 1) / r.total);
                final double defeatTo = r.total <= 1 ? -1.0 : clampPct(100.0 / r.total);
                final String detail = r.myValue >= r.total
                        ? String.format("hold all %d dragons", r.total)
                        : String.format("you %d of %d dragons (+%d win)", r.myValue, r.total, r.total - r.myValue);
                return new GaugeSpec(clampPct(100.0 * r.myValue / r.total), defeatTo, winFrom, detail);
            }
            default:
                return null;
        }
    }

    private double clampPct(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    // ---- HTML rendering -------------------------------------------------------------------------

    private String buildHtml(Assessment a) {
        this.vpFlags = a.vpFlags;
        this.isTeam = a.isTeam;
        final StringBuilder sb = new StringBuilder(2048);
        sb.append("<html><body style='font-family:sans-serif;font-size:10px;'>");

        // headline
        final List<String> pushing = new ArrayList<>();
        final List<String> atRisk = new ArrayList<>();
        for (Row r : a.rows) {
            if (r.kind == VictoryStatus.Kind.INFO || r.kind == VictoryStatus.Kind.TURNLIMIT) {
                continue;
            }
            if (r.state == State.WINNING || r.state == State.CLOSE) {
                pushing.add(conditionName(r.code));
            } else if (r.state == State.AT_RISK || r.state == State.LOSING) {
                atRisk.add(conditionName(r.code));
            }
        }
        sb.append("<h2 style='margin:0 0 6px 0;'>").append(esc(titleFor(a.gameId))).append("</h2>");
        if (!pushing.isEmpty()) {
            sb.append("<p style='margin:2px 0;color:").append(GREEN).append(";'><b>")
                    .append(esc(tx("VDASH.HEAD.WIN", "You can push for victory on:"))).append("</b> ")
                    .append(esc(String.join(", ", pushing))).append("</p>");
        }
        if (!atRisk.isEmpty()) {
            sb.append("<p style='margin:2px 0;color:").append(RED).append(";'><b>")
                    .append(esc(tx("VDASH.HEAD.RISK", "You are at risk on:"))).append("</b> ")
                    .append(esc(String.join(", ", atRisk))).append("</p>");
        }
        if (pushing.isEmpty() && atRisk.isEmpty()) {
            sb.append("<p style='margin:2px 0;color:").append(GRAY).append(";'>")
                    .append(esc(tx("VDASH.HEAD.NONE", "No side is close to a victory condition yet - build your position."))).append("</p>");
        }

        // turn-limit card (always)
        sb.append("<hr>");
        final String metric = a.battleRoyale
                ? tx("VDASH.METRIC.CITY", "city (domination) points")
                : tx("VDASH.METRIC.VP", "victory points");
        sb.append("<p style='margin:4px 0;'><b>").append(esc(tx("VDASH.TURNLIMIT", "Turn limit"))).append("</b> - ");
        sb.append(esc(String.format(tx("VDASH.TURNLIMIT.BODY",
                "the game reaches its limit at turn %s. If no goal is met, the highest %s wins."),
                a.turnoMax, metric)));
        if (a.leaderName != null) {
            sb.append("<br>").append(esc(String.format(tx("VDASH.LEADER", "Current leader: %s (%s)."),
                    a.leaderName, a.leaderValue)));
        }
        sb.append("<br>").append(colorSpan(a.leaderIsMe ? GREEN : (a.myRank <= a.nationCount / 2 ? BLUE : ORANGE),
                esc(String.format(tx("VDASH.YOURANK", "You are #%s of %s by %s (%s)."),
                        a.myRank, a.nationCount, metric, a.myTurnLimitValue))));
        sb.append("</p>");

        // per-condition rows
        sb.append("<hr>");
        boolean anyTerritory = false;
        boolean anyCondition = false;
        for (Row r : a.rows) {
            if (r.kind == VictoryStatus.Kind.TURNLIMIT) {
                continue;
            }
            anyCondition = true;
            anyTerritory |= r.territoryBased;
            sb.append(rowHtml(r));
        }
        if (!anyCondition) {
            sb.append("<p style='color:").append(GRAY).append(";'>")
                    .append(esc(tx("VDASH.NOCONDITIONS", "This game has no special victory goal - it is decided at the turn limit."))).append("</p>");
        }
        if (anyTerritory) {
            sb.append("<hr><p style='color:").append(MUTED).append(";font-size:9px;'>")
                    .append(esc(tx("VDASH.FOG", "Note: opponent territory may be under-counted due to fog of war; victory points are exact.")))
                    .append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String rowHtml(Row r) {
        final StringBuilder sb = new StringBuilder(256);
        final boolean authoritative = r.kind == VictoryStatus.Kind.PROGRESS && r.sides != null;
        final boolean gapKind = r.kind == VictoryStatus.Kind.PROGRESS
                || r.kind == VictoryStatus.Kind.SURVIVAL
                || r.kind == VictoryStatus.Kind.DRAGON;
        sb.append("<p style='margin:6px 0 2px 0;'>");
        sb.append("<b>").append(esc(conditionName(r.code))).append("</b> ");
        // PROGRESS + SURVIVAL rows get an ahead/behind chip; WINNING/LOSING keep the verdict chip, as does
        // NOT_ACTIVE.
        if (gapKind && r.state != State.NOT_ACTIVE && r.state != State.WINNING && r.state != State.LOSING) {
            sb.append(gapChip(r));
        } else {
            sb.append(chip(r.state));
        }
        sb.append("<br><span style='color:").append(GRAY).append(";'>");
        switch (r.kind) {
            case PROGRESS:
                if (r.state == State.NOT_ACTIVE) {
                    sb.append(esc(String.format(tx("VDASH.NOTACTIVE", "Active from turn %s."), r.activeFromTurn)));
                } else if (authoritative) {
                    sb.append(breakdown(r)).append("<br>")
                            .append(esc(String.format(tx("VDASH.WINAT", "Need %s to win (%s of the active total)."),
                                    r.threshold, isTeam ? "3:1" : "over half")));
                } else {
                    // territory: full visible total vs provably-ours; the rest is combined (partly fogged)
                    final String terr = String.format(tx("VDASH.PROGRESS.TERR", "You hold %s of %s (need %s). Others hold %s."),
                            r.myValue, r.total, r.threshold, r.othersValue);
                    final String c = gapColor(gapOf(r));
                    sb.append(c == null ? esc(terr) : colorSpan(c, esc(terr)));
                }
                break;
            case DRAGON:
                if (r.state == State.NOT_ACTIVE) {
                    sb.append(esc(String.format(tx("VDASH.NOTACTIVE", "Active from turn %s."), r.activeFromTurn)));
                } else if (r.total == 0) {
                    sb.append(esc(tx("VDASH.DRAGON.NONE", "No living dragons in play yet. Hold every dragon to win.")));
                } else {
                    final String drg = r.myValue >= r.total
                            ? String.format(tx("VDASH.DRAGON.ALL", "You hold all %s living dragons - Dragonlord!"), r.total)
                            : String.format(tx("VDASH.DRAGON", "You hold %s of %s living dragons. Hold every one to win."),
                                    r.myValue, r.total);
                    final String c = gapColor(gapOf(r));
                    sb.append(c == null ? esc(drg) : colorSpan(c, esc(drg)));
                }
                break;
            case SURVIVAL:
                if (r.state == State.NOT_ACTIVE) {
                    sb.append(esc(String.format(tx("VDASH.NOTACTIVE", "Active from turn %s."), r.activeFromTurn)));
                } else {
                    final String surv;
                    if (r.threshold == 0) {
                        surv = String.format(tx("VDASH.SURVIVAL.WIN", "You hold supremacy now: %s of %s active nations."),
                                r.myValue, r.total);
                    } else {
                        // you can't gain nations - it's an elimination race
                        surv = String.format(tx("VDASH.SURVIVAL", "You %s nations vs %s enemy. Eliminate %s more enemy nation(s), keeping yours, to win."),
                                r.myValue, r.othersValue, r.threshold);
                    }
                    final String c = gapColor(gapOf(r));
                    sb.append(c == null ? esc(surv) : colorSpan(c, esc(surv)));
                }
                break;
            case CAPITALS:
                if (r.state == State.NOT_ACTIVE) {
                    sb.append(esc(String.format(tx("VDASH.NOTACTIVE", "Active from turn %s."), r.activeFromTurn)));
                } else {
                    sb.append(esc(String.format(tx("VDASH.CAPITALS", "Your team has lost %s of %s capitals."),
                            r.myValue, r.threshold)));
                }
                break;
            case INFO:
                sb.append(esc(r.state == State.NOT_ACTIVE
                        ? String.format(tx("VDASH.NOTACTIVE", "Active from turn %s."), r.activeFromTurn)
                        : conditionInfo(r.code)));
                break;
            default:
                break;
        }
        sb.append("</span><br><span style='color:").append(BLUE).append(";'>&#9656; ")
                .append(esc(conditionAction(r.code))).append("</span></p>");
        return sb.toString();
    }

    private String chip(State s) {
        final String color, text;
        switch (s) {
            case WINNING: color = GREEN; text = tx("VDASH.STATE.WINNING", "you meet this now!"); break;
            case CLOSE: color = GREEN; text = tx("VDASH.STATE.CLOSE", "close - push!"); break;
            case AT_RISK: color = ORANGE; text = tx("VDASH.STATE.ATRISK", "at risk - act!"); break;
            case LOSING: color = RED; text = tx("VDASH.STATE.LOSING", "opponents meet this now"); break;
            case CONTESTED: color = GRAY; text = tx("VDASH.STATE.CONTESTED", "contested"); break;
            case NOT_ACTIVE: color = MUTED; text = tx("VDASH.STATE.NOTACTIVE", "not yet active"); break;
            default: return "";
        }
        return "<span style='color:" + color + ";'>[" + esc(text) + "]</span>";
    }

    private String colorSpan(String color, String text) {
        return "<span style='color:" + color + ";'>" + text + "</span>";
    }

    /** Per-side "name value (pct%)" breakdown; my side bold + coloured by my lead/deficit vs the top rival. */
    private String breakdown(Row r) {
        final int total = r.total <= 0 ? 1 : r.total;
        final double myPct = 100.0 * r.myValue / total;
        final double rivalPct = 100.0 * r.othersValue / total;
        final String mineColor = gapColor(myPct - rivalPct);
        final StringBuilder sb = new StringBuilder(128);
        boolean first = true;
        for (VictoryStatus.Side s : r.sides) {
            if (!first) {
                sb.append(" &middot; ");
            }
            first = false;
            final String txt = String.format("%s %s (%.0f%%)", esc(sideName(s.name)), s.value, 100.0 * s.value / total);
            if (s.mine) {
                sb.append("<b>").append(mineColor == null ? txt : colorSpan(mineColor, txt)).append("</b>");
            } else {
                sb.append(txt);
            }
        }
        return sb.toString();
    }

    /** My lead/deficit in percentage points: (mine - others) / total, over the display total. */
    private double gapOf(Row r) {
        final int total = r.total <= 0 ? 1 : r.total;
        return 100.0 * r.myValue / total - 100.0 * r.othersValue / total;
    }

    /** Ahead/behind chip using the bands: >=8% green, >=4% blue, <4% neutral, <=-4% amber, <=-8% red. */
    private String gapChip(Row r) {
        final double gap = gapOf(r);
        final int mag = (int) Math.round(Math.abs(gap));
        final String color = gapColor(gap);
        final String text;
        if (gap >= 4) {
            text = String.format(tx("VDASH.GAP.AHEAD", "ahead by %s%%"), mag);
        } else if (gap <= -4) {
            text = String.format(tx("VDASH.GAP.BEHIND", "behind by %s%%"), mag);
        } else {
            text = tx("VDASH.GAP.EVEN", "even");
        }
        return "<span style='color:" + (color == null ? GRAY : color) + ";'>[" + esc(text) + "]</span>";
    }

    /** null = default/neutral colour. */
    private String gapColor(double gap) {
        if (gap >= 8) {
            return GREEN;
        }
        if (gap >= 4) {
            return BLUE;
        }
        if (gap > -4) {
            return null;
        }
        if (gap > -8) {
            return ORANGE;
        }
        return RED;
    }

    private String sideName(String key) {
        return "-".equals(key) ? tx("VDASH.SIDE.NEUTRAL", "barbarians/AI") : key;
    }

    // ---- plain-text (clipboard) -----------------------------------------------------------------

    /** Full plain-text mirror of the dialog (headline, turn limit, every condition + action, fog note). */
    private String buildPlainText(Assessment a) {
        final StringBuilder sb = new StringBuilder(2048);
        sb.append(titleFor(a.gameId)).append('\n');
        sb.append(String.format(tx("VDASH.TURN", "Turn %s of %s"), a.turno, a.turnoMax)).append("\n\n");

        // headline
        final List<String> pushing = new ArrayList<>();
        final List<String> atRisk = new ArrayList<>();
        for (Row r : a.rows) {
            if (r.kind == VictoryStatus.Kind.INFO || r.kind == VictoryStatus.Kind.TURNLIMIT) {
                continue;
            }
            if (r.state == State.WINNING || r.state == State.CLOSE) {
                pushing.add(conditionName(r.code));
            } else if (r.state == State.AT_RISK || r.state == State.LOSING) {
                atRisk.add(conditionName(r.code));
            }
        }
        if (!pushing.isEmpty()) {
            sb.append(tx("VDASH.HEAD.WIN", "You can push for victory on:")).append(' ')
                    .append(String.join(", ", pushing)).append('\n');
        }
        if (!atRisk.isEmpty()) {
            sb.append(tx("VDASH.HEAD.RISK", "You are at risk on:")).append(' ')
                    .append(String.join(", ", atRisk)).append('\n');
        }
        if (pushing.isEmpty() && atRisk.isEmpty()) {
            sb.append(tx("VDASH.HEAD.NONE", "No side is close to a victory condition yet - build your position.")).append('\n');
        }
        sb.append('\n');

        // turn limit
        final String metric = a.battleRoyale
                ? tx("VDASH.METRIC.CITY", "city (domination) points")
                : tx("VDASH.METRIC.VP", "victory points");
        sb.append(tx("VDASH.TURNLIMIT", "Turn limit")).append(" - ")
                .append(String.format(tx("VDASH.TURNLIMIT.BODY",
                        "the game reaches its limit at turn %s. If no goal is met, the highest %s wins."),
                        a.turnoMax, metric)).append('\n');
        if (a.leaderName != null) {
            sb.append("  ").append(String.format(tx("VDASH.LEADER", "Current leader: %s (%s)."),
                    a.leaderName, a.leaderValue)).append('\n');
        }
        sb.append("  ").append(String.format(tx("VDASH.YOURANK", "You are #%s of %s by %s (%s)."),
                a.myRank, a.nationCount, metric, a.myTurnLimitValue)).append("\n\n");

        // conditions
        boolean anyTerritory = false;
        for (Row r : a.rows) {
            if (r.kind == VictoryStatus.Kind.TURNLIMIT) {
                continue;
            }
            anyTerritory |= r.territoryBased;
            sb.append(conditionName(r.code)).append(" [").append(statusText(r)).append("]\n");
            final String detail = detailPlain(r);
            if (!detail.isEmpty()) {
                sb.append("  ").append(detail).append('\n');
            }
            final String action = conditionAction(r.code);
            if (!action.isEmpty()) {
                sb.append("  -> ").append(action).append('\n');
            }
            sb.append('\n');
        }
        if (anyTerritory) {
            sb.append(tx("VDASH.FOG", "Note: opponent territory may be under-counted due to fog of war; victory points are exact."));
        }
        return sb.toString();
    }

    /** Plain-text status: ahead/behind for gap rows, else the verdict word. */
    private String statusText(Row r) {
        final boolean gapKind = r.kind == VictoryStatus.Kind.PROGRESS
                || r.kind == VictoryStatus.Kind.SURVIVAL
                || r.kind == VictoryStatus.Kind.DRAGON;
        if (r.state != State.NOT_ACTIVE && r.state != State.WINNING && r.state != State.LOSING && gapKind) {
            final double gap = gapOf(r);
            final int mag = (int) Math.round(Math.abs(gap));
            if (gap >= 4) {
                return String.format(tx("VDASH.GAP.AHEAD", "ahead by %s%%"), mag);
            }
            if (gap <= -4) {
                return String.format(tx("VDASH.GAP.BEHIND", "behind by %s%%"), mag);
            }
            return tx("VDASH.GAP.EVEN", "even");
        }
        switch (r.state) {
            case WINNING: return tx("VDASH.STATE.WINNING", "you meet this now!");
            case LOSING: return tx("VDASH.STATE.LOSING", "opponents meet this now");
            case AT_RISK: return tx("VDASH.STATE.ATRISK", "at risk - act!");
            case CLOSE: return tx("VDASH.STATE.CLOSE", "close - push!");
            case NOT_ACTIVE: return tx("VDASH.STATE.NOTACTIVE", "not yet active");
            case INFO: return tx("VDASH.STATE.INFO", "info");
            default: return tx("VDASH.STATE.CONTESTED", "contested");
        }
    }

    /** Plain-text detail line matching the HTML per-kind rendering. */
    private String detailPlain(Row r) {
        if (r.state == State.NOT_ACTIVE) {
            return String.format(tx("VDASH.NOTACTIVE", "Active from turn %s."), r.activeFromTurn);
        }
        switch (r.kind) {
            case PROGRESS:
                if (r.sides != null) {
                    final int total = r.total <= 0 ? 1 : r.total;
                    final List<String> parts = new ArrayList<>();
                    for (VictoryStatus.Side s : r.sides) {
                        parts.add(String.format("%s %s (%.0f%%)", sideName(s.name), s.value, 100.0 * s.value / total));
                    }
                    return String.join(", ", parts) + " | "
                            + String.format(tx("VDASH.WINAT", "Need %s to win (%s of the active total)."),
                                    r.threshold, isTeam ? "3:1" : "over half");
                }
                return String.format(tx("VDASH.PROGRESS.TERR", "You hold %s of %s (need %s). Others hold %s."),
                        r.myValue, r.total, r.threshold, r.othersValue);
            case SURVIVAL:
                return r.threshold == 0
                        ? String.format(tx("VDASH.SURVIVAL.WIN", "You hold supremacy now: %s of %s active nations."), r.myValue, r.total)
                        : String.format(tx("VDASH.SURVIVAL", "You %s nations vs %s enemy. Eliminate %s more enemy nation(s), keeping yours, to win."),
                                r.myValue, r.othersValue, r.threshold);
            case CAPITALS:
                return String.format(tx("VDASH.CAPITALS", "Your team has lost %s of %s capitals."), r.myValue, r.threshold);
            case DRAGON:
                if (r.total == 0) {
                    return tx("VDASH.DRAGON.NONE", "No living dragons in play yet. Hold every dragon to win.");
                }
                return r.myValue >= r.total
                        ? String.format(tx("VDASH.DRAGON.ALL", "You hold all %s living dragons - Dragonlord!"), r.total)
                        : String.format(tx("VDASH.DRAGON", "You hold %s of %s living dragons. Hold every one to win."),
                                r.myValue, r.total);
            case INFO:
                return conditionInfo(r.code);
            default:
                return "";
        }
    }

    // ---- condition text -------------------------------------------------------------------------

    private String conditionName(String code) {
        switch (code) {
            case ";VSP;": return tx("VDASH.NAME.VSP", "Score (victory points)");
            case ";VSC;": return tx("VDASH.NAME.VSC", "Conquest (burghs & metropolises)");
            case ";VCP;": return tx("VDASH.NAME.VCP", "Battle Royale (city points)");
            case ";VSK;": return tx("VDASH.NAME.VSK", "Domination (key cities)");
            case ";VSS;": return tx("VDASH.NAME.VSS", "Supremacy (nations)");
            case ";VKC;": return tx("VDASH.NAME.VKC", "Capitals");
            case ";VSD;": return tx("VDASH.NAME.VSD", "First blood");
            case ";VKK;": return tx("VDASH.NAME.VKK", "King of Kings");
            case ";VDL;": return tx("VDASH.NAME.VDL", "Dragonlord (dragons)");
            default: return code;
        }
    }

    /** De-jargon tooltip for a condition name (game terms explained in plain words); null = no hint. */
    private String conditionHint(String code) {
        switch (code) {
            case ";VSK;": return tx("VDASH.HINT.KEYCITY", "Key cities: a nation's original capital locations.");
            case ";VCP;": return tx("VDASH.HINT.DOMINATION", "Domination points: points from a city's size and importance.");
            default: return null;
        }
    }

    private String conditionAction(String code) {
        switch (code) {
            case ";VSP;": return vpAction();
            case ";VSC;": return tx("VDASH.ACT.VSC", "Take and hold burghs and metropolises.");
            case ";VCP;": return tx("VDASH.ACT.VCP", "Control high-value cities (domination points).");
            case ";VSK;": return tx("VDASH.ACT.VSK", "Hold the original capitals (key cities).");
            case ";VSS;": return tx("VDASH.ACT.VSS", "Eliminate enemy nations while keeping your own alive.");
            case ";VKC;": return tx("VDASH.ACT.VKC", "Protect your capitals - do not let them fall.");
            case ";VSD;": return tx("VDASH.ACT.VSD", "Do not be the first to lose a nation; strike a weak rival.");
            case ";VKK;": return tx("VDASH.ACT.VKK", "Outlast every rival alliance.");
            case ";VDL;": return tx("VDASH.ACT.VDL", "Hold every living dragon - capture, bond, or hatch them.");
            default: return "";
        }
    }

    /** Score action: name only the VP components active for THIS game (never suggests non-scoring effort). */
    private String vpAction() {
        if (vpFlags.isEmpty()) {
            return tx("VDASH.ACT.VSP", "Grow your victory points.");
        }
        final List<String> parts = new ArrayList<>();
        for (String flag : vpFlags) {
            parts.add(vpComponentName(flag));
        }
        return String.format(tx("VDASH.ACT.VSP.LIST", "Victory points come from: %s."), String.join(", ", parts));
    }

    private String vpComponentName(String flag) {
        switch (flag) {
            case ";VCF;": return tx("VDASH.VP.VCF", "cities & fortifications");
            case ";VAN;": return tx("VDASH.VP.VAN", "armies & navies");
            case ";VCH;": return tx("VDASH.VP.VCH", "characters & skills");
            case ";VIM;": return tx("VDASH.VP.VIM", "magic items");
            case ";VID;": return tx("VDASH.VP.VID", "dragon master (dragons held)");
            case ";VGR;": return tx("VDASH.VP.VGR", "gold & resources");
            default: return flag;
        }
    }

    private String conditionInfo(String code) {
        switch (code) {
            case ";VSD;": return tx("VDASH.INFO.VSD", "Sudden death: the game ends the moment any nation is eliminated.");
            case ";VKK;": return tx("VDASH.INFO.VKK", "The game ends when only one alliance remains.");
            default: return "";
        }
    }

    private String titleFor(int gameId) {
        return String.format(tx("VDASH.TITLE.FOR", "%s for Game %s"),
                tx("VDASH.TITLE", "Victory dashboard - the lay of the land"), gameId);
    }

    private String tx(String key, String fallback) {
        final String s = labels.getString(key);
        return (s == null || s.startsWith("N/A (Missing Translation")) ? fallback : s;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
