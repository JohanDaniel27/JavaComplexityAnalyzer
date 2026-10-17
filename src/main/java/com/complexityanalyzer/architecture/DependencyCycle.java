package com.complexityanalyzer.architecture;

import java.util.List;

/** Represents a circular dependency chain at class or package level. */
public class DependencyCycle {

    public enum Level { CLASS, PACKAGE }

    private final Level level;
    private final List<String> chain; // e.g. ["A", "B", "C", "A"]

    public DependencyCycle(Level level, List<String> chain) {
        this.level = level;
        this.chain = List.copyOf(chain);
    }

    public Level getLevel() { return level; }
    public List<String> getChain() { return chain; }
    public int getSize() { return chain.size() - 1; } // exclude repeated start node

    @Override
    public String toString() {
        return level + " cycle: " + String.join(" -> ", chain);
    }
}
