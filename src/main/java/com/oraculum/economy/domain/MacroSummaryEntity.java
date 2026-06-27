package com.oraculum.economy.domain;

import com.oraculum.economy.api.domain.MacroIndicator;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Getter
@Table(name = "v_macro_summary")
public class MacroSummaryEntity {

    @Id
    @Column(name = "indicator_code")
    @Enumerated(EnumType.STRING)
    private MacroIndicator indicator;

    @Column(name = "latest_date")
    private LocalDate latestDate;

    @Column(name = "latest_value")
    private Double latestValue;

    @Column(name = "value_1y_ago")
    private Double value1yAgo;

    @Column(name = "yoy_change_pct")
    private Double yoyChangePct;

    @Column(name = "min_1y")
    private Double min1y;

    @Column(name = "max_1y")
    private Double max1y;

    @Column(name = "avg_1y")
    private Double avg1y;

    @Column(name = "diff_from_1y_avg")
    private Double diffFrom1yAvg;

    protected MacroSummaryEntity() {
    }
}
