# Database and Code Migration Guide

This guide outlines the necessary changes to the database schema and corresponding code to improve data integrity and clarity. The main goal is to use `sim_fin_id` as the central identifier for companies while retaining market-specific context.

## 1. Rename `t_ticker` to `t_company` and Update Entity

The `t_ticker` table will be renamed to `t_company`. The `sim_fin_id` will become the primary key, mapped to the `id` field in the entity.

### Database Changes

**New Schema (`t_company`):**
```sql
CREATE TABLE public.t_company (
    id INTEGER PRIMARY KEY,
    ticker VARCHAR(255) NOT NULL,
    market VARCHAR(10) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    industry_id VARCHAR(255),
    industry_name VARCHAR(255),
    sector_name VARCHAR(255),
    isin VARCHAR(255),
    description TEXT,
    employee_count BIGINT,
    currency VARCHAR(255) NOT NULL,
    cik VARCHAR(255),
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_company_ticker_market UNIQUE (ticker, market)
);
```
**Changes:**
- Renamed table to `t_company`.
- The primary key is now `id`, which will store the `sim_fin_id`.
- `market` column type changed to `VARCHAR(10)`.

### Code Changes
- **`TickerEntity.java` -> `CompanyEntity.java`**: Rename the file and class, update `@Table`, and ensure the `market` field is present. The `id` field (storing `sim_fin_id`) is the `@Id`.
- **`TickerRepository.java` -> `CompanyRepository.java`**: Rename the repository and update its generic type to `JpaRepository<CompanyEntity, Integer>`.

## 2. Financial Statement Tables and Entities

The primary key will be the `composite_key`, and the `market` column will be added. An index will be added to `company_id`.

### Database Changes

**New Schema (Example: `t_balance_sheet`):**
```sql
CREATE TABLE public.t_balance_sheet (
    id VARCHAR(255) PRIMARY KEY,
    company_id INTEGER NOT NULL,
    market VARCHAR(10) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    fiscal_period VARCHAR(255) NOT NULL,
    variant VARCHAR(255) NOT NULL,
    template VARCHAR(255) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL,
    publish_date DATE NOT NULL,
    restated_date DATE,
    extracted_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
);

CREATE INDEX ix_balance_sheet_company_id ON public.t_balance_sheet (company_id);
```
**Changes:**
- The `id` column (from `composite_key`) is the `VARCHAR(255)` `PRIMARY KEY`.
- `market` column type changed to `VARCHAR(10)`.
- **An index is added on `company_id` for faster lookups.**

### Code Changes
- **`BalanceSheetEntity.java`** (and others): Add the `market` field. Add a `@ManyToOne` relationship to `CompanyEntity` on the `companyId` field.

*Apply the same changes (table and index) to `t_cash_flow_statement` and `t_income_statement`.*

## 3. Share Price Table and Entity

The primary key will be a composite of `company_id` and `trade_date`.

### Database Changes

**New Schema (`t_share_price`):**
```sql
CREATE TABLE public.t_share_price (
    company_id INTEGER NOT NULL,
    trade_date DATE NOT NULL,
    market VARCHAR(10) NOT NULL,
    currency VARCHAR(255),
    open REAL,
    high REAL,
    low REAL,
    close REAL,
    adj_close REAL,
    volume BIGINT,
    shares_outstanding BIGINT,
    dividend REAL,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_id, trade_date),
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
) PARTITION BY RANGE (trade_date);
```
**Changes:**
- **The `PRIMARY KEY` is now `(company_id, trade_date)`.**
- The `market` column is kept as a regular column with type `VARCHAR(10)`.

### Code Changes
- **`SharePriceEntity.java`**:
    - The primary key is composite. Create a `SharePriceId` class:
      ```java
      @Embeddable
      public class SharePriceId implements Serializable {
          private Integer companyId;
          private LocalDate tradeDate;
          // equals and hashCode methods
      }
      ```
    - Annotate `SharePriceEntity` with `@IdClass(SharePriceId.class)`.
    - Add `@Id` to `companyId` and `tradeDate` fields.
    - Keep `market` as a standard `@Column` field.

## 4. News Ticker Table and Entity

### Database Changes
**New Schema (`t_news_ticker`):**
```sql
CREATE TABLE public.t_news_ticker (
    news_id VARCHAR(64) NOT NULL,
    ticker VARCHAR(16) NOT NULL,
    market VARCHAR(10) NOT NULL,
    time_published TIMESTAMPTZ NOT NULL,
    ...
    PRIMARY KEY (news_id, ticker, market, time_published)
) PARTITION BY RANGE (time_published);
```
**Changes:**
- `market` column type changed to `VARCHAR(10)`.
