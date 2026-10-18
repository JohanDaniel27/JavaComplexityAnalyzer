package com.complexityanalyzer.qualitygate;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.architecture.DependencyCycle;
import com.complexityanalyzer.metrics.MethodMetrics;

import java.util.ArrayList;
import java.util.List;

/** Evaluates a ProjectAnalysisResult against a QualityGateConfig and returns a QualityGateResult. */
public class QualityGateEvaluator {

    public QualityGateResult evaluate(ProjectAnalysisResult result, QualityGateConfig config) {
        List<QualityGateViolation> violations = new ArrayList<>();

        for (ClassAnalysisResult clazz : result.getClasses()) {
            String cn = clazz.getClassName();

            // Class-level checks
            if (clazz.getMetrics().getLinesOfCode() > config.maxClassLines) {
                violations.add(new QualityGateViolation(
                        "Max Class Lines", cn,
                        "Class has " + clazz.getMetrics().getLinesOfCode()
                                + " lines (max " + config.maxClassLines + ")", "HIGH"));
            }
            if (clazz.getMetrics().getNcloc() > config.maxClassNcloc) {
                violations.add(new QualityGateViolation(
                        "Max Class NCLOC", cn,
                        "Class has " + clazz.getMetrics().getNcloc()
                                + " NCLOC (max " + config.maxClassNcloc + ")", "MEDIUM"));
            }
            if (clazz.getRiskScore() > config.maxClassRisk) {
                violations.add(new QualityGateViolation(
                        "Max Class Risk", cn,
                        "Risk score " + clazz.getRiskScore()
                                + " exceeds maximum " + config.maxClassRisk, "HIGH"));
            }
            if (clazz.getMetrics().getLcom4() > config.maxLcom4) {
                violations.add(new QualityGateViolation(
                        "Max LCOM4", cn,
                        "LCOM4=" + clazz.getMetrics().getLcom4()
                                + " exceeds maximum " + config.maxLcom4, "MEDIUM"));
            }
            if (clazz.getDependency().getCoupling() > config.maxCoupling) {
                violations.add(new QualityGateViolation(
                        "Max Coupling", cn,
                        "Coupling=" + clazz.getDependency().getCoupling()
                                + " exceeds maximum " + config.maxCoupling, "MEDIUM"));
            }

            // Method-level checks
            for (MethodAnalysisResult method : clazz.getMethods()) {
                MethodMetrics m = method.getMetrics();
                String mn = cn + "::" + m.getMethodName();

                if (m.getComplexity() > config.maxMethodCyclomaticComplexity) {
                    violations.add(new QualityGateViolation(
                            "Max Method Cyclomatic Complexity", mn,
                            "Complexity=" + m.getComplexity()
                                    + " exceeds maximum " + config.maxMethodCyclomaticComplexity, "HIGH"));
                }
                if (m.getCognitiveComplexity() > config.maxMethodCognitiveComplexity) {
                    violations.add(new QualityGateViolation(
                            "Max Method Cognitive Complexity", mn,
                            "Cognitive complexity=" + m.getCognitiveComplexity()
                                    + " exceeds maximum " + config.maxMethodCognitiveComplexity, "MEDIUM"));
                }
                if (m.getLinesOfCode() > config.maxMethodLines) {
                    violations.add(new QualityGateViolation(
                            "Max Method Lines", mn,
                            "Method has " + m.getLinesOfCode()
                                    + " lines (max " + config.maxMethodLines + ")", "HIGH"));
                }
                if (m.getParameterCount() > config.maxMethodParameters) {
                    violations.add(new QualityGateViolation(
                            "Max Method Parameters", mn,
                            "Method has " + m.getParameterCount()
                                    + " parameters (max " + config.maxMethodParameters + ")", "MEDIUM"));
                }
                if (m.getNestingDepth() > config.maxMethodNestingDepth) {
                    violations.add(new QualityGateViolation(
                            "Max Method Nesting", mn,
                            "Nesting depth=" + m.getNestingDepth()
                                    + " exceeds maximum " + config.maxMethodNestingDepth, "MEDIUM"));
                }
            }
        }

        // Project-level checks
        if (config.forbidCircularDependencies && !result.getCycles().isEmpty()) {
            for (DependencyCycle cycle : result.getCycles()) {
                violations.add(new QualityGateViolation(
                        "No Circular Dependencies", "Project",
                        cycle.toString(), "HIGH"));
            }
        }

        return new QualityGateResult(violations);
    }
}
