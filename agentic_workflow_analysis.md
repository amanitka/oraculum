# Oraculum Agentic Workflow — Critical Analysis & Improvement Proposals

**Date:** 2026-06-11  
**Scope:** `CompanyAnalysisWorkflowService`, `AgentContext`, `CompanyFactSheetData`, all specialist agent implementations, all prompt files, new database views (`v_company_financial_ratios`, `v_share_price_signals`, `v_ticker_news_sentiment`)

---

## 1. Executive Summary

The current multi-agent pipeline is architecturally sound: a Planner routes to specialists, a Critic finds contradictions, and a Synthesizer produces a final report. However, a significant amount of **new analytical value created in the database** (`v_company_financial_ratios`, `v_share_price_signals`) **is not being fully utilized by the LLM agents**. Prompt inputs are noisy (they include irrelevant columns), the Planner's decisioning is shallower than the data supports, the News agent is underequipped for the new `v_ticker_news_sentiment` view, and serialization of full raw DTOs creates token waste and potentially misleads the models. Concrete, actionable improvements are proposed in each section below.

---

## 2. Workflow Architecture Review

### 2.1 Current Pipeline

```
CompanyFactSheetDataService.create()
  └─> AgentContext (company + factSheetData + metadata)
        ├─ [STANDARD] PlannerAgent       → PlannerPlan (variant selection + analysis_focus)
        ├─ [MINI]     FundamentalsAgent  → FundamentalsAgentOutput
        ├─ [MINI]     CashFlowAgent      → CashFlowAgentOutput
        ├─ [MINI]     ValuationAgent     → ValuationAgentOutput
        ├─ [MINI]     SharePriceAgent    → SharePriceAgentOutput
        ├─ [MINI]     NewsAgent          → NewsAgentOutput
        ├─ [MINI]     RiskAgent          (reads SharePrice output)
        ├─ [PRO]      CriticAgent        (reads all specialists)
        └─ [PRO]      SynthesizerAgent   (reads all specialists + critic)
```

### 2.2 Structural Strengths

- Clean separation of concerns via `Agent<T>` interface.
- `AgentContext` is a single, immutable-ish shared object — good.
- `PlannerPlan` drives variant selection per-agent, preventing one-size-fits-all data queries.
- Critic → Synthesizer feedback loop is a well-established pattern.
- LLM tier routing (MINI/STANDARD/PRO) is appropriate for most agents.
- Lazy caching in `CompanyFactSheetData` avoids redundant serialization.

### 2.3 Structural Weaknesses

| Issue | Severity | Detail |
|---|---|---|
| Sequential specialist execution | Medium | All specialists run one-by-one in a single thread. News and Fundamentals are fully independent — they could run in parallel. |
| No retry/fallback at agent level | Medium | If a single specialist throws, the entire workflow fails. There is no per-agent graceful degradation. |
| Critic is fire-and-forget | Medium | Currently, if `is_consistent = false`, the workflow doesn't re-run specialists. *(Note: This is already correctly identified and planned as "Strategy 3: Critique-and-Refine Loop" in your `advanced_strategies_plan.md`, which is an excellent fix).* |
| Planner output is mostly routing | Low-Med | `PlannerPlan` only decides `StatementVariant`. It does not decide *how many years of history* each agent needs, which agents to skip (e.g. skip News if no articles), or a qualitative investment hypothesis. |
| `AgentContext` stores `agentOutputs` as `Map<AgentType, Object>` | Low | Type erasure means every consumer has to cast. This is a minor maintainability smell. |
| Unused Agents & Prompts | Low | `AgentType.NEWS_SUMMARY` and `PromptType.FACTSHEET` are defined but unused/not implemented as active agents in the pipeline. Dead code / confusion risk. |

---

## 3. AgentContext Analysis

### 3.1 Current Shape

```java
public record AgentContext(
    CompanyDto company,
    CompanyFactSheetData factSheetData,
    LocalDate analysisDate,
    StatementVariant defaultStatementVariant,
    Map<AgentType, StatementVariant> statementVariants,
    int tokenBudget,
    Map<AgentType, Object> agentOutputs
)
```

### 3.2 What Is Missing

The context carries company data and per-agent variant choices, but it does **not carry the Planner's `analysisFocus`** — the single most strategic signal the Planner produces. Every specialist receives identical instructions regardless of whether the focus is "P/E at 10-year low" or "volume spike near 200-day SMA". The `analysisFocus` is computed, stored in the `agentTrace`, but never injected into specialist prompts.

**Proposed addition:**

```java
public record AgentContext(
    CompanyDto company,
    CompanyFactSheetData factSheetData,
    LocalDate analysisDate,
    StatementVariant defaultStatementVariant,
    Map<AgentType, StatementVariant> statementVariants,
    int tokenBudget,
    Map<AgentType, Object> agentOutputs,
    String analysisFocus,             // NEW: from PlannerPlan
    int historyYearsForSpecialists    // NEW: planner-controlled depth
)
```

Every specialist prompt should include a `{{ analysis_focus }}` placeholder that injects this one sentence. This ensures all agents are oriented toward the same investment thesis — reducing contradictions the Critic must resolve and improving Synthesizer coherence.

### 3.3 `factSheetData` as the Single Point of Data Truth

The `CompanyFactSheetData` is passed to all agents. This is acceptable but means each agent is handed the **entire universe of data** and must ignore what it does not need. See section 5 on DTO slimming.

---

## 4. Planner Agent Analysis

### 4.1 What It Currently Does

The Planner receives:
- `daily_share_price_signals` — 30 days of price signals (full `SharePriceSignalDto`, ~50 columns)
- `company_profile` — full `CompanyDto` (13 fields including `cik`, `isin`, `extractedAt`, etc.)

It outputs:
- 4x `StatementVariant` selections
- 1x `analysis_focus` sentence

### 4.2 Critical Problems

**Problem 1: Token waste on the Planner's input.**  
The Planner is fed 30 days × ~50 columns of daily price data just to determine a variant and write one sentence. The Planner does not need `isGrahamNetNet`, `isCashEarnings`, `isNegativeEquity`, `positiveFcfStreak`, `enterpriseValueToFreeCashFlow`, etc. It needs: `trade_date`, `share_price`, `pct_from_50d_ma`, `pct_from_200d_ma`, `volume_velocity`, `pe_ratio`, `composite_signal`. This is ~10 of the 50 columns.

**Problem 2: The Planner does not use the richest signal it has access to.**  
`v_share_price_signals` now computes a `composite_signal` (`STRONG_BUY`, `BUY`, `HOLD`, `AVOID`) and a `quality_score`. These are high-value pre-computed signals the Planner should explicitly reference when formulating `analysis_focus`. The prompt says *"derive the analysis_focus sentence"* but does not tell the model what the composite signal means or that it is a pre-computed ranking signal.

**Problem 3: The Planner does not control data depth.**  
The history depth is configured globally in `application.yaml` as `fact-sheet.history-limit: 3660` (10 years). The Planner has no say on how many years a specialist should receive. For a volatile biotech startup, 10 years of data may not even exist and 2 years of quarterly data is far more relevant than 10 annual periods. For a utility or industrials company, the reverse is true.

**Proposed Planner Enhancement:**

```json
{
  "fundamentals_variant": "annual",
  "cash_flow_variant": "annual",
  "valuation_variant": "ttm",
  "risk_variant": "quarterly",
  "fundamentals_history_years": 5,
  "cash_flow_history_years": 7,
  "share_price_monthly_history_years": 10,
  "analysis_focus": "..."
}
```

The planner can be instructed with a lookup table in its prompt:
- Fast-growing tech / biotech: 3–5 years historical data preferred
- Cyclicals / industrials: 7–10 years to capture a full cycle
- Stable dividend compounders: 7–10 years
- Newly listed / small-cap: take whatever is available, do not force 10-year view

This avoids both token waste and the problem of feeding irrelevant ancient data to agents analyzing recent momentum shifts.

---

## 5. Data Serialization & DTO Analysis (Your Main Question)

### 5.1 The Problem: Full DTO Serialization

Currently the following DTOs are serialized verbatim into prompts:

| Agent | Input Data | Key Problem |
|---|---|---|
| **Planner** | `SharePriceSignalDto` × 30 days | ~50 columns, model must ignore 40 of them |
| **Planner** | `CompanyDto` | `cik`, `isin`, `extractedAt` are noise |
| **Fundamentals** | `IncomeStatementDto.statementData` (raw JSON string), `CompanyFinancialRatiosDto` × N years | Raw `statementData` JSON is a blob with named GAAP lines — this is OK but ratios DTO has `id`, `companyId`, `ticker`, `template`, `variant` as overhead |
| **Share Price** | `SharePriceSignalDto` × 30 days (daily) + × 120 months (monthly) | Monthly: 120 × ~50 columns is a massive context; most columns are irrelevant for trend analysis |
| **Valuation** | `CompanyFinancialRatiosDto` + `SharePriceSignalDto` | Duplicates data already in ratios; `id`, `companyId`, `restatedDate` etc. are noise |
| **Risk** | `BalanceSheetDto.statementData` + `CompanyFinancialRatiosDto` | Same overhead |
| **Cash Flow** | `CashFlowStatementDto.statementData` + `CompanyFinancialRatiosDto` | Same overhead |

### 5.2 Should You Use Custom DTOs? — **Yes, Absolutely**

**The models are smart enough to understand field names from context, but that is not the issue.**

The real costs of full-DTO serialization are:

1. **Token cost** — feeding 50 columns × 120 months to the SharePrice agent for monthly trend analysis is roughly 10–20k tokens of data, most of which is `null` values or metadata fields (`companyId`, `flagLastDayOfMonth`, `market`, `currency`, etc.) the LLM will ignore anyway.

2. **Attention dilution** — LLMs have limited attention and are statistically more likely to miss an important signal buried in a 10k-token JSON blob than in a 1k-token curated JSON. Empirically, the less irrelevant data you send, the more reliably models pick up on subtle but important patterns.

3. **Misleading metadata** — Fields like `id`, `companyId`, `template`, `restatedDate`, `cik`, `isin` are internal database keys. They carry no analytical meaning. When the model sees these, it may attempt to reason about them (e.g., comparing company IDs across periods) or simply allocate attention to them wastefully.

4. **Cost** — Every wasted token costs money on paid API tiers.

### 5.3 Proposed Agent-Specific Input Views

**For the Planner (daily price signals — summarized):**
```json
// Only 10 fields needed
{
  "trade_date": "...",
  "share_price": ...,
  "pct_from_50d_ma": ...,
  "pct_from_200d_ma": ...,
  "volume_velocity": ...,
  "pe_ratio": ...,
  "composite_signal": "BUY",
  "quality_score": ...,
  "piotroski_f_score": ...,
  "revenue_yoy_growth": ...
}
```

**For the Fundamentals Agent (ratios — slim):**
```json
// Drop: id, companyId, ticker, template, variant, restatedDate, ncav, netNetWorkingCapital
// Keep: fiscalYear, fiscalPeriod, reportDate, revenue, netIncome, ebitda, freeCashFlow,
//        returnOnEquity, netMargin, grossMargin, operatingMargin, fcfMargin,
//        revenueYoyGrowth, netIncomeYoyGrowth, epsYoyGrowth, piotroskiFScore,
//        marginExpansionSignal, earningsQualityRatio
```

**For the SharePrice Agent (monthly — slim):**
```json
// Drop: companyId, flagLastDayOfMonth, market, currency, activeReportPublishDate,
//        isGrahamNetNet, isGrahamDefensive, activeFiscalYear, activeFiscalPeriod,
//        priceToNcav, priceToNnwc, isCashEarnings, isNegativeEquity, grahamMarginOfSafety
// Keep: tradeDate, sharePrice, pctFrom50dMa, pctFrom200dMa, volumeVelocity,
//        peRatio, priceToBook, priceToSales, priceToFcf, earningsYield, fcfYield,
//        enterpriseValueToEbitda, returnOnEquity, revenueYoyGrowth, piotroskiFScore,
//        compositeSignal, qualityScore
```

**For the Valuation Agent:**
```json
// companyFinancialRatios: fiscalYear, fiscalPeriod, ebitda, freeCashFlow, returnOnEquity,
//   netMargin, fcfMargin, revenueYoyGrowth, epsYoyGrowth, piotroskiFScore
// sharePriceSignals: only the 5 most recent daily rows, fields: tradeDate, sharePrice,
//   peRatio, priceToBook, priceToSales, priceToFcf, earningsYield,
//   enterpriseValueToEbitda, qualityScore, compositeSignal
```

### 5.4 Implementation Options (Aligning with your Advanced Strategies Plan)

In your `advanced_strategies_plan.md`, you outlined **Strategy 2: Pre-computed Structured Signals**. Your plan proposed creating a `TechnicalIndicatorsDto` and manually computing fields like `lowestVs50` in Java.

**Good news:** Your new database views (`v_share_price_signals`, `v_company_financial_ratios`) have already accomplished the heavy lifting of Strategy 2 directly in SQL! You no longer need to compute these in Java. 

However, the *intent* of Strategy 2—passing a clean, structured DTO instead of raw, noisy data—is still the exact right approach to solve the serialization bloat.

**Option A — Jackson `@JsonView` annotations** on the existing DTOs  
Define named views (`PlannerView`, `SharePriceAgentView`, etc.) and serialize with the appropriate view. Least invasive, no new classes.

**Option B — Dedicated agent input record classes** (Highly Recommended / Aligns with Strategy 2)  
Instead of the manually calculated `TechnicalIndicatorsDto` from your plan, create a `SharePriceAgentMonthlyEntry` record that just holds the 15 necessary fields directly from your existing `SharePriceSignalDto`. Map it in `CompanyFactSheetData` or in each agent's `run()` method. More explicit, better self-documentation, easier to test.

**Option C — Custom `CompanyFactSheetData` serialization methods**  
Add new methods like `getDailySharePriceSignalsForPlanner()`, `getMonthlySharePriceSignalsSlim()` etc. that project the data before JSON serialization. This is close to what exists already (`getCompanyProfile()`, `getDailySharePriceSignals()`) and the least structural change.

**Recommendation:** Option C for the quick win, migrate to Option B over time for testability.

---

## 6. Prompt Quality Analysis

### 6.1 Should You Add Data Field Descriptions?

**Short answer: Yes, selectively — but not exhaustive field-by-field glossaries.**

Here is the nuanced breakdown:

| Scenario | Recommendation |
|---|---|
| **Standard GAAP field names** (`revenue`, `net_income`, `total_equity`) | No description needed — all modern LLMs know these |
| **Domain-standard ratios** (`pe_ratio`, `debt_to_equity`, `return_on_equity`) | No description needed |
| **Application-specific computed signals** (`composite_signal`, `quality_score`, `piotroski_f_score`) | **Yes, describe these** — the LLM does not know your scoring formula |
| **Scale/unit ambiguity** (`revenue`, `ebitda` — are these millions or raw?) | **Yes, add a note** — the current cash_flow prompt has this right ("treat as millions") |
| **Flag/boolean fields as integers** (`is_cash_earnings = 1`, `is_graham_net_net = 0`) | **Yes, briefly clarify** — the LLM may treat `1` as a magnitude rather than a boolean flag |
| **Streak counters** (`revenue_growth_streak`, `positive_fcf_streak`) | **Yes, explain semantics** — "count of consecutive periods with positive FCF in a 4-period rolling window" |
| **Derived valuation with custom definition** (`graham_margin_of_safety`) | **Yes, one line** — the LLM knows Graham's concept but may not know your exact formula |

### 6.2 Per-Prompt Issues & Improvements

#### Planner Prompt

**Issue 1:** The prompt describes heuristics for variant selection but does not tell the model about `composite_signal` or `quality_score` that are now available. The model is expected to derive company type from free-text `description` in `CompanyDto` but could instead use `compositeSignal` as a shortcut signal.

**Issue 2:** The `analysis_focus` is produced but then siloed inside `PlannerPlan.analysisFocus` and never used downstream (see section 3). The prompt should state: *"The analysis_focus you generate will be passed verbatim to every specialist agent as context for their own analysis."* This makes the Planner understand the downstream impact.

**Issue 3:** No mention of `piotroski_f_score` in the input or in the instructions. A score of 1–2 should immediately inform the focus sentence but the model must discover this independently.

**Suggested data context addition:**
```
### DATA DICTIONARY (application-specific fields)
- composite_signal: Pre-computed screening signal. Values: STRONG_BUY, BUY, HOLD, AVOID. 
  Based on quality_score, earnings_yield, piotroski_f_score thresholds.
- quality_score: Composite business quality score (0-100). Weighted combination of 
  profitability (ROE, ROCE), FCF quality, balance sheet strength, and growth consistency.
- piotroski_f_score: Piotroski F-score (0-9). Measures financial health improvement 
  across 9 binary criteria. >=7 = strong, <=2 = distressed.
- pct_from_50d_ma / pct_from_200d_ma: % deviation of current price from its 50/200-day 
  simple moving average. Negative means trading below the average.
- volume_velocity: Current volume / 30-day average volume. >1.5 = significant volume event.
```

---

#### Fundamentals Prompt

**Issue 1:** The prompt says "analyze `company_financial_ratios` data (e.g., ROCE, ROE, Net Margin)" but the new `CompanyFinancialRatiosDto` also includes `piotroski_f_score`, `earnings_quality_ratio`, `margin_expansion_signal`, `revenue_growth_streak`, and all 5 YoY growth rates. These are **not mentioned in the prompt**, so the model may miss or ignore them.

**Issue 2:** The prompt asks for only 3 output fields (`growth_analysis`, `profitability_analysis`, `summary`). Given the richer data, a fourth field is warranted:

```json
{
  "growth_analysis": "...",
  "profitability_analysis": "...",
  "quality_signals": "...",   // NEW: piotroski, earnings quality, margin expansion
  "summary": "..."
}
```

**Issue 3:** No mention that `marginExpansionSignal = 1` means *all three margins (gross, operating, net) expanded simultaneously* — the model would likely interpret `1` as a ratio magnitude.

**Suggested data context addition:**
```
### DATA DICTIONARY (computed signals)
- piotroski_f_score: 0–9. Measures year-over-year improvement in profitability, 
  leverage/liquidity, and operating efficiency. >=7 is financially improving.
- margin_expansion_signal: 1 if gross, operating, AND net margin all improved YoY; else 0.
- earnings_quality_ratio: Operating cash flow / Net income. >1.0 means cash earnings 
  exceed accounting earnings (high quality). <0.7 may indicate earnings manipulation.
- revenue_growth_streak / positive_fcf_streak / positive_earnings_streak: Count of 
  consecutive periods (rolling 4-period window) meeting that criterion.
- *_yoy_growth values: Year-over-year percentage change (e.g., 0.15 = +15%).
```

---

#### Cash Flow Prompt

**Current state:** Best-written prompt of the set. Already has the unit note ("treat raw values as millions"). Has two specific analysis tasks and a clear output schema.

**Issue 1:** Receives `company_financial_ratios` but the prompt only mentions "cash flow metrics". The prompt should explicitly list which ratio fields are relevant (FCF, FCF margin, FCF yield, earnings quality ratio, positive FCF streak) so the model does not scan 30+ fields looking for relevant ones.

**Issue 2:** No mention of `fcf_yoy_growth` which is now available in `CompanyFinancialRatiosDto`. This is the single most useful signal for a cash flow trend analysis.

---

#### Share Price Prompt

**Issue 1 (Critical):** The daily prompt says "up to 30 days" of data and the monthly says "up to 10 years." The `SharePriceSignalDto` has **~50 fields**. For monthly data, 120 entries × 50 fields is a 6,000-element JSON. Many of these fields repeat identically across months (e.g., `companyId`, `market`, `currency`, `ticker`). This is significant context bloat.

**Issue 2:** The prompt does not tell the model what `composite_signal` and `quality_score` mean. For historical monthly data, a shift in `composite_signal` from `AVOID` → `BUY` over 12 months is an extremely strong signal that would be invisible to the model without a data dictionary.

**Issue 3:** The prompt correctly asks for `historical_trend_analysis` (10-year context) but does not instruct the model to look for regime changes — e.g., *"Did the P/E multiple expand or compress over the 10 years? Is the current valuation above or below the historical median?"* The model will do this implicitly, but explicit instructions produce more consistent and structured outputs.

**Issue 4:** The prompt uses `MINI` tier model. Monthly 10-year trend analysis is complex enough that it warrants at least a `STANDARD` tier model, especially if you want nuanced historical regime analysis.

---

#### Valuation Prompt

**Issue 1:** The Valuation agent receives `daily_share_price_signals` (30 days) to get valuation multiples but it only needs the **most recent 3–5 days** (to smooth single-day volatility). Sending 30 days is wasteful.

**Issue 2:** `ValuationAgentOutput` only has two fields: `multiple_analysis` and `summary`. Given the richness of the data (P/E, P/FCF, P/B, P/S, EV/EBITDA, EV/FCF, graham_margin_of_safety, earnings_yield, fcf_yield), the agent should produce a more structured output:

```json
{
  "multiple_analysis": "...",
  "dcf_perspective": "...",    // NEW: using FCF yield as implied discount rate
  "summary": "..."
}
```

**Issue 3:** The prompt says "a high P/E might be justified by exceptional ROE and growth" as an example of nuanced thinking. This is good framing — but the prompt should also hint at the Graham defensive benchmark (P/E < 15, P/B < 1.5) since `is_graham_defensive` is now available in the data and the Synthesizer would benefit from knowing if the valuation qualifies.

---

#### Risk Prompt

**Issue 1 (Structural):** The Risk agent receives the `SharePriceAgentOutput` as context — but it does **not** receive the `FundamentalsAgentOutput` or `CashFlowAgentOutput`, even though those contain directly risk-relevant data: negative equity flag, earnings quality ratio, positive FCF streak, and Piotroski score. The Risk agent is the logical consumer of all these signals.

**Recommendation:** Pass `FundamentalsAgentOutput` and `CashFlowAgentOutput` to the Risk agent as additional context:

```json
{
  "balance_sheet_history": ...,
  "company_financial_ratios": ...,
  "share_price_analysis": ...,       // existing
  "fundamentals_analysis": ...,      // NEW
  "cash_flow_analysis": ...          // NEW
}
```

This means the Risk agent needs to run after CashFlow (currently at order 2) and Fundamentals (order 1) — it already does (order 6), so the execution order is already correct.

**Issue 2:** `key_risks` is limited to 3–5 bullets. With the new data richness, the output schema could be more structured:

```json
{
  "balance_sheet_risks": ["..."],
  "earnings_quality_risks": ["..."],
  "price_trend_risks": ["..."],
  "summary": "..."
}
```

---

#### News Prompt

**Issue 1 (Major):** The `v_ticker_news_sentiment` view now provides pre-aggregated, time-decayed, relevance-weighted sentiment scores for 7d, 14d, and 30d windows with labels. None of this is passed to the News agent. The News agent currently receives raw individual articles and must figure out sentiment itself — which it already does — but it lacks the quantified aggregate view.

**Recommendation:** Feed the structured sentiment signals from `v_ticker_news_sentiment` alongside the raw articles:

```json
{
  "sentiment_summary": {
    "news_count_7d": 12,
    "news_sentiment_7d": 0.22,
    "news_sentiment_label_7d": "SOMEWHAT_BULLISH",
    "news_count_14d": 31,
    "news_sentiment_14d": 0.09,
    "news_sentiment_label_14d": "NEUTRAL",
    "news_count_30d": 47,
    "news_sentiment_30d": 0.14,
    "news_sentiment_label_30d": "NEUTRAL"
  },
  "recent_news": [...]
}
```

The model can then validate its qualitative reading against the quantified aggregate. If it reads articles as mostly bullish but the 14-day weighted score is -0.2, that is a flag it should note. This is a **high-value integration of the new view**.

**Issue 2:** `NewsAgentOutput` only has a `summary` string. Add structured sentiment fields:

```java
public record NewsAgentOutput(
    @JsonProperty("summary") String summary,
    @JsonProperty("sentiment") String sentiment,         // BULLISH/BEARISH/NEUTRAL
    @JsonProperty("sentiment_shift") String sentimentShift  // "improving" / "deteriorating" / "stable"
) {}
```

The Critic and Synthesizer will be able to cross-check sentiment with Valuation and Fundamentals far more reliably with this structure.

---

#### Critic Prompt

**Issue 1:** The Critic receives all specialist outputs but has no access to the raw `analysis_focus` from the Planner. If the Planner said *"stock is at a 10-year P/E low"* and the Valuation agent says *"the stock is fairly valued"*, that is a contradiction the Critic should catch but cannot, because it does not see the Planner's stated focus.

**Recommendation:** Feed `PlannerPlan` (specifically `analysis_focus`) to the Critic prompt so it can verify that specialists aligned with the stated investment hypothesis.

**Issue 2:** Using `PRO` tier for the Critic is correct. Keep this.

**Issue 3:** The Critic's output only flags contradictions as string bullets. A richer output would be:

```json
{
  "contradictions_found": ["..."],
  "is_consistent": true,
  "missing_analysis": ["..."],      // NEW: signals in data that no agent discussed
  "data_quality_flags": ["..."]     // NEW: suspicious data (e.g., EPS YoY growth of 9000%)
}
```

---

#### Synthesizer Prompt

**Issue 1:** The Synthesizer has the richest prompt but receives only text outputs from specialists — it never sees the raw financial data. This means if the Critic flags "Valuation agent says fairly valued but Fundamentals agent says outstanding ROE," the Synthesizer resolves this through prose reasoning alone. It would benefit from seeing the **key quantified signals** in its context alongside the narrative outputs.

**Recommendation:** Pass a compact `key_metrics` section to the Synthesizer:

```json
{
  "key_metrics": {
    "quality_score": 72,
    "piotroski_f_score": 7,
    "composite_signal": "BUY",
    "pe_ratio_current": 12.4,
    "earnings_yield": 0.081,
    "revenue_yoy_growth": 0.14,
    "news_sentiment_30d_label": "SOMEWHAT_BULLISH"
  },
  "specialists": { ... },
  "critic": { ... }
}
```

This 8-field snapshot costs ~100 tokens and dramatically anchors the Synthesizer's verdict in actual numbers.

**Issue 2:** `conviction` is an integer 1–5 but the prompt gives no guidance on what each level means. Add to the prompt:
```
- conviction 5: Overwhelming, multi-factor alignment. High confidence.
- conviction 4: Strong case with minor caveats.
- conviction 3: Balanced. Material uncertainty remains.
- conviction 2: Weak case. Material risks outweigh opportunities.
- conviction 1: Avoid. Clear red flags present.
```

---

## 7. Data History Depth — Should You Feed 10 Years?

### 7.1 Current State

- `fact-sheet.history-limit: 3660` days (~10 years) — used for income statements, balance sheets, cash flows, financial ratios
- `share-price.monthly-history-limit: 3660` days (~10 years) — monthly signals
- `share-price.daily-history-limit: 30` days — daily signals
- `news.history-limit: 30` days

### 7.2 Analysis

**10 years of annual fundamentals = ~10 rows.** This is completely fine. 10 annual income statement rows are ~10k tokens (given the raw JSON blob format) — manageable.

**10 years of quarterly fundamentals = ~40 rows.** Still manageable, ~40k tokens. Acceptable for a pro-tier model.

**10 years of monthly price signals with 50 columns = ~120 rows × 50 fields = 6,000 values.** This is the problem. At ~5 tokens per field, this is 30,000 tokens just for the monthly price history. This is where slimming the DTO (section 5.3) has the greatest impact — reducing to ~15 relevant fields brings it to 9,000 tokens, which is far more manageable.

### 7.3 Recommendation — Keep 10 Years, Slim the Columns

**Do not reduce the time horizon.** 10 years is exactly right for the Share Price agent's `historical_trend_analysis` task (comparing current P/E to 10-year median) and for the Fundamentals agent to assess business quality compounding. Reducing to 5 years would lose the pre-COVID cycle, which is valuable context.

**Instead, slim the columns:**
- Monthly price signals: from ~50 fields → ~15 fields (see section 5.3)
- CompanyFinancialRatiosDto: from ~35 fields → ~20 fields (drop database metadata)

**Do let the Planner control depth for specialists, not globally.** A recently-IPO'd company should not be sent 10 years of empty/null-padded data. The Planner, once enhanced (section 4), can set `fundamentals_history_years: 3` in its plan, and `CompanyFactSheetDataService` can clip accordingly.

---

## 8. The New `v_ticker_news_sentiment` View

### 8.1 Current Integration

**None.** The view exists and is wired into `v_screener_master` and `v_screener_news_sentiment` for the UI screener, but the LLM News agent has no access to it.

### 8.2 Integration Opportunities

1. **News Agent context** — Feed the 7d/14d/30d weighted sentiment scores alongside raw articles (described in section 6 / News Prompt).

2. **Planner context** — Add the 7-day sentiment score to the Planner's input. A `news_sentiment_label_7d = "BEARISH"` combined with a `composite_signal = "HOLD"` should cause the Planner to flag sentiment risk in `analysis_focus`.

3. **New data field in `CompanyFactSheetData`** — Add a `TickerNewsSentimentDto` (7d/14d/30d scores) loaded via a new `CompanyApi` method. Feed it to the News agent and the Planner.

4. **Synthesizer key metrics** — Include `news_sentiment_30d_label` in the compact key metrics snapshot (section 6 / Synthesizer).

---

## 9. Proposed `CompanyFactSheetData` Additions

```java
// New field
private final TickerNewsSentimentDto newsSentimentAggregate;

// New accessor
public String getNewsSentimentAggregate() {
    // serialize TickerNewsSentimentDto — always small (< 200 tokens)
    return JsonUtils.toJson(objectMapper, newsSentimentAggregate, "{}");
}
```

Where `TickerNewsSentimentDto` maps from `v_ticker_news_sentiment`:

```java
public record TickerNewsSentimentDto(
    String ticker,
    int newsCount7d, float newsSentiment7d, String newsSentimentLabel7d,
    int newsCount14d, float newsSentiment14d, String newsSentimentLabel14d,
    int newsCount30d, float newsSentiment30d, String newsSentimentLabel30d
) {}
```

---

## 10. Prioritized Improvement Roadmap

### 🔴 High Priority (biggest ROI, lowest risk)

1. **Slim `SharePriceSignalDto` serialization for monthly data** — reduce from ~50 to ~15 columns in `getMonthlySharePriceSignals()`. Immediately reduces token usage by ~65% on the Share Price agent.

2. **Add `v_ticker_news_sentiment` data to the News agent** — new `getNewsSentimentAggregate()` method in `CompanyFactSheetData`, inject into news prompt. Adds structured sentiment signal that the Critic and Synthesizer can cross-reference.

3. **Add data dictionary sections to prompts for computed/application-specific fields** — `composite_signal`, `quality_score`, `piotroski_f_score`, `margin_expansion_signal`, `earnings_quality_ratio`, streak counters, `is_*` boolean flags.

4. **Propagate `analysisFocus` into specialist prompts** — Add `analysisFocus` to `AgentContext`. Add `{{ analysis_focus }}` placeholder to all specialist prompts. This is a 10-line code change with high behavioral impact.

### 🟡 Medium Priority (structural improvements)

5. **Extend `PlannerPlan` with `history_years_per_agent` map** — Let the Planner control data depth. Requires `CompanyFactSheetDataService` to support per-agent clipping.

6. **Pass `FundamentalsAgentOutput` + `CashFlowAgentOutput` to the Risk agent** — Currently the Risk agent re-discovers things the Fundamentals and CashFlow agents already found.

7. **Add `sentiment` and `sentimentShift` fields to `NewsAgentOutput`** — Makes the output structured for downstream agents.

8. **Add `analysis_focus` from Planner to the Critic's input** — Allows the Critic to verify specialist alignment with stated investment hypothesis.

9. **Upgrade SharePrice agent to `STANDARD` tier** — The monthly 10-year historical trend analysis is non-trivial; MINI tier may miss subtle regime changes.

### 🟢 Low Priority (polish and future-proofing)

10. **Slim `CompanyFinancialRatiosDto` serialization** — Drop `id`, `companyId`, `template`, `restatedDate`.

11. **Slim daily `SharePriceSignalDto` serialization for Planner** — Planner only needs ~10 fields.

12. **Add `conviction` scale description to Synthesizer prompt**.

13. **Add `missing_analysis` and `data_quality_flags` to `CriticAgentOutput`**.

14. **Add compact `key_metrics` snapshot to Synthesizer input**.

15. **Parallel execution for independent specialists** (Fundamentals, CashFlow, News, SharePrice can all run concurrently).

16. **Remove or implement `NEWS_SUMMARY` agent type** — Currently dead code in `AgentType` enum.

---

## 11. Summary Table

| Area | Current State | Issue | Recommended Fix |
|---|---|---|---|
| Planner input | Full 50-col DTO × 30 days | Token waste, noise | Slim to 10 relevant fields |
| Planner output | 4 variants + focus sentence | Focus not propagated downstream | Add `analysisFocus` to `AgentContext`, use in all specialist prompts |
| Planner depth | Global 10-year config | No per-company tuning | Add `history_years` to `PlannerPlan` |
| Fundamentals prompt | Missing new signal descriptions | Model unaware of Piotroski, margin expansion, quality signals | Add data dictionary, add `quality_signals` output field |
| Share Price monthly | Full 50-col DTO × 120 months | ~30k token input | Slim to 15 fields → ~9k tokens |
| Share Price prompt | No mention of `composite_signal` / `quality_score` | Model cannot interpret pre-computed signals | Add data dictionary |
| Valuation input | 30-day daily signals | Needs only 3–5 days for multiples | Clip to last 5 days |
| Risk agent | Only gets share price context from prior agents | Misses FCF and earnings quality signals | Add fundamentals + cash flow outputs to Risk input |
| News agent | No access to `v_ticker_news_sentiment` | Misses pre-computed weighted aggregate | Add `TickerNewsSentimentDto` to `CompanyFactSheetData` and News prompt |
| Critic input | No Planner plan context | Cannot verify alignment with investment thesis | Pass `analysis_focus` to Critic |
| Synthesizer input | Only narrative text from specialists | Verdict unanchored to hard numbers | Add 8-field `key_metrics` snapshot |
| DTO serialization | Full DTOs with DB metadata | Misleading `id`/`companyId`/`template` fields | Jackson `@JsonView` or slim accessor methods |
| Data history | 10 years all agents | Correct for most; excessive for fast-growing small-caps | Planner-controlled depth |
| Field descriptions | Only cash flow has unit note | Computed signals misinterpreted | Add targeted data dictionary sections to all prompts |
