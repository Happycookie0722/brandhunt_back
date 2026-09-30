package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Config.SeleniumConfig;
import com.dev.BrandHunt.Constant.Gender;
import com.dev.BrandHunt.Constant.SiteType;
import com.dev.BrandHunt.DTO.ProductCrawlDto;
import com.dev.BrandHunt.Entity.Category;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * [교육용 설명]
 * SeleniumService는 실제 브랜드 웹사이트를 브라우저처럼 열어 상품 정보를 수집한다.
 *
 * 흐름:
 * Controller -> ProductService.crawlingItem()
 * -> SeleniumService.getNikeProduct()/getAdidasProduct()
 * -> ProductCrawlDto 목록 반환
 * -> ProductService가 DB 저장/갱신
 *
 * 이 클래스의 책임은 웹에서 데이터를 읽는 것까지다.
 * DB 저장과 사용자 노출 여부는 ProductService가 결정한다.
 */
@Service
@RequiredArgsConstructor

public class SeleniumService {
    private final CategoryService categoryService;
    private final SeleniumConfig seleniumConfig;

    public List<ProductCrawlDto> getNikeProduct() throws InterruptedException {
        List<ProductCrawlDto> products = new ArrayList<>();
        WebDriver driver = seleniumConfig.createWebDriver(SiteType.NIKE);

        try {
            String[] urls = {
                    "https://www.nike.com/kr/w/men-shoes-nik1zy7ok",
                    "https://www.nike.com/kr/w/men-apparel-6ymx6znik1",
                    "https://www.nike.com/kr/w/men-bags-backpacks-9xy71znik1",
                    "https://www.nike.com/kr/w/men-hats-visors-headbands-52r49znik1",
                    "https://www.nike.com/kr/w/women-shoes-5e1x6zy7ok",
                    "https://www.nike.com/kr/w/women-apparel-6ymx6znik1",
                    "https://www.nike.com/kr/w/women-bags-backpacks-9xy71znik1",
                    "https://www.nike.com/kr/w/women-hats-visors-headbands-52r49znik1"
            };

            List<Category> categories = categoryService.getCategoryInfo();

            for (String url : urls) {
                Category category = categoryService.matchCategory(url, categories);
                driver.get(url);

                JavascriptExecutor js = (JavascriptExecutor) driver;
                int prevCount = 0;
                int sameCount = 0;

                while (sameCount < 10) {
                    js.executeScript("window.scrollBy(0, 2500);");
                    Thread.sleep(4000);

                    List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
                    int count = cards.size();
                    if (count == 0) break;

                    if (count == prevCount) sameCount++;
                    else {
                        sameCount = 0;
                        prevCount = count;
                    }
                }

                for (WebElement card : driver.findElements(By.cssSelector(".product-card"))) {
                    try {
                        String name = card.findElement(By.cssSelector(".product-card__title")).getText();
                        String image = card.findElement(By.tagName("img")).getAttribute("src");
                        String productUrl = extractHref(card);
                        String externalProductId = extractExternalProductId(productUrl);
                        if (externalProductId == null) continue;

                        List<WebElement> prices = card.findElements(By.cssSelector(".product-price"));
                        String originalPrice = !prices.isEmpty() ? prices.get(0).getText() : null;
                        String salePrice = prices.size() > 1 ? prices.get(1).getText() : null;
                        // 브랜드 사이트에서 카드 자체에 품절 표시가 있으면 판매 불가 상품으로 기록한다.
                        boolean soldOut = card.getText().contains("품절");
                        Gender gender = url.contains("/men-") ? Gender.MALE : Gender.FEMALE;

                        products.add(ProductCrawlDto.builder()
                                .brand("Nike")
                                .category(category != null ? category.getName() : null)
                                .name(name)
                                .imageUrl(image)
                                .productUrl(productUrl)
                                .externalProductId(externalProductId)
                                .originalPrice(originalPrice)
                                .salePrice(salePrice)
                                .gender(gender)
                                .soldOut(soldOut)
                                .build());
                    } catch (Exception e) {
                        System.out.println("나이키 상품 파싱 실패: " + e.getMessage());
                    }
                }
            }
        } finally {
            driver.quit();
        }
        return products;
    }

    public List<ProductCrawlDto> getAdidasProduct() {
        List<ProductCrawlDto> products = new ArrayList<>();
        WebDriver driver = seleniumConfig.createWebDriver(SiteType.ADIDAS);

        try {
            String[] urls = {
                    "https://www.adidas.co.kr/men-shoes",
                    "https://www.adidas.co.kr/men-clothing",
                    "https://www.adidas.co.kr/men-bags-accessories",
                    "https://www.adidas.co.kr/men-headwear",
                    "https://www.adidas.co.kr/women-shoes",
                    "https://www.adidas.co.kr/women-clothing",
                    "https://www.adidas.co.kr/women-bags-accessories",
                    "https://www.adidas.co.kr/women-headwear"
            };

            List<Category> categories = categoryService.getCategoryInfo();

            for (String url : urls) {
                driver.get(url);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                closeAdidasPopups(wait);

                while (true) {
                    List<WebElement> cards =
                            driver.findElements(By.cssSelector("article[data-testid='plp-product-card']"));

                    for (WebElement card : cards) {
                        try {
                            String name = card.findElement(By.cssSelector("p[data-testid='product-card-title']")).getText();
                            String categoryText = card.findElement(By.cssSelector("p[data-testid='product-card-subtitle']")).getText();
                            String image = card.findElement(By.cssSelector("img[data-testid='product-card-primary-image']")).getAttribute("src");
                            String productUrl = extractHref(card);
                            String externalProductId = extractExternalProductId(productUrl);
                            if (externalProductId == null) continue;

                            List<WebElement> originalComponent =
                                    card.findElements(By.cssSelector("div[data-testid='main-price']"));
                            List<WebElement> saleComponent =
                                    card.findElements(By.cssSelector("div[data-testid='original-price']"));
                            List<WebElement> soldOut =
                                    card.findElements(By.cssSelector("a[data-testid='product-card-description-link'] div[data-testid='sold-out']"));

                            String originalPrice = null;
                            String salePrice = null;

                            if (!soldOut.isEmpty()) {
                                originalPrice = "품절";
                            } else if (!saleComponent.isEmpty() && !originalComponent.isEmpty()) {
                                List<WebElement> originalSpans = originalComponent.get(0).findElements(By.tagName("span"));
                                List<WebElement> saleSpans = saleComponent.get(0).findElements(By.tagName("span"));
                                originalPrice = saleSpans.isEmpty() ? null : saleSpans.get(0).getText();
                                salePrice = originalSpans.size() > 1 ? originalSpans.get(1).getText() : null;
                            } else if (!originalComponent.isEmpty()) {
                                List<WebElement> spans = originalComponent.get(0).findElements(By.tagName("span"));
                                originalPrice = spans.size() > 1 ? spans.get(1).getText() : null;
                            }

                            Category category = categoryService.matchCategory(categoryText, categories);
                            Gender gender = url.contains("/men-") ? Gender.MALE : Gender.FEMALE;

                            products.add(ProductCrawlDto.builder()
                                    .brand("Adidas")
                                    .category(category != null ? category.getName() : categoryText)
                                    .name(name)
                                    .imageUrl(image)
                                    .productUrl(productUrl)
                                    .externalProductId(externalProductId)
                                    .originalPrice(originalPrice)
                                    .salePrice(salePrice)
                                    .gender(gender)
                                    .soldOut(!soldOut.isEmpty())
                                    .build());
                        } catch (Exception e) {
                            System.out.println("아디다스 상품 파싱 실패: " + e.getMessage());
                        }
                    }

                    List<WebElement> nextButtons =
                            driver.findElements(By.cssSelector("a[data-testid='pagination-next-button']"));
                    if (nextButtons.isEmpty() || !nextButtons.get(0).isDisplayed()) break;

                    try {
                        String currentUrl = driver.getCurrentUrl();
                        nextButtons.get(0).click();
                        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(currentUrl)));
                        closeAdidasPopups(wait);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        } finally {
            driver.quit();
        }
        return products;
    }

    private void closeAdidasPopups(WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[id='glass-gdpr-default-consent-accept-button']"))).click();
        } catch (TimeoutException | NoSuchElementException ignored) {
        }

        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[id='gl-modal__close-mf-account-portal']"))).click();
        } catch (TimeoutException | NoSuchElementException ignored) {
        }
    }

    private String extractHref(WebElement card) {
        try {
            return card.findElement(By.cssSelector("a[href]")).getAttribute("href");
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String extractExternalProductId(String productUrl) {
        if (productUrl == null || productUrl.isBlank()) return null;
        String[] segments = productUrl.split("\\?")[0].split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (!segments[i].isBlank()) return segments[i];
        }
        return null;
    }
}
