package com.complexityanalyzer.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexityAnalyzerTest {

    private ComplexityAnalyzer analyzer;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        analyzer = new ComplexityAnalyzer();
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(config);
    }

    private MethodDeclaration parseMethod(String methodSource) {
        String wrapped = "class T { " + methodSource + " }";
        CompilationUnit cu = parser.parse(wrapped).getResult().orElseThrow();
        return cu.findAll(MethodDeclaration.class).get(0);
    }

    @Test
    void simpleMethodHasBaseComplexityOne() {
        MethodDeclaration m = parseMethod("void foo() {}");
        assertThat(analyzer.calculate(m)).isEqualTo(1);
    }

    @Test
    void singleIfAddsOne() {
        MethodDeclaration m = parseMethod("void foo(int x) { if (x > 0) { } }");
        assertThat(analyzer.calculate(m)).isEqualTo(2);
    }

    @Test
    void forLoopAddsOne() {
        MethodDeclaration m = parseMethod("void foo() { for (int i=0;i<5;i++) {} }");
        assertThat(analyzer.calculate(m)).isEqualTo(2);
    }

    @Test
    void whileLoopAddsOne() {
        MethodDeclaration m = parseMethod("void foo(int x) { while (x > 0) { x--; } }");
        assertThat(analyzer.calculate(m)).isEqualTo(2);
    }

    @Test
    void logicalAndAddsOne() {
        MethodDeclaration m = parseMethod("void foo(int a, int b) { if (a > 0 && b > 0) {} }");
        // if + && = 3
        assertThat(analyzer.calculate(m)).isEqualTo(3);
    }

    @Test
    void complexMethodHasHighComplexity() {
        // if/else-if/else + for + while + switch entry + ternary = at least 6
        String method = "int foo(int a, int b) {"
                + "if (a > 0) {} else if (a < 0) {} else {}"
                + "for (int i=0;i<5;i++) {}"
                + "while (b > 0) { b--; }"
                + "return a > 0 ? a : b;"
                + "}";
        assertThat(analyzer.calculate(parseMethod(method))).isGreaterThan(5);
    }
}
