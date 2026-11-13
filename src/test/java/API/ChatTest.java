package API;

import config.TestConfig;
import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ChatTest extends TestBaseAPI{

    public static final String URL = TestConfig.API_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель, продавец и администратор отправляют сообщения в заказ")
    public void successfulSendOrderMessageByAllowedRoles() {
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

        Allure.step("Администратор может отправлять сообщение в заказ", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", "Привет!");
            body.put("clientMessageId", UUID.randomUUID().toString());

            MessageDTO message = given()
                    .filter(adminSes.sessionFilter())
                    .header(adminSes.csrf().headerName(), adminSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(MessageDTO.class);

            Assert.assertEquals("Привет!", message.text());
        });

        Allure.step("Продавец может отправлять сообщение в заказ", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", "Привет!");
            body.put("clientMessageId", UUID.randomUUID().toString());

            MessageDTO message = given()
                    .filter(sellerUtil.sessionFilter())
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(MessageDTO.class);

            Assert.assertEquals("Привет!", message.text());
        });

        Allure.step("Покупатель может отправлять сообщение в заказ", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", "Привет!");
            body.put("clientMessageId", UUID.randomUUID().toString());

            MessageDTO message = given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(MessageDTO.class);

            Assert.assertEquals("Привет!", message.text());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Посторонний пользователь и гость не могут читать и отправлять сообщения в заказ")
    public void unsuccessfulAccessOrderMessagesByUnauthorizedUsers() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

        String guestName = ApiUtility.genNick();
        String guestPassword = ApiUtility.genPass();
        UtilityRegister guestUtil =   ApiUtility.registerAndReturnData(URL, guestName, guestPassword);

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

        Allure.step("Случайный пользователь не может отправлять сообщение в заказ", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", "Привет!");
            body.put("clientMessageId", UUID.randomUUID().toString());
            given()
                    .filter(guestUtil.sessionFilter())
                    .header(guestUtil.csrf().headerName(), guestUtil.csrf().token())
                    .auth().preemptive().basic(guestName, guestPassword)
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body("message", equalTo("Нет прав для отправки сообщения"));
        });
    }

    public static Stream<Object> invalidMessage() {
        return Stream.of(
                null,
                "   ",
                "a".repeat(1001),
                ""
        );
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("invalidMessage")
    @DisplayName("Проверка валидации текста сообщения и границы в 1000 символов")
    public void checkOrderMessageTextValidation(String message) {
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

        String uid = UUID.randomUUID().toString();

        Allure.step("Покупатель не может отправлять невалидное сообщение в заказ", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", message);
            body.put("clientMessageId", uid);

            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(400);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Повтор сообщения не создаёт дубль, тот же ID с другим текстом вызывает конфликт")
    public void checkOrderMessageIdempotency() {
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

        String uid = UUID.randomUUID().toString();

        Allure.step("Покупатель может отправлять сообщение в заказ", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", "Привет!");
            body.put("clientMessageId", uid);

            MessageDTO message = given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(MessageDTO.class);

            Assert.assertEquals("Привет!", message.text());
        });

        Allure.step("Покупатель может отправлять сообщение в заказ с тем же UID", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("text", "Привет!");
            body.put("clientMessageId", uid);

            MessageDTO message = given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/"+createdOrder.id()+"/messages")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(MessageDTO.class);

            Assert.assertEquals("Привет!", message.text());
        });
    }

}
