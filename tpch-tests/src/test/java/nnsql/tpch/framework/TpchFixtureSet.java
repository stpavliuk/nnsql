package nnsql.tpch.framework;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public record TpchFixtureSet(
    String scaleFactor,
    Path root,
    Path schemaPath,
    List<String> tables,
    Map<String, List<String>> primaryKeyColumns,
    String fingerprint
) {

    private static final String RESOURCE_ROOT = "data";

    public static TpchFixtureSet load(String scaleFactor) throws IOException {
        var manifestPath = resolveResource("manifest.properties", scaleFactor);
        var manifest = loadManifest(manifestPath);
        var tables = parseCommaSeparated(manifest.getProperty("tables"));
        var pkColumns = new LinkedHashMap<String, List<String>>();
        for (var table : tables) {
            pkColumns.put(table, parseCommaSeparated(manifest.getProperty("table." + table + ".pk", "")));
        }
        var root = manifestPath.getParent();
        return new TpchFixtureSet(
            scaleFactor,
            root,
            root.resolve("schema.sql"),
            List.copyOf(tables),
            Map.copyOf(pkColumns),
            fingerprint(root, tables)
        );
    }

    public Path csvPath(String tableName) {
        return root.resolve(tableName + ".csv");
    }

    public List<String> primaryKeys(String tableName) {
        return primaryKeyColumns.getOrDefault(tableName, List.of());
    }

    static String cacheKey(double nullRate, TpchFixtureSet fixtureSet) {
        var descriptor = "fixture-cache-v1|"
            + fixtureSet.scaleFactor()
            + "|"
            + normalizeRate(nullRate)
            + "|"
            + fixtureSet.fingerprint();
        return sha256Hex(descriptor.getBytes(StandardCharsets.UTF_8)).substring(0, 24);
    }

    private static String normalizeRate(double nullRate) {
        return java.math.BigDecimal.valueOf(nullRate).stripTrailingZeros().toPlainString();
    }

    private static Path resolveResource(String fileName, String scaleFactor) throws IOException {
        var resourceName = "%s/%s/%s".formatted(RESOURCE_ROOT, scaleFactor, fileName);
        var resource = TpchFixtureSet.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IOException("Missing TPCH fixture resource: " + resourceName);
        }
        try {
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Failed to resolve TPCH fixture resource " + resourceName, e);
        }
    }

    private static Properties loadManifest(Path manifestPath) throws IOException {
        var props = new Properties();
        try (var reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    private static List<String> parseCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::strip)
            .filter(part -> !part.isEmpty())
            .toList();
    }

    private static String fingerprint(Path root, List<String> tables) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, root.resolve("manifest.properties"));
            updateDigest(digest, root.resolve("schema.sql"));
            for (var table : tables) {
                updateDigest(digest, root.resolve(table + ".csv"));
            }
            return hex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void updateDigest(MessageDigest digest, Path path) throws IOException {
        digest.update(path.getFileName().toString().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        try (var in = new DigestInputStream(Files.newInputStream(path), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        digest.update((byte) 0);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String hex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (var b : bytes) {
            var value = b & 0xFF;
            sb.append(Character.forDigit((value >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(value & 0xF, 16));
        }
        return sb.toString();
    }
}
