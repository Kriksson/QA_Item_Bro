package UI;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class TestBaseUI {

    @BeforeEach
    void setUp() {
        if (Configuration.remote == null) {
            WebDriverManager.chromedriver().setup();
        }

        Configuration.browser = "chrome";
        Configuration.browserSize = "1024x768";
        Configuration.headless = false;
        SelenideLogger.addListener("allure",  new AllureSelenide()
                .includeSelenideSteps(true)
                .savePageSource(true)
                .screenshots(true));
    }

    @AfterEach()
    void tearDown() {
        Selenide.closeWebDriver();
    }
}
