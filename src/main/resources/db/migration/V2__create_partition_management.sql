-- Flyway migration script for creating and managing table partitions

-- =================================================================
-- FUNCTION: create_monthly_partitions
-- Description: Creates monthly partitions for a given table for a specified date range.
-- =================================================================
CREATE OR REPLACE FUNCTION create_monthly_partitions(_table_name TEXT, _start_date DATE, _end_date DATE)
RETURNS void AS $$
DECLARE
    _partition_date DATE;
    _partition_name TEXT;
    _partition_start TEXT;
    _partition_end TEXT;
BEGIN
    _partition_date := date_trunc('month', _start_date);
    WHILE _partition_date <= _end_date LOOP
        _partition_name := _table_name || '_' || to_char(_partition_date, 'YYYY_MM');
        _partition_start := to_char(_partition_date, 'YYYY-MM-DD');
        _partition_end := to_char(_partition_date + INTERVAL '1 month', 'YYYY-MM-DD');

        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = _partition_name) THEN
            RAISE NOTICE 'Creating monthly partition % for range [%, %)', _partition_name, _partition_start, _partition_end;
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                _partition_name,
                _table_name,
                _partition_start,
                _partition_end
            );
        END IF;

        _partition_date := _partition_date + INTERVAL '1 month';
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =================================================================
-- FUNCTION: create_yearly_partitions
-- Description: Creates yearly partitions for a given table for a specified date range.
-- =================================================================
CREATE OR REPLACE FUNCTION create_yearly_partitions(_table_name TEXT, _start_date DATE, _end_date DATE)
RETURNS void AS $$
DECLARE
    _partition_date DATE;
    _partition_name TEXT;
    _partition_start TEXT;
    _partition_end TEXT;
BEGIN
    _partition_date := date_trunc('year', _start_date);
    WHILE _partition_date <= _end_date LOOP
        _partition_name := _table_name || '_' || to_char(_partition_date, 'YYYY');
        _partition_start := to_char(_partition_date, 'YYYY-01-01');
        _partition_end := to_char(_partition_date + INTERVAL '1 year', 'YYYY-01-01');

        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = _partition_name) THEN
            RAISE NOTICE 'Creating yearly partition % for range [%, %)', _partition_name, _partition_start, _partition_end;
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                _partition_name,
                _table_name,
                _partition_start,
                _partition_end
            );

            -- If creating partitions for t_news_ticker, add the foreign key constraint
            IF _table_name = 't_news_ticker' THEN
                RAISE NOTICE 'Adding foreign key to partition %', _partition_name;
                EXECUTE format(
                    'ALTER TABLE %I ADD CONSTRAINT %s FOREIGN KEY (news_id, time_published) REFERENCES public.t_news(id, time_published) ON DELETE CASCADE',
                    _partition_name,
                    _partition_name || '_fk'
                );
            END IF;
        END IF;

        _partition_date := _partition_date + INTERVAL '1 year';
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- =================================================================
-- FUNCTION: purge_old_partitions
-- Description: Drops partitions older than a specified interval, based on a monthly or yearly naming scheme.
-- =================================================================
CREATE OR REPLACE FUNCTION purge_old_partitions(_table_name TEXT, _retention_interval INTERVAL, _partition_type TEXT)
RETURNS void AS $$
DECLARE
    _partition RECORD;
    _cutoff_date DATE;
    _partition_date DATE;
    _date_format TEXT;
BEGIN
    IF _partition_type = 'monthly' THEN
        _cutoff_date := date_trunc('month', NOW() - _retention_interval);
        _date_format := 'YYYY_MM';
    ELSIF _partition_type = 'yearly' THEN
        _cutoff_date := date_trunc('year', NOW() - _retention_interval);
        _date_format := 'YYYY';
    ELSE
        RAISE EXCEPTION 'Invalid partition type: %. Must be "monthly" or "yearly".', _partition_type;
    END IF;

    RAISE NOTICE 'Purging % partitions for table % older than %', _partition_type, _table_name, _cutoff_date;

    FOR _partition IN
        SELECT child.relname AS partition_name
        FROM pg_inherits
        JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
        JOIN pg_class child ON pg_inherits.inhrelid = child.oid
        WHERE parent.relname = _table_name
    LOOP
        -- Extract date from partition name, e.g., 't_news_2020' -> '2020-01-01'
        _partition_date := to_date(
            substring(_partition.partition_name from (length(_table_name) + 2)),
            _date_format
        );

        IF _partition_date < _cutoff_date THEN
            RAISE NOTICE 'Dropping old partition: %', _partition.partition_name;
            EXECUTE format('DROP TABLE %I', _partition.partition_name);
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
    PERFORM create_monthly_partitions('t_share_price', NOW() - INTERVAL '10 years', NOW() + INTERVAL '2 years');

    RAISE NOTICE '--- Creating initial partitions for t_news (yearly) ---';
    PERFORM create_yearly_partitions('t_news', NOW() - INTERVAL '10 years', NOW() + INTERVAL '2 years');

    RAISE NOTICE '--- Creating initial partitions for t_news_ticker (yearly) ---';
    PERFORM create_yearly_partitions('t_news_ticker', NOW() - INTERVAL '10 years', NOW() + INTERVAL '2 years');

    RAISE NOTICE '--- Purging old yearly partitions for news tables ---';
    -- Purge news data older than 3 years
    PERFORM purge_old_partitions('t_news', '3 years', 'yearly');
    PERFORM purge_old_partitions('t_news_ticker', '3 years', 'yearly');
END;
$$;
