package com.oraculum.economy.domain;

import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroObservationKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "t_macro_observation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_macro_observation", columnNames = {"indicator_code", "observation_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MacroObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_code", nullable = false, length = 50)
    private MacroIndicator indicatorCode;

    @Column(name = "observation_date", nullable = false)
    private LocalDate observationDate;

    @Column
    private Double value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MacroObservationKey getKey() {
        return new MacroObservationKey(indicatorCode, observationDate);
    }
}
