package com.oraculum.load.service.impl;

import org.springframework.stereotype.Component;

@Component("income_statement")
public class IncomeStatementFileLoadServiceImpl extends AbstractStatementFileLoadService {

    public IncomeStatementFileLoadServiceImpl(PostgresParquetFileLoader postgresParquetFileLoader) {
        super(postgresParquetFileLoader);
    }

    @Override
    protected String getTargetTableName() {
        return "t_income_statement";
    }
}