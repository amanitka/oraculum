# Oraculum Future Enhancements Plan

Tento dokument slučuje dřívější plány (z `ANALYSIS_ROADMAP.md` a `FUTURE_ENHANCEMENTS.md`) a představuje jednotný seznam dalších vylepšení Oracula, která ještě nebyla implementována. Funkce *Insider Transactions* již byla úspěšně realizována a do produkce začleněna.

---

## 1. Srovnání s konkurencí (Peer Group / Relative Valuation)
*Současný problém:* Agenti hodnotí firmu izolovaně. Model ví, že má firma P/E 24 a marži 15 %, ale neví, zda je to v daném sektoru hodně nebo málo.
*Vylepšení:* 
- Poskytnout agentům (zejména `ValuationAgent` a `FundamentalsAgent`) agregovaná data za konkurenci.
- **Implementace:** Z databáze lze snadno vytvořit PostgreSQL View (např. `mv_sector_medians`), které pro daný `sector` nebo `industry` vypočítá mediánové hodnoty klíčových ukazatelů. Tyto se přidají do `CompanyFactSheetData`. Agenti tak budou schopni relativně zhodnotit aktuální ukazatele proti sektorovým standardům.

## 2. Transkripty z hovorů k výsledkům (Earnings Call Transcripts)
*Současný problém:* Čísla popisují minulost. Změny v narativu managementu často předcházejí změny v číslech.
*Vylepšení:*
- Sběr transkriptů z kvartálních výsledků (Earnings Calls).
- **Implementace:** Vytvoření nového `ManagementAgenta` nebo integrace do `FundamentalsAgenta`. Agent dostane přepis Q&A sekce hovoru a zhodnotí tón managementu (Sentiment Analysis) – vyhýbali se odpovědím? Jsou přehnaně defenzivní ohledně marží?

## 3. Portfolio Watchlist & Push Notifications
Tato funkce posune Oraculum z pasivního analytického nástroje do pozice aktivního Portfolio Monitorovacího Systému.
*Vylepšení:*
- Vytvořit tabulku portfolia a napojit UI (PortfolioView).
- **Automated Monitoring:** Skedulovaný proces, který 2x denně vyhodnotí stav portfolia pomocí zlehčené analytické smyčky (pouze NewsAgent a SharePriceAgent z důvodu úspory tokenů).
- **Push Notifications:** Pokud dojde k detekci ohrožení (extrémní propad ceny, kritické zprávy), odešle se varovná zpráva na mobil via API služby Pushover (nebo Telegram).

## 4. Makroekonomický kontext
*Současný problém:* Neexistuje napojení na "top-down" pohled. Skvělá firma může trpět kvůli makru.
*Vylepšení:*
- Předávat agentům základní údaje o sazbách, inflaci a komoditách.
- **Implementace:** Stahování základních makro dat (např. z FRED) a předání aktuální výnosové křivky a inflace jako metadat do kontextu agentů. Extrémně důležité pro valuaci REITs a bankovního sektoru.

## 5. Critic Self-Correction (Agentic Loops)
*Poznámka: Teoretický návrh byl již zadokumentován v `advanced_strategies_plan.md`.*
*Vylepšení:*
- Modifikovat agenty tak, aby uměli opravovat vlastní výstupy.
- **Implementace:** Aktualizovat orchestrátor o `while` smyčku. Pokud `CriticAgent` označí výstup `is_consistent: false`, proběhne zavolání funkce `refine` u příslušného agenta. Specialisté tímto dostanou prostor na opravu chyb a nesouladů před finální syntézou zprávy.

## 6. Alternativní data (Web traffic, trendy)
*Současný problém:* Modely nemají "real-time" indikaci prodejů mimo oficiální zprávy.
*Vylepšení:*
- Sběr specifických alternativních dat.
- **Implementace:** Pro E-commerce a SaaS sektory automaticky analyzovat data o návštěvnosti webu (např. SimilarWeb) nebo o popularitě (stažení) aplikací. Poskytnutí tzv. *leading indicators* budoucích zisků ještě před jejich finančním zveřejněním.
