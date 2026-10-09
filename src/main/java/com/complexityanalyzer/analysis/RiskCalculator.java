package com.complexityanalyzer.analysis;

import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.smells.CodeSmell;

import java.util.List;

public class RiskCalculator {

    public int calculate(
            ClassMetrics metrics,
            DependencyResult dependency,
            List<CodeSmell> smells) {

        int score = 0;

        score += Math.min(
                30,
                Math.max(0,
                        (metrics.getTotalComplexity() - 1) * 2)
        );

        score += Math.min(
                20,
                metrics.getLinesOfCode() / 20
        );

        score += Math.min(
                15,
                metrics.getMaxNestingDepth() * 3
        );

        score += Math.min(
                20,
                dependency.getCoupling() * 2
        );

        int smellScore = 0;

        for (CodeSmell smell : smells) {

            switch (smell.getSeverity()) {
                case "HIGH" -> smellScore += 5;
                case "MEDIUM" -> smellScore += 3;
                case "LOW" -> smellScore += 1;
            }
        }

        score += Math.min(15, smellScore);

        return Math.min(100, score);
    }

    public String getRiskLevel(int score) {

        if (score <= 20)
            return "LOW";

        if (score <= 50)
            return "MEDIUM";

        if (score <= 75)
            return "HIGH";

        return "CRITICAL";
    }
}