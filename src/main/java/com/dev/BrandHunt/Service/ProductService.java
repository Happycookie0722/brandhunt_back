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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * [교육용 설명]
 * ProductService는 상품 기능의 중심 서비스 계층이다.
 *
 * Controller는 HTTP 요청/응답을 담당하고,
 * SeleniumService는 웹사이트에서 데이터를 가져오며,
 * ProductRepository는 DB 접근을 담당한다.
 * 그 사이에서 실제 업무 규칙을 결정하는 곳이 ProductService다.
 */
@Service
@RequiredArgsConstructor

public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryService categoryService;
    private final SearchService searchService;
    private final SeleniumService seleniumService;
    private final PriceAlertService priceAlertService;

    /**
     * 현재 판매 중인 상품만 목록으로 반환한다.
     */
    public List<ProductListDto> getProducts() {
        try {
            return productRepository.findByActiveTrue().stream()
                    .map(this::toListDto)
                    .toList();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Entity를 API 응답 DTO로 변환한다.
     * Entity를 그대로 반환하지 않는 이유는 DB 구조와 API 구조를 분리하기 위해서다.
     */
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

    /**
     * 상품 상세 정보를 조회한다.
     *
     * active=true 조건을 함께 검사하기 때문에
     * 판매 종료 상품의 상세 URL을 직접 호출해도 정상 상품처럼 노출하지 않는다.
     */
    public ProductDetailDto getProductDetail(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
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

    /**
     * 상품명을 검색한다.
     * 검색 결과도 active 상품으로 제한하여 판매 종료 상품이 다시 나타나는 것을 막는다.
     */
    public List<ProductListDto> findProduct(ProductDto productDto) {
        String keyword = productDto.getName();

        if (keyword == null || keyword.isBlank()) {
            throw new CustomException(ErrorCode.EMPTY_SEARCH_QUERY);
        }

        try {
            String normalizedKeyword = keyword.trim();
            searchService.recordSearch(new SearchLog(normalizedKeyword));

            return productRepository
                    .findByNameContainingIgnoreCaseAndActiveTrue(normalizedKeyword)
                    .stream()
                    .map(this::toListDto)
                    .toList();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 브랜드 사이트를 크롤링하고 DB의 상품 상태를 동기화한다.
     *
     * 핵심 규칙:
     * 1. 이번 크롤링에서 확인된 판매 가능 상품 -> active=true
     * 2. 이번 크롤링에서 품절로 확인된 상품 -> active=false
     * 3. 해당 브랜드의 이번 크롤링 결과에서 아예 사라진 기존 상품 -> active=false
     * 4. 다음 크롤링에서 다시 등장하면 active=true로 복구
     *
     * 따라서 DB는 상품의 "현재 판매 여부"를 함께 관리하게 된다.
     */
    @Transactional
    public CrawlResultDto crawlingItem() {
        try {
            List<ProductCrawlDto> nikeProducts = seleniumService.getNikeProduct();
            List<ProductCrawlDto> adidasProducts = seleniumService.getAdidasProduct();

            List<Category> categories = categoryService.getCategoryInfo();

            int inserted = 0;
            int updated = 0;

            Map<String, Set<String>> crawledExternalIdsByBrand = new HashMap<>();

            for (ProductCrawlDto dto : nikeProducts) {
                if (processCrawledProduct(dto, categories)) {
                    inserted++;
                } else {
                    updated++;
                }
                registerSeenProduct(crawledExternalIdsByBrand, dto);
            }

            for (ProductCrawlDto dto : adidasProducts) {
                if (processCrawledProduct(dto, categories)) {
                    inserted++;
                } else {
                    updated++;
                }
                registerSeenProduct(crawledExternalIdsByBrand, dto);
            }

            /*
             * 상품 목록에서 완전히 사라진 상품을 비활성화한다.
             * 단, 크롤링 결과가 0건이면 사이트 장애/셀렉터 변경 가능성이 있으므로
             * 기존 상품을 전부 비활성화하지 않는다.
             */
            deactivateMissingProducts(crawledExternalIdsByBrand);

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

    /**
     * 크롤링 결과 한 건을 DB에 반영한다.
     *
     * @return true면 신규 상품, false면 기존 상품
     */
    private boolean processCrawledProduct(ProductCrawlDto dto, List<Category> categories) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return false;
        }

        Brand brand = brandRepository.findByNameIgnoreCase(dto.getBrand())
                .orElseGet(() -> {
                    Brand newBrand = new Brand();
                    newBrand.setName(dto.getBrand());
                    return brandRepository.save(newBrand);
                });

        String name = dto.getName().trim();
        String crawledPrice = dto.getOriginalPrice() == null ? "0" : dto.getOriginalPrice();
        String crawledSalePrice = dto.getSalePrice() == null ? "0" : dto.getSalePrice();

        /*
         * 외부 상품 ID가 가장 안정적인 식별자다.
         * 상품명이 변경될 수 있기 때문에 이름보다 먼저 조회한다.
         */
        Product product = null;

        if (dto.getExternalProductId() != null && !dto.getExternalProductId().isBlank()) {
            product = productRepository
                    .findByBrandIdAndExternalProductId(brand.getId(), dto.getExternalProductId())
                    .orElse(null);
        }

        // 기존 데이터 호환을 위해 externalProductId가 없던 상품은 이름으로 한 번 더 찾는다.
        if (product == null) {
            product = productRepository
                    .findByBrandIdAndNameIgnoreCase(brand.getId(), name)
                    .orElse(null);
        }

        if (product == null) {
            Product newProduct = new Product();
            newProduct.setBrand(brand);
            newProduct.setCategory(findCategory(dto.getCategory(), categories));
            newProduct.setName(name);
            newProduct.setImg(dto.getImageUrl());
            newProduct.setPrice(crawledPrice);
            newProduct.setSalePrice(crawledSalePrice);
            newProduct.setProductUrl(dto.getProductUrl());
            newProduct.setExternalProductId(dto.getExternalProductId());
            newProduct.setGender(dto.getGender());

            // 새로 크롤링된 품절 상품은 DB에는 보존하되 사용자에게 노출하지 않는다.
            newProduct.setActive(!dto.isSoldOut());

            productRepository.save(newProduct);
            return true;
        }

        String previousSalePrice = product.getSalePrice();
        boolean salePriceChanged = !Objects.equals(previousSalePrice, crawledSalePrice);
        boolean originalPriceChanged = !Objects.equals(product.getPrice(), crawledPrice);
        boolean activeChanged = product.isActive() == dto.isSoldOut();

        /*
         * 가격 변경 시 가격만 수정하고,
         * 판매 상태가 바뀌면 active만 수정한다.
         * 이미지/URL 등은 현재 요구사항대로 불필요한 변경을 하지 않는다.
         */
        if (salePriceChanged || originalPriceChanged || activeChanged) {
            product.setPrice(crawledPrice);
            product.setSalePrice(crawledSalePrice);
            product.setActive(!dto.isSoldOut());

            // 기존 데이터에 외부 ID가 없었다면 이번 크롤링에서 보완한다.
            if (product.getExternalProductId() == null || product.getExternalProductId().isBlank()) {
                product.setExternalProductId(dto.getExternalProductId());
            }

            productRepository.save(product);

            // 실제 할인가가 변경된 경우에만 가격 알림을 생성한다.
            if (salePriceChanged && product.isActive()) {
                priceAlertService.createPriceChangeNotifications(
                        product, previousSalePrice, crawledSalePrice);
            }
        }

        return false;
    }

    /**
     * 이번 크롤링에서 실제로 확인된 상품 ID를 브랜드별 Set에 기록한다.
     * Set을 사용하면 같은 상품이 여러 카테고리에 등장해도 중복 처리되지 않는다.
     */
    private void registerSeenProduct(
            Map<String, Set<String>> crawledExternalIdsByBrand,
            ProductCrawlDto dto) {

        if (dto.getBrand() == null) {
            return;
        }

        Set<String> seenProducts = crawledExternalIdsByBrand
                .computeIfAbsent(dto.getBrand(), key -> new HashSet<>());

        // 외부 ID가 있으면 가장 안정적인 식별자로 기록한다.
        if (dto.getExternalProductId() != null && !dto.getExternalProductId().isBlank()) {
            seenProducts.add("ID:" + dto.getExternalProductId());
        }

        // 기존 데이터에 외부 ID가 없는 경우도 정리할 수 있도록 상품명도 함께 기록한다.
        if (dto.getName() != null && !dto.getName().isBlank()) {
            seenProducts.add("NAME:" + dto.getName().trim().toLowerCase());
        }
    }

    /**
     * 이번 크롤링에서 발견되지 않은 기존 상품을 판매 종료로 간주한다.
     *
     * 주의:
     * 크롤링 결과가 0건인 브랜드는 셀렉터 오류나 사이트 장애일 수 있으므로
     * 해당 브랜드의 상품을 일괄 비활성화하지 않는다.
     */
    private void deactivateMissingProducts(Map<String, Set<String>> crawledExternalIdsByBrand) {
        for (Map.Entry<String, Set<String>> entry : crawledExternalIdsByBrand.entrySet()) {
            Brand brand = brandRepository.findByNameIgnoreCase(entry.getKey()).orElse(null);
            if (brand == null) {
                continue;
            }

            Set<String> seenIds = entry.getValue();

            for (Product product : productRepository.findByBrandId(brand.getId())) {
                String externalId = product.getExternalProductId();
                String productName = product.getName();

                boolean seenByExternalId = externalId != null
                        && !externalId.isBlank()
                        && seenIds.contains("ID:" + externalId);

                boolean seenByName = productName != null
                        && seenIds.contains("NAME:" + productName.trim().toLowerCase());

                if (!seenByExternalId && !seenByName && product.isActive()) {
                    product.setActive(false);
                }
            }
        }
    }

    /**
     * 크롤링한 카테고리 이름을 실제 Category Entity로 변환한다.
     */
    private Category findCategory(String categoryName, List<Category> categories) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        return categoryService.matchCategory(categoryName, categories);
    }
}
