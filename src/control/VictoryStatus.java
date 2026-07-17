package control;

import business.facade.CidadeFacade;
import business.facade.LocalFacade;
import business.facade.NacaoFacade;
import business.facade.PointsFacade;
import control.facade.WorldFacadeCounselor;
import java.util.ArrayList;
import java.util.List;
import model.Cidade;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import utils.CounterStringInt;

/**
 * Client-side, read-only assessment of "how is this game won, and how close is each side?" for the
 * Victory Dashboard (see gui.services.VictoryDashboardDialog).
 * <p>
 * Faithful MIRROR of the server's authoritative game-over math in
 * {@code PbmJudge/src/domain/milestones/MilestoneGameOver.java} - keep the two in sync:
 * <ul>
 *   <li>solo/FFA: a side wins a "fraction" condition at &gt;= 50% of the grand total (SOLOFACTOR);</li>
 *   <li>team: a side wins at &gt;= 75% (TEAMFACTOR, the "3:1" rule);</li>
 *   <li>win unit is the TEAM in team games and the NATION in solo (in solo every team flag is "-").</li>
 * </ul>
 * <b>Fog of war.</b> Victory points are authoritative for every nation (the whole VP set is in the EGF),
 * so Score is exact and can be split per rival. Territory is different: every city/hex is VISIBLE, but a
 * city in fog has an UNKNOWN owner ({@code getNacao() == null}). So for Conquest/Domination/City-points we
 * count the full visible total and only what is provably OURS; the rest ("others") is a combined figure -
 * we can NOT attribute it to a specific rival, so no per-rival "at risk / losing" verdict on those. This
 * is why the earlier per-team version was wrong: summing {@code nacao.getCidades()} dropped every fogged
 * city from the total, making the win line far too low (bit game 884: "48 / need 40" with fog cities lost).
 * <p>
 * VDL (Dragonlord) is deferred (Pass 3): the client Personagem model has no isDragon()/isMorto().
 * VVP/VKP are excluded (no enforcement in the milestone; legacy).
 */
public final class VictoryStatus {

    private static final double NEAR = 0.85d;        // a side is "close" at this fraction of its threshold
    private static final float SOLOFACTOR = 0.50f;   // MilestoneGameOver.SOLOFACTOR
    private static final float TEAMFACTOR = 0.75f;   // MilestoneGameOver.TEAMFACTOR

    public enum State {
        WINNING, CLOSE, CONTESTED, AT_RISK, LOSING, NOT_ACTIVE, INFO
    }

    public enum Kind {
        PROGRESS, SURVIVAL, CAPITALS, TURNLIMIT, INFO
    }

    /** A side's standing in an authoritative (per-rival) condition. */
    public static final class Side {
        public final String name;   // team flag or nation name ("-" = barbarians/AI)
        public final int value;
        public final boolean mine;

        Side(String name, int value, boolean mine) {
            this.name = name;
            this.value = value;
            this.mine = mine;
        }
    }

    /** One assessed victory condition; display-ready numbers only (formatting lives in the dialog). */
    public static final class Row {
        public final String code;
        public final Kind kind;
        public final State state;
        public final int activeFromTurn;   // 0 = active now / always
        public final int myValue;          // my side's value (or capitals-lost for CAPITALS)
        public final int threshold;        // value needed to win (or capitals-to-lose for CAPITALS)
        public final int total;            // full visible total (for "X of Y")
        public final int othersValue;      // strongest rival (rivalKnown) or others-combined (!rivalKnown)
        public final String othersName;    // rival team/nation name, or null when combined
        public final boolean rivalKnown;   // true = othersValue is a single rival; false = combined/fogged
        public final boolean territoryBased;
        /** All sides sorted desc (authoritative rows only, for %-breakdown + ahead/behind colour); else null. */
        public List<Side> sides;

        Row(String code, Kind kind, State state, int activeFromTurn, int myValue, int threshold,
                int total, int othersValue, String othersName, boolean rivalKnown, boolean territoryBased) {
            this.code = code;
            this.kind = kind;
            this.state = state;
            this.activeFromTurn = activeFromTurn;
            this.myValue = myValue;
            this.threshold = threshold;
            this.total = total;
            this.othersValue = othersValue;
            this.othersName = othersName;
            this.rivalKnown = rivalKnown;
            this.territoryBased = territoryBased;
        }
    }

    public static final class Assessment {
        public final List<Row> rows = new ArrayList<>();
        /** Active victory-point component flags for this game (subset of the 6 ;VCF;/;VAN;/... codes). */
        public final List<String> vpFlags = new ArrayList<>();
        public int gameId;
        public boolean isTeam;
        public int turno;
        public int turnoMax;
        public boolean battleRoyale;
        public String leaderName;
        public int leaderValue;
        public boolean leaderIsMe;
        public int myTurnLimitValue;
        public int myRank;
        public int nationCount;
    }

    private final WorldFacadeCounselor wfc = WorldFacadeCounselor.getInstance();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final LocalFacade localFacade = new LocalFacade();
    private final PointsFacade pointsFacade = new PointsFacade();

    private boolean isTeam;
    private String myTeam;
    private String myName;

    public Assessment evaluate() {
        final Assessment a = new Assessment();
        final Partida partida = wfc.getPartida();
        a.gameId = partida.getId();
        a.turno = partida.getTurno();
        a.turnoMax = partida.getTurnoMax();
        a.battleRoyale = wfc.isBattleRoyal();
        // client isTeam mirrors PartidaControl.isTeam() = isTeamLocked() || isTeamWithLord() (GLA/GSL)
        a.isTeam = partida.isTeamLocked() || partida.isTeamWithLord();

        final Jogador player = wfc.getJogadorAtivo();
        final Nacao myNation = player.getNacoes().get(player.getNacoes().firstKey());
        this.isTeam = a.isTeam;
        this.myTeam = myNation.getTeamFlag();
        this.myName = myNation.getNome();

        doTurnLimit(a, myNation);

        // Which of the 6 victory-point components are active for this game (Cenario/Partida habilidades).
        // See PbmJudge domain.services.PontosVitoria (categoria array). Lets the Score action name only
        // the sources that actually score - so it never suggests something (e.g. quests) that gives no VP.
        for (String comp : new String[]{";VCF;", ";VAN;", ";VCH;", ";VIM;", ";VID;", ";VGR;"}) {
            if (getHabilidade(comp) != null) {
                a.vpFlags.add(comp);
            }
        }

        // Authoritative (all nations known) -> per-rival verdicts allowed.
        addAuthoritative(a, ";VSP;", scoreStanding());        // Score (victory points)
        if (a.isTeam) {
            addSupremacy(a);                                  // Supremacy (team only) - elimination race
            addCapitalsLost(a);                               // VKC (team only) - your team's losses
        }
        // Territory (fog): count all-visible total vs provably-ours; no per-rival verdict.
        addTerritory(a, ";VSC;", bigCityTally());             // Conquest (burghs & metropolises)
        addTerritory(a, ";VCP;", cityPointsTally());          // Battle Royale (city domination points)
        addTerritory(a, ";VSK;", keyCityTally());             // Domination (key cities)

        addInfo(a, ";VSD;");   // first blood
        addInfo(a, ";VKK;");   // king of kings
        return a;
    }

    // ---- turn-limit default ---------------------------------------------------------------------

    private void doTurnLimit(Assessment a, Nacao myNation) {
        String leader = null;
        int leaderVal = -1, myVal = 0, rank = 1, count = 0;
        for (Nacao nation : wfc.getNacoes().values()) {
            if (!nacaoFacade.isAtivaPC(nation)) {
                continue;
            }
            count++;
            final int val = turnLimitMetric(nation, a.battleRoyale);
            if (nation == myNation) {
                myVal = val;
            }
            if (val > leaderVal) {
                leaderVal = val;
                leader = nation.getNome();
            }
        }
        for (Nacao nation : wfc.getNacoes().values()) {
            if (nacaoFacade.isAtivaPC(nation) && turnLimitMetric(nation, a.battleRoyale) > myVal) {
                rank++;
            }
        }
        a.leaderName = leader;
        a.leaderValue = Math.max(leaderVal, 0);
        a.leaderIsMe = leader != null && leader.equals(myNation.getNome());
        a.myTurnLimitValue = myVal;
        a.myRank = rank;
        a.nationCount = count;
    }

    private int turnLimitMetric(Nacao nation, boolean battleRoyale) {
        return battleRoyale ? nacaoFacade.getPointsDomination(nation) : nacaoFacade.getPointVictory(nation);
    }

    // ---- authoritative (per-unit) conditions: Score, Supremacy ----------------------------------

    /** Per-side buckets for display + the (active-nation) denominator the win threshold uses. */
    private static final class Standing {
        final CounterStringInt counter = new CounterStringInt();
        int raceTotal;   // sum over ACTIVE nations only (matches the server's win math)
    }

    private Standing scoreStanding() {
        // Barbarians/AI can hold victory points that are OUT of the race: show them in the breakdown (so the
        // percentages add up) but exclude them from the win threshold, exactly like MilestoneGameOver
        // (getActiveNations). raceTotal is the active-only total the 50%/75% line is measured against.
        final Standing s = new Standing();
        for (Nacao nation : wfc.getNacoes().values()) {
            final boolean active = nacaoFacade.isAtivaPC(nation);
            final boolean barb = nacaoFacade.isNacaoBarbarian(nation);
            if (!active && !barb) {
                continue;   // skip dead / eliminated nations
            }
            final int vp = nacaoFacade.getPointVictory(nation);
            if (barb && vp <= 0) {
                continue;   // don't show a 0-point barbarian side
            }
            s.counter.add(keyOf(nation), vp);
            if (active) {
                s.raceTotal += vp;
            }
        }
        return s;
    }

    /**
     * Supremacy (team only, ;VSS;): a side wins with &gt;=75% of the ACTIVE nations. Crucially you can't
     * GAIN nations (locked teams don't flip), so this is a survival/elimination race: as enemies die the
     * total shrinks and your fixed count crosses the line. From {@code me >= 0.75*(me + enemy - K)} the
     * enemy eliminations you still need is {@code K = ceil(enemy - me/3)} (e.g. 6v6 -> 4), assuming you
     * lose none. Symmetrically the enemy needs {@code ceil(me - enemy/3)} of yours.
     */
    private void addSupremacy(Assessment a) {
        final Habilidade hab = getHabilidade(";VSS;");
        if (hab == null) {
            return;
        }
        int myAlive = 0, enemyAlive = 0;
        for (Nacao nation : wfc.getNacoes().values()) {
            if (!nacaoFacade.isAtivaPC(nation)) {
                continue;   // barbarians (npc) and dead are already excluded
            }
            if (isMine(nation)) {
                myAlive++;
            } else {
                enemyAlive++;
            }
        }
        final int total = myAlive + enemyAlive;
        final int killsToWin = Math.max(0, (int) Math.ceil(enemyAlive - myAlive / 3.0));
        final int killsToLose = Math.max(0, (int) Math.ceil(myAlive - enemyAlive / 3.0));
        final boolean activeNow = a.turno > hab.getValor();
        final State st;
        if (!activeNow) {
            st = State.NOT_ACTIVE;
        } else if (total <= 0) {
            st = State.CONTESTED;
        } else if (killsToWin == 0) {
            st = State.WINNING;
        } else if (killsToLose == 0) {
            st = State.LOSING;
        } else if (killsToLose <= 1) {
            st = State.AT_RISK;
        } else if (killsToWin <= 1) {
            st = State.CLOSE;
        } else {
            st = State.CONTESTED;
        }
        // myValue=my nations, othersValue=enemy nations, threshold=enemy eliminations needed to win.
        a.rows.add(new Row(";VSS;", Kind.SURVIVAL, st, hab.getValor() + 1, myAlive, killsToWin,
                total, enemyAlive, null, true, false));
    }

    private void addAuthoritative(Assessment a, String code, Standing s) {
        final Habilidade hab = getHabilidade(code);
        if (hab == null) {
            return;
        }
        final int displayTotal = s.counter.getTotal();          // includes out-of-race barbarians (for %)
        final int threshold = (int) Math.ceil(s.raceTotal * factor());
        final int myValue = s.counter.getValue(keyOf());
        // Them = ALL other real sides combined (raceTotal excludes barbarians) - the Us/Them principle, so a
        // 3+ team game reads as "my side vs everyone else". Used for the ahead/behind gap colour.
        final int themCombined = Math.max(0, s.raceTotal - myValue);
        // topRival = strongest single opponent - only for the win/lose verdict (a single team hitting the line).
        int topRival = 0;
        for (String k : s.counter.getKeys()) {
            if (k.equals(keyOf()) || "-".equals(k)) {
                continue;
            }
            if (s.counter.getValue(k) > topRival) {
                topRival = s.counter.getValue(k);
            }
        }
        final boolean activeNow = a.turno > hab.getValor();
        final State state = state(activeNow, s.raceTotal, threshold, myValue, topRival, true);
        final Row row = new Row(code, Kind.PROGRESS, state, hab.getValor() + 1, myValue, threshold,
                displayTotal, themCombined, null, true, false);
        final List<Side> sides = new ArrayList<>();
        for (String k : s.counter.getKeys()) {
            sides.add(new Side(k, s.counter.getValue(k), k.equals(keyOf())));
        }
        sides.sort((x, y) -> Integer.compare(y.value, x.value));
        row.sides = sides;
        a.rows.add(row);
    }

    // ---- territory (fog-aware) conditions: Conquest, City points, Domination --------------------

    /** A [mine, total] tally over all VISIBLE map objects; fogged (null-owner) objects count toward total. */
    private static final class Tally {
        int mine;
        int total;
    }

    private Tally bigCityTally() {
        final Tally t = new Tally();
        for (Cidade city : wfc.getCidades()) {
            if (!cidadeFacade.isBigCity(city)) {
                continue;
            }
            t.total++;
            if (isMine(city.getNacao())) {
                t.mine++;
            }
        }
        return t;
    }

    private Tally keyCityTally() {
        final Tally t = new Tally();
        for (Local hex : wfc.getLocais().values()) {
            if (!localFacade.isKeyLocalCity(hex)) {
                continue;
            }
            t.total++;
            if (isMine(ownerOf(hex))) {
                t.mine++;
            }
        }
        return t;
    }

    private Tally cityPointsTally() {
        final Tally t = new Tally();
        for (Local hex : wfc.getLocais().values()) {
            final int pts = cidadeFacade.getPointsDomination(hex);
            if (pts <= 0) {
                continue;
            }
            t.total += pts;
            if (isMine(ownerOf(hex))) {
                t.mine += pts;
            }
        }
        return t;
    }

    private void addTerritory(Assessment a, String code, Tally t) {
        final Habilidade hab = getHabilidade(code);
        if (hab == null) {
            return;
        }
        final int threshold = (int) Math.ceil(t.total * factor());
        final int others = t.total - t.mine;
        final boolean activeNow = a.turno > hab.getValor();
        // rivalKnown = false: "others" is combined and partly fogged, so no AT_RISK/LOSING verdict.
        final State state = state(activeNow, t.total, threshold, t.mine, others, false);
        a.rows.add(new Row(code, Kind.PROGRESS, state, hab.getValor() + 1, t.mine, threshold,
                t.total, others, null, false, true));
    }

    // ---- VKC: your team's capital (key-city) losses (knowable side only) ------------------------

    private void addCapitalsLost(Assessment a) {
        final Habilidade hab = getHabilidade(";VKC;");
        if (hab == null) {
            return;
        }
        final int limitLost = hab.getValor();
        // per-nation key cities still held (owners we can see); a nation of ours showing 0 has lost them all.
        final CounterStringInt owned = pointsFacade.doVictoryDomination(wfc.getLocais().values(), wfc.getNacaoNeutra());
        int myLost = 0;
        for (Nacao nation : wfc.getNacoes().values()) {
            if (!myTeam.equals(nation.getTeamFlag())) {
                continue;
            }
            if (owned.getValue(nation.getNome()) == 0) {
                myLost++;
            }
        }
        final State state;
        if (myLost >= limitLost) {
            state = State.LOSING;
        } else if (limitLost - myLost <= 1) {
            state = State.AT_RISK;
        } else {
            state = State.CONTESTED;
        }
        // total/others not meaningful for VKC; keep 0.
        a.rows.add(new Row(";VKC;", Kind.CAPITALS, state, hab.getValor() + 1, myLost, limitLost,
                0, 0, null, false, true));
    }

    private void addInfo(Assessment a, String code) {
        final Habilidade hab = getHabilidade(code);
        if (hab == null) {
            return;
        }
        final boolean activeNow = a.turno > hab.getValor();
        a.rows.add(new Row(code, Kind.INFO, activeNow ? State.INFO : State.NOT_ACTIVE,
                hab.getValor() + 1, 0, 0, 0, 0, null, false, false));
    }

    // ---- shared helpers -------------------------------------------------------------------------

    private State state(boolean activeNow, int total, int threshold, int myValue, int oppValue,
            boolean rivalKnown) {
        if (!activeNow) {
            return State.NOT_ACTIVE;
        }
        if (total <= 0) {
            return State.CONTESTED;
        }
        if (myValue >= threshold) {
            return State.WINNING;
        }
        if (rivalKnown && oppValue >= threshold) {
            return State.LOSING;
        }
        if (myValue >= threshold * NEAR) {
            return State.CLOSE;
        }
        if (rivalKnown && oppValue >= threshold * NEAR) {
            return State.AT_RISK;
        }
        return State.CONTESTED;
    }

    private double factor() {
        return isTeam ? TEAMFACTOR : SOLOFACTOR;
    }

    private boolean isMine(Nacao owner) {
        if (owner == null) {
            return false;  // fogged / unknown owner -> not ours
        }
        return isTeam ? myTeam.equals(owner.getTeamFlag()) : myName.equals(owner.getNome());
    }

    private Nacao ownerOf(Local hex) {
        return (localFacade.isCidade(hex) && hex.getCidade() != null) ? hex.getCidade().getNacao() : null;
    }

    private String keyOf() {
        return isTeam ? myTeam : myName;
    }

    private String keyOf(Nacao nation) {
        return isTeam ? nation.getTeamFlag() : nation.getNome();
    }

    private Habilidade getHabilidade(String code) {
        final Habilidade hab = wfc.getPartida().getHabilidades().get(code);
        if (hab != null) {
            return hab;
        }
        return wfc.getCenario().getHabilidades().get(code);
    }
}
