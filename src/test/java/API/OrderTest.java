package API;

import config.TestConfig;
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
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class OrderTest extends TestBaseAPI {

    public static final String URL = TestConfig.API_URL;

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

        Allure.step("Посторонний пользователь запрашивает информацию о заказе и получает 403 с ошибкой \"У вас нет прав\"", () -> {
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

    @Test
    @Owner("kriksson")
    @DisplayName("GET /api/orders возвращает заказы пользователя")
    public void successfulGetOrders() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil = ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO firstCreatedAdvice =  Allure.step("Продавец создаёт объявление №1", () -> {
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

        AdviceDTO secondCreatedAdvice =  Allure.step("Продавец создаёт объявление №2", () -> {
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

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);
        SessionFilter majorFilter = majorSes.sessionFilter();
        CsrfResponse majorCsrf = majorSes.csrf();

        OrderDTO firstOrder = Allure.step("Покупатель покупает первый товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", firstCreatedAdvice.id());

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
        OrderDTO secondOrder = Allure.step("Покупатель покупает второй товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", secondCreatedAdvice.id());

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

        Allure.step("Проверяем что в списках заказов у покупателя появились созданные заказы", () -> {
            List<OrderDTO> orders = given()
                    .filter(majorFilter)
                    .when()
                    .get(URL+"/api/orders")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList(".",  OrderDTO.class);

            Assert.assertTrue(orders.stream().anyMatch(x -> x.adviceDTO().id().equals(firstOrder.adviceDTO().id())));
            Assert.assertTrue(orders.stream().anyMatch(x -> x.adviceDTO().id().equals(secondOrder.adviceDTO().id())));
        });

        Allure.step("Проверяем что в списках заказов у продавца появились созданные заказы", () -> {
            List<OrderDTO> orders = given()
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .when()
                    .get(URL+"/api/orders")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList(".",  OrderDTO.class);

            Assert.assertTrue(orders.stream().anyMatch(x -> x.id().equals(firstOrder.id())));
            Assert.assertTrue(orders.stream().anyMatch(x -> x.id().equals(secondOrder.id())));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель подтверждает открытый заказ")
    public void successfulAcceptOrderByBuyer() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

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

        RegisterAccountResponse sellerAccountInfo = Allure.step("Запрашиваем статистику продавца", () -> given()
                .filter(sellerUtil.sessionFilter())
                .auth().preemptive().basic(sellerName, sellerPassword)
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Покупатель подтверждает покупку", () -> {
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/confirm")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что заказ закрыт", () -> {
            OrderDTO order = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("CLOSED", order.orderStatus());
        });

        RegisterAccountResponse newSellerAccountInfo = Allure.step("Запрашиваем статистику продавца повторно", () -> given()
                .filter(sellerUtil.sessionFilter())
                .auth().preemptive().basic(sellerName, sellerPassword)
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        Allure.step("Проверяем что баланс пополнился на сумму заказа", () -> Assert.assertEquals(0, newSellerAccountInfo.balance().subtract(sellerAccountInfo.balance()).compareTo(BigDecimal.valueOf(199))));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Продавец отменяет открытый заказ")
    public void successfulCancelOrderBySeller() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

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
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Проверяем что заказ открыт", () -> Assert.assertEquals("OPENED", createdOrder.orderStatus()));

        RegisterAccountResponse buyerAccountInfo = Allure.step("Запрашиваем баланс покупателя перед отменой", () -> given()
                .filter(majorSes.sessionFilter())
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        CsrfResponse sellerCsrf = Allure.step("Получаем CSRF для сессии продавца", () -> given()
                .filter(sellerUtil.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        Allure.step("Продавец отменяет заказ", () -> {
            given()
                    .filter(sellerUtil.sessionFilter())
                    .header(sellerCsrf.headerName(), sellerCsrf.token())
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/cancel")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что заказ отменён", () -> {
            OrderDTO order = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("REFUND", order.orderStatus());
        });

        RegisterAccountResponse newBuyerAccountInfo = Allure.step("Запрашиваем баланс покупателя повторно", () -> given()
                .filter(majorSes.sessionFilter())
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        Allure.step("Проверяем что покупателю вернулась стоимость заказа", () -> Assert.assertEquals(0, newBuyerAccountInfo.balance().subtract(buyerAccountInfo.balance()).compareTo(createdOrder.price())));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель не может отменить заказ. Продавец не может подтвердить заказ")
    public void unsuccessfulChangeOrderStatusByWrongRole() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

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
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Покупатель не может отменить заказ", () -> {
            given()
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .filter(majorSes.sessionFilter())
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/cancel")
                    .then()
                    .log().all()
                    .statusCode(403);
        });

        Allure.step("Продавец не может подтвердить заказ", () -> {
            given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/confirm")
                    .then()
                    .log().all()
                    .statusCode(403);
        });
    }
}
