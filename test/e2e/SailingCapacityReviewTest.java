package e2e;

import baseLib.BaseModel;
import control.services.SailingCapacityReview;
import control.services.TurnReview.Finding;
import control.services.TurnReview.Kind;
import control.services.TurnReview.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import model.*;
import org.junit.jupiter.api.Test;
import persistenceCommons.XmlManager;

import static org.junit.jupiter.api.Assertions.*;

class SailingCapacityReviewTest {
    static class Fixture extends RecruitmentReviewTest.Fixture {
        final List<Exercito> armies = new ArrayList<>();
        final TipoTropa ship = new TipoTropa();
        final AtomicInteger skipped = new AtomicInteger();

        Fixture() {
            game.getCenario().setCodigo("GOT13");
            city.getLocal().setCoordenadas("0101");
            city.getLocal().getTerreno().setAncoravel(true);
            ship.setCodigo("tr"); ship.setNome("Transport");
            ability(ship, ";TTN;", 1); ability(ship, ";TTT;", 250);
            race.getTropas().put(ship, 0); game.getCenario().getTipoTropas().put("tr", ship);
        }

        Personagem commander(String name, int land, int transports) {
            Personagem p = new Personagem(); p.setCodigo(name.substring(0, Math.min(5, name.length())).toLowerCase(java.util.Locale.ROOT)); p.setNome(name);
            p.setVida(100); p.setPericiaComandanteNatural(50);
            p.setNacao(nation); p.setLocal(city.getLocal());
            Exercito army = new Exercito(); army.setCodigo(name+"-army");
            army.setNacao(nation); army.setLocal(p.getLocal()); army.setComandante(p);
            put(army, troop, land); put(army, ship, transports);
            armies.add(army); actors.add(p); return p;
        }

        PersonagemOrdem order(Personagem actor, int slot, String code, int number, String... values) {
            Ordem definition = new Ordem(); definition.setCodigo(code); definition.setNumero(number);
            definition.setNome(code);
            if ("830".equals(code)) ability(definition, ";AMW;", 1);
            PersonagemOrdem order = new PersonagemOrdem(); order.setOrdem(definition);
            order.setParametrosId(new ArrayList<>(List.of(values))); actor.setAcao(slot, order);
            return order;
        }

        void sail(Personagem p) { order(p, 1, "830", 830, "e;nr"); }
        void recruit(Personagem p, String amount, TipoTropa type) {
            order(p, 0, "411", 411, amount, type.getCodigo(), "no", "no", "0");
        }
        PersonagemOrdem transfer(Personagem p, String target, String type, int amount) {
            return order(p, 0, "424", 424, target,
                    XmlManager.getInstance().toXml(new TreeMap<>(Map.of(type, amount))));
        }
        Exercito port(int ships) {
            Exercito army = new Exercito(); army.setCodigo("garrison"); army.setNacao(nation);
            army.setLocal(city.getLocal()); put(army, ship, ships); armies.add(army); return army;
        }
        List<Finding> audit() { return SailingCapacityReview.compute(actors, me, game, armies, skipped); }
    }

    static void ability(BaseModel target, String token, int value) {
        RecruitmentReviewTest.ability(target, token, value);
    }
    static void put(Exercito army, TipoTropa type, int amount) {
        Pelotao p = new Pelotao(); p.setTipoTropa(type); p.setQtd(amount);
        army.getPelotoes().put(type.getCodigo(), p);
    }
    static Finding only(Fixture f) {
        var rows = f.audit(); assertEquals(1, rows.size()); return rows.get(0);
    }

    private static Personagem newCommander(Fixture f, String name, String ident) {
        Personagem target = new Personagem(); target.setCodigo(ident); target.setNome(name);
        target.setVida(100); target.setPericiaComandanteNatural(50);
        target.setNacao(f.nation); target.setLocal(f.city.getLocal());
        f.actors.add(target);
        return target;
    }

    @Test void doransTransferToAreoCreatesANewLandArmyBeforeSailing() {
        Fixture f = new Fixture(); Personagem doran = f.commander("Doran Nymeros Martell", 500, 10);
        Personagem areo = newCommander(f, "Areo Hotah", "areo ");
        TipoTropa archers = new TipoTropa(); archers.setCodigo("ar");
        TipoTropa knights = new TipoTropa(); knights.setCodigo("knights"); ability(knights, ";TT2;", 2);
        TipoTropa sand = new TipoTropa(); sand.setCodigo("sand"); ability(sand, ";TT2;", 2);
        put(doran.getExercito(), archers, 750); put(doran.getExercito(), knights, 1250);
        put(doran.getExercito(), sand, 1000);
        f.order(doran, 0, "830", 830, "e;ne;ne;nw;nr");
        String troops = XmlManager.getInstance().toXml(new TreeMap<>(Map.of(
                "ar", 750, "inf", 500, "knights", 10000, "sand", 1000)));
        var transfer = f.order(doran, 1, "424", 424, "areo ", troops);
        assertTrue(f.audit().isEmpty(), "recognized outgoing troops must clear the finding");
        assertNull(areo.getExercito(), "Review must not actually create the recipient army");
        assertEquals(1250, doran.getExercito().getPelotoes().get("knights").getQtd());
        assertEquals("areo ", transfer.getParametrosId().get(0), "the fifth character remains data");
        assertEquals(troops, transfer.getParametrosId().get(1));
        transfer.getParametrosId().set(1, XmlManager.getInstance().toXml(new TreeMap<>(Map.of(
                "ar", 750, "knights", 10000, "sand", 1000))));
        assertTrue(f.audit().isEmpty(), "the reported500 remaining cargo also fits2500 capacity");
        Personagem anders = f.commander("Anders Yronwood", 3000, 10); f.sail(anders);
        Finding remaining = only(f);
        assertSame(anders, remaining.getActor(), "only the still-overloaded sailor remains");
        assertEquals(Kind.SAIL_CAPACITY, remaining.getKind());
        assertEquals(Level.WARN, remaining.getLevel());
    }

    @Test void meaningfulTrailingSpacesKeepDifferentTargetsDistinct() {
        Fixture f = new Fixture(); Personagem source = f.commander("Captain", 500, 1); f.sail(source);
        newCommander(f, "Hugh of the Vale", "hugh ");
        Personagem other = newCommander(f, "Hugh Grandison", "hugh");
        Local elsewhere = new Local(); elsewhere.setCodigo("9999"); other.setLocal(elsewhere);
        var transfer = f.transfer(source, "hugh ", "inf", 300);
        assertTrue(f.audit().isEmpty(), "explicit fifth space selects the same-hex recipient");
        transfer.getParametrosId().set(0, "hugh");
        assertEquals(Level.WARN, only(f).getLevel(), "exact other-hex target must not fall back to the spaced ID");
    }

    @Test void typedShortAndLongIdentifiersResolveWithoutChangingSavedParameters() {
        Fixture f = new Fixture(); Personagem source = f.commander("Captain", 500, 1); f.sail(source);
        newCommander(f, "Areo Hotah", "areo ");
        var transfer = f.transfer(source, "areo", "inf", 300);
        assertTrue(f.audit().isEmpty(), "short ID has no exact match, so right-space fallback is valid");
        transfer.getParametrosId().set(0, "  AREO Hotah");
        assertTrue(f.audit().isEmpty(), "typed prefix is case-insensitive and retains its fifth space");
        assertEquals("  AREO Hotah", transfer.getParametrosId().get(0));
    }

    @Test void foreignOrDeadExactTargetNeverFallsThroughToAnOwnedPaddedTarget() {
        for (boolean foreign : new boolean[]{false, true}) {
            Fixture f = new Fixture(); Personagem source = f.commander("Captain", 500, 1); f.sail(source);
            newCommander(f, "Hugh of the Vale", "hugh ");
            Personagem exact = newCommander(f, "Hugh Grandison", "hugh");
            if (foreign) exact.setNacao(new Nacao()); else exact.setVida(0);
            f.transfer(source, "hugh", "inf", 300);
            assertEquals(Level.WARN, only(f).getLevel());
            assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
        }
    }

    @Test void incomingTransfersResolveSignificantSpacesAtDependencyEndpoints() {
        Fixture f = new Fixture(); Personagem receiver = f.commander("Receiver", 200, 1);
        Personagem areo = f.commander("Areo Hotah", 0, 0); areo.setCodigo("areo ");
        Personagem third = f.commander("Third", 100, 0);
        f.sail(receiver); f.transfer(areo, receiver.getCodigo(), "inf", 100);
        f.transfer(third, "areo ", "inf", 100);
        assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
        assertEquals(Level.INFO, only(f).getLevel());
    }

    @Test void currentOverloadNamesTheSailSlotAndDoesNotNeedRecruitment() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1); f.sail(p);
        Finding row = only(f);
        assertEquals(Kind.SAIL_CAPACITY, row.getKind()); assertEquals(Level.WARN, row.getLevel());
        assertSame(p, row.getActor()); assertEquals(1, row.getSlot());
        assertArrayEquals(new String[]{"500", "250", "0"}, row.getArgs());
        assertEquals(0, f.skipped.get());
    }

    @Test void exactCapacityAndShipsWithoutCargoAreNotShortfalls() {
        Fixture f = new Fixture(); f.sail(f.commander("Exact", 250, 1));
        Personagem empty = f.commander("Warships", 0, 0);
        TipoTropa warship = new TipoTropa(); warship.setCodigo("ws"); ability(warship, ";TTN;", 1);
        put(empty.getExercito(), warship, 10); f.sail(empty);
        assertTrue(f.audit().isEmpty());
    }

    @Test void troopWeightsAndWeightlessTypesUseTheSharedRules() {
        Fixture f = new Fixture(); Personagem p = f.commander("Weighted", 200, 1);
        ability(f.troop, ";TT2;", 2);
        TipoTropa light = new TipoTropa(); light.setCodigo("zero"); ability(light, ";TT0;", 1);
        put(p.getExercito(), light, 10000); f.sail(p);
        assertEquals("400", only(f).getArgs()[0]);
    }

    @Test void variableShipCapacityAppliesOnlyToFirstAgeOrDeathMatch() {
        for (String mode : List.of("GOT13", "ME1A", "death")) {
            Fixture f = new Fixture(); ability(f.ship, ";TTT;", 50);
            if ("death".equals(mode)) ability(f.game, ";GDM;", 1);
            else f.game.getCenario().setCodigo(mode);
            f.sail(f.commander("Captain", 200, 1));
            if ("GOT13".equals(mode)) assertTrue(f.audit().isEmpty());
            else assertArrayEquals(new String[]{"200", "50", "0"}, only(f).getArgs());
        }
    }

    @Test void alternativeTransportAbilityContributesCapacity() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 300, 0);
        TipoTropa small = new TipoTropa(); small.setCodigo("small");
        ability(small, ";TTN;", 1); ability(small, ";TTT5;", 100);
        put(p.getExercito(), small, 2); f.sail(p);
        assertTrue(f.audit().isEmpty(), "legacy capacity is 2x250");
        ability(f.game, ";GDM;", 1);
        assertEquals("200", only(f).getArgs()[1]);
    }

    @Test void ownGarrisonContributesOnlyItsShipsAndAlliedShipsDoNotCount() {
        Fixture f = new Fixture(); f.sail(f.commander("Captain", 500, 1));
        Exercito port = f.port(1); put(port, f.troop, 99999);
        assertTrue(f.audit().isEmpty());
        port.setNacao(new Nacao());
        assertEquals(Kind.SAIL_CAPACITY, only(f).getKind());
    }

    @Test void recruitmentCanCreateAnOverloadWhileOrdersAndRostersRemainUnchanged() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 200, 1);
        f.sail(p); f.recruit(p, "100", f.troop);
        Finding row = only(f); assertEquals(Kind.SAIL_CAPACITY_PLANNED, row.getKind());
        assertEquals(Level.WARN, row.getLevel()); assertEquals("300", row.getArgs()[0]);
        assertEquals(200, p.getExercito().getPelotoes().get("inf").getQtd());
        assertEquals("100", p.getAcoes().get(0).getParametrosId().get(0));
    }

    @Test void recruitingShipsCanResolveAnExistingShortfallConditionally() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1);
        f.sail(p); f.recruit(p, "1", f.ship);
        assertTrue(f.audit().isEmpty());
    }

    @Test void maximumRecruitmentUsesCityAndPerOrderCeilings() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 100, 1);
        f.sail(p); f.recruit(p, "", f.troop);
        assertEquals("400", only(f).getArgs()[0]);
        ability(f.game.getCenario(), ";GRL100;", 100);
        assertTrue(f.audit().isEmpty(), "blank/max is now capped at100, so cargo200 fits");
    }

    @Test void transferringLandTroopsOutRemovesTheShortfallWarning() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1);
        f.sail(p); var transfer = f.transfer(p, "Guarnicao", "inf", 300);
        String original = transfer.getParametrosId().get(1);
        assertTrue(f.audit().isEmpty());
        assertEquals(500, p.getExercito().getPelotoes().get("inf").getQtd());
        assertEquals(original, transfer.getParametrosId().get(1));
        assertEquals(1, f.armies.size(), "no actual garrison was created");
    }

    @Test void incomingTroopsCanCreateAnOverload() {
        Fixture f = new Fixture(); Personagem p = f.commander("Receiver", 200, 1);
        Personagem source = f.commander("Sender", 200, 0);
        f.sail(p); f.transfer(source, p.getCodigo(), "inf", 100);
        assertEquals("300", only(f).getArgs()[0]);
        assertEquals(200, source.getExercito().getPelotoes().get("inf").getQtd());
    }

    @Test void shipsTransferredToTheGarrisonAreNotCountedTwice() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 600, 2);
        f.sail(p); f.transfer(p, "Guarnicao", "tr", 1);
        Finding row = only(f); assertEquals(Level.WARN, row.getLevel());
        assertArrayEquals(new String[]{"600", "250", "250", "Captain / 424"}, row.getArgs());
    }

    @Test void incomingGarrisonShipsCanCoverTheShortfall() {
        Fixture f = new Fixture(); Personagem p = f.commander("Receiver", 500, 0);
        Personagem source = f.commander("Sender", 0, 2);
        f.sail(p); f.transfer(source, "Guarnicao", "tr", 2);
        assertTrue(f.audit().isEmpty());
    }

    @Test void executionNumberNotUiSlotControlsWhetherAChangePrecedesSail() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1);
        f.order(p, 0, "830", 830, "e;nr");
        f.order(p, 4, "424", 900, "Guarnicao",
                XmlManager.getInstance().toXml(new TreeMap<>(Map.of("inf", 500))));
        assertEquals(Kind.SAIL_CAPACITY, only(f).getKind());
        p.getAcoes().get(4).getOrdem().setNumero(424);
        assertTrue(f.audit().isEmpty());
        p.getAcoes().get(4).getOrdem().setNumero(830);
        assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
        assertEquals(Level.WARN, only(f).getLevel());
    }

    @Test void competingRecruitmentDoesNotInventASecondCopyOfCityCapacity() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 100, 1);
        Personagem other = f.commander("Other", 0, 0);
        f.sail(p); f.recruit(p, "", f.troop); f.recruit(other, "", f.troop);
        assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
        assertEquals(Level.INFO, only(f).getLevel());
    }

    @Test void garrisonShipsCannotBePromisedToTwoDifferentSailors() {
        Fixture f = new Fixture(); f.sail(f.commander("First", 500, 1));
        f.sail(f.commander("Second", 500, 1)); f.port(3);
        var rows = f.audit(); assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.getKind() == Kind.SAIL_CAPACITY_UNCERTAIN));
    }

    @Test void negativeTransferAndUncertainAnchoringDoNotClearTheWarning() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1);
        f.sail(p); f.transfer(p, "Guarnicao", "inf", -300);
        assertEquals(Level.WARN, only(f).getLevel());
        f.transfer(p, "Guarnicao", "inf", 300);
        p.getLocal().getTerreno().setAncoravel(false);
        assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
        assertEquals(Level.WARN, only(f).getLevel());
    }

    @Test void recruitmentCanMakeAnotherSailorCompeteForTheSameGarrison() {
        Fixture f = new Fixture(); Personagem first = f.commander("First", 500, 1);
        Personagem second = f.commander("Second", 200, 1);
        f.sail(first); f.sail(second); f.recruit(second, "100", f.troop); f.port(3);
        var rows = f.audit(); assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.getKind() == Kind.SAIL_CAPACITY_UNCERTAIN));
    }

    @Test void multipleRelevantChangesAreQualifiedInsteadOfSilentlyReordered() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1);
        f.sail(p); f.transfer(p, "Guarnicao", "inf", 300);
        f.order(p, 2, "411", 411, "100", "inf", "no", "no", "0");
        assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
    }

    @Test void malformedRostersCountAsSkippedWithoutLosingOtherFindings() {
        Fixture f = new Fixture(); Personagem bad = f.commander("Bad", 500, 1);
        f.sail(bad); bad.getExercito().getPelotoes().put("broken", null);
        f.sail(f.commander("Valid", 500, 1));
        assertEquals(1, f.audit().size()); assertEquals(1, f.skipped.get());
    }

    @Test void unrelatedMalformedOrderDoesNotHideAKnownShortfall() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1); f.sail(p);
        Personagem other = f.commander("Other", 0, 0);
        f.order(other, 0, "not-a-number", 0);
        assertEquals(Kind.SAIL_CAPACITY, only(f).getKind());
        other.getAcoes().get(0).setOrdem(new Ordem());
        assertEquals(Kind.SAIL_CAPACITY, only(f).getKind());
        assertEquals(0, f.skipped.get());
    }

    @Test void unknownPrecedenceQualifiesOnlyTheRelevantSailor() {
        Fixture f = new Fixture(); Personagem p = f.commander("Captain", 500, 1); f.sail(p);
        var change = f.transfer(p, "Guarnicao", "inf", 300);
        change.getOrdem().setCodigo("unknown");
        change.getOrdem().setNumero(0);
        // An unnumbered additional water order is relevant but its execution precedence is unknown.
        ability(change.getOrdem(), ";AMW;", 1);
        var rows = f.audit();
        assertTrue(rows.stream().anyMatch(r -> r.getSlot() == 1
                && r.getKind() == Kind.SAIL_CAPACITY_UNCERTAIN && r.getLevel() == Level.WARN));
        assertEquals(0, f.skipped.get());
    }

    @Test void anIncomingChangeToTheTransferSourceMakesTheChainUncertain() {
        Fixture f = new Fixture(); Personagem receiver = f.commander("Receiver", 200, 1);
        Personagem sender = f.commander("Sender", 0, 0);
        Personagem third = f.commander("Third", 100, 0);
        f.sail(receiver); f.transfer(sender, receiver.getCodigo(), "inf", 100);
        f.transfer(third, sender.getCodigo(), "inf", 100);
        assertEquals(Kind.SAIL_CAPACITY_UNCERTAIN, only(f).getKind());
        assertEquals(Level.INFO, only(f).getLevel());
    }

    @Test void nonSailingAndReadOnlyActorsAreExcluded() {
        Fixture f = new Fixture(); f.commander("Marcher", 500, 1);
        Personagem foreign = f.commander("Read only", 500, 1); f.sail(foreign);
        foreign.setNacao(new Nacao());
        assertTrue(f.audit().isEmpty());
    }
}
