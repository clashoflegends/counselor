/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SubTabTropas.java
 *
 * Created on 30/Abr/2011, 13:15:51
 */
package gui.subtabs;

import baseLib.GenericoTableModel;
import control.TropasSelectionControler;
import control.services.ExercitoConverter;
import gui.TabBase;
import gui.components.IntegerEditor;
import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Exercito;
import model.Pelotao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import persistence.XmlManager;

/**
 *
 * @author jmoura
 */
public class SubTabTropas extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabTropas.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private TropasSelectionControler tropasControl;

    public SubTabTropas(String vlInicial, Exercito exercito, boolean all, int filtro) {
        initComponents();

        //override filtro para all.
        if (all) {
            filtro = 0;
        }
        SortedMap<String, Integer> listInicial = (SortedMap<String, Integer>) XmlManager.getInstance().fromXml(vlInicial);
        GenericoTableModel model = ExercitoConverter.getTropaTipoTableModel(exercito, listInicial, filtro);

        initConfig(model);
    }

    private void initConfig(GenericoTableModel model) {
        this.tropasControl = new TropasSelectionControler(this);
        if (model != null) {
            this.jtMainLista.setModel(model);
            //Set up stricter input validation for the integer column at the last column.
            this.jtMainLista.getColumnModel().getColumn(model.getColumnCount() - 1).setCellEditor(new IntegerEditor(0, 100000));
//            this.jtMainLista.getColumnModel().getColumn(model.getColumnCount() - 1).setCellEditor(new SpinnerEditor(10000));
        } else {
            jtMainLista.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{
                {null, null},
                {null, null},
                {null, null}
            },
                    new String[]{
                labels.getString("TROPA"), labels.getString("QTD")
            }));
        }
        calcColumnWidths(jtMainLista);
        jtMainLista.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jsPanel = new javax.swing.JScrollPane();
        jtMainLista = new javax.swing.JTable();

        jsPanel.setBorder(null);

        jtMainLista.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Troops", "Qty"
            }
        ));
        jsPanel.setViewportView(jtMainLista);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jsPanel;
    private javax.swing.JTable jtMainLista;
    // End of variables declaration//GEN-END:variables

    public String getTropasId() {
        SortedMap<String, Integer> list = new TreeMap<String, Integer>();//cd,qtd
        final int rowCount = this.jtMainLista.getModel().getRowCount();
        final int colCount = this.jtMainLista.getModel().getColumnCount();
        for (int row = 0; row < rowCount; row++) {
            try {
                Integer qtd = (Integer) this.jtMainLista.getModel().getValueAt(row, colCount - 1);
                if (qtd > 0) {
                    Pelotao pelotao = (Pelotao) this.jtMainLista.getModel().getValueAt(row, 0);
                    list.put(pelotao.getCodigo(), qtd);
                }
            } catch (NullPointerException e) {
                //qtd is null
            }
        }
        return XmlManager.getInstance().toXml(list);
    }

    public String getTropasDisplay() {
        SortedMap<String, Integer> list = new TreeMap<String, Integer>();//ds,qtd
        final int rowCount = this.jtMainLista.getModel().getRowCount();
        final int colCount = this.jtMainLista.getModel().getColumnCount();
        for (int row = 0; row < rowCount; row++) {
            Integer qtd = (Integer) this.jtMainLista.getModel().getValueAt(row, colCount - 1);
            if (qtd > 0) {
                Pelotao pelotao = (Pelotao) this.jtMainLista.getModel().getValueAt(row, 0);
                list.put(pelotao.getNome(), qtd);
            }
        }
        return list.toString();
    }
}
