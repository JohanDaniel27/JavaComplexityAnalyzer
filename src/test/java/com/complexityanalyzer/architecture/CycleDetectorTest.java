package com.complexityanalyzer.architecture;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CycleDetectorTest {

    private CycleDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CycleDetector();
    }

    private ClassAnalysisResult makeClass(String name, Set<String> deps) {
        ClassMetrics m = new ClassMetrics(name, 10, 1, 0, 1, 1.0, 0);
        DependencyResult d = new DependencyResult(name, deps);
        return new ClassAnalysisResult(name, null, m, d, List.of(), List.of(), 5, "LOW");
    }

    @Test
    void noCycleWhenNoDependencies() {
        List<ClassAnalysisResult> classes = List.of(
                makeClass("A", Set.of("B")),
                makeClass("B", Set.of()));
        assertThat(detector.detectClassCycles(classes)).isEmpty();
    }

    @Test
    void twoCycleDetected() {
        List<ClassAnalysisResult> classes = List.of(
                makeClass("A", Set.of("B")),
                makeClass("B", Set.of("A")));
        assertThat(detector.detectClassCycles(classes)).isNotEmpty();
    }

    @Test
    void threeCycleDetected() {
        List<ClassAnalysisResult> classes = List.of(
                makeClass("A", Set.of("B")),
                makeClass("B", Set.of("C")),
                makeClass("C", Set.of("A")));
        List<DependencyCycle> cycles = detector.detectClassCycles(classes);
        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0).getChain().size()).isGreaterThanOrEqualTo(3);
    }
}
