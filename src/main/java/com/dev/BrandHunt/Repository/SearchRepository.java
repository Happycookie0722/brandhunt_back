package com.dev.BrandHunt.Repository;

import com.dev.BrandHunt.Entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<SearchLog, Long> {
    List<SearchLog> findAll();
}
