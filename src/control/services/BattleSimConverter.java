package control.services;

import business.combat.ArmySim;
import business.combat.CombatLayer;
import business.combat.CombatLevel;
import business.combat.CasualtyMode;
import business.combat.CityCombatResolver;
import business.combat.CombatResult;
import business.combat.CombatScenario;
import business.combat.LayerParticipation;
import business.combat.RosterDerivation;
import business.combat.RunGate;
import business.combat.ScenarioRoster;
import java.util.ArrayList;
import business.facade.ExercitoFacade;
import java.util.List;
import java.util.Map;
import model.Cenario;
import model.Cidade;
import model.Nacao;
import model.Pelotao;
import msgs.BaseMsgs;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * Everything BattleSim puts on screen as words.
 *
 * The model side answers in enums and counts on purpose - {@code ScenarioRoster} says
 * FIGHTING_AGAINST_ME, {@code RosterDerivation} says PARTLY_ASSUMED and a number - so that the rules
 * can be tested without a bundle and without a window. This is where those become a sentence, and it
 * is the only place in the BattleSim that touches {@code labels.properties}.
 *
 * Every string comes from the bundle. A hardcoded English word here would be invisible until a
 * Portuguese player opened the window.
 */
public class BattleSimConverter {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    private BattleSimConverter() {
    }

    /**
     * A nation node's title: who it is, how many troops it has here, and who it fights.
     *
     * "House Lannister (4,332)  vs House Tyrell". The enemy list is the half that stops
     * this being a step backwards: the four groups it replaces were the ONLY place in the window
     * that said who fights whom, and nothing else shows it - the army editor answers per layer, and
     * the matrix is behind the Diplomacy button. Stated directly now rather than relative to the
     * player, so it reads the same whether or not he has an army on the hex.
     *
     * A nation with no enemies here gets no clause at all rather than "vs nobody", which is the
     * common case on a quiet hex and does not need a sentence.
     *
     * Kept SHORT on purpose: the first draft read "- at war with House Tyrell" and truncated the
     * top row of the tree, which then grew a horizontal scrollbar. A tree node has whatever width
     * the split gives it and no more.
     */
    public static String getNacaoTitle(ScenarioRoster roster, Nacao nacao) {
        final String name = nacao == null
                ? labels.getString("BATTLESIM.NACAO.UNKNOWN") : nacao.getNome();
        final String ret = String.format("%s (%,d)", name, roster.getQtTropas(nacao));
        final List<Nacao> foes = roster.getEnemies(nacao);
        if (foes.isEmpty()) {
            return ret;
        }
        final StringBuilder named = new StringBuilder();
        for (Nacao one : foes) {
            named.append(named.length() == 0 ? "" : ", ").append(one.getNome());
        }
        // the separator is CODE, not part of the label: Properties.load strips leading whitespace,
        // so a label written as "  -  at war with %s" silently loses its gap in every language
        return ret + "  "
                + String.format(labels.getString("BATTLESIM.NACAO.ATWAR"), named.toString());
    }

    public static String getLayerName(CombatLayer layer) {
        switch (layer) {
            case NAVY:
                return labels.getString("BATTLESIM.LAYER.NAVY");
            case ARMY:
                return labels.getString("BATTLESIM.LAYER.ARMY");
            default:
                return labels.getString("BATTLESIM.LAYER.CITY");
        }
    }

    /**
     * The army editor's participation block: a summary line, then ALL THREE layers, each answered.
     *
     * <h3>Three answers, never one</h3>
     *
     * John, 2026-09-19: "The participation is layer by layer. N A C. There are enough edge cases
     * that there is always a situation that someone will participate of 0 to 3 of them."
     *
     * This used to print one reason for the whole army - the first non-FIGHTS answer, asked of the
     * land layer first - and that was a true sentence standing in for three different ones. Live
     * example from hex 1141: four Greyjoy armies sitting on a Tully city read "Takes no part in
     * this battle (no enemy present)". Perfectly true of the LAND layer, and it buried the answer
     * the player actually needed - the city layer said "not ordered to assault", which he fixes by
     * changing Combat level to Attack city. The one thing the line exists to tell him was the one
     * thing it hid.
     *
     * So every layer states its own case, in the N A C order the Judge resolves them, including the
     * ones it fights in. An army in no layer is still the case a player opens the simulator to ask
     * about - a fleet that will not defend his city, a garrison that will not sortie - and now he
     * sees which of the three tests stopped it and can act on that one.
     */
    public static String getFightsIn(LayerParticipation participation) {
        if (participation == null) {
            return "";
        }
        // HTML, because the answer is four short lines rather than one long one.
        final List<CombatLayer> layers = participation.getLayers();
        final StringBuilder ret = new StringBuilder("<html>");
        if (layers.isEmpty()) {
            ret.append(labels.getString("BATTLESIM.FIGHTS.NONE"));
        } else {
            final StringBuilder named = new StringBuilder();
            for (CombatLayer layer : layers) {
                if (named.length() > 0) {
                    named.append(", ");
                }
                named.append(getLayerName(layer));
            }
            ret.append(String.format(labels.getString("BATTLESIM.FIGHTS.IN"), named.toString()));
        }
        // then ALL THREE layers, in the N A C order the Judge resolves them, each with its own
        // answer. Never one sentence for the whole army: see the method javadoc.
        for (CombatLayer layer : new CombatLayer[]{CombatLayer.NAVY, CombatLayer.ARMY,
            CombatLayer.CITY}) {
            ret.append("<br>&nbsp;&nbsp;").append(getLayerName(layer)).append(": ")
                    .append(getReasonName(participation.getReason(layer)));
        }
        // and, when one of those three answers is something the PLAYER can undo, what to do about
        // it. Naming the test that stopped an army was only ever half the sentence: at 906 t3 hex
        // 0452 an unscouted fleet said "land: no troops visible in it" three times over and the
        // player still had to work out on his own that the cure is to type the composition.
        final String fix = getFightsInFix(participation);
        if (!fix.isEmpty()) {
            ret.append("<br>").append(fix);
        }
        return ret.append("</html>").toString();
    }

    /**
     * The one thing to DO about this army, or empty when nothing it says is the player's to fix.
     *
     * Deliberately ONE line for the army rather than one per layer: three instructions on a panel
     * that already carries three diagnoses is a wall, and the reasons are ordered here by how
     * completely they block the army. An army with nothing in it cannot fight in ANY layer, so that
     * instruction outranks a city-assault order it also is not carrying.
     *
     * Whitelisted through a switch rather than looked up as {@code "BATTLESIM.FIX." + reason
     * .name()}: a missing key renders as a visible placeholder rather than as nothing, so a reason
     * with no cure would print a bug on the panel. Adding a cure means adding it in both places,
     * which is the point - it should not be possible to half-add one.
     */
    private static String getFightsInFix(LayerParticipation participation) {
        final LayerParticipation.Reason[] ordered = {
            reasonOf(participation, CombatLayer.ARMY),
            reasonOf(participation, CombatLayer.NAVY),
            reasonOf(participation, CombatLayer.CITY)};
        for (LayerParticipation.Reason reason : ordered) {
            if (reason == LayerParticipation.Reason.NO_TROOPS) {
                return labels.getString("BATTLESIM.FIX.NO_TROOPS");
            }
        }
        for (LayerParticipation.Reason reason : ordered) {
            if (reason == null) {
                continue;
            }
            switch (reason) {
                case CARRIES_NO_TROOPS:
                    return labels.getString("BATTLESIM.FIX.CARRIES_NO_TROOPS");
                case WILL_NOT_ASSAULT_CITY:
                    return labels.getString("BATTLESIM.FIX.WILL_NOT_ASSAULT_CITY");
                case NOT_HOSTILE_TO_CITY:
                    return labels.getString("BATTLESIM.FIX.NOT_HOSTILE_TO_CITY");
                default:
                    break;
            }
        }
        return "";
    }

    private static LayerParticipation.Reason reasonOf(LayerParticipation participation,
            CombatLayer layer) {
        return participation == null ? null : participation.getReason(layer);
    }

    /** One layer's answer: "takes part", or the single test that stopped it. */
    private static String getReasonName(LayerParticipation.Reason reason) {
        if (reason == null || reason == LayerParticipation.Reason.FIGHTS) {
            return labels.getString("BATTLESIM.FIGHTS.YES");
        }
        return labels.getString("BATTLESIM.REASON." + reason.name());
    }

    public static String getCombatLevelName(CombatLevel level) {
        return labels.getString("BATTLESIM.LEVEL." + level.name());
    }

    public static String getProvenanceName(CombatScenario.Provenance provenance) {
        return labels.getString("BATTLESIM.PROVENANCE." + provenance.name());
    }

    /**
     * One of the seven relationship steps, named: "At war enemy", "Ally", "Lord"...
     *
     * From {@code BaseMsgs.nacaoRelacionamento}, the array the rest of the game already reads with
     * the same {@code + 2} offset, so the diplomacy panel invents no vocabulary of its own and a
     * translator has nothing new to translate. Clamped rather than trusted: the array is seven long
     * and the value arrives from an EGF, and an out-of-range index here would take the panel down
     * instead of showing a row.
     */
    public static String getRelationshipName(int valor) {
        final int index = Math.max(0, Math.min(BaseMsgs.nacaoRelacionamento.length - 1, valor + 2));
        return BaseMsgs.nacaoRelacionamento[index];
    }

    /**
     * The status bar: how this scenario decided who fights whom, and how much of it was guessed.
     *
     * R-15 lives here. A guessed peace looks exactly like a known one once it reaches a number, so
     * the count of unresolved pairs is stated rather than left to a tooltip - and the player's own
     * edits are stated too, because a scenario he has adjusted is no longer the one his EGF
     * describes and he should not read the result as if it were.
     */
    public static String getDerivationText(CombatScenario scenario) {
        final RosterDerivation derivation = RosterDerivation.of(scenario);
        final StringBuilder ret = new StringBuilder();
        // Saying "read from your EGF" says nothing: the EGF is the ONLY source of information the
        // Counselor has, so every number on this screen came from it. The status bar is for what
        // the player could NOT be told - pairs nothing could resolve, and his own overrides. When
        // there is neither, it stays quiet rather than reassuring him about the obvious.
        if (derivation.getAssumedPairs() > 0) {
            ret.append(String.format(labels.getString("BATTLESIM.STATUS.ASSUMED"),
                    derivation.getAssumedPairs()));
        }
        final int edited = scenario == null ? 0 : scenario.getEditedCount();
        if (edited > 0) {
            // The gap between the two sentences is HERE and nowhere else. BATTLESIM.STATUS.EDITED
            // used to open with a literal space in all five files, which Properties.load strips
            // before anyone sees it - so it bought nothing, and had it ever worked it would have
            // indented the sentence when it appears alone, which is the usual case.
            if (ret.length() > 0) {
                ret.append("   ");
            }
            ret.append(String.format(labels.getString("BATTLESIM.STATUS.EDITED"), edited));
        }
        return ret.toString();
    }

    /**
     * Why Run is disabled, for the status bar rather than a tooltip. R-40.
     *
     * A tooltip on a disabled button is unreliable across platforms, and this is the one message the
     * player most needs when nothing happens - which is the complaint the whole rebuild started
     * from.
     *
     * The decision is {@link RunGate}'s, in PbmCommons, where it can be tested without a bundle.
     * This only names it, and the label keys are the enum constants, so a new state cannot be added
     * without its sentence. {@link RunGate#READY} maps to an EMPTY string: when Run works, the
     * status bar has nothing to explain and says nothing.
     */
    public static String getRunDisabledReason(CombatScenario scenario) {
        if (scenario == null) {
            return labels.getString("BATTLESIM.RUN.DISABLED.NO_ARMIES");
        }
        final RunGate gate = scenario.getRunGate();
        // NAME the armies when the thing blocking the run is that they are empty. The generic
        // sentence was true and useless: at 906 t3 hex 0452 it said armies here are hostile but
        // cannot reach each other, on a hex where the whole answer was that two named Tyrell
        // fleets had arrived with no platoons in them.
        if (gate == RunGate.NO_ENGAGEMENT) {
            final String empty = namesOfEmptyArmies(scenario);
            if (!empty.isEmpty()) {
                return String.format(
                        labels.getString("BATTLESIM.RUN.DISABLED.NO_ENGAGEMENT.EMPTY"), empty);
            }
        }
        return labels.getString("BATTLESIM.RUN."
                + (gate == RunGate.READY ? "READY" : "DISABLED." + gate.name()));
    }

    /**
     * The armies that hold nothing the player can count, by name, or empty when there are none.
     *
     * These are the ones an {@code Add platoon} would rescue. An army is counted only when EVERY
     * layer stopped on {@code NO_TROOPS} - an army excluded for some other reason is a different
     * problem with a different cure, and sweeping it in here would send the player to edit a
     * composition that was never the issue.
     */
    private static String namesOfEmptyArmies(CombatScenario scenario) {
        final StringBuilder ret = new StringBuilder();
        final Map<ArmySim, LayerParticipation> participation = scenario.getParticipation();
        for (ArmySim army : scenario.getArmies()) {
            final LayerParticipation one = participation.get(army);
            if (one == null || !isEmptyEverywhere(one)) {
                continue;
            }
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(army.getNome());
        }
        return ret.toString();
    }

    private static boolean isEmptyEverywhere(LayerParticipation participation) {
        for (CombatLayer layer : CombatLayer.values()) {
            if (participation.getReason(layer) != LayerParticipation.Reason.NO_TROOPS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether Run may be enabled at all.
     *
     * There was a constant here until T-801, and it is gone rather than flipped: the sea and city
     * layers do not get a second one, because a scenario that engages on one layer is runnable and
     * the result says which layers it resolved.
     */
    public static boolean isRunnable(CombatScenario scenario) {
        return scenario != null && scenario.getRunGate().isRunnable();
    }

    /**
     * Every army and platoon as tab-separated text, for the clipboard. T-430.
     *
     * Ported from the old window with its two defects fixed.
     *
     * <b>Land and naval were SWAPPED against their headers.</b>
     * {@code getAtaqueExercito(army, true)} sat under "Land attack", and {@code true} means NAVAL:
     * {@code BattleSimFacade.getArmyAttack} tests {@code naval == tipoTropa.isBarcos()}. The old
     * army TABLE had it right, so only anyone who pasted the export was misled - quietly, since
     * both columns are plausible numbers.
     *
     * <b>The headers were hardcoded English</b> in a feature where every other string comes from
     * the bundle. They come from the bundle now, reusing the keys the army table already uses.
     */
    public static String getClipboardText(CombatScenario scenario) {
        final StringBuilder ret = new StringBuilder();
        appendRow(ret, labels.getString("COMANDANTE"), labels.getString("NACAO"),
                labels.getString("MORAL"), labels.getString("TROPA.ATAQUE.TERRA"),
                labels.getString("TROPA.DEFESA.TERRA"), labels.getString("TROPA.ATAQUE.NAVAL"),
                labels.getString("TROPA.DEFESA.NAVAL"));
        final ExercitoFacade facade = new ExercitoFacade();
        for (ArmySim army : scenario.getArmies()) {
            appendRow(ret, army.getNome(),
                    army.getNacao() == null ? "" : String.valueOf(army.getNacao().getNome()),
                    String.valueOf(army.getMoral()),
                    // false is LAND, true is NAVAL. The old export had these the other way round.
                    String.valueOf(facade.getAtaqueExercito(army, false)),
                    String.valueOf(facade.getDefesaExercito(army, false)),
                    String.valueOf(facade.getAtaqueExercito(army, true)),
                    String.valueOf(facade.getDefesaExercito(army, true)));
            for (Pelotao pelotao : new control.BattleSimControler.PlatoonTableModel(
                    scenario, army).getPlatoons()) {
                appendRow(ret, "", pelotao.getTipoTropa() == null ? ""
                        : String.valueOf(pelotao.getTipoTropa().getNome()),
                        String.valueOf(pelotao.getQtd()), String.valueOf(pelotao.getTreino()),
                        String.valueOf(pelotao.getModAtaque()),
                        String.valueOf(pelotao.getModDefesa()), "");
            }
        }
        return ret.toString();
    }

    private static void appendRow(StringBuilder to, String... cells) {
        for (int ii = 0; ii < cells.length; ii++) {
            if (ii > 0) {
                to.append('\t');
            }
            to.append(cells[ii]);
        }
        to.append('\n');
    }

    /**
     * What the platoon table's ROW ORDER actually means for this army. T-437.
     *
     * The table is sorted, and a sorted list of rows looks like a sequence whether or not one
     * exists - so when it does not, this says so rather than letting the player read a ranking into
     * it. John, 2026-09-20: "standard tactics splits the damage across all platoons equally, no
     * specific sequence like the other tactics. So not to mislead players, it hid the sequence."
     *
     * The old window hid the whole troop list instead, which was honest about the order and
     * explained nothing. Saying it in words keeps the list, which the player still needs.
     *
     * Covers all three no-sequence cases, not just the obvious one - see {@link CasualtyMode}. An
     * army with ships gets the extra line, because naval casualties rank even when its land
     * casualties do not.
     */
    public static String getCasualtyModeText(ArmySim army, Cenario cenario) {
        if (army == null) {
            return "";
        }
        final CasualtyMode land = CasualtyMode.of(army, cenario, CombatLayer.ARMY);
        final StringBuilder ret = new StringBuilder("<html>");
        if (land.isSequenced()) {
            ret.append(labels.getString("BATTLESIM.CASUALTY.RANKED"));
        } else if (army.getTatica() == CasualtyMode.TATICA_STANDARD) {
            ret.append(labels.getString("BATTLESIM.CASUALTY.STANDARD"));
        } else {
            ret.append(labels.getString("BATTLESIM.CASUALTY.NOTACTICS"));
        }
        if (!land.isSequenced() && hasShips(army)) {
            ret.append("<br>").append(labels.getString("BATTLESIM.CASUALTY.SHIPSRANKED"));
        }
        return ret.append("</html>").toString();
    }

    private static boolean hasShips(ArmySim army) {
        for (model.Pelotao pelotao : army.getPelotoes().values()) {
            if (pelotao.getTipoTropa() != null && pelotao.getTipoTropa().isBarcos()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The four numbers that answer "who is stronger": land and sea, attack and defence.
     *
     * The single biggest thing the three-pane rebuild was missing. The old window's army table
     * carried these as four columns and they are the reason a player opens the tool at all - it
     * could say who fights whom and in what order they die, and could not say who would win.
     *
     * Computed live off {@code ExercitoFacade}, which already takes {@code IExercito}, so the
     * simulated army goes straight in and the numbers are the Judge's own - no second
     * implementation to drift. <b>The boolean is NAVAL, not land</b>:
     * {@code BattleSimFacade.getArmyAttack} tests {@code naval == tipoTropa.isBarcos()}. The old
     * clipboard export had it backwards and printed the fleet's strength under "Land attack".
     */
    public static String getArmyStrength(ArmySim army) {
        if (army == null) {
            return "";
        }
        final ExercitoFacade facade = new ExercitoFacade();
        return String.format("<html>%s: %,d &nbsp; %s: %,d<br>%s: %,d &nbsp; %s: %,d</html>",
                labels.getString("TROPA.ATAQUE.TERRA"), facade.getAtaqueExercito(army, false),
                labels.getString("TROPA.DEFESA.TERRA"), facade.getDefesaExercito(army, false),
                labels.getString("TROPA.ATAQUE.NAVAL"), facade.getAtaqueExercito(army, true),
                labels.getString("TROPA.DEFESA.NAVAL"), facade.getDefesaExercito(army, true));
    }

    /**
     * A compact strength for the roster, so armies can be COMPARED without clicking each one.
     *
     * The old window's scan surface was a table; this one is a tree, and a detail pane only ever
     * shows the selected army. Without something on the node itself the player has to click every
     * army in turn and remember the numbers, which is exactly the job the old table did for him.
     *
     * Only the layer the army actually has troops in, because a land host's naval attack is zero
     * and printing "0/0" beside every army would be noise dressed as data.
     */
    public static String getArmyStrengthShort(ArmySim army) {
        if (army == null) {
            return "";
        }
        final ExercitoFacade facade = new ExercitoFacade();
        final StringBuilder ret = new StringBuilder();
        final int landAttack = facade.getAtaqueExercito(army, false);
        final int landDefense = facade.getDefesaExercito(army, false);
        if (landAttack > 0 || landDefense > 0) {
            ret.append(String.format("%,d/%,d", landAttack, landDefense));
        }
        final int seaAttack = facade.getAtaqueExercito(army, true);
        final int seaDefense = facade.getDefesaExercito(army, true);
        if (seaAttack > 0 || seaDefense > 0) {
            if (ret.length() > 0) {
                ret.append("  ");
            }
            ret.append(String.format("%s %,d/%,d",
                    labels.getString("BATTLESIM.LAYER.NAVY"), seaAttack, seaDefense));
        }
        // Nothing to count, so say what the player WAS told instead of leaving the row blank. The
        // band is the whole of his intelligence on an unscouted army and it is what he will type
        // the composition against; a bare name simply looks like a bug. Land band preferred, since
        // for a fleet it is the force that can come ashore and the naval word hides it.
        if (ret.length() == 0) {
            ret.append(army.getSizeBandLand().isEmpty()
                    ? army.getSizeBand() : army.getSizeBandLand());
        }
        return ret.toString();
    }

    /**
     * What the run did, for the status bar: rounds fought, plus everything it could not do.
     *
     * The caveats are not a footnote. A result that omits what it skipped looks complete, and a
     * player reading casualties off a land-only resolution while a fleet sits on the same hex is
     * being misled by omission. So the layer limit is stated on EVERY result, not only when
     * something goes wrong, and the resolver's own notes follow it.
     */
    public static String getRunResultText(CombatResult result) {
        if (result == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder("<html>");
        // Zero rounds is NOT a battle resolved in zero rounds, and saying "Resolved in 0 round(s)"
        // would be a completed simulation of a fight that never happened. It has two causes and
        // both are real: the hex engages only on the sea or city layer (the run gate says READY for
        // engagement on ANY layer, by design), or the armies standing on land are not hostile to
        // each other. Either way the honest sentence is the same one.
        ret.append(result.getRounds() == 0
                ? labels.getString("BATTLESIM.RESULT.NOLANDBATTLE")
                : String.format(labels.getString("BATTLESIM.RESULT.DONE"), result.getRounds()));
        for (String note : result.getNotes()) {
            final int count = result.getNoteCount(note);
            ret.append("<br>").append(count > 0
                    ? String.format(labels.getString(note), count) : labels.getString(note));
        }
        return ret.append("</html>").toString();
    }

    /**
     * "Reported size: Vast army" - the server's own word for how big this army is.
     *
     * Shown for EVERY army, not only the ones the player cannot see into, and the javadoc used to
     * claim otherwise. The band is server-derived in both cases: for an unscouted enemy it is the
     * only strength figure there is, and for his own army it is the same sentence his army list
     * already shows him. Suppressing it on the armies he can count would also take it away exactly
     * when he starts typing a composition into an unscouted one - which is the moment he most wants
     * to check his guess against what he was told.
     *
     * It is a LABEL and stays one: the band is what he was told, and an editable version would
     * invite him to treat a reversed guess as data. What he types instead is the platoon list,
     * which is his own estimate and is marked as such.
     */
    public static String getSizeBandText(ArmySim army) {
        if (army == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder();
        if (!army.getSizeBand().isEmpty()) {
            ret.append(String.format(labels.getString("BATTLESIM.SIZE.REPORTED"),
                    army.getSizeBand()));
            // BOTH bands when they differ, which happens exactly when the army holds ships: the
            // displayed one then describes the FLEET and the land band - the size of the force it
            // can put ashore - is suppressed. That is the number a player facing a landing wants,
            // and it was being thrown away. See ArmySim.getSizeBandLand.
            final String land = army.getSizeBandLand();
            if (!land.isEmpty() && !land.equals(army.getSizeBand())) {
                ret.append(String.format(labels.getString("BATTLESIM.SIZE.CARRYING"), land));
            }
        }
        if (army.getPelotoes().isEmpty()) {
            // an empty platoon table looks like a bug unless something says what to do about it
            if (ret.length() > 0) {
                ret.append("   ");
            }
            ret.append(labels.getString("BATTLESIM.PLATOON.NONE"));
        }
        return ret.toString();
    }

    /**
     * The city line: what the attackers face, and whether there is a round 0.
     *
     * Two numbers, not one, because the city layer is TWO rounds. Round 0 is siege engines against
     * the FORTIFICATION and is fought only when an attacker carries them; round 1 is the single
     * army-versus-city exchange against the city's DEFENSE. Round 0 comes first and can reduce the
     * fortification, so collapsing them into one figure would hide the order that decides the
     * result.
     */
    public static String getCityText(CombatScenario scenario) {
        if (scenario == null || scenario.getCidadeAtiva() == null) {
            return labels.getString("BATTLESIM.CITY.NONE");
        }
        // Three facts on three lines, not one run-on sentence. Spaced onto one line they ran
        // past the edge of the Ground panel and the siege clause - the half that says whether there
        // is a round 0 at all - was the half that got cut off.
        return String.format("<html>%s: %,d<br>%s: %,d<br>%s</html>",
                labels.getString("BATTLESIM.CITY.DEFENSE"), scenario.getCityDefense(),
                labels.getString("BATTLESIM.CITY.FORTIFICATION.DEFENSE"),
                scenario.getCityFortificationDefense(),
                labels.getString(scenario.isSiegeExpected()
                        ? "BATTLESIM.CITY.SIEGE" : "BATTLESIM.CITY.NOSIEGE"));
    }

    /**
     * The roster leaf: the army's name, and the three layer slots - ALWAYS the three.
     *
     * <h3>The empty block used to be suppressed, and that was the bug</h3>
     *
     * An army in no layer at all rendered as a plain name and a strength, on the argument that a
     * JTree's proportional font does not align the slots into columns anyway, so three dots carried
     * no information and read as a truncated name. The FFA hex is the counter-example: before anyone
     * declares a war, NO army engages, so every row lost its block at once and the player was left
     * with three ordinary-looking rows and a disabled Run button with nothing connecting them. John,
     * on a live hex: "run simulation was disabled but no glyphs."
     *
     * The dots are not decoration in that state, they are the answer - {@code
     * BATTLESIM.OUTCOME.NOT_IN_LAYER} means "not in this layer" and this is the one case where every
     * slot says it. The slot never moves, so the block is also the only thing that distinguishes an
     * army that fights nowhere from one that fights everywhere at a glance. The army editor's
     * "Fights in:" line still carries the reasons; this carries the fact.
     */
    public static String getArmyTitle(ArmySim army, LayerParticipation participation) {
        return getArmyTitle(army, participation, null);
    }

    /**
     * @param outcome how the last run ended for this army, or null before one.
     *
     * The mark goes at the FRONT, where the eye lands first and where it lines up down the column.
     * It is the one thing in the roster that answers the question the player actually opened the
     * window to ask, and reading it should not mean parsing the rest of the row.
     */
    public static String getArmyTitle(ArmySim army, LayerParticipation participation,
            CombatResult result) {
        if (army == null) {
            return "";
        }
        final String name = getUnknownMark(army) + army.getNome();
        final String strength = getArmyStrengthShort(army);
        // No branch on participation: markFor already answers NOT_IN_LAYER for a null one and for
        // every layer it declines, so an idle army renders three dots and keeps its slots.
        return String.format("%s  [%s]  %s", name,
                getLayerMarks(army, participation, result), strength);
    }

    /**
     * A glyph on any army the player cannot count, which is the one thing that silently blocks a
     * run.
     *
     * At the FRONT, before the name, so a column of armies can be read straight down - the same
     * reason the layer marks hold a fixed slot. An unscouted army otherwise rendered as a bare name
     * with nothing after it, which reads as a rendering fault rather than as missing intelligence:
     * 906 t3 hex 0452 showed "Paxter Redwyne" and "Colin Florent" with no numbers at all beside a
     * Jaime Lannister carrying "29,259/78,665", and the player twice concluded the window was
     * broken rather than that he had something to fill in.
     *
     * A GLYPH and not a colour. The roster has no cell renderer of its own, so colouring it means
     * introducing one, and a {@code DefaultTreeCellRenderer} is a single instance reused for every
     * row - a foreground set on one row persists to the next unless every branch resets it, which
     * is the same trap already recorded for table renderers. A glyph also survives a theme change,
     * and the roster is already read in glyphs.
     *
     * Keyed on the TROOP COUNT rather than on an empty platoon list, because an army whose platoons
     * are all at zero is equally uncountable and equally blocking.
     */
    private static String getUnknownMark(ArmySim army) {
        // no null check: the only caller has already returned for a null army, and a guard here
        // that the next token defeats reads as protection that is not there
        return new ExercitoFacade().getQtTropasTotal(army) <= 0
                ? labels.getString("BATTLESIM.ARMY.UNKNOWN") + " " : "";
    }

    /**
     * THREE SLOTS, sea then land then city, and each one says what happened in its own layer.
     *
     * The slot never moves, which is the whole value of it: the middle mark is always the land
     * battle whether or not there was a fight at sea, so a column of armies can be read down rather
     * than parsed one row at a time. A battle is three fights and they can end differently - a fleet
     * can win at sea and the troops it lands still be destroyed ashore - so one mark per army would
     * have to pick one of those and hide the rest.
     *
     * Four states per slot, and they are four different statements:
     * <ul>
     *   <li>a dot - this army is not in that layer at all;</li>
     *   <li>the layer's letter - it is in it, and there is no verdict yet because nothing has been
     *       run. All three layers resolve, so this state no longer survives a run;</li>
     *   <li>an outcome glyph - it fought and this is how it ended;</li>
     *   <li>the watching glyph - it was in the layer and never met an enemy there.</li>
     * </ul>
     */
    public static String getLayerMarks(ArmySim army, LayerParticipation participation,
            CombatResult result) {
        final StringBuilder ret = new StringBuilder();
        for (CombatLayer layer : CombatLayer.values()) {
            if (ret.length() > 0) {
                ret.append(' ');
            }
            ret.append(markFor(army, participation, result, layer));
        }
        return ret.toString();
    }

    private static String markFor(ArmySim army, LayerParticipation participation,
            CombatResult result, CombatLayer layer) {
        if (participation == null || !participation.isIn(layer)) {
            return labels.getString("BATTLESIM.OUTCOME.NOT_IN_LAYER");
        }
        final CombatResult.Outcome outcome = result == null ? null : result.getOutcome(army, layer);
        return outcome == null ? layer.getBadge()
                : labels.getString("BATTLESIM.OUTCOME." + outcome.name());
    }

    /**
     * Before, after and lost for one army, or NULL when it took no part.
     *
     * Null rather than zeroes, because those are different statements: an army that fought and lost
     * nobody has three real numbers, and an army that was never in the battle has none. The table
     * shows "--" for the second.
     */
    public static int[] getArmyTotals(ArmySim army, CombatResult result) {
        int before = 0;
        int after = 0;
        boolean any = false;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (!result.has(pelotao)) {
                continue;
            }
            any = true;
            before += pelotao.getQtd();
            after += result.getAfter(pelotao);
        }
        return any ? new int[]{before, after, before - after} : null;
    }

    /**
     * The verdict, in at most a few lines: who is left holding the hex and what it cost them.
     *
     * Built from the OUTCOMES rather than from the casualty numbers, because the two can disagree in
     * a way that matters: an army that fought and lost nobody and an army that never engaged both
     * end at full strength, and only the resolver knows which is which.
     */
    public static List<String> getVerdictLines(CombatScenario scenario, CombatResult result) {
        final List<String> ret = new ArrayList<>();
        if (result.getRounds() == 0) {
            // The SAME key the status bar uses, not a second one saying the same thing. There were
            // two - BATTLESIM.VERDICT.NOBATTLE and BATTLESIM.RESULT.NOLANDBATTLE - byte-identical in
            // all five languages, and both reached from this one predicate on this one result: the
            // status bar after the run, this dialog when it is opened. One fact, one sentence; two
            // copies of it only means that one day a translator fixes one of them.
            ret.add(labels.getString("BATTLESIM.RESULT.NOLANDBATTLE"));
            return ret;
        }
        // The sea battle happens before anybody is ashore, so it is read first.
        final String sea = getSeaVerdict(scenario, result);
        if (sea != null) {
            ret.add(sea);
        }
        final List<String> standing = new ArrayList<>();
        final List<String> destroyed = new ArrayList<>();
        boolean undecided = false;
        for (ArmySim army : scenario.getArmies()) {
            final CombatResult.Outcome outcome = result.getOutcome(army, CombatLayer.ARMY);
            if (outcome == CombatResult.Outcome.WON) {
                standing.add(army.getNome());
            } else if (outcome == CombatResult.Outcome.LOST) {
                destroyed.add(army.getNome());
            } else if (outcome == CombatResult.Outcome.UNDECIDED) {
                undecided = true;
            }
        }
        if (undecided) {
            ret.add(labels.getString("BATTLESIM.VERDICT.STALEMATE"));
        } else if (!standing.isEmpty()) {
            // "X holds the field" versus "X, Y and Z hold the field" - one army or several is the
            // difference between a verdict that reads and one that reads like a template.
            ret.add(String.format(labels.getString(standing.size() == 1
                    ? "BATTLESIM.VERDICT.HOLDS" : "BATTLESIM.VERDICT.HOLD"), join(standing)));
        }
        if (!destroyed.isEmpty()) {
            ret.add(String.format(labels.getString(destroyed.size() == 1
                    ? "BATTLESIM.VERDICT.DESTROYED" : "BATTLESIM.VERDICT.DESTROYED.MANY"),
                    join(destroyed)));
        }
        final String city = getCityVerdict(scenario, result);
        if (city != null) {
            ret.add(city);
        }
        for (ArmySim army : scenario.getArmies()) {
            final int[] totals = getArmyTotals(army, result);
            if (totals == null || totals[2] <= 0) {
                continue;
            }
            // An army that fought at sea has HULLS in this total as well as bodies, because the
            // sea battle put both at risk and a summary that counted only the bodies would tell a
            // fleet that lost half its ships it had lost nothing. The sentence has to say so:
            // "139 of 2,812 troops" is wrong when 12 of the 139 were cargo ships.
            final boolean atSea =
                    result.getOutcome(army, CombatLayer.NAVY) == CombatResult.Outcome.WON
                    || result.getOutcome(army, CombatLayer.NAVY) == CombatResult.Outcome.LOST;
            ret.add(String.format(labels.getString(atSea
                    ? "BATTLESIM.VERDICT.COST.NAVAL" : "BATTLESIM.VERDICT.COST"), army.getNome(),
                    totals[2], totals[0], Math.round(100f * totals[2] / totals[0])));
        }
        return ret;
    }

    /**
     * Who still has a fleet, or null when no sea battle was fought.
     *
     * Its own line, before the land one, because the sea battle happens first and can decide the
     * rest of the hex: a fleet sunk in open water never puts anybody ashore. WON here is the
     * Judge's own naval verdict - still a fleet, and still present - so an army whose hulls all
     * sank but whose troops reached the beach appears as having LOST at sea and may still hold the
     * field afterwards. The two lines disagreeing is the report working.
     */
    private static String getSeaVerdict(CombatScenario scenario, CombatResult result) {
        if (result.getRounds(CombatLayer.NAVY) <= 0) {
            return null;
        }
        final List<String> afloat = new ArrayList<>();
        final List<String> sunk = new ArrayList<>();
        for (ArmySim army : scenario.getArmies()) {
            final CombatResult.Outcome outcome = result.getOutcome(army, CombatLayer.NAVY);
            if (outcome == CombatResult.Outcome.WON) {
                afloat.add(army.getNome());
            } else if (outcome == CombatResult.Outcome.LOST) {
                sunk.add(army.getNome());
            }
        }
        if (sunk.isEmpty() && afloat.isEmpty()) {
            return null;
        }
        if (sunk.isEmpty()) {
            return labels.getString("BATTLESIM.VERDICT.SEA.STALEMATE");
        }
        return String.format(labels.getString(sunk.size() == 1
                ? "BATTLESIM.VERDICT.SEA.SUNK" : "BATTLESIM.VERDICT.SEA.SUNK.MANY"), join(sunk));
    }

    /**
     * What became of the city, or null when no assault was fought.
     *
     * Its own line rather than a clause on "X holds the field", because the two can disagree and
     * both be true: an attacker can be left standing on the hex having been thrown back off the
     * walls. The land verdict says who is still there; this says whether the city changed hands.
     *
     * {@code NO_ASSAULT} returns null on purpose - a hex whose city nobody attacked has nothing to
     * report, and a line saying so would appear under every land battle fought near a city.
     */
    private static String getCityVerdict(CombatScenario scenario, CombatResult result) {
        final CityCombatResolver.CityResult city = result.getCityResult();
        if (city == null || city.getOutcome() == null
                || city.getOutcome() == CityCombatResolver.CityOutcome.NO_ASSAULT) {
            return null;
        }
        final Cidade cidade = scenario.getLocal() == null ? null : scenario.getLocal().getCidade();
        final String nome = cidade == null || cidade.getNome() == null
                ? labels.getString("BATTLESIM.CITY.TITLE") : cidade.getNome();
        return String.format(labels.getString("BATTLESIM.VERDICT.CITY."
                + city.getOutcome().name()), nome);
    }

    private static String join(List<String> names) {
        final StringBuilder ret = new StringBuilder();
        for (String name : names) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(name);
        }
        return ret.toString();
    }

    /**
     * The three marks in words, for the roster tooltip. T-838.
     *
     * An emoji is a guess unless something says what it means, and this used to answer only after a
     * run and only for an army that fought - which is precisely backwards. The two moments a player
     * hovers a row of dots asking what they mean are BEFORE he has pressed Run, and on the army
     * that is taking no part; both returned null and the hover stayed silent.
     *
     * <h3>Why the dots stay</h3>
     *
     * The original note against placeholders argued that three dots beside a name read as a
     * truncated name. That was true when they rendered bare. They now sit in brackets between the
     * name and the troop count, {@code Name  [. . .]  1,300/4,200}, which cannot be read as a
     * truncation - and removing them would cost the fixed slot that lets a column of armies be read
     * straight down. The thing John actually wanted, a signal that something is BLOCKING the run,
     * is a property of the scenario rather than of any army, and it is answered separately: the
     * uncountable-army glyph in front of the name, and the reason text under the disabled button.
     *
     * So the dots keep their slots and the hover explains them, which is the third option - neither
     * "bare rows say nothing" nor "three dots read as a truncation".
     */
    public static String getLayerHint(ArmySim army, LayerParticipation participation,
            CombatResult result) {
        if (participation == null) {
            return null;
        }
        if (result == null || !participation.isInAnyLayer()) {
            // No verdict to give, so give the participation instead - the same four lines the
            // "Fights in" line already says, rather than a second wording of them that can drift.
            return getFightsIn(participation);
        }
        final StringBuilder ret = new StringBuilder("<html>");
        for (CombatLayer layer : CombatLayer.values()) {
            final String said;
            if (!participation.isIn(layer)) {
                // EVERY layer answers, including the ones it sat out. A slot that says nothing is
                // the dot the player is hovering to ask about.
                said = getReasonName(participation.getReason(layer));
            } else {
                final CombatResult.Outcome outcome = result.getOutcome(army, layer);
                said = outcome == null
                        ? labels.getString("BATTLESIM.OUTCOME.NOT_RESOLVED.HINT")
                        : labels.getString("BATTLESIM.OUTCOME." + outcome.name() + ".HINT");
            }
            ret.append(String.format("%s: %s<br>",
                    labels.getString("BATTLESIM.LAYER." + layer.name()), said));
        }
        return ret.append("</html>").toString();
    }
}
