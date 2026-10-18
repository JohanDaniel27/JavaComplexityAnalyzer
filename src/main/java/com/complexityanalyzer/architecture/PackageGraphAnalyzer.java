package com.complexityanalyzer.architecture;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.dependency.TypeResolver;

import java.util.*;

/** Aggregates class-level dependencies into a package dependency graph and computes package metrics. */
public class PackageGraphAnalyzer {

    private final TypeResolver typeResolver;

    public PackageGraphAnalyzer(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    public List<PackageMetrics> analyze(List<ClassAnalysisResult> classes) {
        // Map package -> classes in that package
        Map<String, List<ClassAnalysisResult>> packageClasses = new LinkedHashMap<>();
        for (ClassAnalysisResult c : classes) {
            String pkg = typeResolver.getPackage(c.getClassName());
            packageClasses.computeIfAbsent(pkg, k -> new ArrayList<>()).add(c);
        }

        // Build package -> Set<dependentPackages>
        Map<String, Set<String>> packageDeps = new HashMap<>();
        for (Map.Entry<String, List<ClassAnalysisResult>> entry : packageClasses.entrySet()) {
            String pkg = entry.getKey();
            Set<String> deps = new HashSet<>();
            for (ClassAnalysisResult c : entry.getValue()) {
                for (String dep : c.getDependency().getDependencies()) {
                    String depPkg = typeResolver.getPackage(dep);
                    if (!depPkg.equals(pkg) && !depPkg.isEmpty()) {
                        deps.add(depPkg);
                    }
                }
            }
            packageDeps.put(pkg, deps);
        }

        // Compute Ca (afferent) for each package
        Map<String, Integer> afferentMap = new HashMap<>();
        for (String pkg : packageClasses.keySet()) afferentMap.put(pkg, 0);
        for (Map.Entry<String, Set<String>> entry : packageDeps.entrySet()) {
            for (String dep : entry.getValue()) {
                afferentMap.merge(dep, 1, Integer::sum);
            }
        }

        List<PackageMetrics> result = new ArrayList<>();
        for (Map.Entry<String, List<ClassAnalysisResult>> entry : packageClasses.entrySet()) {
            String pkg = entry.getKey();
            List<ClassAnalysisResult> pkgClasses = entry.getValue();
            int classCount = pkgClasses.size();
            int abstractCount = (int) pkgClasses.stream()
                    .filter(c -> c.getMetrics().getMethodCount() == 0 ||
                                 c.getClassName().endsWith("Interface") ||
                                 c.getClassName().startsWith("Abstract"))
                    .count();
            int ca = afferentMap.getOrDefault(pkg, 0);
            int ce = packageDeps.getOrDefault(pkg, Set.of()).size();
            result.add(new PackageMetrics(pkg, ca, ce, classCount, abstractCount));
        }
        return result;
    }

    /** Returns a package adjacency map suitable for cycle detection. */
    public Map<String, Set<String>> buildPackageGraph(List<ClassAnalysisResult> classes) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (ClassAnalysisResult c : classes) {
            String pkg = typeResolver.getPackage(c.getClassName());
            if (pkg.isEmpty()) continue;
            graph.computeIfAbsent(pkg, k -> new HashSet<>());
            for (String dep : c.getDependency().getDependencies()) {
                String depPkg = typeResolver.getPackage(dep);
                if (!depPkg.isEmpty() && !depPkg.equals(pkg)) {
                    graph.get(pkg).add(depPkg);
                }
            }
        }
        return graph;
    }
}
