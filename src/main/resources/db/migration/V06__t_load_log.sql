-- Table: t_load_log
-- =================================================================
CREATE TABLE public.t_load_log (
    id BIGSERIAL PRIMARY KEY,
    dataset VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    loaded_rows INTEGER NOT NULL,
    merged_rows INTEGER NOT NULL,
    error_text TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_load_log_idempotency UNIQUE (dataset, run_id, file_checksum)
);

