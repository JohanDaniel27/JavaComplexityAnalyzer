package com.complexityanalyzer.git;

import com.complexityanalyzer.analysis.ClassAnalysisResult;

import java.util.*;

/**
 * Ranks classes as maintenance hotspots by combining Git churn frequency
 * with code complexity.
 *
 * <p>Hotspot Score = Norm(CommitCount) * 0.5 + Norm(CyclomaticComplexity + CognitiveComplexity) * 0.5
 */
public class HotspotRanker {

    /**
     * Produces a ranked list of hotspot results.
     *
     * @param classes     class analysis results with complexity metrics
     * @param gitStatsMap map from relative file path -> FileGitStats (may be empty if no git repo)
     * @return ranked list, highest hotspot first
     */
    public List<HotspotResult> rank(List<ClassAnalysisResult> classes,
                                    Map<String, FileGitStats> gitStatsMap) {

        if (classes.isEmpty()) return List.of();

        int maxCommits    = 1;
        int maxComplexity = 1;

        // Find max values for normalization
        for (ClassAnalysisResult c : classes) {
            FileGitStats stats = findStats(gitStatsMap, c);
            if (stats != null) {
                maxCommits = Math.max(maxCommits, stats.getCommitCount());
            }
            int combined = c.getMetrics().getTotalComplexity()
                         + c.getMetrics().getTotalCognitiveComplexity();
            maxComplexity = Math.max(maxComplexity, combined);
        }

        List<HotspotResult> results = new ArrayList<>();
        for (ClassAnalysisResult c : classes) {
            FileGitStats stats = findStats(gitStatsMap, c);

            int commitCount  = stats != null ? stats.getCommitCount() : 0;
            int churn        = stats != null ? stats.getTotalChurn()  : 0;
            int authors      = stats != null ? stats.getAuthorCount() : 0;
            int cyclCC = c.getMetrics().getTotalComplexity();
            int cogCC  = c.getMetrics().getTotalCognitiveComplexity();

            double normCommit = (double) commitCount / maxCommits;
            double normComp   = (double) (cyclCC + cogCC) / maxComplexity;
            double score      = normCommit * 0.5 + normComp * 0.5;

            HotspotResult.Tier tier;
            if (score >= 0.75)      tier = HotspotResult.Tier.CRITICAL_HOTSPOT;
            else if (score >= 0.50) tier = HotspotResult.Tier.HIGH_HOTSPOT;
            else if (score >= 0.25) tier = HotspotResult.Tier.MODERATE;
            else                    tier = HotspotResult.Tier.LOW;

            results.add(new HotspotResult(
                    c.getClassName(),
                    c.getSourceFile() != null ? c.getSourceFile().toString() : "",
                    commitCount, churn, authors, cyclCC, cogCC, score, tier
            ));
        }

        results.sort(Comparator.comparingDouble(HotspotResult::getHotspotScore).reversed());
        return results;
    }

    private FileGitStats findStats(Map<String, FileGitStats> statsMap, ClassAnalysisResult c) {
        if (statsMap.isEmpty() || c.getSourceFile() == null) return null;
        String absPath = c.getSourceFile().toString().replace('\\', '/');
        for (Map.Entry<String, FileGitStats> entry : statsMap.entrySet()) {
            if (absPath.endsWith(entry.getKey().replace('\\', '/'))) {
                return entry.getValue();
            }
        }
        return null;
    }
}
