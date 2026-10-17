package com.complexityanalyzer.analysis;

import com.complexityanalyzer.metrics.MethodMetrics;
import com.complexityanalyzer.smells.CodeSmell;

import java.util.List;

/** Holds metrics and smell findings for one method. */
public class MethodAnalysisResult {

    private final MethodMetrics metrics;
    private final List<CodeSmell> smells;

    public MethodAnalysisResult(MethodMetrics metrics, List<CodeSmell> smells) {
        this.metrics = metrics;
        this.smells  = List.copyOf(smells);
    }

    public MethodMetrics getMetrics() { return metrics; }
    public List<CodeSmell> getSmells() { return smells; }
}