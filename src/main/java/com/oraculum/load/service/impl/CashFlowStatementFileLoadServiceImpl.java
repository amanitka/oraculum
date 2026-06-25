package com.oraculum.load.service.impl;


import org.springframework.stereotype.Component;

@Component("cash_flow_statement")
public class CashFlowStatementFileLoadServiceImpl extends AbstractStatementFileLoadService {

    public CashFlowStatementFileLoadServiceImpl(PostgresParquetFileLoader postgresParquetFileLoader) {
        super(postgresParquetFileLoader);
    }

    @Override
    protected String getTargetTableName() {
        return "t_cash_flow_statement";
    }
}