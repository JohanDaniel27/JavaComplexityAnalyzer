package com.complexityanalyzer.qualitygate;

import java.util.List;

/** Result of evaluating a project analysis against a quality gate configuration. */
public class QualityGateResult {

    public enum Status { PASSED, FAILED }

    private final Status status;
    private final List<QualityGateViolation> violations;

    public QualityGateResult(List<QualityGateViolation> violations) {
        this.violations = List.copyOf(violations);
        this.status = violations.isEmpty() ? Status.PASSED : Status.FAILED;
    }

    public Status getStatus()                         { return status; }
    public boolean isPassed()                         { return status == Status.PASSED; }
    public List<QualityGateViolation> getViolations() { return violations; }
    public int getViolationCount()                    { return violations.size(); }
}
