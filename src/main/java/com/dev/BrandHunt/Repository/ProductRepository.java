package com.dev.BrandHunt.Repository;

import com.dev.BrandHunt.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * [교육용 설명]
 * Repository는 Service가 DB에 직접 SQL을 작성하지 않고
 * 상품 데이터를 조회/저장하도록 도와주는 Spring Data JPA 계층이다.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    Optional<Product> findByBrandIdAndExternalProductId(Long brandId, String externalProductId);

    Optional<Product> findByBrandIdAndNameIgnoreCase(Long brandId, String name);

    List<Product> findByActiveTrue();

    List<Product> findByBrandId(Long brandId);

    /**
     * 비활성화 후보를 만들 때 현재 판매 중인 상품만 읽는다.
     */
    List<Product> findByBrandIdAndActiveTrue(Long brandId);

    Optional<Product> findByIdAndActiveTrue(Long id);

    /**
     * 여러 상품을 한 번의 UPDATE 문으로 비활성화한다.
     *
     * 개별 entity.setActive(false)를 1,000번 수행하는 대신
     * DB에서 IN 절을 사용하여 일괄 UPDATE한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.active = false where p.id in :productIds and p.active = true")
    int deactivateByIds(@Param("productIds") Collection<Long> productIds);
}
