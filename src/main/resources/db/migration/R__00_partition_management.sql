-- Flyway repeatable migration script for partition management

CREATE OR REPLACE FUNCTION create_monthly_partitions(p_table_name TEXT, p_start_date DATE, p_end_date DATE)
RETURNS void AS $$
DECLARE
    v_partition_date DATE;
    v_partition_name TEXT;
    v_partition_start TEXT;
    v_partition_end TEXT;
BEGIN
    v_partition_date := date_trunc('month', p_start_date);
    WHILE v_partition_date <= p_end_date LOOP
        v_partition_name := p_table_name || '_' || to_char(v_partition_date, 'YYYY_MM');
        v_partition_start := to_char(v_partition_date, 'YYYY-MM-DD');
        v_partition_end := to_char(v_partition_date + INTERVAL '1 month', 'YYYY-MM-DD');

        IF NOT EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = v_partition_name
              AND n.nspname = 'public'
        ) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                v_partition_name, p_table_name, v_partition_start, v_partition_end
            );
        END IF;

        v_partition_date := v_partition_date + INTERVAL '1 month';
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_yearly_partitions(p_table_name TEXT, p_start_date DATE, p_end_date DATE)
RETURNS void AS $$
DECLARE
    v_partition_date DATE;
    v_partition_name TEXT;
    v_partition_start TEXT;
    v_partition_end TEXT;
BEGIN
    v_partition_date := date_trunc('year', p_start_date);
    WHILE v_partition_date <= p_end_date LOOP
        v_partition_name := p_table_name || '_' || to_char(v_partition_date, 'YYYY');
        v_partition_start := to_char(v_partition_date, 'YYYY-MM-DD');
        v_partition_end := to_char(v_partition_date + INTERVAL '1 year', 'YYYY-MM-DD');

        IF NOT EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = v_partition_name
              AND n.nspname = 'public'
        ) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                v_partition_name, p_table_name, v_partition_start, v_partition_end
            );
        END IF;

        v_partition_date := v_partition_date + INTERVAL '1 year';
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Execute Partition Creation
SELECT create_monthly_partitions('t_share_price', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);
SELECT create_yearly_partitions('t_news', (NOW() - INTERVAL '5 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);
SELECT create_yearly_partitions('t_news_ticker', (NOW() - INTERVAL '5 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);
SELECT create_monthly_partitions('t_llm_execution_log', (NOW() - INTERVAL '1 month')::DATE, (NOW() + INTERVAL '1 year')::DATE);
SELECT create_yearly_partitions('t_insider_transaction_ticker', (NOW() - INTERVAL '3 years')::DATE, (NOW() + INTERVAL '1 years')::DATE);
SELECT create_yearly_partitions('t_ticker_document_raw', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);
SELECT create_yearly_partitions('t_ticker_document', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);
