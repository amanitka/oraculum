-- Table: t_news_ticker (Partitioned by time_published)
-- =================================================================
CREATE TABLE public.t_news_ticker (
    news_id VARCHAR(64) NOT NULL,
    ticker VARCHAR(16) NOT NULL,
    time_published TIMESTAMPTZ NOT NULL,
    relevance_score REAL,
    ticker_sentiment_score REAL,
    ticker_sentiment_label VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (news_id, ticker, time_published)
) PARTITION BY RANGE (time_published);

CREATE INDEX ix_news_ticker_ticker ON public.t_news_ticker (ticker);
CREATE INDEX ix_news_ticker_ticker_time ON public.t_news_ticker (ticker, time_published DESC);
