package com.complexityanalyzer.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.*;

/**
 * Computes LCOM4 (Lack of Cohesion in Methods, version 4).
 *
 * <p>LCOM4 = number of connected components in a method-relationship graph where:
 * <ul>
 *   <li>Vertices are methods.</li>
 *   <li>Edges connect two methods if they share a field access OR one calls the other.</li>
 * </ul>
 * LCOM4 = 1 means the class is cohesive. LCOM4 > 1 suggests the class has separable responsibilities.
 */
public class CohesionAnalyzer {

    public int calculateLcom4(ClassOrInterfaceDeclaration clazz) {
        List<MethodDeclaration> methods = clazz.getMethods();
        if (methods.isEmpty()) return 1;

        // Collect field names declared in this class
        Set<String> fieldNames = new HashSet<>();
        for (FieldDeclaration fd : clazz.getFields()) {
            for (VariableDeclarator vd : fd.getVariables()) {
                fieldNames.add(vd.getNameAsString());
            }
        }

        // Map method index to fields accessed and sibling methods called
        int n = methods.size();
        Map<Integer, Set<String>> methodFields = new HashMap<>();
        Map<Integer, Set<String>> methodCalls  = new HashMap<>();

        for (int i = 0; i < n; i++) {
            MethodDeclaration m = methods.get(i);
            Set<String> accessed = new HashSet<>();
            Set<String> called   = new HashSet<>();

            // Field accesses via "this.field" or bare "field"
            for (FieldAccessExpr fae : m.findAll(FieldAccessExpr.class)) {
                String name = fae.getNameAsString();
                if (fieldNames.contains(name)) accessed.add(name);
            }
            for (NameExpr ne : m.findAll(NameExpr.class)) {
                if (fieldNames.contains(ne.getNameAsString())) accessed.add(ne.getNameAsString());
            }

            // Sibling method calls (unqualified)
            for (MethodCallExpr mce : m.findAll(MethodCallExpr.class)) {
                if (mce.getScope().isEmpty()) {
                    called.add(mce.getNameAsString());
                }
            }

            methodFields.put(i, accessed);
            methodCalls.put(i, called);
        }

        // Build adjacency using Union-Find
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Share a common field?
                Set<String> shared = new HashSet<>(methodFields.get(i));
                shared.retainAll(methodFields.get(j));
                if (!shared.isEmpty()) {
                    union(parent, i, j);
                    continue;
                }
                // i calls j or j calls i?
                String nameJ = methods.get(j).getNameAsString();
                String nameI = methods.get(i).getNameAsString();
                if (methodCalls.get(i).contains(nameJ) || methodCalls.get(j).contains(nameI)) {
                    union(parent, i, j);
                }
            }
        }

        // Count distinct roots
        Set<Integer> roots = new HashSet<>();
        for (int i = 0; i < n; i++) roots.add(find(parent, i));
        return roots.size();
    }

    private int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }
}
