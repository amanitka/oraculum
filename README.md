# Oraculum

[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-2.1.1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-modulith)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Redpanda](https://img.shields.io/badge/Streaming-Redpanda%20%2F%20Kafka-FF4B4B?logo=apachekafka&logoColor=white)](https://redpanda.com/)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![DuckDB](https://img.shields.io/badge/Analytics-DuckDB-FFF000?logo=duckdb&logoColor=black)](https://duckdb.org/)
[![Vaadin 25](https://img.shields.io/badge/Frontend-Vaadin%2025-00B4F0?logo=vaadin&logoColor=white)](https://vaadin.com/)
[![Security](https://img.shields.io/badge/Security-OAuth2%20%2F%20Keycloak-009688?logo=keycloak&logoColor=white)](https://www.keycloak.org/)
[![Runtime](https://img.shields.io/badge/Runtime-K3s%20%2F%20Docker-326CE5?logo=kubernetes&logoColor=white)](https://k3s.io/)
[![Architecture](https://img.shields.io/badge/Architecture-Modular%20Monolith-7952B3)](#architecture)
[![License: Non-Commercial](https://img.shields.io/badge/License-Non--Commercial%20%2F%20Source--Available-blue.svg)](LICENSE)

An AI-powered quantitative investment analysis platform built with **Java 25**, **Spring Modulith**, **DuckDB**, **PostgreSQL**, **Redpanda**, and **Vaadin**.

Oraculum acts as your personal AI stock analyst. It orchestrates a multi-agent system to synthesize macroeconomic indicators, fundamental financial data, technical share price signals, insider trading activity, and real-time news sentiment into comprehensive, actionable investment recommendations with **deterministic data provenance**, reducing hallucination risk by separating deterministic financial calculations from LLM-based reasoning.

> [!NOTE]
> Oraculum was built as an AI-powered quantitative investment research platform to screen for high-conviction value opportunities and orchestrate multi-agent AI analysis with strict financial data auditability.
> The system currently operates on a historical dataset covering ~6,000 public companies and 13F institutional holdings, with ~20 GB of persisted data and dozens of generated company analyses.

---

## ✨ Key Features

- 📊 **Quantitative Screener**: Natively screens stocks in PostgreSQL using Piotroski F-Score, Graham Deep Value (NCAV/NNWC), GARP, and Multi-Window Sentiment decay metrics.
- 🔢 **Deterministic Ground-Truth Computation**: AI agents never perform raw financial arithmetic. All fundamental ratios, technical signals, streak analytics, and Graham value metrics are computed natively via PostgreSQL Materialized Views, while complex valuation models (such as Reverse DCF implied growth) run deterministically in Java before injection into agent fact sheets—reducing hallucination risk by separating deterministic financial calculations from LLM-based reasoning.
- 🤖 **Recursive Multi-Agent AI Analyst**: 9 specialized AI agents, an automated Critic feedback loop, and a Synthesizer analyst working together in a structured state machine.
- 🔍 **Traceable Analysis & Data Provenance**: Every reported metric can be traced back to its deterministic ground-truth data source, allowing the user to inspect the underlying database record directly from the analysis trace. Numeric citations (`[citation_id]`) link directly to ground-truth data payloads, enabling instant auditability and verification in the UI.
- ⚡ **High-Throughput ETL Pipeline**: Asynchronous Python FastStream microservice + Redpanda (Kafka) + embedded DuckDB Parquet streaming directly into PostgreSQL, bypassing traditional JVM ORM serialization overhead.
- 📄 **SEC Document Summarization**: On-demand JIT processing or offline batch processing via local LLMs (Ollama / LM Studio on local GPU hardware) for 10-K, 10-Q, 8-K, and Ex-99.1 filings, supplying qualitative context without cloud API fees or bloated context windows.
- 🛡️ **Resilient Multi-Provider LLM Routing**: Resilience4j circuit breakers providing automated fallback routing across OpenAI, Gemini, Groq, and local LM Studio / Ollama models.
- 🔐 **Enterprise Security & Governance**: Federated Single Sign-On (SSO) via **Keycloak / Google OAuth2**, role-based access control (RBAC), and per-user quota/rate limiting to protect API token consumption.
- 🌐 **Reactive Real-Time UI**: Vaadin-based reactive frontend with `@Push` WebSockets for real-time AI progress updates and interactive JSONB data grids.

---

## 🏛️ Engineering Principles

1. **Deterministic Ground Truth**  
   Financial calculations, ratio computations, and valuation models are performed natively by PostgreSQL materialized views and Java algorithms rather than LLMs.

2. **Probabilistic Reasoning**  
   LLMs are leveraged strictly for interpretation, synthesis, and qualitative analysis (e.g. 10-K/8-K document distillation)—never for raw financial arithmetic.

3. **Traceable Analysis & Data Provenance**  
   Every reported metric can be traced back to its deterministic ground-truth data source, allowing the user to inspect the underlying database record directly from the analysis trace via citation IDs.

4. **Resilient Execution**  
   Analysis runs leverage asynchronous messaging, circuit breakers, and multi-provider LLM fallbacks to guarantee robust execution despite API rate limits or external provider outages.

---

## 📸 Screenshots & UI Gallery

| Quantitative Screener | Interactive Analysis Overview |
| :---: | :---: |
| [![Screener View](docs/images/screener.png)](docs/images/screener.png) | [![Analysis Overview](docs/images/analysis_overview.png)](docs/images/analysis_overview.png) |
| *Materialized screens: Piotroski F-Score, Graham Deep Value, GARP* | *Conviction scoring (1–5), Bull/Bear drivers, and reverse DCF context* |

| Multi-Agent Scenario Modeling | Comprehensive Investment Report |
| :---: | :---: |
| [![Analysis Scenarios](docs/images/analysis_scenarios.png)](docs/images/analysis_scenarios.png) | [![Analysis Report](docs/images/analysis_report.png)](docs/images/analysis_report.png) |
| *Bull/Base/Bear scenarios modeled by specialized agents* | *Executive summary with macroeconomic context and valuation justification* |

| Company Valuation Benchmarks | Financial Ratios & Trend Analytics |
| :---: | :---: |
| [![Company Valuation](docs/images/company_valuation.png)](docs/images/company_valuation.png) | [![Company Ratios](docs/images/company_ratios.png)](docs/images/company_ratios.png) |
| *Historical valuation percentiles and multiple expansion/compression* | *Point-in-time ROCE, ROE, margins, and sequential financial health* |

| Specialist Evidence & Clickable Citations | Deep Evidence & Fact Sheet Provenance |
| :---: | :---: |
| [![Analysis Detail I](docs/images/analysis_detail_I.png)](docs/images/analysis_detail_I.png) | [![Analysis Detail II](docs/images/analysis_detail_II.png)](docs/images/analysis_detail_II.png) |
| *Interactive citation badges `[id]` linked directly to raw DB inputs* | *Cash generation, capex intensity, and quantitative audit metrics* |


---

## 🏗️ Architecture

Oraculum uses a decoupled, event-driven architecture powered by **Spring Modulith** on the backend and an asynchronous Python **Harvester** for data ingestion.

```mermaid
flowchart LR
    User([User]) <--> UI["Vaadin UI"]
    
    subgraph Spring Modulith Backend
        UI_Mod["UI Module"]
        Company["Company Module"]
        Analyst["Analyst Module"]
        Harvester_Mod["Harvester Module"]
        Load["Load Module"]
        Database["Database Module"]
        LLM["LLM Module"]
        Audit["Audit Module"]
        Economy["Economy Module"]
        User["User Module"]
        Security["Security Module"]
        Common["Common Module"]
    end
    
    subgraph Data Ingestion
        Kafka[("Redpanda Broker")]
        PythonHarvester["Python Harvester"]
        ExchangeDir[("Parquet Exchange")]
    end

    subgraph External APIs
        SimFin["SimFin API"]
        OpenInsider["OpenInsider"]
        SEC_EDGAR["SEC EDGAR"]
        FRED["FRED API"]
        AI_Models["OpenAI / Gemini / Groq"]
    end

    subgraph Persistence
        Postgres[("PostgreSQL")]
    end

    %% Core UI Flow
    UI <--> UI_Mod
    UI_Mod <--> Company
    UI_Mod --> Analyst
    UI_Mod --> Harvester_Mod
    
    %% Harvester / Ingestion Pipeline
    Harvester_Mod -- "Request" --> Kafka
    Kafka -- "Consume" --> PythonHarvester
    PythonHarvester -- "Fetch" --> SimFin
    PythonHarvester -- "Fetch" --> OpenInsider
    PythonHarvester -- "Fetch" --> SEC_EDGAR
    PythonHarvester -- "Write" --> ExchangeDir
    PythonHarvester -- "Ready Event" --> Kafka
    
    %% Data Load Pipeline
    Kafka -- "Consume" --> Load
    Load -- "Read" --> ExchangeDir
    Load -- "DuckDB ETL" --> Database
    Database -- "UPSERT" --> Postgres
    
    %% Analysis Flow
    Analyst -- "Query Data" --> Company
    Company -- "Read Views" --> Postgres
    Analyst -- "Execute Prompts" --> LLM
    LLM -- "API Call" --> AI_Models
    LLM -- "Event" --> Audit
    Audit -- "Log" --> Postgres
```

### Core Technologies & Engineering Highlights

1. **Spring Modulith 2.1 & Java 25:** Enforces strict logical boundaries between domains (`analyst`, `company`, `load`, `harvester`, `security`, `user`, etc.) communicating exclusively via Spring Application Events and exposed APIs. Complete boundary integrity is automatically verified via unit tests (`ApplicationModules.of(OraculumApplication.class).verify()`). Utilizes **Project Loom Virtual Threads** (`spring.threads.virtual.enabled: true`) for high-concurrency, non-blocking I/O.
2. **Event-Driven DuckDB C++ ETL:** A highly optimized pipeline where the Python Harvester converts financial datasets to Parquet chunks and emits Redpanda events. A Java listener attaches PostgreSQL directly into embedded DuckDB in-memory (`ATTACH '' AS pg (TYPE POSTGRES, SECRET pg_secret)`), streaming Parquet files at C++ native speed directly into PostgreSQL staging tables before executing atomic SQL `UPSERT` merges—completely bypassing JVM ORM serialization overhead.
3. **Advanced PostgreSQL Analytics & Materialized Views:** Features complex SQL materialized views calculating Piotroski F-Scores, Graham Deep Value metrics, Multi-Window Sentiment decay, and GARP screens natively in SQL. Uses Flyway database migrations with automated table partitioning for high-volume time-series data.
4. **SEC 13-F Institutional Holdings Tracking:** Tracks quarterly SEC 13-F filings for Tier-1 institutional managers (e.g., Berkshire Hathaway, Scion Asset Management, Pershing Square, Duquesne, Appaloosa, Baupost, Citadel) with automated delta calculation (`sec_holding_delta`) to surface high-conviction institutional consensus and portfolio changes.
5. **Resilient AI Routing & Circuit Breakers:** The `llm` module integrates **Spring AI** and implements Resilience4j circuit breakers, retries with exponential backoff, and multi-tier fallback routing (`Local LM Studio / Ollama (GPU) → Gemini → DeepSeek → OpenAI → Groq`) enabling resilient analysis execution with circuit breakers and multi-provider LLM fallbacks.
6. **Interactive Data Provenance & Hallucination Mitigation:** The `CitationIntegrityService` audits every bracketed citation `[citation_id]` against ground-truth fact sheet inputs using reflection over Java Records. In the Vaadin UI (`MarkdownRenderer`), citations are rendered as interactive clickable pills opening a detailed modal with the exact underlying database row, fiscal period, and filing timestamp.
7. **Enterprise Security & Governance:** Implements Spring Security 6 with federated Single Sign-On (SSO) supporting both **Keycloak** (OIDC) and **Google OAuth2**. Includes an administration panel with role-based access control (RBAC), user whitelist provisioning, and per-user token quota/rate limiting.
8. **Reactive Real-Time UI:** Built with Vaadin 25 and ApexCharts, featuring `@Push` WebSockets for real-time streaming of multi-agent state progression and interactive financial data grids.

### 💡 Core Engineering & Architectural Decisions

Building a reliable personal investment system required solving complex financial data engineering and AI reliability challenges. Here are the core technical decisions behind Oraculum:

1. **Modular Monolith vs. Microservices (Domain Boundary Isolation)**
   - *Challenge:* Maintaining a clean, scalable codebase as new domains (insider trading, macro indicators, document processing, 13F holdings) are added without microservice operational complexity.
   - *Solution:* Oraculum implements **Spring Modulith**. Domain modules (`analyst`, `company`, `load`, `harvester`, `economy`, `security`, `user`) have strict package-private boundary isolation. Inter-module communication relies on Spring Application Events and exposed API interfaces. Boundary integrity is automatically verified in unit tests via `ApplicationModules.of(...).verify()`.

2. **High-Throughput Ingestion (Bypassing JVM ORM Bottlenecks)**
   - *Challenge:* Traditional JPA/Hibernate bulk inserts suffer severe JVM garbage collection pauses and serialization overhead when processing large financial time-series datasets.
   - *Solution:* Heterogeneous event-driven pipeline. An asynchronous Python FastStream worker writes Parquet chunks to disk and emits Redpanda (Kafka) events. A Java consumer uses **embedded DuckDB** to query Parquet files at C++ native speed and stream rows directly into PostgreSQL staging tables, executing atomic SQL `UPSERT` merges.

3. **Deterministic Agentic Workflow & Automated Feedback Loop**
   - *Challenge:* Financial analysis requires multiple expert perspectives (macro, fundamental, technical, valuation, risk) working together without producing conflicting recommendations.
   - *Solution:* Structured state machine with priority execution order and a **Critic Agent review loop**. If `CriticAgent` detects logical inconsistencies or evidence misalignment, it issues targeted rerun instructions to specific specialists before transferring state to the `SynthesizerAgent`.

4. **Hallucination Detection & Interactive Data Lineage**
   - *Challenge:* Large language models inherently risk hallucinating numbers or confusing fiscal periods, which is dangerous when personal money is involved.
   - *Solution:* **Auditable Data Provenance**. Every data record in the agent fact sheet is assigned a unique `citation_id` by `CitationRegistry`. Prompt contracts enforce bracketed citation numbers (`[142]`). The post-processing `CitationIntegrityService` audits every citation against ground-truth input payloads preserved in the analysis trace JSON and flags any unverified claim with a `[?]` marker and missing sources with `[!]`. In the UI, clicking any citation pill opens an inspector showing the exact raw database record.

5. **Fault-Tolerant Multi-Provider LLM Fallback**
   - *Challenge:* Cloud AI APIs experience rate limits and transient outages, while cloud tokens are expensive for heavy tasks.
   - *Solution:* The `llm` module wraps provider calls using **Resilience4j Circuit Breakers** with automated fallback chains (`Local LM Studio / Ollama (GPU) → Gemini → DeepSeek → OpenAI → Groq`). If a primary tier fails or hits rate limits, execution gracefully degrades down the chain without halting the user's analysis workflow.

6. **Deterministic Financial Computation vs. LLM Arithmetic**
   - *Challenge:* Large language models are inherently prone to arithmetic drift, rounding mistakes, and formula misapplications when tasked with computing financial ratios or valuation models.
   - *Solution:* Oraculum implements a strict separation between **deterministic computation** and **qualitative AI reasoning** to reduce hallucination risk:
     - **Database Layer (SQL Views & MViews):** Native PostgreSQL views compute point-in-time fundamental metrics (ROCE, ROE, margins, NCAV/NNWC), YoY and sequential streaks, 9-point financial trend scores, technical indicators (50d/200d MAs, volume velocity), and Graham margin-of-safety metrics while normalizing vendor sign conventions and preventing lookahead bias.
     - **Java Domain Layer:** Algorithmic solvers (such as `ReverseDcfCalculator` and `HistoricalValuationCalculator`) iteratively compute market-implied 10-year FCF growth rates and historical valuation percentiles.
     - **AI Agent Layer:** Agents consume pre-computed facts registered in `CompanyFactSheetData` with assigned `[citation_id]` tags. Agents focus 100% on qualitative interpretation, catalyst materiality, peer comparison, and thesis synthesis rather than arithmetic.

## 🤖 Multi-Agent AI System

**0. Document Preprocessing (JIT & Offline Batch)**
- 📄 **SEC Document Processing Agent**: Performs Just-In-Time (JIT) extraction during live analysis or offline batch processing (using local LLMs via LM Studio / Ollama on local GPU hardware) on raw SEC filings (10-K/10-Q MD&A, Item 1A Risk Factors, Ex-99.1 earnings releases) to distill massive unstructured text into concise qualitative summaries and sentiment scores before specialist analysis.

**1. The Specialists**
- 🌍 **Macroeconomic Agent**: Evaluates broader economic indicators (e.g., inflation, treasury yields, GDP, interest rates from FRED) and their systemic impact on the company's sector.
- 📊 **Fundamentals Agent**: Analyzes multi-year revenue growth, profitability margins (gross, operating, net), return metrics (ROE/ROA), and sequential financial health trends.
- 💵 **Cash Flow Agent**: Evaluates operating cash flow generation, free cash flow (FCF) trajectory/yield, capital expenditure (capex) intensity, and cash conversion efficiency.
- ⚖️ **Valuation Agent**: Benchmarks historical and current valuation multiples (P/E, P/S, EV/EBITDA, P/FCF) against industry peers and performs reverse DCF modeling to determine market-implied growth expectations.
- 📈 **Share Price Agent**: Analyzes technical indicators, moving average crossovers, relative price strength, volume velocity, and price momentum.
- 🛡️ **Risk Agent**: Assesses balance sheet leverage, debt service coverage, liquidity ratios, bankruptcy indicators, and qualitative SEC risk factors.
- 🗞️ **News Agent**: Evaluates real-time news headlines, press releases, market sentiment, and qualitative catalyst materiality.
- 🕴️ **Insider Agent**: Detects cluster buying, executive insider transaction patterns, Form 4 filings, and management conviction signals.
- 🎯 **Earnings Estimates Agent**: Analyzes Wall Street consensus forward EPS and revenue projections, analyst revision momentum (7-day and 30-day net revisions), and consensus spread width.

**2. The Review Loop**
- 🧐 **Critic Agent**: Reviews the raw outputs of the specialist agents for logical inconsistencies, bias, or conflicting conclusions. If it finds issues, it instructs specific specialists to re-evaluate their data with targeted instructions, creating an automated self-correcting feedback loop.

**3. The Final Thesis**
- 🧠 **Synthesizer (Final Analyst)**: Once the Critic is satisfied, the Synthesizer compiles all verified specialist signals to deliver an in-depth, comprehensive investment thesis with a conviction score (1–5), valuation verdict, and key bull/bear drivers.

### 🔍 Traceability & Provenance
A major risk with AI in finance is data hallucination and math errors. Oraculum systematically mitigates this through two core architectural mechanisms:
1. **Deterministic Pre-Computation:** LLM agents do not perform ad-hoc arithmetic. All valuation metrics, financial ratios, growth streaks, and Reverse DCF models are computed beforehand in PostgreSQL views and Java domain services, supplying agents with pre-verified ground truth.
2. **Auditable Data Lineage:** Every metric cited by an agent (e.g., `[87]`, `[109]`) is a hard-linked citation pointing directly to the ground-truth input data payload preserved in the analysis JSON trace (`analysis.json`). A post-processing `CitationIntegrityService` verifies these citations against raw inputs, flags unverified claims with `[?]` and missing sources with `[!]`, and renders clickable inspection dialogs in the UI.

## 📄 Sample Output & Agent Trace

Curious how the multi-agent system thinks and resolves data conflicts? Check out the raw outputs from a real analysis run on AMD:

* [Raw Agent Trace (JSON)](docs/samples/analysis.json) - *Shows the full state progression, Critic interventions, and ground-truth citations.*

## 🚀 Getting Started

### Prerequisites
- **JDK 25+** (with Project Loom Virtual Threads)
- **Node.js 22+** (for Vaadin frontend build)
- **Docker Compose or K3s** (for PostgreSQL and Redpanda)
- **Python 3.14+ and `uv`** (for the FastStream Harvester)

### 1. Start Infrastructure
Run PostgreSQL and Redpanda via Docker Compose or Kubernetes:
```bash
docker-compose up -d
```

### 2. Configure Environment Variables
You will need API keys for the data providers and LLMs. Create an `.env` file or export them:
- `ORACULUM_HARVESTER_SIMFIN_API_KEY`
- `OPENAI_API_KEY` (or GEMINI/GROQ equivalents depending on your `application.yaml` config)

### 3. Start the Python Harvester
```bash
cd d:/Git/oraculum-harvestor
uv run python -m harvester
```

### 4. Run the Spring Boot Application
Due to the embedded DuckDB high-speed parquet loader, you **must** run the JVM with native access enabled:

**Using Maven:**
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED"
```

**Using the compiled JAR:**
```bash
java --enable-native-access=ALL-UNNAMED -jar target/oraculum-0.0.1-SNAPSHOT.jar
```

## 📊 Modules Overview

* **`ui`**: Vaadin-based reactive frontend with real-time analysis progress pushing via WebSockets.
* **`company`**: Core domain logic, screener strategies, and materialized view entities.
* **`analyst`**: The multi-agent LLM orchestrator.
* **`database`**: DuckDB integration and Flyway partition/maintenance management.
* **`load`**: Redpanda event consumers and Parquet to Postgres ETL pipelines.
* **`harvester`**: Request publishers and API rate limit trackers.
* **`llm`**: Generic chat client wrappers with Resilience4j circuit breakers.
* **`audit`**: Asynchronous tracking of all AI tokens consumed and data loads completed.
* **`economy`**: Macroeconomic data ingestion and analysis (e.g. FRED yield curves, inflation, unemployment).
* **`user`**: User management, usage tracking, and quotas.
* **`security`**: Authentication, authorization (OAuth2), and role-based access control.
* **`common`**: Shared domain models and utilities.

---
## 📄 License & Terms of Use

This project is licensed under the **Non-Commercial & Evaluation License**.
- **Personal & Educational**: Free to view, clone, run, and experiment with for private, educational, or portfolio evaluation purposes.
- **Commercial Restrictions**: Any commercial use, company deployment, SaaS hosting, or distribution requires prior written permission and an explicit commercial license.

For commercial licensing or inquiries: **Luděk Pokorný** ([ballnazzar@gmail.com](mailto:ballnazzar@gmail.com) | [GitHub](https://github.com/amanitka)).

---
*Disclaimer: Oraculum is a personal project intended for educational and analytical purposes. It does not constitute financial, investment, or legal advice.*
