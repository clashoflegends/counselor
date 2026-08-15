package e2e;

import gui.services.ComponentFactory;
import gui.subtabs.SubTabOrdem;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTextField;
import model.Ordem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * KI-017: an order slot's saved parameters are read by POSITION - nothing in
 * {@code OrdemFacade.getParametroDisplay} looks at which order is currently selected. So when the player
 * changes the order in a slot that already holds a saved one, the old value used to be poured into the new
 * order's widget. Combos shrug it off (the display is not found, they fall to index 0), but a free-text
 * parameter takes it verbatim - {@code ComponentFactory} does {@code setText} with no validation and reads
 * {@code getText()} straight back at save, so a move's hex "0512" arrived as a recruit quantity, a gold
 * amount, or a 512% percentage.
 *
 * <p>These pin the guard that decides whether a saved value may seed the widget, and the raw seeding
 * behaviour that makes the guard necessary in the first place.
 */
class OrderParamSeedTest {

    private static final String HEX = "0512";

    private static Ordem ordem(int numero, String... controles) {
        Ordem o = new Ordem();
        o.setNumero(numero);
        o.setNome("Ordem " + numero);
        o.setParametrosIde(controles);
        String[] display = new String[controles.length];
        Arrays.fill(display, "p");
        o.setParametrosIdeDisplay(display);
        return o;
    }

    private static PersonagemOrdem saved(Ordem ord, String... valores) {
        PersonagemOrdem po = new PersonagemOrdem();
        po.setNome("Probe");
        po.setOrdem(ord);
        po.setParametrosId(new ArrayList<>(Arrays.asList(valores)));
        po.setParametrosDisplay(new ArrayList<>(Arrays.asList(valores)));
        return po;
    }

    /** The value a save would collect from a freshly built widget. */
    private static String collected(Component comp) {
        ComponentFactory cf = new ComponentFactory();
        List<String> ids = new ArrayList<>();
        List<String> display = new ArrayList<>();
        cf.getParametros(comp, ids, display);
        return ids.get(0);
    }

    @Test
    void reopeningTheSameOrderStillRestoresItsParameter() {
        PersonagemOrdem gravada = saved(ordem(100, "Coordenada_Navio"), HEX);
        assertTrue(SubTabOrdem.isSameParametroTipo(gravada, "Coordenada_Navio", 0));
    }

    @Test
    void switchingToAnOrderWithADifferentParameterTypeDoesNotCarryTheValue() {
        PersonagemOrdem gravada = saved(ordem(100, "Coordenada_Navio"), HEX);
        // the reported case: a hex becoming a recruit quantity
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Quantidade", 0));
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Ouro", 0));
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Percentage", 0));
    }

    @Test
    void switchingBetweenTwoOrdersOfTheSameShapeKeepsTheTargetPerParameter() {
        // deliberate: retyping the same hex when you swap one movement order for another is the useful
        // half of the seeding, and it stays. The second parameter differs, so only that one is dropped.
        PersonagemOrdem gravada = saved(ordem(100, "Coordenada_Navio", "Quantidade"), HEX, "40");
        assertTrue(SubTabOrdem.isSameParametroTipo(gravada, "Coordenada_Navio", 0));
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Ouro", 1));
    }

    @Test
    void aHexTargetCarriesAcrossOrdersWithADifferentRange() {
        // Coordenada* and Cidade* match as families: those pickers only select from their own model, so a
        // target the new order cannot reach falls back to the actor's hex rather than being submitted.
        PersonagemOrdem gravada = saved(ordem(100, "Coordenada_12"), HEX);
        assertTrue(SubTabOrdem.isSameParametroTipo(gravada, "Coordenada_5", 0));
        assertTrue(SubTabOrdem.isSameParametroTipo(gravada, "Coordenada_Navio", 0));
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Quantidade", 0));
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Cidade_Any", 0));

        PersonagemOrdem cidade = saved(ordem(101, "Cidade_Nacao"), HEX);
        assertTrue(SubTabOrdem.isSameParametroTipo(cidade, "Cidade_Any", 0));
        assertFalse(SubTabOrdem.isSameParametroTipo(cidade, "Ouro", 0));
    }

    @Test
    void aSpellParameterStillRestores() {
        // a spell order records the "Variado" placeholder, not the spell's own parameter key, so the type
        // check cannot apply there - it must stay pass-through or a saved spell target would not restore.
        PersonagemOrdem gravada = saved(ordem(700, "Magia_Known", "variado"), "F1", HEX);
        assertTrue(SubTabOrdem.isSameParametroTipo(gravada, "Coordenada", 1));
    }

    @Test
    void anEmptySlotSeedsNothingSoTheGuardStaysOutOfTheWay() {
        // nothing saved: the seed is "" whichever way this answers, so it must not block a restore
        assertTrue(SubTabOrdem.isSameParametroTipo(null, "Quantidade", 0));
        // a saved order that declares no parameters at all has nothing to hand over either
        assertFalse(SubTabOrdem.isSameParametroTipo(saved(ordem(100)), "Quantidade", 0));
    }

    @Test
    void aParameterTheSavedOrderNeverHadIsNotSeeded() {
        // getParametroIde returns "-" past the end of the saved order's list; it must not match a real control
        PersonagemOrdem gravada = saved(ordem(100, "Coordenada_Navio"), HEX);
        assertFalse(SubTabOrdem.isSameParametroTipo(gravada, "Quantidade", 1));
    }

    @Test
    void aFreeTextParameterTakesWhateverItIsSeededWithAndHandsItBackAtSave() {
        // why the guard exists: no formatter validation happens on setText, and nothing commits the edit,
        // so an unguarded seed reaches the saved order exactly as it was - even 512 in a 1-100 field.
        ComponentFactory cf = new ComponentFactory();
        Component quantidade = cf.getParametroComponent("Quantidade", HEX, HEX, null);
        assertTrue(quantidade instanceof JTextField);
        assertEquals(HEX, collected(quantidade));

        Component percentage = cf.getParametroComponent("Percentage", HEX, HEX, null);
        assertEquals(HEX, collected(percentage));

        // and with the guard's blank seed there is nothing to hand back
        assertEquals("", collected(cf.getParametroComponent("Quantidade", "", "", null)));
    }
}
