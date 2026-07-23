/*
 * CidadeAlvoPicker.java
 */
package gui.subtabs;

import baseLib.IBaseModel;
import control.MapaControler;
import gui.services.SvgIcon;
import gui.services.Toast;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Cidade;
import model.Local;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * City-target picker for resource-transport orders (Cidade_Any / Cidade_Nacao). A filter field narrows a
 * NON-editable combo of "Name (COORD)" cities as you type - matching the name OR the coordinate - with a
 * Name/Coord sort toggle (remembered) and a "pick on the map" button.
 *
 * UI only: the selected value is still the city's coordinate ({@code getComboId}) and its display is
 * still "Name (COORD)" ({@code getComboDisplay}), byte-identical to the plain combo it replaces, so the
 * order serialization / server contract is untouched.
 *
 * Uses a separate filter field driving a non-editable combo (rather than an editable combo) on purpose:
 * it avoids mutating a document from within its own listener, and keeps the selected item always a real
 * model object (never a typed String), so value-read is unambiguous.
 *
 * @author jmoura
 */
public class CidadeAlvoPicker extends JPanel {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final String SORT_CONFIG = "CityTargetSort";  // "name" | "coord"

    private final List<IBaseModel> master = new ArrayList<>();
    private final DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
    private final JComboBox combo = new JComboBox(comboModel);
    private final JTextField filterField = new JTextField(8);
    private final JButton sortBtn = new JButton();
    private final JToggleButton mapBtn = new JToggleButton();
    private final MapaControler mapaControler;
    private final boolean nacaoOnly;   // true = Cidade_Nacao (own cities); false = Cidade_Any
    private boolean sortByCoord;
    private IBaseModel lastValid;

    public CidadeAlvoPicker(ComboBoxModel model, MapaControler mapaControler, boolean nacaoOnly) {
        super(new BorderLayout(4, 0));
        this.mapaControler = mapaControler;
        this.nacaoOnly = nacaoOnly;
        this.sortByCoord = "coord".equals(SettingsManager.getInstance().getConfig(SORT_CONFIG, "name"));
        // Snapshot the caller's model - we mutate our own DefaultComboBoxModel when filtering, never theirs.
        for (int i = 0; i < model.getSize(); i++) {
            Object it = model.getElementAt(i);
            if (it instanceof IBaseModel) {
                master.add((IBaseModel) it);
            }
        }
        combo.setEditable(false);
        filterField.setToolTipText(labels.getString("CITY.PICK.FILTER.TT"));
        filterField.putClientProperty("JTextField.placeholderText", labels.getString("CITY.PICK.FILTER.TT"));
        sortBtn.setToolTipText(labels.getString("CITY.PICK.SORT.TT"));
        mapBtn.setText(labels.getString("CITY.PICK.MAP"));
        mapBtn.setToolTipText(labels.getString("CITY.PICK.MAP.TT"));
        updateSortButtonText();
        sortBtn.setIcon(SvgIcon.themed("arrow-down", 16));  // small down-arrow cue on the sort toggle
        mapBtn.setIcon(SvgIcon.themed("map", 16));           // 16x16 map icon on the pick-on-map button

        JPanel east = new JPanel(new BorderLayout(2, 0));
        east.add(sortBtn, BorderLayout.WEST);
        east.add(mapBtn, BorderLayout.EAST);
        add(filterField, BorderLayout.WEST);
        add(combo, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);

        rebuild();

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleFilter();
            }
        });
        combo.addActionListener(e -> {
            Object s = combo.getSelectedItem();
            if (s instanceof IBaseModel) {
                lastValid = (IBaseModel) s;
            }
        });
        sortBtn.addActionListener(e -> {
            sortByCoord = !sortByCoord;
            SettingsManager.getInstance().setConfigAndSaveToFile(SORT_CONFIG, sortByCoord ? "coord" : "name");
            updateSortButtonText();
            rebuild();
        });
        mapBtn.addActionListener(e -> {
            if (mapaControler == null) {
                return;
            }
            if (mapBtn.isSelected()) {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w != null) {
                    Toast.show(w, labels.getString(nacaoOnly ? "CITY.PICK.MAP.HINT.NACAO" : "CITY.PICK.MAP.HINT.ANY"));
                }
                mapaControler.armCityPick(this::onMapPick);
            } else {
                mapaControler.cancelCityPick();
                Toast.dismissCurrent();   // clear the "click a city" hint if they cancel
            }
        });
    }

    private void updateSortButtonText() {
        // Language-neutral: A-Z = by name, 1-9 = by coordinate.
        sortBtn.setText(sortByCoord ? "1-9" : "A-Z");
    }

    // Defer model mutation off the document-notification (avoids "Attempt to mutate in notification").
    private void scheduleFilter() {
        SwingUtilities.invokeLater(this::applyFilter);
    }

    private List<IBaseModel> sortedMaster() {
        List<IBaseModel> list = new ArrayList<>(master);
        Comparator<IBaseModel> c = sortByCoord
                ? Comparator.comparing(IBaseModel::getComboId)
                : Comparator.comparing(IBaseModel::getComboDisplay, String.CASE_INSENSITIVE_ORDER);
        list.sort(c);
        return list;
    }

    private void applyFilter() {
        final String t = filterField.getText().trim().toLowerCase();
        final Object prev = combo.getSelectedItem();
        comboModel.removeAllElements();
        for (IBaseModel o : sortedMaster()) {
            if (t.isEmpty() || o.getComboDisplay().toLowerCase().contains(t)) {
                comboModel.addElement(o);
            }
        }
        if (prev instanceof IBaseModel && comboModel.getIndexOf(prev) >= 0) {
            combo.setSelectedItem(prev);
        } else if (comboModel.getSize() > 0) {
            combo.setSelectedIndex(0);
            lastValid = (IBaseModel) comboModel.getElementAt(0);
        }
        // If the filter matches nothing, lastValid keeps the previous valid pick so we never
        // serialize an empty target (KI-017 territory).
        if (comboModel.getSize() > 0 && !t.isEmpty() && filterField.isFocusOwner()) {
            combo.setPopupVisible(true);
        }
    }

    private void rebuild() {
        applyFilter();
    }

    private void onMapPick(Local local) {
        mapBtn.setSelected(false);        // one-shot; MapaControler already cleared its callback
        Toast.dismissCurrent();           // the "click a city" hint has done its job - fade it now
        if (!isShowing()) {
            return;                       // stale widget (param area was rebuilt) - ignore
        }
        final Cidade c = (local == null) ? null : local.getCidade();
        if (c == null) {
            Toast.showError(labels.getString("CITY.PICK.INVALID"));
            return;
        }
        final String coord = c.getComboId();
        for (IBaseModel o : master) {
            if (o.getComboId().equals(coord)) {
                setSelectedById(coord);
                return;
            }
        }
        Toast.showError(labels.getString("CITY.PICK.INVALID"));
    }

    // --- selection API: used by ComponentFactory init-select and value read ---

    public void setSelectedById(String id) {
        selectMatching(id, false);
    }

    public void setSelectedByDisplay(String display) {
        selectMatching(display, true);
    }

    private void selectMatching(String key, boolean byDisplay) {
        if (key == null) {
            return;
        }
        filterField.setText("");   // show the full list before selecting
        applyFilter();
        for (int i = 0; i < comboModel.getSize(); i++) {
            IBaseModel o = (IBaseModel) comboModel.getElementAt(i);
            String cmp = byDisplay ? o.getComboDisplay() : o.getComboId();
            if (key.equals(cmp)) {
                combo.setSelectedItem(o);
                lastValid = o;
                return;
            }
        }
    }

    public String getComboId() {
        Object s = combo.getSelectedItem();
        if (s instanceof IBaseModel) {
            return ((IBaseModel) s).getComboId();
        }
        return (lastValid != null) ? lastValid.getComboId() : "";
    }

    public String getComboDisplay() {
        Object s = combo.getSelectedItem();
        if (s instanceof IBaseModel) {
            return ((IBaseModel) s).getComboDisplay();
        }
        return (lastValid != null) ? lastValid.getComboDisplay() : "";
    }
}
