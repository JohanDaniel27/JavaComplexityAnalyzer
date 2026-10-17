package com.complexityanalyzer.architecture;

import com.complexityanalyzer.analysis.ClassAnalysisResult;

import java.util.*;

/**
 * Detects circular dependencies in a class dependency graph using Tarjan's SCC algorithm.
 */
public class CycleDetector {

    /**
     * Detects all cycles in the class dependency graph.
     *
     * @param classes analysis results containing class names and their dependency sets
     * @return list of detected dependency cycles
     */
    public List<DependencyCycle> detectClassCycles(List<ClassAnalysisResult> classes) {
        // Build adjacency map
        Map<String, Set<String>> graph = new HashMap<>();
        for (ClassAnalysisResult clazz : classes) {
            graph.put(clazz.getClassName(),
                      new HashSet<>(clazz.getDependency().getDependencies()));
        }
        return detectCycles(graph, DependencyCycle.Level.CLASS);
    }

    /**
     * Detects cycles in a package dependency graph.
     *
     * @param packageGraph map from package name -> set of dependent package names
     */
    public List<DependencyCycle> detectPackageCycles(Map<String, Set<String>> packageGraph) {
        return detectCycles(packageGraph, DependencyCycle.Level.PACKAGE);
    }

    private List<DependencyCycle> detectCycles(Map<String, Set<String>> graph,
                                               DependencyCycle.Level level) {
        // Tarjan's SCC
        Map<String, Integer> index   = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Map<String, Boolean> onStack = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        int[] idx = {0};
        List<DependencyCycle> cycles = new ArrayList<>();

        for (String node : graph.keySet()) {
            if (!index.containsKey(node)) {
                strongConnect(node, graph, index, lowlink, onStack, stack, idx, cycles, level);
            }
        }
        return cycles;
    }

    private void strongConnect(
            String v,
            Map<String, Set<String>> graph,
            Map<String, Integer> index,
            Map<String, Integer> lowlink,
            Map<String, Boolean> onStack,
            Deque<String> stack,
            int[] idx,
            List<DependencyCycle> cycles,
            DependencyCycle.Level level) {

        index.put(v, idx[0]);
        lowlink.put(v, idx[0]);
        idx[0]++;
        stack.push(v);
        onStack.put(v, true);

        Set<String> neighbors = graph.getOrDefault(v, Set.of());
        for (String w : neighbors) {
            if (!index.containsKey(w)) {
                strongConnect(w, graph, index, lowlink, onStack, stack, idx, cycles, level);
                lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
            } else if (Boolean.TRUE.equals(onStack.get(w))) {
                lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
            }
        }

        if (lowlink.get(v).equals(index.get(v))) {
            List<String> scc = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.put(w, false);
                scc.add(w);
            } while (!w.equals(v));

            // Only cycles with more than 1 node are actual circular dependencies
            if (scc.size() > 1) {
                Collections.reverse(scc);
                List<String> chain = new ArrayList<>(scc);
                chain.add(scc.get(0)); // close the cycle
                cycles.add(new DependencyCycle(level, chain));
            }
        }
    }
}
