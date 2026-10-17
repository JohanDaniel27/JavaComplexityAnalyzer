package com.complexityanalyzer.analysis;

import com.complexityanalyzer.architecture.DependencyCycle;
import com.complexityanalyzer.git.HotspotResult;
import com.complexityanalyzer.smells.CodeCloneFinding;

import java.nio.file.Path;
import java.util.List;

/** Aggregates all analysis results for a scanned project directory. */
public class ProjectAnalysisResult {

    private final Path projectPath;
    private final List<ClassAnalysisResult> classes;
    private final List<AnalysisDiagnostic> diagnostics;
    private final List<DependencyCycle> cycles;
    private final List<HotspotResult> hotspots;
    private final List<CodeCloneFinding> clones;

    public ProjectAnalysisResult(Path projectPath,
                                  List<ClassAnalysisResult> classes,
                                  List<AnalysisDiagnostic> diagnostics,
                                  List<DependencyCycle> cycles,
                                  List<HotspotResult> hotspots,
                                  List<CodeCloneFinding> clones) {
        this.projectPath = projectPath;
        this.classes    = List.copyOf(classes);
        this.diagnostics = List.copyOf(diagnostics);
        this.cycles     = List.copyOf(cycles);
        this.hotspots   = List.copyOf(hotspots);
        this.clones     = List.copyOf(clones);
    }

    /** Minimal constructor for backward compatibility. */
    public ProjectAnalysisResult(Path projectPath, List<ClassAnalysisResult> classes) {
        this(projectPath, classes, List.of(), List.of(), List.of(), List.of());
    }

    /** Constructor with diagnostics only (for partial analysis callers). */
    public ProjectAnalysisResult(Path projectPath,
                                  List<ClassAnalysisResult> classes,
                                  List<AnalysisDiagnostic> diagnostics) {
        this(projectPath, classes, diagnostics, List.of(), List.of(), List.of());
    }

    public Path getProjectPath()                   { return projectPath; }
    public List<ClassAnalysisResult> getClasses()  { return classes; }
    public List<AnalysisDiagnostic> getDiagnostics() { return diagnostics; }
    public List<DependencyCycle> getCycles()        { return cycles; }
    public List<HotspotResult> getHotspots()        { return hotspots; }
    public List<CodeCloneFinding> getClones()       { return clones; }
}