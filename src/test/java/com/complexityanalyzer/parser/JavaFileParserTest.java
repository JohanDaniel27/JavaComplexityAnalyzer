package com.complexityanalyzer.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaFileParserTest {

    private final JavaFileParser parser = new JavaFileParser();

    @Test
    void parsesValidFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Valid.java");
        Files.writeString(file, "public class Valid { public void foo() {} }");
        ParseFileResult result = parser.parse(file);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getCompilationUnit()).isPresent();
    }

    @Test
    void handlesFileWithSyntaxError(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Broken.java");
        Files.writeString(file, "public class Broken { public void broken( { } }");
        ParseFileResult result = parser.parse(file);
        // Should not throw; may or may not be successful
        assertThat(result).isNotNull();
        assertThat(result.getProblems()).isNotNull();
    }

    @Test
    void returnsProblemsForSyntaxError(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Error.java");
        Files.writeString(file, "class E { void x( { }");
        ParseFileResult result = parser.parse(file);
        // Either not successful or has problems recorded
        boolean hasIssues = !result.isSuccessful() || !result.getProblems().isEmpty();
        assertThat(hasIssues).isTrue();
    }
}
