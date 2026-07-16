<div align="center">
   <h1>Alloy Redundancy Analyzer</h1>
   <a href="https://zenodo.org/record/20795324">
      <img src="https://zenodo.org/badge/DOI/10.5281/zenodo.20795324.svg" alt="DOI Badge" />
   </a>
   <img src="https://img.shields.io/badge/Alloy-6.0%2B-brightgreen.svg" alt="Alloy Version Badge" />
   <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License Badge" />
   <hr>
   <p align="left"><strong>Alloy Redundancy Analyzer</strong> is an enhanced, customized distribution of the official <a href="https://github.com/alloytools/alloy">Alloy Analyzer</a>, designed to automatically detect, compute maximal sets of, and explain <strong>redundant constraints</strong> in Alloy specifications.</p>

   <p align="left">For more information, please refer to our <a href="https://conf.researchr.org/details/models-2026/models-2026-research-papers/20/On-Redundancy-in-Alloy-Models">MODELS 2026 paper</a>.</p>
   </p>

   <img src="./resources/GUI.png" alt="Alloy Redundancy Analyzer GUI Screenshot" />
</div>

---

---

## Key Features

- **Automated Redundancy Detection**: Instantly identify redundant formulas and constraints across your entire model (**Global Redundancy**) or within specific commands (**Local Redundancy**).
- **Maximal Redundant Sets**: Compute the largest possible subset of constraints that can be jointly removed without changing the semantic meaning or SAT/UNSAT outcomes of your commands.
- **Automated Explanation Engine**: Understand _why_ a constraint is redundant! Utilizes Delta Debugging (**DDMin**) and Native UNSAT Core Extraction (**MiniSat**) to provide minimal explanations for redundancy.
- **Seamless GUI Integration**: Built directly into the official Alloy Analyzer GUI. Analyze your models with a single click using the new **Redundancy** and **Exp. Red.** toolbar buttons.
- **Upstream Aligned**: Structured as a clean fork of `org.alloytools.alloy`, enabling straightforward reuse and future integration into the official Alloy ecosystem.

---

## Quick Start & Installation

### Build from Source (Recommended for Developers)

#### Prerequisites

- **Java 17**
- **Git**

#### Build Instructions

Clone the repository and build using the included Gradle wrapper:

```bash
git clone https://github.com/se-buw/alloy-redundancy.git
cd alloy-redundancy

# Build the project (skipping tests for a faster build)
./gradlew clean build -x test
```

Once built, launch the customized Alloy Analyzer GUI:

```bash
java -jar org.alloytools.alloy.dist/target/org.alloytools.alloy.dist.jar
```

> [!TIP]
> **VS Code Dev Containers**: This repository includes a `.devcontainer` configuration. Opening the project in VS Code with the Dev Containers extension will automatically provision your environment and build the project!


## Using the Redundancy Analyzer in the GUI

1. **Launch the Analyzer**: Run `java -jar org.alloytools.alloy.dist.jar` to open the GUI.
2. **Load an Alloy Model**: Open any `.als` specification file.
3. **Save Your Changes**: Ensure your model is saved before running redundancy checks.
4. **Analyze Redundancy**:
   - Click **`Redundancy`** in the top toolbar to analyze and highlight redundant constraints in your specification.
   - Click **`Exp. Red.`** (Explain Redundancy) to generate a minimal explanation showing which underlying formulas make a selected constraint redundant.

---



## Contributing & Architecture

This codebase is built as a modular extension to the official Alloy 6 architecture:

- `org.alloytools.alloy.application/src/main/java/de/buw/*`: Core Java packages implementing redundancy detection, DDMin explanation algorithms, and AST traversal.
- `org.alloytools.alloy.application/`: GUI modifications integrating redundancy actions into the editor toolbar.

We welcome contributions, bug reports, and pull requests! Since this project is maintained as a clean fork, our long-term goal is to contribute these redundancy analysis features upstream to the core Alloy project.

---

## Citation

If you use this tool or dataset in your academic research, please cite our MODELS 2026 paper:

```bibtex

```

---

## License

This project is licensed under the [MIT License](LICENSE).
