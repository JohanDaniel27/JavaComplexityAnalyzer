package com.complexityanalyzer.export;

import com.complexityanalyzer.analysis.ClassAnalysisResult;
import com.complexityanalyzer.analysis.MethodAnalysisResult;
import com.complexityanalyzer.analysis.ProjectAnalysisResult;
import com.complexityanalyzer.git.HotspotResult;
import com.complexityanalyzer.qualitygate.QualityGateResult;
import com.complexityanalyzer.qualitygate.QualityGateViolation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

/** Exports analysis results to a JSON file. */
public class JsonReportExporter {

    private final ObjectMapper mapper;

    public JsonReportExporter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void export(ProjectAnalysisResult result, QualityGateResult gateResult,
                       Path outputPath) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("projectPath", result.getProjectPath().toString());
        root.put("generatedAt", Instant.now().toString());
        root.put("totalClasses", result.getClasses().size());
        root.put("totalCycles", result.getCycles().size());
        root.put("totalClones", result.getClones().size());

        if (gateResult != null) {
            ObjectNode gate = root.putObject("qualityGate");
            gate.put("status", gateResult.getStatus().toString());
            gate.put("violations", gateResult.getViolationCount());
            ArrayNode vArr = gate.putArray("violationList");
            for (QualityGateViolation v : gateResult.getViolations()) {
                ObjectNode vn = vArr.addObject();
                vn.put("rule", v.getRule());
                vn.put("entity", v.getEntity());
                vn.put("message", v.getMessage());
                vn.put("severity", v.getSeverity());
            }
        }

        ArrayNode classes = root.putArray("classes");
        for (ClassAnalysisResult c : result.getClasses()) {
            ObjectNode cn = classes.addObject();
            cn.put("name", c.getClassName());
            cn.put("file", c.getSourceFile() != null ? c.getSourceFile().toString() : "");
            cn.put("riskScore", c.getRiskScore());
            cn.put("riskLevel", c.getRiskLevel());
            cn.put("loc", c.getMetrics().getLinesOfCode());
            cn.put("ncloc", c.getMetrics().getNcloc());
            cn.put("totalComplexity", c.getMetrics().getTotalComplexity());
            cn.put("cognitiveComplexity", c.getMetrics().getTotalCognitiveComplexity());
            cn.put("lcom4", c.getMetrics().getLcom4());
            cn.put("coupling", c.getDependency().getCoupling());
            cn.put("smells", c.getSmells().size());

            ArrayNode methods = cn.putArray("methods");
            for (MethodAnalysisResult m : c.getMethods()) {
                ObjectNode mn = methods.addObject();
                mn.put("name", m.getMetrics().getMethodName());
                mn.put("loc", m.getMetrics().getLinesOfCode());
                mn.put("complexity", m.getMetrics().getComplexity());
                mn.put("cognitive", m.getMetrics().getCognitiveComplexity());
                mn.put("nesting", m.getMetrics().getNestingDepth());
                mn.put("smells", m.getSmells().size());
            }
        }

        ArrayNode hotspots = root.putArray("hotspots");
        for (HotspotResult h : result.getHotspots()) {
            ObjectNode hn = hotspots.addObject();
            hn.put("class", h.getClassName());
            hn.put("score", h.getHotspotScore());
            hn.put("tier", h.getTier().toString());
            hn.put("commits", h.getCommitCount());
            hn.put("churn", h.getTotalChurn());
        }

        mapper.writeValue(outputPath.toFile(), root);
    }
}
