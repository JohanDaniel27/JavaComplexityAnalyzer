package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.HashSet;
import java.util.Set;

/** Analyses a ClassOrInterfaceDeclaration and produces a fully-populated ClassMetrics. */
public class ClassMetricsAnalyzer {

    private final MetricsAnalyzer metricsAnalyzer = new MetricsAnalyzer();
    private final CohesionAnalyzer cohesionAnalyzer = new CohesionAnalyzer();
    private final SourceLineMetricsAnalyzer lineAnalyzer = new SourceLineMetricsAnalyzer();

    public ClassMetrics analyzeClass(ClassOrInterfaceDeclaration clazz) {
        return analyzeClass(clazz, new String[0]);
    }

    public ClassMetrics analyzeClass(ClassOrInterfaceDeclaration clazz, String[] fileLines) {

        String className = clazz.getNameAsString();

        int startLine = clazz.getRange().map(r -> r.begin.line).orElse(0);
        int endLine   = clazz.getRange().map(r -> r.end.line).orElse(0);
        int lines     = (startLine > 0 && endLine >= startLine) ? endLine - startLine + 1 : 0;

        int methodCount = clazz.getMethods().size();
        int fieldCount  = clazz.getFields().size();

        int totalCyclomaticComplexity  = 0;
        int totalCognitiveComplexity   = 0;
        int maxNestingDepth            = 0;

        // RFC: collect all distinct method names reachable from this class
        Set<String> reachableMethods = new HashSet<>();

        for (MethodDeclaration method : clazz.getMethods()) {
            MethodMetrics result = metricsAnalyzer.analyzeMethod(method, fileLines);
            totalCyclomaticComplexity += result.getComplexity();
            totalCognitiveComplexity  += result.getCognitiveComplexity();
            maxNestingDepth = Math.max(maxNestingDepth, result.getNestingDepth());

            reachableMethods.add(method.getNameAsString());
            for (MethodCallExpr mce : method.findAll(MethodCallExpr.class)) {
                reachableMethods.add(mce.getNameAsString());
            }
        }

        double averageCyclomaticComplexity = methodCount > 0
                ? (double) totalCyclomaticComplexity / methodCount : 0.0;
        double averageCognitiveComplexity = methodCount > 0
                ? (double) totalCognitiveComplexity / methodCount : 0.0;

        int wmc  = totalCyclomaticComplexity; // WMC = sum of cyclomatic complexities
        int rfc  = reachableMethods.size();
        int lcom4 = cohesionAnalyzer.calculateLcom4(clazz);

        SourceLineMetricsAnalyzer.LineBreakdown lb = lineAnalyzer.analyze(clazz, fileLines);

        return new ClassMetrics(
                className, lines, methodCount, fieldCount,
                totalCyclomaticComplexity, averageCyclomaticComplexity, maxNestingDepth,
                totalCognitiveComplexity, averageCognitiveComplexity,
                wmc, rfc, lcom4,
                lb.ncloc(), lb.cloc(), lb.blankLines(),
                startLine, endLine
        );
    }
}