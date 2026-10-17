package e2e;

import baseLib.BaseModel;
import control.services.RecruitmentReview;
import control.services.TurnReview.Finding;
import control.services.TurnReview.Kind;
import java.util.*;
import model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecruitmentReviewTest {
    static void ability(BaseModel target, String token, int value) {
        Habilidade h = new Habilidade(); h.setCodigo(token); h.setValor(value); target.addHabilidade(h);
    }
    static long count(List<Finding> findings, Kind kind) {
        return findings.stream().filter(f -> f.getKind() == kind).count();
    }
    static Finding first(List<Finding> findings, Kind kind) {
        return findings.stream().filter(f -> f.getKind() == kind).findFirst().orElseThrow();
    }
    static class Fixture {
        final Jogador me = new Jogador(); final Nacao nation = new Nacao();
        final Raca race = new Raca(); final TipoTropa troop = new TipoTropa();
        final Partida game = new Partida(); final Cidade city = new Cidade();
        final List<BaseModel> actors = new ArrayList<>(); final Produto[] products = new Produto[7];
        Fixture() {
            me.setNome("Player"); me.setCodigo("player"); nation.setNome("Nation"); nation.setCodigo("n");
            nation.setOwner(me); me.addNacao(nation); nation.setRaca(race); nation.setMoney(100000);
            race.setNome("Race"); race.setCodigo("race"); troop.setNome("Infantry"); troop.setCodigo("inf");
            race.getTropas().put(troop, 0); game.setCenario(new Cenario());
            game.getCenario().getTipoTropas().put("by name", troop);
            city.setNome("City"); city.setCodigo("city"); city.setNacao(nation); city.setRaca(race); city.setTamanho(3);
            nation.addCidade(city);
            Local l = new Local(); l.setCodigo("0101"); l.setCidade(city); city.setLocal(l);
            Terreno terrain = new Terreno(); terrain.setCodigo("P"); l.setTerreno(terrain);
            String[] codes = {"CR", "BR", "FE", "AC", "CM", "LE", "MO"};
            for (int i=0; i<7; i++) {
                Produto p = new Produto(); p.setCodigo(codes[i]); p.setNome(codes[i]); products[i]=p;
                game.getCenario().getProdutos().put(codes[i],p); city.setEstoque(p,10000);
            }
        }
        Personagem order(String code, String... params) {
            Personagem actor = new Personagem(); actor.setCodigo("p"+actors.size()); actor.setNome("Actor"+actors.size());
            actor.setNacao(nation); actor.setLocal(city.getLocal());
            Ordem o = new Ordem(); o.setCodigo(code); o.setNome("Recruit");
            PersonagemOrdem po = new PersonagemOrdem(); po.setOrdem(o); po.setParametrosId(new ArrayList<>(List.of(params)));
            actor.setAcao(0,po); actors.add(actor); return actor;
        }
        Personagem recruit(int qty, String weapon, String armour) {
            return order("411", Integer.toString(qty), "inf", weapon, armour, "0");
        }
        List<Finding> run() { return RecruitmentReview.compute(actors,me,game); }
    }

    @Test void twoCommandersCompeteForCitySlotsAndFindingKeepsIdentity() {
        Fixture f = new Fixture(); Personagem actor=f.recruit(200,"no","no"); f.recruit(200,"no","no");
        var rows=f.run(); assertEquals(1,count(rows,Kind.RECRUIT_CAPACITY));
        assertArrayEquals(new String[]{"City","400","300"},first(rows,Kind.RECRUIT_CAPACITY).getArgs());
        assertSame(actor,first(rows,Kind.RECRUIT_CAPACITY).getActor());
        assertEquals(0,first(rows,Kind.RECRUIT_CAPACITY).getSlot());
        assertEquals(1,count(rows,Kind.RECRUIT_SCOPE));
    }

    @Test void maximumOrdersAreDynamicAndCompeteRatherThanMeaningZero() {
        Fixture f=new Fixture(); f.recruit(0,"no","no"); f.recruit(-1,"no","no");
        assertArrayEquals(new String[]{"City","600","300"},first(f.run(),Kind.RECRUIT_CAPACITY).getArgs());
    }

    @Test void newRulesSlotFactorsAndPerOrderCapAreApplied() {
        Fixture f=new Fixture(); ability(f.game,";GNR;",1); ability(f.troop,";TCR10;",10);
        ability(f.game.getCenario(),";GRL100;",100); f.recruit(20,"no","no");
        assertArrayEquals(new String[]{"20","10","City"},first(f.run(),Kind.RECRUIT_LIMIT).getArgs());
    }

    @Test void materialsAreSummedAcrossOrdersAndNotMutated() {
        Fixture f=new Fixture(); ability(f.troop,";TEI10;",10); f.city.setEstoque(f.products[2],500);
        f.recruit(30,"no","no"); f.recruit(30,"no","no");
        assertArrayEquals(new String[]{"City","FE","600","500"},first(f.run(),Kind.RECRUIT_MATERIAL).getArgs());
        assertEquals(500,f.city.getEstoque(f.products[2])); assertEquals(2,f.actors.size());
        assertEquals(1,count(f.run(),Kind.RECRUIT_MATERIAL));
    }

    @Test void equipmentCannotSpendRecruitmentOrOtherEquipmentStockTwice() {
        Fixture f=new Fixture(); ability(f.troop,";TEI10;",10); f.city.setEstoque(f.products[2],115);
        f.recruit(10,"FE","FE"); var rows=f.run();
        assertEquals(0,count(rows,Kind.RECRUIT_MATERIAL));
        assertArrayEquals(new String[]{"City","FE","10","10","15"},first(rows,Kind.RECRUIT_EQUIPMENT).getArgs());
        assertEquals(0,count(rows,Kind.RECRUIT_CAPACITY));
    }

    @Test void capitalWaiverAndWoodDiscountAreIncluded() {
        Fixture f=new Fixture(); ability(f.troop,";TTC;",1); ability(f.nation,";PCR;",1); f.city.setCapital(true);
        ability(f.game.getCenario(),";SNC;",1);
        f.city.setEstoque(f.products[0],0); f.city.setEstoque(f.products[6],0); f.recruit(50,"no","no");
        assertEquals(0,count(f.run(),Kind.RECRUIT_MATERIAL));
        ability(f.troop,";TTN;",1); ability(f.troop,";TTW;",100); ability(f.nation,";PSW;",2);
        f.city.setEstoque(f.products[5],2000);
        assertArrayEquals(new String[]{"City","LE","2500","2000"},first(f.run(),Kind.RECRUIT_MATERIAL).getArgs());
    }
    @Test void capitalWaiverRequiresTheScenarioCapitalRule() {
        Fixture f=new Fixture(); ability(f.troop,";TTC;",1); ability(f.nation,";PCR;",1); f.city.setCapital(true);
        f.city.setEstoque(f.products[0],0); f.city.setEstoque(f.products[6],0); f.recruit(10,"no","no");
        assertEquals(2,count(f.run(),Kind.RECRUIT_MATERIAL));
    }

    @Test void foodProvisionIsSeparateFromFoodToRaiseTroops() {
        Fixture f=new Fixture(); ability(f.troop,";TEF100;",100); f.city.setEstoque(f.products[4],1100);
        f.order("413","10","inf","no","no","200");
        assertEquals(0,count(f.run(),Kind.RECRUIT_MATERIAL));
        assertArrayEquals(new String[]{"City","200","100"},first(f.run(),Kind.RECRUIT_PROVISION).getArgs());
    }

    @Test void musterSharesCapacityButIsExplicitlyConditionalAndIncludesFood() {
        Fixture f=new Fixture(); f.city.setTamanho(4); f.city.setEstoque(f.products[4],1000);
        f.order("401","inf"); f.recruit(50,"no","no"); var rows=f.run();
        assertEquals(1,count(rows,Kind.RECRUIT_CONDITIONAL)); assertEquals(1,count(rows,Kind.RECRUIT_CAPACITY));
        assertEquals(1,count(rows,Kind.RECRUIT_PROVISION));
    }

    @Test void goldIncludesDiscountAndCompetingRecruitRequestsAcrossNation() {
        Fixture f=new Fixture(); f.troop.setRecruitCostMoney(10); ability(f.nation,";NDH;",50);
        f.nation.setMoney(99); f.recruit(10,"no","no"); f.recruit(10,"no","no");
        assertArrayEquals(new String[]{"100","99"},first(f.run(),Kind.RECRUIT_GOLD).getArgs());
    }

    @Test void missingStocksAreUnknownRatherThanEmptyAndMalformedOrderDoesNotReserve() {
        Fixture f=new Fixture(); ability(f.troop,";TEI10;",10); f.city.getEstoques().remove(f.products[2]);
        f.recruit(10,"no","no"); assertEquals(1,count(f.run(),Kind.RECRUIT_INCOMPLETE));
        assertEquals(0,count(f.run(),Kind.RECRUIT_MATERIAL));
        f.actors.clear(); f.order("411","300","inf","no","no","invalid"); f.recruit(1,"no","no");
        assertEquals(0,count(f.run(),Kind.RECRUIT_CAPACITY));
        assertTrue(count(f.run(),Kind.RECRUIT_INCOMPLETE)>0);
    }

    @Test void noResourceRecruitDoesNotInterpretAllyTargetAsWeaponOrRequireFood() {
        Fixture f=new Fixture(); ability(f.game.getCenario(),";SNR;",1); ability(f.troop,";TTW;",100);
        f.order("411","10","inf","ally-ident");
        assertEquals(0,count(f.run(),Kind.RECRUIT_MATERIAL)); assertEquals(0,count(f.run(),Kind.RECRUIT_INCOMPLETE));
        f.order("413","10","inf","no","no","0");
        assertEquals(1,count(f.run(),Kind.RECRUIT_INCOMPLETE));
    }

    @Test void unavailableTroopsFlagEligibilityAndUnknownTypeIsNotSilentlySubstituted() {
        Fixture f=new Fixture(); ability(f.troop,";TTRP;",1); f.recruit(1,"no","no");
        assertEquals(1,count(f.run(),Kind.RECRUIT_ELIGIBILITY));
        f.order("411","10","missing","no","no","0"); assertEquals(1,count(f.run(),Kind.RECRUIT_INCOMPLETE));
    }

    @Test void failedRecruitEligibilityStillCompetesForFoodProvision() {
        Fixture f=new Fixture(); ability(f.troop,";TTRP;",1); f.city.setEstoque(f.products[4],100);
        f.order("411","1","inf","no","no","80");
        TipoTropa eligible=new TipoTropa(); eligible.setCodigo("other"); eligible.setNome("Other");
        f.race.getTropas().put(eligible,0); f.game.getCenario().getTipoTropas().put("other",eligible);
        f.order("411","1","other","no","no","80");
        assertArrayEquals(new String[]{"City","160","100"},first(f.run(),Kind.RECRUIT_PROVISION).getArgs());
        assertEquals(1,count(f.run(),Kind.RECRUIT_ELIGIBILITY));
    }

    @Test void readOnlyAlliedActorsAndNonRecruitOrdersAreExcluded() {
        Fixture f=new Fixture(); Personagem ally=f.recruit(999,"no","no");
        Nacao other=new Nacao(); other.setNome("Other"); other.setCodigo("other");
        Jogador owner=new Jogador(); owner.setNome("Other player"); owner.setCodigo("other player");
        other.setOwner(owner); ally.setNacao(other);
        f.order("1001"," "); assertTrue(f.run().isEmpty());
    }
    @Test void malformedActorDoesNotHideValidCityShortage() {
        Fixture f=new Fixture(); f.actors.add(null); f.recruit(200,"no","no"); f.recruit(200,"no","no");
        var rows=f.run(); assertEquals(1,count(rows,Kind.RECRUIT_INCOMPLETE));
        assertEquals(1,count(rows,Kind.RECRUIT_CAPACITY));
    }
}
