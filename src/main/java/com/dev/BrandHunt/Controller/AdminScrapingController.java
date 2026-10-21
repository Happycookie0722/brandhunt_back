package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.DTO.CrawlResultDto;
import com.dev.BrandHunt.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 상품 수집 API.
 *
 * Selenium으로 나이키/아디다스 상품을 수집하고 ProductService에서 DB와 동기화한다.
 * SecurityConfig에서 관리자 권한(ROLE_ADMIN)만 접근하도록 제한한다.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminScrapingController {

    private final ProductService productService;

    @PostMapping("/scraping")
    public ResponseEntity<CrawlResultDto> runScraping() {
        return ResponseEntity.ok(productService.crawlingItem());
    }
}
