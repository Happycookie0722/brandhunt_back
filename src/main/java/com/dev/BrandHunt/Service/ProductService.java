package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Constant.ErrorCode;
import com.dev.BrandHunt.DTO.ProductDto;
import com.dev.BrandHunt.Entity.Product;
import com.dev.BrandHunt.Entity.SearchLog;
import com.dev.BrandHunt.Repository.ProductRepository;
import com.dev.BrandHunt.Repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final SearchService searchService;
    private final SeleniumService seleniumService;

    public List<Product> getProducts() {
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

//    public List<Product> getPopularProducts() {
//        try {
//            return productRepository.findByName(searchService.getPopularKeyword());
//        } catch (Exception e) {
//            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
//        }
//    }

    public List<Product> findProduct(ProductDto productDto) {
        try {
            searchService.recordSearch(new SearchLog(productDto.getName()));
            return productRepository.findByName(productDto);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean crawlingItem() {
        try {
            seleniumService.getNikeProduct();
            seleniumService.getAdidasProduct();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return true;
    }

}
