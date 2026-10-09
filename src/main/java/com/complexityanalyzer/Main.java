package com.complexityanalyzer;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalyzer;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        ProjectAnalyzer analyzer =
                new ProjectAnalyzer();

        ProjectAnalysisResult project =
                analyzer.analyze(
                        Path.of("testcode")
                );

        for (ClassAnalysisResult result :
                project.getClasses()) {

            System.out.println("\n===== CLASS =====");

            System.out.println(
                    "Class: " + result.getClassName()
            );

            System.out.println(
                    "File: " + result.getSourceFile()
            );

            System.out.println(
                    "Lines: " +
                            result.getMetrics().getLinesOfCode()
            );

            System.out.println(
                    "Methods: " +
                            result.getMetrics().getMethodCount()
            );

            System.out.println(
                    "Fields: " +
                            result.getMetrics().getFieldCount()
            );

            System.out.println(
                    "Total Complexity: " +
                            result.getMetrics().getTotalComplexity()
            );

            System.out.println(
                    "Average Complexity: " +
                            result.getMetrics().getAverageComplexity()
            );

            System.out.println(
                    "Max Nesting Depth: " +
                            result.getMetrics().getMaxNestingDepth()
            );

            System.out.println(
                    "Dependencies: " +
                            result.getDependency().getDependencies()
            );

            System.out.println(
                    "Coupling: " +
                            result.getDependency().getCoupling()
            );

            System.out.println(
                    "Risk Score: " +
                            result.getRiskScore() +
                            "/100"
            );

            System.out.println(
                    "Risk Level: " +
                            result.getRiskLevel()
            );
        }
    }
}