package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.git.HotspotResult;
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
import java.util.List;
import java.util.Map;

/** Builds the Overview dashboard with KPI cards, charts, and hotspot list. */
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

        HBox kpiRow = new HBox(10);
        kpiRow.setAlignment(Pos.CENTER_LEFT);
        kpiRow.setPadding(new Insets(0, 0, 10, 0));
        kpiRow.getChildren().addAll(
                kpiCard("Classes",  String.valueOf(result.getClasses().size()), "#3498db"),
                kpiCard("Methods",  String.valueOf(totalMethods), "#2ecc71"),
                kpiCard("NCLOC",    String.valueOf(totalNcloc), "#9b59b6"),
                kpiCard("Smells",   String.valueOf(totalSmells), "#e67e22"),
                kpiCard("Cycles",   String.valueOf(cycles), cycles > 0 ? "#e74c3c" : "#27ae60"),
                kpiCard("Clones",   String.valueOf(clones), clones > 0 ? "#e67e22" : "#27ae60")
        );

        // Quality gate badge
        if (gateResult != null) {
            String label = gateResult.isPassed() ? "✅ Quality Gate: PASSED" : "❌ Quality Gate: FAILED";
            String color = gateResult.isPassed() ? "#27ae60" : "#e74c3c";
            kpiRow.getChildren().add(kpiCard("Quality Gate",
                    gateResult.isPassed() ? "PASSED" : "FAILED (" + gateResult.getViolationCount() + ")", color));
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
        riskChart.setMinWidth(320);
        riskChart.setPrefHeight(350);

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
        complexityChart.setMinWidth(480);
        complexityChart.setPrefHeight(350);
        XYChart.Series<String, Number> cyclSeries = new XYChart.Series<>();
        result.getClasses().stream()
                .sorted(Comparator.comparingInt((ClassAnalysisResult c) -> c.getMetrics().getTotalComplexity()).reversed())
                .limit(10)
                .forEach(c -> cyclSeries.getData().add(
                        new XYChart.Data<>(c.getClassName(), c.getMetrics().getTotalComplexity())));
        complexityChart.getData().add(cyclSeries);

        // ---- Cognitive Complexity bar chart ----
        CategoryAxis cogXAxis = new CategoryAxis();
        cogXAxis.setTickLabelRotation(30);
        NumberAxis cogYAxis = new NumberAxis();
        cogYAxis.setLabel("Cognitive Complexity");
        cogYAxis.setForceZeroInRange(true);
        BarChart<String, Number> cogChart = new BarChart<>(cogXAxis, cogYAxis);
        cogChart.setTitle("Top 10 Cognitive Complexity");
        cogChart.setLegendVisible(false);
        cogChart.setAnimated(false);
        cogChart.setMinWidth(480);
        cogChart.setPrefHeight(350);
        XYChart.Series<String, Number> cogSeries = new XYChart.Series<>();
        result.getClasses().stream()
                .sorted(Comparator.comparingInt((ClassAnalysisResult c) ->
                        c.getMetrics().getTotalCognitiveComplexity()).reversed())
                .limit(10)
                .forEach(c -> cogSeries.getData().add(
                        new XYChart.Data<>(c.getClassName(), c.getMetrics().getTotalCognitiveComplexity())));
        cogChart.getData().add(cogSeries);

        HBox charts = new HBox(15, riskChart, complexityChart, cogChart);
        HBox.setHgrow(riskChart, Priority.ALWAYS);
        HBox.setHgrow(complexityChart, Priority.ALWAYS);
        HBox.setHgrow(cogChart, Priority.ALWAYS);
        charts.setPadding(new Insets(10, 0, 10, 0));

        // ---- Hotspot list ----
        VBox hotspotBox = new VBox(6);
        if (!result.getHotspots().isEmpty()) {
            Label hsTitle = new Label("🔥 Top Maintenance Hotspots");
            hsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            hotspotBox.getChildren().add(hsTitle);
            result.getHotspots().stream().limit(8).forEach(h -> {
                String tierColor = switch (h.getTier()) {
                    case CRITICAL_HOTSPOT -> "#e74c3c";
                    case HIGH_HOTSPOT -> "#e67e22";
                    case MODERATE -> "#f39c12";
                    default -> "#27ae60";
                };
                Label hl = new Label(String.format("%-35s  score=%.2f  commits=%d  [%s]",
                        h.getClassName(), h.getHotspotScore(), h.getCommitCount(), h.getTier()));
                hl.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-text-fill: " + tierColor + ";");
                hotspotBox.getChildren().add(hl);
            });
        }

        // ---- Cycle list ----
        VBox cycleBox = new VBox(4);
        if (!result.getCycles().isEmpty()) {
            Label cycleTitle = new Label("⚠ Circular Dependencies");
            cycleTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #e74c3c;");
            cycleBox.getChildren().add(cycleTitle);
            result.getCycles().forEach(cycle -> {
                Label cl = new Label("  " + cycle);
                cl.setStyle("-fx-font-family: monospace; -fx-text-fill: #e74c3c;");
                cycleBox.getChildren().add(cl);
            });
        }

        Label note = new Label(
                "Risk scores are project-defined heuristics. " +
                "Hotspot score = 0.5 × Norm(commits) + 0.5 × Norm(complexity).");
        note.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        VBox content = new VBox(12, kpiRow, charts, hotspotBox, cycleBox, note);
        content.setPadding(new Insets(16));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        return sp;
    }

    private VBox kpiCard(String title, String value, String color) {
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");
        VBox card = new VBox(2, valueLabel, titleLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 18, 10, 18));
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
        return card;
    }
}
