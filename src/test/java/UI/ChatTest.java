package UI;

import API.ApiUtility;
import API.Records.*;
import UI.Pages.*;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import config.TestConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static com.codeborne.selenide.Selenide.$x;
import static io.restassured.RestAssured.given;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;

public class ChatTest extends TestBaseUI {

    public static final String URL = TestConfig.UI_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель и продавец переписываются без обновления страницы, F5 сохраняет историю")
    public void successfulExchangeOrderMessages() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil = ApiUtility.registerAndReturnData(TestConfig.API_URL, sellerName, sellerPassword);
        CategoryDTO category = ApiUtility.createTestCategory(TestConfig.API_URL, ApiUtility.genNick());
        String adviceName = ApiUtility.genNick();

        AdviceDTO createdAdvice = Allure.step("Продавец создаёт тестовое объявление", () -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", adviceName);
            body.put("game_id", category.id());
            body.put("price", BigDecimal.valueOf(199));
            return given()
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(TestConfig.API_URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        AuthorizedSession majorSes = ApiUtility.authMajor(TestConfig.API_URL);
        OrderDTO createdOrder = Allure.step("Покупатель создаёт тестовый заказ", () -> {
            HashMap<String, Long> body = new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(TestConfig.API_URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("MAJOR", "Major123!");
        Assert.assertEquals("MAJOR", mainPage.getUsername());
        OrdersPage ordersPage = mainPage.openOrdersPage();
        OrderPage orderPage = ordersPage.openOrder(adviceName);
        String buyerMessage = "Привет! Когда будет готово?";
        String sellerMessage = "Привет! Уже всё готово";

        SelenideDriver sellerBrowser = new SelenideDriver(new SelenideConfig()
                .browser("chrome")
                .browserSize("1024x768")
                .headless(TestConfig.HEADLESS)
                .browserCapabilities(Configuration.browserCapabilities)
                .remote(TestConfig.REMOTE.isBlank() ? null : TestConfig.REMOTE)
                .browserVersion(TestConfig.BROWSER_VERSION));
        try {
            Allure.step("Продавец открывает заказ в отдельном браузере", () -> {
                sellerBrowser.open(URL);
                sellerBrowser.$x("//button[text()='Войти']").shouldBe(Condition.visible).click();
                sellerBrowser.$x("//input[@placeholder='Твоё имя']").shouldBe(Condition.visible).setValue(sellerName);
                sellerBrowser.$x("//input[@placeholder='Твой пароль']").setValue(sellerPassword);
                sellerBrowser.$x("//button[text()='Войти'][@class='button button-full']").click();
                sellerBrowser.$x("//div[@class='account-menu']").shouldHave(Condition.text(sellerName));
                sellerBrowser.open(URL+"/orders/"+createdOrder.id());
                sellerBrowser.$x("//textarea[@aria-label='Сообщение']").shouldBe(Condition.enabled);
                orderPage.messageInput.shouldBe(Condition.enabled);
            });

            orderPage.sendMessage(buyerMessage);
            Allure.step("Проверяем что продавец получил сообщение без обновления страницы", () -> {
                sellerBrowser.$x("//article[contains(@class, 'chat-message')][p='"+buyerMessage+"']")
                        .shouldBe(Condition.visible).shouldHave(Condition.text("MAJOR"));
            });

            Allure.step("Продавец отвечает покупателю", () -> {
                sellerBrowser.$x("//textarea[@aria-label='Сообщение']")
                        .scrollIntoCenter().setValue(sellerMessage);
                sellerBrowser.$x("//button[@aria-label='Отправить сообщение']")
                        .shouldBe(Condition.enabled).scrollIntoCenter().click();
                sellerBrowser.$x("//textarea[@aria-label='Сообщение']").shouldHave(Condition.exactValue(""));
            });

            Allure.step("Проверяем что покупатель получил ответ без обновления страницы", () -> {
                $x("//article[contains(@class, 'chat-message')][p='"+sellerMessage+"']")
                        .shouldBe(Condition.visible).shouldHave(Condition.text(sellerName));
            });

            Allure.step("Перезагружаем страницу покупателя и проверяем историю чата", () -> {
                Selenide.refresh();
                $x("//article[contains(@class, 'chat-message')][p='"+buyerMessage+"']")
                        .shouldBe(Condition.visible).shouldHave(Condition.text("MAJOR"));
                $x("//article[contains(@class, 'chat-message')][p='"+sellerMessage+"']")
                        .shouldBe(Condition.visible).shouldHave(Condition.text(sellerName));
            });
        } finally {
            sellerBrowser.close();
        }
    }

}
