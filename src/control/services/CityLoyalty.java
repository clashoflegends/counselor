/*
 * CityLoyalty.java
 */
package control.services;

import java.io.Serializable;

/**
 * Cell value for the city-loyalty column of the cities table. Carries the displayed loyalty together
 * with the pre-computed loyalty-reduction threshold, so {@code gui.services.CityLoyaltyTableCellRenderer}
 * can flag cities at risk of a size reduction without having to resolve the {@code Cidade} at render time.
 *
 * The threshold is the raw-loyalty value below which the city risks a reduction (already net of any
 * nation loyalty bonus) - see PbmJudge {@code MilestoneProducaoBase.doCidadeTestFlip}. Sorts and
 * displays by the raw loyalty value, so the column behaves like the plain Integer it replaced.
 *
 * @author jmoura
 */
public class CityLoyalty implements Comparable<CityLoyalty>, Serializable {

    private final int loyalty;
    private final int threshold;
    private final boolean eligible;

    public CityLoyalty(int loyalty, int threshold, boolean eligible) {
        this.loyalty = loyalty;
        this.threshold = threshold;
        this.eligible = eligible;
    }

    public int getLoyalty() {
        return loyalty;
    }

    /**
     * Raw-loyalty value below which the city risks a reduction (already net of any nation loyalty bonus).
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * False for cities that cannot flip on loyalty (capital) or whose loyalty is unknown (loyalty &lt;= 0,
     * e.g. enemy cities) - those are never colored.
     */
    public boolean isEligible() {
        return eligible;
    }

    @Override
    public int compareTo(CityLoyalty o) {
        return Integer.compare(this.loyalty, o.loyalty);
    }

    @Override
    public String toString() {
        return Integer.toString(loyalty);
    }
}
