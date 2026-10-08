package com.complexityanalyzer.dependency;

import java.util.Set;

public class DependencyResult {

    private final String className;
    private final Set<String> dependencies;

    public DependencyResult(String className, Set<String> dependencies) {
        this.className = className;
        this.dependencies = dependencies;
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public int getCoupling() {
        return dependencies.size();
    }
}