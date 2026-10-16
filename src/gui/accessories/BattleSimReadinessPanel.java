package gui.accessories;

import business.combat.CombatLayer;
import business.combat.CombatScenario;
import control.services.BattleSimConverter;
import control.services.BattleSimReadiness;
import gui.services.SvgIcon;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import persistenceCommons.BundleManager;

/** Compact initial eligibility and assumption indicators; all explanations are click-through. */
public final class BattleSimReadinessPanel extends JPanel {
    public enum Topic { NAVY, ARMY, CITY, MORALE, TROOPS, DIPLOMACY, GATE }

    private final BundleManager labels;
    private final EnumMap<Topic, JButton> buttons = new EnumMap<>(Topic.class);

    public BattleSimReadinessPanel(BundleManager labels, Consumer<Topic> open) {
        super(new FlowLayout(FlowLayout.TRAILING, 2, 0));
        this.labels = labels;
        addBadge(Topic.NAVY, "sailboat", initial(BattleSimConverter.getLayerName(CombatLayer.NAVY)), open);
        add(new JLabel(" > "));
        addBadge(Topic.ARMY, "swords", initial(BattleSimConverter.getLayerName(CombatLayer.ARMY)), open);
        add(new JLabel(" > "));
        addBadge(Topic.CITY, "building-castle", initial(BattleSimConverter.getLayerName(CombatLayer.CITY)), open);
        addBadge(Topic.MORALE, "heart", initial(labels.getString("MORAL")), open);
        addBadge(Topic.TROOPS, "users", initial(labels.getString("BATTLESIM.PLATOON.TITLE")), open);
        addBadge(Topic.DIPLOMACY, "chart-radar", initial(labels.getString("BATTLESIM.DIPLOMACY")), open);
        addBadge(Topic.GATE, "help", "?", open);
    }

    private static String initial(String label) {
        return label.isEmpty() ? "?" : label.substring(0, 1).toUpperCase(java.util.Locale.getDefault());
    }

    private void addBadge(Topic topic, String asset, String fallback, Consumer<Topic> open) {
        final JButton button = new JButton(icon(asset, fallback));
        button.putClientProperty("BattleSim.readiness", topic.name());
        button.setMargin(new Insets(2, 4, 2, 4));
        button.addActionListener(event -> open.accept(topic));
        buttons.put(topic, button);
        add(button);
    }

    /** Missing assets must remain usable rather than depending on a platform's emoji font. */
    static Icon icon(String asset, String fallback) {
        if (BattleSimReadinessPanel.class.getResource("/images/icons/" + asset + ".svg") != null) {
            return SvgIcon.themed(asset, 16);
        }
        return new Icon() {
            public int getIconWidth() { return 16; }
            public int getIconHeight() { return 16; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                graphics.setColor(component.getForeground());
                graphics.drawString(fallback, x + 2, y + 13);
            }
        };
    }

    public void refresh(CombatScenario scenario) {
        final BattleSimReadiness facts = BattleSimReadiness.of(scenario);
        for (CombatLayer layer : CombatLayer.values()) {
            final Topic topic = Topic.valueOf(layer.name());
            final int count = facts.participants(layer).size();
            final JButton button = buttons.get(topic);
            button.setText(count > 0 ? (button.getFont().canDisplay('\u2713') ? "\u2713" : "+") : "-");
            describe(button, String.format(labels.getString("BATTLESIM.READINESS.LAYER"),
                    BattleSimConverter.getLayerName(layer), count)
                    + " " + labels.getString("BATTLESIM.READINESS.LAYER.HINT"));
        }
        warning(Topic.MORALE, facts.unknownMorale().size());
        warning(Topic.TROOPS, facts.composition().size());
        warning(Topic.DIPLOMACY, facts.assumedDirections().size());
        final JButton gate = buttons.get(Topic.GATE);
        gate.setIcon(icon(facts.gate().isRunnable() ? "check" : "help", facts.gate().isRunnable() ? "+" : "!"));
        gate.setText(facts.gate().isRunnable() ? "" : "!");
        describe(gate, facts.gate().isRunnable() ? labels.getString("BATTLESIM.READINESS.READY")
                : scenario == null ? labels.getString("BATTLESIM.RUN.DISABLED.NO_ARMIES")
                        : BattleSimConverter.getRunDisabledReason(scenario));
        revalidate();
    }

    private void warning(Topic topic, int count) {
        final JButton button = buttons.get(topic);
        button.setVisible(count > 0);
        button.setText("? " + count);
        describe(button, String.format(labels.getString("BATTLESIM.READINESS." + topic), count));
    }

    private static void describe(JButton button, String text) {
        button.setToolTipText(text);
        button.getAccessibleContext().setAccessibleName(text);
        button.getAccessibleContext().setAccessibleDescription(text);
    }
}
