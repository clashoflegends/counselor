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
    // Line-chart styling (VP history): the active player's own nation is emphasised (thicker + markers);
    // enemy nations (not on the player's side) are drawn dashed - colour = identity, stroke = allegiance.
    private boolean emphasis = false;
    private boolean dashed = false;

    public DataSetForChart(String key, double value, String grouping, Color color) {
        this.key = key;
        this.value = value;
        this.grouping = grouping;
        if (color != null) {
            this.color = color;
        }
    }

    public boolean isEmphasis() {
        return emphasis;
    }

    public DataSetForChart setEmphasis(boolean emphasis) {
        this.emphasis = emphasis;
        return this;
    }

    public boolean isDashed() {
        return dashed;
    }

    public DataSetForChart setDashed(boolean dashed) {
        this.dashed = dashed;
        return this;
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
