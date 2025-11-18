package com.dev.BrandHunt.Repository;

import com.dev.BrandHunt.DTO.ProductDto;
import com.dev.BrandHunt.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAll();
    List<Product> findByName(ProductDto productDto);
//    List<Product> findByName(List<String> keyword);
}
