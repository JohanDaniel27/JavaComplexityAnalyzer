package com.complexityanalyzer.smells;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import java.nio.file.Path;
import java.util.*;

/**
 * Detects duplicate code blocks using an AST sliding-window approach.
 *
 * <p>Compares statement sequences across all methods in a project.
 * Produces {@link CodeCloneFinding} entries for duplicated blocks of at least {@link #MIN_STATEMENTS} statements.
 */
public class DuplicateCodeDetector {

    private static final int MIN_STATEMENTS = 6;

    /**
     * Detects clone findings across all class analysis results.
     * Each result must have been parsed so the underlying CompilationUnit is available via the class.
     */
    public List<CodeCloneFinding> detect(List<ClassAnalysisResult> classes) {
        // Collect all (method, statements, path) tuples
        record MethodBlock(String className, String methodName, Path filePath,
                           List<Statement> stmts) {}

        List<MethodBlock> blocks = new ArrayList<>();
        for (ClassAnalysisResult clazz : classes) {
            Path fp = clazz.getSourceFile();
            for (var methodResult : clazz.getMethods()) {
                // We need the original MethodDeclaration; store it via MethodAnalysisResult
                // Since we don't have it here, we skip AST-level clone - handled at ProjectAnalyzer level
            }
        }
        // This method returns empty; actual detection is performed in ProjectAnalyzer
        // which has access to the parsed CompilationUnit.
        return List.of();
    }

    /**
     * Core detection: given a flat list of (label, stmtList, path, startLineOffset) records,
     * find duplicated statement sequences of length >= MIN_STATEMENTS.
     */
    public List<CodeCloneFinding> detectFromMethods(
            List<MethodInfo> methods) {

        List<CodeCloneFinding> findings = new ArrayList<>();

        // Build hash -> list of positions
        Map<String, List<StmtPos>> hashIndex = new HashMap<>();

        for (MethodInfo mi : methods) {
            List<Statement> stmts = mi.statements();
            for (int start = 0; start <= stmts.size() - MIN_STATEMENTS; start++) {
                String hash = hashWindow(stmts, start, MIN_STATEMENTS, false);
                hashIndex.computeIfAbsent(hash, k -> new ArrayList<>())
                         .add(new StmtPos(mi, start));
            }
        }

        Set<String> reported = new HashSet<>();
        for (Map.Entry<String, List<StmtPos>> entry : hashIndex.entrySet()) {
            List<StmtPos> positions = entry.getValue();
            if (positions.size() < 2) continue;

            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    StmtPos p1 = positions.get(i);
                    StmtPos p2 = positions.get(j);

                    // Skip if same method
                    if (p1.method().equals(p2.method()) && p1.startIdx() == p2.startIdx()) continue;

                    // Extend the match as far as possible
                    int maxLen = extendMatch(
                            p1.method().statements(), p1.startIdx(),
                            p2.method().statements(), p2.startIdx(),
                            false);

                    if (maxLen < MIN_STATEMENTS) continue;

                    Statement s1start = p1.method().statements().get(p1.startIdx());
                    Statement s1end   = p1.method().statements().get(p1.startIdx() + maxLen - 1);
                    Statement s2start = p2.method().statements().get(p2.startIdx());
                    Statement s2end   = p2.method().statements().get(p2.startIdx() + maxLen - 1);

                    int l1s = s1start.getRange().map(r -> r.begin.line).orElse(0);
                    int l1e = s1end.getRange().map(r -> r.end.line).orElse(0);
                    int l2s = s2start.getRange().map(r -> r.begin.line).orElse(0);
                    int l2e = s2end.getRange().map(r -> r.end.line).orElse(0);

                    String key = Math.min(l1s, l2s) + ":" + Math.max(l1e, l2e) + ":" +
                                 p1.method().filePath() + ":" + p2.method().filePath();
                    if (reported.contains(key)) continue;
                    reported.add(key);

                    SourceLocation loc1 = new SourceLocation(p1.method().filePath(), l1s, 1, l1e, 999);
                    SourceLocation loc2 = new SourceLocation(p2.method().filePath(), l2s, 1, l2e, 999);
                    findings.add(new CodeCloneFinding(loc1, loc2,
                            Math.max(l1e - l1s, l2e - l2s) + 1, maxLen, "TYPE_2"));
                }
            }
        }
        return findings;
    }

    private int extendMatch(List<Statement> stmts1, int start1,
                            List<Statement> stmts2, int start2,
                            boolean exact) {
        int len = 0;
        while (start1 + len < stmts1.size() && start2 + len < stmts2.size()) {
            Statement a = stmts1.get(start1 + len);
            Statement b = stmts2.get(start2 + len);
            String ha = normalizeStmt(a, exact);
            String hb = normalizeStmt(b, exact);
            if (!ha.equals(hb)) break;
            len++;
        }
        return len;
    }

    private String hashWindow(List<Statement> stmts, int start, int len, boolean exact) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + len && i < stmts.size(); i++) {
            sb.append(normalizeStmt(stmts.get(i), exact)).append("|");
        }
        return sb.toString();
    }

    /** Normalizes statement representation for Type-2 (structure-only) matching. */
    private String normalizeStmt(Statement stmt, boolean exact) {
        if (exact) return stmt.toString();
        // Type-2: use class name as key (ignores variable names and literals)
        return stmt.getClass().getSimpleName();
    }

    public record MethodInfo(String className, String methodName, Path filePath,
                             List<Statement> statements) {}

    private record StmtPos(MethodInfo method, int startIdx) {}
}
