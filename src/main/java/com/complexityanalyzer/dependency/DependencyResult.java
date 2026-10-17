package com.complexityanalyzer.dependency;

import java.util.Set;

/** Holds dependency information for one class. */
public class DependencyResult {

    private final String className;
    private final Set<String> dependencies;
    private final String packageName;
    private final int afferentCoupling;   // How many project classes depend ON this class

    public DependencyResult(String className, Set<String> dependencies) {
        this(className, dependencies, "", 0);
    }

    public DependencyResult(String className, Set<String> dependencies,
                            String packageName, int afferentCoupling) {
        this.className = className;
        this.dependencies = Set.copyOf(dependencies);
        this.packageName = packageName;
        this.afferentCoupling = afferentCoupling;
    }

    public String getClassName() { return className; }
    public Set<String> getDependencies() { return dependencies; }
    public String getPackageName() { return packageName; }
    public int getCoupling() { return dependencies.size(); }
    public int getAfferentCoupling() { return afferentCoupling; }
}