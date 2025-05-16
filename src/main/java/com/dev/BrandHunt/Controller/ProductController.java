package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/products")
@RequiredArgsConstructor
public class ProductController {
    private ProductService productService;

    @GetMapping("/list")
    public ResponseEntity getProduct() {

        return new ResponseEntity();
    }
}
