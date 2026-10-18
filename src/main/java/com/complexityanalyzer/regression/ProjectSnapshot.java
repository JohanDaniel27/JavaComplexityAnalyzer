package com.complexityanalyzer.regression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/** A serializable snapshot of a project analysis, used for regression comparisons. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectSnapshot {

    public String projectPath;
    public String capturedAt;
    public int totalClasses;
    public int totalSmells;
    public List<SnapshotEntry> entries;

    public ProjectSnapshot() {}

    public ProjectSnapshot(String projectPath, List<SnapshotEntry> entries) {
        this.projectPath = projectPath;
        this.capturedAt = Instant.now().toString();
        this.entries = entries;
        this.totalClasses = entries.size();
        this.totalSmells = entries.stream().mapToInt(e -> e.smellCount).sum();
    }
}
