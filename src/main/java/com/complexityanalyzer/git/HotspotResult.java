package com.complexityanalyzer.git;

/** Hotspot ranking result for one class. */
public class HotspotResult {

    public enum Tier { CRITICAL_HOTSPOT, HIGH_HOTSPOT, MODERATE, LOW }

    private final String className;
    private final String filePath;
    private final int commitCount;
    private final int totalChurn;
    private final int authorCount;
    private final int cyclomaticComplexity;
    private final int cognitiveComplexity;
    private final double hotspotScore;
    private final Tier tier;

    public HotspotResult(String className, String filePath, int commitCount, int totalChurn,
                         int authorCount, int cyclomaticComplexity, int cognitiveComplexity,
                         double hotspotScore, Tier tier) {
        this.className = className;
        this.filePath = filePath;
        this.commitCount = commitCount;
        this.totalChurn = totalChurn;
        this.authorCount = authorCount;
        this.cyclomaticComplexity = cyclomaticComplexity;
        this.cognitiveComplexity = cognitiveComplexity;
        this.hotspotScore = hotspotScore;
        this.tier = tier;
    }

    public String getClassName() { return className; }
    public String getFilePath() { return filePath; }
    public int getCommitCount() { return commitCount; }
    public int getTotalChurn() { return totalChurn; }
    public int getAuthorCount() { return authorCount; }
    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public int getCognitiveComplexity() { return cognitiveComplexity; }
    public double getHotspotScore() { return hotspotScore; }
    public Tier getTier() { return tier; }
}
