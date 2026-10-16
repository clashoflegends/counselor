package gui.accessories;

import business.combat.CombatLayer;
import control.services.BattleSimRunSnapshot;
import control.services.BattleSimVariants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/** Comparison reads frozen pins only; it never reaches a producing scenario or result. */
final class BattleSimComparisonDialog extends JDialog {
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final BattleSimVariants.Family family;
    private final JComboBox<String> baseline = new JComboBox<>();
    private final JComboBox<String> reportChoice = new JComboBox<>();
    private final JTextArea report = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton remove = new JButton(text("REMOVE"));
    private final JLabel baselineNotice = new JLabel();
    private List<BattleSimRunSnapshot> pins = List.of();
    private boolean refreshing;
    private final Runnable onChanged = this::refresh;

    BattleSimComparisonDialog(Frame owner, BattleSimVariants.Family family) {
        super(owner, text("COMPARE"), false);
        this.family = family;
        family.listen(onChanged);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final JPanel top = new JPanel(new BorderLayout(4, 4));
        final JTextArea hint = new JTextArea(text("SESSION"));
        hint.setEditable(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setOpaque(false);
        top.add(hint, BorderLayout.NORTH);
        final JPanel controls = new JPanel(new gui.services.WrapLayout(FlowLayout.LEADING, 4, 4));
        controls.add(new JLabel(text("BASELINE")));
        baseline.getAccessibleContext().setAccessibleName(text("BASELINE"));
        baseline.setPreferredSize(new Dimension(360, baseline.getPreferredSize().height));
        controls.add(baseline);
        controls.add(remove);
        controls.add(baselineNotice);
        remove.addActionListener(event -> {
            if (baseline.getSelectedIndex() >= 0) {
                family.remove(baseline.getSelectedIndex());
            }
        });
        baseline.addActionListener(event -> { if (!refreshing) rebuild(); });
        top.add(controls, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        final JTextArea unitsHint = new JTextArea(text("UNITS"));
        unitsHint.setEditable(false);
        unitsHint.setLineWrap(true);
        unitsHint.setWrapStyleWord(true);
        unitsHint.setRows(2);
        unitsHint.setOpaque(false);
        add(unitsHint, BorderLayout.SOUTH);
        report.setEditable(false);
        report.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        report.getAccessibleContext().setAccessibleName(text("REPORT"));
        reportChoice.getAccessibleContext().setAccessibleName(text("REPORT"));
        reportChoice.addActionListener(event -> updateReport());
        final Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1160, screen.width * 9 / 10), Math.min(700, screen.height * 8 / 10));
        setLocationRelativeTo(owner);
        refresh();
    }

    void refresh() {
        final int selected = baseline.getSelectedIndex();
        final int selectedReport = reportChoice.getSelectedIndex();
        final java.util.UUID baselineId = selected < 0 || selected >= pins.size() ? null : pins.get(selected).runId();
        final java.util.UUID reportId = selectedReport < 0 || selectedReport >= pins.size() ? null : pins.get(selectedReport).runId();
        refreshing = true;
        pins = family.pins();
        baseline.removeAllItems();
        reportChoice.removeAllItems();
        for (int i = 0; i < pins.size(); i++) {
            final var pin = pins.get(i);
            final String title = (i + 1) + ". " + pin.name() + " - " + pin.time();
            baseline.addItem(title);
            reportChoice.addItem(title);
        }
        if (!pins.isEmpty()) {
            baseline.setSelectedIndex(indexOf(baselineId));
            reportChoice.setSelectedIndex(indexOf(reportId));
            baseline.setToolTipText(baselineId != null && pins.stream().noneMatch(pin -> pin.runId().equals(baselineId))
                    ? text("BASELINE.REMOVED") : null);
            baselineNotice.setText(baseline.getToolTipText());
        }
        remove.setEnabled(!pins.isEmpty());
        refreshing = false;
        rebuild();
    }

    private int indexOf(java.util.UUID id) {
        for (int i = 0; i < pins.size(); i++) if (pins.get(i).runId().equals(id)) return i;
        return 0;
    }
    @Override public void dispose() {
        family.unlisten(onChanged);
        super.dispose();
    }

    private void rebuild() {
        final int tab = tabs.getSelectedIndex();
        tabs.removeAll();
        if (pins.isEmpty()) {
            tabs.addTab(text("SUMMARY"), new JLabel(text("EMPTY")));
            return;
        }
        final List<Map<String, String>> summaries = new ArrayList<>();
        final Map<String, String> names = new LinkedHashMap<>();
        for (BattleSimRunSnapshot pin : pins) summaries.add(summary(pin, names));
        tabs.addTab(text("SUMMARY"), table(summaries, names, false));
        final List<Map<String, String>> inputs = new ArrayList<>();
        final Map<String, String> inputNames = new LinkedHashMap<>();
        for (BattleSimRunSnapshot pin : pins) {
            final Map<String, String> values = new LinkedHashMap<>();
            pin.inputs().forEach((key, value) -> {
                inputNames.putIfAbsent(key, value.label());
                values.put(key, value.value());
            });
            inputs.add(values);
        }
        tabs.addTab(text("CHANGES"), table(inputs, inputNames, true));
        final JPanel full = new JPanel(new BorderLayout());
        full.add(reportChoice, BorderLayout.NORTH);
        full.add(new JScrollPane(report), BorderLayout.CENTER);
        tabs.addTab(text("REPORT"), full);
        if (tab >= 0) tabs.setSelectedIndex(Math.min(tab, tabs.getTabCount() - 1));
        updateReport();
    }

    private JScrollPane table(List<Map<String, String>> values, Map<String, String> names, boolean changes) {
        final String[] columns = new String[pins.size() + 1];
        columns[0] = changes ? text("CHANGES") : text("SUMMARY");
        for (int i = 0; i < pins.size(); i++) columns[i + 1] = (i + 1) + ". " + pins.get(i).name();
        final DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        final int base = Math.max(0, baseline.getSelectedIndex());
        final var keys = new LinkedHashSet<String>();
        values.forEach(value -> keys.addAll(value.keySet()));
        for (String key : keys) {
            final String original = values.get(base).get(key);
            if (changes && values.stream().allMatch(value -> Objects.equals(original, value.get(key)))) continue;
            final Object[] row = new Object[columns.length];
            row[0] = names.get(key);
            for (int i = 0; i < values.size(); i++) {
                final String value = values.get(i).get(key);
                row[i + 1] = value == null ? text("REMOVED")
                        : original == null && i != base ? text("ADDED") + ": " + value : value;
            }
            model.addRow(row);
        }
        final JTable table = new JTable(model) {
            @Override public String getToolTipText(java.awt.event.MouseEvent event) {
                final int row = rowAtPoint(event.getPoint()), column = columnAtPoint(event.getPoint());
                return row < 0 || column < 0 ? null : String.valueOf(getValueAt(row, column));
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(Math.max(24, table.getRowHeight() + 6));
        for (int i = 0; i < columns.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 320 : 260);
        table.getAccessibleContext().setAccessibleName(changes ? text("CHANGES") : text("SUMMARY"));
        return new JScrollPane(table);
    }

    private static Map<String, String> summary(BattleSimRunSnapshot pin, Map<String, String> names) {
        final Map<String, String> ret = new LinkedHashMap<>();
        put(ret, names, "snapshot", text("SNAPSHOT"), pin.game() + " / " + pin.turn() + " / " + pin.time());
        put(ret, names, "city", labels.getString("BATTLESIM.CITY.TITLE"), pin.city());
        for (CombatLayer layer : CombatLayer.values()) put(ret, names, "rounds/" + layer,
                labels.getString("BATTLESIM.LAYER." + layer), String.format(labels.getString("BATTLESIM.RESULTS.ROUNDS"), pin.rounds().get(layer)));
        for (var army : pin.armies()) {
            final String id = army.id().toString();
            final String title = army.nation() + " / " + army.name() + " [" + id.substring(0, 8) + "]";
            put(ret, names, id + "/soldiers", title + " / " + text("SOLDIERS"), units(army.soldiers()));
            put(ret, names, id + "/ships", title + " / " + labels.getString("NAVIOS"), units(army.ships()));
            for (CombatLayer layer : CombatLayer.values()) put(ret, names, id + "/" + layer,
                    title + " / " + labels.getString("BATTLESIM.LAYER." + layer), army.outcomes().get(layer));
        }
        return ret;
    }

    private static void put(Map<String, String> values, Map<String, String> names, String key, String label, String value) {
        values.put(key, value);
        names.putIfAbsent(key, label);
    }
    private static String units(BattleSimRunSnapshot.Units units) {
        return units.before() + " / " + (units.after() == null ? "--" : units.after()) + " / "
                + (units.lost() == null ? "--" : units.lost()) + (units.unreported() > 0 ? "; ? " + units.unreported() : "");
    }
    private void updateReport() {
        final int index = reportChoice.getSelectedIndex();
        report.setText(index >= 0 && index < pins.size() ? pins.get(index).report() : "");
        report.setCaretPosition(0);
    }
    private static String text(String key) { return labels.getString("BATTLESIM.VARIANT." + key); }
}
