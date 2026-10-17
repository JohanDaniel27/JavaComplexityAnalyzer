package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalyzer;
import com.complexityanalyzer.export.CsvReportExporter;
import com.complexityanalyzer.export.HtmlReportExporter;
import com.complexityanalyzer.export.JsonReportExporter;
import com.complexityanalyzer.export.MarkdownReportExporter;
import com.complexityanalyzer.qualitygate.QualityGateConfig;
import com.complexityanalyzer.qualitygate.QualityGateEvaluator;
import com.complexityanalyzer.qualitygate.QualityGateResult;
import com.complexityanalyzer.regression.RegressionDiffResult;
import com.complexityanalyzer.regression.SnapshotComparator;
import com.complexityanalyzer.regression.SnapshotManager;
import com.complexityanalyzer.regression.ProjectSnapshot;
import com.complexityanalyzer.smells.CodeSmell;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class AnalyzerApp extends Application {

    // ---- Analysis state ----
    private Path selectedPath;
    private ProjectAnalysisResult lastResult;
    private QualityGateResult lastGateResult;
    private QualityGateConfig gateConfig = QualityGateConfig.standard();
    private final SnapshotManager snapshotManager = new SnapshotManager();
    private final QualityGateEvaluator gateEvaluator = new QualityGateEvaluator();

    // ---- Controls ----
    private final Label folderLabel   = new Label("No folder selected");
    private final Label summaryLabel  = new Label("No analysis yet");
    private final Label statusLabel   = new Label("Choose a Java project folder");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("");
    private final Button chooseButton  = new Button("Choose Folder");
    private final Button analyzeButton = new Button("Analyze Project");

    // ---- Tabs ----
    private Tab overviewTab;
    private Tab classesTab;
    private Tab dependencyTab;
    private Tab hotspotsTab;
    private Tab qualityTab;
    private Tab diagnosticsTab;
    private final TabPane tabs = new TabPane();

    // ---- Classes tab controls ----
    private final TableView<ClassAnalysisResult> classTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> riskFilter  = new ComboBox<>();
    private final ComboBox<String> smellFilter = new ComboBox<>();
    private final TextArea detailsArea = new TextArea();
    private final CodeViewerPane codeViewer = new CodeViewerPane();

    // ---- Dashboard / graph builders ----
    private final DashboardCharts dashboardCharts = new DashboardCharts();
    private final DependencyGraphView graphView    = new DependencyGraphView();

    @Override
    public void start(Stage stage) {

        // Header
        Label title    = new Label("Java Complexity Analyzer");
        Label subtitle = new Label("Analyze complexity, smells, dependencies, and maintenance risk.");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        subtitle.setStyle("-fx-text-fill: #555;");

        folderLabel.setWrapText(true);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        analyzeButton.setDisable(true);
        chooseButton.setOnAction(e -> chooseFolder(stage));
        analyzeButton.setOnAction(e -> analyzeProject());

        HBox controls = new HBox(10, chooseButton, folderLabel, analyzeButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        HBox progressRow = new HBox(10, progressBar, progressLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        summaryLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        statusLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");

        VBox top = new VBox(8, title, subtitle, controls, progressRow, summaryLabel, statusLabel);
        top.setPadding(new Insets(16));

        // Build tabs
        overviewTab    = makeTab("Overview", new Label("Run analysis to view dashboard."));
        classesTab     = makeTab("Classes", buildClassesPane());
        dependencyTab  = makeTab("Dependencies", new Label("Run analysis to view graph."));
        hotspotsTab    = makeTab("Hotspots", new Label("Run analysis to view hotspots."));
        qualityTab     = makeTab("Quality & Snapshots", buildQualityPane());
        diagnosticsTab = makeTab("Diagnostics", new Label("Run analysis to view diagnostics."));

        tabs.getTabs().addAll(overviewTab, classesTab, dependencyTab,
                hotspotsTab, qualityTab, diagnosticsTab);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(tabs);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: #f4f6fa;");

        stage.setTitle("Java Complexity Analyzer");
        stage.setScene(new Scene(root, 1200, 750));
        stage.show();
    }

    // ------------------------------------------------------------------ //
    // Tab builders
    // ------------------------------------------------------------------ //

    private Tab makeTab(String name, javafx.scene.Node content) {
        Tab t = new Tab(name, content);
        t.setClosable(false);
        return t;
    }

    private javafx.scene.Node buildClassesPane() {

        // Filter bar
        searchField.setPromptText("Search class or method…");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());

        riskFilter.getItems().addAll("All", "CRITICAL", "HIGH", "MEDIUM", "LOW");
        riskFilter.setValue("All");
        riskFilter.setOnAction(e -> applyFilters());

        smellFilter.getItems().addAll("All", "Has Smells", "No Smells");
        smellFilter.setValue("All");
        smellFilter.setOnAction(e -> applyFilters());

        HBox filterBar = new HBox(10, new Label("Search:"), searchField,
                new Label("Risk:"), riskFilter,
                new Label("Smells:"), smellFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(8, 8, 4, 8));

        // Class table columns
        TableColumn<ClassAnalysisResult, String> nameCol = new TableColumn<>("Class");
        nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getClassName()));

        TableColumn<ClassAnalysisResult, Integer> locCol = new TableColumn<>("LOC");
        locCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMetrics().getLinesOfCode()));

        TableColumn<ClassAnalysisResult, Integer> nclocCol = new TableColumn<>("NCLOC");
        nclocCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMetrics().getNcloc()));

        TableColumn<ClassAnalysisResult, Integer> methodsCol = new TableColumn<>("Methods");
        methodsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMetrics().getMethodCount()));

        TableColumn<ClassAnalysisResult, Integer> ccCol = new TableColumn<>("Cyclomatic");
        ccCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMetrics().getTotalComplexity()));

        TableColumn<ClassAnalysisResult, Integer> cogCol = new TableColumn<>("Cognitive");
        cogCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMetrics().getTotalCognitiveComplexity()));

        TableColumn<ClassAnalysisResult, Integer> lcom4Col = new TableColumn<>("LCOM4");
        lcom4Col.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMetrics().getLcom4()));

        TableColumn<ClassAnalysisResult, Integer> coupCol = new TableColumn<>("Coupling");
        coupCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getDependency().getCoupling()));

        TableColumn<ClassAnalysisResult, Integer> riskCol = new TableColumn<>("Risk");
        riskCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getRiskScore()));

        TableColumn<ClassAnalysisResult, String> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRiskLevel()));

        classTable.getColumns().addAll(nameCol, locCol, nclocCol, methodsCol, ccCol, cogCol, lcom4Col, coupCol, riskCol, levelCol);
        classTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        classTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, sel) -> { if (sel != null) showClassDetail(sel); });

        // Right side: tab between text detail and code viewer
        detailsArea.setEditable(false);
        detailsArea.setWrapText(false);
        detailsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        detailsArea.setPromptText("Select a class to view details.");

        TabPane detailTabs = new TabPane();
        Tab metricsDetailTab = makeTab("Metrics", detailsArea);
        Tab codeTab = makeTab("Source", codeViewer.getNode());
        detailTabs.getTabs().addAll(metricsDetailTab, codeTab);

        SplitPane split = new SplitPane(classTable, detailTabs);
        split.setDividerPositions(0.50);

        VBox classesPane = new VBox(filterBar, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        return classesPane;
    }

    private javafx.scene.Node buildQualityPane() {
        Button saveBtn    = new Button("Save Baseline Snapshot");
        Button compareBtn = new Button("Compare with Baseline");
        Button exportBtn  = new Button("Export Report…");

        Label qgLabel = new Label("Quality Gate: Not yet evaluated");
        qgLabel.setStyle("-fx-font-weight: bold;");

        TextArea qgDetails = new TextArea();
        qgDetails.setEditable(false);
        qgDetails.setWrapText(true);
        qgDetails.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        qgDetails.setPrefHeight(300);

        TextArea diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.setWrapText(true);
        diffArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        diffArea.setPrefHeight(200);
        diffArea.setPromptText("Regression diff will appear here after comparison.");

        ComboBox<String> presetBox = new ComboBox<>();
        presetBox.getItems().addAll("Standard", "Strict", "Permissive");
        presetBox.setValue("Standard");
        presetBox.setOnAction(e -> {
            gateConfig = switch (presetBox.getValue()) {
                case "Strict" -> QualityGateConfig.strict();
                case "Permissive" -> QualityGateConfig.permissive();
                default -> QualityGateConfig.standard();
            };
            if (lastResult != null) {
                lastGateResult = gateEvaluator.evaluate(lastResult, gateConfig);
                updateQualityGateDisplay(qgLabel, qgDetails);
            }
        });

        saveBtn.setOnAction(e -> {
            if (lastResult == null) return;
            try {
                snapshotManager.save(lastResult);
                qgDetails.appendText("\nBaseline snapshot saved to: " +
                        lastResult.getProjectPath() + "\\.analyzer-snapshot.json\n");
            } catch (Exception ex) {
                qgDetails.appendText("\nError saving snapshot: " + ex.getMessage() + "\n");
            }
        });

        compareBtn.setOnAction(e -> {
            if (lastResult == null) return;
            try {
                if (!snapshotManager.hasSnapshot(lastResult.getProjectPath())) {
                    diffArea.setText("No baseline snapshot found. Run 'Save Baseline Snapshot' first.");
                    return;
                }
                ProjectSnapshot baseline = snapshotManager.load(lastResult.getProjectPath());
                SnapshotComparator comparator = new SnapshotComparator();
                RegressionDiffResult diff = comparator.compare(baseline, lastResult);
                StringBuilder sb = new StringBuilder();
                sb.append("=== Regression Diff ===").append("\n");
                sb.append("Added classes: ").append(diff.getAddedClasses().size()).append("\n");
                sb.append("Removed classes: ").append(diff.getRemovedClasses().size()).append("\n");
                sb.append("Changed classes: ").append(diff.getChangedClasses().size()).append("\n");
                sb.append("New smells introduced: ").append(diff.getNewSmells()).append("\n");
                sb.append("Smells resolved: ").append(diff.getResolvedSmells()).append("\n");
                sb.append("LOC delta: ").append(diff.getTotalLocDelta()).append("\n");
                sb.append("Complexity delta: ").append(diff.getTotalComplexityDelta()).append("\n\n");
                diff.getChangedClasses().stream().limit(20).forEach(cd -> {
                    sb.append(String.format("  %-40s  ΔLOC=%+d  ΔCC=%+d  ΔRisk=%+d  ΔSmells=%+d%n",
                            cd.getClassName(), cd.getLocDelta(), cd.getComplexityDelta(),
                            cd.getRiskScoreDelta(), cd.getSmellDelta()));
                });
                diffArea.setText(sb.toString());
            } catch (Exception ex) {
                diffArea.setText("Error during comparison: " + ex.getMessage());
            }
        });

        exportBtn.setOnAction(e -> showExportDialog());

        HBox toolbar = new HBox(10, new Label("Quality Gate Preset:"), presetBox,
                saveBtn, compareBtn, exportBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8));

        VBox pane = new VBox(10, toolbar, qgLabel, qgDetails,
                new Separator(), new Label("Regression Diff:"), diffArea);
        pane.setPadding(new Insets(12));

        // Store references for later updates
        pane.setUserData(new Object[]{qgLabel, qgDetails});
        return pane;
    }

    // ------------------------------------------------------------------ //
    // Analysis execution
    // ------------------------------------------------------------------ //

    private void chooseFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Java Project Folder");
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            selectedPath = folder.toPath();
            folderLabel.setText(selectedPath.toAbsolutePath().toString());
            analyzeButton.setDisable(false);
            statusLabel.setText("Ready to analyze.");
        }
    }

    private void analyzeProject() {
        if (selectedPath == null) return;
        Path projectPath = selectedPath;

        setBusy(true);
        statusLabel.setText("Analyzing project…");
        progressBar.setProgress(0);

        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        analyzer.setProgressCallback((stage, current, total) ->
                Platform.runLater(() -> {
                    progressBar.setProgress((double) current / total);
                    progressLabel.setText(current + "/" + total + ": " + stage);
                }));

        Task<ProjectAnalysisResult> task = new Task<>() {
            @Override
            protected ProjectAnalysisResult call() throws Exception {
                return analyzer.analyze(projectPath);
            }
        };

        task.setOnSucceeded(e -> {
            lastResult = task.getValue();
            lastGateResult = gateEvaluator.evaluate(lastResult, gateConfig);

            // Update class table
            classTable.getItems().setAll(lastResult.getClasses());

            // Overview tab
            overviewTab.setContent(dashboardCharts.create(lastResult, lastGateResult));

            // Dependencies tab
            dependencyTab.setContent(
                    graphView.create(lastResult.getClasses(), lastResult.getCycles()));

            // Hotspots tab
            hotspotsTab.setContent(buildHotspotsPane(lastResult));

            // Diagnostics tab
            diagnosticsTab.setContent(buildDiagnosticsPane(lastResult));

            // Update quality pane QG display
            javafx.scene.Node qualNode = qualityTab.getContent();
            if (qualNode instanceof VBox vb && vb.getUserData() instanceof Object[] ud) {
                updateQualityGateDisplay((Label) ud[0], (TextArea) ud[1]);
            }

            updateSummary(lastResult);
            statusLabel.setText("Analysis complete. Classes: " + lastResult.getClasses().size()
                    + "  |  Cycles: " + lastResult.getCycles().size()
                    + "  |  Diagnostics: " + lastResult.getDiagnostics().size());
            progressLabel.setText("Done");
            progressBar.setProgress(1.0);
            setBusy(false);

            if (!lastResult.getClasses().isEmpty()) {
                classTable.getSelectionModel().selectFirst();
            }
        });

        task.setOnFailed(e -> {
            Throwable err = task.getException();
            statusLabel.setText("Analysis failed.");
            progressLabel.setText("Failed");
            setBusy(false);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Analysis Failed");
            alert.setHeaderText("Could not analyze this project");
            alert.setContentText(err == null ? "Unknown error" : err.getMessage());
            alert.showAndWait();
        });

        Thread t = new Thread(task, "project-analysis");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ //
    // Helper pane builders
    // ------------------------------------------------------------------ //

    private javafx.scene.Node buildHotspotsPane(ProjectAnalysisResult result) {
        if (result.getHotspots().isEmpty()) {
            Label lbl = new Label("No hotspot data (project may not be a Git repository or has no commit history).");
            lbl.setStyle("-fx-text-fill: #888;");
            VBox b = new VBox(lbl);
            b.setPadding(new Insets(16));
            return b;
        }

        TableView<com.complexityanalyzer.git.HotspotResult> hotTable = new TableView<>();

        TableColumn<com.complexityanalyzer.git.HotspotResult, String> classCol = new TableColumn<>("Class");
        classCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getClassName()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, String> tierCol = new TableColumn<>("Tier");
        tierCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTier().toString()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, Number> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getHotspotScore()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, Integer> commitsCol = new TableColumn<>("Commits");
        commitsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCommitCount()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, Integer> churnCol = new TableColumn<>("Churn");
        churnCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getTotalChurn()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, Integer> authorsCol = new TableColumn<>("Authors");
        authorsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getAuthorCount()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, Integer> cyclicCol = new TableColumn<>("Cyclomatic");
        cyclicCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCyclomaticComplexity()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, Integer> cogCol = new TableColumn<>("Cognitive");
        cogCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCognitiveComplexity()));

        hotTable.getColumns().addAll(classCol, tierCol, scoreCol, commitsCol, churnCol, authorsCol, cyclicCol, cogCol);
        hotTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        hotTable.getItems().setAll(result.getHotspots());

        Label title = new Label("Maintenance Hotspots (ranked by combined churn × complexity)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        VBox box = new VBox(8, title, hotTable);
        box.setPadding(new Insets(12));
        VBox.setVgrow(hotTable, Priority.ALWAYS);
        return box;
    }

    private javafx.scene.Node buildDiagnosticsPane(ProjectAnalysisResult result) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        if (result.getDiagnostics().isEmpty()) {
            area.setText("No diagnostics. All files parsed successfully.");
        } else {
            StringBuilder sb = new StringBuilder();
            result.getDiagnostics().forEach(d -> sb.append(d).append("\n"));
            area.setText(sb.toString());
        }
        VBox box = new VBox(new Label("Parse diagnostics and skipped files:"), area);
        box.setPadding(new Insets(12));
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }

    private void updateQualityGateDisplay(Label qgLabel, TextArea qgDetails) {
        if (lastGateResult == null) return;
        if (lastGateResult.isPassed()) {
            qgLabel.setText("✅ Quality Gate: PASSED");
            qgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
            qgDetails.setText("All quality rules passed.");
        } else {
            qgLabel.setText("❌ Quality Gate: FAILED  (" + lastGateResult.getViolationCount() + " violations)");
            qgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            StringBuilder sb = new StringBuilder();
            lastGateResult.getViolations().forEach(v ->
                    sb.append(v).append("\n"));
            qgDetails.setText(sb.toString());
        }
    }

    // ------------------------------------------------------------------ //
    // Detail view
    // ------------------------------------------------------------------ //

    private void showClassDetail(ClassAnalysisResult result) {
        // Load source file in code viewer
        codeViewer.loadFile(result.getSourceFile());
        if (result.getSourceFile() != null) {
            codeViewer.navigateTo(result.getMetrics().getStartLine(),
                    result.getMetrics().getEndLine());
        }

        // Build metrics text
        StringBuilder out = new StringBuilder();
        var m = result.getMetrics();
        out.append("CLASS: ").append(result.getClassName()).append("\n");
        out.append("File: ").append(result.getSourceFile()).append("\n");
        out.append("Risk: ").append(result.getRiskLevel())
                .append(" (").append(result.getRiskScore()).append("/100)\n\n");

        out.append("── CLASS METRICS ──────────────────────────\n");
        out.append(String.format("  LOC:                   %d%n", m.getLinesOfCode()));
        out.append(String.format("  NCLOC:                 %d%n", m.getNcloc()));
        out.append(String.format("  CLOC:                  %d%n", m.getCloc()));
        out.append(String.format("  Blank Lines:           %d%n", m.getBlankLines()));
        out.append(String.format("  Methods:               %d%n", m.getMethodCount()));
        out.append(String.format("  Fields:                %d%n", m.getFieldCount()));
        out.append(String.format("  Total Cyclomatic CC:   %d%n", m.getTotalComplexity()));
        out.append(String.format("  Avg Cyclomatic CC:     %.1f%n", m.getAverageComplexity()));
        out.append(String.format("  Total Cognitive CC:    %d%n", m.getTotalCognitiveComplexity()));
        out.append(String.format("  Max Nesting Depth:     %d%n", m.getMaxNestingDepth()));
        out.append(String.format("  WMC:                   %d%n", m.getWmc()));
        out.append(String.format("  RFC:                   %d%n", m.getRfc()));
        out.append(String.format("  LCOM4:                 %d%n", m.getLcom4()));
        out.append(String.format("  Coupling (efferent):   %d%n", result.getDependency().getCoupling()));
        out.append(String.format("  Afferent Coupling:     %d%n", result.getDependency().getAfferentCoupling()));
        out.append(String.format("  Dependencies: %s%n", result.getDependency().getDependencies()));

        appendSmells(out, "\n── CLASS CODE SMELLS ─────────────────────", result.getSmells());

        out.append("\n── METHOD DETAILS ──────────────────────────\n");
        for (MethodAnalysisResult method : result.getMethods()) {
            var mm = method.getMetrics();
            out.append(String.format("%n  %s  [line %d–%d]%n",
                    mm.getMethodName(), mm.getStartLine(), mm.getEndLine()));
            out.append(String.format("    LOC: %d  NCLOC: %d  Params: %d  Stmts: %d%n",
                    mm.getLinesOfCode(), mm.getNcloc(), mm.getParameterCount(), mm.getStatementCount()));
            out.append(String.format("    Cyclomatic: %d  Cognitive: %d  Nesting: %d%n",
                    mm.getComplexity(), mm.getCognitiveComplexity(), mm.getNestingDepth()));
            appendSmells(out, "    Smells", method.getSmells());
        }

        detailsArea.setText(out.toString());
    }

    private void appendSmells(StringBuilder out, String heading, List<CodeSmell> smells) {
        out.append(heading).append(":\n");
        if (smells.isEmpty()) {
            out.append("  None\n");
            return;
        }
        for (CodeSmell smell : smells) {
            out.append(String.format("  - [%s] %s: %s%n",
                    smell.getSeverity(), smell.getType(), smell.getMessage()));
            if (smell.getAdvice() != null) {
                out.append(String.format("    💡 %s%n", smell.getAdvice().getGuidance()));
            }
            if (smell.getLocation() != null) {
                out.append(String.format("    📍 %s%n", smell.getLocation()));
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Summary
    // ------------------------------------------------------------------ //

    private void updateSummary(ProjectAnalysisResult result) {
        int methods = result.getClasses().stream().mapToInt(c -> c.getMethods().size()).sum();
        int smells  = result.getClasses().stream().mapToInt(c -> {
            int s = c.getSmells().size();
            for (MethodAnalysisResult m : c.getMethods()) s += m.getSmells().size();
            return s;
        }).sum();
        long risky = result.getClasses().stream()
                .filter(c -> c.getRiskLevel().equals("HIGH") || c.getRiskLevel().equals("CRITICAL"))
                .count();
        summaryLabel.setText(String.format(
                "Classes: %d   Methods: %d   Smells: %d   High/Critical Risk: %d   Cycles: %d   Clones: %d",
                result.getClasses().size(), methods, smells, risky,
                result.getCycles().size(), result.getClones().size()));
    }

    // ------------------------------------------------------------------ //
    // Filters
    // ------------------------------------------------------------------ //

    private void applyFilters() {
        if (lastResult == null) return;
        String search    = searchField.getText().toLowerCase();
        String riskLevel = riskFilter.getValue();
        String smellOpt  = smellFilter.getValue();

        List<ClassAnalysisResult> filtered = lastResult.getClasses().stream()
                .filter(c -> {
                    if (!search.isEmpty() && !c.getClassName().toLowerCase().contains(search)) return false;
                    if (!"All".equals(riskLevel) && !c.getRiskLevel().equals(riskLevel)) return false;
                    if ("Has Smells".equals(smellOpt)) {
                        int s = c.getSmells().size();
                        for (MethodAnalysisResult m : c.getMethods()) s += m.getSmells().size();
                        if (s == 0) return false;
                    }
                    if ("No Smells".equals(smellOpt)) {
                        int s = c.getSmells().size();
                        for (MethodAnalysisResult m : c.getMethods()) s += m.getSmells().size();
                        if (s > 0) return false;
                    }
                    return true;
                })
                .toList();
        classTable.getItems().setAll(filtered);
    }

    // ------------------------------------------------------------------ //
    // Export
    // ------------------------------------------------------------------ //

    private void showExportDialog() {
        if (lastResult == null) {
            new Alert(Alert.AlertType.INFORMATION, "Run analysis first.").showAndWait();
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>("HTML", "HTML", "JSON", "CSV", "Markdown");
        dialog.setTitle("Export Report");
        dialog.setHeaderText("Choose export format");
        dialog.setContentText("Format:");
        dialog.showAndWait().ifPresent(format -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Report");
            String ext = switch (format) {
                case "JSON" -> ".json";
                case "CSV"  -> "";
                case "Markdown" -> ".md";
                default -> ".html";
            };
            if (!"".equals(ext)) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(format, "*" + ext));
            }
            File out = fc.showSaveDialog(null);
            if (out == null) return;
            try {
                switch (format) {
                    case "HTML" -> new HtmlReportExporter().export(lastResult, lastGateResult, out.toPath());
                    case "JSON" -> new JsonReportExporter().export(lastResult, lastGateResult, out.toPath());
                    case "CSV"  -> new CsvReportExporter().export(lastResult, out.getParentFile().toPath());
                    case "Markdown" -> new MarkdownReportExporter().export(lastResult, lastGateResult, out.toPath());
                }
                new Alert(Alert.AlertType.INFORMATION,
                        "Report exported to: " + out.getAbsolutePath()).showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).showAndWait();
            }
        });
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private void setBusy(boolean busy) {
        chooseButton.setDisable(busy);
        analyzeButton.setDisable(busy || selectedPath == null);
        progressBar.setVisible(busy);
        progressBar.setManaged(busy);
        progressLabel.setVisible(busy || !busy); // always visible
    }

    public static void main(String[] args) { launch(args); }
}