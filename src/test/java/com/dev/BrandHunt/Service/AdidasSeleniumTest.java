package com.dev.BrandHunt.Service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;


public class AdidasSeleniumTest {
    private WebDriver driver;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080"); // 화면 크기 명시
        options.addArguments("--headless=new"); // Headless 모드
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.3072.124 Safari/537.36");

        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");               // 리눅스 환경에서 충돌 방지
        options.addArguments("--disable-dev-shm-usage");    // 공유 메모리 크기 제한하지 않음
        options.addArguments("--disable-popup-blocking");   //팝업안띄움
        options.addArguments("--disable-gpu");			    //gpu 비활성화
        options.addArguments("--blink-settings=imagesEnabled=false"); //이미지 다운 안받음

        driver = new ChromeDriver(options);
    }

    @Test
    void testAdidasPageScrollAndParse() throws InterruptedException {
        String[] urls = {
            "https://www.adidas.co.kr/men-shoes",
            "https://www.adidas.co.kr/men-clothing",
            "https://www.adidas.co.kr/men-bags-accessories",
            "https://www.adidas.co.kr/men-headwear",
            "https://www.adidas.co.kr/men-socks",
            "https://www.adidas.co.kr/women-shoes",
            "https://www.adidas.co.kr/women-clothing",
            "https://www.adidas.co.kr/women-bags-accessories",
            "https://www.adidas.co.kr/women-headwear",
            "https://www.adidas.co.kr/women-socks",
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
                        System.out.println("----------");
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
    }
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
