package com.complexityanalyzer.metrics;

/** Holds all measured metrics for a single method. */
public class MethodMetrics {

    private final String methodName;
    private final int linesOfCode;
    private final int parameterCount;
    private final int statementCount;
    private final int complexity;
    private final int nestingDepth;
    private final int cognitiveComplexity;
    private final int ncloc;
    private final int cloc;
    private final int blankLines;
    private final int startLine;
    private final int endLine;

    /** Full constructor. */
    public MethodMetrics(String methodName, int linesOfCode, int parameterCount,
                         int statementCount, int complexity, int nestingDepth,
                         int cognitiveComplexity, int ncloc, int cloc, int blankLines,
                         int startLine, int endLine) {
        this.methodName = methodName;
        this.linesOfCode = linesOfCode;
        this.parameterCount = parameterCount;
        this.statementCount = statementCount;
        this.complexity = complexity;
        this.nestingDepth = nestingDepth;
        this.cognitiveComplexity = cognitiveComplexity;
        this.ncloc = ncloc;
        this.cloc = cloc;
        this.blankLines = blankLines;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    /** Backward-compatible 6-arg constructor for existing callers. */
    public MethodMetrics(String methodName, int linesOfCode, int parameterCount,
                         int statementCount, int complexity, int nestingDepth) {
        this(methodName, linesOfCode, parameterCount, statementCount,
             complexity, nestingDepth, 0, linesOfCode, 0, 0, 0, 0);
    }

    public String getMethodName() { return methodName; }
    public int getLinesOfCode() { return linesOfCode; }
    public int getParameterCount() { return parameterCount; }
    public int getStatementCount() { return statementCount; }
    public int getComplexity() { return complexity; }
    public int getNestingDepth() { return nestingDepth; }
    public int getCognitiveComplexity() { return cognitiveComplexity; }
    public int getNcloc() { return ncloc; }
    public int getCloc() { return cloc; }
    public int getBlankLines() { return blankLines; }
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
}