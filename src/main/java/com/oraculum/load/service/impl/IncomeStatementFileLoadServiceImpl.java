package com.oraculum.load.service.impl;

import com.oraculum.load.domain.Dataset;
import org.springframework.stereotype.Component;

@Component(Dataset.INCOME_STATEMENT)
public class IncomeStatementFileLoadServiceImpl extends AbstractStatementFileLoadService {

    public IncomeStatementFileLoadServiceImpl(PostgresParquetFileLoader postgresParquetFileLoader) {
        super(postgresParquetFileLoader);
    }

    @Override
    protected String getTargetTableName() {
        return "t_income_statement";
    }
}
