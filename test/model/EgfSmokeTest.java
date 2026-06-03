package model;

import org.junit.jupiter.api.Test;
import persistenceCommons.XmlManager;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that PbmCommons.jar bundled in the Counselor can still load old
 * EGF files end-to-end (XmlManager + readResolve backward-compat machinery).
 */
class EgfSmokeTest {

    @Test
    void loadsOldFormatEgf() throws Exception {
        URL url = getClass().getResource("/egf/game_88_20.rr.egf");
        assertNotNull(url, "Test fixture missing from test/resources/egf/");

        Object result = XmlManager.getInstance().get(new File(url.toURI()));
        assertInstanceOf(World.class, result);

        World w = (World) result;
        assertNotNull(w.getPartida(),    "partida null");
        assertNotNull(w.getNacoes(),     "nacoes null");
        assertFalse(w.getNacoes().isEmpty(), "nacoes should not be empty");
        assertNotNull(w.getPackages(),
                "World.packages null — readResolve did not fire on old EGF");
    }
}
