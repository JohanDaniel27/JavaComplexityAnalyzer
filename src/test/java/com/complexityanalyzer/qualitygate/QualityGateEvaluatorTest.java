package com.complexityanalyzer.qualitygate;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QualityGateEvaluatorTest {

    private QualityGateEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new QualityGateEvaluator();
    }

    private ClassAnalysisResult makeClass(String name, int loc, int risk) {
        ClassMetrics m = new ClassMetrics(name, loc, 5, 2, 8, 1.6, 2);
        DependencyResult d = new DependencyResult(name, Set.of());
        return new ClassAnalysisResult(name, null, m, d, List.of(), List.of(), risk,
                risk > 75 ? "CRITICAL" : risk > 50 ? "HIGH" : risk > 20 ? "MEDIUM" : "LOW");
    }

    @Test
    void passesWhenAllClassesCompliant() {
        ProjectAnalysisResult result = new ProjectAnalysisResult(
                java.nio.file.Path.of("."),
                List.of(makeClass("A", 50, 15), makeClass("B", 60, 10)));
        QualityGateResult gate = evaluator.evaluate(result, QualityGateConfig.standard());
        assertThat(gate.isPassed()).isTrue();
    }

    @Test
    void failsWhenClassTooLarge() {
        ProjectAnalysisResult result = new ProjectAnalysisResult(
                java.nio.file.Path.of("."),
                List.of(makeClass("BigClass", 500, 30)));
        QualityGateResult gate = evaluator.evaluate(result, QualityGateConfig.standard());
        assertThat(gate.isPassed()).isFalse();
        assertThat(gate.getViolations()).anyMatch(v -> v.getRule().contains("Class Lines"));
    }

    @Test
    void strictConfigMoreViolations() {
        ProjectAnalysisResult result = new ProjectAnalysisResult(
                java.nio.file.Path.of("."),
                List.of(makeClass("C", 250, 55)));
        QualityGateResult standard = evaluator.evaluate(result, QualityGateConfig.standard());
        QualityGateResult strict   = evaluator.evaluate(result, QualityGateConfig.strict());
        assertThat(strict.getViolationCount()).isGreaterThanOrEqualTo(
                standard.getViolationCount());
    }
}
