package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class OrderTest extends TestBaseAPI {

    public static final String URL = "http://localhost:8085";

    @Test
    @Owner("kriksson")
    @DisplayName("Покупка активного объявления")
    public void successfulBuyActiveAdvice() {
        String name = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, name, password);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", ApiUtility.genNick());
        body.put("game_id", createdCategory.id());
        body.put("price", BigDecimal.valueOf(199));
        AdviceDTO createdAdvice = Allure.step("Создание тестового объявления", () -> given()
                .header(util.csrf().headerName(), util.csrf().token())
                .filter(util.sessionFilter())
                .contentType(ContentType.JSON)
                .auth().preemptive().basic(name, password)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(AdviceDTO.class));

        AuthorizedSession major = ApiUtility.authMajor(URL);
        CsrfResponse csrfMajor = major.csrf();
        SessionFilter sessionMajor = major.sessionFilter();

        RegisterAccountResponse majorAccount = Allure.step("Запрашиваем статистику аккаунта с балансом", () -> given()
                .filter(sessionMajor)
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        Map<String, Long> createOrderBody = new HashMap<>();
        createOrderBody.put("id", createdAdvice.id());

        OrderDTO createdOrder = Allure.step("Создаём заказ по объявлению", () -> given()
                .filter(sessionMajor)
                .header(csrfMajor.headerName(), csrfMajor.token())
                .contentType(ContentType.JSON)
                .body(createOrderBody)
                .when()
                .post(URL+"/api/orders/")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(OrderDTO.class));

        Allure.step("Проверяем данные созданного заказа", () -> {
            Assert.assertEquals("OPENED", createdOrder.orderStatus());
            Assert.assertTrue(EqualsBuilder.reflectionEquals(createdOrder.adviceDTO(), createdAdvice, "price"));
            Assert.assertEquals(0, createdOrder.adviceDTO().price().compareTo(createdAdvice.price()));
            Assert.assertEquals(0, createdOrder.price().compareTo(BigDecimal.valueOf(199)));
        });

        Allure.step("Проверяем баланс покупателя, должно списаться 199", () -> {
            RegisterAccountResponse response = given()
                    .filter(sessionMajor)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(RegisterAccountResponse.class);

            BigDecimal expectedBalance = majorAccount.balance().subtract(createdAdvice.price());

            Assert.assertEquals(0, expectedBalance.compareTo(response.balance()));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("При недостаточном балансе заказ не создаётся")
    public void unsuccessfulBuyActiveAdvice() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil = ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);
        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", ApiUtility.genNick());
        body.put("game_id", createdCategory.id());
        body.put("price", BigDecimal.valueOf(199));
        AdviceDTO createdAdvice = Allure.step("Создание тестового объявления", () -> given()
                .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                .filter(sellerUtil.sessionFilter())
                .contentType(ContentType.JSON)
                .auth().preemptive().basic(sellerName, sellerPassword)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(AdviceDTO.class));

        String buyerName = ApiUtility.genNick();
        String buyerPassword = ApiUtility.genPass();
        UtilityRegister buyerUtil = ApiUtility.registerAndReturnData(URL, buyerName, buyerPassword);
        Map<String, Long> createOrderBody = new HashMap<>();
        createOrderBody.put("id", createdAdvice.id());

        Allure.step("Пытаемся купить товар без средств и ожидаем 409 с ошибкой \"Недостаточно средств\"", () -> {
            given()
                    .filter(buyerUtil.sessionFilter())
                    .header(buyerUtil.csrf().headerName(), buyerUtil.csrf().token())
                    .auth().preemptive().basic(buyerName, buyerPassword)
                    .contentType(ContentType.JSON)
                    .body(createOrderBody)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(409)
                    .body("message", equalTo("Недостаточно средств"));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Покупка несуществующего объявления отклоняется")
    public void unsuccessfulBuyNonExistAdvice() {
        String name = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util =  ApiUtility.registerAndReturnData(URL, name, password);

        Map<String, Long> createOrderBody = new HashMap<>();
        createOrderBody.put("id", 999L);

        Allure.step("Пытаемся купить несуществующий товар", () -> {
            given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(), util.csrf().token())
                    .auth().preemptive().basic(name, password)
                    .contentType(ContentType.JSON)
                    .body(createOrderBody)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(404)
                    .body("message", equalTo("Объявление не найдено"));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Гость не может создать заказ")
    public void unsuccessfulBuyNonExistCategory() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil = ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);
        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", ApiUtility.genNick());
        body.put("game_id", createdCategory.id());
        body.put("price", BigDecimal.valueOf(199));
        AdviceDTO createdAdvice = Allure.step("Создание тестового объявления", () -> given()
                .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                .filter(sellerUtil.sessionFilter())
                .contentType(ContentType.JSON)
                .auth().preemptive().basic(sellerName, sellerPassword)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(AdviceDTO.class));

        Map<String, Long> createOrderBody = new HashMap<>();
        createOrderBody.put("id", createdAdvice.id());

        CsrfResponse csrf = Allure.step("Получаем CSRF гостя", () -> given()
                .when()
                .get(URL+"/csrf")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        Allure.step("Создание заказа гостем приведёт к 403", () -> {
            given()
                    .header(csrf.headerName(), csrf.token())
                    .body(createOrderBody)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(403);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Заказ могут прочитать только 3 пользователя, покупатель, продавец, администратор")
    public void successfulReadOrderByRoles() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil = ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        String guestName =  ApiUtility.genNick();
        String guestPassword = ApiUtility.genPass();
        UtilityRegister guestUtil = ApiUtility.registerAndReturnData(URL, guestName, guestPassword);

        AuthorizedSession adminSes = ApiUtility.authAdmin(URL);
        SessionFilter adminFilter = adminSes.sessionFilter();

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);
        SessionFilter majorFilter = majorSes.sessionFilter();
        CsrfResponse majorCsrf =  majorSes.csrf();

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO createdAdvice =  Allure.step("Продавец создаёт объявление", () -> {
            HashMap<String, Object> createBody = new HashMap<>();
            createBody.put("name", ApiUtility.genNick());
            createBody.put("game_id", createdCategory.id());
            createBody.put("price", BigDecimal.valueOf(199));
            return given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());

            return given()
                    .filter(majorFilter)
                    .header(majorCsrf.headerName(), majorCsrf.token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Продавец запрашивает информацию о заказе и проверяем что заказ тот же", () -> {
            OrderDTO order = given()
                    .filter(sellerUtil.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals(createdOrder.id(), order.id());
        });

        Allure.step("Покупатель запрашивает информацию о заказе и проверяем что заказ тот же", () -> {
            OrderDTO order = given()
                    .filter(majorFilter)
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals(createdOrder.id(), order.id());
        });

        Allure.step("Администратор запрашивает информацию о заказе и проверяем что заказ тот же", () -> {
            OrderDTO order = given()
                    .filter(adminFilter)
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals(createdOrder.id(), order.id());
        });

        Allure.step("Гость запрашивает информацию о заказе и получает 403 с ошибкой \"У вас нет прав\"", () -> {
            given()
                    .filter(guestUtil.sessionFilter())
                    .auth().preemptive().basic(guestName, guestPassword)
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body("message", equalTo("У вас нет прав"));
        });
    }
}
