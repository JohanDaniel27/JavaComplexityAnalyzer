package com.complexityanalyzer.metrics;

public class ClassMetrics {

    private final String className;
    private final int linesOfCode;
    private final int methodCount;
    private final int fieldCount;
    private final int totalComplexity;
    private final double averageComplexity;
    private final int maxNestingDepth;

    public ClassMetrics(String className,
                        int linesOfCode,
                        int methodCount,
                        int fieldCount,
                        int totalComplexity,
                        double averageComplexity,
                        int maxNestingDepth) {

        this.className = className;
        this.linesOfCode = linesOfCode;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.totalComplexity = totalComplexity;
        this.averageComplexity = averageComplexity;
        this.maxNestingDepth = maxNestingDepth;
    }

    public String getClassName() {
        return className;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public int getTotalComplexity() {
        return totalComplexity;
    }

    public double getAverageComplexity() {
        return averageComplexity;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }
}