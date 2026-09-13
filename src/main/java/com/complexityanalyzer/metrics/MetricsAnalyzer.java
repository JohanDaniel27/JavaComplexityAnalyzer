package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.body.MethodDeclaration;

public class MetricsAnalyzer {

    public MethodMetrics analyzeMethod(MethodDeclaration method) {

        String name = method.getNameAsString();

        int lines = method.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        int parameters = method.getParameters().size();

        int statements = method.getBody()
                .map(body -> body.getStatements().size())
                .orElse(0);

        return new MethodMetrics(
                name,
                lines,
                parameters,
                statements
        );
    }
}