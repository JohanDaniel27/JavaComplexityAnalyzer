package com.complexityanalyzer.regression;

/** Delta between baseline and current analysis for one class. */
public class ClassDiff {

    public enum Status { ADDED, REMOVED, CHANGED, UNCHANGED }

    private final String className;
    private final Status status;
    private final int locDelta;
    private final int nclocDelta;
    private final int complexityDelta;
    private final int cognitiveComplexityDelta;
    private final int riskScoreDelta;
    private final int smellDelta; // positive = new smells, negative = fixed smells

    public ClassDiff(String className, Status status, int locDelta, int nclocDelta,
                     int complexityDelta, int cognitiveComplexityDelta,
                     int riskScoreDelta, int smellDelta) {
        this.className = className;
        this.status = status;
        this.locDelta = locDelta;
        this.nclocDelta = nclocDelta;
        this.complexityDelta = complexityDelta;
        this.cognitiveComplexityDelta = cognitiveComplexityDelta;
        this.riskScoreDelta = riskScoreDelta;
        this.smellDelta = smellDelta;
    }

    public String getClassName() { return className; }
    public Status getStatus() { return status; }
    public int getLocDelta() { return locDelta; }
    public int getNclocDelta() { return nclocDelta; }
    public int getComplexityDelta() { return complexityDelta; }
    public int getCognitiveComplexityDelta() { return cognitiveComplexityDelta; }
    public int getRiskScoreDelta() { return riskScoreDelta; }
    public int getSmellDelta() { return smellDelta; }
}
