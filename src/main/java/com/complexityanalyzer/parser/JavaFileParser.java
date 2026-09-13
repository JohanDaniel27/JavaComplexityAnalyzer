package com.complexityanalyzer.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;

public class JavaFileParser {

    public CompilationUnit parse(Path file) throws Exception {
        return StaticJavaParser.parse(file);
    }
}