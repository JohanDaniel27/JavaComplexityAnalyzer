package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.qualitygate.QualityGateResult;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the Overview dashboard with modern dark KPI cards, charts, and hotspot list. */
public class DashboardCharts {

    public Node create(ProjectAnalysisResult result) {
        return create(result, null);
    }

    public Node create(ProjectAnalysisResult result, QualityGateResult gateResult) {

        // ---- KPI cards ----
        int totalMethods = result.getClasses().stream().mapToInt(c -> c.getMethods().size()).sum();
        int totalSmells  = result.getClasses().stream()
                .mapToInt(c -> c.getSmells().size() +
                               c.getMethods().stream().mapToInt(m -> m.getSmells().size()).sum())
                .sum();
        int totalNcloc = result.getClasses().stream().mapToInt(c -> c.getMetrics().getNcloc()).sum();
        int cycles = result.getCycles().size();
        int clones = result.getClones().size();

        HBox kpiRow = new HBox(12);
        kpiRow.setAlignment(Pos.CENTER_LEFT);
        kpiRow.setPadding(new Insets(4, 0, 14, 0));
        kpiRow.getChildren().addAll(
                kpiCard("CLASSES",  String.valueOf(result.getClasses().size()), "#6366f1"),
                kpiCard("METHODS",  String.valueOf(totalMethods), "#38bdf8"),
                kpiCard("NCLOC",    String.valueOf(totalNcloc), "#a855f7"),
                kpiCard("SMELLS",   String.valueOf(totalSmells), totalSmells > 0 ? "#f59e0b" : "#10b981"),
                kpiCard("CYCLES",   String.valueOf(cycles), cycles > 0 ? "#ef4444" : "#10b981"),
                kpiCard("CLONES",   String.valueOf(clones), clones > 0 ? "#f59e0b" : "#10b981")
        );

        // Quality gate card
        if (gateResult != null) {
            String gateColor = gateResult.isPassed() ? "#10b981" : "#ef4444";
            String gateStatus = gateResult.isPassed() ? "PASSED" : "FAILED (" + gateResult.getViolationCount() + ")";
            kpiRow.getChildren().add(kpiCard("QUALITY GATE", gateStatus, gateColor));
        }

        // ---- Risk distribution pie ----
        Map<String, Integer> riskCounts = new LinkedHashMap<>();
        riskCounts.put("LOW", 0);
        riskCounts.put("MEDIUM", 0);
        riskCounts.put("HIGH", 0);
        riskCounts.put("CRITICAL", 0);
        for (ClassAnalysisResult c : result.getClasses()) {
            riskCounts.merge(c.getRiskLevel(), 1, Integer::sum);
        }
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        riskCounts.forEach((level, count) -> {
            if (count > 0) pieData.add(new PieChart.Data(level + " (" + count + ")", count));
        });
        PieChart riskChart = new PieChart(pieData);
        riskChart.setTitle("Class Risk Distribution");
        riskChart.setLabelsVisible(true);
        riskChart.setLegendVisible(true);
        riskChart.setMinWidth(330);
        riskChart.setPrefHeight(340);

        // Apply slice colors dynamically
        for (PieChart.Data data : pieData) {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) applySliceColor(data, newNode);
            });
            if (data.getNode() != null) {
                applySliceColor(data, data.getNode());
            }
        }

        // ---- Top 10 Cyclomatic Complexity bar chart ----
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Class");
        xAxis.setTickLabelRotation(30);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cyclomatic Complexity");
        yAxis.setForceZeroInRange(true);
        BarChart<String, Number> complexityChart = new BarChart<>(xAxis, yAxis);
        complexityChart.setTitle("Top 10 Most Complex Classes");
        complexityChart.setLegendVisible(false);
        complexityChart.setAnimated(false);
        complexityChart.setMinWidth(460);
        complexityChart.setPrefHeight(340);
        XYChart.Series<String, Number> cyclSeries = new XYChart.Series<>();
        result.getClasses().stream()
                .sorted(Comparator.comparingInt((ClassAnalysisResult c) -> c.getMetrics().getTotalComplexity()).reversed())
                .limit(10)
                .forEach(c -> cyclSeries.getData().add(
                        new XYChart.Data<>(c.getClassName(), c.getMetrics().getTotalComplexity())));
        complexityChart.getData().add(cyclSeries);

        // ---- Cognitive Complexity bar chart ----
        CategoryAxis cogXAxis = new CategoryAxis();
        cogXAxis.setLabel("Class");
        cogXAxis.setTickLabelRotation(30);
        NumberAxis cogYAxis = new NumberAxis();
        cogYAxis.setLabel("Cognitive Complexity");
        cogYAxis.setForceZeroInRange(true);
        BarChart<String, Number> cogChart = new BarChart<>(cogXAxis, cogYAxis);
        cogChart.setTitle("Top 10 Cognitive Complexity");
        cogChart.setLegendVisible(false);
        cogChart.setAnimated(false);
        cogChart.setMinWidth(460);
        cogChart.setPrefHeight(340);
        XYChart.Series<String, Number> cogSeries = new XYChart.Series<>();
        result.getClasses().stream()
                .sorted(Comparator.comparingInt((ClassAnalysisResult c) ->
                        c.getMetrics().getTotalCognitiveComplexity()).reversed())
                .limit(10)
                .forEach(c -> cogSeries.getData().add(
                        new XYChart.Data<>(c.getClassName(), c.getMetrics().getTotalCognitiveComplexity())));
        cogChart.getData().add(cogSeries);

        HBox charts = new HBox(16, riskChart, complexityChart, cogChart);
        HBox.setHgrow(riskChart, Priority.ALWAYS);
        HBox.setHgrow(complexityChart, Priority.ALWAYS);
        HBox.setHgrow(cogChart, Priority.ALWAYS);
        charts.setPadding(new Insets(10, 0, 10, 0));

        // ---- Hotspot list ----
        VBox hotspotBox = new VBox(8);
        hotspotBox.setStyle("-fx-background-color: #16171e; -fx-border-color: #262833; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14;");
        if (!result.getHotspots().isEmpty()) {
            Label hsTitle = new Label("Top Maintenance Hotspots");
            hsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #e6edf3;");
            hotspotBox.getChildren().add(hsTitle);
            result.getHotspots().stream().limit(8).forEach(h -> {
                String tierColor = switch (h.getTier()) {
                    case CRITICAL_HOTSPOT -> "#ef4444";
                    case HIGH_HOTSPOT -> "#f97316";
                    case MODERATE -> "#f59e0b";
                    default -> "#10b981";
                };

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(3, 0, 3, 0));

                Label tierBadge = new Label(h.getTier().toString());
                tierBadge.setStyle(String.format("-fx-background-color: %s22; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px; -fx-font-weight: bold;",
                        tierColor, tierColor, tierColor));

                Label classLabel = new Label(h.getClassName());
                classLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-family: monospace; -fx-font-weight: 500;");

                Label statsLabel = new Label(String.format("Score: %.2f  |  Commits: %d  |  Churn: %d",
                        h.getHotspotScore(), h.getCommitCount(), h.getTotalChurn()));
                statsLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

                row.getChildren().addAll(tierBadge, classLabel, statsLabel);
                hotspotBox.getChildren().add(row);
            });
        }

        // ---- Cycle list ----
        VBox cycleBox = new VBox(6);
        if (!result.getCycles().isEmpty()) {
            cycleBox.setStyle("-fx-background-color: #1f1618; -fx-border-color: #ef444444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14;");
            Label cycleTitle = new Label("Circular Dependencies Detected");
            cycleTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ef4444;");
            cycleBox.getChildren().add(cycleTitle);
            result.getCycles().forEach(cycle -> {
                Label cl = new Label("  " + cycle);
                cl.setStyle("-fx-font-family: monospace; -fx-text-fill: #fca5a5;");
                cycleBox.getChildren().add(cl);
            });
        }

        Label note = new Label(
                "Risk scores are project-defined heuristics. " +
                "Hotspot score = 0.5 × Norm(commits) + 0.5 × Norm(complexity).");
        note.setStyle("-fx-text-fill: #6e7681; -fx-font-size: 11px;");

        VBox content = new VBox(14, kpiRow, charts, hotspotBox, cycleBox, note);
        content.setPadding(new Insets(18));
        content.setStyle("-fx-background-color: #121317;");

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #121317;");
        return sp;
    }

    private void applySliceColor(PieChart.Data data, Node node) {
        String name = data.getName();
        String color = "#10b981";
        if (name.startsWith("CRITICAL")) color = "#ef4444";
        else if (name.startsWith("HIGH")) color = "#f97316";
        else if (name.startsWith("MEDIUM")) color = "#f59e0b";
        node.setStyle("-fx-pie-color: " + color + ";");
    }

    private VBox kpiCard(String title, String value, String accentColor) {
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #8b949e;");

        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.setMinWidth(130);
        card.setStyle(
                "-fx-background-color: #1a1b23;" +
                "-fx-border-color: #262833;" +
                "-fx-border-width: 1 1 1 3;" +
                "-fx-border-color: #262833 #262833 #262833 " + accentColor + ";" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return card;
    }
}
