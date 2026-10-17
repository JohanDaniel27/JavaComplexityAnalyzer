package com.complexityanalyzer.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NestingAnalyzerTest {

    private NestingAnalyzer analyzer;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        analyzer = new NestingAnalyzer();
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(config);
    }

    private MethodDeclaration parseMethod(String src) {
        CompilationUnit cu = parser.parse("class T { " + src + " }").getResult().orElseThrow();
        return cu.findAll(MethodDeclaration.class).get(0);
    }

    @Test
    void flatMethodHasZeroNesting() {
        assertThat(analyzer.calculate(parseMethod("void foo() { int x = 1; }"))).isEqualTo(0);
    }

    @Test
    void singleIfHasDepthOne() {
        assertThat(analyzer.calculate(parseMethod("void foo(int x) { if (x > 0) { } }"))).isEqualTo(1);
    }

    @Test
    void nestedIfHasDepthTwo() {
        String m = "void foo(int x, int y) { if (x > 0) { if (y > 0) { } } }";
        assertThat(analyzer.calculate(parseMethod(m))).isEqualTo(2);
    }

    @Test
    void tripleNestingDetected() {
        String m = "void foo(int x) {"
                + "if (x > 0) {"
                + "  for (int i = 0; i < x; i++) {"
                + "    while (x > 0) { x--; }"
                + "  }"
                + "}"
                + "}";
        assertThat(analyzer.calculate(parseMethod(m))).isEqualTo(3);
    }
}
