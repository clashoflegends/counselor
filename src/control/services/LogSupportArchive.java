package control.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Local, bounded diagnostic export. Never reads configuration or sends email. */
public final class LogSupportArchive {
    public static final String GM_EMAIL = "clashoflegends.thegame@gmail.com";
    private static final int MAX_FILE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 48 * 1024 * 1024;
    private static final String KEY = "[a-z0-9_.-]{0,48}(?:token|password|passwd|pwd|secret|authorization|api[_-]?key)[a-z0-9_.-]{0,16}";
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)((?<![a-z0-9_])[\"']?" + KEY + "[\"']?\\s*[:=]\\s*)"
            + "(?:\"(?:[^\"\\\\\\r\\n]++|\\\\[^\\r\\n])*+\"|'(?:[^'\\\\\\r\\n]++|\\\\[^\\r\\n])*+'|[\"'][^\\r\\n]*|[^\\r\\n&,;}<>]*)");
    private static final String ENCODED_DOUBLE_QUOTED = "%22(?:%5c(?:%[0-9a-f]{2}|[^%\\r\\n])|(?!(?:%22|%5c))[^\\r\\n])*+%22";
    private static final String ENCODED_SINGLE_QUOTED = "%27(?:%5c(?:%[0-9a-f]{2}|[^%\\r\\n])|(?!(?:%27|%5c))[^\\r\\n])*+%27";
    private static final Pattern ENCODED_ASSIGNMENT = Pattern.compile(
            "(?i)((?:%22|%27)?" + KEY + "(?:%22|%27)?(?:%20|\\s)*(?:%3a|%3d)(?:%20|\\s)*)"
            + "(?:" + ENCODED_DOUBLE_QUOTED + "|" + ENCODED_SINGLE_QUOTED
            + "|(?:%22|%27)[^\\r\\n]*|[^\\r\\n]*?(?=%26|%2c|\\r|\\n|$))");
    private static final Pattern LONG_HEX = Pattern.compile("(?i)(?<![a-f0-9])[a-f0-9]{32,}(?![a-f0-9])");
    private static final Pattern URL_CREDENTIALS = Pattern.compile("(?i)([a-z][a-z0-9+.-]*://)[^\\s/@]+:[^\\s/@]+@");

    private LogSupportArchive() {}

    public record Archive(Path path, LocalDate firstDay, LocalDate lastDay, List<String> files) {}

    public static Archive create(Path activeLog, Path destination, LocalDate today,
            Charset encoding, List<String> credentials, boolean replace) throws IOException {
        Path active = activeLog.toAbsolutePath().normalize();
        Path target = destination.toAbsolutePath().normalize();
        if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")
                || Files.isSymbolicLink(target) || target.equals(active)) {
            throw new IOException("Invalid archive destination");
        }
        List<Path> sources = new ArrayList<>();
        for (int ago = 2; ago >= 0; ago--) {
            Path rotated = active.resolveSibling(active.getFileName() + "." + today.minusDays(ago));
            if (Files.isRegularFile(rotated, LinkOption.NOFOLLOW_LINKS)) sources.add(rotated);
        }
        if (Files.isRegularFile(active, LinkOption.NOFOLLOW_LINKS)) sources.add(active);
        if (sources.isEmpty()) throw new IOException("No log files available");
        for (Path source : sources) {
            if (target.equals(source) || (Files.exists(target) && Files.isSameFile(target, source))) {
                throw new IOException("Archive would replace a log file");
            }
        }
        if (!replace && Files.exists(target)) throw new FileAlreadyExistsException(target.toString());
        Path temporary = Files.createTempFile(target.getParent(), ".counselor-logs-", ".tmp");
        try {
            long total = 0;
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                for (Path source : sources) {
                    // Capture the length once. Appends during collection belong to the next export.
                    try (SeekableByteChannel channel = Files.newByteChannel(source,
                            Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                        long length = channel.size();
                        total += length;
                        if (length > MAX_FILE_BYTES || total > MAX_TOTAL_BYTES) {
                            throw new IOException("Log files exceed the export size limit");
                        }
                        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
                        ByteBuffer buffer = ByteBuffer.allocate(8192);
                        long remaining = length;
                        while (remaining > 0) {
                            buffer.clear();
                            buffer.limit((int) Math.min(buffer.capacity(), remaining));
                            int count = channel.read(buffer);
                            if (count < 0) throw new IOException("Log file changed during collection; try again");
                            if (count == 0) continue;
                            snapshot.write(buffer.array(), 0, count);
                            remaining -= count;
                        }
                        put(zip, source.getFileName().toString(), redact(snapshot.toString(encoding), credentials));
                    }
                }
                put(zip, "README.txt", "Counselor diagnostic logs\nDates: " + today.minusDays(2) + " to " + today
                        + "\nOnly available log files are included. Credentials are redacted from exported copies."
                        + "\nFiles:\n" + String.join("\n", sources.stream().map(p -> p.getFileName().toString()).toList()) + "\n");
            }
            if (replace) {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // No REPLACE_EXISTING: preserve a destination created after the save dialog.
                Files.move(temporary, target);
            }
            return new Archive(target, today.minusDays(2), today,
                    sources.stream().map(p -> p.getFileName().toString()).toList());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void put(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    static String redact(String text, List<String> credentials) {
        for (String credential : credentials) {
            if (credential != null && !credential.isEmpty()) {
                text = text.replace(credential, "[REDACTED]")
                        .replace(URLEncoder.encode(credential, StandardCharsets.UTF_8), "[REDACTED]");
            }
        }
        text = ENCODED_ASSIGNMENT.matcher(text).replaceAll("$1[REDACTED]");
        text = ASSIGNMENT.matcher(text).replaceAll("$1[REDACTED]");
        text = URL_CREDENTIALS.matcher(text).replaceAll("$1[REDACTED]@");
        return LONG_HEX.matcher(text).replaceAll("[REDACTED]");
    }

    public static URI emailUri(String subject, String body) {
        return URI.create("mailto:" + GM_EMAIL + "?subject=" + encode(subject) + "&body=" + encode(body));
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
