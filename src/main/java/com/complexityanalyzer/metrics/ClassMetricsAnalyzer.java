package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

public class ClassMetricsAnalyzer {

    private final MetricsAnalyzer metricsAnalyzer = new MetricsAnalyzer();

    public ClassMetrics analyzeClass(ClassOrInterfaceDeclaration clazz) {

        String className = clazz.getNameAsString();

        int lines = clazz.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        int methods = clazz.getMethods().size();

        int fields = clazz.getFields().size();

        int totalComplexity = 0;
        double averageComplexity = 0;
        int maxNestingDepth = 0;

        for (MethodDeclaration method : clazz.getMethods()) {

            MethodMetrics result =
                    metricsAnalyzer.analyzeMethod(method);

            totalComplexity += result.getComplexity();

            maxNestingDepth = Math.max(
                    maxNestingDepth,
                    result.getNestingDepth()
            );
        }

        if (methods > 0) {
            averageComplexity =
                    (double) totalComplexity / methods;
        }

        return new ClassMetrics(
                className,
                lines,
                methods,
                fields,
                totalComplexity,
                averageComplexity,
                maxNestingDepth
        );
    }
}