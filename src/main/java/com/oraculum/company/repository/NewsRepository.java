package com.oraculum.company.repository;

import com.oraculum.company.domain.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<NewsEntity, NewsEntity.NewsId> {
    List<NewsEntity> findByIdIn(Collection<String> ids);

    @Query("SELECT MAX(n.timePublished) FROM NewsEntity n")
    Optional<OffsetDateTime> findMaxTimePublished();
}
