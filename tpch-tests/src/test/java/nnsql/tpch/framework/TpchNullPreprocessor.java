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

    static TableData loadTable(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path);
             var parser = INPUT_CSV.parse(reader)) {
            var headers = List.copyOf(parser.getHeaderNames());
            var rows = parser.stream()
                .map(record -> {
                    var row = new ArrayList<String>(headers.size());
                    for (int i = 0; i < headers.size(); i++) {
                        row.add(record.get(i));
                    }
                    return (List<String>) row;
                })
                .toList();
            return new TableData(headers, rows);
        }
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

        var protectedCells = new HashSet<CellRef>();
        var candidates = new ArrayList<NullCandidate>();

        for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
            var columnName = headers.get(colIndex);
            if (primaryKeys.contains(columnName)) {
                continue;
            }

            NullCandidate protectedCandidate = null;
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                var value = rows.get(rowIndex).get(colIndex);
                if (isNullOrEmpty(value)) {
                    continue;
                }
                var candidate = new NullCandidate(
                    new CellRef(rowIndex, colIndex),
                    stableScore(tableName, columnName, rowIndex)
                );
                candidates.add(candidate);
                if (protectedCandidate == null || candidate.score().compareTo(protectedCandidate.score()) > 0) {
                    protectedCandidate = candidate;
                }
            }
            if (protectedCandidate != null) {
                protectedCells.add(protectedCandidate.cellRef());
            }
        }

        if (candidates.isEmpty()) {
            return copyRows(rows);
        }

        var nullifiable = candidates.stream()
            .filter(candidate -> !protectedCells.contains(candidate.cellRef()))
            .sorted(Comparator.comparing(NullCandidate::score))
            .toList();
        var targetCount = (int) Math.floor(candidates.size() * nullRate);
        var actualCount = Math.min(targetCount, nullifiable.size());
        var cellsToNull = new HashSet<CellRef>();
        for (int i = 0; i < actualCount; i++) {
            cellsToNull.add(nullifiable.get(i).cellRef());
        }

        var mutatedRows = new ArrayList<List<String>>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = new ArrayList<>(rows.get(rowIndex));
            for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                if (cellsToNull.contains(new CellRef(rowIndex, colIndex))) {
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
        var table = loadTable(inputPath);
        var mutated = nullifyTable(tableName, table.headers(), table.rows(), Set.copyOf(primaryKeys), nullRate);
        try (var writer = OUTPUT_CSV.print(Files.newBufferedWriter(outputPath))) {
            writer.printRecord(table.headers());
            for (var row : mutated) {
                writer.printRecord(row);
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

    private static String stableScore(String tableName, String columnName, int rowIndex) {
        var payload = "%s|%s|%d".formatted(tableName, columnName, rowIndex);
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
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

    record TableData(List<String> headers, List<List<String>> rows) {
    }

    private record CellRef(int rowIndex, int columnIndex) {
    }

    private record NullCandidate(CellRef cellRef, String score) {
    }
}
