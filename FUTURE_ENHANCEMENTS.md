# Oraculum Future Enhancements Plan

This document outlines the architectural plans for three major future enhancements to the Oraculum platform: Agentic Self-Correction, Insider Transactions data sourcing, and a Portfolio Watchlist with Push Notifications.

---

## 1. Critic Self-Correction (Agentic Loops)

*Note: You are completely correct that this is already covered in the existing documentation.*

The full technical specification for this feature is already written in `advanced_strategies_plan.md` under **Strategy 3: Critique-and-Refine Loop**. 

**Implementation Summary (Refer to `advanced_strategies_plan.md` for code):**
- Modify `Agent.java` to include a `refine(AgentContext ctx, T previousOutput, String criticFeedback)` default method.
- Update `CompanyAnalysisWorkflowService` to run a `while` loop (max 2 retries).
- If the Critic outputs `is_consistent: false`, the workflow passes the `contradictions_found` directly back into the Specialist agents via the `refine` method, forcing them to fix their math or logic before the Synthesizer generates the final report.

---

## 2. Insider Transactions (Free Data Sources)

Alpha Vantage's free tier is too restrictive (25 calls/day) for scraping insider transactions across a broad portfolio. To build this feature, we will use one of the following free alternatives:

### Option A: SEC EDGAR API (Recommended & 100% Free)
- **How it works**: By law, every US insider transaction must be reported via a "Form 4". The SEC provides a completely free, official REST API to query these filings.
- **Implementation**: We will build a harvester in `DataFileLoadService` that queries the SEC EDGAR API for Form 4 filings for a specific ticker, parses the JSON/XML, and stores it in the Oraculum database.
- **Pros**: No paywalls, no daily limits, official source of truth.

### Option B: Financial Modeling Prep (FMP)
- **How it works**: FMP provides a generous free tier (250 requests/day) and has a dedicated endpoint for insider trading.
- **Pros**: Easier to parse than raw SEC filings, JSON format is clean.

### Option C: Yahoo Finance Scraping
- **How it works**: Use a Java library (like `yahoofinance-api`) or a custom HTML scraper (using JSoup) to pull the "Insider Roster" and "Insider Transactions" tables directly from the Yahoo Finance ticker page.

---

## 3. Portfolio Watchlist & Push Notifications

This feature will transform Oraculum from a passive analysis tool into an active Portfolio Monitoring System.

### Phase A: The Vaadin Watchlist UI
1. Create a `t_portfolio` database table (columns: `user_id`, `ticker`, `shares_owned`, `average_buy_price`).
2. Create a new `PortfolioView.java` in Vaadin (secured by the Keycloak/Google SSO architecture).
3. The UI will display a grid of owned stocks, current market value, and a button to "Trigger Manual Analysis".

### Phase B: Automated Monitoring (Spring `@Scheduled`)
1. Create a `PortfolioMonitoringScheduler.java` class with a `@Scheduled(cron = "0 0 8,16 * * *")` annotation to run twice a day (e.g., Morning and Market Close).
2. The scheduler iterates through the `t_portfolio` table.
3. For each ticker, it executes a lightweight analysis workflow (running *only* the `NewsAgent` and `SharePriceAgent` to save LLM tokens).
4. If the `NewsAgent` detects extreme bearish sentiment, or the `SharePriceAgent` detects a massive price drop (e.g., > 5%), it triggers the Alert Service.

### Phase C: Push Notifications (Pushover / Telegram)
Since you already have a paid Pushover account, we will integrate it as the primary alert channel.

1. Add the Pushover API dependency or use standard Java `HttpClient` to make a POST request to `https://api.pushover.net/1/messages.json`.
2. When the Automated Monitoring detects a threat, it sends a payload:
   ```json
   {
     "token": "APP_TOKEN",
     "user": "USER_KEY",
     "title": "🚨 Oraculum Alert: GILD",
     "message": "Stock dropped 6% below 50-day SMA. News Agent reports high distress due to oncology trial failure.",
     "priority": 1
   }
   ```
3. Your phone instantly receives the push notification on the lock screen, allowing you to react immediately to portfolio threats.
