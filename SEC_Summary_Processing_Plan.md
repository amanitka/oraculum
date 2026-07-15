# SEC Document Analyst Integration & JIT Preprocessing Plan

## Overview
This plan implements the final phase of the SEC Document processing pipeline: integrating the extracted SEC summaries (`t_ticker_document`) directly into the AI Analyst workflow. 

It introduces a unified processing architecture that handles both background batch processing (Mass Mode) and Just-In-Time (JIT) ad-hoc processing (Phase 0), ensuring the LLMs only process what is strictly necessary.

## 1. Data Strategy & Retention

Feeding 5 years of 10-K documents to the analyst agents is inefficient, token-expensive, and prone to "lost in the middle" LLM degradation.

**Target Context Window:**
1. **The Most Recent 10-K (Annual Report)**: Deep fundamental baseline (Business Model, Risk Factors, MD&A).
2. **Subsequent 10-Qs (Quarterly Reports)**: 1-3 quarters filed *after* the most recent 10-K (momentum and trends).
3. **Recent 8-Ks (Current Events)**: Material events filed *since* the last 10-Q.

**Historical Data Retention:**
Historical documents (e.g., a 2021 10-K) will be strictly filtered out of the *current* Analyst workflow to save tokens. However, they remain highly valuable and will be kept in `t_ticker_document` for future advanced features:
- **Sentiment Trend Lines**: Plotting management's sentiment score over time.
- **Diff / Anomaly Detection**: Comparing Year-Over-Year changes to risk factors.
- **Backtesting**: Testing AI predictions using point-in-time historical data.

---

## 2. Proposed Implementation

### A. Batch Queue Optimization (Database)
Currently, the background GPU worker attempts to process *every* historical document in `t_ticker_document_raw`. Because the Analyst only needs the most recent documents, the 69,000 document backlog contains significant dead weight.

**[MODIFY] `R__01_base_metrics.sql`**
- Redefine the `v_ticker_document_pending` SQL view. 
- Use a `ROW_NUMBER() OVER (PARTITION BY ticker, document_type, document_subtype ORDER BY report_period DESC)` window function.
- Filter the view to only queue the **most recent** 10-K, the 3 most recent 10-Qs, and 8-Ks from the last 12 months. This will drastically reduce the local GPU backlog to only the essential subset.

### B. Unified Processing Service (DRY Architecture)
We will build a single, unified processing service (`SecDocumentProcessingService`) that handles both Mass and Ad-Hoc modes. 

**[NEW/MODIFY] `SecDocumentProcessingService.java`**
- The core logic takes a list of raw documents and processes them.
- **Mass Mode**: Selects from `v_ticker_document_pending`. Injects a `providerFallbackOrderOverride` containing *only* `[LMSTUDIO]` to ensure we never spend money on cloud APIs for bulk processing.
- **Ad-Hoc (JIT) Mode**: Selects specific missing documents for a requested ticker. Uses the standard system fallback (e.g., `LMSTUDIO -> GEMINI -> OPENAI`) so the user isn't blocked if their local GPU is busy during a live analysis.

### C. Workflow Orchestration: Phase 0 (JIT Preprocessing)
When an analysis is requested for a specific ticker:
1. **Check Cache (`t_ticker_document`)**: Query the DB for the target context window (recent 10-K, 10-Qs, recent 8-Ks).
2. **Identify Missing Documents**: If the target documents haven't been summarized by the background GPU worker yet, query `t_ticker_document_raw` to get the raw text.
3. **JIT Processing**: Pass the missing documents to the unified processing service in Ad-Hoc mode.
4. **Persist**: Save the newly generated summaries into `t_ticker_document`.
5. **Proceed**: Continue to the standard data gathering and Agent execution workflow.

### D. Analyst Context Injection

**[MODIFY] `CompanyTickerDocumentApi.java` & `CompanyFactSheetData.java`**
- Add methods to fetch the target context window documents for analysis.
- Expose `recent10K`, `recent10Qs`, and `recent8Ks` in the FactSheet.

**[MODIFY] Analyst Agents**
Instead of dumping all documents into every agent, route specific *subtypes* to the agents that care about them:
- **`FundamentalsAgent`**: Inject **Item 1 (Business)** and **Item 7 (MD&A)** summaries.
- **`RiskAgent`**: Inject **Item 1A (Risk Factors)** and **Item 3 (Legal Proceedings)** summaries.
- **`NewsAgent`**: Inject **recent 8-K** summaries (material news events, earnings releases, M&A).
