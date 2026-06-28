-- Table: t_news (Partitioned by time_published)
-- =================================================================
CREATE TABLE public.t_news (
    id VARCHAR(64) NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    time_published TIMESTAMPTZ NOT NULL,
    authors JSONB,
    summary TEXT NOT NULL,
    source VARCHAR(255),
    category_within_source VARCHAR(255),
    source_domain VARCHAR(255),
    topics JSONB,
    overall_sentiment_score REAL,
    overall_sentiment_label VARCHAR(50),
    extracted_at TIMESTAMPTZ NOT NULL,
    sentiment_score_definition TEXT,
    relevance_score_definition TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, time_published)
) PARTITION BY RANGE (time_published);

CREATE INDEX ix_news_time_published ON public.t_news (time_published);

