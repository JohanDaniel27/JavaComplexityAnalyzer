package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;

public class ASTMetricsAnalyzer {

    public int countStatements(Node node) {
        return node.findAll(Statement.class).size();
    }
}