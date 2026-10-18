package com.complexityanalyzer.regression;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;

import java.util.*;

/** Computes the diff between a baseline ProjectSnapshot and the current ProjectAnalysisResult. */
public class SnapshotComparator {

    public RegressionDiffResult compare(ProjectSnapshot baseline, ProjectAnalysisResult current) {

        Map<String, SnapshotEntry> baselineMap = new HashMap<>();
        for (SnapshotEntry e : baseline.entries) {
            baselineMap.put(e.className, e);
        }

        Map<String, ClassAnalysisResult> currentMap = new LinkedHashMap<>();
        for (ClassAnalysisResult c : current.getClasses()) {
            currentMap.put(c.getClassName(), c);
        }

        List<ClassDiff> diffs = new ArrayList<>();

        // Classes in baseline
        for (String name : baselineMap.keySet()) {
            SnapshotEntry b = baselineMap.get(name);
            if (!currentMap.containsKey(name)) {
                // Removed
                diffs.add(new ClassDiff(name, ClassDiff.Status.REMOVED,
                        -b.linesOfCode, -b.ncloc, -b.totalComplexity,
                        -b.totalCognitiveComplexity, -b.riskScore, -b.smellCount));
            } else {
                ClassAnalysisResult c = currentMap.get(name);
                int smellCount = c.getSmells().size();
                for (MethodAnalysisResult m : c.getMethods()) smellCount += m.getSmells().size();

                int locDelta  = c.getMetrics().getLinesOfCode() - b.linesOfCode;
                int nclocDelta = c.getMetrics().getNcloc() - b.ncloc;
                int ccDelta   = c.getMetrics().getTotalComplexity() - b.totalComplexity;
                int cogDelta  = c.getMetrics().getTotalCognitiveComplexity() - b.totalCognitiveComplexity;
                int riskDelta = c.getRiskScore() - b.riskScore;
                int smellDelta = smellCount - b.smellCount;

                ClassDiff.Status status = (locDelta == 0 && ccDelta == 0 && smellDelta == 0)
                        ? ClassDiff.Status.UNCHANGED : ClassDiff.Status.CHANGED;

                diffs.add(new ClassDiff(name, status, locDelta, nclocDelta,
                        ccDelta, cogDelta, riskDelta, smellDelta));
            }
        }

        // New classes not in baseline
        for (String name : currentMap.keySet()) {
            if (!baselineMap.containsKey(name)) {
                ClassAnalysisResult c = currentMap.get(name);
                int smellCount = c.getSmells().size();
                for (MethodAnalysisResult m : c.getMethods()) smellCount += m.getSmells().size();
                diffs.add(new ClassDiff(name, ClassDiff.Status.ADDED,
                        c.getMetrics().getLinesOfCode(), c.getMetrics().getNcloc(),
                        c.getMetrics().getTotalComplexity(),
                        c.getMetrics().getTotalCognitiveComplexity(),
                        c.getRiskScore(), smellCount));
            }
        }

        return new RegressionDiffResult(diffs);
    }
}
