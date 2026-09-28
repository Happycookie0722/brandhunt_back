package com.dev.BrandHunt.DTO;

import com.dev.BrandHunt.Constant.Gender;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductCrawlDto {
    private String brand;
    private String category;
    private String name;
    private String imageUrl;
    private String productUrl;
    private String externalProductId;
    private String originalPrice;
    private String salePrice;
    private Gender gender;
}
