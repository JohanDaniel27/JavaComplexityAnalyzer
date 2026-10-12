
package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.ClassAnalysisResult;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyGraphView {

    private static final double NODE_WIDTH = 150;
    private static final double NODE_HEIGHT = 55;

    public Node create(List<ClassAnalysisResult> classes) {

        Pane canvas = new Pane();

        if (classes.isEmpty()) {
            return new Label("No classes to display.");
        }

        int count = classes.size();

        double radius = count == 1 ? 0 : Math.max(180, count * 35);
        double width = Math.max(900, radius * 2 + NODE_WIDTH + 100);
        double height = Math.max(650, radius * 2 + NODE_HEIGHT + 100);

        double centerX = width / 2;
        double centerY = height / 2;

        Map<String, Point> positions = new HashMap<>();

        for (int i = 0; i < count; i++) {

            double angle = -Math.PI / 2 + 2 * Math.PI * i / count;

            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            positions.put(
                    classes.get(i).getClassName(),
                    new Point(x, y)
            );
        }

        canvas.setPrefSize(width, height);
        canvas.setMinSize(width, height);

        // Draw dependency arrows first so they appear behind the nodes.
        for (ClassAnalysisResult clazz : classes) {

            Point source = positions.get(clazz.getClassName());

            for (String dependency :
                    clazz.getDependency().getDependencies()) {

                Point target = positions.get(dependency);

                if (target != null && source != null) {
                    drawArrow(canvas, source, target);
                }
            }
        }

        // Draw class nodes over the arrows.
        for (ClassAnalysisResult clazz : classes) {

            Point point = positions.get(clazz.getClassName());

            Label node = new Label(clazz.getClassName());
            node.setAlignment(Pos.CENTER);
            node.setTextAlignment(TextAlignment.CENTER);
            node.setWrapText(true);
            node.setPrefSize(NODE_WIDTH, NODE_HEIGHT);
            node.setMinSize(NODE_WIDTH, NODE_HEIGHT);

            node.setStyle(
                    "-fx-background-color: #E8F0FE;" +
                            "-fx-border-color: #5275A5;" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 5;"
            );

            node.setLayoutX(point.x - NODE_WIDTH / 2);
            node.setLayoutY(point.y - NODE_HEIGHT / 2);

            canvas.getChildren().add(node);
        }

        Label legend = new Label(
                "An arrow A → B means class A depends on class B."
        );
        legend.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(legend);
        root.setCenter(canvas);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setPannable(true);

        return scroll;
    }

    private void drawArrow(Pane canvas, Point source, Point target) {

        Point start = borderPoint(source, target);
        Point end = borderPoint(target, source);

        Line line = new Line(start.x, start.y, end.x, end.y);
        line.setStroke(Color.GRAY);
        line.setStrokeWidth(1.5);

        canvas.getChildren().add(line);

        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double length = Math.hypot(dx, dy);

        if (length == 0) {
            return;
        }

        double ux = dx / length;
        double uy = dy / length;

        double arrowLength = 11;
        double arrowWidth = 7;

        double baseX = end.x - arrowLength * ux;
        double baseY = end.y - arrowLength * uy;

        Polygon arrow = new Polygon(
                end.x, end.y,
                baseX - arrowWidth * uy,
                baseY + arrowWidth * ux,
                baseX + arrowWidth * uy,
                baseY - arrowWidth * ux
        );

        arrow.setFill(Color.GRAY);
        canvas.getChildren().add(arrow);
    }

    private Point borderPoint(Point from, Point toward) {

        double dx = toward.x - from.x;
        double dy = toward.y - from.y;

        double scale = 1 / Math.max(
                Math.abs(dx) / (NODE_WIDTH / 2),
                Math.abs(dy) / (NODE_HEIGHT / 2)
        );

        return new Point(
                from.x + dx * scale,
                from.y + dy * scale
        );
    }

    private static class Point {

        private final double x;
        private final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
