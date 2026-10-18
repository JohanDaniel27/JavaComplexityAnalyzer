package com.complexityanalyzer.metrics;

/** Holds all measured metrics for a single class or interface. */
public class ClassMetrics {

    private final String className;
    private final int linesOfCode;
    private final int methodCount;
    private final int fieldCount;
    private final int totalComplexity;
    private final double averageComplexity;
    private final int maxNestingDepth;
    private final int totalCognitiveComplexity;
    private final double averageCognitiveComplexity;
    private final int wmc;          // Weighted Methods per Class (sum of cyclomatic complexities)
    private final int rfc;          // Response for a Class (distinct methods callable)
    private final int lcom4;        // Lack of Cohesion in Methods version 4
    private final int ncloc;        // Non-comment source lines
    private final int cloc;         // Comment lines
    private final int blankLines;
    private final int startLine;
    private final int endLine;

    /** Full constructor. */
    public ClassMetrics(String className, int linesOfCode, int methodCount, int fieldCount,
                        int totalComplexity, double averageComplexity, int maxNestingDepth,
                        int totalCognitiveComplexity, double averageCognitiveComplexity,
                        int wmc, int rfc, int lcom4,
                        int ncloc, int cloc, int blankLines,
                        int startLine, int endLine) {
        this.className = className;
        this.linesOfCode = linesOfCode;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.totalComplexity = totalComplexity;
        this.averageComplexity = averageComplexity;
        this.maxNestingDepth = maxNestingDepth;
        this.totalCognitiveComplexity = totalCognitiveComplexity;
        this.averageCognitiveComplexity = averageCognitiveComplexity;
        this.wmc = wmc;
        this.rfc = rfc;
        this.lcom4 = lcom4;
        this.ncloc = ncloc;
        this.cloc = cloc;
        this.blankLines = blankLines;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    /** Backward-compatible 7-arg constructor. */
    public ClassMetrics(String className, int linesOfCode, int methodCount, int fieldCount,
                        int totalComplexity, double averageComplexity, int maxNestingDepth) {
        this(className, linesOfCode, methodCount, fieldCount,
             totalComplexity, averageComplexity, maxNestingDepth,
             0, 0.0, totalComplexity, 0, 1,
             linesOfCode, 0, 0, 0, 0);
    }

    public String getClassName() { return className; }
    public int getLinesOfCode() { return linesOfCode; }
    public int getMethodCount() { return methodCount; }
    public int getFieldCount() { return fieldCount; }
    public int getTotalComplexity() { return totalComplexity; }
    public double getAverageComplexity() { return averageComplexity; }
    public int getMaxNestingDepth() { return maxNestingDepth; }
    public int getTotalCognitiveComplexity() { return totalCognitiveComplexity; }
    public double getAverageCognitiveComplexity() { return averageCognitiveComplexity; }
    public int getWmc() { return wmc; }
    public int getRfc() { return rfc; }
    public int getLcom4() { return lcom4; }
    public int getNcloc() { return ncloc; }
    public int getCloc() { return cloc; }
    public int getBlankLines() { return blankLines; }
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
}