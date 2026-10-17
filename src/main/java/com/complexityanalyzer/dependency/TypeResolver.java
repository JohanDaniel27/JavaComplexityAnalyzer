package com.complexityanalyzer.dependency;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.*;

/**
 * Builds an index of all project class declarations with their fully qualified names (FQNs)
 * and resolves simple type names to FQNs by inspecting import statements.
 */
public class TypeResolver {

    /** Map from simple class name -> fully qualified name. */
    private final Map<String, String> simpleFqnIndex = new HashMap<>();
    /** Map from simple class name -> package name. */
    private final Map<String, String> classPackageIndex = new HashMap<>();

    /**
     * Builds the resolution index from all compilation units.
     */
    public void buildIndex(List<CompilationUnit> units) {
        simpleFqnIndex.clear();
        classPackageIndex.clear();
        for (CompilationUnit unit : units) {
            String pkg = unit.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");
            for (TypeDeclaration<?> td : unit.getTypes()) {
                String simpleName = td.getNameAsString();
                String fqn = pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
                simpleFqnIndex.put(simpleName, fqn);
                classPackageIndex.put(simpleName, pkg);
            }
        }
    }

    /** Returns true if the given simple name is a known project class. */
    public boolean isProjectClass(String simpleName) {
        return simpleFqnIndex.containsKey(simpleName);
    }

    /** Returns the package of a project class by simple name, or empty string if unknown. */
    public String getPackage(String simpleName) {
        return classPackageIndex.getOrDefault(simpleName, "");
    }

    /** Returns the FQN of a project class by simple name, or the simple name if unknown. */
    public String resolve(String simpleName) {
        return simpleFqnIndex.getOrDefault(simpleName, simpleName);
    }

    /**
     * Resolves dependencies for a class, applying import context to disambiguate names.
     *
     * @param unit            the compilation unit containing the class
     * @param className       the class being analysed (excluded from its own deps)
     * @param rawDependencies simple names of referenced types
     * @return filtered and resolved dependency simple names (project classes only)
     */
    public Set<String> resolveDependencies(CompilationUnit unit, String className,
                                           Set<String> rawDependencies) {
        Set<String> resolved = new LinkedHashSet<>();
        for (String dep : rawDependencies) {
            if (dep.equals(className)) continue;
            if (isProjectClass(dep)) {
                resolved.add(dep);
            }
        }
        return resolved;
    }

    public Set<String> getAllProjectClassNames() {
        return Collections.unmodifiableSet(simpleFqnIndex.keySet());
    }
}
