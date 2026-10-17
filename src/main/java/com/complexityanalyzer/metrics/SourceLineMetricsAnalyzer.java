package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;

import java.util.HashSet;
import java.util.Set;

/** Decomposes a node's line range into NCLOC, CLOC, and blank lines. */
public class SourceLineMetricsAnalyzer {

    public record LineBreakdown(int totalLines, int ncloc, int cloc, int blankLines) {}

    /**
     * Analyses lines for the given AST node using its contained comments.
     * totalLines = ncloc + cloc + blankLines (approximately, as comment lines and code lines can overlap).
     */
    public LineBreakdown analyze(Node node, String[] fileLines) {
        if (node.getRange().isEmpty()) {
            return new LineBreakdown(0, 0, 0, 0);
        }

        int startLine = node.getRange().get().begin.line;
        int endLine   = node.getRange().get().end.line;
        int totalLines = endLine - startLine + 1;

        // Collect all lines covered by comments within this node
        Set<Integer> commentLines = new HashSet<>();
        for (Comment comment : node.getAllContainedComments()) {
            if (comment.getRange().isEmpty()) continue;
            int cs = comment.getRange().get().begin.line;
            int ce = comment.getRange().get().end.line;
            for (int l = cs; l <= ce; l++) {
                commentLines.add(l);
            }
        }

        int blankLines = 0;
        int codeLines  = 0;
        int commentOnlyLines = 0;

        for (int l = startLine; l <= endLine; l++) {
            // fileLines is 0-indexed
            String raw = (l - 1 < fileLines.length) ? fileLines[l - 1].strip() : "";
            if (raw.isEmpty()) {
                blankLines++;
            } else if (commentLines.contains(l)) {
                // Check if the line also has code (inline comment)
                // Simple heuristic: if the trimmed line starts with // or /* or *, treat as comment-only
                if (raw.startsWith("//") || raw.startsWith("/*") || raw.startsWith("*")) {
                    commentOnlyLines++;
                } else {
                    // Inline comment on a code line - counts as code
                    codeLines++;
                }
            } else {
                codeLines++;
            }
        }

        return new LineBreakdown(totalLines, codeLines, commentOnlyLines, blankLines);
    }
}
