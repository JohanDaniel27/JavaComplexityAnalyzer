package com.complexityanalyzer.ui;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.architecture.DependencyCycle;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;

import java.util.*;

/**
 * Draws the class dependency graph with:
 * - Modern dark node cards with risk-level borders
 * - Customizable zoom controls (slider, in/out buttons, fit to view, 100% actual size)
 * - Initial automatic fit-to-view so the entire graph and all connections are visible at first
 * - Circular-dependency edges highlighted in red
 * - Interactive panning and Ctrl+wheel zooming
 * - Tooltips on node hover with metrics
 */
public class DependencyGraphView {

    private static final double NODE_WIDTH  = 140;
    private static final double NODE_HEIGHT = 48;

    private static final double MIN_ZOOM = 0.05; // 5%
    private static final double MAX_ZOOM = 3.00; // 300%

    public Node create(List<ClassAnalysisResult> classes, List<DependencyCycle> cycles) {
        if (classes == null || classes.isEmpty()) {
            Label placeholder = new Label("No classes to display.");
            placeholder.setStyle("-fx-text-fill: #6e7681; -fx-font-size: 14px;");
            VBox box = new VBox(placeholder);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: #121317;");
            return box;
        }

        // Collect all class pairs that are part of a cycle
        Set<String> cycleEdges = new HashSet<>();
        if (cycles != null) {
            for (DependencyCycle cycle : cycles) {
                List<String> chain = cycle.getChain();
                for (int i = 0; i < chain.size() - 1; i++) {
                    cycleEdges.add(chain.get(i) + "->" + chain.get(i + 1));
                }
            }
        }

        int count = classes.size();
        double radius = count == 1 ? 0 : Math.max(200, count * 38);
        double width  = Math.max(900, radius * 2 + NODE_WIDTH + 140);
        double height = Math.max(700, radius * 2 + NODE_HEIGHT + 140);
        double cx = width / 2, cy = height / 2;

        Map<String, double[]> positions = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / count;
            positions.put(classes.get(i).getClassName(),
                    new double[]{cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)});
        }

        Pane canvas = new Pane();
        canvas.setPrefSize(width, height);
        canvas.setMinSize(width, height);
        canvas.setMaxSize(width, height);
        canvas.setStyle("-fx-background-color: #121317;");

        // 1. Draw dependency edges first (behind nodes)
        int connectionCount = 0;
        for (ClassAnalysisResult clazz : classes) {
            double[] src = positions.get(clazz.getClassName());
            for (String dep : clazz.getDependency().getDependencies()) {
                double[] tgt = positions.get(dep);
                if (src == null || tgt == null) continue;

                boolean isCycle = cycleEdges.contains(clazz.getClassName() + "->" + dep);
                drawArrow(canvas, src[0], src[1], tgt[0], tgt[1], isCycle);
                connectionCount++;
            }
        }

        // 2. Draw nodes on top
        for (ClassAnalysisResult clazz : classes) {
            double[] pt = positions.get(clazz.getClassName());
            Label node = new Label(clazz.getClassName());
            node.setAlignment(Pos.CENTER);
            node.setTextAlignment(TextAlignment.CENTER);
            node.setWrapText(true);
            node.setPrefSize(NODE_WIDTH, NODE_HEIGHT);
            node.setMinSize(NODE_WIDTH, NODE_HEIGHT);
            node.setMaxSize(NODE_WIDTH, NODE_HEIGHT);

            String borderColor = switch (clazz.getRiskLevel()) {
                case "CRITICAL" -> "#ef4444";
                case "HIGH" -> "#f97316";
                case "MEDIUM" -> "#f59e0b";
                default -> "#3b82f6";
            };

            node.setStyle(
                    "-fx-background-color: #1a1b24;" +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-weight: 600;" +
                    "-fx-font-size: 11px;" +
                    "-fx-padding: 4;"
            );
            node.setLayoutX(pt[0] - NODE_WIDTH / 2);
            node.setLayoutY(pt[1] - NODE_HEIGHT / 2);

            String tip = String.format("%s\nLOC: %d | Methods: %d | Risk: %s (%d)\nComplexity: %d\nDependencies (efferent): %d\nDepended on by (afferent): %d",
                    clazz.getClassName(),
                    clazz.getMetrics().getLinesOfCode(),
                    clazz.getMetrics().getMethodCount(),
                    clazz.getRiskLevel(), clazz.getRiskScore(),
                    clazz.getMetrics().getTotalComplexity(),
                    clazz.getDependency().getCoupling(),
                    clazz.getDependency().getAfferentCoupling());
            Tooltip.install(node, new Tooltip(tip));

            canvas.getChildren().add(node);
        }

        // 3. Zoom setup with Scale transform & Group
        Scale scaleTransform = new Scale(1.0, 1.0, 0, 0);
        canvas.getTransforms().setAll(scaleTransform);

        Group zoomGroup = new Group(canvas);
        StackPane centeredContainer = new StackPane(zoomGroup);
        centeredContainer.setAlignment(Pos.CENTER);
        centeredContainer.setStyle("-fx-background-color: #121317;");

        ScrollPane scrollPane = new ScrollPane(centeredContainer);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #121317;");

        // 4. Zoom UI Controls
        Slider zoomSlider = new Slider(MIN_ZOOM, MAX_ZOOM, 1.0);
        zoomSlider.setPrefWidth(140);
        zoomSlider.setBlockIncrement(0.05);

        Label zoomPercentLabel = new Label("100%");
        zoomPercentLabel.setMinWidth(44);
        zoomPercentLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: 600; -fx-text-fill: #e6edf3;");

        Button zoomOutBtn = new Button("−");
        zoomOutBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 28px;");

        Button zoomInBtn = new Button("+");
        zoomInBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 28px;");

        Button fitBtn = new Button("Fit to View");
        fitBtn.setStyle("-fx-font-weight: 600;");

        Button actualSizeBtn = new Button("100%");

        // Hook up slider to transform
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double scale = newVal.doubleValue();
            scaleTransform.setX(scale);
            scaleTransform.setY(scale);
            zoomPercentLabel.setText(String.format("%d%%", (int) Math.round(scale * 100)));
            centeredContainer.requestLayout();
        });

        zoomOutBtn.setOnAction(e -> {
            double current = zoomSlider.getValue();
            zoomSlider.setValue(Math.max(MIN_ZOOM, current * 0.85));
        });

        zoomInBtn.setOnAction(e -> {
            double current = zoomSlider.getValue();
            zoomSlider.setValue(Math.min(MAX_ZOOM, current * 1.15));
        });

        actualSizeBtn.setOnAction(e -> {
            zoomSlider.setValue(1.0);
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
        });

        final double finalWidth = width;
        final double finalHeight = height;

        fitBtn.setOnAction(e -> fitToView(scrollPane, zoomSlider, finalWidth, finalHeight));

        // Ctrl + Mouse Wheel zoom
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown() || event.isAltDown()) {
                event.consume();
                double delta = event.getDeltaY();
                if (delta != 0) {
                    double zoomFactor = delta > 0 ? 1.15 : 0.85;
                    double newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomSlider.getValue() * zoomFactor));
                    zoomSlider.setValue(newScale);
                }
            }
        });

        // 5. Initial Full Zoom-Out
        double estimatedVw = 1050;
        double estimatedVh = 600;
        double estScale = Math.min((estimatedVw - 40) / width, (estimatedVh - 40) / height);
        estScale = Math.max(MIN_ZOOM, Math.min(estScale, 1.0));
        zoomSlider.setValue(estScale);

        final boolean[] initialFitDone = {false};
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (!initialFitDone[0] && newBounds.getWidth() > 50 && newBounds.getHeight() > 50) {
                initialFitDone[0] = true;
                Platform.runLater(() -> fitToView(scrollPane, zoomSlider, finalWidth, finalHeight));
            }
        });

        // 6. Build Toolbar and Container
        Label zoomLabel = new Label("Zoom:");
        zoomLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #8b949e;");

        Label tipLabel = new Label("Tip: Ctrl + Mouse Wheel to zoom | Drag to pan");
        tipLabel.setStyle("-fx-text-fill: #6e7681; -fx-font-size: 11px;");
        HBox.setHgrow(tipLabel, Priority.ALWAYS);
        tipLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox toolbar = new HBox(8, zoomLabel, zoomOutBtn, zoomSlider, zoomPercentLabel, zoomInBtn,
                fitBtn, actualSizeBtn, new Separator(javafx.geometry.Orientation.VERTICAL), tipLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 14, 8, 14));
        toolbar.setStyle("-fx-background-color: #16171e; -fx-border-color: #262833; -fx-border-width: 0 0 1 0;");

        // Bottom status / legend
        Label legend = new Label(String.format(
                "Classes: %d  |  Connections: %d  |  Cycles: %d   •   Red arrows = Circular dependency   •   Muted arrows = Normal dependency",
                count, connectionCount, (cycles == null ? 0 : cycles.size())));
        legend.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e;");

        BorderPane container = new BorderPane();
        container.setTop(toolbar);
        container.setCenter(scrollPane);
        container.setBottom(legend);
        container.setStyle("-fx-background-color: #121317;");
        BorderPane.setMargin(legend, new Insets(8, 14, 8, 14));

        // Keyboard shortcuts (+ / - / 0)
        container.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.EQUALS) {
                    zoomInBtn.fire();
                    event.consume();
                } else if (event.getCode() == KeyCode.MINUS) {
                    zoomOutBtn.fire();
                    event.consume();
                } else if (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0) {
                    actualSizeBtn.fire();
                    event.consume();
                }
            }
        });

        return container;
    }

    private void fitToView(ScrollPane scrollPane, Slider zoomSlider, double canvasWidth, double canvasHeight) {
        double vw = scrollPane.getViewportBounds().getWidth();
        double vh = scrollPane.getViewportBounds().getHeight();

        if (vw <= 50) vw = 1050;
        if (vh <= 50) vh = 600;

        double margin = 40.0;
        double scaleX = (vw - margin) / canvasWidth;
        double scaleY = (vh - margin) / canvasHeight;
        double fitScale = Math.min(scaleX, scaleY);

        fitScale = Math.max(MIN_ZOOM, Math.min(fitScale, MAX_ZOOM));
        zoomSlider.setValue(fitScale);

        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
    }

    /** Backward-compatible overload without cycles. */
    public Node create(List<ClassAnalysisResult> classes) {
        return create(classes, List.of());
    }

    private void drawArrow(Pane canvas, double x1, double y1, double x2, double y2, boolean isCycle) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;

        // Adjust endpoints to node borders
        double ux = dx / len, uy = dy / len;
        double startX = x1 + ux * (NODE_WIDTH / 2);
        double startY = y1 + uy * (NODE_HEIGHT / 2);
        double endX   = x2 - ux * (NODE_WIDTH / 2 + 6);
        double endY   = y2 - uy * (NODE_HEIGHT / 2 + 6);

        Color lineColor = isCycle ? Color.web("#ef4444") : Color.web("#475569");
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(lineColor);
        line.setStrokeWidth(isCycle ? 2.2 : 1.2);

        // Arrow head
        double angle = Math.atan2(endY - startY, endX - startX);
        double arrowLen = 10;
        double arrowAngle = Math.PI / 7;
        double ax1 = endX - arrowLen * Math.cos(angle - arrowAngle);
        double ay1 = endY - arrowLen * Math.sin(angle - arrowAngle);
        double ax2 = endX - arrowLen * Math.cos(angle + arrowAngle);
        double ay2 = endY - arrowLen * Math.sin(angle + arrowAngle);

        Polygon arrowHead = new Polygon(endX, endY, ax1, ay1, ax2, ay2);
        arrowHead.setFill(lineColor);

        canvas.getChildren().addAll(line, arrowHead);
    }
}
