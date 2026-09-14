# JavaComplexityAnalyzer

JavaComplexityAnalyzer is a desktop static-analysis tool built with Java that analyzes Java source code and reports software complexity, maintainability, code smells, and structural relationships between classes.

The project is designed to go beyond simple line counting by parsing Java source code into an Abstract Syntax Tree (AST) and performing analysis on actual Java language constructs.

## Features

### Current Features

* Java project and source-file scanning
* Recursive detection of `.java` files
* Java source-code parsing using JavaParser
* Abstract Syntax Tree (AST) based analysis
* Method-level metrics
* Lines of Code
* Parameter count
* Statement count
* Cyclomatic Complexity
* Maximum Nesting Depth
* Class-level metrics
* Total complexity
* Average complexity
* Maximum class nesting depth

### Planned Features

* Code smell detection
* Long method detection
* Large class detection
* Excessive parameter detection
* High complexity detection
* Deep nesting detection
* Class dependency analysis
* Coupling metrics
* Heuristic code-risk scoring
* JavaFX desktop dashboard
* Complexity charts and visualizations
* Class dependency graph
* Detailed method and class reports
* Project-level analysis
* Exportable analysis reports

## How It Works

The analyzer follows this pipeline:

```text
Java Project
     |
     v
Project Scanner
     |
     v
Java Source Files
     |
     v
JavaParser
     |
     v
Abstract Syntax Tree (AST)
     |
     v
Analysis Engine
     |
     +------------------+
     |                  |
     v                  v
Method Metrics      Class Metrics
     |                  |
     +--------+---------+
              |
              v
       Code Smell Detection
              |
              v
         Risk Analysis
              |
              v
        JavaFX Dashboard
```

## Why AST-Based Analysis?

A simple text-based analyzer can produce incorrect results.

For example:

```java
String message = "if this happens";
```

contains the text `if`, but it is not a conditional statement.

JavaComplexityAnalyzer instead parses Java source code into an AST. This allows the analyzer to identify actual language constructs such as:

* `if`
* `for`
* `while`
* `do-while`
* `switch`
* `catch`
* conditional expressions
* logical operators
* methods
* fields
* classes

This makes the analysis more reliable and extensible.

## Metrics

### Lines of Code

Measures the source-code range occupied by a method or class.

### Parameter Count

Counts the number of parameters accepted by a method.

### Statement Count

Counts statements represented in the method's AST.

### Cyclomatic Complexity

A basic cyclomatic-complexity calculation starts with:

```text
Complexity = 1 + Decision Points
```

Decision points currently include constructs such as:

* `if`
* `for`
* `while`
* `do-while`
* `catch`
* `switch` entries
* conditional expressions
* logical `&&`
* logical `||`

### Nesting Depth

Measures how deeply control-flow constructs are nested inside a method.

For example:

```java
if (...) {
    for (...) {
        if (...) {
            // depth = 3
        }
    }
}
```

### Class Metrics

The analyzer aggregates method-level information to produce class-level metrics such as:

* number of methods
* number of fields
* total complexity
* average complexity
* maximum nesting depth
* lines of code

## Example

Given a method such as:

```java
public int calculate(int a, int b) {

    if (a > b && b > 0) {
        return a;
    } else if (a == b) {
        return 0;
    }

    for (int i = 0; i < 5; i++) {
        number++;
    }

    return b;
}
```

the analyzer can identify:

```text
Method: calculate

Parameters: 2
Cyclomatic Complexity: 4
```

and additional metrics such as lines, statements, and nesting depth.

## Technology Stack

| Technology | Purpose                                |
| ---------- | -------------------------------------- |
| Java 25    | Core programming language              |
| Maven      | Build and dependency management        |
| JavaParser | Java source parsing and AST generation |
| JavaFX     | Desktop graphical interface            |
| JUnit      | Testing                                |

## Project Structure

```text
JavaComplexityAnalyzer/
│
├── pom.xml
├── README.md
│
├── testcode/
│   └── TestCode.java
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── complexityanalyzer/
    │   │           ├── Main.java
    │   │           │
    │   │           ├── parser/
    │   │           │   ├── JavaFileParser.java
    │   │           │   └── ProjectScanner.java
    │   │           │
    │   │           ├── metrics/
    │   │           │   ├── MethodMetrics.java
    │   │           │   ├── MetricsAnalyzer.java
    │   │           │   ├── ASTMetricsAnalyzer.java
    │   │           │   ├── ComplexityAnalyzer.java
    │   │           │   ├── NestingAnalyzer.java
    │   │           │   ├── ClassMetrics.java
    │   │           │   └── ClassMetricsAnalyzer.java
    │   │           │
    │   │           ├── smells/
    │   │           ├── model/
    │   │           ├── scoring/
    │   │           └── ui/
    │   │
    │   └── resources/
    │
    └── test/
        └── java/
```

The structure will grow as additional analysis and JavaFX functionality is implemented.

## Requirements

To build and run the project, install:

* JDK 25
* Maven
* IntelliJ IDEA or another Java IDE

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

## Running the Project

Clone the repository:

```bash
git clone <repository-url>
```

Enter the project directory:

```bash
cd JavaComplexityAnalyzer
```

Build the project:

```bash
mvn clean install
```

Run the application from IntelliJ IDEA or through the configured Maven/JavaFX run configuration.

## Development Roadmap

The project is being developed incrementally:

```text
Phase 1
Project setup
        |
Phase 2
JavaParser integration
        |
Phase 3
File/project scanning
        |
Phase 4
Method-level complexity analysis
        |
Phase 5
Class-level metrics
        |
Phase 6
Code smell detection
        |
Phase 7
Coupling and dependency analysis
        |
Phase 8
Risk scoring
        |
Phase 9
JavaFX desktop interface
        |
Phase 10
Visualization and dependency graphs
        |
Phase 11
Testing and validation
        |
Phase 12
Final polish and reporting
```

## Goals

The final version of JavaComplexityAnalyzer aims to provide a developer-oriented desktop tool that helps answer questions such as:

* Which methods are the most complex?
* Which classes are difficult to maintain?
* Where is excessive nesting present?
* Which methods have too many parameters?
* Which classes are highly coupled?
* Where are potential code smells located?
* Which parts of a project require the most attention?

## Design Principles

The project follows several software-engineering principles:

* Separation of concerns
* Modular analysis components
* AST-based source analysis
* Reusable metric classes
* Extensible code-smell detection
* Independent analysis and presentation layers
* Testable analysis logic

The goal is to keep the analysis engine independent from the graphical interface so that the same analysis functionality can be reused in different interfaces later.

## Status

**Current status:** Under active development.

The current implementation supports Java source scanning, AST parsing, method-level metrics, cyclomatic complexity, nesting analysis, and class-level metrics. More advanced static-analysis features and the JavaFX interface are being added incrementally.

## License

This project is developed for educational and academic purposes.
