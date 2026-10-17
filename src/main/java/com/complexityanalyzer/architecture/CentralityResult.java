package com.complexityanalyzer.architecture;

/** Holds graph centrality metrics for one class or package node. */
public class CentralityResult {

    private final String name;
    private final int inDegree;   // Number of other classes that depend on this one
    private final int outDegree;  // Number of classes this one depends on
    private final double pageRank;

    public CentralityResult(String name, int inDegree, int outDegree, double pageRank) {
        this.name = name;
        this.inDegree = inDegree;
        this.outDegree = outDegree;
        this.pageRank = pageRank;
    }

    public String getName() { return name; }
    public int getInDegree() { return inDegree; }
    public int getOutDegree() { return outDegree; }
    public double getPageRank() { return pageRank; }
}
