package com.oraculum.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "t_balance_sheet")
@NoArgsConstructor
public class BalanceSheetEntity extends BaseFinancialStatementEntity {
}