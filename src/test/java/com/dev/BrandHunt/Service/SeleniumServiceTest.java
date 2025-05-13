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


public class SeleniumServiceTest {
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
//        options.addArguments("--blink-settings=imagesEnabled=false"); //이미지 다운 안받음

        driver = new ChromeDriver(options);
    }

    @Test
    void testNikePageScrollAndParse() throws InterruptedException {
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

            while(true) {
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
                    String originalPrice = !prices.isEmpty() ? prices.get(0).getText() : "없음";
                    String salePrice = prices.size() == 2 ? prices.get(1).getText() : "-";

                    System.out.println("카테고리: " + category);
                    System.out.println("상품명: " + name);
                    System.out.println("이미지: " + image);
                    System.out.println("정가: " + originalPrice);
                    System.out.println("할인가: " + salePrice);
                    System.out.println("----------");
                } catch (Exception e) {
                    System.out.println("상품 파싱 실패: " + e.getMessage());
                }
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
