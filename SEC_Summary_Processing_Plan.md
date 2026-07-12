# SEC Document Summary & Extraction Pipeline — Final Implementation Plan

## Overview

This plan adds **LLM-powered summarization** of raw SEC filings (`t_ticker_document_raw`) that have already been harvested and pre-segmented into individual sections (ITEM_1A, ITEM_7, EX99_1) by the Python Harvester. Each subtype is processed with a **dedicated prompt** that extracts subtype-specific structured JSON, producing richer insights than a one-size-fits-all schema. Results are persisted to a new `t_ticker_document` table.

### Processing Modes

| Mode | Trigger | Provider Behavior | Use Case |
|---|---|---|---|
| **Batch / Mass** | UI button "Process Pending" on Refresh page | `[LMSTUDIO]` only — no cloud fallback | Initial bulk load via free local Gemma 4 12B. If LM Studio is down, documents are skipped |
| **On-Demand** | Analyst agent needs summaries during analysis (future phase) | Global fallback `[LMSTUDIO, GEMINI, OPENAI, GROQ]` | Real-time gap-fill — tries local first, falls back to cloud |

---

## Proposed Changes

### 1. Database Module

#### [NEW] `V20__t_ticker_document.sql`

Lean table — only `sentiment_score` as a top-level extracted column. All other insights live in the subtype-specific `summary` JSON blob.

```sql
CREATE TABLE public.t_ticker_document (
    id                     VARCHAR(64)  NOT NULL,  -- Same SHA256 hash as t_ticker_document_raw.id
    ticker                 VARCHAR(16)  NOT NULL,
    market                 VARCHAR(10)  NOT NULL DEFAULT 'US',
    document_type          VARCHAR(20)  NOT NULL,  -- '8K', '10K'
    document_subtype       VARCHAR(50)  NOT NULL,  -- 'ITEM_1A', 'ITEM_7', 'EX99_1'
    report_period          DATE         NOT NULL,  -- Partition key
    summary                TEXT         NOT NULL,  -- Subtype-specific structured JSON
    sentiment_score        REAL         NOT NULL,  -- Universal: -1.0 (bearish) to +1.0 (bullish)
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, report_period)
) PARTITION BY RANGE (report_period);

CREATE INDEX ix_ticker_document_lookup
    ON public.t_ticker_document (ticker, market, document_type, report_period DESC);
```

**Design Rationale:**
- `sentiment_score` is top-level because it's universal across all subtypes and useful for screener filtering/aggregation.
- Everything else (revenue drivers, risks, guidance, metrics) lives in the `summary` JSON. Different subtypes produce different JSON schemas — this is intentional. Schema evolves by editing prompts, not migrations.

#### [MODIFY] `R__00_partition_management.sql`

```sql
SELECT create_yearly_partitions('t_ticker_document', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);
```

#### [MODIFY] `PartitionConfig.java`

Add: `TickerDocument("t_ticker_document", PartitionType.YEARLY, 24, 120)`

#### [MODIFY] `R__01_base_metrics.sql` — Prioritized Processing Queue

To prevent dependency dropping during rebuilds (since cascading drops on `mv_share_price_signals_recent` would otherwise leave orphaned views in un-run scripts), we define the `v_ticker_document_pending` view directly at the end of `R__01_base_metrics.sql` (after the materialized view `mv_share_price_signals_recent` is created):

```sql
CREATE OR REPLACE VIEW v_ticker_document_pending AS
SELECT
    r.id,
    r.ticker,
    r.market,
    r.document_type,
    r.document_subtype,
    r.report_period,
    r.filing_date,
    r.content,
    c.company_name,
    COALESCE(s.market_capitalization, 0) AS market_capitalization,
    COALESCE(s.company_size, 'MICRO')   AS company_size
FROM t_ticker_document_raw r
JOIN t_company c ON c.ticker = r.ticker AND c.market = r.market
LEFT JOIN mv_share_price_signals_recent s ON s.company_id = c.id
    AND s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = c.id)
WHERE r.status = 'PENDING'
ORDER BY COALESCE(s.market_capitalization, 0) DESC, r.ticker, r.report_period DESC;
```

With a batch limit of 50, this processes **all** filings for the most important companies first before moving to the next. For example:
- Batch 1 (50 docs): All Apple filings (ITEM_7, ITEM_1A, EX99 × multiple periods), then top Microsoft filings
- Batch 2 (50 docs): Remaining Microsoft, then NVIDIA, then Google...
- Later batches: Smaller companies

---

### 2. Company Module — Data Access Layer

Currently there is **no JPA entity** for `t_ticker_document_raw` — data is loaded exclusively via the `load` module's Parquet bulk upsert. We now need a read-only entity for the prioritized processing queue view.

#### [NEW] `TickerDocumentPendingEntity.java`

`@Immutable` JPA entity mapped to the **view** `v_ticker_document_pending`. This gives us prioritized pending documents with company name and market cap, ready for the processing service.

#### [NEW] `TickerDocumentPendingRepository.java`

- `findPendingDocuments(Pageable)` — returns prioritized pending documents (large cap first, view handles ordering)

#### [NEW] `TickerDocumentRawRepository.java`

- `updateStatus(id, reportPeriod, status)` — `@Modifying` native query on `t_ticker_document_raw` to update status to `PROCESSED` or `FAILED`

#### [NEW] `TickerDocumentEntity.java`

Writable JPA entity for `t_ticker_document`.

#### [NEW] `TickerDocumentRepository.java`

CRUD with `findByTickerAndMarketOrderByReportPeriodDesc()`.

#### [MODIFY] `CompanyTickerDocumentApi.java`

Add 4 new methods:
```java
List<TickerDocumentRawDto> getPendingRawDocuments(int limit);
void saveDocumentSummary(TickerDocumentDto summary);
void updateRawDocumentStatus(String id, LocalDate reportPeriod, String status);
List<TickerDocumentRawDto> getPendingRawDocumentsByTicker(String ticker, String market);
```

#### [MODIFY] `CompanyTickerDocumentServiceImpl.java`

Implement the 4 new methods. `getPendingRawDocuments` delegates to the view-backed `TickerDocumentPendingRepository`.

#### [NEW] DTOs: `TickerDocumentRawDto`, `TickerDocumentDto`

#### Transaction Design — Commit Per Document

Each document is committed independently. If document #27 fails, the previous 26 are already persisted. No batch rollback.

```
processPendingDocuments()          ← NOT @Transactional (just a loop)
  ├─ document 1: LLM call → saveAndMarkProcessed() → COMMIT ✅
  ├─ document 2: LLM call → saveAndMarkProcessed() → COMMIT ✅
  ├─ document 3: LLM call → FAILS → markFailed()   → COMMIT ✅  (no rollback of 1 & 2)
  └─ document 4: LLM call → saveAndMarkProcessed() → COMMIT ✅
```

Implementation:
- `saveDocumentSummary()` — `@Transactional(propagation = REQUIRES_NEW)`: saves summary to `t_ticker_document` AND updates raw status to `PROCESSED` atomically in one transaction.
- `updateRawDocumentStatus()` — `@Transactional(propagation = REQUIRES_NEW)`: used in the `catch` block to mark `FAILED` independently.
- The outer `processPendingDocuments()` in `SecDocumentProcessingService` has **no** `@Transactional` annotation — it's a plain loop that catches exceptions per document and continues.

---

### 3. LLM Module — Provider, Routing & Health

#### [MODIFY] `LlmProviderType.java`

Add `LMSTUDIO`.

#### [MODIFY] `LlmHealthProvider.java` — Exponential Backoff

Replace fixed 30s block with exponential backoff:

```java
@Slf4j
@Component
public class LlmHealthProvider {

    private static final long INITIAL_BLOCK_MS = 30_000;       // 30 seconds
    private static final long MAX_BLOCK_MS = 30 * 60 * 1000;   // 30 minutes cap
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final Map<LlmProviderType, State> state = new ConcurrentHashMap<>();

    public boolean isBlocked(LlmProviderType provider) {
        State s = state.get(provider);
        return s != null && System.currentTimeMillis() < s.blockedUntil;
    }

    public void markFailure(LlmProviderType provider) {
        State s = state.computeIfAbsent(provider, _ -> new State());
        s.consecutiveFailures++;
        long blockMs = Math.min(
            (long)(INITIAL_BLOCK_MS * Math.pow(BACKOFF_MULTIPLIER, s.consecutiveFailures - 1)),
            MAX_BLOCK_MS);
        s.blockedUntil = System.currentTimeMillis() + blockMs;
        log.warn("Provider {} failed (attempt {}). Blocked for {}s.",
                provider, s.consecutiveFailures, blockMs / 1000);
    }

    public void markSuccess(LlmProviderType provider) {
        state.remove(provider);  // Full reset on success
    }

    private static class State {
        volatile long blockedUntil;
        volatile int consecutiveFailures;
    }
}
```

**Backoff:** 30s → 60s → 2min → 4min → 8min → 16min → 30min (cap). First success → full reset.

Note: `markFailure` signature changes from `markFailure(provider, blockMs)` to `markFailure(provider)`. Callers in `LlmRouterServiceImpl` updated accordingly.

#### [MODIFY] `LlmCallRequest.java`

Add `List<LlmProviderType> providerFallbackOrderOverride` (nullable). Existing factory methods pass `null`. New factory:

```java
public static <T> LlmCallRequest<T> withFallbackOverride(
        LlmTierType tier, String prompt, Class<T> responseType,
        UUID correlationId, CorrelationType correlationType, String source,
        List<LlmProviderType> fallbackOrder)
```

#### [MODIFY] `LlmRouterServiceImpl.java`

Two changes:

**A) Tier-aware skipping** — `resolveModel` returns `null` (skip) instead of throwing when a provider has no model for a tier:

```java
private String resolveModel(LlmTierType tier, LlmProviderType provider) {
    Map<LlmProviderType, String> providerMap = properties.models().get(tier);
    if (providerMap == null) {
        throw new IllegalArgumentException("Configuration missing for tier: " + tier);
    }
    return providerMap.get(provider);  // null → skip this provider
}
```

**B) Fallback override** — use `request.providerFallbackOrderOverride()` if present, else global default.

**C) Skip on null model** — add `if (model == null) { continue; }` in the provider loop.

**Result:** LMSTUDIO is in the global fallback order but only has MINI model → automatically skipped for STANDARD/PRO calls.

#### [MODIFY] `application.yaml`

```yaml
oraculum:
  llm:
    common:
      provider-fallback-order:
        - lmstudio     # Tried first for MINI; auto-skipped for STANDARD/PRO
        - gemini
        - openai
        - groq
    providers:
      lmstudio:
        base-url: "http://localhost:1234/v1"
        api-key: "lmstudio"
    models:
      mini:
        lmstudio: "gemma-4-12b"      # Only MINI tier
        gemini: "gemini-3.1-flash-lite"
        groq: "llama-3.1-8b-instant"
        openai: "gpt-5.4-nano"
      standard:                       # No lmstudio → auto-skipped
        gemini: "gemini-3.5-flash"
        groq: "llama-3.1-70b-versatile"
        openai: "gpt-5.4-mini"
      pro:                            # No lmstudio → auto-skipped
        gemini: "gemini-3.1-pro-preview"
        groq: "llama-3.3-70b"
        openai: "gpt-5.4"
```

---

### 4. Analyst Module — Subtype-Specific Processing

#### [MODIFY] `PromptType.java`

Add three new prompt types:
```java
SEC_ITEM_7("/prompt/sec_item_7.md"),
SEC_ITEM_1A("/prompt/sec_item_1a.md"),
SEC_EX99("/prompt/sec_ex99.md"),
```

#### [NEW] `sec_item_7.md` — MD&A Extraction

Extracts from Management Discussion & Analysis:
```json
{
  "summary": "Concise narrative of key operational performance and outlook...",
  "primary_revenue_drivers": ["driver 1", "driver 2"],
  "margin_trends": "Description of margin expansion/contraction and drivers",
  "segment_highlights": ["Americas +6%", "Greater China -3%"],
  "guidance_signal": "RAISED | LOWERED | MAINTAINED | NONE",
  "management_outlook": "Forward-looking management commentary",
  "capital_allocation": ["$90B buyback authorized", "Dividend increased 4%"],
  "key_metrics": {"revenue_growth_yoy_pct": 8.5, "gross_margin_pct": 46.2},
  "sentiment_score": 0.65
}
```

#### [NEW] `sec_item_1a.md` — Risk Factor Extraction

Extracts from Risk Factors:
```json
{
  "summary": "Concise narrative of the company's risk landscape...",
  "material_risks": [
    {"risk": "China regulatory pressure", "category": "REGULATORY", "severity": "HIGH"},
    {"risk": "AI infrastructure costs", "category": "OPERATIONAL", "severity": "MODERATE"}
  ],
  "risk_level": "LOW | MODERATE | HIGH | CRITICAL",
  "new_risks": ["Risks not present in prior filings"],
  "mitigations": ["Steps management is taking to address risks"],
  "sentiment_score": -0.35
}
```

#### [NEW] `sec_ex99.md` — Press Release / Earnings Extraction

Extracts from Exhibit 99 press releases:
```json
{
  "summary": "Concise narrative of the earnings release or announcement...",
  "headline": "Apple Reports Record Q1 2026 Results",
  "reported_metrics": {"revenue_b": 124.3, "eps": 2.41, "net_income_b": 36.3},
  "guidance": {"next_quarter_revenue_range": "89B-93B", "signal": "RAISED"},
  "key_announcements": ["$110B buyback program", "AI partnership expanded"],
  "management_tone": "Confident, emphasized AI monetization",
  "sentiment_score": 0.72
}
```

#### [NEW] `SecDocumentProcessingApi.java`

```java
public interface SecDocumentProcessingApi {
    int processPendingDocuments(int limit, List<LlmProviderType> providerFallbackOrder);
}
```

#### [NEW] `SecDocumentProcessingService.java`

Core service:
1. Fetches pending raw documents via `CompanyTickerDocumentApi.getPendingRawDocuments(limit)`.
2. Selects the correct prompt based on `documentSubtype` (ITEM_7 → `sec_item_7.md`, etc.).
3. Calls `LlmRouterApi.executeCall()` with the selected prompt and fallback override.
4. Parses JSON response, extracts `sentiment_score` from within the JSON.
5. Saves to `t_ticker_document` and marks raw as `PROCESSED`.
6. On failure: marks raw as `FAILED`, continues to next document.

```java
private PromptType resolvePromptType(String documentSubtype) {
    return switch (documentSubtype) {
        case "ITEM_7"  -> PromptType.SEC_ITEM_7;
        case "ITEM_1A" -> PromptType.SEC_ITEM_1A;
        case "EX99_1"  -> PromptType.SEC_EX99;
        default -> throw new IllegalArgumentException("Unsupported subtype: " + documentSubtype);
    };
}
```

Since the JSON schema varies per subtype, the LLM response is parsed as a generic `Map<String, Object>` (or raw `String`). The `sentiment_score` is extracted from the parsed JSON to populate the top-level column.

#### [MODIFY] `CorrelationType.java`

Add `SEC_DOCUMENT_SUMMARY`.

---

### 5. UI Module — Refresh Page

#### [MODIFY] `RefreshView.java`

New tile **"SEC Document Summaries"**:
- NumberField: batch limit (default 50)
- Button: **"Process Pending"** → `secDocumentProcessingApi.processPendingDocuments(limit, List.of(LMSTUDIO))`
- Strict local-only: if LM Studio is offline, documents are marked FAILED, not sent to cloud

---

### 6. On-Demand Processing (Future Phase)

Will be implemented after the batch flow is validated.

When an analyst agent needs SEC summaries for a ticker:
1. Query `t_ticker_document` for the ticker.
2. If missing, process pending raw docs using default fallback (tries LMSTUDIO first, falls back to cloud).
3. Feed the subtype-specific JSON into the agent's context.

---

## Impact on Agentic Analysis Workflow

### What Agents Lack Today vs. What They'll Get

| Agent | Current Data Sources | New Data from SEC Summaries | Impact |
|---|---|---|---|
| **Fundamentals** | Income/balance sheet ratios | ITEM_7: revenue drivers, margin trends, segment performance | Can explain *why* ratios changed, not just *that* they changed |
| **Risk** | Financial ratios, news | ITEM_1A: categorized risks with severity, new vs. recurring risks | Primary risk source instead of inferring from ratios |
| **Valuation** | Ratios, earnings estimates | EX99: forward guidance signal (RAISED/LOWERED), reported metrics | Guidance directly impacts DCF/target price |
| **Cash Flow** | Cash flow statements | ITEM_7: capital allocation (buybacks, dividends, capex plans) | Management's stated capital allocation intentions |
| **Synthesizer** | All agent outputs | Cross-references management claims against actual financial data | Can flag contradictions ("mgmt claims margin expansion but ratios show contraction") |
| **News** | News articles + sentiment | EX99: official press releases with management tone | Separates official company narrative from media interpretation |

### Quantitative Impact Estimate

- **Data completeness**: Adds ~30% more context to each company analysis (qualitative management narrative + forward guidance)
- **Risk assessment quality**: Moves from inferred risks (ratios/news) to **authoritative disclosed risks** (ITEM_1A)
- **Valuation accuracy**: Guidance signals are the #1 driver of forward estimates — currently completely absent

---

## File Change Summary

| Module | File | Action | Description |
|--------|------|--------|-------------|
| database | `V20__t_ticker_document.sql` | NEW | Summary table migration |
| database | `R__00_partition_management.sql` | MODIFY | Add yearly partitions |
| database | `R__06_sec_document_pending.sql` | NEW | Prioritized processing queue view |
| database | `PartitionConfig.java` | MODIFY | Add enum entry |
| company | `TickerDocumentPendingEntity.java` | NEW | Read-only entity for prioritized queue view |
| company | `TickerDocumentPendingRepository.java` | NEW | Query prioritized pending docs |
| company | `TickerDocumentRawRepository.java` | NEW | Update raw document status |
| company | `TickerDocumentEntity.java` | NEW | Writable entity for summaries |
| company | `TickerDocumentRepository.java` | NEW | CRUD for summaries |
| company | `CompanyTickerDocumentApi.java` | MODIFY | Add 4 new methods |
| company | `CompanyTickerDocumentServiceImpl.java` | MODIFY | Implement new methods |
| company | `TickerDocumentRawDto.java` | NEW | DTO for raw documents |
| company | `TickerDocumentDto.java` | NEW | DTO for summaries |
| llm | `LlmProviderType.java` | MODIFY | Add `LMSTUDIO` |
| llm | `LlmCallRequest.java` | MODIFY | Add fallback override field |
| llm | `LlmHealthProvider.java` | MODIFY | Exponential backoff |
| llm | `LlmRouterServiceImpl.java` | MODIFY | Tier-aware skip + fallback override |
| analyst | `SecDocumentProcessingApi.java` | NEW | Public API interface |
| analyst | `SecDocumentProcessingService.java` | NEW | Core processing service |
| analyst | `PromptType.java` | MODIFY | Add 3 prompt types |
| llm | `CorrelationType.java` | MODIFY | Add `SEC_DOCUMENT_SUMMARY` |
| resources | `sec_item_7.md` | NEW | MD&A extraction prompt |
| resources | `sec_item_1a.md` | NEW | Risk factor extraction prompt |
| resources | `sec_ex99.md` | NEW | Press release extraction prompt |
| ui | `RefreshView.java` | MODIFY | Add processing tile |
| config | `application.yaml` | MODIFY | Add lmstudio provider + model |

---

## Verification Plan

### Automated Tests
- `LlmHealthProviderTest`: Verify exponential backoff progression, cap, and reset on success.
- `LlmRouterServiceImplTest`: Verify tier-aware skipping, fallback override, updated `markFailure()`.
- `SecDocumentProcessingServiceTest`: Mock APIs, verify prompt selection by subtype, sentiment extraction from JSON, status updates.
- `CompanyTickerDocumentServiceImplTest`: Verify DTO mapping and persistence.

### Manual Verification
- `mvn clean test` — all tests pass.
- Start LM Studio with Gemma 4 12B → trigger "Process Pending" → verify subtype-specific summaries in DB.
- Shut down LM Studio → trigger again → documents marked FAILED, no cloud calls.
- Run standard MINI-tier analysis → verify LMSTUDIO tried first, falls back to GEMINI if offline.
