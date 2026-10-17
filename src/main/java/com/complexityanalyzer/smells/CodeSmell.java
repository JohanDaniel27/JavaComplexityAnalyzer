package com.complexityanalyzer.smells;

/** Represents a single detected code smell with exact source location and refactoring advice. */
public class CodeSmell {

    private final String type;
    private final String message;
    private final String severity;
    private final SourceLocation location;
    private final RefactoringAdvice advice;

    /** Full constructor with location and advice. */
    public CodeSmell(String type, String message, String severity,
                     SourceLocation location, RefactoringAdvice advice) {
        this.type = type;
        this.message = message;
        this.severity = severity;
        this.location = location;
        this.advice = advice;
    }

    /** Backward-compatible constructor without location/advice. */
    public CodeSmell(String type, String message, String severity) {
        this(type, message, severity, null, null);
    }

    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getSeverity() { return severity; }
    public SourceLocation getLocation() { return location; }
    public RefactoringAdvice getAdvice() { return advice; }
}