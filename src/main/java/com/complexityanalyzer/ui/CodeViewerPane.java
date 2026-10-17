package com.complexityanalyzer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A read-only JavaFX source code viewer with line numbers and line-range highlighting.
 * Call {@link #loadFile(Path)} to display a file, then {@link #highlightRange(int, int)}
 * and {@link #scrollToLine(int)} to navigate to a finding.
 */
public class CodeViewerPane {

    private static final Font CODE_FONT = Font.font("Monospaced", 13);
    private static final String HIGHLIGHT_STYLE = "-fx-background-color: #fff9c4;";
    private static final String NORMAL_STYLE    = "-fx-background-color: transparent;";

    private final VBox lineContainer = new VBox(0);
    private final ScrollPane scroll  = new ScrollPane(lineContainer);

    private final List<HBox> lineRows = new ArrayList<>();
    private int totalLines = 0;

    public CodeViewerPane() {
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #fafafa;");
        lineContainer.setStyle("-fx-background-color: #fafafa;");
        lineContainer.setPadding(new Insets(8));
    }

    /** Returns the root JavaFX node to embed in a scene. */
    public ScrollPane getNode() { return scroll; }

    /**
     * Loads and displays the given source file.
     * Does nothing if the file cannot be read.
     */
    public void loadFile(Path filePath) {
        lineContainer.getChildren().clear();
        lineRows.clear();
        totalLines = 0;

        if (filePath == null || !filePath.toFile().exists()) {
            Label placeholder = new Label("Source file not available: " +
                    (filePath != null ? filePath.getFileName() : "null"));
            placeholder.setStyle("-fx-text-fill: #888;");
            lineContainer.getChildren().add(placeholder);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            totalLines = lines.size();
            int lineNumWidth = Math.max(3, String.valueOf(totalLines).length());

            for (int i = 0; i < lines.size(); i++) {
                int lineNum = i + 1;

                Label gutterLabel = new Label(String.format("%" + lineNumWidth + "d", lineNum));
                gutterLabel.setFont(CODE_FONT);
                gutterLabel.setTextFill(Color.web("#9e9e9e"));
                gutterLabel.setMinWidth(lineNumWidth * 9 + 12);
                gutterLabel.setPadding(new Insets(0, 8, 0, 4));

                Text codeText = new Text(lines.get(i));
                codeText.setFont(CODE_FONT);
                codeText.setFill(Color.web("#212121"));

                TextFlow textFlow = new TextFlow(codeText);
                textFlow.setPrefWidth(Double.MAX_VALUE);

                HBox row = new HBox(gutterLabel, textFlow);
                row.setStyle(NORMAL_STYLE);
                row.setPadding(new Insets(0, 4, 0, 0));

                lineRows.add(row);
                lineContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            Label err = new Label("Error reading file: " + e.getMessage());
            err.setStyle("-fx-text-fill: red;");
            lineContainer.getChildren().add(err);
        }
    }

    /**
     * Highlights a range of lines (1-indexed, inclusive).
     * Clears any previous highlight first.
     */
    public void highlightRange(int startLine, int endLine) {
        // Clear previous highlights
        for (HBox row : lineRows) {
            row.setStyle(NORMAL_STYLE);
        }
        // Apply new highlights
        for (int l = startLine; l <= endLine && l <= lineRows.size(); l++) {
            lineRows.get(l - 1).setStyle(HIGHLIGHT_STYLE);
        }
    }

    /**
     * Scrolls so that the given line (1-indexed) is visible.
     */
    public void scrollToLine(int line) {
        if (totalLines <= 1 || line <= 0) return;
        double fraction = (double) (Math.max(1, line) - 1) / Math.max(1, totalLines - 1);
        scroll.setVvalue(Math.min(1.0, Math.max(0.0, fraction)));
    }

    /** Convenience: highlight and scroll to a range. */
    public void navigateTo(int startLine, int endLine) {
        highlightRange(startLine, endLine);
        scrollToLine(startLine);
    }
}
