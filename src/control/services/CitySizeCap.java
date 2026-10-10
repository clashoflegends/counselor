/*
 * CitySizeCap.java
 */
package control.services;

import java.io.Serializable;
import model.Local;

/**
 * Cell value for the city-size column of the cities table. Carries the size name that the column has
 * always displayed, plus the numeric size it sorts by, plus the hex of the neighbouring city that
 * currently caps this one's growth under {@code ;SCC;} (null when nothing does).
 *
 * The blocker is resolved once per table build by the shared {@code CidadeFacade.isCheckCitySizeCapToUpgrade}
 * rule - the same call the Judge makes - so the client can never disagree with the turn result about
 * WHY an upgrade was refused. {@code gui.services.CitySizeCapTableCellRenderer} only reads the result.
 *
 * <b>Only ever evaluated for the active player's own cities.</b> The rule reads nation flags
 * ({@code ;PKW;} for the Night's Watch Wall, {@code ;PIC;} for the Greyjoy range reduction), and the
 * EGF carries a nation's habilidades only for the nations the player owns - a foreign nation arrives
 * with an empty or partial map, so the same call on an enemy city would silently answer with the
 * exemptions missing. Verified on game 915: only the Night's Watch player's own file carries {@code ;PKW;}.
 *
 * <b>The absence of a blocker is not a promise.</b> A big city flagged {@code oculto} is dropped from
 * the EGF, so the client can miss one. This is why the renderer marks only the blocked state and never
 * paints a positive "can upgrade" one: a mark is certain, its absence is not.
 *
 * Sorts by the numeric size (the column used to be a bare String declared {@code Integer.class}, so it
 * sorted alphabetically - Burgh, Camp, City, Metropolis, Town).
 *
 * @author jmoura
 */
public class CitySizeCap implements Comparable<CitySizeCap>, Serializable {

    private final int size;
    private final String sizeName;
    private final Local blocker;

    public CitySizeCap(int size, String sizeName, Local blocker) {
        this.size = size;
        this.sizeName = sizeName == null ? "" : sizeName;
        this.blocker = blocker;
    }

    public int getSize() {
        return size;
    }

    /**
     * The neighbouring city's hex that caps this city's growth, or null when the rule found no reason
     * to deny - which, per the class javadoc, includes "the rule was not evaluated for this city".
     */
    public Local getBlocker() {
        return blocker;
    }

    /** True when a visible neighbour currently caps this city's growth. */
    public boolean isBlocked() {
        return blocker != null;
    }

    @Override
    public int compareTo(CitySizeCap o) {
        final int bySize = Integer.compare(this.size, o.size);
        return bySize != 0 ? bySize : this.sizeName.compareToIgnoreCase(o.sizeName);
    }

    @Override
    public String toString() {
        return sizeName;
    }
}
