package com.complexityanalyzer.export;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.architecture.DependencyCycle;
import com.complexityanalyzer.git.HotspotResult;
import com.complexityanalyzer.qualitygate.QualityGateResult;
import com.complexityanalyzer.qualitygate.QualityGateViolation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

/** Generates a standalone HTML report with inline CSS styling. */
public class HtmlReportExporter {

    public void export(ProjectAnalysisResult result, QualityGateResult gateResult,
                       Path outputPath) throws IOException {
        try (PrintWriter w = new PrintWriter(outputPath.toFile(), StandardCharsets.UTF_8)) {
            w.println("<!DOCTYPE html>");
            w.println("<html lang=\"en\">");
            w.println("<head><meta charset=\"UTF-8\">");
            w.println("<title>Java Complexity Analyzer Report</title>");
            w.println("<style>");
            w.println("body{font-family:'Segoe UI',Arial,sans-serif;margin:20px;background:#f4f6fa;color:#333}");
            w.println("h1{color:#2c3e50} h2{color:#34495e;border-bottom:2px solid #ddd;padding-bottom:5px}");
            w.println(".card{background:#fff;padding:15px 20px;border-radius:8px;box-shadow:0 1px 4px rgba(0,0,0,.1);margin-bottom:20px}");
            w.println(".kpi{display:inline-block;background:#3498db;color:#fff;padding:8px 16px;border-radius:6px;margin:4px;font-weight:bold}");
            w.println(".risk-LOW{color:#27ae60} .risk-MEDIUM{color:#f39c12} .risk-HIGH{color:#e67e22} .risk-CRITICAL{color:#e74c3c}");
            w.println(".badge-PASSED{background:#27ae60;color:#fff;padding:3px 10px;border-radius:4px}");
            w.println(".badge-FAILED{background:#e74c3c;color:#fff;padding:3px 10px;border-radius:4px}");
            w.println("table{width:100%;border-collapse:collapse;font-size:13px}");
            w.println("th{background:#2c3e50;color:#fff;padding:8px;text-align:left}");
            w.println("td{padding:6px 8px;border-bottom:1px solid #eee}");
            w.println("tr:hover td{background:#eaf3fb}");
            w.println(".tier-CRITICAL_HOTSPOT{color:#e74c3c;font-weight:bold}");
            w.println(".tier-HIGH_HOTSPOT{color:#e67e22;font-weight:bold}");
            w.println(".tier-MODERATE{color:#f39c12}");
            w.println("</style></head>");
            w.println("<body>");
            w.println("<h1>Java Complexity Analyzer Report</h1>");
            w.println("<p><em>Generated: " + Instant.now() + "</em></p>");
            w.println("<p><em>Project: " + result.getProjectPath() + "</em></p>");

            // KPI summary cards
            w.println("<div class=\"card\">");
            int totalSmells = result.getClasses().stream()
                    .mapToInt(c -> c.getSmells().size() +
                                  c.getMethods().stream().mapToInt(m -> m.getSmells().size()).sum())
                    .sum();
            int totalMethods = result.getClasses().stream().mapToInt(c -> c.getMethods().size()).sum();
            w.printf("<span class=\"kpi\">Classes: %d</span>", result.getClasses().size());
            w.printf("<span class=\"kpi\">Methods: %d</span>", totalMethods);
            w.printf("<span class=\"kpi\">Smells: %d</span>", totalSmells);
            w.printf("<span class=\"kpi\">Cycles: %d</span>", result.getCycles().size());
            w.printf("<span class=\"kpi\">Clones: %d</span>", result.getClones().size());

            if (gateResult != null) {
                String badge = gateResult.isPassed() ? "PASSED" : "FAILED";
                w.printf("&nbsp;&nbsp;<span class=\"badge-%s\">Quality Gate: %s</span>", badge, badge);
            }
            w.println("</div>");

            // Quality Gate Violations
            if (gateResult != null && !gateResult.isPassed()) {
                w.println("<div class=\"card\">");
                w.println("<h2>Quality Gate Violations</h2>");
                w.println("<table><tr><th>Severity</th><th>Rule</th><th>Entity</th><th>Message</th></tr>");
                for (QualityGateViolation v : gateResult.getViolations()) {
                    w.printf("<tr><td class=\"risk-%s\">%s</td><td>%s</td><td><code>%s</code></td><td>%s</td></tr>%n",
                            v.getSeverity(), v.getSeverity(), esc(v.getRule()), esc(v.getEntity()), esc(v.getMessage()));
                }
                w.println("</table></div>");
            }

            // Classes table
            w.println("<div class=\"card\">");
            w.println("<h2>Class Analysis</h2>");
            w.println("<table><tr><th>Class</th><th>LOC</th><th>NCLOC</th><th>Methods</th>"
                    + "<th>Cyclomatic</th><th>Cognitive</th><th>LCOM4</th>"
                    + "<th>Coupling</th><th>Risk Score</th><th>Risk Level</th><th>Smells</th></tr>");
            result.getClasses().stream()
                    .sorted(Comparator.comparingInt(ClassAnalysisResult::getRiskScore).reversed())
                    .forEach(c -> {
                        int smells = c.getSmells().size() +
                                c.getMethods().stream().mapToInt(m -> m.getSmells().size()).sum();
                        w.printf("<tr><td><code>%s</code></td><td>%d</td><td>%d</td><td>%d</td>"
                                + "<td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td>"
                                + "<td class=\"risk-%s\">%s</td><td>%d</td></tr>%n",
                                esc(c.getClassName()),
                                c.getMetrics().getLinesOfCode(), c.getMetrics().getNcloc(),
                                c.getMetrics().getMethodCount(),
                                c.getMetrics().getTotalComplexity(),
                                c.getMetrics().getTotalCognitiveComplexity(),
                                c.getMetrics().getLcom4(),
                                c.getDependency().getCoupling(),
                                c.getRiskScore(), c.getRiskLevel(), c.getRiskLevel(), smells);
                    });
            w.println("</table></div>");

            // Hotspots
            if (!result.getHotspots().isEmpty()) {
                w.println("<div class=\"card\">");
                w.println("<h2>Maintenance Hotspots</h2>");
                w.println("<table><tr><th>Class</th><th>Tier</th><th>Score</th><th>Commits</th><th>Churn</th><th>Complexity</th></tr>");
                result.getHotspots().stream().limit(20).forEach(h -> {
                    w.printf("<tr><td><code>%s</code></td><td class=\"tier-%s\">%s</td>"
                            + "<td>%.3f</td><td>%d</td><td>%d</td><td>%d</td></tr>%n",
                            esc(h.getClassName()), h.getTier(), h.getTier(),
                            h.getHotspotScore(), h.getCommitCount(),
                            h.getTotalChurn(), h.getCyclomaticComplexity());
                });
                w.println("</table></div>");
            }

            // Cycles
            if (!result.getCycles().isEmpty()) {
                w.println("<div class=\"card\">");
                w.println("<h2>Circular Dependencies</h2><ul>");
                for (DependencyCycle cycle : result.getCycles()) {
                    w.println("<li><code>" + esc(cycle.toString()) + "</code></li>");
                }
                w.println("</ul></div>");
            }

            w.println("</body></html>");
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
