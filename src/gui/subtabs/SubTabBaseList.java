/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SubTabTextArea.java
 *
 * Created on 17/Abr/2011, 9:35:23
 */
package gui.subtabs;

import gui.TabBase;
import java.io.Serializable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class SubTabBaseList extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabBaseList.class);
    protected static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    /**
     * Creates new form SubTabTextArea
     */
    public SubTabBaseList() {
        initComponents();
        iniciaConfig();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        detLista = new javax.swing.JScrollPane();
        jtListaBase = new javax.swing.JTable();

        detLista.setBorder(null);
        detLista.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jtListaBase.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"-", "-", "-", "-"}
            },
            new String [] {
                "Feitico", "Habilidade", "Tomo", "Obs"
            }
        ));
        detLista.setViewportView(jtListaBase);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(detLista, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 375, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(detLista, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane detLista;
    private javax.swing.JTable jtListaBase;
    // End of variables declaration//GEN-END:variables

    private void iniciaConfig() {
        //configura grid de apoio
        setListModelClear();
        jtListaBase.setAutoCreateColumnsFromModel(true);
        jtListaBase.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtListaBase.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jtListaBase.setAutoCreateRowSorter(true);
    }

    public void setListModelClear() {
        jtListaBase.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
            {"-", "-", "-", "-"}},
                new String[]{
            labels.getString("NOME"), labels.getString("PODER"), labels.getString("VALOR"), labels.getString("DESCRICAO")
        }));
    }

    public void setListModel(TableModel model) {
        if (model == null) {
            setListModelClear();
        } else {
            this.jtListaBase.setModel(model);
            if (model.getRowCount() > 0) {
                doConfigTableColumns(jtListaBase);
            }
        }
    }
}
