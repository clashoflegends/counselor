package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.combat.ScenarioDefaults;
import business.combat.ScenarioRoster;
import business.facade.ExercitoFacade;
import control.BattleSimControler;
import control.services.BattleSimConverter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/** Grouped roster with stable identities; presentation preferences never enter the scenario. */
final class BattleSimRosterTable extends JTable {
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final String CONFIG = "battleSimRosterLayout";
    private static final String[] IDS = {"name", "layers", "count", "morale", "land", "sea"};
    private static final String[] KEYS = {"BATTLESIM.ARMY.TITLE", "BATTLESIM.ROSTER.LAYERS", "BATTLESIM.ROSTER.COUNT",
        "MORAL", "BATTLESIM.ROSTER.LAND", "BATTLESIM.ROSTER.SEA"};
    private static final int[] DEFAULT_WIDTHS = {128, 56, 100, 60, 110, 110};
    private record Row(ArmySim army, String group, String[] cells, String hint, boolean unidentified) { }
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Integer> widths = new LinkedHashMap<>();
    private final List<String> visible = new ArrayList<>();
    private final BattleSimControler controller;
    private final Predicate<ArmySim> selection;
    private final Runnable densityChanged;
    private boolean updating;
    private boolean compact;
    private TableColumn resizing;
    private final RosterModel data = new RosterModel();

    BattleSimRosterTable(BattleSimControler controller, Predicate<ArmySim> selection, Runnable densityChanged) {
        this.controller = controller;
        this.selection = selection;
        this.densityChanged = densityChanged;
        setAutoCreateColumnsFromModel(false);
        setModel(data);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        setFillsViewportHeight(true);
        setShowGrid(false);
        getTableHeader().setReorderingAllowed(false);
        getAccessibleContext().setAccessibleName(labels.getString("BATTLESIM.ARMIES.TITLE"));
        defaults();
        restore(SettingsManager.getInstance().getConfig(CONFIG, ""));
        rebuildColumns();
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                final Row one = rows.get(row);
                setFont(table.getFont().deriveFont(one.army == null ? Font.BOLD : Font.PLAIN));
                setHorizontalAlignment(convertColumnIndexToModel(column) == 0 ? SwingConstants.LEADING : SwingConstants.TRAILING);
                setToolTipText(one.hint);
                return this;
            }
        });
        getSelectionModel().addListSelectionListener(event -> {
            if (updating || event.getValueIsAdjusting()) return;
            final int row = getSelectedRow();
            if (row < 0 || row >= rows.size()) return;
            if (rows.get(row).army == null) { selectArmy(controller.getSelected()); return; }
            if (!selection.test(rows.get(row).army)) selectArmy(controller.getSelected());
        });
        getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { resizing = getTableHeader().getResizingColumn(); }
            @Override public void mouseReleased(MouseEvent event) {
                if (resizing != null) {
                    final int model = resizing.getModelIndex();
                    final int width = clamp(IDS[model], resizing.getWidth());
                    widths.put(IDS[model], width);
                    resizing.setPreferredWidth(width);
                    resizing = null;
                    save();
                }
            }
        });
    }

    void refresh() {
        updating = true;
        try {
            rows.clear();
            final CombatScenario scenario = controller.getScenario();
            final var grouped = ScenarioRoster.of(scenario);
            final var participation = scenario.getParticipation();
            final var facade = new ExercitoFacade();
            for (var nation : grouped.getNacoes()) {
                final String group = BattleSimConverter.getNacaoTitle(grouped, nation);
                rows.add(new Row(null, group, new String[]{group, "", "", "", "", ""}, group, false));
                for (ArmySim army : grouped.getArmies(nation)) {
                    long soldiers = 0, ships = 0;
                    boolean placeholder = false, unknownLand = false, unknownSea = false;
                    for (var platoon : army.getPelotoes().values()) {
                        if (platoon.getTipoTropa() != null && platoon.getTipoTropa().isBarcos()) ships += platoon.getQtd();
                        else soldiers += platoon.getQtd();
                        if (platoon.getQtd() > 0 && ScenarioDefaults.isPlaceholder(platoon.getTipoTropa())) {
                            placeholder = true;
                            if (platoon.getTipoTropa() != null && platoon.getTipoTropa().isBarcos()) unknownSea = true;
                            else unknownLand = true;
                        }
                    }
                    final boolean unknown = placeholder || soldiers + ships == 0;
                    final String size = army.getSizeBand() + (army.getSizeBandLand().isEmpty()
                            || army.getSizeBandLand().equals(army.getSizeBand()) ? "" : " / " + army.getSizeBandLand());
                    final String title = scenario.getDisplayName(army) + (unknown ? " (?) " + size : "");
                    final String[] cells = {title,
                        BattleSimConverter.getLayerMarks(army, participation.get(army), controller.getLastResult()),
                        soldiers + ships == 0 ? "?" : String.format("%,d / %,d%s", soldiers, ships, placeholder ? " (?)" : ""),
                        scenario.isMoraleUnknown(army) ? "?" : Integer.toString(army.getMoral()),
                        unknownLand || soldiers + ships == 0 ? "?" : String.format("%,d / %,d", facade.getAtaqueExercito(army, false), facade.getDefesaExercito(army, false)),
                        unknownSea || soldiers + ships == 0 ? "?" : String.format("%,d / %,d", facade.getAtaqueExercito(army, true), facade.getDefesaExercito(army, true))};
                    final String hint = group + " | " + title + " | " + labels.getString("BATTLESIM.ROSTER.COUNT")
                            + ": " + cells[2] + " | " + BattleSimConverter.getLayerHint(army, participation.get(army), controller.getLastResult()).replaceAll("<[^>]+>", " ")
                            + " | " + labels.getString("BATTLESIM.PROVENANCE." + scenario.getProvenance(army));
                    rows.add(new Row(army, group, cells, hint, unknown));
                }
            }
            data.fireTableDataChanged();
            applyDensity();
            selectArmy(controller.getSelected());
        } finally { updating = false; }
    }

    void selectArmy(ArmySim army) {
        final boolean previous = updating;
        updating = true;
        try {
            clearSelection();
            for (int row = 0; row < rows.size(); row++) if (rows.get(row).army == army && army != null) {
                setRowSelectionInterval(row, row); break;
            }
        } finally { updating = previous; }
    }
    void scrollToSelection() {
        if (getSelectedRow() >= 0) scrollRectToVisible(getCellRect(getSelectedRow(), 0, true));
    }
    @Override public void changeSelection(int row, int column, boolean toggle, boolean extend) {
        if (!rows.isEmpty()) {
            final int step = row == 0 ? 1 : row < getSelectedRow() ? -1 : 1;
            while (row >= 0 && row < rows.size() && rows.get(row).army == null) row += step;
            if (row < 0 || row >= rows.size()) return;
        }
        super.changeSelection(row, column, toggle, extend);
    }
    @Override public boolean getScrollableTracksViewportWidth() {
        if (getParent() == null) return true;
        if (visible.size() == 3 && visible.containsAll(List.of("name", "layers", "count"))
                && widths.get("name") == DEFAULT_WIDTHS[0] && widths.get("layers") == DEFAULT_WIDTHS[1]
                && widths.get("count") == DEFAULT_WIDTHS[2]) return true;
        int width = 0;
        for (int i = 0; i < getColumnCount(); i++) width += getColumnModel().getColumn(i).getPreferredWidth();
        return width <= getParent().getWidth();
    }
    @Override public String getToolTipText(MouseEvent event) {
        final int row = rowAtPoint(event.getPoint());
        return row < 0 ? null : rows.get(row).hint;
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        // Full-width group presentation; the same full text remains a real model/accessibility cell.
        final Rectangle clip = graphics.getClipBounds();
        for (int row = 0; row < rows.size(); row++) if (rows.get(row).army == null) {
            final Rectangle bounds = getCellRect(row, 0, true);
            bounds.width = getWidth();
            if (clip != null && !clip.intersects(bounds)) continue;
            graphics.setColor(getBackground()); graphics.fillRect(0, bounds.y, bounds.width, bounds.height);
            final JLabel label = new JLabel(rows.get(row).group);
            label.setFont(getFont().deriveFont(Font.BOLD)); label.setForeground(getForeground());
            final Graphics child = graphics.create(4, bounds.y, Math.max(1, bounds.width - 8), bounds.height);
            label.setSize(Math.max(1, bounds.width - 8), bounds.height); label.paint(child); child.dispose();
        }
    }

    JPopupMenu options() {
        final JPopupMenu popup = new JPopupMenu();
        for (int i = 0; i < IDS.length; i++) {
            final String id = IDS[i];
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(labels.getString(KEYS[i]), visible.contains(id));
            item.setEnabled(i != 0);
            item.addActionListener(event -> {
                if (item.isSelected()) visible.add(id); else visible.remove(id);
                rebuildColumns(); save();
            });
            popup.add(item);
        }
        popup.addSeparator();
        final JCheckBoxMenuItem density = new JCheckBoxMenuItem(labels.getString("BATTLESIM.ROSTER.COMPACT"), compact);
        density.addActionListener(event -> { compact = density.isSelected(); applyDensity(); densityChanged.run(); save(); });
        popup.add(density);
        final JMenuItem reset = new JMenuItem(labels.getString("BATTLESIM.ROSTER.RESET"));
        reset.addActionListener(event -> { defaults(); rebuildColumns(); applyDensity(); densityChanged.run(); save(); });
        popup.add(reset);
        return popup;
    }
    boolean compact() { return compact; }
    private void applyDensity() { setRowHeight(Math.max(18, getFontMetrics(getFont()).getHeight() + (compact ? 2 : 8))); }
    private void defaults() {
        compact = false; visible.clear(); widths.clear();
        for (int i = 0; i < IDS.length; i++) widths.put(IDS[i], DEFAULT_WIDTHS[i]);
        visible.addAll(List.of("name", "layers", "count"));
    }
    private void rebuildColumns() {
        while (getColumnModel().getColumnCount() > 0) removeColumn(getColumnModel().getColumn(0));
        for (int i = 0; i < IDS.length; i++) if (visible.contains(IDS[i])) {
            final TableColumn column = new TableColumn(i, widths.get(IDS[i]));
            column.setIdentifier(IDS[i]); column.setHeaderValue(labels.getString(KEYS[i]));
            column.setMinWidth(IDS[i].equals("name") ? 96 : 48);
            column.setPreferredWidth(widths.get(IDS[i])); addColumn(column);
        }
    }
    private static int clamp(String id, int width) { return Math.max(id.equals("name") ? 96 : 48, Math.min(500, width)); }
    private void restore(String saved) {
        if (saved.isBlank()) return;
        final String[] parts = saved.split(";");
        if (parts.length == 0) return;
        compact = parts[0].equals("compact");
        if (parts.length < 2) return;
        final List<String> restored = new ArrayList<>(); restored.add("name");
        boolean valid = false;
        for (String part : parts[1].split(",")) {
            final String[] pair = part.split(":");
            if (pair.length < 2 || !widths.containsKey(pair[0])) continue;
            try {
                widths.put(pair[0], clamp(pair[0], Integer.parseInt(pair[1])));
                if ((pair.length < 3 || !pair[2].equals("0")) && !restored.contains(pair[0])) restored.add(pair[0]);
                valid = true;
            } catch (NumberFormatException ignored) { }
        }
        if (valid) { visible.clear(); visible.addAll(restored); }
    }
    private void save() {
        final StringBuilder saved = new StringBuilder(compact ? "compact;" : "comfortable;");
        for (String id : IDS) saved.append(id).append(':').append(widths.get(id)).append(':')
                .append(visible.contains(id) ? '1' : '0').append(',');
        SettingsManager.getInstance().setConfigAndSaveToFile(CONFIG, saved.toString());
    }
    private final class RosterModel extends AbstractTableModel {
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return IDS.length; }
        @Override public String getColumnName(int column) { return labels.getString(KEYS[column]); }
        @Override public Object getValueAt(int row, int column) { return rows.get(row).cells[column]; }
    }
}
