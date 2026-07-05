-- Table: t_industry
-- =================================================================
CREATE TABLE public.t_industry (
    industry_id VARCHAR(255) PRIMARY KEY,
    sector_name VARCHAR(255) NOT NULL,
    industry_name VARCHAR(255) NOT NULL,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

