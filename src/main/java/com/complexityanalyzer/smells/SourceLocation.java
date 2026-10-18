package com.complexityanalyzer.smells;

import java.nio.file.Path;

/** Exact source coordinates of an AST node or code smell finding. */
public class SourceLocation {

    private final Path filePath;
    private final int beginLine;
    private final int beginColumn;
    private final int endLine;
    private final int endColumn;

    public SourceLocation(Path filePath, int beginLine, int beginColumn, int endLine, int endColumn) {
        this.filePath = filePath;
        this.beginLine = beginLine;
        this.beginColumn = beginColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public Path getFilePath() { return filePath; }
    public int getBeginLine() { return beginLine; }
    public int getBeginColumn() { return beginColumn; }
    public int getEndLine() { return endLine; }
    public int getEndColumn() { return endColumn; }

    @Override
    public String toString() {
        return filePath.getFileName() + ":" + beginLine + ":" + beginColumn;
    }
}
