package com.oraculum.company.repository;

import com.oraculum.company.domain.SecFilerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecFilerRepository extends JpaRepository<SecFilerEntity, String> {

    List<SecFilerEntity> findByTierOrderByManagerNameAsc(short tier);
}
