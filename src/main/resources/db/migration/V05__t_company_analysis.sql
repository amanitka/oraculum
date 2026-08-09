-- Table: t_company_analysis
-- =================================================================
CREATE TABLE public.t_company_analysis
(
    id              UUID PRIMARY KEY,
    company_id      INTEGER      NOT NULL,
    market          VARCHAR(255) NOT NULL,
    ticker          VARCHAR(255) NOT NULL,
    analysis_date   DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    -- Serialised AnalysisResult: contains both landing snapshot
    -- (outlook, valuation, conviction, thesis, top_bull/bear_points, etc.)
    -- and full report (executive_summary, factor_scores, key_drivers, key_risks).
    analysis_result JSONB,
    -- Full agent trace, kept separate from the business result for debugging.
    analysis_data   JSON,
    error           TEXT,
    requested_by    BIGINT REFERENCES t_user (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_company_analysis_company_id ON public.t_company_analysis (company_id);
CREATE INDEX ix_company_analysis_ticker_market_created ON public.t_company_analysis (ticker, market, created_at);
CREATE INDEX ix_company_analysis_status_created ON public.t_company_analysis (status, created_at);

-- Expression indexes on the JSONB analysis_result column for filtering by valuation / outlook
CREATE INDEX ix_company_analysis_valuation ON public.t_company_analysis ((analysis_result ->>'valuation'));
CREATE INDEX ix_company_analysis_outlook ON public.t_company_analysis ((analysis_result ->>'outlook'));
