package com.dev.BrandHunt.Repository;

import com.dev.BrandHunt.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * [교육용 설명]
 * Repository는 Service가 DB에 직접 SQL을 작성하지 않고
 * 상품 데이터를 조회/저장하도록 도와주는 Spring Data JPA 계층이다.
 *
 * 메서드 이름을 Spring Data JPA 규칙에 맞게 작성하면
 * 구현체와 SQL을 프레임워크가 자동으로 만들어준다.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 검색어가 상품명에 포함되고 현재 판매 중인 상품만 조회한다.
     * "품절/판매종료 상품이 검색되는 문제"를 DB 조회 단계에서 차단한다.
     */
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    /**
     * 브랜드 사이트의 외부 상품 ID를 이용한 정확한 상품 조회다.
     * 상품명이 바뀌더라도 동일 상품을 식별할 수 있기 때문에
     * 크롤링 저장 시 상품명보다 우선해서 사용한다.
     */
    Optional<Product> findByBrandIdAndExternalProductId(Long brandId, String externalProductId);

    /**
     * 과거 데이터와의 호환을 위한 보조 조회다.
     * 기존 상품에 externalProductId가 없거나 잘못 저장된 경우 이름으로 찾는다.
     */
    Optional<Product> findByBrandIdAndNameIgnoreCase(Long brandId, String name);

    /**
     * 현재 판매 중인 상품만 전체 목록으로 가져온다.
     */
    List<Product> findByActiveTrue();

    /**
     * 특정 브랜드에 속한 모든 상품을 가져온다.
     * 크롤링 마지막 단계에서 이번 크롤링 결과에 없는 상품을 비활성화할 때 사용한다.
     */
    List<Product> findByBrandId(Long brandId);

    /**
     * 특정 상품 ID가 현재 판매 중인지까지 검증한다.
     */
    Optional<Product> findByIdAndActiveTrue(Long id);
}
