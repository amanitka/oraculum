package com.oraculum.audit.repository;

import com.oraculum.audit.domain.LlmExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LlmExecutionLogRepository extends JpaRepository<LlmExecutionLogEntity, Long> {
}
