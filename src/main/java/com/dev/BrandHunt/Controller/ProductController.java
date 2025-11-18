package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.DTO.ProductDto;
import com.dev.BrandHunt.Entity.Product;
import com.dev.BrandHunt.Service.ProductService;
import com.dev.BrandHunt.Service.SearchService;
import com.dev.BrandHunt.Service.SeleniumService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(path = "/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SeleniumService seleniumService;

    @GetMapping("/list")
    public ResponseEntity<?> getProductList() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @PostMapping("/search")
    public ResponseEntity<?> findProduct(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.findProduct(productDto));
    }

    @PostMapping("/crawling")
    public ResponseEntity<?> crawlingItem() throws InterruptedException {
        try {
            seleniumService.getNikeProduct();
            seleniumService.getAdidasProduct();
            return ResponseEntity.ok("크롤링 성공");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("크롤링 실패: " + e.getMessage());
        }
    }

}
