package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.DTO.ProductDto;
import com.dev.BrandHunt.DTO.ProductDetailDto;
import com.dev.BrandHunt.Security.UserPrincipal;
import com.dev.BrandHunt.Service.PriceAlertService;
import com.dev.BrandHunt.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(path = "/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final PriceAlertService priceAlertService;

    @GetMapping("/list")
    public ResponseEntity<?> getProductList() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailDto> getProductDetail(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductDetail(productId));
    }

    @PostMapping("/search")
    public ResponseEntity<?> findProduct(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.findProduct(productDto));
    }

    @PostMapping("/crawling")
    public ResponseEntity<?> crawlingItem() {
        return ResponseEntity.ok(productService.crawlingItem());
    }

    @PostMapping("/{productId}/price-alert")
    public ResponseEntity<Boolean> togglePriceAlert(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                priceAlertService.togglePriceAlert(productId, userPrincipal.getUser().getId()));
    }
}
