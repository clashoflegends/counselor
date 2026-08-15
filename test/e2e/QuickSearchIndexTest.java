package e2e;

import baseLib.GenericoTableModel;
import gui.services.QuickSearchIndex;
import gui.services.QuickSearchIndex.Entry;
import java.util.ArrayList;
import java.util.List;
import model.Local;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quick search (Ctrl+P) is a navigation aid, so it fails softly: a bad rank or a dropped row just
 * means the player cannot find the thing they typed, with no error anywhere. These pin the behaviour
 * that would otherwise only surface as a complaint - what gets indexed, and the ranking that decides
 * whether the row you meant is first or buried under a few hundred incidental matches.
 *
 * <p>Drives {@link QuickSearchIndex} directly against table models rather than through the dialog:
 * the models are what the real index reads, and this keeps the test headless.
 */
class QuickSearchIndexTest {

    private static List<Entry> index;

    /** Columns mirroring a real data tab: name, an order, the hex, a skill, the nation. */
    private static final String[] COLS = {"Name", "Action 1", "Local", "Duel", "Nation"};
    private static final Class[] CLASSES = {String.class, String.class, Local.class, Integer.class, String.class};

    @BeforeAll
    static void buildIndex() {
        index = new ArrayList<>();
        QuickSearchIndex.indexTable(index, 0, "Characters", new GenericoTableModel(COLS, new Object[][]{
            {"Daemon Targaryen", "Move", hex("1122"), 85, "Blacks"},
            {"Rhaenyra Targaryen", "Recruit troops", hex("0517"), 40, "Blacks"},
            {"Aemond Targaryen", "Move", hex("1122"), 85, "Greens"},
            {"Ser Criston Cole", "Duel a character", hex("0904"), 72, "Greens"}
        }, CLASSES));
        QuickSearchIndex.indexTable(index, 1, "Cities", new GenericoTableModel(COLS, new Object[][]{
            {"Kings Landing", "Build fortification", hex("0904"), 5, "Greens"},
            {"Dragonstone", "-", hex("1122"), 4, "Blacks"}
        }, CLASSES));
    }

    private static Local hex(String coord) {
        final Local l = new Local();
        l.setCoordenadas(coord); //Local.toString() is the coordinate - that is what makes hexes searchable
        return l;
    }

    @Test
    void indexesEveryRowOfEveryTab() {
        assertEquals(6, index.size(), "every model row of both tabs should be indexed");
        assertEquals("Daemon Targaryen", index.get(0).getName());
        assertEquals("Characters", index.get(0).getTabTitle());
        assertEquals("Cities", index.get(4).getTabTitle());
        assertEquals(0, index.get(4).getModelRow(), "model row is per-tab, not global");
    }

    @Test
    void capturesTheHexFromTheLocalColumn() {
        assertEquals("1122", index.get(0).getCoord());
        assertEquals("0904", index.get(3).getCoord());
    }

    @Test
    void skipsTheBlankPlaceholderRowOfAnEmptyTab() {
        //An empty tab's converter emits one all-blank row; indexing it would put a nameless entry in
        //every result list.
        final List<Entry> out = new ArrayList<>();
        QuickSearchIndex.indexTable(out, 0, "Armies", new GenericoTableModel(COLS,
                new Object[][]{{"", "", null, null, ""}}, CLASSES));
        assertTrue(out.isEmpty());
    }

    @Test
    void findsByPartialName() {
        final List<Entry> hits = QuickSearchIndex.match(index, "daem", 50);
        assertEquals(1, hits.size());
        assertEquals("Daemon Targaryen", hits.get(0).getName());
    }

    @Test
    void findsByHexCoordinate() {
        final List<Entry> hits = QuickSearchIndex.match(index, "1122", 50);
        assertEquals(3, hits.size(), "two characters and a city sit on 1122");
        for (Entry e : hits) {
            assertEquals("1122", e.getCoord());
        }
    }

    @Test
    void findsByOrderName() {
        final List<Entry> hits = QuickSearchIndex.match(index, "recruit", 50);
        assertEquals(1, hits.size());
        assertEquals("Rhaenyra Targaryen", hits.get(0).getName());
    }

    @Test
    void everyTokenMustMatchSoTwoTermsNarrow() {
        assertEquals(3, QuickSearchIndex.match(index, "targaryen", 50).size(), "three Targaryens");
        final List<Entry> hits = QuickSearchIndex.match(index, "targaryen greens", 50);
        assertEquals(1, hits.size(), "the second token must narrow, not widen");
        assertEquals("Aemond Targaryen", hits.get(0).getName());
    }

    @Test
    void nameMatchesOutrankIncidentalColumnMatches() {
        //"cole" is a name; nothing else carries it. "85" is a skill on two rows and a name on none -
        //the point is that when a query hits BOTH a name and other columns, the name comes first.
        final List<Entry> hits = QuickSearchIndex.match(index, "dragonstone", 50);
        assertEquals(1, hits.size());
        assertEquals("Dragonstone", hits.get(0).getName());

        final List<Entry> mixed = QuickSearchIndex.match(index, "greens", 50);
        assertFalse(mixed.isEmpty());
        //Nation column only - no name carries "greens" - so all three land in the catch-all bucket
        //and stay findable rather than being dropped.
        assertEquals(3, mixed.size());
    }

    @Test
    void anExactHexRanksAboveEverythingElse() {
        //"0904" is the hex of Ser Criston Cole and of Kings Landing; both are exact-coordinate hits and
        //must come before any row that merely contains the digits elsewhere.
        final List<Entry> hits = QuickSearchIndex.match(index, "0904", 50);
        assertEquals(2, hits.size());
        assertEquals("0904", hits.get(0).getCoord());
        assertEquals("0904", hits.get(1).getCoord());
    }

    @Test
    void blankQueryMatchesNothing() {
        assertTrue(QuickSearchIndex.match(index, "", 50).isEmpty());
        assertTrue(QuickSearchIndex.match(index, "   ", 50).isEmpty());
        assertTrue(QuickSearchIndex.match(index, null, 50).isEmpty());
    }

    @Test
    void respectsTheDisplayCapButStillReportsTheRealTotal() {
        //The dialog shows "... and N more" off countMatches, so the cap must not distort the count.
        final List<Entry> capped = QuickSearchIndex.match(index, "targaryen", 2);
        assertEquals(2, capped.size());
        assertEquals(3, QuickSearchIndex.countMatches(index, "targaryen"));
    }

    @Test
    void searchIsCaseInsensitive() {
        final List<Entry> upper = QuickSearchIndex.match(index, "DAEMON", 50);
        final List<Entry> lower = QuickSearchIndex.match(index, "daemon", 50);
        assertEquals(1, upper.size());
        assertEquals(lower.get(0).getName(), upper.get(0).getName());
    }

    @Test
    void theRenderedRowNamesTheEntityItsHexAndItsTab() {
        final String html = index.get(0).toString();
        assertNotNull(html);
        assertTrue(html.contains("Daemon Targaryen"), html);
        assertTrue(html.contains("1122"), html);
        assertTrue(html.contains("Characters"), html);
    }
}
