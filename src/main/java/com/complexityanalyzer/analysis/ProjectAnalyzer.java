package com.complexityanalyzer.analysis;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectAnalyzer {

    private final ProjectScanner scanner =
            new ProjectScanner();

    private final JavaFileParser parser =
            new JavaFileParser();

    private final MetricsAnalyzer metricsAnalyzer =
            new MetricsAnalyzer();

    private final ClassMetricsAnalyzer classAnalyzer =
            new ClassMetricsAnalyzer();

    private final CodeSmellDetector smellDetector =
            new CodeSmellDetector();

    private final DependencyAnalyzer dependencyAnalyzer =
            new DependencyAnalyzer();

    private final RiskCalculator riskCalculator =
            new RiskCalculator();

    public ProjectAnalysisResult analyze(Path project)
            throws Exception {

        List<Path> files =
                scanner.findJavaFiles(project);

        List<CompilationUnit> units =
                new ArrayList<>();

        for (Path file : files) {
            units.add(parser.parse(file));
        }

        Map<String, DependencyResult> dependencies =
                dependencyAnalyzer.analyze(units);

        List<ClassAnalysisResult> classResults =
                new ArrayList<>();

        for (int i = 0; i < units.size(); i++) {

            CompilationUnit unit = units.get(i);
            Path file = files.get(i);

            for (ClassOrInterfaceDeclaration clazz :
                    unit.findAll(ClassOrInterfaceDeclaration.class)) {

                List<MethodAnalysisResult> methodResults =
                        new ArrayList<>();

                for (MethodDeclaration method :
                        clazz.getMethods()) {

                    MethodMetrics metrics =
                            metricsAnalyzer.analyzeMethod(method);

                    List<CodeSmell> smells =
                            smellDetector.detectMethodSmells(metrics);

                    methodResults.add(
                            new MethodAnalysisResult(
                                    metrics,
                                    smells
                            )
                    );
                }

                ClassMetrics classMetrics =
                        classAnalyzer.analyzeClass(clazz);

                List<CodeSmell> classSmells =
                        smellDetector.detectClassSmells(
                                classMetrics
                        );

                DependencyResult dependency =
                        dependencies.get(
                                clazz.getNameAsString()
                        );

                if (dependency == null) {
                    dependency = new DependencyResult(
                            clazz.getNameAsString(),
                            new java.util.HashSet<>()
                    );
                }

                List<CodeSmell> allSmells = new ArrayList<>(classSmells);

                for (MethodAnalysisResult method : methodResults) {
                    allSmells.addAll(method.getSmells());
                }

                int riskScore = riskCalculator.calculate(
                        classMetrics,
                        dependency,
                        allSmells
                );

                String riskLevel = riskCalculator.getRiskLevel(riskScore);

                classResults.add(
                        new ClassAnalysisResult(
                                clazz.getNameAsString(),
                                file,
                                classMetrics,
                                dependency,
                                methodResults,
                                classSmells,
                                riskScore,
                                riskLevel
                        )
                );
            }
        }

        return new ProjectAnalysisResult(
                project,
                classResults
        );
    }
}