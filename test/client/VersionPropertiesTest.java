package client;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class VersionPropertiesTest {

    @Test
    void versionFileLoadsAndBuildIsInteger() throws Exception {
        Properties p = new Properties();
        try (var in = getClass().getResourceAsStream("/version_counselor.properties")) {
            assertNotNull(in, "version_counselor.properties not found on classpath");
            p.load(in);
        }
        String build = p.getProperty("BUILD");
        assertNotNull(build, "BUILD key missing");
        assertFalse(build.startsWith("${"), "BUILD contains unexpanded token: " + build);
        assertTrue(Integer.parseInt(build) > 0, "BUILD is not a positive integer: " + build);
    }
}
