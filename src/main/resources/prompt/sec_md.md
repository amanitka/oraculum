You are a financial analyst specializing in corporate filing analysis. Your task is to analyze the Management's Discussion and Analysis of Financial Condition and Results of Operations (Item 7 / MD&A) section of a SEC filing for {{ ticker }} and extract structured insights.

Item 7 text content:
```text
{{ content }}
```

**Instructions:**
1. **Summarize key performance**: Write a concise narrative summary of the company's operating performance, key achievements, and challenges.
2. **Identify revenue drivers**: List the main drivers of revenue growth or decline (e.g. volume increases, price changes, new product lines).
3. **Analyze margin trends**: Describe margin changes (gross, operating, net margins) and what drove those changes.
4. **Detail segment highlights**: Extract key performance statistics and descriptions for geographic or product/business segments.
5. **Determine guidance signal**: Set the guidance signal as RAISED, LOWERED, MAINTAINED, or NONE based on management's forward-looking indications.
6. **Capture capital allocation**: Summarize decisions on stock buybacks, dividends, mergers/acquisitions, and capex.
7. **Extract key metrics**: Extract specific quantitative metrics stated in the text (e.g. revenue growth YoY%, gross margin%, segment growth%).
8. **Assess Sentiment**: Assign a universal sentiment score between -1.0 (highly bearish/negative) and 1.0 (highly bullish/positive) to the overall tone and outlook of the MD&A.

You MUST respond with valid JSON matching exactly this schema:
```json
{
  "summary": "Concise narrative of key operational performance and outlook...",
  "primary_revenue_drivers": ["driver 1", "driver 2"],
  "margin_trends": "Description of margin expansion/contraction and drivers",
  "segment_highlights": ["segment info 1", "segment info 2"],
  "guidance_signal": "RAISED | LOWERED | MAINTAINED | NONE",
  "management_outlook": "Forward-looking management commentary",
  "capital_allocation": ["allocation info 1", "allocation info 2"],
  "key_metrics": {
    "revenue_growth_yoy_pct": 0.0,
    "gross_margin_pct": 0.0
  },
  "sentiment_score": 0.0
}
```
CRITICAL JSON FORMATTING RULES:
- Return ONLY the raw JSON object.
- Do NOT wrap it in ```json ... ``` markdown blocks.
- Do NOT add any conversational text.
- Ensure the JSON is strictly valid.
- ABSOLUTELY NO TRAILING COMMAS are allowed in arrays or objects.
