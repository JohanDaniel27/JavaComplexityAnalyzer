package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.List;

/**
 * Calculates Cognitive Complexity as defined by SonarSource (G. Ann Campbell).
 *
 * <p>Rules:
 * <ul>
 *   <li>B1 - Structural increment: +1 for each if/else-if/else/for/while/do/switch/catch/break label/continue label/ternary.</li>
 *   <li>B2 - Nesting increment: each nesting level adds an extra +nesting on top of B1 for nested control structures.</li>
 *   <li>B3 - Logical sequence: +1 for each sequence of mixed &&/|| operators (not per operator).</li>
 * </ul>
 */
public class CognitiveComplexityAnalyzer {

    public int calculate(MethodDeclaration method) {
        int[] score = {0};
        calculateNode(method, 0, score);
        // Add logical sequence bonuses
        score[0] += countLogicalSequences(method);
        return score[0];
    }

    private void calculateNode(Node node, int nestingLevel, int[] score) {
        for (Node child : node.getChildNodes()) {
            if (child instanceof IfStmt ifStmt) {
                // increment + nesting
                score[0] += 1 + nestingLevel;
                // recurse into then-branch at same nesting (no further nesting for then-block itself)
                calculateNode(ifStmt.getThenStmt(), nestingLevel + 1, score);
                // else-if or else
                ifStmt.getElseStmt().ifPresent(elseStmt -> {
                    if (elseStmt instanceof IfStmt) {
                        // else-if: +1 structural but no extra nesting increment
                        score[0] += 1;
                        calculateNode(elseStmt, nestingLevel, score);
                    } else {
                        // plain else: +1 structural
                        score[0] += 1;
                        calculateNode(elseStmt, nestingLevel + 1, score);
                    }
                });
            } else if (child instanceof ForStmt || child instanceof ForEachStmt
                    || child instanceof WhileStmt || child instanceof DoStmt) {
                score[0] += 1 + nestingLevel;
                calculateChildren(child, nestingLevel + 1, score);
            } else if (child instanceof SwitchStmt) {
                score[0] += 1 + nestingLevel;
                calculateChildren(child, nestingLevel + 1, score);
            } else if (child instanceof CatchClause) {
                score[0] += 1 + nestingLevel;
                calculateChildren(child, nestingLevel + 1, score);
            } else if (child instanceof ConditionalExpr) {
                score[0] += 1 + nestingLevel;
                calculateChildren(child, nestingLevel + 1, score);
            } else if (child instanceof LambdaExpr) {
                // Lambdas increase nesting
                calculateChildren(child, nestingLevel + 1, score);
            } else {
                calculateNode(child, nestingLevel, score);
            }
        }
    }

    private void calculateChildren(Node node, int nestingLevel, int[] score) {
        for (Node child : node.getChildNodes()) {
            calculateNode(child, nestingLevel, score);
        }
    }

    /**
     * Count logical operator sequences: each transition between a sequence of &&/|| adds +1.
     * E.g. "a && b && c" = 1 sequence = +1.
     * "a && b || c" = 2 sequences = +2.
     */
    private int countLogicalSequences(Node node) {
        int count = 0;
        List<BinaryExpr> binaryExprs = node.findAll(BinaryExpr.class);
        // Walk top-level binary expressions only; nested ones are sub-nodes
        for (BinaryExpr expr : binaryExprs) {
            boolean isLogical = isLogicalOp(expr.getOperator());
            if (!isLogical) continue;
            // Count only if the parent is NOT also a logical binary expression (top of chain)
            boolean parentIsLogical = expr.getParentNode()
                    .filter(p -> p instanceof BinaryExpr b && isLogicalOp(b.getOperator()))
                    .isPresent();
            if (!parentIsLogical) {
                // Count distinct operator-type transitions in this chain
                count += countChainSequences(expr);
            }
        }
        return count;
    }

    private int countChainSequences(BinaryExpr root) {
        // Flatten the chain and count runs of same operator type
        BinaryExpr.Operator lastOp = null;
        int sequences = 0;
        for (BinaryExpr expr : flattenChain(root)) {
            BinaryExpr.Operator op = expr.getOperator();
            if (!op.equals(lastOp)) {
                sequences++;
                lastOp = op;
            }
        }
        return sequences;
    }

    private List<BinaryExpr> flattenChain(BinaryExpr expr) {
        java.util.List<BinaryExpr> result = new java.util.ArrayList<>();
        flattenHelper(expr, result);
        return result;
    }

    private void flattenHelper(Node node, List<BinaryExpr> result) {
        if (node instanceof BinaryExpr b && isLogicalOp(b.getOperator())) {
            flattenHelper(b.getLeft(), result);
            result.add(b);
            flattenHelper(b.getRight(), result);
        }
    }

    private boolean isLogicalOp(BinaryExpr.Operator op) {
        return op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR;
    }
}
