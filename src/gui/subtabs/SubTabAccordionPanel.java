/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.subtabs;

import control.MapaControler;
import gui.TabBase;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import model.Local;

/**
 *
 * @author serguei
 */
public class SubTabAccordionPanel extends TabBase implements ActionListener {
    
    private JPanel topPanel = new JPanel(new GridLayout(1, 1));
    private JPanel bottomPanel = new JPanel(new GridLayout(1, 1));
    private Map<String, BarInfo> bars = new LinkedHashMap();
    private int visibleBar = -1;
    private JComponent visibleComponent = null;
    

    public SubTabAccordionPanel(MapaControler mapaControl) {
        this.setLayout(new BorderLayout());
        this.add(topPanel, BorderLayout.NORTH);
        this.add(bottomPanel, BorderLayout.SOUTH);
        setMapaControler(mapaControl);
    }

    public void addBar(String name, JComponent component, Local local) {
        BarInfo barInfo = new BarInfo(name, component, local);
        barInfo.getButton().addActionListener(this);
        this.bars.put(name, barInfo);
        render();
    }

    public void addBar(String name, Icon icon, JComponent component, Local local) {
        BarInfo barInfo = new BarInfo(name, icon, component, local);
        barInfo.getButton().addActionListener(this);
        this.bars.put(name, barInfo);
        render();
    }

    public void removeBar(String name) {
        this.bars.remove(name);
        render();
    }

    public int getVisibleBar() {
        return this.visibleBar;
    }

    public void setVisibleBar(int visibleBar) {
        if (visibleBar > 0
                && visibleBar < this.bars.size() - 1) {
            this.visibleBar = visibleBar;
            render();
        }
    }

    
    public synchronized void render() {

        int totalBars = this.bars.size();
        int topBars = totalBars;
        if (this.visibleBar != -1 && this.bars.size() > 0) {
            topBars = this.visibleBar + 1;
        }

        int bottomBars = totalBars - topBars;

        Iterator itr = this.bars.keySet().iterator();

        this.topPanel.removeAll();
        GridLayout topLayout = (GridLayout) this.topPanel.getLayout();
        topLayout.setRows(topBars);
        BarInfo barInfo;
        String selectedName = null;
        for (int i = 0; i < topBars; i++) {
            String barName = (String) itr.next();
            barInfo = (BarInfo) this.bars.get(barName);
            if (i != this.visibleBar) {
                barInfo.getButton().setSelected(false);
            } else {
              //  barInfo.getButton().setSelected(true);
                selectedName = barName;
            }
            this.topPanel.add(barInfo.getButton());
        }
        this.topPanel.validate();

        if (this.visibleComponent != null) {
            this.remove(this.visibleComponent);
        }
        barInfo = (BarInfo) this.bars.get(selectedName);
        if (barInfo != null && barInfo.getButton().isSelected()) {
            this.visibleComponent = barInfo.getComponent();
            this.add(visibleComponent, BorderLayout.CENTER);
        }

        this.bottomPanel.removeAll();
        GridLayout bottomLayout = (GridLayout) this.bottomPanel.getLayout();
        bottomLayout.setRows(bottomBars);
        for (int i = 0; i < bottomBars; i++) {
            String barName = (String) itr.next();
            barInfo = (BarInfo) this.bars.get(barName);
            barInfo.getButton().setSelected(false);
            this.bottomPanel.add(barInfo.getButton());
        }
        this.bottomPanel.validate();

        revalidate();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int currentBar = 0;
        for (BarInfo barInfo : this.bars.values()) {
            if (barInfo.getButton() == e.getSource()) {
                if (barInfo.getButton().isSelected()) {
                    this.visibleBar = currentBar;
                } else {
                    this.visibleBar = -1;
                }                       
                getMapaControler().printTag(barInfo.getHexCombat());
                render();
                return;
            }
            currentBar++;
        }
    }

    @Override
    public synchronized void removeAll() {
        if (visibleComponent != null) {
            this.remove(visibleComponent);
            visibleComponent = null;
        }
        this.topPanel.removeAll();
        this.bottomPanel.removeAll();
        for (BarInfo barInfo : bars.values()) {
            barInfo.getButton().removeActionListener(this);
        }
        this.bars.clear();  
        visibleBar = -1;
        this.render();
        
    }

    class BarInfo {

        private String name;
        private final JToggleButton button;
        private final JComponent component;
        private final Local hexCombat;

        public BarInfo(String name, JComponent component, Local hexCombat) {
            this.name = name;
            this.component = component;
            this.button = new JToggleButton(name);
            this.hexCombat = hexCombat;
        }

        public BarInfo(String name, Icon icon, JComponent component, Local hexCombat) {
            this.name = name;
            this.component = component;
            this.button = new JToggleButton(name, icon);
            this.hexCombat = hexCombat;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public JToggleButton getButton() {
            return this.button;
        }

        public JComponent getComponent() {
            return this.component;
        }

        public Local getHexCombat() {
            return hexCombat;
        }
    }
}