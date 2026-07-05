package com.oraculum.company.repository;

import com.oraculum.company.domain.IndustryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndustryRepository extends JpaRepository<IndustryEntity, String> {
}
