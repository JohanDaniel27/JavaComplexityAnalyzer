package com.complexityanalyzer.analysis;

import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.smells.CodeSmell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RiskCalculatorTest {

    private RiskCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new RiskCalculator();
    }

    @Test
    void zeroInputsGiveLowScore() {
        ClassMetrics m = new ClassMetrics("T", 1, 0, 0, 0, 0.0, 0);
        DependencyResult d = new DependencyResult("T", Set.of());
        assertThat(calc.calculate(m, d, List.of())).isLessThanOrEqualTo(20);
    }

    @Test
    void highComplexityRaisesScore() {
        ClassMetrics m = new ClassMetrics("T", 100, 5, 2, 20, 4.0, 4);
        DependencyResult d = new DependencyResult("T", Set.of("A", "B", "C"));
        int score = calc.calculate(m, d, List.of());
        assertThat(score).isGreaterThan(20);
    }

    @Test
    void scoreIsCapAt100() {
        ClassMetrics m = new ClassMetrics("T", 9999, 100, 50, 9999, 100.0, 10);
        DependencyResult d = new DependencyResult("T", Set.of("A","B","C","D","E","F","G","H","I","J"));
        List<CodeSmell> smells = List.of(
                new CodeSmell("A", "a", "HIGH"), new CodeSmell("B", "b", "HIGH"),
                new CodeSmell("C", "c", "HIGH"), new CodeSmell("D", "d", "HIGH"));
        assertThat(calc.calculate(m, d, smells)).isLessThanOrEqualTo(100);
    }

    @Test
    void riskLevelBoundaries() {
        assertThat(calc.getRiskLevel(0)).isEqualTo("LOW");
        assertThat(calc.getRiskLevel(20)).isEqualTo("LOW");
        assertThat(calc.getRiskLevel(21)).isEqualTo("MEDIUM");
        assertThat(calc.getRiskLevel(50)).isEqualTo("MEDIUM");
        assertThat(calc.getRiskLevel(51)).isEqualTo("HIGH");
        assertThat(calc.getRiskLevel(75)).isEqualTo("HIGH");
        assertThat(calc.getRiskLevel(76)).isEqualTo("CRITICAL");
        assertThat(calc.getRiskLevel(100)).isEqualTo("CRITICAL");
    }
}
