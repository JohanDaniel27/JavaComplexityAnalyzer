package com.complexityanalyzer.smells;

import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.metrics.MethodMetrics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects code smells at method and class level.
 * Attaches source locations and actionable refactoring advice to each finding.
 */
public class CodeSmellDetector {

    // Thresholds
    private static final int LONG_METHOD_LINES       = 50;
    private static final int HIGH_CYCLOMATIC          = 10;
    private static final int HIGH_COGNITIVE           = 15;
    private static final int DEEP_NESTING             = 3;
    private static final int TOO_MANY_PARAMETERS      = 5;
    private static final int LARGE_CLASS_LINES        = 300;
    private static final int GOD_CLASS_METHODS        = 20;
    private static final int GOD_CLASS_LOC            = 500;
    private static final int GOD_CLASS_LCOM4          = 3;
    private static final int DATA_CLASS_MAX_COMPLEXITY = 2;
    private static final int BRAIN_METHOD_LINES        = 65;
    private static final int BRAIN_METHOD_COMPLEXITY   = 8;

    // -----------------------------------------------------------------------
    // Method-level smells
    // -----------------------------------------------------------------------

    public List<CodeSmell> detectMethodSmells(MethodMetrics metrics) {
        return detectMethodSmells(metrics, null);
    }

    public List<CodeSmell> detectMethodSmells(MethodMetrics metrics, Path filePath) {
        List<CodeSmell> smells = new ArrayList<>();

        SourceLocation loc = filePath == null ? null :
                new SourceLocation(filePath, metrics.getStartLine(), 1,
                                   metrics.getEndLine(), 1);

        if (metrics.getLinesOfCode() > LONG_METHOD_LINES) {
            smells.add(new CodeSmell(
                    "Long Method",
                    "Method '" + metrics.getMethodName() + "' has " + metrics.getLinesOfCode() + " lines",
                    "HIGH", loc,
                    new RefactoringAdvice("Extract Method",
                            "Long methods are hard to understand and test.",
                            "Identify cohesive blocks and extract them into well-named helper methods.")
            ));
        }

        if (metrics.getComplexity() > HIGH_CYCLOMATIC) {
            smells.add(new CodeSmell(
                    "High Cyclomatic Complexity",
                    "Method '" + metrics.getMethodName() + "' has cyclomatic complexity " + metrics.getComplexity(),
                    "HIGH", loc,
                    new RefactoringAdvice("Extract Method / Replace Conditional with Polymorphism",
                            "High cyclomatic complexity indicates too many decision paths.",
                            "Extract branches into separate methods; consider polymorphism or strategy pattern.")
            ));
        }

        if (metrics.getCognitiveComplexity() > HIGH_COGNITIVE) {
            smells.add(new CodeSmell(
                    "High Cognitive Complexity",
                    "Method '" + metrics.getMethodName() + "' has cognitive complexity " + metrics.getCognitiveComplexity(),
                    "HIGH", loc,
                    new RefactoringAdvice("Simplify Control Flow",
                            "High cognitive complexity makes code hard to read and reason about.",
                            "Flatten nested conditions, extract guard clauses, and decompose into smaller methods.")
            ));
        }

        if (metrics.getNestingDepth() > DEEP_NESTING) {
            smells.add(new CodeSmell(
                    "Deep Nesting",
                    "Method '" + metrics.getMethodName() + "' nesting depth is " + metrics.getNestingDepth(),
                    "MEDIUM", loc,
                    new RefactoringAdvice("Extract Method / Guard Clause",
                            "Deep nesting reduces readability.",
                            "Use early returns (guard clauses) and extract inner blocks into methods.")
            ));
        }

        if (metrics.getParameterCount() > TOO_MANY_PARAMETERS) {
            smells.add(new CodeSmell(
                    "Too Many Parameters",
                    "Method '" + metrics.getMethodName() + "' has " + metrics.getParameterCount() + " parameters",
                    "MEDIUM", loc,
                    new RefactoringAdvice("Introduce Parameter Object",
                            "Too many parameters increase coupling and reduce readability.",
                            "Group related parameters into a dedicated data class or builder.")
            ));
        }

        return smells;
    }

    // -----------------------------------------------------------------------
    // Class-level smells
    // -----------------------------------------------------------------------

    public List<CodeSmell> detectClassSmells(ClassMetrics metrics) {
        return detectClassSmells(metrics, null);
    }

    public List<CodeSmell> detectClassSmells(ClassMetrics metrics, Path filePath) {
        List<CodeSmell> smells = new ArrayList<>();

        SourceLocation loc = filePath == null ? null :
                new SourceLocation(filePath, metrics.getStartLine(), 1, metrics.getEndLine(), 1);

        // Large Class
        if (metrics.getLinesOfCode() > LARGE_CLASS_LINES) {
            smells.add(new CodeSmell(
                    "Large Class",
                    "Class '" + metrics.getClassName() + "' has " + metrics.getLinesOfCode() + " lines",
                    "HIGH", loc,
                    new RefactoringAdvice("Extract Class",
                            "Classes exceeding ~300 lines often have too many responsibilities.",
                            "Identify cohesive groups of methods and fields; extract them into separate classes.")
            ));
        }

        // God Class
        if (metrics.getMethodCount() >= GOD_CLASS_METHODS
                && metrics.getLinesOfCode() >= GOD_CLASS_LOC
                && metrics.getLcom4() >= GOD_CLASS_LCOM4) {
            smells.add(new CodeSmell(
                    "God Class",
                    "Class '" + metrics.getClassName() + "' has " + metrics.getMethodCount()
                            + " methods, " + metrics.getLinesOfCode() + " lines and LCOM4=" + metrics.getLcom4(),
                    "HIGH", loc,
                    new RefactoringAdvice("Extract Class / Apply Single Responsibility Principle",
                            "God classes accumulate too many responsibilities and resist change.",
                            "Apply SRP: identify distinct responsibilities and extract each into a focused class.")
            ));
        }

        // Data Class (many fields, tiny complexity)
        if (metrics.getFieldCount() >= 3
                && metrics.getMethodCount() > 0
                && metrics.getAverageComplexity() <= DATA_CLASS_MAX_COMPLEXITY
                && metrics.getWmc() <= DATA_CLASS_MAX_COMPLEXITY * metrics.getMethodCount()) {
            smells.add(new CodeSmell(
                    "Data Class",
                    "Class '" + metrics.getClassName()
                            + "' appears to only hold data (avg complexity "
                            + String.format("%.1f", metrics.getAverageComplexity()) + ")",
                    "LOW", loc,
                    new RefactoringAdvice("Move Behavior",
                            "Data classes expose all their data and have little behavior.",
                            "Move operations that manipulate this data from other classes into this class.")
            ));
        }

        // Low Cohesion
        if (metrics.getLcom4() > 2 && metrics.getMethodCount() > 3) {
            smells.add(new CodeSmell(
                    "Low Cohesion",
                    "Class '" + metrics.getClassName() + "' has LCOM4=" + metrics.getLcom4()
                            + " suggesting " + metrics.getLcom4() + " separable responsibilities",
                    "MEDIUM", loc,
                    new RefactoringAdvice("Extract Class",
                            "Low cohesion (LCOM4 > 1) indicates the class has multiple unrelated responsibilities.",
                            "Each connected component in the LCOM4 graph is a candidate for extraction into its own class.")
            ));
        }

        return smells;
    }

    /** Detects a Brain Method smell (oversized + overly complex single method in a class). */
    public CodeSmell detectBrainMethod(MethodMetrics m, Path filePath) {
        if (m.getLinesOfCode() > BRAIN_METHOD_LINES && m.getComplexity() > BRAIN_METHOD_COMPLEXITY) {
            SourceLocation loc = filePath == null ? null :
                    new SourceLocation(filePath, m.getStartLine(), 1, m.getEndLine(), 1);
            return new CodeSmell(
                    "Brain Method",
                    "Method '" + m.getMethodName() + "' has " + m.getLinesOfCode()
                            + " lines and complexity " + m.getComplexity(),
                    "HIGH", loc,
                    new RefactoringAdvice("Decompose Method",
                            "Brain methods centralise too much logic and are the hardest to test.",
                            "Break into a small coordination method calling well-named helper methods.")
            );
        }
        return null;
    }
}