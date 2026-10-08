package com.complexityanalyzer;

import com.complexityanalyzer.dependency.DependencyAnalyzer;
import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.metrics.ClassMetricsAnalyzer;
import com.complexityanalyzer.metrics.MethodMetrics;
import com.complexityanalyzer.metrics.MetricsAnalyzer;
import com.complexityanalyzer.parser.JavaFileParser;
import com.complexityanalyzer.parser.ProjectScanner;
import com.complexityanalyzer.smells.CodeSmell;
import com.complexityanalyzer.smells.CodeSmellDetector;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        Path project = Path.of("testcode");

        ProjectScanner scanner = new ProjectScanner();
        JavaFileParser parser = new JavaFileParser();

        List<Path> files = scanner.findJavaFiles(project);
        List<CompilationUnit> units = new ArrayList<>();

        for (Path file : files) {
            units.add(parser.parse(file));
        }

        MetricsAnalyzer metricsAnalyzer = new MetricsAnalyzer();
        ClassMetricsAnalyzer classAnalyzer = new ClassMetricsAnalyzer();
        CodeSmellDetector smellDetector = new CodeSmellDetector();

        for (CompilationUnit unit : units) {

            for (ClassOrInterfaceDeclaration clazz :
                    unit.findAll(ClassOrInterfaceDeclaration.class)) {

                System.out.println("\n===== CLASS =====");
                System.out.println("Class: " + clazz.getNameAsString());

                for (MethodDeclaration method : clazz.getMethods()) {

                    MethodMetrics result =
                            metricsAnalyzer.analyzeMethod(method);

                    System.out.println("\nMethod: " +
                            result.getMethodName());

                    System.out.println("Lines: " +
                            result.getLinesOfCode());

                    System.out.println("Parameters: " +
                            result.getParameterCount());

                    System.out.println("Statements: " +
                            result.getStatementCount());

                    System.out.println("Complexity: " +
                            result.getComplexity());

                    System.out.println("Nesting Depth: " +
                            result.getNestingDepth());

                    List<CodeSmell> smells =
                            smellDetector.detectMethodSmells(result);

                    for (CodeSmell smell : smells) {
                        System.out.println(
                                "⚠ " + smell.getType()
                                        + " [" + smell.getSeverity() + "]"
                        );

                        System.out.println(
                                "  " + smell.getMessage()
                        );
                    }
                }

                ClassMetrics classResult =
                        classAnalyzer.analyzeClass(clazz);

                System.out.println("\n--- CLASS METRICS ---");

                System.out.println("Lines: " +
                        classResult.getLinesOfCode());

                System.out.println("Methods: " +
                        classResult.getMethodCount());

                System.out.println("Fields: " +
                        classResult.getFieldCount());

                System.out.println("Total Complexity: " +
                        classResult.getTotalComplexity());

                System.out.println("Average Complexity: " +
                        classResult.getAverageComplexity());

                System.out.println("Max Nesting Depth: " +
                        classResult.getMaxNestingDepth());

                List<CodeSmell> classSmells =
                        smellDetector.detectClassSmells(classResult);

                for (CodeSmell smell : classSmells) {
                    System.out.println(
                            "⚠ " + smell.getType()
                                    + " [" + smell.getSeverity() + "]"
                    );

                    System.out.println(
                            "  " + smell.getMessage()
                    );
                }
            }
        }

        DependencyAnalyzer dependencyAnalyzer =
                new DependencyAnalyzer();

        Map<String, DependencyResult> dependencies =
                dependencyAnalyzer.analyze(units);

        System.out.println("\n===== DEPENDENCY ANALYSIS =====");

        for (DependencyResult result : dependencies.values()) {

            System.out.println("\nClass: " +
                    result.getClassName());

            System.out.println("Dependencies: " +
                    result.getDependencies());

            System.out.println("Coupling: " +
                    result.getCoupling());
        }
    }
}