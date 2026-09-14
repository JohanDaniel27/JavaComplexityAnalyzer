package com.complexityanalyzer;

import com.complexityanalyzer.metrics.MethodMetrics;
import com.complexityanalyzer.metrics.MetricsAnalyzer;
import com.complexityanalyzer.parser.JavaFileParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        Path file = Path.of("testcode/TestCode.java");

        JavaFileParser parser = new JavaFileParser();

        CompilationUnit cu = parser.parse(file);

        ClassOrInterfaceDeclaration clazz =
                cu.findFirst(ClassOrInterfaceDeclaration.class).get();

        MetricsAnalyzer analyzer = new MetricsAnalyzer();

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
        }
    }
}