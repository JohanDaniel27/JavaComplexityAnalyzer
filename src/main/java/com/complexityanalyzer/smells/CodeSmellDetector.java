package com.complexityanalyzer.smells;

import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.metrics.MethodMetrics;

import java.util.ArrayList;
import java.util.List;

public class CodeSmellDetector {

    private static final int LONG_METHOD_LINES = 50;
    private static final int HIGH_COMPLEXITY = 10;
    private static final int DEEP_NESTING = 3;
    private static final int TOO_MANY_PARAMETERS = 5;
    private static final int LARGE_CLASS_LINES = 300;

    public List<CodeSmell> detectMethodSmells(MethodMetrics metrics) {

        List<CodeSmell> smells = new ArrayList<>();

        if (metrics.getLinesOfCode() > LONG_METHOD_LINES) {
            smells.add(new CodeSmell(
                    "Long Method",
                    "Method has " + metrics.getLinesOfCode() + " lines",
                    "HIGH"
            ));
        }

        if (metrics.getComplexity() > HIGH_COMPLEXITY) {
            smells.add(new CodeSmell(
                    "High Complexity",
                    "Cyclomatic complexity is " + metrics.getComplexity(),
                    "HIGH"
            ));
        }

        if (metrics.getNestingDepth() > DEEP_NESTING) {
            smells.add(new CodeSmell(
                    "Deep Nesting",
                    "Nesting depth is " + metrics.getNestingDepth(),
                    "MEDIUM"
            ));
        }

        if (metrics.getParameterCount() > TOO_MANY_PARAMETERS) {
            smells.add(new CodeSmell(
                    "Too Many Parameters",
                    "Method has " + metrics.getParameterCount() + " parameters",
                    "MEDIUM"
            ));
        }

        return smells;
    }

    public List<CodeSmell> detectClassSmells(ClassMetrics metrics) {

        List<CodeSmell> smells = new ArrayList<>();

        if (metrics.getLinesOfCode() > LARGE_CLASS_LINES) {
            smells.add(new CodeSmell(
                    "Large Class",
                    "Class has " + metrics.getLinesOfCode() + " lines",
                    "HIGH"
            ));
        }

        return smells;
    }
}