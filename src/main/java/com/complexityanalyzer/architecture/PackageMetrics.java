package com.complexityanalyzer.architecture;

/** Robert C. Martin package-level coupling metrics. */
public class PackageMetrics {

    private final String packageName;
    private final int afferentCoupling;  // Ca: how many external packages depend on this one
    private final int efferentCoupling;  // Ce: how many external packages this one depends on
    private final double instability;    // I = Ce / (Ca + Ce)
    private final double abstractness;   // A = abstract types / total types
    private final double distanceFromMainSequence; // D = |A + I - 1|
    private final int classCount;
    private final int abstractClassCount;

    public PackageMetrics(String packageName, int afferentCoupling, int efferentCoupling,
                          int classCount, int abstractClassCount) {
        this.packageName = packageName;
        this.afferentCoupling = afferentCoupling;
        this.efferentCoupling = efferentCoupling;
        this.classCount = classCount;
        this.abstractClassCount = abstractClassCount;

        int total = afferentCoupling + efferentCoupling;
        this.instability = total == 0 ? 0.0 : (double) efferentCoupling / total;
        this.abstractness = classCount == 0 ? 0.0 : (double) abstractClassCount / classCount;
        this.distanceFromMainSequence = Math.abs(abstractness + instability - 1.0);
    }

    public String getPackageName() { return packageName; }
    public int getAfferentCoupling() { return afferentCoupling; }
    public int getEfferentCoupling() { return efferentCoupling; }
    public double getInstability() { return instability; }
    public double getAbstractness() { return abstractness; }
    public double getDistanceFromMainSequence() { return distanceFromMainSequence; }
    public int getClassCount() { return classCount; }
    public int getAbstractClassCount() { return abstractClassCount; }
}
