package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;

public class ComplexityAnalyzer {

    public int calculate(Node method) {

        int complexity = 1;

        complexity += method.findAll(IfStmt.class).size();
        complexity += method.findAll(ForStmt.class).size();
        complexity += method.findAll(WhileStmt.class).size();
        complexity += method.findAll(DoStmt.class).size();
        complexity += method.findAll(CatchClause.class).size();
        complexity += method.findAll(SwitchEntry.class).size();
        complexity += method.findAll(ConditionalExpr.class).size();

        for (BinaryExpr expr : method.findAll(BinaryExpr.class)) {
            if (expr.getOperator() == BinaryExpr.Operator.AND ||
                    expr.getOperator() == BinaryExpr.Operator.OR) {
                complexity++;
            }
        }

        return complexity;
    }
}