package e2e;

import baseLib.GenericoTableModel;
import gui.services.QuickSearchIndex;
import gui.services.QuickSearchIndex.Entry;
import java.util.ArrayList;
import java.util.List;
import model.Local;
import model.Personagem;
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
        //The fixture is built so that a query can land in DIFFERENT ranking buckets at once, which is
        //the only way the bucket order is actually under test: "green" hits one NAME (Greenstone) and
        //three nation columns; "0904" is the hex of two rows and merely appears in a third's order.
        QuickSearchIndex.indexTable(index, 0, "Characters", new GenericoTableModel(COLS, new Object[][]{
            {"Daemon Targaryen", "Move", hex("1122"), 85, "Blacks"},
            {"Rhaenyra Targaryen", "Recruit troops", hex("0517"), 40, "Blacks"},
            {"Aemond Targaryen", "Move", hex("1122"), 85, "Greens"},
            {"Ser Criston Cole", "Duel a character", hex("0904"), 72, "Greens"},
            {"Elenda Baratheon", "Move to 0904", hex("1531"), 30, "Blacks"}
        }, CLASSES));
        QuickSearchIndex.indexTable(index, 1, "Cities", new GenericoTableModel(COLS, new Object[][]{
            {"Kings Landing", "Build fortification", hex("0904"), 5, "Greens"},
            {"Dragonstone", "-", hex("1122"), 4, "Blacks"},
            {"Greenstone", "Build fortification", hex("1531"), 3, "Blacks"}
        }, CLASSES));
    }

    private static Local hex(String coord) {
        final Local l = new Local();
        l.setCoordenadas(coord); //Local.toString() is the coordinate - that is what makes hexes searchable
        return l;
    }

    @Test
    void indexesEveryRowOfEveryTab() {
        assertEquals(8, index.size(), "every model row of both tabs should be indexed");
        assertEquals("Daemon Targaryen", index.get(0).getName());
        assertEquals("Characters", index.get(0).getTabTitle());
        assertEquals("Kings Landing", index.get(5).getName());
        assertEquals("Cities", index.get(5).getTabTitle());
        assertEquals(0, index.get(5).getModelRow(), "model row is per-tab, not global");
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
        //"green" is the NAME of one city and the NATION of three other rows. Without the name-above-
        //the-rest split, the city the player is obviously after sorts wherever the index happened to
        //put it - here, dead last. This is the property that silently degrades.
        final List<Entry> hits = QuickSearchIndex.match(index, "green", 50);
        assertEquals(4, hits.size(), "one name match plus three nation-column matches");
        assertEquals("Greenstone", hits.get(0).getName(), "the name match must come first");
        for (int i = 1; i < hits.size(); i++) {
            assertFalse(hits.get(i).getName().toLowerCase().contains("green"),
                    "the remaining hits match on a non-name column only");
        }
    }

    @Test
    void anExactHexRanksAboveARowThatMerelyMentionsIt() {
        //Two rows STAND on 0904; a third only carries it in its order text ("Move to 0904"). Someone
        //typing a coordinate wants what is there, not what is heading there.
        final List<Entry> hits = QuickSearchIndex.match(index, "0904", 50);
        assertEquals(3, hits.size());
        assertEquals("0904", hits.get(0).getCoord());
        assertEquals("0904", hits.get(1).getCoord());
        assertEquals("Elenda Baratheon", hits.get(2).getName(), "the mention ranks last");
        assertFalse(hits.get(2).getCoord().equals("0904"));
    }

    @Test
    void blankQueryMatchesNothing() {
        assertTrue(QuickSearchIndex.match(index, "", 50).isEmpty());
        assertTrue(QuickSearchIndex.match(index, "   ", 50).isEmpty());
        assertTrue(QuickSearchIndex.match(index, null, 50).isEmpty());
    }

    @Test
    void respectsTheDisplayCapButStillReportsTheRealTotal() {
        //The dialog shows "... and N more" from the same single pass, so the cap must not distort it.
        final QuickSearchIndex.Matches m = QuickSearchIndex.search(index, "targaryen", 2);
        assertEquals(2, m.getHits().size(), "capped for display");
        assertEquals(3, m.getTotal(), "but the real total is reported");
    }

    @Test
    void indexesTheTurnResultTextOfTheRowsEntity() {
        //Result text lives on the model object, not in any cell - it is the reason the index needs a
        //parallel actor list at all. Searching the narrative is the point ("who was ambushed?").
        final Personagem pc = new Personagem();
        pc.setNome("Harwin Strong");
        pc.setResultados("Ambushed on the kingsroad and left for dead.");
        final List<Entry> out = new ArrayList<>();
        QuickSearchIndex.indexTable(out, 0, "Characters", new GenericoTableModel(COLS, new Object[][]{
            {"Harwin Strong", "Move", hex("1122"), 60, "Blacks"}
        }, CLASSES), java.util.Collections.singletonList(pc));

        assertEquals(1, out.size());
        assertEquals(1, QuickSearchIndex.match(out, "ambushed", 50).size(),
                "a word that appears only in the turn result must find the row");
        assertEquals("Harwin Strong", QuickSearchIndex.match(out, "kingsroad", 50).get(0).getName());
    }

    @Test
    void aTabWithNoActorListStillIndexesItsCells() {
        //Catalogue tabs (Actions, Troops, Game) have no backing entity list; they must not be dropped.
        final List<Entry> out = new ArrayList<>();
        QuickSearchIndex.indexTable(out, 0, "Actions", new GenericoTableModel(COLS, new Object[][]{
            {"Build fortification", "-", hex("0101"), 1, "-"}
        }, CLASSES), null);
        assertEquals(1, out.size());
        assertEquals(1, QuickSearchIndex.match(out, "fortification", 50).size());
    }

    @Test
    void aShortActorListDoesNotBreakTheRemainingRows() {
        //Defensive: the actor list is only as parallel as the tab keeps it. A mismatch must cost the
        //result text for those rows, not the rows themselves.
        final List<Entry> out = new ArrayList<>();
        QuickSearchIndex.indexTable(out, 0, "Characters", new GenericoTableModel(COLS, new Object[][]{
            {"Alpha", "Move", hex("0101"), 1, "Blacks"},
            {"Beta", "Move", hex("0202"), 2, "Blacks"}
        }, CLASSES), java.util.Collections.emptyList());
        assertEquals(2, out.size(), "rows survive an actor list that is too short");
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
        final String row = index.get(0).toString();
        assertNotNull(row);
        assertTrue(row.contains("Daemon Targaryen"), row);
        assertTrue(row.contains("1122"), row);
        assertTrue(row.contains("Characters"), row);
        //Plain text, never HTML: a JList measures every row, and measuring HTML is ~130x dearer -
        //that was the whole of the original typing lag. Guard against it creeping back.
        assertFalse(row.toLowerCase().contains("<html"), row);
    }
}
