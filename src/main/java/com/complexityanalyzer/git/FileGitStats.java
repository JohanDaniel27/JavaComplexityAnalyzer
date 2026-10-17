package com.complexityanalyzer.git;

import java.time.Instant;

/** Holds Git commit history statistics for one source file. */
public class FileGitStats {

    private final String filePath;      // Relative path in the repo
    private final int commitCount;      // Number of commits touching this file
    private final int linesAdded;
    private final int linesDeleted;
    private final int authorCount;      // Distinct authors
    private final Instant lastCommit;

    public FileGitStats(String filePath, int commitCount, int linesAdded,
                        int linesDeleted, int authorCount, Instant lastCommit) {
        this.filePath    = filePath;
        this.commitCount = commitCount;
        this.linesAdded  = linesAdded;
        this.linesDeleted = linesDeleted;
        this.authorCount = authorCount;
        this.lastCommit  = lastCommit;
    }

    public String getFilePath()  { return filePath; }
    public int getCommitCount()  { return commitCount; }
    public int getLinesAdded()   { return linesAdded; }
    public int getLinesDeleted() { return linesDeleted; }
    public int getAuthorCount()  { return authorCount; }
    public Instant getLastCommit() { return lastCommit; }
    public int getTotalChurn()   { return linesAdded + linesDeleted; }
}
