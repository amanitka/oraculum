package com.oraculum.load.service.impl;

import com.oraculum.load.domain.Dataset;
import org.springframework.stereotype.Component;

@Component(Dataset.BALANCE_SHEET)
public class BalanceSheetFileLoadServiceImpl extends AbstractStatementFileLoadService {

    public BalanceSheetFileLoadServiceImpl(PostgresParquetFileLoader postgresParquetFileLoader) {
        super(postgresParquetFileLoader);
    }

    @Override
    protected String getTargetTableName() {
        return "t_balance_sheet";
    }
}
