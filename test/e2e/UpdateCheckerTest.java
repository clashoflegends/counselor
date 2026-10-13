package e2e;

import control.services.UpdateChecker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code UpdateChecker.classify} - the whole decision behind the About box's "check for updates"
 * button, separated from the network so it can be tested at all.
 *
 * The case that matters is the UNREADABLE one. {@code fetchLatestBuild} returns -1 when the GitHub
 * response carries no "Counselor 2.NNN" release name, and an unparseable local build parses to -1
 * too. Before the button existed that only ever reached the log as "latest on GitHub 2.-1". Behind
 * a button a player pressed, a -1 compared numerically would read as "you have the latest version"
 * - a confident wrong answer to the one question the button exists to answer. Both sides negative
 * must be FAILED.
 */
class UpdateCheckerTest {

    @Test
    void aHigherBuildOnGitHubIsAnUpdate() {
        final UpdateChecker.Result r = UpdateChecker.classify(934, 935);
        assertEquals(UpdateChecker.Status.NEWER, r.getStatus());
        assertTrue(r.isNewer());
        assertEquals("2.935", r.getLatestVersion());
        assertEquals("2.934", r.getLocalVersion());
    }

    @Test
    void theSameBuildIsUpToDate() {
        final UpdateChecker.Result r = UpdateChecker.classify(935, 935);
        assertEquals(UpdateChecker.Status.CURRENT, r.getStatus());
        assertFalse(r.isNewer());
        assertNull(r.getLatestVersion(), "nothing to download when you are current");
    }

    /**
     * Master runs one build AHEAD of the newest release (the CI bump-build job commits BUILD=N+1
     * after every release), so this is the maintainer's everyday case and it must not offer a
     * downgrade.
     */
    @Test
    void aDevBuildAheadOfTheLatestReleaseIsUpToDate() {
        assertEquals(UpdateChecker.Status.CURRENT, UpdateChecker.classify(935, 934).getStatus());
    }

    @Test
    void anUnreadableGitHubResponseIsAFailureNotAReassurance() {
        final UpdateChecker.Result r = UpdateChecker.classify(935, -1);
        assertEquals(UpdateChecker.Status.FAILED, r.getStatus(),
                "a -1 latest build means the release name could not be read, not that we are current");
        assertFalse(r.isNewer());
        assertNull(r.getLatestVersion());
    }

    @Test
    void anUnreadableLocalBuildIsAlsoAFailure() {
        assertEquals(UpdateChecker.Status.FAILED, UpdateChecker.classify(-1, 935).getStatus());
    }

    @Test
    void bothUnreadableIsAFailure() {
        assertEquals(UpdateChecker.Status.FAILED, UpdateChecker.classify(-1, -1).getStatus());
    }
}
