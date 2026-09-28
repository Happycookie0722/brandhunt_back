package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.DTO.ProductDto;
import com.dev.BrandHunt.Entity.Product;
import com.dev.BrandHunt.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public ResponseEntity<?> getProductList() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @PostMapping("/search")
    public ResponseEntity<?> findProduct(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.findProduct(productDto));
    }

    @PostMapping("/crawling")
    public ResponseEntity<?> crawlingItem() {
        return ResponseEntity.ok(productService.crawlingItem());
    }
}
