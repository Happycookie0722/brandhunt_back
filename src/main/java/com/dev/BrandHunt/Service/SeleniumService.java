package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Config.SeleniumConfig;
import com.dev.BrandHunt.Constant.SiteType;
import com.dev.BrandHunt.Entity.Product;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeleniumService {
    private SeleniumConfig seleniumConfig;

    public void getNikeProduct() throws InterruptedException {
        WebDriver driver = seleniumConfig.createWebDriver(SiteType.NIKE);

        try {
            String[] urls = {
                    "https://www.nike.com/kr/w/men-shoes-nik1zy7ok", // 신발
                    "https://www.nike.com/kr/w/men-apparel-6ymx6znik1", // 의류
                    "https://www.nike.com/kr/w/men-bags-backpacks-9xy71znik1", // 가방
                    "https://www.nike.com/kr/w/men-hats-visors-headbands-52r49znik1", // 모자 & 헤드밴드
                    "https://www.nike.com/kr/w/women-shoes-5e1x6zy7ok", // 신발
                    "https://www.nike.com/kr/w/women-apparel-6ymx6znik1", // 의류
                    "https://www.nike.com/kr/w/women-bags-backpacks-9xy71znik1", // 가방
                    "https://www.nike.com/kr/w/women-hats-visors-headbands-52r49znik1", // 모자 & 헤드밴드
            };

            for (String url : urls) {
                System.out.println("크롤링 시작: " + url);
                driver.get(url);

                JavascriptExecutor js = (JavascriptExecutor) driver;
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

                int prevCount = 0, sameCount = 0;

                while (true) {
                    js.executeScript("window.scrollBy(0, 2500);");
                    Thread.sleep(4000);

                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

                    int count = driver.findElements(By.cssSelector(".product-card")).size();
                    System.out.println("현재 상품 수: " + count);

                    if (count == prevCount) {
                        sameCount++;
                        if (sameCount >= 10) {
                            System.out.println("상품 수 변화 없음, 종료.");
                            break;
                        }
                    } else {
                        sameCount = 0;
                        prevCount = count;
                    }
                }


                List<WebElement> productCards = driver.findElements(By.cssSelector(".product-card"));
                System.out.println("총 상품 수: " + productCards.size());

                for (WebElement card : productCards) {

                    try {
                        String category = card.findElement(By.className("product-card__subtitle")).getText();
                        String name = card.findElement(By.cssSelector(".product-card__title")).getText();
                        String image = card.findElement(By.tagName("img")).getAttribute("src");

                        List<WebElement> prices = card.findElements(By.cssSelector(".product-price"));
                        String price = !prices.isEmpty() ? prices.get(0).getText() : "없음";
                        String salePrice = prices.size() == 2 ? prices.get(1).getText() : "-";

                        System.out.println("카테고리: " + category);
                        System.out.println("상품명: " + name);
                        System.out.println("이미지: " + image);
                        System.out.println("정가: " + price);
                        System.out.println("할인가: " + salePrice);
                        System.out.println("----------");
                    } catch (Exception e) {
                        System.out.println("상품 파싱 실패: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            driver.quit();
        }
    }

    public void getAdidasProduct() {
        WebDriver driver = seleniumConfig.createWebDriver(SiteType.NIKE);

        try {
            String[] urls = {
                    "https://www.adidas.co.kr/men-shoes",
                    "https://www.adidas.co.kr/men-clothing",
                    "https://www.adidas.co.kr/men-bags-accessories",
                    "https://www.adidas.co.kr/men-headwear",
                    "https://www.adidas.co.kr/women-shoes",
                    "https://www.adidas.co.kr/women-clothing",
                    "https://www.adidas.co.kr/women-bags-accessories",
                    "https://www.adidas.co.kr/women-headwear",
            };

            for (String url : urls) {
                System.out.println("크롤링 시작: " + url);
                driver.get(url);

                WebDriverWait defaultWait = new WebDriverWait(driver, Duration.ofSeconds(3));

                while (true) {
                    try {
                        WebElement cookieBtn = defaultWait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("button[id='glass-gdpr-default-consent-accept-button']")));
                        cookieBtn.click();  // 쿠키 동의 버튼 클릭
                        System.out.println("쿠키 동의 버튼 클릭 완료");

                        // 모달창이 닫히는 것을 기다림
                        defaultWait.until(ExpectedConditions.invisibilityOfElementLocated(
                                By.cssSelector("button[id='glass-gdpr-default-consent-accept-button']")));
                        System.out.println("쿠키 모달 닫힘");
                    } catch (NoSuchElementException e) {
                        System.out.println("쿠키 동의 버튼 없음");
                    } catch (TimeoutException e) {
                        System.out.println("쿠키 모달 시간 초과");
                    }

                    try {
                        WebElement loginBtn = defaultWait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("button[id='gl-modal__close-mf-account-portal']")));
                        loginBtn.click();
                        System.out.println("로그인 닫기 버튼 클릭 완료");

                        defaultWait.until(ExpectedConditions.invisibilityOfElementLocated(
                                By.cssSelector("button[id='gl-modal__close-mf-account-portal']")));
                        System.out.println("로그인 모달 닫힘");
                    } catch (NoSuchElementException e) {
                        System.out.println("로그인 모달 없음");
                    } catch (TimeoutException e) {
                        System.out.println("로그인 모달 시간 초과");
                    }

                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("window.scrollBy(0, 2500);");

                    List<WebElement> productCards = driver.findElements(By.cssSelector("article[data-testid='plp-product-card']"));

                    if (!productCards.isEmpty()) {
                        System.out.println("현재 페이지 상품수: " + productCards.size());
                    } else {
                        System.out.println("현재 페이지에 상품이 없습니다.");
                    }

                    for (WebElement card : productCards) {
                        try {
                            String name = card.findElement(By.cssSelector("p[data-testid='product-card-title']")).getText();
                            String category = card.findElement(By.cssSelector("p[data-testid='product-card-subtitle']")).getText();
                            String image = card.findElement(By.cssSelector("img[data-testid='product-card-primary-image']")).getAttribute("src");
                            String price, salePrice;
                            List<WebElement> originalPriceComponent = card.findElements(By.cssSelector("div[data-testid='main-price']"));
                            List<WebElement> salePriceComponent = card.findElements(By.cssSelector("div[data-testid='original-price']"));
                            List<WebElement> soldOutComponent = card.findElements(By.cssSelector("a[data-testid='product-card-description-link'] div[data-testid='sold-out']"));

                            if (!soldOutComponent.isEmpty()) {
                                // 품절 상태
                                price = "품절";
                                salePrice = null;
                                System.out.println("품절 상품입니다.");
                            } else if (!salePriceComponent.isEmpty() && !originalPriceComponent.isEmpty()) {
                                // 세일 품목
                                List<WebElement> ogSpans = originalPriceComponent.get(0).findElements(By.tagName("span"));
                                List<WebElement> saleSpans = salePriceComponent.get(0).findElements(By.tagName("span"));
                                price = saleSpans.get(0).getText();
                                salePrice = ogSpans.get(1).getText();
                            } else if (!originalPriceComponent.isEmpty()) {
                                // 세일 아닌 정가 판매
                                List<WebElement> spans = originalPriceComponent.get(0).findElements(By.tagName("span"));
                                price = spans.get(1).getText();
                                salePrice = null;
                            } else {
                                // 가격 정보 없음
                                price = "가격 정보 없음";
                                salePrice = null;
                            }

                            System.out.println("카테고리: " + category);
                            System.out.println("상품명: " + name);
                            System.out.println("이미지: " + image);
                            System.out.println("가격: " + price);
                            System.out.println("세일 가격: " + salePrice);
                        } catch (Exception e) {
                            System.out.println("상품 파싱 실패: " + e.getMessage());
                        }
                    }

                    // 다음 페이지 버튼이 있는 경우 클릭
                    List<WebElement> nextButtons = driver.findElements(By.cssSelector("a[data-testid='pagination-next-button']"));
                    if (nextButtons.isEmpty() || !nextButtons.get(0).isDisplayed()) break;

                    nextButtons.get(0).click();
                }
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            driver.quit();
        }
    }
}
