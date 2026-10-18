package com.complexityanalyzer.qualitygate;

/** Configurable quality gate rules. */
public class QualityGateConfig {

    // Method thresholds
    public int maxMethodCyclomaticComplexity = 10;
    public int maxMethodCognitiveComplexity  = 15;
    public int maxMethodLines                = 50;
    public int maxMethodParameters           = 5;
    public int maxMethodNestingDepth         = 3;

    // Class thresholds
    public int maxClassLines   = 300;
    public int maxClassNcloc   = 250;
    public int maxClassRisk    = 70;
    public int maxLcom4        = 2;
    public int maxCoupling     = 10;

    // Project-level rules
    public boolean forbidCircularDependencies = true;
    public boolean failOnNewHighSeveritySmells = false;

    /** Standard preset (default). */
    public static QualityGateConfig standard() {
        return new QualityGateConfig();
    }

    /** Strict preset for mature codebases. */
    public static QualityGateConfig strict() {
        QualityGateConfig c = new QualityGateConfig();
        c.maxMethodCyclomaticComplexity = 6;
        c.maxMethodCognitiveComplexity  = 10;
        c.maxMethodLines                = 30;
        c.maxMethodParameters           = 4;
        c.maxMethodNestingDepth         = 2;
        c.maxClassLines   = 200;
        c.maxClassNcloc   = 150;
        c.maxClassRisk    = 50;
        c.maxLcom4        = 1;
        c.maxCoupling     = 7;
        c.forbidCircularDependencies = true;
        c.failOnNewHighSeveritySmells = true;
        return c;
    }

    /** Permissive preset for legacy codebases. */
    public static QualityGateConfig permissive() {
        QualityGateConfig c = new QualityGateConfig();
        c.maxMethodCyclomaticComplexity = 20;
        c.maxMethodCognitiveComplexity  = 30;
        c.maxMethodLines                = 100;
        c.maxClassLines = 600;
        c.maxClassRisk  = 90;
        c.maxLcom4      = 5;
        c.maxCoupling   = 20;
        c.forbidCircularDependencies = false;
        return c;
    }
}
