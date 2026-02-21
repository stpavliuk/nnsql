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
        var querySql = TpchDataProvider.normalizeQuerySql(readResource(fileName));
        if (querySql == null || querySql.isBlank()) {
            throw new IllegalStateException("TPCH query SQL is blank after normalization");
        }
        var orderSensitive = TpchDataProvider.hasOuterOrderBy(querySql);
        return Stream.of(Arguments.of(querySql, orderSensitive));
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
