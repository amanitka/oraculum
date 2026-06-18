package com.oraculum.harvester.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_api_usage")
@Getter
@Setter
public class ApiUsageEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private ProviderType provider;

    @Column(name = "last_usage_date", nullable = false)
    private LocalDate lastUsageDate;

    @Column(name = "call_count", nullable = false)
    private Integer callCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
