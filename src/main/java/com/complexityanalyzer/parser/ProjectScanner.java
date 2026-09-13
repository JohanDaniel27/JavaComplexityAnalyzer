package com.complexityanalyzer.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ProjectScanner {

    public List<Path> findJavaFiles(Path project) throws IOException {

        try (Stream<Path> paths = Files.walk(project)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }
}