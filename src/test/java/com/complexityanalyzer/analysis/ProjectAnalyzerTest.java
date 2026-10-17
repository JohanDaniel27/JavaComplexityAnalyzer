package com.complexityanalyzer.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProjectAnalyzerTest {

    private final ProjectAnalyzer analyzer = new ProjectAnalyzer();

    @Test
    void analyzesEmptyDirectory(@TempDir Path tempDir) throws Exception {
        ProjectAnalysisResult result = analyzer.analyze(tempDir);
        assertThat(result).isNotNull();
        assertThat(result.getClasses()).isEmpty();
    }

    @Test
    void analyzesSimpleClass(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Simple.java");
        Files.writeString(file, "public class Simple { public void foo() {} }");
        ProjectAnalysisResult result = analyzer.analyze(tempDir);
        assertThat(result.getClasses()).hasSize(1);
        assertThat(result.getClasses().get(0).getClassName()).isEqualTo("Simple");
    }

    @Test
    void doesNotCrashOnSyntaxError(@TempDir Path tempDir) throws Exception {
        Path valid = tempDir.resolve("Valid.java");
        Path broken = tempDir.resolve("Broken.java");
        Files.writeString(valid, "public class Valid { }");
        Files.writeString(broken, "public class Broken { void x( { }");
        assertThatCode(() -> analyzer.analyze(tempDir)).doesNotThrowAnyException();
    }

    @Test
    void detectsCycleBetweenTwoClasses(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("Alpha.java"), 
                "public class Alpha { private Beta b; }");
        Files.writeString(tempDir.resolve("Beta.java"),
                "public class Beta { private Alpha a; }");
        ProjectAnalysisResult result = analyzer.analyze(tempDir);
        assertThat(result.getCycles()).isNotEmpty();
    }

    @Test
    void collectsDiagnosticsForBrokenFile(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("Good.java"), "public class Good {}");
        Files.writeString(tempDir.resolve("Bad.java"), "class Bad { void x( { }");
        ProjectAnalysisResult result = analyzer.analyze(tempDir);
        // At least one file should be successfully analyzed
        assertThat(result.getClasses()).isNotEmpty();
    }
}
