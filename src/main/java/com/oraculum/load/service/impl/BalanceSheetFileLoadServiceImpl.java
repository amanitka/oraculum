package com.oraculum.load.service.impl;

import org.springframework.stereotype.Component;

@Component("balance_sheet")
public class BalanceSheetFileLoadServiceImpl extends AbstractStatementFileLoadService {

    public BalanceSheetFileLoadServiceImpl(PostgresParquetFileLoader postgresParquetFileLoader) {
        super(postgresParquetFileLoader);
    }

    @Override
    protected String getTargetTableName() {
        return "t_balance_sheet";
    }
}