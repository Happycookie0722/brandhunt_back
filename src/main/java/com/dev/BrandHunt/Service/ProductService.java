package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Constant.ErrorCode;
import com.dev.BrandHunt.DTO.CrawlResultDto;
import com.dev.BrandHunt.DTO.ProductCrawlDto;
import com.dev.BrandHunt.DTO.ProductDto;
import com.dev.BrandHunt.Entity.Brand;
import com.dev.BrandHunt.Entity.Category;
import com.dev.BrandHunt.Entity.Product;
import com.dev.BrandHunt.Entity.SearchLog;
import com.dev.BrandHunt.Repository.BrandRepository;
import com.dev.BrandHunt.Repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryService categoryService;
    private final SearchService searchService;
    private final SeleniumService seleniumService;

    public List<Product> getProducts() {
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Product> findProduct(ProductDto productDto) {
        String keyword = productDto.getName();
        if (keyword == null || keyword.isBlank()) {
            throw new CustomException(ErrorCode.EMPTY_SEARCH_QUERY);
        }

        try {
            searchService.recordSearch(new SearchLog(keyword));
            return productRepository.findByNameContainingIgnoreCase(keyword.trim());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public CrawlResultDto crawlingItem() {
        try {
            List<ProductCrawlDto> crawledProducts = seleniumService.getNikeProduct();
            crawledProducts.addAll(seleniumService.getAdidasProduct());

            List<Category> categories = categoryService.getCategoryInfo();
            int inserted = 0;
            int updated = 0;

            for (ProductCrawlDto dto : crawledProducts) {
                if (dto.getName() == null || dto.getName().isBlank()
                        || dto.getExternalProductId() == null || dto.getExternalProductId().isBlank()) {
                    continue;
                }

                Brand brand = brandRepository.findByNameIgnoreCase(dto.getBrand())
                        .orElseGet(() -> {
                            Brand newBrand = new Brand();
                            newBrand.setName(dto.getBrand());
                            return brandRepository.save(newBrand);
                        });

                Product product = productRepository
                        .findByBrandIdAndExternalProductId(brand.getId(), dto.getExternalProductId())
                        .orElseGet(() -> new Product());

                boolean isNew = product.getId() == null;

                product.setBrand(brand);
                product.setCategory(findCategory(dto.getCategory(), categories));
                product.setName(dto.getName());
                product.setImg(dto.getImageUrl());
                product.setPrice(dto.getOriginalPrice() == null ? "0" : dto.getOriginalPrice());
                product.setSalePrice(dto.getSalePrice() == null ? "0" : dto.getSalePrice());
                product.setProductUrl(dto.getProductUrl());
                product.setExternalProductId(dto.getExternalProductId());
                product.setGender(dto.getGender());

                productRepository.save(product);

                if (isNew) inserted++;
                else updated++;
            }

            return CrawlResultDto.builder()
                    .total(inserted + updated)
                    .inserted(inserted)
                    .updated(updated)
                    .build();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Category findCategory(String categoryName, List<Category> categories) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        return categoryService.matchCategory(categoryName, categories);
    }
}
