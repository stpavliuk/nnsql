package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TpchHtmlReportTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedReportShowsTimingChartAndHeaderDurations() throws IOException {
        var reportPath = tempDir.resolve("tpch-report.html");
        var originalReportPath = System.getProperty("nnsql.tpch.reportPath");
        var entries = reportEntries();
        entries.clear();
        System.setProperty("nnsql.tpch.reportPath", reportPath.toString());

        try {
            TpchHtmlReport.record(new TpchHtmlReport.QueryReportEntry(
                "chart-test-q01",
                false,
                "select 1",
                12.345d,
                "source explain",
                null,
                1,
                "select 1 translated",
                34.567d,
                "translated explain",
                null,
                1,
                true,
                null
            ));
            TpchHtmlReport.record(new TpchHtmlReport.QueryReportEntry(
                "chart-test-q02",
                false,
                "select 2",
                45.678d,
                "source explain",
                null,
                1,
                "select 2 translated",
                23.456d,
                "translated explain",
                null,
                1,
                true,
                null
            ));

            var html = Files.readString(reportPath);

            assertTrue(html.contains("Execution Time Comparison"));
            assertTrue(html.contains("Grouped original vs translated timings for every query."));
            assertTrue(html.contains("chart-test-q01"));
            assertTrue(html.contains("chart-test-q02"));
            assertTrue(html.contains("https://cdn.plot.ly/plotly-3.1.0.min.js"));
            assertTrue(html.contains("id=\"timing-plot\""));
            assertTrue(html.contains("data-source-ms=\"12.345\""));
            assertTrue(html.contains("data-translated-ms=\"34.567\""));
            assertTrue(html.contains("Original: 12.345 ms"));
            assertTrue(html.contains("Translated: 34.567 ms"));
            assertTrue(html.contains("Translated is 2.80x slower"));
        } finally {
            entries.clear();
            if (originalReportPath == null) {
                System.clearProperty("nnsql.tpch.reportPath");
            } else {
                System.setProperty("nnsql.tpch.reportPath", originalReportPath);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<TpchHtmlReport.QueryReportEntry> reportEntries() {
        try {
            var entriesField = TpchHtmlReport.class.getDeclaredField("ENTRIES");
            entriesField.setAccessible(true);
            return (List<TpchHtmlReport.QueryReportEntry>) entriesField.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access TPCH report entries", e);
        }
    }
}
