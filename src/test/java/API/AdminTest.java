package API;

import config.TestConfig;
import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AdminTest extends TestBaseAPI{

    public static final String URL = TestConfig.API_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Только администратор может получить общий список заказов")
    public void checkAccessToAllOrdersByRole() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);
        AuthorizedSession adminSes = ApiUtility.authAdmin(URL);

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

        Allure.step("Администратор получает список всех заказов", () -> {
            List<OrderDTO> list = given()
                    .filter(adminSes.sessionFilter())
                    .when()
                    .get(URL+"/api/admin/orders")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList(".", OrderDTO.class);

            Assert.assertTrue(list.stream().anyMatch(x -> x.id().equals(createdOrder.id())));
        });

        Allure.step("Пользователь без админки не может получить список всех заказов", () -> {
            given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/admin/orders")
                    .then()
                    .log().all()
                    .statusCode(403);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Администратор повторно открывает закрытый заказ, затем закрывает. Статус OPENED, выплата списывается у продавца")
    public void successfulReopenClosedOrderByAdmin() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);
        AuthorizedSession adminSes = ApiUtility.authAdmin(URL);

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

        Allure.step("Покупатель подтверждает заказ. Заказ закрыт", () -> {
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/confirm")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что после подтверждения у продавца пополнился баланс", () -> {
            RegisterAccountResponse response = given()
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(RegisterAccountResponse.class);

            Assert.assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(199)));
        });

        Allure.step("Администратор заново открывает заказ", () -> {
            given()
                    .filter(adminSes.sessionFilter())
                    .header(adminSes.csrf().headerName(), adminSes.csrf().token())
                    .when()
                    .patch(URL+"/api/admin/orders/"+createdOrder.id()+"/reopen")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что после переоткрытия у продавца снялся баланс", () -> {
            RegisterAccountResponse response = given()
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(RegisterAccountResponse.class);

            Assert.assertEquals(-1, response.balance().compareTo(BigDecimal.valueOf(199)));
        });

        Allure.step("Проверяем что у заказа статус OPENED", () -> {
            OrderDTO order = given()
                    .filter(adminSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("OPENED", order.orderStatus());
        });

        Allure.step("Администратор подтверждает заказ", () -> {
            given()
                    .filter(adminSes.sessionFilter())
                    .header(adminSes.csrf().headerName(), adminSes.csrf().token())
                    .when()
                    .patch(URL+"/api/admin/orders/"+createdOrder.id()+"/confirm")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что у заказа статус CLOSED", () -> {
            OrderDTO order = given()
                    .filter(adminSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("CLOSED", order.orderStatus());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Администратор возвращает деньги покупателю")
    public void successfulRefundOrConfirmReopenedOrderByAdmin() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);
        AuthorizedSession adminSes = ApiUtility.authAdmin(URL);

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

        RegisterAccountResponse oldUserData = Allure.step("Запрашиваем статистику покупателя для сравнения", () -> given()
                .filter(majorSes.sessionFilter())
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        Allure.step("Администратор отменяет заказ и возвращает деньги", () -> {
            given()
                    .filter(adminSes.sessionFilter())
                    .header(adminSes.csrf().headerName(), adminSes.csrf().token())
                    .when()
                    .patch(URL+"/api/admin/orders/"+createdOrder.id()+"/refund")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что баланс покупателя пополнился после возврата", () -> {
            BigDecimal expectedBalance = oldUserData.balance().add(BigDecimal.valueOf(199));

            RegisterAccountResponse newUserData = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(RegisterAccountResponse.class);

            Assert.assertEquals(expectedBalance, newUserData.balance());
        });
    }

}
