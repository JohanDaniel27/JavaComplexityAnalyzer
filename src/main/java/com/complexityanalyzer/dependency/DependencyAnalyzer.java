package com.complexityanalyzer.dependency;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;

/**
 * Analyses class dependencies using the TypeResolver for improved name disambiguation.
 */
public class DependencyAnalyzer {

    private final TypeResolver typeResolver = new TypeResolver();

    public Map<String, DependencyResult> analyze(List<CompilationUnit> units) {

        // Build full type index first
        typeResolver.buildIndex(units);

        // Count afferent coupling for each class
        Map<String, Integer> afferentCount = new HashMap<>();
        typeResolver.getAllProjectClassNames().forEach(name -> afferentCount.put(name, 0));

        // Build raw dependency sets
        Map<String, Set<String>> rawDeps = new LinkedHashMap<>();
        Map<String, String> classPackages = new LinkedHashMap<>();

        for (CompilationUnit unit : units) {
            String pkg = unit.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            for (ClassOrInterfaceDeclaration clazz : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                String className = clazz.getNameAsString();
                classPackages.put(className, pkg);

                Set<String> raw = new HashSet<>();
                for (ClassOrInterfaceType type : clazz.findAll(ClassOrInterfaceType.class)) {
                    String dep = type.getNameAsString();
                    if (typeResolver.isProjectClass(dep) && !dep.equals(className)) {
                        raw.add(dep);
                    }
                }
                rawDeps.put(className, raw);

                // Count afferent coupling
                for (String dep : raw) {
                    afferentCount.merge(dep, 1, Integer::sum);
                }
            }
        }

        // Assemble final results
        Map<String, DependencyResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : rawDeps.entrySet()) {
            String className = entry.getKey();
            Set<String> deps = entry.getValue();
            String pkg = classPackages.getOrDefault(className, "");
            int ca = afferentCount.getOrDefault(className, 0);
            results.put(className, new DependencyResult(className, deps, pkg, ca));
        }
        return results;
    }

    public TypeResolver getTypeResolver() { return typeResolver; }
}