package gui.subtabs;

import control.services.ArtefatoConverter;
import gui.TabBase;
import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import model.Artefato;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * The character's magic items, with what each one actually DOES underneath. T-850.
 *
 * <h3>The request</h3>
 *
 * A player, 2026-09-30: "when a character is carrying a magic item, it would be useful if the tab
 * that contains magic items for that character also contains the help information for that magic
 * item. Things like dormant powers and whether or not it can move would be good to see instantly
 * instead of looking them up."
 *
 * <h3>Why a class of its own</h3>
 *
 * The tab was a bare {@link SubTabBaseList}, which is a table and nothing else - and is shared with
 * the character's spell list, so growing a detail pane there would have grown one for spells too,
 * from a model with different columns. This is the same table with a text pane under it, written by
 * hand rather than in the form editor so that no {@code .form} has to be hand-edited.
 *
 * <h3>Rows and items are kept side by side</h3>
 *
 * The table carries strings, not the items they came from, so the selection has to be mapped back.
 * The list is handed in next to the model and in the same order the model was built from, and the
 * view row is converted to a model row first - the table sorts, so the two differ the moment anyone
 * clicks a header.
 */
public class SubTabMagicItems extends TabBase implements Serializable {

    private static final long serialVersionUID = 1L;
    protected static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    /** Enough for the history plus a few powers without hiding the table it belongs to. */
    private static final int DETAIL_DIVIDER = 190;

    private final JTable jtListaBase = new JTable();
    private final JTextArea detail = new JTextArea();
    private final transient List<Artefato> items = new ArrayList<>();

    public SubTabMagicItems() {
        setLayout(new BorderLayout());

        jtListaBase.setAutoCreateColumnsFromModel(true);
        jtListaBase.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtListaBase.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jtListaBase.setAutoCreateRowSorter(true);
        jtListaBase.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                doShowSelected();
            }
        });

        detail.setEditable(false);
        detail.setFocusable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        // The table is themed; a text area left on its own defaults are not, and a white slab under
        // a dark table is the one thing that would make this look bolted on.
        detail.setOpaque(false);
        detail.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));

        final JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.setBorder(null);

        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(jtListaBase), detailScroll);
        split.setBorder(null);
        split.setDividerLocation(DETAIL_DIVIDER);
        // The table keeps its size when the window grows; the text pane takes the new room, because
        // a character carries two or three items and the thing that gets longer is the explanation.
        split.setResizeWeight(0);
        add(split, BorderLayout.CENTER);

        setListModelClear();
    }

    /** The empty state: the same column headings the list shows, and no detail. */
    public final void setListModelClear() {
        items.clear();
        jtListaBase.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{{"-", "-", "-", "-"}},
                new String[]{labels.getString("NOME"), labels.getString("PODER"),
                    labels.getString("VALOR"), labels.getString("DESCRICAO")}));
        detail.setText("");
    }

    /**
     * The table, plus the items it was built from IN THE SAME ORDER.
     *
     * Passing them separately rather than deriving one from the other is deliberate: the model holds
     * formatted strings, and matching an item back by its displayed name would pick the wrong one of
     * two items that share a name.
     */
    public void setListModel(TableModel model, Collection<Artefato> artefatos) {
        if (model == null) {
            setListModelClear();
            return;
        }
        items.clear();
        if (artefatos != null) {
            items.addAll(artefatos);
        }
        jtListaBase.setModel(model);
        if (model.getRowCount() > 0) {
            doConfigTableColumns(jtListaBase);
            // Select the first item rather than leaving the pane blank: a character with one item
            // would otherwise have to click the row he can already see to be told anything.
            jtListaBase.getSelectionModel().setSelectionInterval(0, 0);
        } else {
            detail.setText("");
        }
    }

    private void doShowSelected() {
        final int viewRow = jtListaBase.getSelectedRow();
        if (viewRow < 0) {
            detail.setText("");
            return;
        }
        final int modelRow = jtListaBase.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= items.size()) {
            // The placeholder model has a row and no item behind it, and a stale selection can
            // outlive a model swap. Neither is worth a stack trace in the log.
            detail.setText("");
            return;
        }
        detail.setText(ArtefatoConverter.getDetailText(items.get(modelRow)));
        detail.setCaretPosition(0);
    }
}
