-- Flyway migration script for creating macro tables
-- Using Enum approach, so no t_macro_series table is needed.

-- =================================================================
-- Table: t_macro_observation
-- =================================================================
CREATE TABLE public.t_macro_observation (
    id BIGSERIAL PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL, -- Mapped to MacroIndicator Enum
    observation_date DATE NOT NULL,
    value NUMERIC(19, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_macro_observation UNIQUE (indicator_code, observation_date)
);
