package com.dev.BrandHunt.DTO;

import lombok.Builder;
import lombok.Getter;

/**
 * [교육용 설명]
 * 한 번의 크롤링 작업 결과를 API로 반환하는 DTO다.
 * total은 새로 저장하거나 변경된 상품 수,
 * inserted는 신규 저장 수,
 * updated는 기존 상품의 가격/상태 등이 변경된 수를 의미한다.
 */
@Getter
@Builder
public class CrawlResultDto {
    private int total;
    private int inserted;
    private int updated;
}
