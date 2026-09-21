package com.complexityanalyzer;

import com.complexityanalyzer.metrics.MethodMetrics;
import com.complexityanalyzer.metrics.MetricsAnalyzer;
import com.complexityanalyzer.parser.JavaFileParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.metrics.ClassMetricsAnalyzer;
import com.complexityanalyzer.smells.CodeSmell;
import com.complexityanalyzer.smells.CodeSmellDetector;
import java.util.List;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        Path file = Path.of("testcode/TestCode.java");

        JavaFileParser parser = new JavaFileParser();

        CompilationUnit cu = parser.parse(file);

        ClassOrInterfaceDeclaration clazz =
                cu.findFirst(ClassOrInterfaceDeclaration.class).get();

        MetricsAnalyzer analyzer = new MetricsAnalyzer();

        CodeSmellDetector smellDetector = new CodeSmellDetector();

        for (MethodDeclaration method : clazz.getMethods()) {

            MethodMetrics result = analyzer.analyzeMethod(method);

            System.out.println("Method: " +
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

            System.out.println("--------------------");

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

        ClassMetricsAnalyzer classAnalyzer =
                new ClassMetricsAnalyzer();

        ClassMetrics classResult =
                classAnalyzer.analyzeClass(clazz);

        System.out.println("===== CLASS METRICS =====");
        System.out.println("Class: " +
                classResult.getClassName());

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
    }
}