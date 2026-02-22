package nnsql.tpch.framework;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TpchHtmlReport {

    private static final Object LOCK = new Object();
    private static final List<QueryReportEntry> ENTRIES = new ArrayList<>();
    private static final Path DEFAULT_REPORT_PATH = Path.of("build", "reports", "tpch", "query-report.html");
    private static final String REPORT_TEMPLATE_RESOURCE = "tpch/report-template.html";
    private static final Template REPORT_TEMPLATE = loadTemplate();
    private static final double TREE_DEFAULT_ZOOM = 0.78;
    private static final String TREE_COMPACT_STYLE = """
        <style id="nnsql-tree-compact-style">
          body {
            margin: 0;
            padding: 8px;
            overflow: auto;
          }
          .tf-tree .tf-nc { min-width: 120px; }
          .title {
            padding: 6px !important;
            font-size: 13px !important;
          }
          .sub-title {
            font-size: 12px !important;
            padding-top: 4px !important;
          }
          .value {
            margin: 2px 0 4px 0 !important;
            font-size: 11px !important;
          }
        </style>
        """;
    private static final String TREE_COMPACT_RUNTIME_SCRIPT = """
        <script id="nnsql-tree-compact-runtime">
        (() => {
          const defaultZoom = %s;
          const minZoom = 0.35;
          const maxZoom = 2.00;
          let currentZoom = defaultZoom;

          const clampZoom = (value) => {
            if (!Number.isFinite(value)) {
              return defaultZoom;
            }
            return Math.min(maxZoom, Math.max(minZoom, value));
          };

          const applyZoom = (value) => {
            currentZoom = clampZoom(value);

            if (window.CSS && CSS.supports("zoom", "1")) {
              document.documentElement.style.zoom = String(currentZoom);
              document.body.style.transform = "";
              document.body.style.transformOrigin = "";
              document.body.style.width = "";
              return;
            }

            document.body.style.transform = "scale(" + currentZoom + ")";
            document.body.style.transformOrigin = "top left";
            document.body.style.width = (100 / currentZoom) + "%%";
          };

          applyZoom(defaultZoom);

          window.addEventListener("message", (event) => {
            const data = event.data;
            if (!data || data.type !== "nnsql-tree-zoom") {
              return;
            }

            applyZoom(typeof data.zoom === "number" ? data.zoom : Number.parseFloat(data.zoom));

            if (event.source && typeof event.source.postMessage === "function") {
              event.source.postMessage({
                type: "nnsql-tree-zoom-state",
                zoom: currentZoom
              }, "*");
            }
          });

          if (window.parent && window.parent !== window) {
            window.parent.postMessage({
              type: "nnsql-tree-ready",
              zoom: currentZoom
            }, "*");
          }
        })();
        </script>
        """.formatted(String.format(Locale.ROOT, "%.2f", TREE_DEFAULT_ZOOM));

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
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, renderHtml(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to write TPCH HTML report: " + e.getMessage());
        }
    }

    private static Path reportPath() {
        var configured = System.getProperty("nnsql.tpch.reportPath");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_REPORT_PATH.toAbsolutePath().normalize();
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static String renderHtml() {
        var sortedEntries = ENTRIES.stream()
            .sorted(Comparator.comparing(QueryReportEntry::queryName))
            .toList();
        var passed = sortedEntries.stream().filter(QueryReportEntry::success).count();
        var failed = sortedEntries.size() - passed;

        var model = new ReportModel(
            OffsetDateTime.now().toString(),
            reportPath().toString(),
            sortedEntries.size(),
            passed,
            failed,
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
        var sourceExplainTree = toHtmlDataUri(entry.sourceExplainHtml());
        var translatedExplainTree = toHtmlDataUri(entry.translatedExplainHtml());

        sections.add(new SectionModel(
            "Source Query - " + displayDuration(entry.sourceExecutionMs()),
            defaultText(entry.sourceQuery()),
            true,
            "",
            true,
            null
        ));
        sections.add(new SectionModel(
            "Source EXPLAIN Tree",
            sourceExplainTree == null ? "(not available)" : "",
            false,
            "",
            false,
            sourceExplainTree
        ));
        sections.add(new SectionModel(
            "Translated Query - " + displayDuration(entry.translatedExecutionMs()),
            defaultText(entry.translatedQuery()),
            true,
            "",
            true,
            null
        ));
        sections.add(new SectionModel(
            "Translated EXPLAIN Tree",
            translatedExplainTree == null ? "(not available)" : "",
            false,
            "",
            false,
            translatedExplainTree
        ));
        if (!entry.success()) {
            sections.add(new SectionModel(
                "Failure",
                defaultText(entry.failureMessage()),
                true,
                "failure",
                false,
                null
            ));
        }

        return new EntryModel(
            entry.queryName(),
            entry.success() ? "success" : "danger",
            entry.success() ? "PASS" : "FAIL",
            String.valueOf(entry.orderSensitive()),
            displayCount(entry.sourceRowCount()),
            displayCount(entry.translatedRowCount()),
            sections
        );
    }

    private static String displayDuration(Double ms) {
        if (ms == null) {
            return "n/a";
        }
        return "%.3f ms".formatted(ms);
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

    private static String toHtmlDataUri(String htmlDocument) {
        if (htmlDocument == null || htmlDocument.isBlank()) {
            return null;
        }

        var compactHtml = compactTreeHtml(htmlDocument);
        var encoded = Base64.getEncoder().encodeToString(compactHtml.getBytes(StandardCharsets.UTF_8));
        return "data:text/html;charset=utf-8;base64," + encoded;
    }

    private static String compactTreeHtml(String htmlDocument) {
        var lower = htmlDocument.toLowerCase(Locale.ROOT);
        var headCloseIndex = lower.indexOf("</head>");
        if (headCloseIndex >= 0) {
            return htmlDocument.substring(0, headCloseIndex)
                + TREE_COMPACT_STYLE
                + TREE_COMPACT_RUNTIME_SCRIPT
                + htmlDocument.substring(headCloseIndex);
        }

        return TREE_COMPACT_STYLE + htmlDocument + TREE_COMPACT_RUNTIME_SCRIPT;
    }

    public record QueryReportEntry(
        String queryName,
        boolean orderSensitive,
        String sourceQuery,
        Double sourceExecutionMs,
        String sourceExplain,
        String sourceExplainHtml,
        Integer sourceRowCount,
        String translatedQuery,
        Double translatedExecutionMs,
        String translatedExplain,
        String translatedExplainHtml,
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
        List<EntryModel> entries
    ) {}

    public record EntryModel(
        String queryName,
        String statusClass,
        String statusLabel,
        String orderSensitive,
        String sourceRowCount,
        String translatedRowCount,
        List<SectionModel> sections
    ) {}

    public record SectionModel(
        String title,
        String content,
        boolean open,
        String detailsClass,
        boolean sqlContent,
        String iframeSrc
    ) {}
}
