-- View: v_company_analysis
-- =================================================================
CREATE OR REPLACE VIEW public.v_company_analysis AS
SELECT 
    a.id,
    a.company_id,
    c.company_name,
    c.industry_name,
    c.sector_name,
    a.market,
    a.ticker,
    a.analysis_date,
    a.status,
    a.report,
    a.summary,
    a.outlook,
    a.recommendation,
    a.conviction,
    a.analysis_data,
    a.error,
    a.requested_by,
    a.created_at,
    a.updated_at,
    (ROW_NUMBER() OVER (PARTITION BY a.company_id ORDER BY a.created_at DESC)) = 1 AS is_latest,
    COUNT(a.id) OVER (PARTITION BY a.company_id) AS total_analyses
FROM public.t_company_analysis a
LEFT JOIN public.t_company c ON a.company_id = c.id;
