package nnsql.tpch.framework;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class TpchNullPreprocessor {

    private static final CSVFormat INPUT_CSV = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .get();
    private static final CSVFormat OUTPUT_CSV = CSVFormat.DEFAULT.builder()
        .setRecordSeparator(System.lineSeparator())
        .get();

    private TpchNullPreprocessor() {
    }

    public static Path stageFixtures(TpchFixtureSet fixtureSet, double nullRate, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        for (var table : fixtureSet.tables()) {
            var inputPath = fixtureSet.csvPath(table);
            var outputPath = outputDir.resolve(table + ".csv");
            if (nullRate <= 0.0d) {
                Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                continue;
            }
            stageTable(table, inputPath, outputPath, fixtureSet.primaryKeys(table), nullRate);
        }
        return outputDir;
    }

    static List<List<String>> nullifyTable(
        String tableName,
        List<String> headers,
        List<List<String>> rows,
        Set<String> primaryKeys,
        double nullRate
    ) {
        if (nullRate <= 0.0d || rows.isEmpty()) {
            return copyRows(rows);
        }

        var protectedRowsByColumn = protectedRowsByColumn(tableName, headers, rows, primaryKeys);
        var nullThreshold = nullThreshold(nullRate);

        var mutatedRows = new ArrayList<List<String>>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = new ArrayList<>(rows.get(rowIndex));
            for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                var columnName = headers.get(colIndex);
                if (shouldNullCell(
                    tableName,
                    columnName,
                    colIndex,
                    rowIndex,
                    row.get(colIndex),
                    primaryKeys,
                    protectedRowsByColumn,
                    nullThreshold
                )) {
                    row.set(colIndex, "");
                }
            }
            mutatedRows.add(row);
        }
        return mutatedRows;
    }

    private static List<List<String>> copyRows(List<List<String>> rows) {
        return rows.stream()
            .<List<String>>map(ArrayList::new)
            .toList();
    }

    private static void stageTable(
        String tableName,
        Path inputPath,
        Path outputPath,
        List<String> primaryKeys,
        double nullRate
    ) throws IOException {
        var primaryKeySet = Set.copyOf(primaryKeys);
        try (var reader = Files.newBufferedReader(inputPath);
             var parser = INPUT_CSV.parse(reader)) {
            var headers = List.copyOf(parser.getHeaderNames());
            var protectedRowsByColumn = scanProtectedRows(tableName, headers, parser, primaryKeySet);
            var nullThreshold = nullThreshold(nullRate);

            try (var writer = OUTPUT_CSV.print(Files.newBufferedWriter(outputPath));
                 var secondReader = Files.newBufferedReader(inputPath);
                 var secondParser = INPUT_CSV.parse(secondReader)) {
                writer.printRecord(headers);
                int rowIndex = 0;
                for (var record : secondParser) {
                    var row = new ArrayList<String>(headers.size());
                    for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                        var value = record.get(colIndex);
                        row.add(shouldNullCell(
                            tableName,
                            headers.get(colIndex),
                            colIndex,
                            rowIndex,
                            value,
                            primaryKeySet,
                            protectedRowsByColumn,
                            nullThreshold
                        ) ? "" : value);
                    }
                    writer.printRecord(row);
                    rowIndex++;
                }
            }
        }
    }

    private static boolean isNullOrEmpty(String value) {
        if (value == null) {
            return true;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equals("\\N");
    }

    private static Map<Integer, Integer> protectedRowsByColumn(
        String tableName,
        List<String> headers,
        List<List<String>> rows,
        Set<String> primaryKeys
    ) {
        var protectedRows = new HashMap<Integer, Integer>();
        var protectedScores = new HashMap<Integer, Long>();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = rows.get(rowIndex);
            for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                var columnName = headers.get(colIndex);
                if (primaryKeys.contains(columnName) || isNullOrEmpty(row.get(colIndex))) {
                    continue;
                }

                var score = stableScore(tableName, columnName, rowIndex);
                var bestScore = protectedScores.get(colIndex);
                if (bestScore == null || Long.compareUnsigned(score, bestScore) > 0) {
                    protectedScores.put(colIndex, score);
                    protectedRows.put(colIndex, rowIndex);
                }
            }
        }

        return protectedRows;
    }

    private static Map<Integer, Integer> scanProtectedRows(
        String tableName,
        List<String> headers,
        CSVParser parser,
        Set<String> primaryKeys
    ) throws IOException {
        var protectedRows = new HashMap<Integer, Integer>();
        var protectedScores = new HashMap<Integer, Long>();

        int rowIndex = 0;
        for (var record : parser) {
            for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                var columnName = headers.get(colIndex);
                var value = record.get(colIndex);
                if (primaryKeys.contains(columnName) || isNullOrEmpty(value)) {
                    continue;
                }

                var score = stableScore(tableName, columnName, rowIndex);
                var bestScore = protectedScores.get(colIndex);
                if (bestScore == null || Long.compareUnsigned(score, bestScore) > 0) {
                    protectedScores.put(colIndex, score);
                    protectedRows.put(colIndex, rowIndex);
                }
            }
            rowIndex++;
        }

        return protectedRows;
    }

    private static boolean shouldNullCell(
        String tableName,
        String columnName,
        int colIndex,
        int rowIndex,
        String value,
        Set<String> primaryKeys,
        Map<Integer, Integer> protectedRowsByColumn,
        long nullThreshold
    ) {
        if (primaryKeys.contains(columnName) || isNullOrEmpty(value)) {
            return false;
        }

        var protectedRow = protectedRowsByColumn.get(colIndex);
        if (protectedRow != null && protectedRow == rowIndex) {
            return false;
        }
        return Long.compareUnsigned(stableScore(tableName, columnName, rowIndex), nullThreshold) < 0;
    }

    private static long nullThreshold(double nullRate) {
        if (nullRate <= 0.0d) {
            return 0L;
        }
        if (nullRate >= 1.0d) {
            return (1L << 53) - 1;
        }
        return (long) Math.floor(nullRate * (1L << 53));
    }

    private static long stableScore(String tableName, String columnName, int rowIndex) {
        var payload = "%s|%s|%d".formatted(tableName, columnName, rowIndex);
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            long value = 0L;
            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (digest[i] & 0xFFL);
            }
            return value >>> 11;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

}
