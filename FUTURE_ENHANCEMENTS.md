# Oraculum Future Enhancements Plan

Tento dokument slučuje dřívější plány (z `ANALYSIS_ROADMAP.md` a `FUTURE_ENHANCEMENTS.md`) a představuje jednotný seznam
dalších vylepšení Oracula, která ještě nebyla implementována. Funkce *Insider Transactions* již byla úspěšně realizována
a do produkce začleněna.

---

## 1. Transkripty z hovorů k výsledkům (Earnings Call Transcripts)

*Současný problém:* Čísla popisují minulost. Změny v narativu managementu často předcházejí změny v číslech.
*Vylepšení:*

- Sběr transkriptů z kvartálních výsledků (Earnings Calls).
- **Implementace:** Vytvoření nového `ManagementAgenta` nebo integrace do `FundamentalsAgenta`. Agent dostane přepis Q&A
  sekce hovoru a zhodnotí tón managementu (Sentiment Analysis) – vyhýbali se odpovědím? Jsou přehnaně defenzivní ohledně
  marží?

## 2. Portfolio Watchlist & Push Notifications

Tato funkce posune Oraculum z pasivního analytického nástroje do pozice aktivního Portfolio Monitorovacího Systému.
*Vylepšení:*

- Vytvořit tabulku portfolia a napojit UI (PortfolioView).
- **Automated Monitoring:** Skedulovaný proces, který 2x denně vyhodnotí stav portfolia pomocí zlehčené analytické
  smyčky (pouze NewsAgent a SharePriceAgent z důvodu úspory tokenů).
- **Push Notifications:** Pokud dojde k detekci ohrožení (extrémní propad ceny, kritické zprávy), odešle se varovná
  zpráva na mobil via API služby Pushover (nebo Telegram).

## 3. Makroekonomický kontext

*Současný problém:* Neexistuje napojení na "top-down" pohled. Skvělá firma může trpět kvůli makru.
*Vylepšení:*

- Předávat agentům základní údaje o sazbách, inflaci a komoditách.
- **Implementace:** Stahování základních makro dat (např. z FRED) a předání aktuální výnosové křivky a inflace jako
  metadat do kontextu agentů. Extrémně důležité pro valuaci REITs a bankovního sektoru.

## 4. Alternativní data (Web traffic, trendy)

*Současný problém:* Modely nemají "real-time" indikaci prodejů mimo oficiální zprávy.
*Vylepšení:*

- Sběr specifických alternativních dat.
- **Implementace:** Pro E-commerce a SaaS sektory automaticky analyzovat data o návštěvnosti webu (např. SimilarWeb)
  nebo o popularitě (stažení) aplikací. Poskytnutí tzv. *leading indicators* budoucích zisků ještě před jejich finančním
  zveřejněním.

---

## 5. SEC EDGAR Additional Datasets (Python Harvester)

Because the Python Harvester uses `edgartools`, it is extremely trivial to expand its capabilities to pull other SEC forms.

### Form 13F (Institutional Holdings) - "Smart Money Tracker"
- **Goal:** Track what major hedge funds (Berkshire Hathaway, Renaissance Technologies, etc.) are buying and selling.
- **Why it matters:** If Oraculum flags a company as undervalued, and the 13F data shows multiple top-tier hedge funds are aggressively accumulating shares, the AI Conviction Score can be significantly increased.
- **Implementation Idea:** Use `edgartools` to fetch `13F-HR` filings for specific CIKs (hedge funds) and parse the XML information table to extract the list of held stocks and share counts.

### Form 10-K (Annual Reports)
- **Goal:** Provide the AI with the company's full business model and identified risks.
- **Why it matters:** 
  - **Item 1A (Risk Factors):** Essential for the "Critic Agent" to understand what the company itself identifies as threats to its long-term survival.
  - **Item 7 (MD&A):** Deep dive into long-term financial trends and capital allocation strategies.
- **Implementation Idea:** Fetch the 10-K and use NLP or targeted parsing to extract only Items 1A and 7 to avoid blowing up the LLM token context limits.
