package com.complexityanalyzer.smells;

/** Actionable refactoring suggestion attached to a code smell finding. */
public class RefactoringAdvice {

    private final String pattern;
    private final String rationale;
    private final String guidance;

    public RefactoringAdvice(String pattern, String rationale, String guidance) {
        this.pattern = pattern;
        this.rationale = rationale;
        this.guidance = guidance;
    }

    public String getPattern() { return pattern; }
    public String getRationale() { return rationale; }
    public String getGuidance() { return guidance; }

    @Override
    public String toString() {
        return pattern + ": " + guidance;
    }
}
