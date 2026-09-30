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
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.ArrayList;
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
 * ProductRepository/JdbcTemplate은 DB 접근을 담당한다.
 * 그 사이에서 실제 업무 규칙과 크롤링 동기화 전략을 결정하는 곳이 ProductService다.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int JDBC_BATCH_SIZE = 50;

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryService categoryService;
    private final SearchService searchService;
    private final SeleniumService seleniumService;
    private final PriceAlertService priceAlertService;
    private final JdbcTemplate jdbcTemplate;

    public List<ProductListDto> getProducts() {
        try {
            return productRepository.findByActiveTrue().stream()
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
     * 크롤링 결과를 DB와 동기화한다.
     *
     * 성능상 중요한 원칙:
     * 1. 상품 1개마다 SELECT하지 않는다.
     *    브랜드별 기존 상품을 한 번에 읽어서 메모리 Map으로 비교한다.
     * 2. 신규 상품 INSERT는 JdbcTemplate.batchUpdate()로 묶는다.
     * 3. 기존 상품 UPDATE는 관리 중인 JPA Entity의 dirty checking에 맡기고
     *    Hibernate JDBC batching으로 묶는다.
     * 4. 판매 종료 상품 UPDATE는 IN 절을 이용한 bulk UPDATE로 처리한다.
     *
     * 따라서 1,000개 상품을 처리하더라도 DB round trip을
     * "상품 수에 비례하는 형태"에서 "배치 수에 비례하는 형태"로 줄인다.
     */
    @Transactional
    public CrawlResultDto crawlingItem() {
        try {
            List<ProductCrawlDto> nikeProducts = seleniumService.getNikeProduct();
            List<ProductCrawlDto> adidasProducts = seleniumService.getAdidasProduct();
            List<Category> categories = categoryService.getCategoryInfo();

            Map<String, Set<String>> crawledProductsByBrand = new HashMap<>();

            SyncResult nikeResult = syncBrandProducts(nikeProducts, categories, crawledProductsByBrand);
            SyncResult adidasResult = syncBrandProducts(adidasProducts, categories, crawledProductsByBrand);

            deactivateMissingProducts(crawledProductsByBrand);

            return CrawlResultDto.builder()
                    .total(nikeResult.total() + adidasResult.total())
                    .inserted(nikeResult.inserted() + adidasResult.inserted())
                    .updated(nikeResult.updated() + adidasResult.updated())
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 브랜드 하나의 크롤링 결과를 일괄 동기화한다.
     *
     * 이 메서드 내부의 for문 자체는 문제가 아니다.
     * 중요한 것은 for문 안에서 DB를 호출하지 않는 것이다.
     * 모든 비교는 메모리 Map에서 수행하고, DB 작업은 마지막에 배치로 실행한다.
     */
    private SyncResult syncBrandProducts(
            List<ProductCrawlDto> crawledProducts,
            List<Category> categories,
            Map<String, Set<String>> crawledProductsByBrand) {

        if (crawledProducts == null || crawledProducts.isEmpty()) {
            return new SyncResult(0, 0, 0);
        }

        String brandName = crawledProducts.get(0).getBrand();
        if (brandName == null || brandName.isBlank()) {
            return new SyncResult(0, 0, 0);
        }

        Brand brand = brandRepository.findByNameIgnoreCase(brandName)
                .orElseGet(() -> {
                    Brand newBrand = new Brand();
                    newBrand.setName(brandName);
                    return brandRepository.save(newBrand);
                });

        /*
         * 여기서 딱 한 번만 해당 브랜드의 기존 상품을 읽는다.
         * 이후 1,000개 상품을 비교할 때는 DB를 조회하지 않고 Map을 사용한다.
         */
        List<Product> existingProducts = productRepository.findByBrandId(brand.getId());

        Map<String, Product> productsByExternalId = new HashMap<>();
        Map<String, Product> productsByName = new HashMap<>();

        for (Product product : existingProducts) {
            if (product.getExternalProductId() != null && !product.getExternalProductId().isBlank()) {
                productsByExternalId.put(product.getExternalProductId(), product);
            }
            if (product.getName() != null && !product.getName().isBlank()) {
                productsByName.put(normalize(product.getName()), product);
            }
        }

        List<Product> newProducts = new ArrayList<>();
        Set<String> pendingInsertKeys = new HashSet<>();

        int updated = 0;

        for (ProductCrawlDto dto : crawledProducts) {
            if (dto.getName() == null || dto.getName().isBlank()) {
                continue;
            }

            String name = dto.getName().trim();
            Product product = findExistingProduct(
                    dto,
                    name,
                    productsByExternalId,
                    productsByName
            );

            if (product == null) {
                String newProductKey = buildProductKey(dto, name);

                // 동일 크롤링 결과에 같은 상품이 중복으로 들어오는 것도 방지한다.
                if (!pendingInsertKeys.add(newProductKey)) {
                    registerSeenProduct(crawledProductsByBrand, dto);
                    continue;
                }

                Product newProduct = createProductEntity(dto, brand, name, categories);
                newProducts.add(newProduct);

                // 같은 실행 안에서 중복 DTO가 다시 들어와도 신규 INSERT를 중복 생성하지 않도록
                // 메모리 Map에도 등록한다.
                if (dto.getExternalProductId() != null && !dto.getExternalProductId().isBlank()) {
                    productsByExternalId.put(dto.getExternalProductId(), newProduct);
                }
                productsByName.put(normalize(name), newProduct);

            } else {
                updateExistingProduct(product, dto, name);
                updated++;
            }

            registerSeenProduct(crawledProductsByBrand, dto);
        }

        /*
         * 신규 상품은 IDENTITY PK 때문에 Hibernate JDBC INSERT batching을 사용할 수 없다.
         * 따라서 JdbcTemplate의 batchUpdate로 INSERT를 직접 배치한다.
         */
        batchInsertProducts(newProducts, brand);

        return new SyncResult(
                newProducts.size() + updated,
                newProducts.size(),
                updated
        );
    }

    private Product findExistingProduct(
            ProductCrawlDto dto,
            String name,
            Map<String, Product> productsByExternalId,
            Map<String, Product> productsByName) {

        if (dto.getExternalProductId() != null && !dto.getExternalProductId().isBlank()) {
            Product product = productsByExternalId.get(dto.getExternalProductId());
            if (product != null) {
                return product;
            }
        }

        return productsByName.get(normalize(name));
    }

    private Product createProductEntity(
            ProductCrawlDto dto,
            Brand brand,
            String name,
            List<Category> categories) {

        Product product = new Product();
        product.setBrand(brand);
        product.setCategory(findCategory(dto.getCategory(), categories));
        product.setName(name);
        product.setImg(dto.getImageUrl());
        product.setPrice(dto.getOriginalPrice() == null ? "0" : dto.getOriginalPrice());
        product.setSalePrice(dto.getSalePrice() == null ? "0" : dto.getSalePrice());
        product.setProductUrl(dto.getProductUrl());
        product.setExternalProductId(dto.getExternalProductId());
        product.setGender(dto.getGender());
        product.setActive(!dto.isSoldOut());
        return product;
    }

    /**
     * 기존 Entity는 이미 현재 Transaction의 Persistence Context에서 관리되고 있다.
     * 따라서 productRepository.save(product)를 매번 호출할 필요가 없다.
     * 필드만 변경하면 Transaction commit/flush 시 Hibernate가 변경 내용을 감지한다.
     */
    private void updateExistingProduct(Product product, ProductCrawlDto dto, String name) {
        String crawledPrice = dto.getOriginalPrice() == null ? "0" : dto.getOriginalPrice();
        String crawledSalePrice = dto.getSalePrice() == null ? "0" : dto.getSalePrice();

        String previousSalePrice = product.getSalePrice();
        boolean salePriceChanged = !Objects.equals(previousSalePrice, crawledSalePrice);
        boolean originalPriceChanged = !Objects.equals(product.getPrice(), crawledPrice);
        boolean activeChanged = product.isActive() == dto.isSoldOut();

        if (!salePriceChanged && !originalPriceChanged && !activeChanged) {
            return;
        }

        product.setPrice(crawledPrice);
        product.setSalePrice(crawledSalePrice);
        product.setActive(!dto.isSoldOut());

        if (product.getExternalProductId() == null || product.getExternalProductId().isBlank()) {
            product.setExternalProductId(dto.getExternalProductId());
        }

        if (salePriceChanged && product.isActive()) {
            priceAlertService.createPriceChangeNotifications(
                    product,
                    previousSalePrice,
                    crawledSalePrice
            );
        }
    }

    /**
     * 신규 상품을 50건 단위 JDBC batch로 INSERT한다.
     *
     * 1,000건이면 SQL 1,000번을 각각 execute하는 것이 아니라
     * 50건씩 JDBC batch로 전달하여 네트워크 왕복을 크게 줄인다.
     *
     * Product의 ID는 DB AUTO_INCREMENT가 생성하므로 이 경로에서는
     * 새 Product Entity의 id를 즉시 사용할 필요가 없는 구조로 유지한다.
     */
    private void batchInsertProducts(List<Product> products, Brand brand) {
        if (products.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO products
                (brand_id, category_id, name, img, price, sale_price,
                 product_url, external_product_id, gender, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                products,
                JDBC_BATCH_SIZE,
                (PreparedStatement ps, Product product) -> {
                    ps.setLong(1, brand.getId());

                    if (product.getCategory() == null) {
                        ps.setObject(2, null);
                    } else {
                        ps.setLong(2, product.getCategory().getId());
                    }

                    ps.setString(3, product.getName());
                    ps.setString(4, product.getImg());
                    ps.setString(5, product.getPrice());
                    ps.setString(6, product.getSalePrice());
                    ps.setString(7, product.getProductUrl());
                    ps.setString(8, product.getExternalProductId());
                    ps.setString(9, product.getGender().name());
                    ps.setBoolean(10, product.isActive());
                }
        );
    }

    /**
     * 이번 크롤링에서 실제로 확인된 상품을 브랜드별 Set에 기록한다.
     */
    private void registerSeenProduct(
            Map<String, Set<String>> crawledProductsByBrand,
            ProductCrawlDto dto) {

        if (dto.getBrand() == null) {
            return;
        }

        Set<String> seenProducts = crawledProductsByBrand
                .computeIfAbsent(dto.getBrand(), key -> new HashSet<>());

        if (dto.getExternalProductId() != null && !dto.getExternalProductId().isBlank()) {
            seenProducts.add("ID:" + dto.getExternalProductId());
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            seenProducts.add("NAME:" + normalize(dto.getName()));
        }
    }

    /**
     * 이번 크롤링에서 발견되지 않은 기존 active 상품을 한 번에 비활성화한다.
     *
     * 기존 구현:
     *   SELECT -> for -> product.setActive(false) -> dirty checking
     *
     * 개선 구현:
     *   SELECT 1회 -> 메모리에서 missing ID 계산 -> UPDATE ... WHERE id IN (...)
     *
     * 1,000개 상품이라도 entity별 UPDATE를 만들지 않는다.
     */
    private void deactivateMissingProducts(
            Map<String, Set<String>> crawledProductsByBrand) {

        for (Map.Entry<String, Set<String>> entry : crawledProductsByBrand.entrySet()) {
            Brand brand = brandRepository.findByNameIgnoreCase(entry.getKey()).orElse(null);
            if (brand == null) {
                continue;
            }

            Set<String> seenProducts = entry.getValue();

            List<Long> missingProductIds = productRepository
                    .findByBrandIdAndActiveTrue(brand.getId())
                    .stream()
                    .filter(product -> !isSeenProduct(product, seenProducts))
                    .map(Product::getId)
                    .toList();

            /*
             * IN 절은 DB parameter가 너무 커지지 않도록 500건 단위로 나눈다.
             * 1,000건이면 UPDATE 약 2번으로 끝난다.
             */
            for (int start = 0; start < missingProductIds.size(); start += 500) {
                List<Long> batchIds = missingProductIds.subList(
                        start,
                        Math.min(start + 500, missingProductIds.size())
                );

                productRepository.deactivateByIds(batchIds);
            }
        }
    }

    private boolean isSeenProduct(Product product, Set<String> seenProducts) {
        String externalId = product.getExternalProductId();

        boolean seenByExternalId = externalId != null
                && !externalId.isBlank()
                && seenProducts.contains("ID:" + externalId);

        boolean seenByName = product.getName() != null
                && seenProducts.contains("NAME:" + normalize(product.getName()));

        return seenByExternalId || seenByName;
    }

    private String buildProductKey(ProductCrawlDto dto, String name) {
        if (dto.getExternalProductId() != null && !dto.getExternalProductId().isBlank()) {
            return "ID:" + dto.getExternalProductId();
        }
        return "NAME:" + normalize(name);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private Category findCategory(String categoryName, List<Category> categories) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        return categoryService.matchCategory(categoryName, categories);
    }

    private record SyncResult(int total, int inserted, int updated) {
    }
}
