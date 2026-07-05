package com.oraculum.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "t_cash_flow_statement")
@NoArgsConstructor
public class CashFlowStatementEntity extends BaseFinancialStatementEntity {
}
