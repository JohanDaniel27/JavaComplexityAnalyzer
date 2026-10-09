
package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.*;
import com.complexityanalyzer.smells.CodeSmell;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class AnalyzerApp extends Application {

    private final TableView<ClassAnalysisResult> table = new TableView<>();
    private final TextArea details = new TextArea();

    private final Label folderLabel = new Label("No folder selected");
    private final Label summaryLabel = new Label("No analysis yet");
    private final Label statusLabel = new Label("Choose a Java project folder");

    private final Button chooseButton = new Button("Choose Folder");
    private final Button analyzeButton = new Button("Analyze Project");
    private final ProgressIndicator progress = new ProgressIndicator();

    private Path selectedPath;

    @Override
    public void start(Stage stage) {

        Label title = new Label("Java Complexity Analyzer");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label subtitle = new Label(
                "Analyze complexity, code smells, dependencies and risk."
        );

        folderLabel.setWrapText(true);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        analyzeButton.setDisable(true);
        progress.setPrefSize(25, 25);
        progress.setVisible(false);
        progress.setManaged(false);

        chooseButton.setOnAction(e -> chooseFolder(stage));
        analyzeButton.setOnAction(e -> analyzeProject());

        HBox controls = new HBox(
                10, chooseButton, folderLabel, analyzeButton, progress
        );

        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox top = new VBox(12, title, subtitle, controls, summaryLabel, statusLabel);
        top.setPadding(new Insets(20));

        createColumns();

        details.setEditable(false);
        details.setWrapText(false);
        details.setPromptText("Select a class to view detailed analysis.");
        details.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, selected) -> {
                    if (selected != null) {
                        showDetails(selected);
                    }
                }
        );

        SplitPane split = new SplitPane(table, details);
        split.setDividerPositions(0.55);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(split);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: #f4f6fa;");

        stage.setTitle("Java Complexity Analyzer");
        stage.setScene(new Scene(root, 1100, 700));
        stage.show();
    }

    private void createColumns() {

        TableColumn<ClassAnalysisResult, String> name =
                new TableColumn<>("Class");

        name.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getClassName()));

        TableColumn<ClassAnalysisResult, Integer> loc =
                new TableColumn<>("LOC");

        loc.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getMetrics().getLinesOfCode()));

        TableColumn<ClassAnalysisResult, Integer> methods =
                new TableColumn<>("Methods");

        methods.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getMetrics().getMethodCount()));

        TableColumn<ClassAnalysisResult, Integer> complexity =
                new TableColumn<>("Complexity");

        complexity.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getMetrics().getTotalComplexity()));

        TableColumn<ClassAnalysisResult, Integer> coupling =
                new TableColumn<>("Coupling");

        coupling.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getDependency().getCoupling()));

        TableColumn<ClassAnalysisResult, Integer> risk =
                new TableColumn<>("Risk Score");

        risk.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getRiskScore()));

        TableColumn<ClassAnalysisResult, String> level =
                new TableColumn<>("Risk Level");

        level.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getRiskLevel()));

        table.getColumns().addAll(
                name, loc, methods, complexity, coupling, risk, level
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
    }

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

        if (selectedPath == null) {
            return;
        }

        Path projectPath = selectedPath;

        setBusy(true);
        statusLabel.setText("Analyzing project...");

        Task<ProjectAnalysisResult> task = new Task<>() {
            @Override
            protected ProjectAnalysisResult call() throws Exception {
                return new ProjectAnalyzer().analyze(projectPath);
            }
        };

        task.setOnSucceeded(e -> {

            ProjectAnalysisResult result = task.getValue();

            table.getItems().setAll(result.getClasses());
            updateSummary(result);

            details.clear();

            if (!result.getClasses().isEmpty()) {
                table.getSelectionModel().selectFirst();
            }

            statusLabel.setText("Analysis completed.");
            setBusy(false);
        });

        task.setOnFailed(e -> {

            Throwable error = task.getException();

            statusLabel.setText("Analysis failed.");
            setBusy(false);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Analysis Failed");
            alert.setHeaderText("Could not analyze this project");
            alert.setContentText(
                    error == null ? "Unknown error" : error.getMessage()
            );
            alert.showAndWait();
        });

        Thread thread = new Thread(task, "project-analysis");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {

        chooseButton.setDisable(busy);
        analyzeButton.setDisable(busy || selectedPath == null);
        progress.setVisible(busy);
        progress.setManaged(busy);
    }

    private void updateSummary(ProjectAnalysisResult result) {

        int methods = 0;
        int smells = 0;
        int riskyClasses = 0;

        for (ClassAnalysisResult clazz : result.getClasses()) {

            methods += clazz.getMethods().size();
            smells += clazz.getSmells().size();

            for (MethodAnalysisResult method : clazz.getMethods()) {
                smells += method.getSmells().size();
            }

            if (clazz.getRiskLevel().equals("HIGH")
                    || clazz.getRiskLevel().equals("CRITICAL")) {
                riskyClasses++;
            }
        }

        summaryLabel.setText(
                "Classes: " + result.getClasses().size()
                        + "    Methods: " + methods
                        + "    Code smells: " + smells
                        + "    High/Critical risk: " + riskyClasses
        );
    }

    private void showDetails(ClassAnalysisResult result) {

        StringBuilder out = new StringBuilder();
        var metrics = result.getMetrics();

        out.append("CLASS: ").append(result.getClassName()).append("\n");
        out.append("File: ").append(result.getSourceFile()).append("\n");
        out.append("Risk: ").append(result.getRiskLevel())
                .append(" (").append(result.getRiskScore()).append("/100)\n\n");

        out.append("CLASS METRICS\n");
        out.append("Lines: ").append(metrics.getLinesOfCode()).append("\n");
        out.append("Methods: ").append(metrics.getMethodCount()).append("\n");
        out.append("Fields: ").append(metrics.getFieldCount()).append("\n");
        out.append("Total complexity: ").append(metrics.getTotalComplexity()).append("\n");
        out.append("Average complexity: ").append(metrics.getAverageComplexity()).append("\n");
        out.append("Maximum nesting: ").append(metrics.getMaxNestingDepth()).append("\n");
        out.append("Coupling: ").append(result.getDependency().getCoupling()).append("\n");
        out.append("Dependencies: ").append(result.getDependency().getDependencies()).append("\n\n");

        appendSmells(out, "CLASS CODE SMELLS", result.getSmells());

        out.append("\nMETHOD DETAILS\n");

        for (MethodAnalysisResult method : result.getMethods()) {

            var m = method.getMetrics();

            out.append("\n").append(m.getMethodName()).append("\n");
            out.append("  LOC: ").append(m.getLinesOfCode()).append("\n");
            out.append("  Parameters: ").append(m.getParameterCount()).append("\n");
            out.append("  Statements: ").append(m.getStatementCount()).append("\n");
            out.append("  Complexity: ").append(m.getComplexity()).append("\n");
            out.append("  Nesting: ").append(m.getNestingDepth()).append("\n");

            appendSmells(out, "  Code smells", method.getSmells());
        }

        details.setText(out.toString());
    }

    private void appendSmells(
            StringBuilder out,
            String heading,
            List<CodeSmell> smells) {

        out.append(heading).append(":\n");

        if (smells.isEmpty()) {
            out.append("  None\n");
            return;
        }

        for (CodeSmell smell : smells) {
            out.append("  - ").append(smell.getType())
                    .append(" [").append(smell.getSeverity()).append("]\n");
            out.append("    ").append(smell.getMessage()).append("\n");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}