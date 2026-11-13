package UI;

import API.ApiUtility;
import API.Records.*;
import UI.Pages.*;
import com.codeborne.selenide.Condition;
import config.TestConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class FeedbackTest extends TestBaseUI {

    public static final String URL = TestConfig.UI_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель оставляет отзыв, продавец отвечает")
    public void successfulAddFeedbackAndSellerResponse() {
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
        String feedback = "Отличная работа!";
        String response = "Спасибо за отзыв!";
        orderPage.leaveFeedback("4", feedback);

        Allure.step("Проверяем текст и оценку отзыва в заказе", () -> {
            orderPage.feedbackText.shouldHave(Condition.exactText(feedback));
            orderPage.ratingStars.shouldHave(Condition.attribute("aria-label", "Оценка 4 из 5"));
        });
        mainPage.logOut();
        mainPage.authUser(sellerName, sellerPassword);
        Assert.assertEquals(sellerName, mainPage.getUsername());
        OrdersPage sellerOrdersPage = mainPage.openOrdersPage();
        OrderPage sellerOrderPage = sellerOrdersPage.openOrder(adviceName);
        sellerOrderPage.leaveSellerResponse(response);

        Allure.step("Проверяем что у продавца видны отзыв, оценка и ответ", () -> {
            sellerOrderPage.feedbackText.shouldHave(Condition.exactText(feedback));
            sellerOrderPage.ratingStars.shouldHave(Condition.attribute("aria-label", "Оценка 4 из 5"));
            sellerOrderPage.sellerResponseText.shouldHave(Condition.exactText(response));
        });
        mainPage.logOut();
        mainPage.authUser("MAJOR", "Major123!");
        Assert.assertEquals("MAJOR", mainPage.getUsername());
        OrdersPage buyerOrdersPage = mainPage.openOrdersPage();
        OrderPage buyerOrderPage = buyerOrdersPage.openOrder(adviceName);

        Allure.step("Проверяем что покупатель видит отзыв и ответ продавца", () -> {
            buyerOrderPage.feedbackText.shouldHave(Condition.exactText(feedback));
            buyerOrderPage.ratingStars.shouldHave(Condition.attribute("aria-label", "Оценка 4 из 5"));
            buyerOrderPage.sellerResponseText.shouldHave(Condition.exactText(response));
        });
    }

}
