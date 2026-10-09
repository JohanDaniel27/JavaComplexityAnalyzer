package com.complexityanalyzer.analysis;

import java.nio.file.Path;
import java.util.List;

public class ProjectAnalysisResult {

    private final Path projectPath;
    private final List<ClassAnalysisResult> classes;

    public ProjectAnalysisResult(
            Path projectPath,
            List<ClassAnalysisResult> classes) {

        this.projectPath = projectPath;
        this.classes = List.copyOf(classes);
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public List<ClassAnalysisResult> getClasses() {
        return classes;
    }
}