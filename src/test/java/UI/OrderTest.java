package UI;

import API.ApiUtility;
import API.Records.*;
import UI.Pages.*;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
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

public class OrderTest extends TestBaseUI {

    public static final String URL = TestConfig.UI_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Гостю предлагается войти, своё объявление купить нельзя")
    public void checkBuyAdviceAccessByRole() {
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

        MainPage mainPage = new MainPage(URL);
        AdvicesPage advicesPage = mainPage.openCategory(category.name());
        advicesPage.openAdvice(adviceName);

        Allure.step("Гостю предлагается войти для покупки", () -> {
            advicesPage.loginForBuyButton.shouldBe(Condition.visible).scrollIntoCenter().click();
            $x("//dialog//input[@placeholder='Твой пароль']").shouldBe(Condition.visible);
            Assert.assertTrue(WebDriverRunner.url().endsWith("/login"));
        });

        Selenide.open(URL);
        mainPage.authUser(sellerName, sellerPassword);
        Assert.assertEquals(sellerName, mainPage.getUsername());
        mainPage.openCategory(category.name());
        advicesPage.openAdvice(adviceName);

        Allure.step("На своём объявлении покупка недоступна", () -> {
            advicesPage.ownAdviceButton.shouldBe(Condition.visible).shouldBe(Condition.disabled);
            advicesPage.createOrderButton.shouldNot(Condition.exist);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель покупает чужое объявление")
    public void successfulBuyAdvice() {
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

        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("MAJOR", "Major123!");
        Assert.assertEquals("MAJOR", mainPage.getUsername());
        BigDecimal oldBalance = mainPage.getBalance();
        AdvicesPage advicesPage = mainPage.openCategory(category.name());
        advicesPage.openAdvice(adviceName);
        OrderPage orderPage = advicesPage.buyAdvice();
        String orderURL = WebDriverRunner.url();

        Allure.step("Проверяем данные открывшегося заказа", () -> {
            Assert.assertTrue(orderURL.matches(".*/orders/[0-9]+"));
            Assert.assertEquals(adviceName, orderPage.getAdviceName());
            Assert.assertEquals(0, createdAdvice.price().compareTo(orderPage.getPrice()));
            Assert.assertEquals("MAJOR", orderPage.getBuyerName());
            Assert.assertEquals(sellerName, orderPage.getSellerName());
            orderPage.statusText.shouldHave(Condition.exactText("Открыт"));
        });

        Allure.step("Проверяем что баланс покупателя уменьшился на стоимость объявления", () -> {
            mainPage.balanceText.shouldHave(Condition.text("99 801"));
            Assert.assertEquals(0, oldBalance.subtract(createdAdvice.price()).compareTo(mainPage.getBalance()));
        });

        OrdersPage ordersPage = mainPage.openOrdersPage();
        Allure.step("Проверяем что созданный заказ появился в списке покупателя", () -> {
            Assert.assertTrue(ordersPage.orderContain(adviceName));
            ordersPage.openOrder(adviceName);
            Assert.assertEquals(orderURL, WebDriverRunner.url());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("При недостаточном балансе заказ не создаётся")
    public void unsuccessfulBuyAdviceWithoutMoney() {
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

        String buyerName = ApiUtility.genNick();
        String buyerPassword = ApiUtility.genPass();
        ApiUtility.registerAndReturnData(TestConfig.API_URL, buyerName, buyerPassword);

        MainPage mainPage = new MainPage(URL);
        mainPage.authUser(buyerName, buyerPassword);
        Assert.assertEquals(buyerName, mainPage.getUsername());
        AdvicesPage advicesPage = mainPage.openCategory(category.name());
        advicesPage.openAdvice(adviceName);

        Allure.step("Покупатель без денег пытается купить объявление", () -> {
            Assert.assertEquals(0, mainPage.getBalance().compareTo(BigDecimal.ZERO));
            advicesPage.createOrderButton.shouldBe(Condition.visible).scrollIntoCenter().click();
            Assert.assertEquals("Недостаточно средств", mainPage.getFormError());
        });

        Allure.step("Проверяем что заказ не создался", () -> {
            $x("//dialog//button[@aria-label='Закрыть окно']").click();
            OrdersPage ordersPage = mainPage.openOrdersPage();
            $x("//div[@class='empty-state']/h3").shouldHave(Condition.exactText("Заказов нет"));
            Assert.assertFalse(ordersPage.orderContain(adviceName));
            Assert.assertEquals(0, mainPage.getBalance().compareTo(BigDecimal.ZERO));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель подтверждает выполнение заказа")
    public void successfulConfirmOrderByBuyer() {
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
        orderPage.confirmOrder();

        Allure.step("Проверяем что заказ завершён и подтвердить его ещё раз нельзя", () -> {
            orderPage.statusText.shouldHave(Condition.exactText("Закрыт"));
            orderPage.confirmOrderButton.shouldNot(Condition.exist);
            $x("//p[@class='order-terminal-note']").shouldHave(Condition.text("Сделка завершена"));
        });

        Allure.step("Перезагружаем страницу и проверяем что заказ остался завершённым", () -> {
            Selenide.refresh();
            orderPage.statusText.shouldHave(Condition.exactText("Закрыт"));
            orderPage.confirmOrderButton.shouldNot(Condition.exist);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Продавец отменяет заказ, деньги возвращаются покупателю")
    public void successfulCancelOrderBySeller() {
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
        Allure.step("Проверяем что стоимость заказа списалась у покупателя", () ->
                Assert.assertEquals(0, mainPage.getBalance().compareTo(BigDecimal.valueOf(99801))));
        mainPage.logOut();

        mainPage.authUser(sellerName, sellerPassword);
        Assert.assertEquals(sellerName, mainPage.getUsername());
        OrdersPage ordersPage = mainPage.openOrdersPage();
        OrderPage orderPage = ordersPage.openOrder(adviceName);
        orderPage.cancelOrder();

        Allure.step("Проверяем что заказ отменён", () -> {
            orderPage.statusText.shouldHave(Condition.exactText("Отменён"));
            orderPage.cancelOrderButton.shouldNot(Condition.exist);
        });
        mainPage.logOut();
        mainPage.authUser("MAJOR", "Major123!");
        Assert.assertEquals("MAJOR", mainPage.getUsername());

        Allure.step("Проверяем что деньги вернулись покупателю", () ->
                Assert.assertEquals(0, mainPage.getBalance().compareTo(BigDecimal.valueOf(100000))));
        OrdersPage buyerOrdersPage = mainPage.openOrdersPage();
        OrderPage buyerOrderPage = buyerOrdersPage.openOrder(adviceName);
        buyerOrderPage.statusText.shouldHave(Condition.exactText("Отменён"));
    }

}
