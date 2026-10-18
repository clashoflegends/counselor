package gui.services;

import baseLib.BaseModel;
import business.facade.AcaoFacade;
import control.facade.WorldFacadeCounselor;
import control.services.OrdersList;
import control.services.OrdersList.Row;
import control.support.DispatchManager;
import gui.MainDadosGui;
import gui.TabBase;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import persistenceCommons.SettingsManager;

/** Experimental orders browser; created only when EnableOrdersTab=1. */
public final class OrdersPanel extends JPanel {
    private final WorldFacadeCounselor world = WorldFacadeCounselor.getInstance();
    private final Object partida = world.getPartida();
    private final OrderModel model = new OrderModel();
    private final JTable table = new JTable(model) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // A chronological boundary only makes sense when time is the primary sort key.
            if (waterline == null || sorter.getSortKeys().isEmpty()
                    || sorter.getSortKeys().get(0).getColumn() != 0) return;
            g.setColor(getSelectionBackground());
            for (int row = 1; row < getRowCount(); row++) {
                Instant previous = model.rows.get(convertRowIndexToModel(row - 1)).time();
                Instant current = model.rows.get(convertRowIndexToModel(row)).time();
                if (previous != null && current != null
                        && previous.isAfter(waterline) != current.isAfter(waterline)) {
                    int y = getCellRect(row, 0, true).y;
                    g.fillRect(0, y, getWidth(), 2);
                }
            }
        }
    };
    private final TableRowSorter<OrderModel> sorter = new TableRowSorter<>(model);
    private final JComboBox<Choice> players = new JComboBox<>();
    private final JComboBox<Choice> nations = new JComboBox<>();
    private final JComboBox<String> zones = new JComboBox<>(new String[]{"UTC", tx("LOCAL")});
    private final JTextField search = new JTextField(15);
    private final JTextField since = new JTextField(10);
    private final JCheckBox movements = new JCheckBox(tx("MOVEMENT"));
    private final JLabel count = new JLabel();
    private final JLabel waterlineLabel = new JLabel();
    private final JTextArea details = new JTextArea();
    private Instant waterline;
    private boolean updating;

    private record Choice(int id, String label) {
        @Override public String toString() { return label; }
    }

    public OrdersPanel() {
        super(new BorderLayout(0, 4));
        players.setName("ordersPlayers"); nations.setName("ordersNations"); zones.setName("ordersTimezone");
        search.setName("ordersSearch"); since.setName("ordersSince"); movements.setName("ordersMovement");
        waterlineLabel.setName("ordersWaterline");
        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        JPanel first = line();
        first.add(new JLabel(tx("PLAYER"))); first.add(players);
        first.add(new JLabel(tx("NATION"))); first.add(nations);
        players.setPreferredSize(new Dimension(155, players.getPreferredSize().height));
        nations.setPreferredSize(new Dimension(155, nations.getPreferredSize().height));
        JButton reload = new JButton(tx("REFRESH"));
        reload.setToolTipText(tx("REFRESH.HINT"));
        reload.addActionListener(e -> DispatchManager.getInstance().sendDispatchForMsg(
                DispatchManager.SHARED_ORDERS_REFRESH, reload));
        first.add(reload);
        JPanel second = line();
        second.add(new JLabel(tx("SEARCH"))); second.add(search); second.add(movements);
        JCheckBox moreColumns = new JCheckBox(tx("COLUMNS"));
        moreColumns.setName("ordersMoreColumns");
        moreColumns.setSelected(SettingsManager.getInstance().isConfig("OrdersTab.MoreColumns", "1", "0"));
        second.add(moreColumns);
        JPanel third = line();
        third.add(new JLabel(tx("SINCE"))); third.add(since); third.add(zones);
        since.setToolTipText(tx("SINCE.HINT"));
        since.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
        JButton today = new JButton(tx("TODAY"));
        today.addActionListener(e -> since.setText(LocalDate.now(zone()).toString()));
        JButton all = new JButton(tx("ALL.DATES"));
        all.addActionListener(e -> since.setText(""));
        JButton clear = new JButton(tx("CLEAR"));
        clear.addActionListener(e -> clearFilters());
        third.add(today); third.add(all); third.add(clear); third.add(count);
        filters.add(first); filters.add(second); filters.add(third);
        JPanel reference = new JPanel(new BorderLayout());
        reference.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        reference.add(waterlineLabel, BorderLayout.CENTER); filters.add(reference);
        add(filters, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Bind the formatter to the column itself, including after filtering and LAF updates.
        table.getColumnModel().getColumn(0).setCellRenderer(new TimeRenderer());
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        int[] widths = {145, 125, 145, 180, 115, 125, 40, 65};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        List<javax.swing.table.TableColumn> extraColumns = new ArrayList<>();
        for (int i = 4; i < 8; i++) extraColumns.add(table.getColumnModel().getColumn(i));
        if (!moreColumns.isSelected()) extraColumns.forEach(table::removeColumn);
        moreColumns.addActionListener(e -> {
            if (moreColumns.isSelected()) extraColumns.forEach(table::addColumn);
            else extraColumns.forEach(table::removeColumn);
            SettingsManager.getInstance().setConfig("OrdersTab.MoreColumns", moreColumns.isSelected() ? "1" : "0");
        });
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !updating) showSelected(); });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) openSelected(); }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-order");
        table.getActionMap().put("open-order", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { openSelected(); }
        });
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.setFont(table.getFont());
        JPanel lower = new JPanel(new BorderLayout());
        lower.add(new JScrollPane(details), BorderLayout.CENTER);
        JButton open = new JButton(tx("OPEN"));
        open.addActionListener(e -> openSelected());
        lower.add(open, BorderLayout.SOUTH);
        lower.setMinimumSize(new Dimension(100, 90));
        JScrollPane listScroll = new JScrollPane(table);
        listScroll.setColumnHeaderView(table.getTableHeader());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScroll, lower);
        split.setName("ordersDivider");
        rememberDivider(split);
        add(split, BorderLayout.CENTER);
        players.addActionListener(e -> filter()); nations.addActionListener(e -> filter());
        zones.addActionListener(e -> { table.getColumnModel().getColumn(table.convertColumnIndexToView(0)).setHeaderValue(timeTitle());
            table.getTableHeader().repaint(); updateWaterlineLabel(); filter(); showSelected(); table.repaint(); });
        movements.addActionListener(e -> filter());
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        };
        search.getDocument().addDocumentListener(listener); since.getDocument().addDocumentListener(listener);
        refresh();
    }

    private static JPanel line() { return new JPanel(new WrapLayout(FlowLayout.LEADING, 5, 2)); }
    public static String tx(String key) { return SettingsManager.getInstance().getBundleManager().getString("ORDERS.TAB." + key); }
    public JTable table() { return table; }
    public String searchText(int row) {
        return row >= 0 && row < model.rows.size() ? model.rows.get(row).searchText() : "";
    }
    public void clearFilters() {
        updating = true;
        search.setText(""); since.setText(""); movements.setSelected(false);
        players.setSelectedIndex(0); nations.setSelectedIndex(0);
        updating = false;
        filter(); showSelected();
    }

    private void rememberDivider(JSplitPane split) {
        SettingsManager settings = SettingsManager.getInstance();
        int saved = settings.getConfigAsInt("OrdersTab.Divider", "400");
        split.setResizeWeight(0.78);
        split.setDividerLocation(Math.max(90, saved));
        split.getTopComponent().setMinimumSize(new Dimension(100, 90));
        split.addComponentListener(new ComponentAdapter() {
            private boolean restored;
            @Override public void componentResized(ComponentEvent e) {
                int max = split.getHeight() - split.getDividerSize() - 90;
                if (max < 90) return;
                split.setDividerLocation(Math.max(90, Math.min(max, restored ? split.getDividerLocation() : saved)));
                restored = true;
                split.putClientProperty("ordersDividerRestored", true);
            }
        });
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
            if (Boolean.TRUE.equals(split.getClientProperty("ordersDividerRestored"))) {
                settings.setConfig("OrdersTab.Divider", Integer.toString(split.getDividerLocation()));
            }
        });
    }
    public boolean currentWorld() { return partida == world.getPartida(); }
    private ZoneId zone() { return zones == null || zones.getSelectedIndex() == 0 ? ZoneOffset.UTC : ZoneId.systemDefault(); }
    private String timeTitle() { return tx("TIME") + " (" + zone().getId() + ")"; }
    private Row selected() {
        int view = table.getSelectedRow();
        return view < 0 ? null : model.rows.get(table.convertRowIndexToModel(view));
    }

    public void refresh() {
        if (!currentWorld()) return;
        Row before = selected();
        updating = true;
        model.rows = OrdersList.collect(world.getActors(), world.getLocais());
        waterline = OrdersList.latestOwnCharacterTime(model.rows, world.getJogadorAtivo());
        updateWaterlineLabel();
        populateChoices();
        model.fireTableDataChanged();
        updating = false;
        filter();
        if (before != null) {
            for (int i = 0; i < model.rows.size(); i++) {
                if (model.rows.get(i).key().equals(before.key())) {
                    int view = table.convertRowIndexToView(i);
                    if (view >= 0) table.setRowSelectionInterval(view, view);
                    break;
                }
            }
        }
        showSelected();
    }

    private void updateWaterlineLabel() {
        waterlineLabel.setVisible(waterline != null);
        if (waterline == null) return;
        waterlineLabel.setText(tx("WATERLINE") + ": " + formatTime(waterline) + " (" + zone().getId() + ")");
        waterlineLabel.setToolTipText(tx("WATERLINE.HINT") + " " + waterline);
    }

    private String formatTime(Instant time) {
        LocalDate today = LocalDate.now(zone()), day = time.atZone(zone()).toLocalDate();
        String date = today.equals(day) ? tx("TODAY") : today.minusDays(1).equals(day) ? tx("YESTERDAY") : day.toString();
        return date + " " + DateTimeFormatter.ofPattern("HH:mm").format(time.atZone(zone()));
    }

    private void populateChoices() {
        Choice player = (Choice) players.getSelectedItem(), nation = (Choice) nations.getSelectedItem();
        TreeMap<Integer, String> ps = new TreeMap<>(), ns = new TreeMap<>();
        for (Row row : model.rows) {
            ns.put(row.nation().getId(), row.nation().getNome());
            if (row.nation().getOwner() != null) ps.put(row.nation().getOwner().getId(), row.player());
        }
        players.removeAllItems(); nations.removeAllItems();
        players.addItem(new Choice(-1, tx("ALL")));
        players.addItem(new Choice(-2, tx("MINE")));
        players.addItem(new Choice(-3, tx("SHARED")));
        nations.addItem(new Choice(-1, tx("ALL")));
        ps.forEach((id, name) -> players.addItem(new Choice(id, name)));
        ns.forEach((id, name) -> nations.addItem(new Choice(id, name)));
        restore(players, player); restore(nations, nation);
    }

    private static void restore(JComboBox<Choice> combo, Choice old) {
        if (old == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id == old.id) { combo.setSelectedIndex(i); return; }
        }
        // A nation whose last action disappeared should stay selected with zero rows.
        combo.addItem(old);
        combo.setSelectedItem(old);
    }

    private void filter() {
        if (updating) return;
        LocalDate date = null;
        boolean valid = true;
        try { if (!since.getText().isBlank()) date = LocalDate.parse(since.getText().trim()); }
        catch (DateTimeParseException e) { valid = false; }
        since.setToolTipText(valid ? tx("SINCE.HINT") : tx("DATE.INVALID"));
        final LocalDate cutoff = date;
        final boolean validDate = valid;
        Choice p = (Choice) players.getSelectedItem(), n = (Choice) nations.getSelectedItem();
        sorter.setRowFilter(new RowFilter<>() {
            public boolean include(Entry<? extends OrderModel, ? extends Integer> e) {
                Row row = model.rows.get(e.getIdentifier());
                boolean mine = OrdersList.ownedBy(row.nation(), world.getJogadorAtivo());
                return validDate && (p == null || p.id == -1 || p.id == -2 && mine || p.id == -3 && !mine
                        || p.id >= 0 && row.nation().getOwner() != null && row.nation().getOwner().getId() == p.id)
                        && (n == null || n.id == -1 || n.id == row.nation().getId())
                        && (!movements.isSelected() || new AcaoFacade().isMovimento(row.order()))
                        && OrdersList.matches(row, search.getText(), cutoff, zone());
            }
        });
        count.setText(valid ? table.getRowCount() + " / " + model.getRowCount() : tx("DATE.INVALID"));
    }

    private void showSelected() {
        Row row = selected();
        if (row == null) { details.setText(""); return; }
        details.setText(row.player() + " / " + row.nation().getNome() + " / " + row.actor().getNome()
                + " / " + tx("SLOT") + " " + (row.slot() + 1) + "\n"
                + row.order().getOrdem().getDescricao() + "\n" + row.parameters() + "\n"
                + row.coordinates() + "\n" + tx("TIME") + ": "
                + (row.time() == null ? tx("UNKNOWN") : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(row.time().atZone(zone()))));
        details.setCaretPosition(0);
        if (isShowing() && row.location() != null) world.getMapaControler().printTag(row.location());
    }

    private void openSelected() {
        Row row = selected();
        if (row == null || !currentWorld()) return;
        MainDadosGui dados = (MainDadosGui) SwingUtilities.getAncestorOfClass(MainDadosGui.class, this);
        if (dados == null) return;
        JTabbedPane tabs = dados.getTabbedPane();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getComponentAt(i) instanceof TabBase tab && tab.openOrder(row.actor(), row.slot())) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    private final class TimeRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            Instant time = (Instant) value;
            LocalDate today = LocalDate.now(zone());
            Row order = model.rows.get(t.convertRowIndexToModel(row));
            boolean newerShared = time != null && waterline != null && time.isAfter(waterline)
                    && !OrdersList.ownedBy(order.nation(), world.getJogadorAtivo());
            setText(time == null ? "\u2014" : (newerShared ? "\u2191 " : "") + formatTime(time));
            setFont(t.getFont().deriveFont(time != null && today.equals(day(time)) ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
            setToolTipText(time == null ? tx("UNKNOWN") : (newerShared ? tx("NEWER.SHARED") + " " : "") + time);
            boolean boundary = row > 0 && !sorter.getSortKeys().isEmpty() && sorter.getSortKeys().get(0).getColumn() == 0
                    && !Objects.equals(day(time), day(model.rows.get(t.convertRowIndexToModel(row - 1)).time()));
            if (boundary) setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, t.getGridColor()));
            return this;
        }
        private LocalDate day(Instant time) { return time == null ? null : time.atZone(zone()).toLocalDate(); }
    }

    private final class OrderModel extends AbstractTableModel {
        private List<Row> rows = new ArrayList<>();
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return 8; }
        public String getColumnName(int c) {
            return c == 0 ? timeTitle() : tx(new String[]{"TIME", "ACTOR", "ACTION", "PARAMETERS", "PLAYER", "NATION", "SLOT", "HEX"}[c]);
        }
        public Class<?> getColumnClass(int c) { return c == 0 ? Instant.class : c == 6 ? Integer.class : String.class; }
        public Object getValueAt(int r, int c) {
            Row row = rows.get(r);
            return switch (c) {
                case 0 -> row.time(); case 1 -> row.actor().getNome(); case 2 -> row.order().getOrdem().getDescricao();
                case 3 -> row.parameters(); case 4 -> row.player();
                case 5 -> row.nation().getNome(); case 6 -> row.slot() + 1;
                default -> row.location() == null ? "" : business.facade.LocalFacade.getCoordenadas(row.location());
            };
        }
    }
}
