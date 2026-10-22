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
    private final Label summaryLabel  = new Label("Ready to analyze");
    private final Label statusLabel   = new Label("Select a Java project directory to begin");
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

        // Header Labels
        Label title = new Label("Java Complexity Analyzer");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Static AST metrics, architectural dependencies, code smells, and maintenance risk");
        subtitle.getStyleClass().add("app-subtitle");

        folderLabel.setWrapText(true);
        folderLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-family: monospace; -fx-font-size: 12px;");
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        chooseButton.getStyleClass().add("button");
        chooseButton.setOnAction(e -> chooseFolder(stage));

        analyzeButton.getStyleClass().addAll("button", "button-primary");
        analyzeButton.setDisable(true);
        analyzeButton.setOnAction(e -> analyzeProject());

        HBox controls = new HBox(12, chooseButton, folderLabel, analyzeButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        progressBar.setPrefWidth(350);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        HBox progressRow = new HBox(10, progressBar, progressLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(10, title, subtitle, controls, progressRow);
        top.getStyleClass().add("app-header");

        // Build tabs
        overviewTab    = makeTab("Overview", buildEmptyPlaceholder("Select a project and click 'Analyze Project' to view dashboard metrics."));
        classesTab     = makeTab("Classes", buildClassesPane());
        dependencyTab  = makeTab("Dependencies", buildEmptyPlaceholder("Run analysis to view interactive class dependency graph."));
        hotspotsTab    = makeTab("Hotspots", buildEmptyPlaceholder("Run analysis to view Git churn and maintenance hotspots."));
        qualityTab     = makeTab("Quality & Snapshots", buildQualityPane());
        diagnosticsTab = makeTab("Diagnostics", buildEmptyPlaceholder("Run analysis to view parser diagnostics."));

        tabs.getTabs().addAll(overviewTab, classesTab, dependencyTab,
                hotspotsTab, qualityTab, diagnosticsTab);

        // Summary bar at bottom
        summaryLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #e6edf3;");
        statusLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        HBox bottomBar = new HBox(16, summaryLabel, statusLabel);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getStyleClass().add("summary-bar");

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(tabs);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1260, 800);
        var cssUrl = getClass().getResource("/styles/dark-theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Java Complexity Analyzer");
        stage.setScene(scene);
        stage.show();
    }

    private javafx.scene.Node buildEmptyPlaceholder(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #6e7681; -fx-font-size: 14px;");
        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #121317;");
        return box;
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
        searchField.setPromptText("Filter class name…");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());

        riskFilter.getItems().addAll("All", "CRITICAL", "HIGH", "MEDIUM", "LOW");
        riskFilter.setValue("All");
        riskFilter.setOnAction(e -> applyFilters());

        smellFilter.getItems().addAll("All", "Has Smells", "No Smells");
        smellFilter.setValue("All");
        smellFilter.setOnAction(e -> applyFilters());

        Label searchLabel = new Label("Search:");
        searchLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: 500;");
        Label riskLabel = new Label("Risk Level:");
        riskLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: 500;");
        Label smellLabel = new Label("Smell Status:");
        smellLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: 500;");

        HBox filterBar = new HBox(12, searchLabel, searchField, riskLabel, riskFilter, smellLabel, smellFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 14, 10, 14));
        filterBar.setStyle("-fx-background-color: #16171e; -fx-border-color: #262833; -fx-border-width: 0 0 1 0;");

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

        TableColumn<ClassAnalysisResult, Integer> riskCol = new TableColumn<>("Risk Score");
        riskCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getRiskScore()));

        TableColumn<ClassAnalysisResult, String> levelCol = new TableColumn<>("Risk Tier");
        levelCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRiskLevel()));
        levelCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(level);
                    String color = switch (level) {
                        case "CRITICAL" -> "#ef4444";
                        case "HIGH" -> "#f97316";
                        case "MEDIUM" -> "#f59e0b";
                        default -> "#10b981";
                    };
                    badge.setStyle(String.format(
                            "-fx-background-color: %s22; -fx-text-fill: %s; -fx-border-color: %s; " +
                            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;",
                            color, color, color));
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        classTable.getColumns().addAll(nameCol, locCol, nclocCol, methodsCol, ccCol, cogCol, lcom4Col, coupCol, riskCol, levelCol);
        classTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        classTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, sel) -> { if (sel != null) showClassDetail(sel); });

        // Right side: tab between text detail and code viewer
        detailsArea.setEditable(false);
        detailsArea.setWrapText(false);
        detailsArea.setPromptText("Select a class from the table to view metrics and detected smells.");

        TabPane detailTabs = new TabPane();
        Tab metricsDetailTab = makeTab("Metrics & Smells", detailsArea);
        Tab codeTab = makeTab("Source View", codeViewer.getNode());
        detailTabs.getTabs().addAll(metricsDetailTab, codeTab);

        SplitPane split = new SplitPane(classTable, detailTabs);
        split.setDividerPositions(0.50);
        split.setStyle("-fx-background-color: #121317;");

        VBox classesPane = new VBox(filterBar, split);
        classesPane.setStyle("-fx-background-color: #121317;");
        VBox.setVgrow(split, Priority.ALWAYS);
        return classesPane;
    }

    private javafx.scene.Node buildQualityPane() {
        Button saveBtn    = new Button("Save Baseline Snapshot");
        Button compareBtn = new Button("Compare with Baseline");
        Button exportBtn  = new Button("Export Report…");
        exportBtn.getStyleClass().add("button-primary");

        Label qgLabel = new Label("Quality Gate: Not evaluated yet");
        qgLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #e6edf3;");

        TextArea qgDetails = new TextArea();
        qgDetails.setEditable(false);
        qgDetails.setWrapText(true);
        qgDetails.setPrefHeight(260);

        TextArea diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.setWrapText(true);
        diffArea.setPrefHeight(220);
        diffArea.setPromptText("Regression diff will appear here after comparison with a baseline snapshot.");

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
                    diffArea.setText("No baseline snapshot found. Click 'Save Baseline Snapshot' first.");
                    return;
                }
                ProjectSnapshot baseline = snapshotManager.load(lastResult.getProjectPath());
                SnapshotComparator comparator = new SnapshotComparator();
                RegressionDiffResult diff = comparator.compare(baseline, lastResult);
                StringBuilder sb = new StringBuilder();
                sb.append("=== REGRESSION DIFF ===").append("\n");
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

        Label presetLabel = new Label("Quality Gate Preset:");
        presetLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: 500;");

        HBox toolbar = new HBox(12, presetLabel, presetBox, saveBtn, compareBtn, exportBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 14, 10, 14));
        toolbar.setStyle("-fx-background-color: #16171e; -fx-border-color: #262833; -fx-border-width: 0 0 1 0;");

        Label regrLabel = new Label("Regression Diff:");
        regrLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #e6edf3;");

        VBox pane = new VBox(12, toolbar, qgLabel, qgDetails, new Separator(), regrLabel, diffArea);
        pane.setPadding(new Insets(16));
        pane.setStyle("-fx-background-color: #121317;");

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
            statusLabel.setText("Directory selected. Click 'Analyze Project' to run.");
        }
    }

    private void analyzeProject() {
        if (selectedPath == null) return;
        Path projectPath = selectedPath;

        setBusy(true);
        statusLabel.setText("Analyzing source tree…");
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
            statusLabel.setText("Completed. Classes: " + lastResult.getClasses().size()
                    + "  |  Cycles: " + lastResult.getCycles().size()
                    + "  |  Diagnostics: " + lastResult.getDiagnostics().size());
            progressLabel.setText("Ready");
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
            Label lbl = new Label("No hotspot data (target folder is not a Git repository or has no commit history).");
            lbl.setStyle("-fx-text-fill: #6e7681; -fx-font-size: 13px;");
            VBox b = new VBox(lbl);
            b.setAlignment(Pos.CENTER);
            b.setPadding(new Insets(24));
            b.setStyle("-fx-background-color: #121317;");
            return b;
        }

        TableView<com.complexityanalyzer.git.HotspotResult> hotTable = new TableView<>();

        TableColumn<com.complexityanalyzer.git.HotspotResult, String> classCol = new TableColumn<>("Class");
        classCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getClassName()));

        TableColumn<com.complexityanalyzer.git.HotspotResult, String> tierCol = new TableColumn<>("Tier");
        tierCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTier().toString()));
        tierCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tier, boolean empty) {
                super.updateItem(tier, empty);
                if (empty || tier == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(tier);
                    String color = switch (tier) {
                        case "CRITICAL_HOTSPOT" -> "#ef4444";
                        case "HIGH_HOTSPOT" -> "#f97316";
                        case "MODERATE" -> "#f59e0b";
                        default -> "#10b981";
                    };
                    badge.setStyle(String.format(
                            "-fx-background-color: %s22; -fx-text-fill: %s; -fx-border-color: %s; " +
                            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;",
                            color, color, color));
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

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

        Label title = new Label("Maintenance Hotspots (Ranked by Churn × Structural Complexity)");
        title.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #e6edf3;");

        VBox box = new VBox(10, title, hotTable);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #121317;");
        VBox.setVgrow(hotTable, Priority.ALWAYS);
        return box;
    }

    private javafx.scene.Node buildDiagnosticsPane(ProjectAnalysisResult result) {
        TextArea area = new TextArea();
        area.setEditable(false);
        if (result.getDiagnostics().isEmpty()) {
            area.setText("No diagnostics. All source files parsed successfully without errors or warnings.");
        } else {
            StringBuilder sb = new StringBuilder();
            result.getDiagnostics().forEach(d -> sb.append(d).append("\n"));
            area.setText(sb.toString());
        }

        Label label = new Label("Parser Diagnostics & Skipped Constructs:");
        label.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #e6edf3;");

        VBox box = new VBox(10, label, area);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #121317;");
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }

    private void updateQualityGateDisplay(Label qgLabel, TextArea qgDetails) {
        if (lastGateResult == null) return;
        if (lastGateResult.isPassed()) {
            qgLabel.setText("[PASSED] Quality Gate - All criteria satisfied");
            qgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981; -fx-font-size: 14px;");
            qgDetails.setText("All defined quality gate conditions passed successfully.");
        } else {
            qgLabel.setText("[FAILED] Quality Gate (" + lastGateResult.getViolationCount() + " violations)");
            qgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-font-size: 14px;");
            StringBuilder sb = new StringBuilder();
            lastGateResult.getViolations().forEach(v ->
                    sb.append("• ").append(v).append("\n"));
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
        out.append("File:  ").append(result.getSourceFile()).append("\n");
        out.append("Risk:  ").append(result.getRiskLevel())
                .append(" (").append(result.getRiskScore()).append("/100)\n\n");

        out.append("── CLASS METRICS ──────────────────────────────────────────\n");
        out.append(String.format("  Lines of Code (LOC):   %d%n", m.getLinesOfCode()));
        out.append(String.format("  NCLOC (Non-Comment):   %d%n", m.getNcloc()));
        out.append(String.format("  CLOC (Comments):       %d%n", m.getCloc()));
        out.append(String.format("  Blank Lines:           %d%n", m.getBlankLines()));
        out.append(String.format("  Method Count:          %d%n", m.getMethodCount()));
        out.append(String.format("  Field Count:           %d%n", m.getFieldCount()));
        out.append(String.format("  Total Cyclomatic CC:   %d%n", m.getTotalComplexity()));
        out.append(String.format("  Avg Cyclomatic CC:     %.1f%n", m.getAverageComplexity()));
        out.append(String.format("  Total Cognitive CC:    %d%n", m.getTotalCognitiveComplexity()));
        out.append(String.format("  Max Nesting Depth:     %d%n", m.getMaxNestingDepth()));
        out.append(String.format("  Weighted Methods (WMC):%d%n", m.getWmc()));
        out.append(String.format("  Response for Class(RFC):%d%n", m.getRfc()));
        out.append(String.format("  Lack of Cohesion(LCOM4):%d%n", m.getLcom4()));
        out.append(String.format("  Efferent Coupling (Ce):%d%n", result.getDependency().getCoupling()));
        out.append(String.format("  Afferent Coupling (Ca):%d%n", result.getDependency().getAfferentCoupling()));
        out.append(String.format("  Direct Dependencies:   %s%n", result.getDependency().getDependencies()));

        appendSmells(out, "\n── CLASS CODE SMELLS ─────────────────────────────────────", result.getSmells());

        out.append("\n── METHOD DETAILS ─────────────────────────────────────────\n");
        for (MethodAnalysisResult method : result.getMethods()) {
            var mm = method.getMetrics();
            out.append(String.format("%n  %s  [Lines %d–%d]%n",
                    mm.getMethodName(), mm.getStartLine(), mm.getEndLine()));
            out.append(String.format("    LOC: %d  |  NCLOC: %d  |  Params: %d  |  Stmts: %d%n",
                    mm.getLinesOfCode(), mm.getNcloc(), mm.getParameterCount(), mm.getStatementCount()));
            out.append(String.format("    Cyclomatic: %d  |  Cognitive: %d  |  Nesting Depth: %d%n",
                    mm.getComplexity(), mm.getCognitiveComplexity(), mm.getNestingDepth()));
            appendSmells(out, "    Smells", method.getSmells());
        }

        detailsArea.setText(out.toString());
    }

    private void appendSmells(StringBuilder out, String heading, List<CodeSmell> smells) {
        out.append(heading).append(":\n");
        if (smells.isEmpty()) {
            out.append("  None detected\n");
            return;
        }
        for (CodeSmell smell : smells) {
            out.append(String.format("  - [%s] %s: %s%n",
                    smell.getSeverity(), smell.getType(), smell.getMessage()));
            if (smell.getAdvice() != null) {
                out.append(String.format("    Advice: %s%n", smell.getAdvice().getGuidance()));
            }
            if (smell.getLocation() != null) {
                out.append(String.format("    Location: %s%n", smell.getLocation()));
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
                "Classes: %d   |   Methods: %d   |   Smells: %d   |   High/Critical Risk: %d   |   Cycles: %d   |   Clones: %d",
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
        progressLabel.setVisible(true);
    }

    public static void main(String[] args) { launch(args); }
}