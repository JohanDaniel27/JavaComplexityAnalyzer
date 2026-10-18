package com.complexityanalyzer.export;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.git.HotspotResult;
import com.complexityanalyzer.qualitygate.QualityGateResult;
import com.complexityanalyzer.qualitygate.QualityGateViolation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Exports a concise Markdown report suitable for CI/CD PR comments. */
public class MarkdownReportExporter {

    public void export(ProjectAnalysisResult result, QualityGateResult gateResult,
                       Path outputPath) throws IOException {
        try (PrintWriter w = new PrintWriter(outputPath.toFile(), StandardCharsets.UTF_8)) {
            w.println("# Java Complexity Analyzer Report");
            w.println();
            w.println("Generated: " + Instant.now());
            w.println();

            // Summary
            w.println("## Summary");
            int totalSmells = result.getClasses().stream()
                    .mapToInt(c -> c.getSmells().size() +
                                  c.getMethods().stream().mapToInt(m -> m.getSmells().size()).sum())
                    .sum();
            int totalMethods = result.getClasses().stream().mapToInt(c -> c.getMethods().size()).sum();
            w.printf("- **Classes**: %d%n", result.getClasses().size());
            w.printf("- **Methods**: %d%n", totalMethods);
            w.printf("- **Total Code Smells**: %d%n", totalSmells);
            w.printf("- **Circular Dependency Cycles**: %d%n", result.getCycles().size());
            w.printf("- **Code Clones Detected**: %d%n", result.getClones().size());
            w.println();

            // Quality Gate
            if (gateResult != null) {
                String badge = gateResult.isPassed() ? "✅ PASSED" : "❌ FAILED";
                w.println("## Quality Gate: " + badge);
                if (!gateResult.isPassed()) {
                    w.println();
                    w.println("| Severity | Rule | Entity | Message |");
                    w.println("|---|---|---|---|");
                    for (QualityGateViolation v : gateResult.getViolations()) {
                        w.printf("| %s | %s | `%s` | %s |%n",
                                v.getSeverity(), v.getRule(), v.getEntity(), v.getMessage());
                    }
                }
                w.println();
            }

            // Top 10 riskiest classes
            w.println("## Top Risky Classes");
            w.println();
            w.println("| Class | Risk | LOC | Complexity | Smells |");
            w.println("|---|---|---|---|---|");
            result.getClasses().stream()
                    .sorted(Comparator.comparingInt(ClassAnalysisResult::getRiskScore).reversed())
                    .limit(10)
                    .forEach(c -> {
                        int smells = c.getSmells().size() +
                                c.getMethods().stream().mapToInt(m -> m.getSmells().size()).sum();
                        w.printf("| `%s` | **%s** (%d) | %d | %d | %d |%n",
                                c.getClassName(), c.getRiskLevel(), c.getRiskScore(),
                                c.getMetrics().getLinesOfCode(),
                                c.getMetrics().getTotalComplexity(), smells);
                    });
            w.println();

            // Hotspots
            if (!result.getHotspots().isEmpty()) {
                w.println("## Maintenance Hotspots");
                w.println();
                w.println("| Class | Tier | Score | Commits | Complexity |");
                w.println("|---|---|---|---|---|");
                result.getHotspots().stream().limit(10).forEach(h -> {
                    w.printf("| `%s` | %s | %.2f | %d | %d |%n",
                            h.getClassName(), h.getTier(), h.getHotspotScore(),
                            h.getCommitCount(), h.getCyclomaticComplexity());
                });
                w.println();
            }

            // Cycles
            if (!result.getCycles().isEmpty()) {
                w.println("## Circular Dependencies");
                w.println();
                result.getCycles().forEach(cycle -> w.println("- " + cycle));
                w.println();
            }
        }
    }
}
