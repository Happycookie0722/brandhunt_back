package com.dev.BrandHunt.Entity;

import com.dev.BrandHunt.Constant.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * [교육용 설명]
 * Product는 DB의 products 테이블과 1:1로 대응하는 핵심 상품 엔티티다.
 *
 * 전체 흐름:
 * 1. SeleniumService가 브랜드 사이트에서 상품 정보를 수집한다.
 * 2. ProductService가 수집 결과를 Product로 변환/저장한다.
 * 3. Controller가 Product를 DTO로 변환하여 프론트엔드에 전달한다.
 *
 * 중요한 설계:
 * - active=true  : 현재 브랜드 사이트에서 판매 가능한 상품
 * - active=false : 판매 종료/품절/크롤링 결과에서 사라진 과거 상품
 *
 * 과거 상품을 DB에서 물리적으로 삭제하지 않고 active 상태로 관리하는 이유:
 * 가격 알림, 검색 이력, 통계 등의 연관 데이터를 보존하면서
 * 사용자 화면에서는 현재 판매 상품만 보여주기 위해서다.
 */
@Getter
@Setter
@Entity
@Table(name = "products",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_product_brand_external_id",
               columnNames = {"brand_id", "external_product_id"}
       ))
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 브랜드의 상품인지 연결한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    // 상품의 상위 카테고리다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // 사용자에게 보여줄 상품명이다.
    @NotNull
    private String name;

    // 상품 대표 이미지 URL이다.
    @NotNull
    private String img;

    // 정가/기준 가격이다.
    @NotNull
    private String price;

    // 현재 할인가다. 할인하지 않는 상품은 "0"으로 저장한다.
    @ColumnDefault("0")
    private String salePrice;

    // 실제 브랜드 공식 상품 상세 페이지 URL이다.
    private String productUrl;

    // Nike/Adidas 사이트에서 사용하는 외부 상품 식별자다.
    @Column(name = "external_product_id")
    private String externalProductId;

    // 남성/여성 등의 상품 성별 구분이다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /**
     * 현재 판매 가능한 상품인지 나타낸다.
     * 목록/검색 API에서는 true인 상품만 사용자에게 노출한다.
     */
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean active = true;
}
