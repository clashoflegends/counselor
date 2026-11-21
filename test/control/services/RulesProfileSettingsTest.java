package control.services;

import baseLib.GenericoTableModel;
import model.Cenario;
import model.Habilidade;
import model.Partida;
import model.Produto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RulesProfileSettingsTest {
    private Habilidade power(String code, String name) {
        Habilidade h = new Habilidade(); h.setCodigo(code); h.setNome(name); return h;
    }
    private Partida game(String gameMarker, String inheritedMarker) {
        Partida game = new Partida(); game.setNome("Profile fixture");
        Cenario scenario = new Cenario(); scenario.setNome("Scenario fixture");
        Produto money = new Produto(); money.setNome("Gold"); scenario.setMoney(money);
        game.setCenario(scenario);
        if (gameMarker != null) game.addHabilidade(power(gameMarker, gameMarker.equals(";GTR;") ? "New Trial Rules" : "Standard Rules"));
        scenario.addHabilidade(power(inheritedMarker, inheritedMarker.equals(";GTR;") ? "New Trial Rules" : "Standard Rules"));
        return game;
    }
    private long count(GenericoTableModel settings, String text) {
        long result = 0;
        for (int row = 0; row < settings.getRowCount(); row++) if (text.equals(settings.getValueAt(row, 1))) result++;
        return result;
    }
    @Test void trialOverridesInheritedStandardLabelWithoutMutatingVariant() {
        Partida game = game(";GTR;", ";GNR;");
        GenericoTableModel settings = PartidaConverter.getPartidaModel(game);
        assertEquals(1, count(settings, "New Trial Rules")); assertEquals(0, count(settings, "Standard Rules"));
        assertTrue(game.getCenario().hasHabilidade(";GNR;"));
    }
    @Test void standardOverridesInheritedTrialLabel() {
        GenericoTableModel settings = PartidaConverter.getPartidaModel(game(";GNR;", ";GTR;"));
        assertEquals(1, count(settings, "Standard Rules")); assertEquals(0, count(settings, "New Trial Rules"));
    }
    @Test void inheritedLabelRemainsVisibleForLegacyFilesWithoutGameMarker() {
        assertEquals(1, count(PartidaConverter.getPartidaModel(game(null, ";GNR;")), "Standard Rules"));
    }
}
