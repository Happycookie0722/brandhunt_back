package com.dev.BrandHunt.Config;

import com.dev.BrandHunt.Constant.SiteType;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

@Component
public class SeleniumConfig {
    public WebDriver createWebDriver(SiteType siteType) {
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

        if (siteType == SiteType.ADIDAS) {
            options.addArguments("--blink-settings=imagesEnabled=false");
        }

        return new ChromeDriver(options);
    }

    // 서버 종료시점에 일괄 종료 시킬 수 있음
    @PreDestroy
    public void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }
}
