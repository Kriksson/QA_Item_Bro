package UI;

import config.TestConfig;
import API.ApiUtility;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class TestBaseUI {

    @BeforeEach
    void setUp() throws SQLException {
        Configuration.remote = TestConfig.REMOTE.isBlank() ? null : TestConfig.REMOTE;
        Configuration.browserVersion = TestConfig.BROWSER_VERSION;
        if (Configuration.remote == null) {
            WebDriverManager.chromedriver().setup();
        }

        Configuration.browser = "chrome";
        Configuration.browserSize = "1024x768";
        Configuration.headless = TestConfig.HEADLESS;
        SelenideLogger.addListener("allure",  new AllureSelenide()
                .includeSelenideSteps(true)
                .savePageSource(true)
                .screenshots(true));
        prepareDb();
    }

    void prepareDb() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                TestConfig.DB_URL,
                TestConfig.DB_USER,
                TestConfig.DB_PASSWORD
        );
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE order_messages, orders, advices, games, users RESTART IDENTITY");
            ApiUtility.registerAndReturnData(
                    TestConfig.API_URL, "ADMIN", "Admin123!"
            );

            ApiUtility.registerAndReturnData(
                    TestConfig.API_URL, "MAJOR", "Major123!"
            );
            statement.execute("UPDATE users SET role = 'ADMIN' WHERE username = 'ADMIN'");
            statement.execute("UPDATE users SET balance = '100000' WHERE username = 'MAJOR'");
        }
    }

    @AfterEach()
    void tearDown() {
        Selenide.closeWebDriver();
    }
}
