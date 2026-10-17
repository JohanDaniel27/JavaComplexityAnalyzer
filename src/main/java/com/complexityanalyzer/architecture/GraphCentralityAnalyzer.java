package com.complexityanalyzer.architecture;

import com.complexityanalyzer.analysis.ClassAnalysisResult;

import java.util.*;

/** Computes degree centrality and PageRank for the class dependency graph. */
public class GraphCentralityAnalyzer {

    private static final double DAMPING = 0.85;
    private static final int ITERATIONS = 50;

    public List<CentralityResult> analyze(List<ClassAnalysisResult> classes) {
        int n = classes.size();
        if (n == 0) return List.of();

        Map<String, Integer> nameToIdx = new HashMap<>();
        for (int i = 0; i < n; i++) nameToIdx.put(classes.get(i).getClassName(), i);

        int[] inDegree  = new int[n];
        int[] outDegree = new int[n];
        double[] rank   = new double[n];
        Arrays.fill(rank, 1.0 / n);

        List<Set<Integer>> outEdges = new ArrayList<>();
        for (int i = 0; i < n; i++) outEdges.add(new HashSet<>());

        for (int i = 0; i < n; i++) {
            ClassAnalysisResult c = classes.get(i);
            for (String dep : c.getDependency().getDependencies()) {
                Integer j = nameToIdx.get(dep);
                if (j != null && j != i) {
                    outEdges.get(i).add(j);
                    outDegree[i]++;
                    inDegree[j]++;
                }
            }
        }

        // PageRank iterations
        for (int iter = 0; iter < ITERATIONS; iter++) {
            double[] newRank = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j : outEdges.get(i)) {
                    newRank[j] += DAMPING * rank[i] / Math.max(1, outDegree[i]);
                }
            }
            double dangling = 0;
            for (int i = 0; i < n; i++) {
                if (outDegree[i] == 0) dangling += DAMPING * rank[i] / n;
            }
            for (int i = 0; i < n; i++) {
                newRank[i] += dangling + (1.0 - DAMPING) / n;
            }
            rank = newRank;
        }

        List<CentralityResult> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(new CentralityResult(
                    classes.get(i).getClassName(),
                    inDegree[i], outDegree[i], rank[i]));
        }
        results.sort(Comparator.comparingDouble(CentralityResult::getPageRank).reversed());
        return results;
    }
}
