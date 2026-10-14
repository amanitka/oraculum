package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "t_sec_filer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecFilerEntity {

    @Id
    @Column(name = "cik", length = 10, nullable = false)
    private String cik;

    @Column(name = "manager_name", length = 255, nullable = false)
    private String managerName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "tier", nullable = false)
    private short tier;

    @Column(name = "last_filing_date")
    private LocalDate lastFilingDate;
}
