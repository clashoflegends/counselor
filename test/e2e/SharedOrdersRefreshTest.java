package e2e;

import baseLib.BaseModel;
import control.services.SharedOrdersRefresh;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedOrdersRefreshTest {
    Partida game;
    Jogador me, ally;
    Nacao mine, theirs;
    Personagem own, shared, absent;
    Ordem action;
    List<BaseModel> actors;
    Map<Nacao, java.util.Set<PersonagemOrdem>> forecasts;

    @BeforeEach void setup() {
        game = new Partida(); game.setId(1); game.setCodigo("g1"); game.setTurno(12);
        me = new Jogador(); me.setId(10); ally = new Jogador(); ally.setId(20);
        mine = new Nacao(); mine.setId(100); mine.setCodigo("N100"); mine.setOwner(me); me.addNacao(mine);
        theirs = new Nacao(); theirs.setId(200); theirs.setCodigo("N200"); theirs.setOwner(ally); ally.addNacao(theirs);
        own = pc("own", mine); shared = pc("shared", theirs); absent = pc("absent", theirs);
        actors = List.of(mine, theirs, own, shared, absent);
        action = new Ordem(); action.setCodigo("555"); action.setNome("Map Area");
        own.setAcao(0, po()); shared.setAcao(0, po()); absent.setAcao(0, po());
        game.setJogadorAtivo(me);
        forecasts = new java.util.HashMap<>();
        forecasts.put(mine, new java.util.HashSet<>(List.of(own.getAcao(0))));
        forecasts.put(theirs, new java.util.HashSet<>(List.of(shared.getAcao(0), absent.getAcao(0))));
    }
    Personagem pc(String code, Nacao nation) {
        Personagem pc = new Personagem(); pc.setCodigo(code); pc.setNome(code); pc.setNacao(nation); return pc;
    }
    PersonagemOrdem po() { PersonagemOrdem po = new PersonagemOrdem(); po.setOrdem(action); return po; }
    Comando command(boolean empty) {
        game.setJogadorAtivo(ally); Comando c = new Comando(); c.setInfos(game); game.setJogadorAtivo(me);
        if (!empty) c.addComando(shared, action, List.of("0968"), List.of("0968"), Instant.EPOCH);
        return c;
    }
    SharedOrdersRefresh.Prepared prepare(Comando c) {
        return SharedOrdersRefresh.prepare(c, game, actors, Map.of("555", action));
    }

    @Test void validReplacementPreservesLocalAndRemovesAbsentSharedOrders() {
        PersonagemOrdem local = own.getAcao(0);
        var prepared = prepare(command(false));
        assertEquals(1, absent.getAcaoSize(), "preflight must not mutate");
        SharedOrdersRefresh.apply(prepared, forecasts, (po,n) -> 100);
        assertSame(local, own.getAcao(0)); assertEquals(0, absent.getAcaoSize());
        assertEquals(List.of("0968"), shared.getAcao(0).getParametrosId());
        assertEquals(Instant.EPOCH, shared.getAcao(0).getRecordedUpdateTime());
        assertEquals(java.util.Set.of(local), forecasts.get(mine));
        assertEquals(java.util.Set.of(shared.getAcao(0)), forecasts.get(theirs));
        SharedOrdersRefresh.apply(prepare(command(false)), forecasts, (po,n) -> 100);
        assertEquals(1, forecasts.get(theirs).size(), "repeat refresh replaces forecast instead of duplicating");
    }

    @Test void validEmptySetClearsOnlyItsOwner() {
        SharedOrdersRefresh.apply(prepare(command(true)), forecasts, (po,n) -> 100);
        assertEquals(1, own.getAcaoSize()); assertEquals(0, shared.getAcaoSize()); assertEquals(0, absent.getAcaoSize());
    }

    @Test void mergedNationMembershipDoesNotChangeOwnerClassification() {
        me.addNacao(theirs);
        assertTrue(me.isNacao(theirs));
        assertFalse(control.services.OrdersList.ownedBy(theirs, me));
        assertTrue(control.services.OrdersList.ownedBy(mine, me));
        assertNotNull(prepare(command(false)));
    }

    @Test void localOwnerAndWrongGameAndWrongTurnRejectedBeforeMutation() {
        Comando local = new Comando(); local.setInfos(game);
        assertThrows(IllegalArgumentException.class, () -> prepare(local));
        Comando foreign = command(false); game.setId(2);
        assertThrows(IllegalArgumentException.class, () -> prepare(foreign));
        game.setId(1); game.setTurno(13);
        assertThrows(IllegalArgumentException.class, () -> prepare(foreign));
        assertEquals(1, shared.getAcaoSize()); assertEquals(1, own.getAcaoSize());
    }

    @Test void oneBadDetailRejectsEntireSetAndForeignPackagesAreRejected() {
        Comando c = command(false); c.addComando(own, action, List.of(), List.of(), null);
        assertThrows(IllegalArgumentException.class, () -> prepare(c));
        assertEquals(1, absent.getAcaoSize());
        Comando packages = command(false); packages.getPackages().put(mine.getId(), ";-;");
        assertThrows(IllegalArgumentException.class, () -> prepare(packages));
        Comando unknown = command(false); action.setCodigo("missing");
        unknown.addComando(shared, action, List.of(), List.of(), null);
        assertThrows(IllegalArgumentException.class, () -> prepare(unknown));
    }

    @Test void malformedCostedOrderCannotClearAnyActorOrForecast() {
        PersonagemOrdem previous = shared.getAcao(0), previousAbsent = absent.getAcao(0);
        action.setCodigo("411");
        Comando c = command(true);
        c.addComando(shared, action, List.of(), List.of(), null);
        var prepared = SharedOrdersRefresh.prepare(c, game, actors, Map.of("411", action));
        assertThrows(IndexOutOfBoundsException.class, () -> SharedOrdersRefresh.apply(prepared, forecasts,
                (po,n) -> new business.facade.AcaoFacade().getCusto(po,n,null,null)));
        assertSame(previous, shared.getAcao(0)); assertSame(previousAbsent, absent.getAcao(0));
        assertEquals(java.util.Set.of(previous, previousAbsent), forecasts.get(theirs));
        assertEquals(1, own.getAcaoSize());
    }
}
