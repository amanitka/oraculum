-- Migration to relax t_company and company_id foreign key constraints
-- Allow currency to be null in t_company
ALTER TABLE public.t_company ALTER COLUMN currency DROP NOT NULL;

-- Drop foreign key constraints on company_id referencing t_company(id)
ALTER TABLE public.t_company_analysis DROP CONSTRAINT IF EXISTS t_company_analysis_company_id_fkey;
ALTER TABLE public.t_balance_sheet DROP CONSTRAINT IF EXISTS t_balance_sheet_company_id_fkey;
ALTER TABLE public.t_cash_flow_statement DROP CONSTRAINT IF EXISTS t_cash_flow_statement_company_id_fkey;
ALTER TABLE public.t_income_statement DROP CONSTRAINT IF EXISTS t_income_statement_company_id_fkey;
ALTER TABLE public.t_share_price DROP CONSTRAINT IF EXISTS t_share_price_company_id_fkey;
