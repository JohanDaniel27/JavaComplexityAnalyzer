package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * Coordinates all method-level metric calculations and returns a fully-populated MethodMetrics.
 */
public class MetricsAnalyzer {

    private final ASTMetricsAnalyzer astAnalyzer = new ASTMetricsAnalyzer();
    private final ComplexityAnalyzer complexityAnalyzer = new ComplexityAnalyzer();
    private final NestingAnalyzer nestingAnalyzer = new NestingAnalyzer();
    private final CognitiveComplexityAnalyzer cognitiveAnalyzer = new CognitiveComplexityAnalyzer();
    private final SourceLineMetricsAnalyzer lineAnalyzer = new SourceLineMetricsAnalyzer();

    public MethodMetrics analyzeMethod(MethodDeclaration method) {
        return analyzeMethod(method, new String[0]);
    }

    public MethodMetrics analyzeMethod(MethodDeclaration method, String[] fileLines) {

        String name = method.getNameAsString();

        int startLine = method.getRange().map(r -> r.begin.line).orElse(0);
        int endLine   = method.getRange().map(r -> r.end.line).orElse(0);
        int lines     = (startLine > 0 && endLine >= startLine) ? endLine - startLine + 1 : 0;

        int parameters = method.getParameters().size();
        int statements = astAnalyzer.countStatements(method);
        int complexity = complexityAnalyzer.calculate(method);
        int nestingDepth = nestingAnalyzer.calculate(method);
        int cognitiveComplexity = cognitiveAnalyzer.calculate(method);

        SourceLineMetricsAnalyzer.LineBreakdown lineBreakdown =
                lineAnalyzer.analyze(method, fileLines);

        return new MethodMetrics(
                name, lines, parameters, statements, complexity, nestingDepth,
                cognitiveComplexity,
                lineBreakdown.ncloc(),
                lineBreakdown.cloc(),
                lineBreakdown.blankLines(),
                startLine, endLine
        );
    }
}