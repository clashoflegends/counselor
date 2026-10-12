/*
 * CityThreat.java
 */
package control.services;

import control.services.ThreatReach.Threat;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Cell value for the Threat column of the cities table: how soon a hostile force could be standing on
 * this city, and which forces those are.
 *
 * <b>Marks a threat, never safety.</b> {@code 0} means nothing in the player's EGF reaches this hex,
 * which is not the same as nothing reaching it - an army he has not scouted is not in the file at
 * all. That is the same one-directional contract {@link CitySizeCap} keeps, and the renderer honours
 * it by colouring only the threatened rows.
 *
 * <b>Own cities only.</b> Not for secrecy - every hostile army in the file is visible to the whole
 * computation - but because the column answers "what do I have to defend", and a row of threat
 * numbers against foreign cities is noise that would also make the common case (sort by soonest, see
 * your own front line) useless.
 *
 * Sorts soonest first, with the unthreatened rows last rather than first: {@code 0} would otherwise
 * sort ahead of {@code 1} and bury the rows the player opened the column for. That is the ASCENDING
 * contract; a second click reverses it and puts the blank rows on top, which is what reversing a
 * sort means and is left alone.
 *
 * @author jmoura
 */
public class CityThreat implements Comparable<CityThreat>, Serializable {

    private final int turns;
    private final List<Threat> threats;

    public CityThreat(int turns, List<Threat> threats) {
        this.turns = turns;
        this.threats = threats == null ? Collections.<Threat>emptyList() : threats;
    }

    /** 1 or 2, or 0 when nothing in the file reaches this hex. */
    public int getTurns() {
        return turns;
    }

    public List<Threat> getThreats() {
        return threats;
    }

    public boolean isThreatened() {
        return turns > 0;
    }

    @Override
    public int compareTo(CityThreat other) {
        //unthreatened last, so sorting the column puts the front line at the top
        final int mine = turns == 0 ? Integer.MAX_VALUE : turns;
        final int theirs = other.turns == 0 ? Integer.MAX_VALUE : other.turns;
        return Integer.compare(mine, theirs);
    }

    @Override
    public String toString() {
        return turns == 0 ? "" : String.valueOf(turns);
    }
}
