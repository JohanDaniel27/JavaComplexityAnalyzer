package com.complexityanalyzer.qualitygate;

/** Describes a single quality gate rule violation. */
public class QualityGateViolation {

    private final String rule;
    private final String entity;     // Class or method name
    private final String message;
    private final String severity;   // "HIGH", "MEDIUM", "LOW"

    public QualityGateViolation(String rule, String entity, String message, String severity) {
        this.rule = rule;
        this.entity = entity;
        this.message = message;
        this.severity = severity;
    }

    public String getRule()     { return rule; }
    public String getEntity()   { return entity; }
    public String getMessage()  { return message; }
    public String getSeverity() { return severity; }

    @Override
    public String toString() { return "[" + severity + "] " + rule + " in " + entity + ": " + message; }
}
