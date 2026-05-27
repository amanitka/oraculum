package com.oraculum.company.repository;

import com.oraculum.company.domain.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<NewsEntity, NewsEntity.NewsId> {
    List<NewsEntity> findByIdIn(Collection<String> ids);
}