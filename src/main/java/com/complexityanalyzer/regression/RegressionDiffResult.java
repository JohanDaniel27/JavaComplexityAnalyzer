package com.complexityanalyzer.regression;

import java.util.List;

/** Aggregated diff between a baseline snapshot and the current analysis. */
public class RegressionDiffResult {

    private final List<ClassDiff> diffs;
    private final int newSmells;
    private final int resolvedSmells;
    private final int totalLocDelta;
    private final int totalComplexityDelta;

    public RegressionDiffResult(List<ClassDiff> diffs) {
        this.diffs = List.copyOf(diffs);
        this.newSmells = diffs.stream().mapToInt(d -> Math.max(0, d.getSmellDelta())).sum();
        this.resolvedSmells = diffs.stream().mapToInt(d -> Math.max(0, -d.getSmellDelta())).sum();
        this.totalLocDelta = diffs.stream().mapToInt(ClassDiff::getLocDelta).sum();
        this.totalComplexityDelta = diffs.stream().mapToInt(ClassDiff::getComplexityDelta).sum();
    }

    public List<ClassDiff> getDiffs() { return diffs; }
    public int getNewSmells() { return newSmells; }
    public int getResolvedSmells() { return resolvedSmells; }
    public int getTotalLocDelta() { return totalLocDelta; }
    public int getTotalComplexityDelta() { return totalComplexityDelta; }

    public List<ClassDiff> getAddedClasses() {
        return diffs.stream().filter(d -> d.getStatus() == ClassDiff.Status.ADDED).toList();
    }
    public List<ClassDiff> getRemovedClasses() {
        return diffs.stream().filter(d -> d.getStatus() == ClassDiff.Status.REMOVED).toList();
    }
    public List<ClassDiff> getChangedClasses() {
        return diffs.stream().filter(d -> d.getStatus() == ClassDiff.Status.CHANGED).toList();
    }
}
