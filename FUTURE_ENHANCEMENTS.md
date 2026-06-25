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
