package com.oraculum.audit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_llm_execution_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlmExecutionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "correlation_type", length = 50)
    private String correlationType;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "response", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String response;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
