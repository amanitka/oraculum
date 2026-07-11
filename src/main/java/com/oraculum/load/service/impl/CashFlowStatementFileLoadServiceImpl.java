package com.oraculum.load.service.impl;


import com.oraculum.load.domain.Dataset;
import org.springframework.stereotype.Component;

@Component(Dataset.CASH_FLOW_STATEMENT)
public class CashFlowStatementFileLoadServiceImpl extends AbstractStatementFileLoadService {

    public CashFlowStatementFileLoadServiceImpl(PostgresParquetFileLoader postgresParquetFileLoader) {
        super(postgresParquetFileLoader);
    }

    @Override
    protected String getTargetTableName() {
        return "t_cash_flow_statement";
    }
}
