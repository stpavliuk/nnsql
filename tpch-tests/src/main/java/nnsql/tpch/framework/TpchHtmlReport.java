package nnsql.tpch.framework;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TpchHtmlReport {

    private static final Object LOCK = new Object();
    private static final List<QueryReportEntry> ENTRIES = new ArrayList<>();
    private static final DateTimeFormatter REPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final OffsetDateTime REPORT_STARTED_AT = OffsetDateTime.now();
    private static final Path DEFAULT_REPORT_DIRECTORY = Path.of("build", "reports", "tpch");
    private static final Path DEFAULT_LATEST_REPORT_PATH = DEFAULT_REPORT_DIRECTORY.resolve("query-report.html");
    private static final String REPORT_TEMPLATE_RESOURCE = "tpch/report-template.html";
    private static final Template REPORT_TEMPLATE = loadTemplate();

    private TpchHtmlReport() {}

    public static void record(QueryReportEntry entry) {
        synchronized (LOCK) {
            replaceOrAppend(entry);
            writeHtmlReport();
        }
    }

    private static void replaceOrAppend(QueryReportEntry entry) {
        for (int i = 0; i < ENTRIES.size(); i++) {
            if (ENTRIES.get(i).queryName().equals(entry.queryName())) {
                ENTRIES.set(i, entry);
                return;
            }
        }
        ENTRIES.add(entry);
    }

    private static void writeHtmlReport() {
        var reportPath = reportPath();
        try {
            var html = renderHtml();
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, html, StandardCharsets.UTF_8);
            writeLatestHtmlReport(reportPath, html);
        } catch (IOException e) {
            System.err.println("Failed to write TPCH HTML report: " + e.getMessage());
        }
    }

    private static void writeLatestHtmlReport(Path reportPath, String html) throws IOException {
        if (isReportPathConfigured()) {
            return;
        }

        var latestReportPath = DEFAULT_LATEST_REPORT_PATH.toAbsolutePath().normalize();
        if (latestReportPath.equals(reportPath)) {
            return;
        }

        Files.writeString(latestReportPath, html, StandardCharsets.UTF_8);
    }

    private static Path reportPath() {
        if (!isReportPathConfigured()) {
            var timestamp = REPORT_TIMESTAMP_FORMAT.format(REPORT_STARTED_AT);
            return DEFAULT_REPORT_DIRECTORY.resolve("query-report-" + timestamp + ".html").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("nnsql.tpch.reportPath")).toAbsolutePath().normalize();
    }

    private static boolean isReportPathConfigured() {
        var configured = System.getProperty("nnsql.tpch.reportPath");
        return configured != null && !configured.isBlank();
    }

    private static String renderHtml() {
        var sortedEntries = ENTRIES.stream()
            .sorted(Comparator.comparing(QueryReportEntry::queryName))
            .toList();
        var passed = sortedEntries.stream().filter(QueryReportEntry::success).count();
        var failed = sortedEntries.size() - passed;
        var timingChart = toTimingChartModel(sortedEntries);
        var reportPath = reportPath();

        var model = new ReportModel(
            REPORT_STARTED_AT.toString(),
            reportPath.toString(),
            sortedEntries.size(),
            passed,
            failed,
            timingChart,
            sortedEntries.stream()
                .map(TpchHtmlReport::toEntryModel)
                .toList()
        );

        try {
            return REPORT_TEMPLATE.apply(model);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render TPCH HTML report", e);
        }
    }

    private static Template loadTemplate() {
        try (var in = TpchHtmlReport.class.getClassLoader().getResourceAsStream(REPORT_TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing TPCH report template: " + REPORT_TEMPLATE_RESOURCE);
            }
            var source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new Handlebars().compileInline(source);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load TPCH report template: " + REPORT_TEMPLATE_RESOURCE, e);
        }
    }

    private static EntryModel toEntryModel(QueryReportEntry entry) {
        var sections = new ArrayList<SectionModel>();
        var sourceExecution = displayDuration(entry.sourceExecutionMs());
        var translatedExecution = displayDuration(entry.translatedExecutionMs());
        var timingComparison = describeTimingComparison(entry.sourceExecutionMs(), entry.translatedExecutionMs());

        sections.add(new SectionModel(
            "Source Query - " + sourceExecution,
            defaultText(entry.sourceQuery()),
            true,
            "",
            true
        ));
        sections.add(new SectionModel(
            "Source EXPLAIN",
            defaultText(entry.sourceExplain()),
            false,
            "",
            false
        ));
        sections.add(new SectionModel(
            "Translated Query - " + translatedExecution,
            defaultText(entry.translatedQuery()),
            true,
            "",
            true
        ));
        sections.add(new SectionModel(
            "Translated EXPLAIN",
            defaultText(entry.translatedExplain()),
            false,
            "",
            false
        ));
        if (!entry.success()) {
            sections.add(new SectionModel(
                "Failure",
                defaultText(entry.failureMessage()),
                true,
                "failure",
                false
            ));
        }

        return new EntryModel(
            entry.queryName(),
            entry.success() ? "success" : "danger",
            entry.success() ? "PASS" : "FAIL",
            sourceExecution,
            translatedExecution,
            timingComparison,
            String.valueOf(entry.orderSensitive()),
            displayCount(entry.sourceRowCount()),
            displayCount(entry.translatedRowCount()),
            sections
        );
    }

    private static TimingChartModel toTimingChartModel(List<QueryReportEntry> entries) {
        var maxDurationMs = entries.stream()
            .flatMap(entry -> java.util.stream.Stream.of(entry.sourceExecutionMs(), entry.translatedExecutionMs()))
            .filter(duration -> duration != null && duration > 0.0d)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0d);

        return new TimingChartModel(
            maxDurationMs > 0.0d,
            displayDuration(maxDurationMs > 0.0d ? maxDurationMs : null),
            entries.stream()
                .map(entry -> new TimingRowModel(
                    entry.queryName(),
                    formatNumericDuration(entry.sourceExecutionMs()),
                    displayDuration(entry.sourceExecutionMs()),
                    entry.sourceExecutionMs() != null,
                    formatNumericDuration(entry.translatedExecutionMs()),
                    displayDuration(entry.translatedExecutionMs()),
                    entry.translatedExecutionMs() != null
                ))
                .toList()
        );
    }

    private static String formatNumericDuration(Double durationMs) {
        if (durationMs == null) {
            return "";
        }
        return "%.3f".formatted(durationMs);
    }

    private static String displayDuration(Double ms) {
        if (ms == null) {
            return "n/a";
        }
        return "%.3f ms".formatted(ms);
    }

    private static String describeTimingComparison(Double sourceMs, Double translatedMs) {
        if (sourceMs == null || translatedMs == null || sourceMs <= 0.0d || translatedMs <= 0.0d) {
            return "";
        }

        var slowerMs = Math.max(sourceMs, translatedMs);
        var fasterMs = Math.min(sourceMs, translatedMs);
        var ratio = slowerMs / fasterMs;

        if (!Double.isFinite(ratio)) {
            return "";
        }
        if (Math.abs(ratio - 1.0d) < 0.05d) {
            return "About the same speed";
        }

        var slowerLabel = translatedMs > sourceMs ? "Translated" : "Original";
        return "%s is %.2fx slower".formatted(slowerLabel, ratio);
    }

    private static String displayCount(Integer count) {
        return count == null ? "n/a" : count.toString();
    }

    private static String defaultText(String value) {
        if (value == null || value.isBlank()) {
            return "(not available)";
        }
        return value;
    }

    public record QueryReportEntry(
        String queryName,
        boolean orderSensitive,
        String sourceQuery,
        Double sourceExecutionMs,
        String sourceExplain,
        Integer sourceRowCount,
        String translatedQuery,
        Double translatedExecutionMs,
        String translatedExplain,
        Integer translatedRowCount,
        boolean success,
        String failureMessage
    ) {}

    public record ReportModel(
        String generatedAt,
        String reportPath,
        int total,
        long passed,
        long failed,
        TimingChartModel timingChart,
        List<EntryModel> entries
    ) {}

    public record EntryModel(
        String queryName,
        String statusClass,
        String statusLabel,
        String sourceExecution,
        String translatedExecution,
        String timingComparison,
        String orderSensitive,
        String sourceRowCount,
        String translatedRowCount,
        List<SectionModel> sections
    ) {}

    public record TimingChartModel(
        boolean hasData,
        String maxExecution,
        List<TimingRowModel> rows
    ) {}

    public record TimingRowModel(
        String queryName,
        String sourceExecutionMs,
        String sourceExecution,
        boolean sourceAvailable,
        String translatedExecutionMs,
        String translatedExecution,
        boolean translatedAvailable
    ) {}

    public record SectionModel(
        String title,
        String content,
        boolean open,
        String detailsClass,
        boolean sqlContent
    ) {}
}
