/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.charts;

import java.awt.Color;
import java.io.Serializable;

/**
 *
 * @author John
 */
public class DataSetForChart implements Serializable {

    private final String key;
    private final double value;
    private final String grouping;
    private Color color = Color.MAGENTA;

    public DataSetForChart(String key, double value, String grouping, Color color) {
        this.key = key;
        this.value = value;
        this.grouping = grouping;
        if (color != null) {
            this.color = color;
        }
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * @return the value
     */
    public String getGrouping() {
        return grouping;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

}
