package com.dev.BrandHunt.Repository;

import com.dev.BrandHunt.Entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    Optional<PriceAlert> findByUserIdAndProductId(Long userId, Long productId);
}
