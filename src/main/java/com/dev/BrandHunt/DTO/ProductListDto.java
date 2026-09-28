package com.dev.BrandHunt.DTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductListDto {
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
