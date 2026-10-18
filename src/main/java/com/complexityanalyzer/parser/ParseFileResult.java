package com.complexityanalyzer.parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Wraps a JavaParser ParseResult together with the originating file path. */
public class ParseFileResult {

    private final Path filePath;
    private final ParseResult<CompilationUnit> parseResult;

    public ParseFileResult(Path filePath, ParseResult<CompilationUnit> parseResult) {
        this.filePath = filePath;
        this.parseResult = parseResult;
    }

    public Path getFilePath() { return filePath; }

    public boolean isSuccessful() { return parseResult.isSuccessful(); }

    public Optional<CompilationUnit> getCompilationUnit() { return parseResult.getResult(); }

    public List<Problem> getProblems() { return parseResult.getProblems(); }
}
