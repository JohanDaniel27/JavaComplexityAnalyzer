package com.complexityanalyzer.analysis;

import com.complexityanalyzer.architecture.CycleDetector;
import com.complexityanalyzer.architecture.DependencyCycle;
import com.complexityanalyzer.dependency.DependencyAnalyzer;
import com.complexityanalyzer.dependency.DependencyResult;
import com.complexityanalyzer.git.FileGitStats;
import com.complexityanalyzer.git.GitHistoryAnalyzer;
import com.complexityanalyzer.git.HotspotRanker;
import com.complexityanalyzer.git.HotspotResult;
import com.complexityanalyzer.metrics.ClassMetrics;
import com.complexityanalyzer.metrics.ClassMetricsAnalyzer;
import com.complexityanalyzer.metrics.MethodMetrics;
import com.complexityanalyzer.metrics.MetricsAnalyzer;
import com.complexityanalyzer.parser.JavaFileParser;
import com.complexityanalyzer.parser.ParseFileResult;
import com.complexityanalyzer.parser.ProjectScanner;
import com.complexityanalyzer.smells.CodeSmell;
import com.complexityanalyzer.smells.CodeSmellDetector;
import com.complexityanalyzer.smells.DuplicateCodeDetector;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Orchestrates all analysis passes:
 * parsing -> metrics -> smells -> dependencies -> cycles -> hotspots.
 */
public class ProjectAnalyzer {

    private final ProjectScanner scanner             = new ProjectScanner();
    private final JavaFileParser parser              = new JavaFileParser();
    private final MetricsAnalyzer metricsAnalyzer   = new MetricsAnalyzer();
    private final ClassMetricsAnalyzer classAnalyzer = new ClassMetricsAnalyzer();
    private final CodeSmellDetector smellDetector    = new CodeSmellDetector();
    private final DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer();
    private final RiskCalculator riskCalculator      = new RiskCalculator();
    private final CycleDetector cycleDetector        = new CycleDetector();
    private final GitHistoryAnalyzer gitAnalyzer     = new GitHistoryAnalyzer();
    private final HotspotRanker hotspotRanker        = new HotspotRanker();
    private final DuplicateCodeDetector cloneDetector = new DuplicateCodeDetector();

    /** Progress callback: receives a stage message string (may be null for no callback). */
    private ProgressCallback progressCallback;

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(String stage, int current, int total);
    }

    public ProjectAnalysisResult analyze(Path project) throws Exception {

        progress("Scanning source files", 0, 5);
        List<Path> files = scanner.findJavaFiles(project);

        progress("Parsing AST", 1, 5);
        List<CompilationUnit> units  = new ArrayList<>();
        List<AnalysisDiagnostic> diagnostics = new ArrayList<>();
        List<String[]> fileLinesList = new ArrayList<>();  // file lines per unit
        List<Path> parsedFiles = new ArrayList<>();

        for (Path file : files) {
            try {
                ParseFileResult result = parser.parse(file);
                if (result.isSuccessful() && result.getCompilationUnit().isPresent()) {
                    CompilationUnit cu = result.getCompilationUnit().get();
                    units.add(cu);
                    parsedFiles.add(file);
                    // Read file lines for NCLOC analysis
                    try {
                        String content = Files.readString(file);
                        fileLinesList.add(content.split("\n", -1));
                    } catch (Exception e) {
                        fileLinesList.add(new String[0]);
                    }
                } else {
                    for (Problem p : result.getProblems()) {
                        int line = p.getLocation()
                                .flatMap(loc -> loc.toRange())
                                .map(r -> r.begin.line)
                                .orElse(0);
                        diagnostics.add(new AnalysisDiagnostic(
                                file, AnalysisDiagnostic.Severity.ERROR,
                                p.getMessage(), line));
                    }
                }
            } catch (Exception e) {
                diagnostics.add(new AnalysisDiagnostic(
                        file, AnalysisDiagnostic.Severity.ERROR, e.getMessage(), 0));
            }
        }

        progress("Analysing dependencies", 2, 5);
        Map<String, DependencyResult> dependencies = dependencyAnalyzer.analyze(units);

        progress("Computing metrics and smells", 3, 5);
        List<ClassAnalysisResult> classResults = new ArrayList<>();

        // Collect MethodInfo for clone detection
        List<DuplicateCodeDetector.MethodInfo> methodInfos = new ArrayList<>();

        for (int i = 0; i < units.size(); i++) {
            CompilationUnit unit = units.get(i);
            Path file = parsedFiles.get(i);
            String[] fileLines = fileLinesList.get(i);

            for (ClassOrInterfaceDeclaration clazz :
                    unit.findAll(ClassOrInterfaceDeclaration.class)) {

                List<MethodAnalysisResult> methodResults = new ArrayList<>();

                for (MethodDeclaration method : clazz.getMethods()) {
                    MethodMetrics metrics = metricsAnalyzer.analyzeMethod(method, fileLines);
                    List<CodeSmell> methodSmells = smellDetector.detectMethodSmells(metrics, file);
                    CodeSmell brainSmell = smellDetector.detectBrainMethod(metrics, file);
                    if (brainSmell != null) methodSmells = new ArrayList<>(methodSmells) {{ add(brainSmell); }};
                    methodResults.add(new MethodAnalysisResult(metrics, methodSmells));

                    // Collect for clone detection
                    List<Statement> stmts = method.findAll(Statement.class);
                    if (stmts.size() >= 6) {
                        methodInfos.add(new DuplicateCodeDetector.MethodInfo(
                                clazz.getNameAsString(), method.getNameAsString(), file, stmts));
                    }
                }

                ClassMetrics classMetrics = classAnalyzer.analyzeClass(clazz, fileLines);
                List<CodeSmell> classSmells = smellDetector.detectClassSmells(classMetrics, file);

                DependencyResult dependency = dependencies.getOrDefault(
                        clazz.getNameAsString(),
                        new DependencyResult(clazz.getNameAsString(), Set.of()));

                List<CodeSmell> allSmells = new ArrayList<>(classSmells);
                for (MethodAnalysisResult m : methodResults) allSmells.addAll(m.getSmells());

                int riskScore = riskCalculator.calculate(classMetrics, dependency, allSmells);
                String riskLevel = riskCalculator.getRiskLevel(riskScore);

                classResults.add(new ClassAnalysisResult(
                        clazz.getNameAsString(), file, classMetrics, dependency,
                        methodResults, classSmells, riskScore, riskLevel));
            }
        }

        progress("Running Git hotspot analysis", 4, 5);
        Map<String, FileGitStats> gitStats = gitAnalyzer.analyze(project);
        List<HotspotResult> hotspots = hotspotRanker.rank(classResults, gitStats);

        // Detect circular dependencies
        List<DependencyCycle> cycles = cycleDetector.detectClassCycles(classResults);

        // Detect code clones
        List<com.complexityanalyzer.smells.CodeCloneFinding> clones =
                cloneDetector.detectFromMethods(methodInfos);

        progress("Complete", 5, 5);

        return new ProjectAnalysisResult(project, classResults, diagnostics, cycles, hotspots, clones);
    }

    private void progress(String stage, int current, int total) {
        if (progressCallback != null) {
            progressCallback.onProgress(stage, current, total);
        }
    }
}