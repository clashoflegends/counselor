package e2e;

import baseLib.BaseModel;
import control.facade.WorldFacadeCounselor;
import model.Nacao;
import model.PersonagemOrdem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The status-bar "action's cost" label answers "what is MY turn costing", so it must never name a
 * nation the player does not own. It used to render whichever nation the last order-change dispatch
 * carried, so loading a team's orders left it showing an ally's total.
 * <p>
 * Ownership is by OWNER identity here, not {@code Jogador.isNacao}: allies' turn files merge into one
 * shared world on a team-order load, and that is exactly when the label went wrong.
 */
class OwnNationOrderCostTest {

    private static final int ORDEM_CUSTO = 1555;

    private static WorldFacadeCounselor wfc;

    @BeforeAll
    static void openTurnFile() throws Exception {
        wfc = TestWorld.load();
    }

    @AfterEach
    void clearForecast() {
        for (java.util.Set<PersonagemOrdem> orders : wfc.getMapPersonagemOrdens().values()) {
            orders.clear();
        }
    }

    @Test
    void picksANationTheActivePlayerOwns() {
        Nacao top = wfc.getNacaoTopOrderCost();

        assertNotNull(top, "fixture player owns no nation");
        assertSame(wfc.getJogadorAtivo(), top.getOwner(),
                "the label named a nation the active player does not own");
    }

    /** An ally's costly orders must not capture the label. */
    @Test
    void ignoresNationsTheActivePlayerDoesNotOwn() {
        Nacao foreign = foreignNation();
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        PersonagemOrdem expensive = TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_CUSTO), "Kingsguard");
        wfc.addNacaoPersonagemOrdens(foreign, expensive);

        assertNotSame(foreign, wfc.getNacaoTopOrderCost(),
                "an ally's order cost was reported as the player's own");
    }

    @Test
    void reportsTheCostliestOwnedNation() {
        java.util.List<Nacao> mine = wfc.getNacoesJogadorAtivo();
        if (mine.size() < 2) {
            return; //single-nation fixture: the costliest is trivially the only one, covered above
        }
        BaseModel pc = TestWorld.freshPersonagem(wfc);
        Nacao second = mine.get(1);
        wfc.addNacaoPersonagemOrdens(second,
                TestWorld.buildOrder(pc, TestWorld.ordem(wfc, ORDEM_CUSTO), "Kingsguard"));

        assertSame(second, wfc.getNacaoTopOrderCost());
    }

    private static Nacao foreignNation() {
        for (Nacao nacao : wfc.getNacoes().values()) {
            if (nacao.getOwner() != wfc.getJogadorAtivo()) {
                return nacao;
            }
        }
        throw new IllegalStateException("fixture has no nation outside the active player's");
    }
}
