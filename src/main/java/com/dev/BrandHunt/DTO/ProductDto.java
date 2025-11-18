package com.dev.BrandHunt.DTO;

import lombok.Getter;

@Getter
public class ProductDto {
    String category;
    String name;
    String image;
    int originalPrice;
    int salePrice;
}
