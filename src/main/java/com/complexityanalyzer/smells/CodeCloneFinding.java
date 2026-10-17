package com.complexityanalyzer.smells;

/** Represents a detected duplicate code block (Type-1 or Type-2 clone). */
public class CodeCloneFinding {

    private final SourceLocation location1;
    private final SourceLocation location2;
    private final int duplicatedLines;
    private final int statementCount;
    private final String cloneType; // "TYPE_1" or "TYPE_2"

    public CodeCloneFinding(SourceLocation location1, SourceLocation location2,
                            int duplicatedLines, int statementCount, String cloneType) {
        this.location1 = location1;
        this.location2 = location2;
        this.duplicatedLines = duplicatedLines;
        this.statementCount = statementCount;
        this.cloneType = cloneType;
    }

    public SourceLocation getLocation1() { return location1; }
    public SourceLocation getLocation2() { return location2; }
    public int getDuplicatedLines() { return duplicatedLines; }
    public int getStatementCount() { return statementCount; }
    public String getCloneType() { return cloneType; }

    @Override
    public String toString() {
        return cloneType + " clone: " + location1 + " duplicates " + location2
                + " (" + duplicatedLines + " lines, " + statementCount + " statements)";
    }
}
