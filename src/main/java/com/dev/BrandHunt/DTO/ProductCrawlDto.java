package com.dev.BrandHunt.DTO;

import com.dev.BrandHunt.Constant.Gender;
import lombok.Builder;
import lombok.Getter;

/**
 * [교육용 설명]
 * ProductCrawlDto는 Selenium이 수집한 "사이트 원본 데이터"를
 * Product 엔티티와 분리해서 전달하기 위한 DTO다.
 *
 * 크롤러가 JPA Entity를 직접 생성하지 않게 하면
 * "아직 DB에 저장되지 않은 임시 객체"와 "DB의 영속 객체"를 구분할 수 있어
 * 서비스 계층의 책임이 명확해진다.
 */
@Getter
@Builder
public class ProductCrawlDto {

    // 어느 브랜드에서 가져온 데이터인지
    private String brand;

    // 크롤링한 카테고리 이름
    private String category;

    // 브랜드 사이트에서 표시되는 상품명
    private String name;

    // 상품 대표 이미지 URL
    private String imageUrl;

    // 공식 상품 상세 페이지 URL
    private String productUrl;

    // 브랜드 사이트의 외부 상품 ID
    private String externalProductId;

    // 정가
    private String originalPrice;

    // 할인가. 할인하지 않으면 null일 수 있다.
    private String salePrice;

    // 상품 성별
    private Gender gender;

    /**
     * 브랜드 사이트에서 품절/판매불가 상태인지 나타낸다.
     * true면 DB에는 남기되 active=false로 처리한다.
     */
    private boolean soldOut;
}
