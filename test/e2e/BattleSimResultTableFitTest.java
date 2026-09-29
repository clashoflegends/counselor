package e2e;

import business.combat.ArmySim;
import business.combat.CombatChain;
import business.combat.CombatScenario;
import business.combat.RelationshipMatrix;
import control.BattleSimControler;
import gui.accessories.BattleSimResultPanel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every table on the result screen shows all of its rows.
 *
 * <h3>The report this is the regression for</h3>
 *
 * A player, 2026-09-28: <i>"e na tela de resultado, as tabelas nao da pra ver direito, precisa
 * aumentar um pouco"</i>, with a screenshot of a four-army result in which the last army is sliced
 * in half - in the casualties table AND in the city layer, on the one screen the whole feature
 * exists to show.
 *
 * The holder's height was {@code rowHeight * (rows + 1) + 4}, which assumes a header exactly one
 * row tall and rows with no spacing between them. Neither is true: a header is typically taller,
 * and JTable puts {@code rowMargin} between rows. The error grows with the row count, so it was
 * invisible on the two-army battles everything was built against.
 *
 * <h3>Why the assertion is relative and not a pixel count</h3>
 *
 * Fonts, LookAndFeel and display scaling all move these numbers, and CI is a Linux box with none of
 * the player's. Asserting "the box is at least as tall as the things inside it" is the property
 * that actually matters and it holds everywhere; asserting a height in pixels would pass on one
 * machine and lie on every other.
 */
public class BattleSimResultTableFitTest {

    /** The two pixels of border slack the panel adds. Kept here so the pin reads as one number. */
    private static final int PAD = 2;

    private static final Terreno PLAIN = plain();

    private static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        return ret;
    }

    private static TipoTropa troopType(String codigo) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        final java.util.SortedMap<Terreno, Integer> attack = new java.util.TreeMap<>();
        attack.put(PLAIN, 50);
        final java.util.SortedMap<Terreno, Integer> defence = new java.util.TreeMap<>();
        defence.put(PLAIN, 40);
        final java.util.SortedMap<Terreno, Integer> move = new java.util.TreeMap<>();
        move.put(PLAIN, 5);
        ret.setAtaqueTerreno(attack);
        ret.setDefesaTerreno(defence);
        ret.setMovimentoTerreno(move);
        final Habilidade none = new Habilidade();
        none.setCodigo(";-;");
        none.setNome(";-;");
        ret.addHabilidade(none);
        return ret;
    }

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("1139");
        ret.setCoordenadas("1139");
        ret.setTerreno(PLAIN);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, int troops) {
        final ArmySim ret = new ArmySim(nome, PLAIN, nacao);
        ret.setLocal(hex());
        ret.setMoral(100);
        ret.setComandante(50);
        final Pelotao one = new Pelotao();
        one.setTipoTropa(troopType("inf"));
        one.setQtd(troops);
        one.setTreino(50);
        ret.getPelotoes().put(one.getCodigo(), one);
        return ret;
    }

    /**
     * FOUR armies, because that is the shape the player photographed and two would not have shown
     * it - the shortfall is per-table, not per-row, and a short table hid it inside the slack.
     */
    private static CombatScenario fourArmyBattle() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario ret = new CombatScenario(null, hex());
        ret.addArmy(army("Victarion Greyjoy", mine, 0), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("Mellos Rivers", mine, 0), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("Hobert Waters", mine, 2000), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("Errold Rivers", foe, 1000), CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(mine, foe, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(foe, mine, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    /** Every JPanel holding a header plus a table, wherever it sits in the tree. */
    private static List<JPanel> tableHolders(Container root) {
        final List<JPanel> ret = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel) {
                final JPanel panel = (JPanel) child;
                boolean header = false, table = false;
                for (Component inner : panel.getComponents()) {
                    header |= inner instanceof JTableHeader;
                    table |= inner instanceof JTable;
                }
                if (header && table) {
                    ret.add(panel);
                }
            }
            if (child instanceof Container) {
                ret.addAll(tableHolders((Container) child));
            }
        }
        return ret;
    }

    @Test
    public void noTableOnTheResultScreenClipsItsLastRow() {
        final CombatScenario scenario = fourArmyBattle();
        final BattleSimControler controler = new BattleSimControler(scenario);
        controler.doRun();

        final BattleSimResultPanel panel = new BattleSimResultPanel(controler);
        final List<JPanel> holders = tableHolders(panel);

        assertFalse(holders.isEmpty(), "the result screen must actually contain tables");
        for (JPanel holder : holders) {
            int needed = 0;
            for (Component child : holder.getComponents()) {
                needed += child.getPreferredSize().height;
            }
            assertEquals(needed + PAD, holder.getPreferredSize().height,
                    "a table box must be sized from the header and the table it holds, not from an"
                    + " assumed row height - that is what sliced the last army in half");
        }
    }
}
