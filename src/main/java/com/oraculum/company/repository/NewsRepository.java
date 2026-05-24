package com.oraculum.company.repository;

import com.oraculum.company.domain.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<NewsEntity, NewsEntity.NewsId> {
}