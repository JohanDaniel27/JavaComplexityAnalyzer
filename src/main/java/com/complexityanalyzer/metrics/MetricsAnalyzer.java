package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.body.MethodDeclaration;

public class MetricsAnalyzer {

    private final ASTMetricsAnalyzer astAnalyzer = new ASTMetricsAnalyzer();
    private final ComplexityAnalyzer complexityAnalyzer = new ComplexityAnalyzer();

    public MethodMetrics analyzeMethod(MethodDeclaration method) {

        String name = method.getNameAsString();

        int lines = method.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        int parameters = method.getParameters().size();

        int statements = astAnalyzer.countStatements(method);

        int complexity = complexityAnalyzer.calculate(method);

        return new MethodMetrics(
                name,
                lines,
                parameters,
                statements,
                complexity
        );
    }
}