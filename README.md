Java Complexity Analyzer
A Java desktop application for inspecting Java source code, measuring method and class complexity, detecting selected code smells, examining class dependencies, and summarizing potential maintenance risk.
The analyzer uses JavaParser to parse source files into an Abstract Syntax Tree (AST). A JavaFX interface lets users select a project folder and explore analysis results in tables, charts, and a dependency graph.
Project status: In development. The current implementation includes source scanning, AST-based metrics, selected code-smell rules, simple-name class dependency analysis, heuristic risk scoring, and a JavaFX dashboard. See Current limitations for important details.

Features
Source analysis
- Choose a Java project folder from the desktop interface.
- Recursively locate .java files.
- Parse source files using JavaParser.
- Analyze discovered class and interface declarations.
- Run analysis in a background task so the JavaFX interface remains responsive during processing.
Method metrics
- Method name
- Source line span
- Parameter count
- Recursive AST statement count
- Cyclomatic-complexity estimate
- Maximum nesting depth
Class metrics
- Class/interface name
- Source line span
- Method count
- Field-declaration count
- Total method complexity
- Average method complexity
- Maximum method nesting depth
Code-smell detection
The current detector uses configurable-in-code heuristic thresholds to flag:
Code smell	Current rule	Severity
Long Method	Method line span > 50	High
High Complexity	Cyclomatic complexity > 10	High
Deep Nesting	Nesting depth > 3	Medium
Too Many Parameters	Parameter count > 5	Medium
Large Class	Class line span > 300	High


These thresholds are project choices, not universal standards. The rules can be adjusted in CodeSmellDetector.
Dependency and coupling analysis
- Identify references to class/interface types whose simple names exist in the scanned project.
- Collect distinct dependencies for each class/interface.
- Calculate a simple coupling count as the number of distinct detected project dependencies.
- Draw dependency arrows in the JavaFX Dependencies tab.
An arrow A → B means that class A is detected as depending on class B.
Heuristic risk score
Each class receives a score from 0 to 100 and a risk level. The score is a project-defined heuristic, not a calibrated prediction of defects.
Input	Maximum contribution	Current calculation
Total cyclomatic complexity	30	min(30, max(0, (totalComplexity - 1) × 2))
Class line span	20	min(20, linesOfCode / 20) using integer division
Maximum nesting depth	15	min(15, nestingDepth × 3)
Coupling	20	min(20, coupling × 2)
Detected smells	15	High = 5, Medium = 3, Low = 1 per finding, capped at 15


Risk levels:
- LOW: 0–20
- MEDIUM: 21–50
- HIGH: 51–75
- CRITICAL: 76–100
The current risk calculation includes class-level findings and method-level findings associated with that class. Review the underlying metrics and findings rather than treating the score as an absolute quality judgement.
JavaFX desktop interface
The application currently provides three tabs:
- Overview: risk-distribution pie chart and a bar chart of up to 10 classes ordered by total complexity.
- Classes: a table of classes with LOC, method count, total complexity, coupling, risk score, and risk level. Selecting a row displays class metrics, dependencies, code smells, and method details.
- Dependencies: a visual graph of detected class dependencies.
Technology stack
Technology	Purpose
Java 25	Main programming language
Maven	Build and dependency management
JavaParser Core 3.28.2	Java source parsing and AST traversal
JavaFX Controls 25.0.4	Desktop interface, tables, controls, and charts
JavaFX Maven Plugin 0.0.8	Launching the JavaFX application with Maven


Requirements
- JDK 25
- Maven installed and available on PATH
- IntelliJ IDEA or another Java IDE (recommended, not required)
- Internet access on the first Maven build so dependencies can be downloaded
Verify the installations in a terminal:
java -version
javac -version
mvn -version
Getting started
1. Clone the repository
git clone <repository-url>
cd JavaComplexityAnalyzer
Replace <repository-url> with the URL of your Git repository.
2. Open the project
Open the folder containing pom.xml in IntelliJ IDEA and allow Maven to import the project and download dependencies.
3. Build the project
mvn clean package
4. Launch the desktop application
From the directory containing pom.xml, run:
mvn clean javafx:run
The JavaFX window should open. Select a folder containing Java source files, click Analyze Project, and use the tabs to inspect the results.
The console-based entry point com.complexityanalyzer.Main is retained for backend checks; the desktop application entry point is com.complexityanalyzer.ui.AnalyzerApp.
Architecture
Selected project folder
          |
          v
    ProjectScanner
          |
          v
     JavaFileParser
          |
          v
   CompilationUnit ASTs
          |
          v
    ProjectAnalyzer
      /    |     \
     v     v      v
  Metrics Smells Dependencies
      \    |     /
       v   v    v
       RiskCalculator
              |
              v
    ProjectAnalysisResult
              |
              v
        JavaFX UI
       /     |      \
  Overview Classes Dependencies
The analysis layer is separate from the user interface. ProjectAnalyzer coordinates scanning, parsing, metrics, smell detection, dependency analysis, and risk scoring. It returns a ProjectAnalysisResult that the JavaFX layer can display.
Project structure
JavaComplexityAnalyzer/
├── pom.xml
├── README.md
├── testcode/                         # Sample Java source files for testing
└── src/
    └── main/
        └── java/
            └── com/
                └── complexityanalyzer/
                    ├── Main.java
                    ├── analysis/
                    │   ├── MethodAnalysisResult.java
                    │   ├── ClassAnalysisResult.java
                    │   ├── ProjectAnalysisResult.java
                    │   ├── RiskCalculator.java
                    │   └── ProjectAnalyzer.java
                    ├── dependency/
                    │   ├── DependencyAnalyzer.java
                    │   └── DependencyResult.java
                    ├── metrics/
                    │   ├── MethodMetrics.java
                    │   ├── MetricsAnalyzer.java
                    │   ├── ASTMetricsAnalyzer.java
                    │   ├── ComplexityAnalyzer.java
                    │   ├── NestingAnalyzer.java
                    │   ├── ClassMetrics.java
                    │   └── ClassMetricsAnalyzer.java
                    ├── parser/
                    │   ├── JavaFileParser.java
                    │   └── ProjectScanner.java
                    ├── smells/
                    │   ├── CodeSmell.java
                    │   └── CodeSmellDetector.java
                    └── ui/
                        ├── AnalyzerApp.java
                        ├── DashboardCharts.java
                        └── DependencyGraphView.java
The exact tree may vary slightly as the project evolves.
Design choices
- AST-based analysis: analyse parsed Java constructs rather than counting keywords in raw text.
- Separation of concerns: scanning, parsing, metrics, smells, dependencies, risk scoring, and presentation are separate components.
- Reusable result models: method, class, and project results are represented as objects so the console and GUI can consume the same analysis output.
- Background analysis: the JavaFX interface uses a background task for the analysis operation.
- Transparent heuristics: rules and score contributions are documented and can be revised as testing improves.
Current limitations
- Line span is not comment-free LOC. The current line metric uses the AST node's start and end line numbers, so blank lines and comments inside that range can be included.
- Cyclomatic complexity is an estimate based on the AST rules currently implemented. Its treatment of constructs such as switch should be validated against a documented metric convention before comparing scores with external tools.
- Dependencies use simple class names. Different packages containing classes with the same name can be ambiguous. Dependency detection is based on type references found in the parsed AST and is not a complete Java semantic/type-resolution engine.
- Supported declarations: current class-level processing centres on JavaParser ClassOrInterfaceDeclaration nodes; enums and records are not yet represented by the same class-analysis flow.
- Risk score is heuristic. It has not been statistically calibrated against real defect data and should be used as a prioritisation aid, not as proof that code is defective.
- Analysis scope: the scanner recursively considers .java files under the selected directory. For meaningful results, select a source directory or project folder and avoid including irrelevant generated sources when possible.
- Test coverage: the project should be validated with a broader suite of known-input test cases before relying on its metrics for production decisions.
Development roadmap
Potential next improvements include:
1. Add automated unit tests for complexity, nesting, LOC, code smells, dependency extraction, and risk scoring.
2. Improve type resolution using fully qualified class names and package information.
3. Refine metric conventions and document edge cases such as lambdas, switch expressions, constructors, records, and enums.
4. Improve graph layout and handling of large projects.
5. Add analysis reports and export options.
6. Add configurable smell thresholds and risk-score weights.
7. Improve error handling for malformed or unsupported source files.
License
No license has been specified yet. Add a LICENSE file before distributing the project as open-source software.
