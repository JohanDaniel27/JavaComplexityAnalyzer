package com.complexityanalyzer.analysis;

import java.nio.file.Path;

/** Records a parse error, syntax warning, or unanalyzable file found during analysis. */
public class AnalysisDiagnostic {

    public enum Severity { WARNING, ERROR }

    private final Path filePath;
    private final Severity severity;
    private final String message;
    private final int line;

    public AnalysisDiagnostic(Path filePath, Severity severity, String message, int line) {
        this.filePath = filePath;
        this.severity = severity;
        this.message = message;
        this.line = line;
    }

    public Path getFilePath() { return filePath; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public int getLine() { return line; }

    @Override
    public String toString() {
        return severity + " [" + filePath.getFileName() + ":" + line + "] " + message;
    }
}
