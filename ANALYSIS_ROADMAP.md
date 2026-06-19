# Oraculum: Vize a Datová Roadmapa pro AI Analýzu

Tento dokument slouží jako zhodnocení současného stavu AI agentů v Oraculu a navrhuje další datová rozšíření, která skokově zvýší kvalitu generovaných investičních tezí (tzv. *alfa*).

## Zhodnocení současného stavu
Oraculum aktuálně stojí na mimořádně robustní architektuře:
- **Technologický fundament**: Spring Modulith, asynchronní event-driven design pro LLM auditování, clean architektura.
- **Mixture of Experts (MoE)**: Rozdělení kognitivní zátěže mezi specializované agenty (Fundamentals, Valuation, Risk, News, atd.) řeší problém ztráty kontextu velkých jazykových modelů (tzv. *Lost in the Middle* syndrom).
- **Zpracovávaná data**: Agenti mají vynikající historický a kvantitativní přehled díky napojení na výkazy, poměrové ukazatele, odhady analytiků (Consensus Estimates) a denní sentimenty zpráv.

Současný stav představuje perfektní **fundamentální screener a analyzátor**. Pro přechod na úroveň **institucionálního analytika (Hedge Fund Analyst)** je třeba přidat data, která modelům dodají širší kontext a tzv. *forward-looking* (výhledové) signály.

---

## Návrhy na vylepšení (Nové datové dimenze)

### 1. Srovnání s konkurencí (Peer Group / Relative Valuation)
*Současný problém:* Agenti hodnotí firmu izolovaně. Model ví, že má firma P/E 24 a marži 15 %, ale neví, zda je to v daném sektoru hodně nebo málo.
*Vylepšení:* 
- Poskytnout agentům (zejména `ValuationAgent` a `FundamentalsAgent`) agregovaná data za konkurenci.
- **Implementace:** Vzhledem k tomu, že Oraculum už stahuje finanční data pro mnoho firem, **tato data již v databázi pravděpodobně máme**. Lze vytvořit PostgreSQL View (např. `mv_sector_medians`), které pro daný `sector` nebo `industry` vypočítá mediánové hodnoty klíčových ukazatelů (P/E, Gross Margin, ROIC, YoY Growth). Do `CompanyFactSheetData` se pak přidá objekt `peer_group_ratios`. Agenti tak budou schopni říct: *"P/E 24 je sice absolutně vysoké, ale sektorový medián je 30, takže je firma relativně podhodnocená."*

### 2. Transkripty z hovorů k výsledkům (Earnings Call Transcripts)
*Současný problém:* Čísla popisují minulost. Změny v narativu managementu často předcházejí změny v číslech.
*Vylepšení:*
- Sběr transkriptů z kvartálních výsledků (Earnings Calls).
- **Implementace:** Vytvoření nového `ManagementAgenta` nebo integrace do `FundamentalsAgenta`. Agent dostane přepis Q&A sekce hovoru a zhodnotí tón managementu (Sentiment Analysis) – vyhýbali se odpovědím? Jsou přehnaně defenzivní ohledně marží?

### 3. Insider Trading a Institucionální toky (Form 4 & 13F)
*Současný problém:* Modely neví, jak se chovají lidé, kteří mají o firmě nejvíce informací (C-level management).
*Vylepšení:*
- Sledovat hlášení pro SEC o nákupech a prodejích akcií vlastním managementem (Insider Trading) a změnách pozic velkých fondů.
- **Implementace:** Agentovi (např. `RiskAgent` nebo nový `FlowsAgent`) poskytneme sumarizaci "net insider buying" za posledních 6 měsíců. Masivní nákupy od CEO jsou jedním z nejsilnějších pozitivních signálů na trhu.

### 4. Makroekonomický kontext
*Současný problém:* Neexistuje napojení na "top-down" pohled. Skvělá firma může trpět kvůli makru.
*Vylepšení:*
- Předávat agentům základní údaje o sazbách, inflaci a komoditách.
- **Implementace:** Stahování základních makro dat (např. z FRED) a předání aktuální výnosové křivky a inflace jako metadat do `AgentContext`. Extrémně důležité pro valuaci REITs (nemovitostních fondů) a bankovního sektoru.

### 5. Alternativní data (Web traffic, trendy)
*Současný problém:* Modely nemají "real-time" indikaci prodejů mimo oficiální zprávy.
*Vylepšení:*
- Pro určité sektory (E-commerce, SaaS) sbírat data o návštěvnosti webu (SimilarWeb) nebo počtu stažení aplikací. Toto slouží jako tzv. *leading indicator* budoucích zisků ještě před jejich zveřejněním.

---

## Závěr
Nejsnazším a nejúčinnějším dalším krokem je implementace bodu **1. Srovnání s konkurencí**. Databázový fundament pro to již existuje a stačí data vhodně naagregovat pomocí SQL View a předložit je současným LLM agentům. To okamžitě a radikálně zvýší kontextuální kvalitu valuační analýzy.
