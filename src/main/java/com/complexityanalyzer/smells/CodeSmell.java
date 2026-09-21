package com.complexityanalyzer.smells;

public class CodeSmell {

    private final String type;
    private final String message;
    private final String severity;

    public CodeSmell(String type, String message, String severity) {
        this.type = type;
        this.message = message;
        this.severity = severity;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSeverity() {
        return severity;
    }
}