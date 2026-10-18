package com.complexityanalyzer.git;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HotspotRankerTest {

    private final HotspotRanker ranker = new HotspotRanker();

    private ClassAnalysisResult makeClass(String name, int complexity, int cognitive) {
        ClassMetrics m = new ClassMetrics(name, 50, 3, 1, complexity, (double) complexity / 3, 2,
                cognitive, (double) cognitive / 3, complexity, 5, 1, 40, 2, 3, 1, 50);
        DependencyResult d = new DependencyResult(name, Set.of());
        return new ClassAnalysisResult(name, null, m, d, List.of(), List.of(), 20, "LOW");
    }

    @Test
    void ranksComplexClassHigher() {
        ClassAnalysisResult simple = makeClass("Simple", 2, 2);
        ClassAnalysisResult complex = makeClass("Complex", 20, 25);
        List<HotspotResult> results = ranker.rank(List.of(simple, complex), Map.of());
        assertThat(results.get(0).getClassName()).isEqualTo("Complex");
    }

    @Test
    void returnsEmptyForNoClasses() {
        assertThat(ranker.rank(List.of(), Map.of())).isEmpty();
    }

    @Test
    void allClassesHaveTier() {
        List<ClassAnalysisResult> classes = List.of(
                makeClass("A", 5, 3),
                makeClass("B", 15, 20));
        List<HotspotResult> results = ranker.rank(classes, Map.of());
        assertThat(results).allMatch(h -> h.getTier() != null);
    }
}
