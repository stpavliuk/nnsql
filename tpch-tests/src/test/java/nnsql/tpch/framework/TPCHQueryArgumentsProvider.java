package nnsql.tpch.framework;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class TPCHQueryArgumentsProvider
    implements ArgumentsProvider, AnnotationConsumer<TPCHQueryTest> {
    private static final String RESOURCE_ROOT = "tpch/sqlite_tpc/";

    private String fileName;

    @Override
    public void accept(TPCHQueryTest annotation) {
        fileName = annotation.value();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("TPCH query file name is not configured");
        }
        return Stream.of(Arguments.of(normalizeSql(readResource(fileName))));
    }

    private static String normalizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalStateException("TPCH query SQL is blank");
        }

        var stripped = sql.lines()
            .map(TPCHQueryArgumentsProvider::stripLineComment)
            .toList();
        var cleaned = String.join("\n", stripped).strip();

        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).strip();
        }

        if (cleaned.isBlank()) {
            throw new IllegalStateException("TPCH query SQL is blank after normalization");
        }

        return cleaned;
    }

    private static String stripLineComment(String line) {
        var idx = line.indexOf("--");
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    private static String readResource(String fileName) {
        var resource = RESOURCE_ROOT + fileName;
        try (var in = TPCHQueryArgumentsProvider.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing TPCH SQL resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource " + resource, e);
        }
    }
}
