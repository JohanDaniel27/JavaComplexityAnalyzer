package com.complexityanalyzer.analysis;

import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.smells.CodeSmell;

import java.nio.file.Path;
import java.util.List;

public class ClassAnalysisResult {

    private final String className;
    private final Path sourceFile;
    private final ClassMetrics metrics;
    private final DependencyResult dependency;
    private final List<MethodAnalysisResult> methods;
    private final List<CodeSmell> smells;
    private final int riskScore;
    private final String riskLevel;

    public ClassAnalysisResult(
            String className,
            Path sourceFile,
            ClassMetrics metrics,
            DependencyResult dependency,
            List<MethodAnalysisResult> methods,
            List<CodeSmell> smells,
            int riskScore,
            String riskLevel) {

        this.className = className;
        this.sourceFile = sourceFile;
        this.metrics = metrics;
        this.dependency = dependency;
        this.methods = List.copyOf(methods);
        this.smells = List.copyOf(smells);
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    public String getClassName() {
        return className;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public ClassMetrics getMetrics() {
        return metrics;
    }

    public DependencyResult getDependency() {
        return dependency;
    }

    public List<MethodAnalysisResult> getMethods() {
        return methods;
    }

    public List<CodeSmell> getSmells() {
        return smells;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }
}