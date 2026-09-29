package control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Ordem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@code OrdemControler.hasBlankSoleParam}, the guard that stops a single-parameter order being saved
 * with nothing in its one parameter.
 *
 * <p>Why it exists: {@code ComponentFactory.getParametros} mints a single space when a picker has nothing
 * selected, which is what an empty combo yields - most often a {@code Personagem_Local_NoNacao} order issued
 * from a hex holding no enemy character. The order then shipped and the Judge threw it out with
 * {@code @PARAMETRO.INCORRETO#}, costing the player the action. 253 of the 258 such rejections in the whole
 * life of the Judge log are this, and every affected order is single-parameter.
 *
 * <p>The guard is deliberately no broader than the server's own rule, and these tests are mostly about that
 * boundary: the Judge tests the whole joined parameter string against {@code ""}, which only an arity-1
 * order can match, and it never applies that test to {@code none}/{@code stringop}/{@code Var}. Anything
 * this guard rejects must be something the Judge would have rejected anyway.
 */
class BlankSoleParamTest {

    private static Ordem ordem(String chave, String... controles) {
        Ordem o = new Ordem();
        o.setNumero(338);
        o.setNome("FeitBadLuck");
        o.setChave(chave);
        o.setParametrosIde(controles);
        String[] display = new String[controles.length];
        Arrays.fill(display, "p");
        o.setParametrosIdeDisplay(display);
        return o;
    }

    private static PersonagemOrdem po(Ordem ord, String... ids) {
        PersonagemOrdem p = new PersonagemOrdem();
        p.setOrdem(ord);
        List<String> lista = new ArrayList<>(Arrays.asList(ids));
        p.setParametrosId(lista);
        p.setParametrosDisplay(new ArrayList<>(lista));
        return p;
    }

    // --- the case the guard exists for -------------------------------------------------------------

    @Test
    void blocksTheNullComboSentinel() {
        // ComponentFactory hands back a single space when the combo had nothing to select.
        assertTrue(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao"), " ")));
    }

    @Test
    void blocksAnEmptyString() {
        assertTrue(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao"), "")));
    }

    @Test
    void blocksANullId() {
        assertTrue(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao"), (String) null)));
    }

    @Test
    void blocksTheNumericOrdersAPrefixListCannotReach() {
        // MudImp (Percentage) and BidLibrarian (Quantidade) are 57 of the 258 and carry no target token,
        // which is exactly why the guard keys on arity rather than on a control-name prefix.
        assertTrue(OrdemControler.hasBlankSoleParam(po(ordem("Number", "Percentage"), " ")));
        assertTrue(OrdemControler.hasBlankSoleParam(po(ordem("Valor", "Quantidade"), " ")));
    }

    // --- must NOT fire ------------------------------------------------------------------------------

    @Test
    void allowsARealSelection() {
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao"), "1234")));
    }

    @Test
    void allowsATypedTarget() {
        // The pickers are setEditable(true) for fog of war: a name the player typed comes back through
        // ComponentFactory's ClassCastException branch as itself, and must survive.
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao"), "Cersei Lannister")));
    }

    @Test
    void ignoresMultiParameterOrders() {
        // At arity > 1 the joined string keeps its ';' separators, so it never equals "" and the Judge does
        // NOT reject it. Firing here would be broader than the server and could block a legal order.
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem("Valor Tropa", "Quantidade", "Tropa_Tipo"), " ", " ")));
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem("Valor Tropa", "Quantidade", "Tropa_Tipo"), " ", "nw")));
    }

    @Test
    void ignoresTheChavesTheJudgeNeverChecks() {
        // none and stringop return early in Ordem.tratamentoParametro; Var takes its own branch and a
        // different message. Blocking any of them would reject something the server accepts.
        assertFalse(OrdemControler.hasBlankSoleParam(po(ordem("none", "Personagem_Local_NoNacao"), " ")));
        assertFalse(OrdemControler.hasBlankSoleParam(po(ordem("None", "Personagem_Local_NoNacao"), " ")));
        assertFalse(OrdemControler.hasBlankSoleParam(po(ordem("stringop", "Personagem_Local_NoNacao"), " ")));
        assertFalse(OrdemControler.hasBlankSoleParam(po(ordem("StringOp", "Personagem_Local_NoNacao"), " ")));
        assertFalse(OrdemControler.hasBlankSoleParam(po(ordem("Var", "Personagem_Local_NoNacao"), " ")));
    }

    @Test
    void ignoresAnIdListThatDoesNotMatchTheOrdersArity() {
        // A hidden parameter leaves the collected ids out of step with the order; don't risk a false reject.
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao", "Quantidade"), " ")));
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem("Id", "Personagem_Local_NoNacao"), " ", " ")));
    }

    @Test
    void survivesMissingPieces() {
        assertFalse(OrdemControler.hasBlankSoleParam(null));
        assertFalse(OrdemControler.hasBlankSoleParam(po(null, " ")));
        PersonagemOrdem semIds = new PersonagemOrdem();
        semIds.setOrdem(ordem("Id", "Personagem_Local_NoNacao"));
        assertFalse(OrdemControler.hasBlankSoleParam(semIds));
    }

    @Test
    void toleratesAnOrderWithNoChave() {
        assertFalse(OrdemControler.hasBlankSoleParam(
                po(ordem(null, "Personagem_Local_NoNacao"), " ")));
    }
}
