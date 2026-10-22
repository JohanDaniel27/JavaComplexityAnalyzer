# Java Complexity Analyzer

A Java desktop application for inspecting Java source code, measuring method and class complexity, detecting selected code smells, examining class dependencies, and summarizing potential maintenance risk.

The analyzer uses **JavaParser** to parse source files into an Abstract Syntax Tree (AST). A **JavaFX** interface lets users select a project folder and explore analysis results in tables, charts, and a dependency graph.

> **Project status:** In development. The current implementation includes source scanning, AST-based metrics, selected code-smell rules, simple-name class dependency analysis, heuristic risk scoring, and a JavaFX dashboard. See [Current limitations](#current-limitations) for important details.

## Features

### Source analysis

- Choose a Java project folder from the desktop interface.
- Recursively locate `.java` files.
- Parse source files using JavaParser.
- Analyze discovered class and interface declarations.
- Run analysis in a background task so the JavaFX interface remains responsive during processing.

### Method metrics

- Method name and source line span (start to end line)
- Parameter count and AST statement count
- Cyclomatic complexity estimate
- Cognitive complexity (SonarSource G. Ann Campbell specification)
- Maximum control flow nesting depth
- Line breakdown: Non-Comment Source Lines of Code (NCLOC), Comment Lines (CLOC), and Blank Lines

### Class metrics

- Class/interface name
- Physical line count (LOC), NCLOC, CLOC, and Blank Lines
- Method count and field count
- Total and average cyclomatic complexity
- Total and average cognitive complexity
- Maximum method nesting depth
- Weighted Methods per Class (WMC) and Response for a Class (RFC)
- Lack of Cohesion in Methods (LCOM4 via Union-Find connected components)

### Code-smell and clone detection

The smell detector flags structural flaws and attaches exact `SourceLocation` (file, start/end line) and actionable `RefactoringAdvice`:

| Code smell | Detection rule | Severity | Actionable Advice |
|---|---|---|---|
| Long Method | Method line span > 50 | High | Extract Method |
| High Cyclomatic Complexity | Cyclomatic complexity > 10 | High | Replace Conditional with Polymorphism / Extract Method |
| High Cognitive Complexity | Cognitive complexity > 15 | High | Simplify Control Flow / Guard Clauses |
| Deep Nesting | Nesting depth > 3 | Medium | Extract Method / Guard Clause |
| Too Many Parameters | Parameter count > 5 | Medium | Introduce Parameter Object |
| Large Class | Class line span > 300 | High | Extract Class |
| God Class | Methods ≥ 20, LOC ≥ 500, LCOM4 ≥ 3 | High | Apply Single Responsibility Principle / Extract Class |
| Data Class | Fields ≥ 3, avg complexity ≤ 2, low WMC | Low | Move Behavior |
| Low Cohesion | LCOM4 > 2 and Methods > 3 | Medium | Extract Class for separated components |
| Brain Method | Lines > 65 and Complexity > 8 | High | Decompose Method |
| Duplicate Code (Clones) | AST sliding-window sequence matching (Type-2, ≥6 stmts) | Info/Med | Extract Method / Pull Up Method |

### Architecture and dependency analysis

- **Type resolution:** `TypeResolver` indexes project declarations with FQNs and packages to eliminate simple-name collisions.
- **Dependency graph:** Computes incoming (afferent) and outgoing (efferent) coupling for every class.
- **Cycle detection:** Tarjan's Strongly Connected Components algorithm detects circular dependency chains at both class and package level.
- **Package metrics:** Calculates Afferent Coupling ($C_a$), Efferent Coupling ($C_e$), Instability ($I$), Abstractness ($A$), and Distance from Main Sequence ($D$).
- **Graph centrality:** Calculates in-degree, out-degree, and iterative PageRank to identify architectural hub classes.

### Git history and maintenance hotspots

- Scans Git history using **JGit** (pure Java, cross-platform, no external `git.exe` dependency).
- Measures commit frequency, total churn (lines added + deleted), distinct author count, and last commit timestamp.
- **Hotspot score:** Transparently ranks classes:
  $$\text{HotspotScore} = 0.5 \times \text{Norm}(\text{CommitCount}) + 0.5 \times \text{Norm}(\text{Complexity})$$
- Tiers: `CRITICAL_HOTSPOT`, `HIGH_HOTSPOT`, `MODERATE`, `LOW`. Gracefully degrades if the folder is not a Git repository.

### Quality regression and reporting

- **Snapshots:** Persist and reload project baselines (`.analyzer-snapshot.json`) with Jackson.
- **Regression diffs:** Track changes between baseline and current analysis (added/removed/modified classes, $\Delta\text{LOC}$, $\Delta\text{Complexity}$, new vs resolved smells).
- **Configurable quality gates:** Evaluate analysis against Standard, Strict, or Permissive rule presets.
- **Export formats:** Export comprehensive analysis results to HTML (interactive with CSS badges), JSON, CSV (`classes.csv` and `methods.csv`), and Markdown (for CI/CD pull request summaries).

### Heuristic risk score

Each class receives a score from 0 to 100 and a risk level. The score is a **project-defined heuristic**, not a calibrated prediction of defects.

| Input | Maximum contribution | Current calculation |
|---|---:|---|
| Total cyclomatic complexity | 30 | `min(30, max(0, (totalComplexity - 1) × 2))` |
| Class line span | 20 | `min(20, linesOfCode / 20)` using integer division |
| Maximum nesting depth | 15 | `min(15, nestingDepth × 3)` |
| Coupling | 20 | `min(20, coupling × 2)` |
| Detected smells | 15 | High = 5, Medium = 3, Low = 1 per finding, capped at 15 |

Risk levels:

- **LOW:** 0–20
- **MEDIUM:** 21–50
- **HIGH:** 51–75
- **CRITICAL:** 76–100

### JavaFX desktop interface

The desktop interface provides six tabs:

1. **Overview:** Executive KPI cards (Classes, Methods, NCLOC, Smells, Cycles, Clones, Quality Gate status), risk distribution pie chart, cyclomatic & cognitive complexity bar charts, and top maintenance hotspots.
2. **Classes:** Searchable, filterable table (by risk level and smell status). Selecting a class displays comprehensive metrics, detected smells with refactoring advice, and loads the source in the interactive `CodeViewerPane` with line-range highlighting.
3. **Dependencies:** Interactive class dependency graph with circular dependency edges highlighted in red, tooltips with metrics, and pan/scroll navigation.
4. **Hotspots:** Table of classes ranked by maintenance risk, showing churn, commit counts, and severity tiers.
5. **Quality & Snapshots:** Quality Gate evaluation, preset selector, baseline snapshot saving, regression comparison, and multi-format report exporter.
6. **Diagnostics:** Reports any parsing warnings or syntax problems encountered in partially analyzable projects.

### Modern Dark UI & Visual Design

The application features a modern, developer-first dark theme inspired by modern IDEs and telemetry dashboards:

- **Glassmorphism & Elevation:** Deep dark canvas (`#0d1117`), elevated card panels (`#161b22`), sleek borders (`#30363d`), and rounded containers.
- **KPI Stat Cards:** Prominent metric cards with vivid top-accent borders, glowing count badges, and clear descriptive indicators.
- **Dark-Themed Charting:** Harmonious color palette for charts (Risk distribution, Top Cyclomatic & Cognitive Complexity) with styled plot backgrounds, clean legends, and high-contrast bars.
- **Interactive Source Viewer (`CodeViewerPane`):** Monospace source display with line numbers gutter and glowing visual line-range highlights pinpointing code smell locations.
- **Dependency Canvas (`DependencyGraphView`):** Interactive canvas with dark background, smooth node rendering, directional dependency curves, circular cycle highlights in crimson, and zoom/pan controls.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 25 | Main programming language |
| Maven | Build and dependency management |
| JavaParser Core 3.28.2 | Java source parsing and AST traversal |
| JavaFX Controls 25.0.4 | Desktop interface, tables, controls, and charts |
| JavaFX CSS | Modern dark theme styling, glassmorphic cards, and custom chart palettes |
| JavaFX Maven Plugin 0.0.8 | Launching the JavaFX application with Maven |
| JGit 7.1.0 | Pure Java Git commit history and churn analysis |
| Jackson 2.18.2 | Snapshot serialization and JSON report export |
| JUnit 5.11.4 | Automated unit and integration testing framework |
| AssertJ 3.27.3 | Fluent test assertions |

## Requirements

- JDK 25
- Maven installed and available on `PATH`
- IntelliJ IDEA or another Java IDE (recommended, not required)
- Internet access on the first Maven build so dependencies can be downloaded

Verify the installations in a terminal:

```bash
java -version
javac -version
mvn -version
```

## Getting started

### 1. Clone the repository

```bash
git clone <repository-url>
cd JavaComplexityAnalyzer
```

Replace `<repository-url>` with the URL of your Git repository.

### 2. Open the project

Open the folder containing `pom.xml` in IntelliJ IDEA and allow Maven to import the project and download dependencies.

### 3. Build the project

```bash
mvn clean package
```

### 4. Launch the desktop application

From the directory containing `pom.xml`, run:

```bash
mvn clean javafx:run
```

The JavaFX window should open. Select a folder containing Java source files, click **Analyze Project**, and use the tabs to inspect the results.

The console-based entry point `com.complexityanalyzer.Main` is retained for backend checks; the desktop application entry point is `com.complexityanalyzer.ui.AnalyzerApp`.

## Architecture

```text
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
```

The analysis layer is separate from the user interface. `ProjectAnalyzer` coordinates scanning, parsing, metrics, smell detection, dependency analysis, and risk scoring. It returns a `ProjectAnalysisResult` that the JavaFX layer can display.

## Project structure

```text
JavaComplexityAnalyzer/
├── pom.xml
├── README.md
├── testcode/                                 # Sample Java source files for testing
└── src/
    ├── main/
    │   ├── java/com/complexityanalyzer/
    │   │   ├── Main.java
    │   │   ├── analysis/                     # ProjectAnalyzer, RiskCalculator, Diagnostics, Results
    │   │   ├── architecture/                 # CycleDetector, GraphCentrality, PackageGraph
    │   │   ├── dependency/                   # TypeResolver, DependencyAnalyzer, DependencyResult
    │   │   ├── export/                       # Html, Json, Csv, Markdown report exporters
    │   │   ├── git/                          # GitHistoryAnalyzer, HotspotRanker, FileGitStats
    │   │   ├── metrics/                      # Cyclomatic, Cognitive, Cohesion (LCOM4), Lines (NCLOC)
    │   │   ├── parser/                       # JavaFileParser, ProjectScanner, ParseFileResult
    │   │   ├── qualitygate/                  # QualityGateConfig, Evaluator, Violations, Results
    │   │   ├── regression/                   # SnapshotManager, SnapshotComparator, Diffs
    │   │   ├── smells/                       # CodeSmellDetector, DuplicateCodeDetector, Locations, Advice
    │   │   └── ui/                           # AnalyzerApp, DashboardCharts, DependencyGraphView, CodeViewerPane
    │   └── resources/
    │       └── styles/
    │           └── dark-theme.css            # Dark theme styling, UI design tokens, chart palette
    └── test/
        ├── java/com/complexityanalyzer/      # 10 test suites covering all analyzers & engine
        └── resources/samples/                # Test fixture files (simple, complex, cycles, broken)
```

## Design choices

- **AST-based analysis:** analyse parsed Java constructs rather than counting keywords in raw text.
- **Separation of concerns:** scanning, parsing, metrics, smells, dependencies, risk scoring, git hotspot ranking, regression diffs, and presentation are separate components.
- **Reusable result models:** method, class, and project results are represented as objects so the console, exporters, and GUI consume the same analysis output.
- **Background analysis:** the JavaFX interface uses a background task with live stage progress callbacks.
- **Transparent heuristics:** rules and score contributions are documented and customizable via Quality Gate presets.
- **Robust error handling:** parse failures produce diagnostics and do not crash the analysis run.

## License

No license has been specified yet. Add a `LICENSE` file before distributing the project as open-source software.
