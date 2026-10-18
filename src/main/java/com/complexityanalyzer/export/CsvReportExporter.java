package com.complexityanalyzer.export;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Exports class and method metrics to CSV files. */
public class CsvReportExporter {

    public void export(ProjectAnalysisResult result, Path outputDir) throws IOException {
        exportClasses(result, outputDir.resolve("classes.csv"));
        exportMethods(result, outputDir.resolve("methods.csv"));
    }

    private void exportClasses(ProjectAnalysisResult result, Path path) throws IOException {
        try (PrintWriter w = new PrintWriter(path.toFile(), StandardCharsets.UTF_8)) {
            w.println("Class,LOC,NCLOC,Methods,Fields,Cyclomatic,Cognitive,LCOM4,Coupling,Risk Score,Risk Level,Smells");
            for (ClassAnalysisResult c : result.getClasses()) {
                int smells = c.getSmells().size();
                for (MethodAnalysisResult m : c.getMethods()) smells += m.getSmells().size();
                w.printf("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%d%n",
                        csvEsc(c.getClassName()),
                        c.getMetrics().getLinesOfCode(),
                        c.getMetrics().getNcloc(),
                        c.getMetrics().getMethodCount(),
                        c.getMetrics().getFieldCount(),
                        c.getMetrics().getTotalComplexity(),
                        c.getMetrics().getTotalCognitiveComplexity(),
                        c.getMetrics().getLcom4(),
                        c.getDependency().getCoupling(),
                        c.getRiskScore(),
                        csvEsc(c.getRiskLevel()),
                        smells);
            }
        }
    }

    private void exportMethods(ProjectAnalysisResult result, Path path) throws IOException {
        try (PrintWriter w = new PrintWriter(path.toFile(), StandardCharsets.UTF_8)) {
            w.println("Class,Method,LOC,NCLOC,Parameters,Statements,Cyclomatic,Cognitive,Nesting,Smells");
            for (ClassAnalysisResult c : result.getClasses()) {
                for (MethodAnalysisResult m : c.getMethods()) {
                    w.printf("%s,%s,%d,%d,%d,%d,%d,%d,%d,%d%n",
                            csvEsc(c.getClassName()),
                            csvEsc(m.getMetrics().getMethodName()),
                            m.getMetrics().getLinesOfCode(),
                            m.getMetrics().getNcloc(),
                            m.getMetrics().getParameterCount(),
                            m.getMetrics().getStatementCount(),
                            m.getMetrics().getComplexity(),
                            m.getMetrics().getCognitiveComplexity(),
                            m.getMetrics().getNestingDepth(),
                            m.getSmells().size());
                }
            }
        }
    }

    private String csvEsc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
