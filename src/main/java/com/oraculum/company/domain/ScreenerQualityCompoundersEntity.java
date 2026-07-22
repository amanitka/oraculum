package com.oraculum.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_screener_quality_compounders")
public class ScreenerQualityCompoundersEntity extends CompanyOverviewBaseEntity {
}
