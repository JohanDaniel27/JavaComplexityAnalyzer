package com.complexityanalyzer.dependency;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;

public class DependencyAnalyzer {

    public Map<String, DependencyResult> analyze(
            List<CompilationUnit> units) {

        Set<String> projectClasses = new HashSet<>();

        for (CompilationUnit unit : units) {
            for (ClassOrInterfaceDeclaration clazz :
                    unit.findAll(ClassOrInterfaceDeclaration.class)) {

                projectClasses.add(clazz.getNameAsString());
            }
        }

        Map<String, DependencyResult> results = new HashMap<>();

        for (CompilationUnit unit : units) {

            for (ClassOrInterfaceDeclaration clazz :
                    unit.findAll(ClassOrInterfaceDeclaration.class)) {

                String className = clazz.getNameAsString();

                Set<String> dependencies = new HashSet<>();

                for (ClassOrInterfaceType type :
                        clazz.findAll(ClassOrInterfaceType.class)) {

                    String dependency =
                            type.getNameAsString();

                    if (projectClasses.contains(dependency)
                            && !dependency.equals(className)) {

                        dependencies.add(dependency);
                    }
                }

                results.put(
                        className,
                        new DependencyResult(
                                className,
                                dependencies
                        )
                );
            }
        }

        return results;
    }
}