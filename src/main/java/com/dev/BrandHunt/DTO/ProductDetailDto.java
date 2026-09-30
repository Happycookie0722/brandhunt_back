package com.dev.BrandHunt.DTO;

import lombok.Builder;
import lombok.Getter;

/**
 * [교육용 설명]
 * 상품 상세 페이지에서 필요한 데이터만 담는 DTO다.
 * 목록 DTO와 분리해서 API 응답의 목적을 명확하게 한다.
 */
@Getter
@Builder
public class ProductDetailDto {
    private Long id;
    private String brand;
    private String category;
    private String name;
    private String imageUrl;
    private String price;
    private String salePrice;
    private String productUrl;
    private String gender;
}
