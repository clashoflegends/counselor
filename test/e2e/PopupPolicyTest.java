package e2e;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regression guard for the approved native-picker/fatal-error exception policy. */
class PopupPolicyTest {
    @Test void routineFlowsCannotAddDirectOrSharedMessageDialogs() throws Exception {
        Path src = Path.of("src");
        Pattern forbidden = Pattern.compile("(?:JOptionPane\\.(?:show\\w+Dialog)|SysApoio\\.showDialog(?:Error|Alert|Info)|new\\s+JOptionPane)\\s*\\(");
        try (var files = Files.walk(src)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.endsWith(Path.of("client", "PbmApplication.java"))) continue; // E03: fatal errors only
                String code = Files.readString(file).replaceAll("(?m)^\\s*(?:import|//|\\*).*", "");
                assertFalse(forbidden.matcher(code).find(), file.toString());
                assertFalse(code.contains("new ProgressMonitor("), file.toString());
                assertFalse(Pattern.compile("(?:new (?:javax\\.swing\\.)?JDialog|super)\\([^;\\n]*, true\\)").matcher(code).find(), file.toString());
            }
        }
    }

    @Test void orderEntryUsesInlineQuantitiesAndReviewInsteadOfConsent() throws Exception {
        String controller = Files.readString(Path.of("src/control/OrdemControler.java"));
        assertFalse(controller.contains("confirmSizeCapOverride"));
        String troops = Files.readString(Path.of("src/gui/subtabs/SubTabTropas.java"));
        assertFalse(troops.contains("new IntegerEditor("));
        assertTrue(troops.contains("new QuantityCellEditor("));
        String review = Files.readString(Path.of("src/control/services/TurnReview.java"));
        assertTrue(review.contains("Kind.SIZE_CAP"));
    }
}
