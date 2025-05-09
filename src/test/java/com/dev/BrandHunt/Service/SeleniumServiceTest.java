package com.dev.BrandHunt.Service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.test.context.SpringBootTest;

public class SeleniumServiceTest {
    private WebDriver driver;

    @BeforeEach
    void setUp() {
        // ChromeDriver 경로 설정
        System.setProperty("webdriver.chrome.driver", "C:/Users/user/Downloads/chromedriver-win64/chromedriver-win64/chromedriver.exe");
        driver = new ChromeDriver();
    }

    @Test
    void testNikePageTitle() {
        driver.get("https://www.nike.com/kr/w/men-shoes-nik1zy7ok");
        String title = driver.getTitle();
        System.out.println("페이지 타이틀: " + title);
        assert title != null && !title.isEmpty();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
