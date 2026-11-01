package control.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LogSupportArchiveTest {
    @TempDir Path directory;
    private Path write(String name, String value) throws IOException {
        return Files.writeString(directory.resolve(name), value);
    }

    @Test void calendarWindowCrossesYearBoundaryAndDoesNotCollectOtherFiles() throws Exception {
        Path active = write("counselor.log", "today\n");
        write("counselor.log.2025-12-31", "yesterday\n");
        write("counselor.log.2025-12-30", "two days ago\n");
        write("counselor.log.2025-12-29", "too old");
        write("counselor.log.2026-01-02", "future");
        write("counselor.log.2025-12-31.bak", "backup");
        write("properties.config", "password=private");
        write("orders.rc.egf", "orders");
        var result = LogSupportArchive.create(active, directory.resolve("support.zip"), LocalDate.of(2026, 1, 1),
                StandardCharsets.UTF_8, List.of(), false);
        assertEquals(List.of("counselor.log.2025-12-30", "counselor.log.2025-12-31", "counselor.log"), result.files());
        try (ZipFile zip = new ZipFile(result.path().toFile())) {
            assertEquals(4, zip.size());
            assertEquals("today\n", new String(zip.getInputStream(zip.getEntry("counselor.log")).readAllBytes(), StandardCharsets.UTF_8));
            assertNull(zip.getEntry("properties.config"));
            assertNull(zip.getEntry("orders.rc.egf"));
        }
    }

    @Test void missingDaysAreSkippedAndNoLogsDoesNotReplaceAnExistingFile() throws Exception {
        Path destination = write("support.zip", "keep me");
        assertThrows(IOException.class, () -> LogSupportArchive.create(directory.resolve("counselor.log"), destination,
                LocalDate.of(2026, 1, 1), StandardCharsets.UTF_8, List.of(), true));
        assertEquals("keep me", Files.readString(destination));
        write("counselor.log.2025-12-31", "one day");
        var result = LogSupportArchive.create(directory.resolve("counselor.log"), destination,
                LocalDate.of(2026, 1, 1), StandardCharsets.UTF_8, List.of(), true);
        assertEquals(List.of("counselor.log.2025-12-31"), result.files());
    }

    @Test void credentialsAreRemovedFromAssignmentsJsonUrlsAndUnlabelledTokens() {
        String source = "pPlayerToken=old-credential&game=911\n"
                + "{\"PASSWORD\": \"two words secret\", \"message\": \"still visible\"}\n"
                + "secret: another secret with spaces\n"
                + "pEgfToken=abc%2Bdef%3D&turn=7\n"
                + "%22pToken%22%3A%22encoded-secret%22%2C%22game%22%3A911\n"
                + "Authorization: Bearer bearer-value\n"
                + "http://username:uri-password@example.invalid/path\n"
                + "current exact known+credential and known%2Bcredential\n"
                + "token without label: 0123456789abcdef0123456789ABCDEF\n"
                + "java.lang.IllegalStateException: stack remains\n";
        String cleaned = LogSupportArchive.redact(source, List.of("known+credential"));
        for (String secret : List.of("old-credential", "two words secret", "another secret", "abc%2Bdef",
                "encoded-secret", "bearer-value", "uri-password", "known+credential", "known%2Bcredential", "0123456789abcdef")) {
            assertFalse(cleaned.contains(secret), secret);
        }
        assertTrue(cleaned.contains("still visible"));
        assertTrue(cleaned.contains("game=911"));
        assertTrue(cleaned.contains("java.lang.IllegalStateException: stack remains"));
    }

    @Test void exportRedactsCopiesWithoutChangingSourceAndRequiresOverwritePermission() throws Exception {
        Path active = write("counselor.log", "pToken=private-value\nstack\n");
        Path target = write("support.zip", "existing");
        assertThrows(FileAlreadyExistsException.class, () -> LogSupportArchive.create(active, target, LocalDate.now(),
                StandardCharsets.UTF_8, List.of(), false));
        LogSupportArchive.create(active, target, LocalDate.now(), StandardCharsets.UTF_8, List.of(), true);
        assertEquals("pToken=private-value\nstack\n", Files.readString(active));
        try (ZipFile zip = new ZipFile(target.toFile())) {
            String exported = new String(zip.getInputStream(zip.getEntry("counselor.log")).readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(exported.contains("private-value"));
            assertTrue(exported.contains("stack"));
        }
    }

    @Test void escapedQuotesBackslashesAndMalformedQuotesCannotLeakCredentialTails() {
        for (String value : List.of("prefix\\\"secretTail", "prefix\\\\secretTail", "prefix\\\\\\\"secretTail")) {
            String json = "{\"password\":\"" + value + "\",\"message\":\"keep this\"}";
            String cleaned = LogSupportArchive.redact(json, List.of());
            assertFalse(cleaned.contains("secretTail"), cleaned);
            assertTrue(cleaned.contains("keep this"), cleaned);
            String encoded = java.net.URLEncoder.encode(json, StandardCharsets.UTF_8);
            String cleanedEncoded = LogSupportArchive.redact(encoded, List.of());
            assertFalse(cleanedEncoded.contains("secretTail"), cleanedEncoded);
            assertTrue(cleanedEncoded.contains("keep+this"), cleanedEncoded);
        }
        assertFalse(LogSupportArchive.redact("password=\"unterminated,secretTail", List.of()).contains("secretTail"));
        assertFalse(LogSupportArchive.redact("password%3D%22unterminated%2CsecretTail", List.of()).contains("secretTail"));
    }

    @Test void excessiveLogDoesNotReplaceDestinationOrLeaveTemporaryFiles() throws Exception {
        Path active = write("counselor.log", "");
        try (var channel = java.nio.channels.FileChannel.open(active, StandardOpenOption.WRITE)) {
            channel.position(16 * 1024 * 1024);
            channel.write(java.nio.ByteBuffer.wrap(new byte[]{1}));
        }
        Path target = write("support.zip", "keep me");
        assertThrows(IOException.class, () -> LogSupportArchive.create(active, target, LocalDate.now(),
                StandardCharsets.UTF_8, List.of(), true));
        assertEquals("keep me", Files.readString(target));
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
    }

    @Test void sourceAndNonZipDestinationsAreRejected() throws Exception {
        Path active = write("counselor.zip", "log data");
        assertThrows(IOException.class, () -> LogSupportArchive.create(active, active, LocalDate.now(),
                StandardCharsets.UTF_8, List.of(), true));
        Path config = write("properties.config", "keep configuration");
        assertThrows(IOException.class, () -> LogSupportArchive.create(active, config, LocalDate.now(),
                StandardCharsets.UTF_8, List.of(), true));
        assertEquals("keep configuration", Files.readString(config));
    }

    @Test void mailtoUsesFixedGmAndEncodesBodyWithoutAddingAttachmentParameters() {
        String uri = LogSupportArchive.emailUri("Counselor + logs", "Attach a file & describe\nNext line").toString();
        assertTrue(uri.startsWith("mailto:clashoflegends.thegame@gmail.com?subject="));
        assertFalse(uri.contains("&attach"));
        assertTrue(URLDecoder.decode(uri, StandardCharsets.UTF_8).contains("&body=Attach a file & describe\nNext line"));
        assertTrue(uri.contains("%2B"));
    }
}
