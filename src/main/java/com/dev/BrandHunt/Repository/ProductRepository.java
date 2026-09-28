package com.dev.BrandHunt.Repository;

import com.dev.BrandHunt.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);

    Optional<Product> findByBrandIdAndExternalProductId(Long brandId, String externalProductId);

    Optional<Product> findByBrandIdAndNameIgnoreCase(Long brandId, String name);
}
