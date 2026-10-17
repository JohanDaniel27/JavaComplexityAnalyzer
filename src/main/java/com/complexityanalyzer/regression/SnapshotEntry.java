package com.complexityanalyzer.regression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Lightweight snapshot record for one class, stored in a baseline JSON file. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnapshotEntry {

    public String className;
    public String riskLevel;
    public int riskScore;
    public int linesOfCode;
    public int ncloc;
    public int totalComplexity;
    public int totalCognitiveComplexity;
    public int methodCount;
    public int fieldCount;
    public int lcom4;
    public int smellCount;
    public int coupling;

    /** Default constructor for Jackson. */
    public SnapshotEntry() {}

    public SnapshotEntry(String className, String riskLevel, int riskScore,
                         int linesOfCode, int ncloc, int totalComplexity,
                         int totalCognitiveComplexity, int methodCount, int fieldCount,
                         int lcom4, int smellCount, int coupling) {
        this.className = className;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.linesOfCode = linesOfCode;
        this.ncloc = ncloc;
        this.totalComplexity = totalComplexity;
        this.totalCognitiveComplexity = totalCognitiveComplexity;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.lcom4 = lcom4;
        this.smellCount = smellCount;
        this.coupling = coupling;
    }
}
