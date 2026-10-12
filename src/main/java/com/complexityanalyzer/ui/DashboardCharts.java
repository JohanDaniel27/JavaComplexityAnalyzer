
package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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

public class DashboardCharts {

    public Node create(ProjectAnalysisResult result) {

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("LOW", 0);
        counts.put("MEDIUM", 0);
        counts.put("HIGH", 0);
        counts.put("CRITICAL", 0);

        for (ClassAnalysisResult clazz : result.getClasses()) {
            String level = clazz.getRiskLevel();
            counts.merge(level, 1, Integer::sum);
        }

        ObservableList<PieChart.Data> data =
                FXCollections.observableArrayList();

        counts.forEach((level, count) ->
                data.add(new PieChart.Data(level, count)));

        PieChart riskChart = new PieChart(data);
        riskChart.setTitle("Class Risk Distribution");
        riskChart.setLabelsVisible(true);
        riskChart.setLegendVisible(true);
        riskChart.setMinWidth(350);
        riskChart.setPrefHeight(400);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Class");
        xAxis.setTickLabelRotation(25);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Cyclomatic Complexity");
        yAxis.setForceZeroInRange(true);

        BarChart<String, Number> complexityChart =
                new BarChart<>(xAxis, yAxis);

        complexityChart.setTitle("Top 10 Most Complex Classes");
        complexityChart.setLegendVisible(false);
        complexityChart.setAnimated(false);
        complexityChart.setMinWidth(500);
        complexityChart.setPrefHeight(400);

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        List<ClassAnalysisResult> topClasses =
                result.getClasses().stream()
                        .sorted(
                                Comparator.comparingInt(
                                        (ClassAnalysisResult c) ->
                                                c.getMetrics()
                                                        .getTotalComplexity()
                                ).reversed()
                        )
                        .limit(10)
                        .toList();

        for (ClassAnalysisResult clazz : topClasses) {
            series.getData().add(
                    new XYChart.Data<>(
                            clazz.getClassName(),
                            clazz.getMetrics().getTotalComplexity()
                    )
            );
        }

        complexityChart.getData().add(series);

        HBox charts = new HBox(15, riskChart, complexityChart);
        HBox.setHgrow(riskChart, Priority.ALWAYS);
        HBox.setHgrow(complexityChart, Priority.ALWAYS);
        charts.setPadding(new Insets(15));

        Label note = new Label(
                "Risk scores are project-defined heuristics. "
                        + "Review individual findings before drawing conclusions."
        );
        note.setWrapText(true);

        VBox content = new VBox(10, note, charts);
        content.setPadding(new Insets(15));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }
}
