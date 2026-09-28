package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Constant.ErrorCode;
import com.dev.BrandHunt.DTO.CrawlResultDto;
import com.dev.BrandHunt.DTO.ProductCrawlDto;
import com.dev.BrandHunt.DTO.ProductDetailDto;
import com.dev.BrandHunt.DTO.ProductListDto;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryService categoryService;
    private final SearchService searchService;
    private final SeleniumService seleniumService;
    private final PriceAlertService priceAlertService;

    public List<ProductListDto> getProducts() {
        try {
            return productRepository.findAll().stream()
                    .map(this::toListDto)
                    .toList();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private ProductListDto toListDto(Product product) {
        return ProductListDto.builder()
                .id(product.getId())
                .brand(product.getBrand() == null ? null : product.getBrand().getName())
                .category(product.getCategory() == null ? null : product.getCategory().getName())
                .name(product.getName())
                .imageUrl(product.getImg())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .productUrl(product.getProductUrl())
                .gender(product.getGender() == null ? null : product.getGender().name())
                .build();
    }

    public ProductDetailDto getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductDetailDto.builder()
                .id(product.getId())
                .brand(product.getBrand() == null ? null : product.getBrand().getName())
                .category(product.getCategory() == null ? null : product.getCategory().getName())
                .name(product.getName())
                .imageUrl(product.getImg())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .productUrl(product.getProductUrl())
                .gender(product.getGender() == null ? null : product.getGender().name())
                .build();
    }

    public List<ProductListDto> findProduct(ProductDto productDto) {
        String keyword = productDto.getName();
        if (keyword == null || keyword.isBlank()) {
            throw new CustomException(ErrorCode.EMPTY_SEARCH_QUERY);
        }

        try {
            searchService.recordSearch(new SearchLog(keyword));
            return productRepository.findByNameContainingIgnoreCase(keyword.trim()).stream()
                    .map(this::toListDto)
                    .toList();
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
                if (dto.getName() == null || dto.getName().isBlank()) {
                    continue;
                }

                Brand brand = brandRepository.findByNameIgnoreCase(dto.getBrand())
                        .orElseGet(() -> {
                            Brand newBrand = new Brand();
                            newBrand.setName(dto.getBrand());
                            return brandRepository.save(newBrand);
                        });

                String crawledPrice = dto.getOriginalPrice() == null ? "0" : dto.getOriginalPrice();
                String crawledSalePrice = dto.getSalePrice() == null ? "0" : dto.getSalePrice();

                Product product = productRepository
                        .findByBrandIdAndNameIgnoreCase(brand.getId(), dto.getName().trim())
                        .orElse(null);

                if (product == null) {
                    product = new Product();
                    product.setBrand(brand);
                    product.setCategory(findCategory(dto.getCategory(), categories));
                    product.setName(dto.getName().trim());
                    product.setImg(dto.getImageUrl());
                    product.setPrice(crawledPrice);
                    product.setSalePrice(crawledSalePrice);
                    product.setProductUrl(dto.getProductUrl());
                    product.setExternalProductId(dto.getExternalProductId());
                    product.setGender(dto.getGender());

                    productRepository.save(product);
                    inserted++;
                    continue;
                }

                String previousSalePrice = product.getSalePrice();
                boolean salePriceChanged = !Objects.equals(previousSalePrice, crawledSalePrice);
                boolean originalPriceChanged = !Objects.equals(product.getPrice(), crawledPrice);

                if (salePriceChanged || originalPriceChanged) {
                    product.setPrice(crawledPrice);
                    product.setSalePrice(crawledSalePrice);
                    productRepository.save(product);
                    updated++;

                    if (salePriceChanged) {
                        priceAlertService.createPriceChangeNotifications(
                                product, previousSalePrice, crawledSalePrice);
                    }
                }
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
