package com.complexityanalyzer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A sleek, dark-themed read-only Java source code viewer with line numbers,
 * Java syntax highlighting, and line-range highlighting for code smells.
 */
public class CodeViewerPane {

    private static final Font CODE_FONT = Font.font("Consolas", 12.5);

    private static final String NORMAL_STYLE =
            "-fx-background-color: transparent;";

    private static final String HIGHLIGHT_STYLE =
            "-fx-background-color: rgba(99, 102, 241, 0.18); -fx-border-color: #6366f1; -fx-border-width: 0 0 0 3;";

    // Syntax colors
    private static final Color COLOR_KEYWORD    = Color.web("#c678dd");
    private static final Color COLOR_TYPE       = Color.web("#4ec9b0");
    private static final Color COLOR_STRING     = Color.web("#ce9178");
    private static final Color COLOR_COMMENT    = Color.web("#6a9955");
    private static final Color COLOR_ANNOTATION = Color.web("#dcdcaa");
    private static final Color COLOR_NUMBER     = Color.web("#b5cea8");
    private static final Color COLOR_DEFAULT    = Color.web("#d4d4d4");

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "record", "sealed", "permits",
            "var", "yield", "true", "false", "null"
    );

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(//.*$)|(/\\*.*?\\*/)|(\"(?:\\\\\"|[^\"])*\"|'(?:\\\\'|[^'])*')|(@[a-zA-Z_][a-zA-Z0-9_]*)|(\\b\\d+(?:\\.\\d+)?[fFdDlL]?\\b)|(\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)|([^a-zA-Z0-9_\\s\"'@/]+)|(\\s+)",
            Pattern.DOTALL
    );

    private final VBox lineContainer = new VBox(0);
    private final ScrollPane scroll  = new ScrollPane(lineContainer);
    private final List<HBox> lineRows = new ArrayList<>();
    private int totalLines = 0;

    public CodeViewerPane() {
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #14151b; -fx-border-color: #262833; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        lineContainer.setStyle("-fx-background-color: #14151b;");
        lineContainer.setPadding(new Insets(6, 0, 6, 0));
    }

    /** Returns the root JavaFX node to embed in a scene. */
    public ScrollPane getNode() { return scroll; }

    /**
     * Loads and displays the given source file with syntax highlighting.
     */
    public void loadFile(Path filePath) {
        lineContainer.getChildren().clear();
        lineRows.clear();
        totalLines = 0;

        if (filePath == null || !filePath.toFile().exists()) {
            Label placeholder = new Label("Source file not available: " +
                    (filePath != null ? filePath.getFileName() : "null"));
            placeholder.setStyle("-fx-text-fill: #6e7681; -fx-font-size: 13px; -fx-padding: 16;");
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
                gutterLabel.setTextFill(Color.web("#525866"));
                gutterLabel.setMinWidth(lineNumWidth * 9 + 18);
                gutterLabel.setAlignment(Pos.CENTER_RIGHT);
                gutterLabel.setPadding(new Insets(1, 10, 1, 4));
                gutterLabel.setStyle("-fx-border-color: #22242f; -fx-border-width: 0 1 0 0; -fx-background-color: #16171f;");

                TextFlow textFlow = buildHighlightedLine(lines.get(i));
                textFlow.setPrefWidth(Double.MAX_VALUE);
                textFlow.setPadding(new Insets(1, 6, 1, 10));
                HBox.setHgrow(textFlow, Priority.ALWAYS);

                HBox row = new HBox(gutterLabel, textFlow);
                row.setStyle(NORMAL_STYLE);
                row.setAlignment(Pos.CENTER_LEFT);

                lineRows.add(row);
                lineContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            Label err = new Label("Error reading file: " + e.getMessage());
            err.setStyle("-fx-text-fill: #ef4444; -fx-padding: 12;");
            lineContainer.getChildren().add(err);
        }
    }

    private TextFlow buildHighlightedLine(String line) {
        TextFlow flow = new TextFlow();
        if (line.isEmpty()) {
            Text empty = new Text(" ");
            empty.setFont(CODE_FONT);
            flow.getChildren().add(empty);
            return flow;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(line);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                Text unparsed = new Text(line.substring(lastEnd, matcher.start()));
                unparsed.setFont(CODE_FONT);
                unparsed.setFill(COLOR_DEFAULT);
                flow.getChildren().add(unparsed);
            }

            String token = matcher.group();
            Text text = new Text(token);
            text.setFont(CODE_FONT);

            if (matcher.group(1) != null || matcher.group(2) != null) {
                // Comments
                text.setFill(COLOR_COMMENT);
            } else if (matcher.group(3) != null) {
                // Strings
                text.setFill(COLOR_STRING);
            } else if (matcher.group(4) != null) {
                // Annotations
                text.setFill(COLOR_ANNOTATION);
            } else if (matcher.group(5) != null) {
                // Numbers
                text.setFill(COLOR_NUMBER);
            } else if (matcher.group(6) != null) {
                // Words: check if keyword or type
                if (KEYWORDS.contains(token)) {
                    text.setFill(COLOR_KEYWORD);
                } else if (Character.isUpperCase(token.charAt(0))) {
                    text.setFill(COLOR_TYPE);
                } else {
                    text.setFill(COLOR_DEFAULT);
                }
            } else {
                text.setFill(COLOR_DEFAULT);
            }

            flow.getChildren().add(text);
            lastEnd = matcher.end();
        }

        if (lastEnd < line.length()) {
            Text remaining = new Text(line.substring(lastEnd));
            remaining.setFont(CODE_FONT);
            remaining.setFill(COLOR_DEFAULT);
            flow.getChildren().add(remaining);
        }

        return flow;
    }

    /**
     * Highlights a range of lines (1-indexed, inclusive).
     * Clears any previous highlight first.
     */
    public void highlightRange(int startLine, int endLine) {
        for (HBox row : lineRows) {
            row.setStyle(NORMAL_STYLE);
        }
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
