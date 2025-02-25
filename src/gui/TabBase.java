/*
 * TabBase.java
 *
 * Created on April 24, 2008, 10:28 PM
 */
package gui;

import control.MapaControler;
import gui.services.ActorActionTableCellRenderer;
import gui.services.ColumnWidthsAdjuster;
import gui.services.LocalTableCellRenderer;
import gui.services.OpenSlotTableCellRenderer;
import java.io.Serializable;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import model.ActorAction;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.OpenSlotCounter;

/**
 *
 * @author gurgel
 */
public abstract class TabBase extends javax.swing.JRootPane implements Serializable {

    private static final Log log = LogFactory.getLog(TabBase.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private String dica;
    private String title;
    private String keyFilterProperty;
    private ImageIcon icone;
    private MapaControler mapaControler;
    private int filtroDefault = -9999;

    /**
     * Creates new form TabBase
     */
    public TabBase() {
        initComponents();
    }

    protected int getComboFiltroSize() {
        return 0;
    }

    public final String getKeyFilterProperty() {
        return keyFilterProperty;
    }

    public final void setKeyFilterProperty(String keyFilterProperty) {
        this.keyFilterProperty = keyFilterProperty;
        this.setFiltroDefault(SettingsManager.getInstance().getConfigAsInt(keyFilterProperty, "-9999"));
    }

    public final int getFiltroDefault() {
        if (filtroDefault == -9999 || this.getComboFiltroSize() <= filtroDefault) {
            //load default
            int vlFiltro = SettingsManager.getInstance().getConfigAsInt("filtro.default");
            //se diferente de 0,provavelmente eh um valor invalido, ou chave nao encontrada.
            if (vlFiltro != 1 || this.getComboFiltroSize() <= filtroDefault) {
                vlFiltro = 0;
            }
            this.setFiltroDefault(vlFiltro);
        }
        return filtroDefault;
    }

    private void setFiltroDefault(int filtroDefault) {
        this.filtroDefault = filtroDefault;
    }

    public String getDica() {
        return dica;
    }

    public final void setDica(String toolTip) {
        this.dica = toolTip;
    }

    public String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        this.title = title;
    }

    public ImageIcon getIcone() {
        return icone;
    }

    public final void setIcone(String iconeName) {
        try {
            this.icone = new ImageIcon(getClass().getResource(iconeName));
        } catch (NullPointerException ex) {
        }
    }

    public MapaControler getMapaControler() {
        return this.mapaControler;
    }

    protected final void setMapaControler(MapaControler mapaControl) {
        this.mapaControler = mapaControl;
    }

    public void doTagHide() {
        this.mapaControler.getTabGui().hidefocusTag();
    }

    protected void doConfigTableColumns(JTable table) {
        //set renders for Action columns
        table.setDefaultRenderer(ActorAction.class, new ActorActionTableCellRenderer(table));
        //set renders for Open Slot columns
        table.setDefaultRenderer(OpenSlotCounter.class, new OpenSlotTableCellRenderer(table));
        //set render for Local/Hex
        if (this.mapaControler != null) {
            table.setDefaultRenderer(Local.class, new LocalTableCellRenderer(this.mapaControler, table));
        }
        //Adjust all columns to fit.
        if (SettingsManager.getInstance().isTableColumnAdjust()) {
            final ColumnWidthsAdjuster cwa = new ColumnWidthsAdjuster();
            cwa.calcColumnWidths(table);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    protected DefaultComboBoxModel getDefaultComboBoxModelTodosProprio() {
        return new DefaultComboBoxModel(new String[]{labels.getString("FILTRO.TODOS"), labels.getString("FILTRO.PROPRIOS")});
    }

    private void applyGlobalFilter(String searchText) {
        // Make sure we have a row sorter
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) getMainLista().getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(getMainLista().getModel());
            getMainLista().setRowSorter(sorter);
        }

        if (searchText == null || searchText.trim().isEmpty()) {
            // No search text -> show all rows
            sorter.setRowFilter(null);
        } else {
            // Build a case-insensitive partial-match regex,
            // quoting any special regex characters from the user input:
            String regex = "(?i).*" + Pattern.quote(searchText.trim()) + ".*";

            try {
                // If you omit column indices here, it will search ALL columns
                sorter.setRowFilter(RowFilter.regexFilter(regex));
            } catch (java.util.regex.PatternSyntaxException e) {
                // In case the user types an invalid regex, fall back to no filter
                sorter.setRowFilter(null);
            }
        }
    }

    protected final void addDocumentListener(JTextField searchField) {
        //add listener to search field for filtering.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyGlobalFilter(searchField.getText());
                refocusField(searchField);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyGlobalFilter(searchField.getText());
                refocusField(searchField);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Typically not fired for plain text components, but just in case:
                applyGlobalFilter(searchField.getText());
            }
        });
    }

    private void refocusField(JTextField field) {
        SwingUtilities.invokeLater(() -> {
            field.requestFocusInWindow();
        });
    }

    public JTable getMainLista() {
        log.fatal("Can't use getMainLista() here. Needs to override int he tab.");
        return null;
    }
}
