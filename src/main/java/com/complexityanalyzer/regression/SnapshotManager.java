package com.complexityanalyzer.regression;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Saves and loads project analysis snapshots to JSON files. */
public class SnapshotManager {

    private static final String SNAPSHOT_FILE = ".analyzer-snapshot.json";
    private final ObjectMapper mapper;

    public SnapshotManager() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Saves a snapshot of the analysis result into the project directory. */
    public void save(ProjectAnalysisResult result) throws IOException {
        save(result, result.getProjectPath().resolve(SNAPSHOT_FILE));
    }

    /** Saves a snapshot to the specified file. */
    public void save(ProjectAnalysisResult result, Path outputPath) throws IOException {
        List<SnapshotEntry> entries = new ArrayList<>();
        for (ClassAnalysisResult c : result.getClasses()) {
            int smellCount = c.getSmells().size();
            for (MethodAnalysisResult m : c.getMethods()) smellCount += m.getSmells().size();
            entries.add(new SnapshotEntry(
                    c.getClassName(), c.getRiskLevel(), c.getRiskScore(),
                    c.getMetrics().getLinesOfCode(), c.getMetrics().getNcloc(),
                    c.getMetrics().getTotalComplexity(),
                    c.getMetrics().getTotalCognitiveComplexity(),
                    c.getMetrics().getMethodCount(), c.getMetrics().getFieldCount(),
                    c.getMetrics().getLcom4(), smellCount,
                    c.getDependency().getCoupling()));
        }
        ProjectSnapshot snapshot = new ProjectSnapshot(
                result.getProjectPath().toString(), entries);
        mapper.writeValue(outputPath.toFile(), snapshot);
    }

    /** Loads a baseline snapshot from the project directory or directly from a snapshot file. */
    public ProjectSnapshot load(Path path) throws IOException {
        Path snapshotFile = java.nio.file.Files.isDirectory(path)
                ? path.resolve(SNAPSHOT_FILE)
                : path;
        return mapper.readValue(snapshotFile.toFile(), ProjectSnapshot.class);
    }

    public ProjectSnapshot loadFromFile(Path snapshotFile) throws IOException {
        return mapper.readValue(snapshotFile.toFile(), ProjectSnapshot.class);
    }

    public boolean hasSnapshot(Path projectPath) {
        return projectPath.resolve(SNAPSHOT_FILE).toFile().exists();
    }
}
