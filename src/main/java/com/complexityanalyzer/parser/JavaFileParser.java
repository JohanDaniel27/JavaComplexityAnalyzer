package com.complexityanalyzer.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Parses a single .java file into a CompilationUnit using JavaParser.
 * Uses ParseResult so that files with syntax errors do not throw exceptions;
 * callers should check {@link ParseFileResult#isSuccessful()} before using the AST.
 */
public class JavaFileParser {

    private final JavaParser parser;

    public JavaFileParser() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
        this.parser = new JavaParser(config);
    }

    /**
     * Parses the given file.
     *
     * @param file path to the .java file
     * @return a ParseFileResult that is always non-null; check isSuccessful() before use
     * @throws Exception if the file cannot be read from disk
     */
    public ParseFileResult parse(Path file) throws Exception {
        ParseResult<CompilationUnit> result = parser.parse(file);
        return new ParseFileResult(file, result);
    }
}