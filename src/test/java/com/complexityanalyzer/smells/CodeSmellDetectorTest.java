package com.complexityanalyzer.smells;

import com.complexityanalyzer.metrics.MethodMetrics;
import com.complexityanalyzer.metrics.ClassMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeSmellDetectorTest {

    private CodeSmellDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CodeSmellDetector();
    }

    @Test
    void longMethodDetected() {
        MethodMetrics m = new MethodMetrics("foo", 60, 1, 10, 5, 2);
        List<CodeSmell> smells = detector.detectMethodSmells(m);
        assertThat(smells).anyMatch(s -> s.getType().equals("Long Method"));
    }

    @Test
    void highComplexityDetected() {
        MethodMetrics m = new MethodMetrics("bar", 20, 1, 15, 12, 2);
        List<CodeSmell> smells = detector.detectMethodSmells(m);
        assertThat(smells).anyMatch(s -> s.getType().equals("High Cyclomatic Complexity")
                || s.getType().equals("High Complexity"));
    }

    @Test
    void tooManyParametersDetected() {
        MethodMetrics m = new MethodMetrics("baz", 10, 7, 5, 2, 1);
        List<CodeSmell> smells = detector.detectMethodSmells(m);
        assertThat(smells).anyMatch(s -> s.getType().equals("Too Many Parameters"));
    }

    @Test
    void largeClassDetected() {
        ClassMetrics cm = new ClassMetrics("BigClass", 400, 10, 5, 20, 2.0, 3);
        List<CodeSmell> smells = detector.detectClassSmells(cm);
        assertThat(smells).anyMatch(s -> s.getType().equals("Large Class"));
    }

    @Test
    void smellHasSeverity() {
        MethodMetrics m = new MethodMetrics("hi", 60, 1, 10, 5, 2);
        List<CodeSmell> smells = detector.detectMethodSmells(m);
        assertThat(smells).allMatch(s -> s.getSeverity() != null);
    }

    @Test
    void cleanMethodHasNoSmells() {
        MethodMetrics m = new MethodMetrics("clean", 10, 2, 5, 3, 1);
        assertThat(detector.detectMethodSmells(m)).isEmpty();
    }
}
