package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.*;

public class NestingAnalyzer {

    public int calculate(Node method) {
        return calculateDepth(method, 0);
    }

    private int calculateDepth(Node node, int depth) {

        int maxDepth = depth;

        for (Node child : node.getChildNodes()) {

            int newDepth = depth;

            if (isNestingNode(child)) {
                newDepth++;
            }

            maxDepth = Math.max(
                    maxDepth,
                    calculateDepth(child, newDepth)
            );
        }

        return maxDepth;
    }

    private boolean isNestingNode(Node node) {

        return node instanceof IfStmt
                || node instanceof ForStmt
                || node instanceof ForEachStmt
                || node instanceof WhileStmt
                || node instanceof DoStmt
                || node instanceof SwitchStmt
                || node instanceof CatchClause;
    }
}