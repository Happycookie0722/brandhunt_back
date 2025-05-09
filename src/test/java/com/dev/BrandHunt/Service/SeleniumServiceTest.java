package com.dev.BrandHunt.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;


public class SeleniumServiceTest {
    private WebDriver driver;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless=new"); // Headless 모드
        options.addArguments("--no-sandbox"); // 리눅스 환경에서 충돌 방지
        options.addArguments("--disable-dev-shm-usage"); // 공유 메모리 이슈 방지


        driver = new ChromeDriver(options);
    }

    @Test
    void testNikePageTitle() throws InterruptedException {
        // 크롤링할 URL 리스트
        String[] urls = {
                "https://www.nike.com/kr/w/men-shoes-nik1zy7ok",
                "https://www.nike.com/kr/w/men-apparel-6ymx6znik1",
                "https://www.nike.com/kr/w/men-accessories-equipment-awwpwznik1"
//                "https://www.nike.com/kr/w/women-shoes-5e1x6zy7ok",
//                "https://www.nike.com/kr/w/unisex-shoes-3rauvzy7ok"
        };

        for (String url : urls) {
            System.out.println("크롤링 시작: " + url);
            driver.get(url);

            // 페이지 끝까지 스크롤
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000); // 스크롤 후 로딩 대기

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) break;
                lastHeight = newHeight;
            }

            // 상품 카드 추출
            List<WebElement> productCards = driver.findElements(By.cssSelector(".product-card"));
            System.out.println("총 상품 수: " + productCards.size());

            for (WebElement card : productCards) {
                try {
                    String name = card.findElement(By.cssSelector(".product-card__title")).getText();
                    String image = card.findElement(By.tagName("img")).getAttribute("src");

                    List<WebElement> prices = card.findElements(By.cssSelector(".product-price"));
                    String salePrice = !prices.isEmpty() ? prices.get(0).getText() : "없음";
                    String originalPrice = prices.size() == 2 ? prices.get(1).getText() : "-";

                    System.out.println("상품명: " + name);
                    System.out.println("이미지: " + image);
                    System.out.println("할인가: " + salePrice);
                    System.out.println("정가: " + originalPrice);
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
