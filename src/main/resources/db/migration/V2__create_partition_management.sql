-- Flyway migration script for creating and managing table partitions

-- =================================================================
-- FUNCTION: create_monthly_partitions
-- Description: Creates monthly partitions for a given table for a specified date range.
-- =================================================================
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

        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_partition_name) THEN
            RAISE NOTICE 'Creating monthly partition % for range [%, %)', v_partition_name, v_partition_start, v_partition_end;
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                v_partition_name,
                p_table_name,
                v_partition_start,
                v_partition_end
            );
        END IF;

        v_partition_date := v_partition_date + INTERVAL '1 month';
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =================================================================
-- FUNCTION: create_yearly_partitions
-- Description: Creates yearly partitions for a given table for a specified date range.
-- =================================================================
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
        v_partition_start := to_char(v_partition_date, 'YYYY-01-01');
        v_partition_end := to_char(v_partition_date + INTERVAL '1 year', 'YYYY-01-01');

        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_partition_name) THEN
            RAISE NOTICE 'Creating yearly partition % for range [%, %)', v_partition_name, v_partition_start, v_partition_end;
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                v_partition_name,
                p_table_name,
                v_partition_start,
                v_partition_end
            );

            -- If creating partitions for t_news_ticker, add the foreign key constraint
            IF p_table_name = 't_news_ticker' THEN
                RAISE NOTICE 'Adding foreign key to partition %', v_partition_name;
                EXECUTE format(
                    'ALTER TABLE %I ADD CONSTRAINT %s FOREIGN KEY (news_id, time_published) REFERENCES public.t_news(id, time_published) ON DELETE CASCADE',
                    v_partition_name,
                    v_partition_name || '_fk'
                );
            END IF;
        END IF;

        v_partition_date := v_partition_date + INTERVAL '1 year';
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- =================================================================
-- FUNCTION: purge_old_partitions
-- Description: Drops partitions older than a specified interval, based on a monthly or yearly naming scheme.
-- =================================================================
CREATE OR REPLACE FUNCTION purge_old_partitions(p_table_name TEXT, p_retention_interval INTERVAL, p_partition_type TEXT)
RETURNS void AS $$
DECLARE
    v_partition RECORD;
    v_cutoff_date DATE;
    v_partition_date DATE;
    v_date_format TEXT;
BEGIN
    IF p_partition_type = 'monthly' THEN
        v_cutoff_date := date_trunc('month', NOW() - p_retention_interval);
        v_date_format := 'YYYY_MM';
    ELSIF p_partition_type = 'yearly' THEN
        v_cutoff_date := date_trunc('year', NOW() - p_retention_interval);
        v_date_format := 'YYYY';
    ELSE
        RAISE EXCEPTION 'Invalid partition type: %. Must be "monthly" or "yearly".', p_partition_type;
    END IF;

    RAISE NOTICE 'Purging % partitions for table % older than %', p_partition_type, p_table_name, v_cutoff_date;

    FOR v_partition IN
        SELECT child.relname AS partition_name
        FROM pg_inherits
        JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
        JOIN pg_class child ON pg_inherits.inhrelid = child.oid
        WHERE parent.relname = p_table_name
    LOOP
        -- Extract date from partition name, e.g., 't_news_2020' -> '2020-01-01'
        v_partition_date := to_date(
            substring(v_partition.partition_name from (length(p_table_name) + 2)),
            v_date_format
        );

        IF v_partition_date < v_cutoff_date THEN
            RAISE NOTICE 'Dropping old partition: %', v_partition.partition_name;
            EXECUTE format('DROP TABLE %I CASCADE', v_partition.partition_name);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- =================================================================
-- INITIAL PARTITION CREATION & PURGING
-- =================================================================
DO $$
BEGIN
    RAISE NOTICE '--- Creating initial partitions for t_share_price (monthly) ---';
    PERFORM create_monthly_partitions('t_share_price', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);

    RAISE NOTICE '--- Creating initial partitions for t_news (yearly) ---';
    PERFORM create_yearly_partitions('t_news', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);

    RAISE NOTICE '--- Creating initial partitions for t_news_ticker (yearly) ---';
    PERFORM create_yearly_partitions('t_news_ticker', (NOW() - INTERVAL '10 years')::DATE, (NOW() + INTERVAL '2 years')::DATE);

    RAISE NOTICE '--- Purging old yearly partitions for news tables ---';
    -- Purge news data older than 3 years
    PERFORM purge_old_partitions('t_news_ticker', '3 years', 'yearly');
    PERFORM purge_old_partitions('t_news', '3 years', 'yearly');
END;
$$;