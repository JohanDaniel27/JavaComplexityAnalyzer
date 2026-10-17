package com.complexityanalyzer.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveComplexityAnalyzerTest {

    private CognitiveComplexityAnalyzer analyzer;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        analyzer = new CognitiveComplexityAnalyzer();
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(config);
    }

    private MethodDeclaration parseMethod(String src) {
        CompilationUnit cu = parser.parse("class T { " + src + " }").getResult().orElseThrow();
        return cu.findAll(MethodDeclaration.class).get(0);
    }

    @Test
    void emptyMethodHasZeroCognitive() {
        assertThat(analyzer.calculate(parseMethod("void foo() {}"))).isEqualTo(0);
    }

    @Test
    void singleIfHasCognitiveOne() {
        assertThat(analyzer.calculate(parseMethod("void foo(int x) { if (x > 0) {} }"))).isEqualTo(1);
    }

    @Test
    void nestedIfIncrementsNesting() {
        // outer if: +1, inner if: +1+1=2 -> total 3
        String m = "void foo(int x, int y) { if (x > 0) { if (y > 0) {} } }";
        assertThat(analyzer.calculate(parseMethod(m))).isGreaterThanOrEqualTo(3);
    }

    @Test
    void logicalSequencesCountedOnce() {
        // a && b && c is one AND sequence = +1
        String m = "void foo(int a, int b, int c) { if (a > 0 && b > 0 && c > 0) {} }";
        // if=1, &&-sequence=1 => total 2 minimum
        assertThat(analyzer.calculate(parseMethod(m))).isGreaterThanOrEqualTo(2);
    }
}
