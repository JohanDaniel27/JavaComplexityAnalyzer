package com.complexityanalyzer.metrics;

public class MethodMetrics {

    private final String methodName;
    private final int linesOfCode;
    private final int parameterCount;
    private final int statementCount;
    private final int complexity;

    public MethodMetrics(String methodName,
                         int linesOfCode,
                         int parameterCount,
                         int statementCount,
                         int complexity) {

        this.methodName = methodName;
        this.linesOfCode = linesOfCode;
        this.parameterCount = parameterCount;
        this.statementCount = statementCount;
        this.complexity = complexity;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public int getStatementCount() {
        return statementCount;
    }

    public int getComplexity() {
        return complexity;
    }
}