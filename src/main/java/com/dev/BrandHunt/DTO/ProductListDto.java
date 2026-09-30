package com.dev.BrandHunt.DTO;

import lombok.Builder;
import lombok.Getter;

/**
 * [교육용 설명]
 * ProductListDto는 상품 목록 화면 전용 DTO다.
 *
 * Entity를 그대로 JSON으로 반환하면 Brand -> Product -> Brand처럼
 * 양방향 연관관계가 JSON으로 반복 직렬화될 수 있다.
 * 따라서 API 응답에 필요한 필드만 DTO로 만들어 반환한다.
 */
@Getter
@Builder
public class ProductListDto {
    private Long id;
    private String brand;
    private String category;
    private String name;
    private String imageUrl;
    private String price;
    private String salePrice;
    private String productUrl;
    private String gender;

    /**
     * active 상품만 Service에서 DTO로 변환하므로
     * 현재 화면에는 판매 가능한 상품만 전달된다.
     */
}
